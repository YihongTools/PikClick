package com.pikclick.app

import android.app.Activity
import android.app.AlertDialog
import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.net.Uri
import android.os.Bundle
import android.provider.Settings
import android.text.InputType
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.view.inputmethod.EditorInfo
import android.widget.EditText
import android.widget.FrameLayout
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView
import android.widget.Toast

class MainActivity : Activity() {
    private lateinit var delayInput: EditText
    private lateinit var overlayPermissionCell: LinearLayout
    private lateinit var overlayPermissionIcon: ImageView
    private lateinit var overlayPermissionStatus: TextView
    private lateinit var accessibilityPermissionCell: LinearLayout
    private lateinit var accessibilityPermissionIcon: ImageView
    private lateinit var accessibilityPermissionStatus: TextView
    private lateinit var primaryActionButton: LinearLayout
    private lateinit var delayError: TextView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(createContentView())
    }

    override fun onResume() {
        super.onResume()
        updateStatus()
    }

    private fun createContentView(): View {
        val root = ScrollView(this).apply {
            setBackgroundColor(getColor(R.color.surface_screen))
        }

        val content = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(resources.getDimensionPixelSize(R.dimen.screen_horizontal_inset), resources.getDimensionPixelSize(R.dimen.screen_vertical_inset), resources.getDimensionPixelSize(R.dimen.screen_horizontal_inset), resources.getDimensionPixelSize(R.dimen.screen_vertical_inset))
        }

        content.isFocusableInTouchMode = true
        content.addView(textView(getString(R.string.screen_title), 28f, true), matchWrapParams())
        content.addView(textView("PikClick", 16f, false, getColor(R.color.text_secondary)), matchWrapParams())

        content.addView(createPermissionCell(getString(R.string.overlay_permission), R.drawable.ic_overlay) {
            openOverlaySettings()
        }.also {
            overlayPermissionCell = it
            overlayPermissionIcon = it.findViewWithTag(ICON_TAG)
            overlayPermissionStatus = it.findViewWithTag(STATUS_TAG)
        }, matchWrapParams().apply { topMargin = dp(12) })
        content.addView(separator(), matchWrapParams().apply { height = dp(1) })
        content.addView(createPermissionCell(getString(R.string.accessibility_permission), R.drawable.ic_accessibility) {
            showAccessibilityDisclosure()
        }.also {
            accessibilityPermissionCell = it
            accessibilityPermissionIcon = it.findViewWithTag(ICON_TAG)
            accessibilityPermissionStatus = it.findViewWithTag(STATUS_TAG)
        }, matchWrapParams())
        content.addView(separator(), matchWrapParams().apply { height = dp(1) })

        primaryActionButton = mainActionButton(
            label = getString(R.string.show_bubble),
            backgroundRes = R.drawable.main_action_background,
            contentDescription = getString(R.string.show_bubble),
        ) {
            when (PrimaryActionState.from(Settings.canDrawOverlays(this@MainActivity), isAccessibilityServiceEnabled())) {
                PrimaryActionState.ENABLE_OVERLAY -> openOverlaySettings()
                PrimaryActionState.ENABLE_ACCESSIBILITY -> showAccessibilityDisclosure()
                PrimaryActionState.SHOW_BUTTON -> {
                    if (!applyDelayFromInput(showSuccessToast = false)) return@mainActionButton
                    startService(Intent(this@MainActivity, OverlayBubbleService::class.java))
                    Toast.makeText(this@MainActivity, R.string.bubble_shown, Toast.LENGTH_SHORT).show()
                    updateStatus(); moveTaskToBack(true)
                }
            }
        }
        content.addView(createMainControlCard(), matchWrapParams().apply {
            setMargins(0, 0, 0, resources.getDimensionPixelSize(R.dimen.space_16))
        })

        content.addView(textView(getString(R.string.operation_hint), 14f, false, getColor(R.color.text_secondary)).apply {
            gravity = Gravity.CENTER
            setPadding(dp(8), 0, dp(8), dp(20))
        }, matchWrapParams())
        content.addView(separator(), matchWrapParams().apply { height = dp(1) })
        content.addView(LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.TOP
            setPadding(0, dp(16), 0, dp(16))
            addView(ImageView(this@MainActivity).apply {
                setImageResource(R.drawable.ic_warning)
                importantForAccessibility = View.IMPORTANT_FOR_ACCESSIBILITY_NO
            }, LinearLayout.LayoutParams(dp(28), dp(28)))
            addView(LinearLayout(this@MainActivity).apply {
                orientation = LinearLayout.VERTICAL
                setPadding(dp(12), 0, 0, 0)
                addView(textView(getString(R.string.safety_title), 17f, true), matchWrapParams())
                addView(textView(getString(R.string.safety_warning), 14f, false, getColor(R.color.text_secondary)).apply {
                    setPadding(0, dp(8), 0, 0)
                }, matchWrapParams())
            }, LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f))
        }, matchWrapParams())

        content.addView(createDonateSection(), matchWrapParams().apply {
            setMargins(0, dp(8), 0, 0)
        })

        root.addView(content)
        return root
    }

    private fun createDonateSection(): LinearLayout {
        return LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER
            minimumHeight = resources.getDimensionPixelSize(R.dimen.touch_target_min)
            setPadding(dp(16), dp(12), dp(16), dp(12))
            background = getDrawable(R.drawable.permission_row_background)
            isClickable = true
            isFocusable = true
            contentDescription = getString(R.string.support_development)
            setOnClickListener { openDonatePage() }

            addView(ImageView(this@MainActivity).apply {
                setImageResource(R.drawable.ic_heart_outline)
                setColorFilter(getColor(R.color.text_secondary))
                importantForAccessibility = View.IMPORTANT_FOR_ACCESSIBILITY_NO
            }, LinearLayout.LayoutParams(dp(24), dp(24)))

            addView(textView(getString(R.string.support_development), 15f, false, getColor(R.color.text_secondary)).apply {
                setPadding(dp(8), 0, 0, 0)
            }, LinearLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT))
        }
    }

    private fun createMainControlCard(): LinearLayout {
        return LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            addView(createDelayCell(), matchWrapParams().apply { topMargin = dp(18) })
            addView(primaryActionButton, matchWrapParams().apply { topMargin = dp(18) })
        }
    }

    private fun mainActionButton(
        label: String,
        backgroundRes: Int,
        contentDescription: String,
        onClick: View.OnClickListener,
    ): LinearLayout {
        return LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            gravity = Gravity.CENTER
            isClickable = true
            isFocusable = true
            this.contentDescription = contentDescription
            setPadding(dp(12), dp(14), dp(12), dp(14))
            background = getDrawable(backgroundRes)
            setOnClickListener(onClick)
            minimumHeight = resources.getDimensionPixelSize(R.dimen.primary_action_min_height)

            addView(textView(label, 21f, true, Color.WHITE).apply {
                tag = PRIMARY_LABEL_TAG
                gravity = Gravity.CENTER
                includeFontPadding = true
                maxLines = 2
            }, matchWrapParams())
        }
    }

    private fun createDelayCell(): LinearLayout {
        val contents = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            gravity = Gravity.CENTER_HORIZONTAL
            setPadding(dp(12), dp(54), dp(12), dp(16))
            addView(textView(getString(R.string.seconds), 14f, true, getColor(R.color.text_secondary)).apply {
                gravity = Gravity.CENTER
            }, matchWrapParams())
            addView(LinearLayout(this@MainActivity).apply {
                orientation = LinearLayout.HORIZONTAL
                gravity = Gravity.CENTER
                delayInput = EditText(this@MainActivity).apply {
                    setText(formatDelay(ClickSettings.getDelaySeconds(this@MainActivity)))
                    inputType = InputType.TYPE_CLASS_NUMBER or InputType.TYPE_NUMBER_FLAG_DECIMAL
                    imeOptions = EditorInfo.IME_ACTION_DONE
                    setSingleLine(true)
                    textSize = 48f
                    setTypeface(typeface, android.graphics.Typeface.BOLD)
                    setTextColor(getColor(R.color.text_primary))
                    setSelectAllOnFocus(true)
                    setPadding(0, 0, 0, 0)
                    backgroundTintList = android.content.res.ColorStateList.valueOf(getColor(R.color.text_secondary))
                    contentDescription = getString(R.string.seconds)
                    minWidth = 0
                    minimumWidth = 0
                    gravity = Gravity.CENTER
                    setOnFocusChangeListener { _, hasFocus ->
                        if (!hasFocus && shouldValidateDelayOnFocusLoss(delayError.visibility == View.VISIBLE)) {
                            applyDelayFromInput(showSuccessToast = false)
                        }
                    }
                    setOnEditorActionListener { _, actionId, _ ->
                        if (actionId == EditorInfo.IME_ACTION_DONE) {
                            applyDelayFromInput(showSuccessToast = true)
                            clearFocus()
                            true
                        } else {
                            false
                        }
                    }
                }
                addView(delayInput, LinearLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT))
                addView(textView(getString(R.string.seconds_unit), 20f, true).apply { setPadding(dp(8), 0, 0, 0) })
            }, matchWrapParams())
            addView(textView(getString(R.string.delay_description), 13f, false, getColor(R.color.text_secondary)).apply {
                gravity = Gravity.CENTER
                setPadding(0, dp(8), 0, 0)
            }, matchWrapParams())
        }
        return LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            addView(DelayPanel(this@MainActivity).apply {
                tag = "delay_panel"
                addView(contents, FrameLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT, Gravity.CENTER))
            }, matchWrapParams())
            delayError = textView(getString(R.string.invalid_delay), 12f, false, getColor(R.color.text_error)).apply {
                visibility = View.GONE
                setPadding(0, dp(8), 0, 0)
                accessibilityLiveRegion = View.ACCESSIBILITY_LIVE_REGION_POLITE
            }
            addView(delayError, matchWrapParams())
        }
    }

    private fun createPermissionCell(label: String, iconRes: Int, onClick: View.OnClickListener): LinearLayout {
        return LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            minimumHeight = dp(64)
            setPadding(dp(4), dp(12), dp(4), dp(12))
            background = getDrawable(R.drawable.permission_row_background)
            isClickable = true
            isFocusable = true
            setOnClickListener(onClick)
            addView(ImageView(this@MainActivity).apply {
                tag = ICON_TAG
                setImageResource(iconRes)
                setColorFilter(getColor(R.color.text_primary))
                importantForAccessibility = View.IMPORTANT_FOR_ACCESSIBILITY_NO
            }, LinearLayout.LayoutParams(dp(28), dp(28)))
            addView(textView(label, 16f, true).apply { setPadding(dp(16), 0, dp(8), 0) },
                LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f))
            addView(ImageView(this@MainActivity).apply {
                tag = READY_TAG
                setImageResource(R.drawable.ic_ready)
                importantForAccessibility = View.IMPORTANT_FOR_ACCESSIBILITY_NO
            }, LinearLayout.LayoutParams(dp(20), dp(20)))
            addView(textView(getString(R.string.go_to_settings), 14f, false, getColor(R.color.text_secondary)).apply {
                tag = STATUS_TAG
                setPadding(dp(8), 0, dp(8), 0)
            })
            addView(ImageView(this@MainActivity).apply {
                setImageResource(R.drawable.ic_chevron)
                importantForAccessibility = View.IMPORTANT_FOR_ACCESSIBILITY_NO
            }, LinearLayout.LayoutParams(dp(16), dp(16)))
        }
    }

    private fun separator(): View = View(this).apply { setBackgroundColor(getColor(R.color.screen_divider)) }

    private fun updateStatus() {
        val hasOverlayPermission = Settings.canDrawOverlays(this)
        val hasAccessibilityPermission = isAccessibilityServiceEnabled()
        val actionState = PrimaryActionState.from(hasOverlayPermission, hasAccessibilityPermission)
        val labelRes = when (actionState) {
            PrimaryActionState.ENABLE_OVERLAY -> R.string.enable_overlay_permission
            PrimaryActionState.ENABLE_ACCESSIBILITY -> R.string.enable_accessibility_service
            PrimaryActionState.SHOW_BUTTON -> R.string.show_bubble
        }
        val label = getString(labelRes)
        primaryActionButton.contentDescription = label
        primaryActionButton.findViewWithTag<TextView>(PRIMARY_LABEL_TAG)?.text = label

        updatePermissionCell(
            cell = overlayPermissionCell,
            icon = overlayPermissionIcon,
            statusText = overlayPermissionStatus,
            status = getString(if (hasOverlayPermission) R.string.authorized else R.string.authorize),
            isReady = hasOverlayPermission,
        )
        updatePermissionCell(
            cell = accessibilityPermissionCell,
            icon = accessibilityPermissionIcon,
            statusText = accessibilityPermissionStatus,
            status = getString(if (hasAccessibilityPermission) R.string.enabled else R.string.enable),
            isReady = hasAccessibilityPermission,
        )
    }

    private fun updatePermissionCell(
        cell: LinearLayout,
        icon: ImageView,
        statusText: TextView,
        status: String,
        isReady: Boolean,
    ) {
        statusText.text = status
        statusText.setTextColor(getColor(R.color.text_secondary))
        icon.setColorFilter(getColor(R.color.text_primary))
        cell.findViewWithTag<ImageView>(READY_TAG).visibility = if (isReady) View.VISIBLE else View.INVISIBLE
    }

    private fun applyDelayFromInput(showSuccessToast: Boolean): Boolean {
        val rawValue = delayInput.text.toString().trim()
        val value = rawValue.toFloatOrNull()
        if (value == null) {
            showDelayError()
            restoreSavedDelayText()
            return false
        }
        if (!ClickPolicy.isValidDelay(value)) {
            showDelayError()
            restoreSavedDelayText()
            return false
        }

        delayError.visibility = View.GONE
        ClickSettings.saveDelaySeconds(this, value)
        delayInput.setText(formatDelay(value))
        delayInput.setSelection(delayInput.text.length)
        if (showSuccessToast) {
            Toast.makeText(this, R.string.seconds_applied, Toast.LENGTH_SHORT).show()
        }
        return true
    }

    private fun showDelayError() {
        delayError.text = getString(R.string.seconds_range, ClickSettings.MIN_DELAY_SECONDS.toDouble(), ClickSettings.MAX_DELAY_SECONDS.toDouble())
        delayError.visibility = View.VISIBLE
    }

    private fun restoreSavedDelayText() {
        delayInput.setText(formatDelay(ClickSettings.getDelaySeconds(this)))
        delayInput.setSelection(delayInput.text.length)
    }

    private fun openOverlaySettings() {
        val intent = Intent(
            Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
            Uri.parse("package:$packageName"),
        )
        startActivity(intent)
    }

    private fun showAccessibilityDisclosure() {
        AlertDialog.Builder(this)
            .setTitle(R.string.accessibility_disclosure_title)
            .setMessage(R.string.accessibility_disclosure_message)
            .setNegativeButton(R.string.cancel, null)
            .setPositiveButton(R.string.continue_to_settings) { _, _ ->
                startActivity(Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS))
            }
            .show()
    }

    private fun openDonatePage() {
        val intent = Intent(Intent.ACTION_VIEW, Uri.parse(DONATE_URL)).apply {
            addCategory(Intent.CATEGORY_BROWSABLE)
        }
        startActivity(intent)
    }

    private fun isAccessibilityServiceEnabled(): Boolean {
        val enabledServices = Settings.Secure.getString(
            contentResolver,
            Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES,
        ) ?: return false
        val serviceName = "$packageName/${AutoClickAccessibilityService::class.java.name}"
        val shortServiceName = "$packageName/.${AutoClickAccessibilityService::class.java.simpleName}"
        return enabledServices
            .split(':')
            .any { it == serviceName || it == shortServiceName }
    }

    private fun textView(
        text: String,
        size: Float,
        bold: Boolean,
        color: Int = getColor(R.color.text_primary),
    ): TextView {
        return TextView(this).apply {
            this.text = text
            textSize = size
            setTextColor(color)
            includeFontPadding = true
            if (bold) {
                setTypeface(typeface, android.graphics.Typeface.BOLD)
            }
        }
    }

    private fun matchWrapParams(): LinearLayout.LayoutParams {
        return LinearLayout.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT,
            ViewGroup.LayoutParams.WRAP_CONTENT,
        )
    }

    private fun formatDelay(value: Float): String {
        return "%.1f".format(value)
    }

    private companion object {
        const val READY_TAG = "ready"
        const val ICON_TAG = "icon"
        const val STATUS_TAG = "status"
        const val DONATE_URL = "https://yihongcho.bobaboba.me"
        const val PRIMARY_LABEL_TAG = "primary_label"
    }
}

fun Context.dp(value: Int): Int {
    return (value * resources.displayMetrics.density).toInt()
}

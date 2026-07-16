package com.pikclick.app

import android.app.Activity
import android.app.AlertDialog
import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.graphics.drawable.GradientDrawable
import android.net.Uri
import android.os.Bundle
import android.provider.Settings
import android.text.InputType
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.view.inputmethod.EditorInfo
import android.widget.EditText
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
            setBackgroundColor(Color.rgb(248, 250, 252))
        }

        val content = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(20), dp(28), dp(20), dp(28))
        }

        content.addView(textView(getString(R.string.app_name), 24f, true).apply {
            gravity = Gravity.CENTER_HORIZONTAL
        }, matchWrapParams())

        content.addView(controlRow(topMargin = 20, bottomMargin = 14).apply {
            addView(createDelayCell(), weightedCellParams(height = 96, endMargin = 6))

            addView(mainActionButton(
                label = getString(R.string.show_bubble),
                iconRes = R.drawable.ic_play,
                backgroundRes = R.drawable.main_action_background,
                contentDescription = getString(R.string.show_bubble),
            ) {
                if (!applyDelayFromInput(showSuccessToast = false)) return@mainActionButton
                if (!Settings.canDrawOverlays(this@MainActivity)) {
                    openOverlaySettings()
                    return@mainActionButton
                }
                startService(Intent(this@MainActivity, OverlayBubbleService::class.java))
                Toast.makeText(this@MainActivity, R.string.bubble_shown, Toast.LENGTH_SHORT).show()
                updateStatus()
                moveTaskToBack(true)
            }, weightedCellParams(height = 96, startMargin = 6))
        })

        content.addView(controlRow(bottomMargin = 16).apply {
            addView(createPermissionCell(
                label = getString(R.string.overlay_permission),
                iconRes = R.drawable.ic_shield,
                onClick = { openOverlaySettings() },
            ).also {
                overlayPermissionCell = it
                overlayPermissionIcon = it.findViewWithTag(ICON_TAG)
                overlayPermissionStatus = it.findViewWithTag(STATUS_TAG)
            }, weightedCellParams(endMargin = 6))

            addView(createPermissionCell(
                label = getString(R.string.accessibility_permission),
                iconRes = R.drawable.ic_accessibility,
                onClick = { showAccessibilityDisclosure() },
            ).also {
                accessibilityPermissionCell = it
                accessibilityPermissionIcon = it.findViewWithTag(ICON_TAG)
                accessibilityPermissionStatus = it.findViewWithTag(STATUS_TAG)
            }, weightedCellParams(startMargin = 6))
        })

        content.addView(textView(
            getString(R.string.safety_warning),
            14f,
            false,
            Color.rgb(153, 27, 27),
        ).apply {
            gravity = Gravity.CENTER
            setPadding(dp(12), dp(10), dp(12), dp(10))
            background = getDrawable(R.drawable.warning_background)
        }, matchWrapParams())

        content.addView(createDonateSection(), matchWrapParams().apply {
            setMargins(0, dp(14), 0, 0)
        })

        root.addView(content)
        return root
    }

    private fun createDonateSection(): LinearLayout {
        return LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER
            setPadding(dp(12), dp(10), dp(12), dp(10))
            background = getDrawable(R.drawable.button_secondary_background)

            addView(textView(
                getString(R.string.support_message),
                14f,
                false,
                Color.rgb(75, 85, 99),
            ).apply {
                gravity = Gravity.CENTER_VERTICAL
            }, LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f))

            addView(ImageView(this@MainActivity).apply {
                setImageResource(R.drawable.ic_heart)
                setColorFilter(Color.rgb(255, 94, 91))
                background = circleDrawable(Color.WHITE)
                setPadding(dp(8), dp(8), dp(8), dp(8))
                isClickable = true
                isFocusable = true
                contentDescription = getString(R.string.support_development)
                setOnClickListener { openDonatePage() }
            }, LinearLayout.LayoutParams(
                dp(44),
                dp(44),
            ))
        }
    }

    private fun mainActionButton(
        label: String,
        iconRes: Int,
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
            setPadding(dp(10), dp(14), dp(10), dp(14))
            background = getDrawable(backgroundRes)
            setOnClickListener(onClick)

            addView(ImageView(this@MainActivity).apply {
                setImageResource(iconRes)
            }, LinearLayout.LayoutParams(dp(34), dp(34)))

            addView(textView(label, 16f, true, Color.WHITE).apply {
                gravity = Gravity.CENTER
                includeFontPadding = true
                maxLines = 1
                setPadding(0, dp(8), 0, 0)
            }, matchWrapParams())
        }
    }

    private fun createDelayCell(): LinearLayout {
        return LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER
            setPadding(dp(10), dp(8), dp(10), dp(8))
            background = getDrawable(R.drawable.button_secondary_background)

            addView(iconView(R.drawable.ic_clock, Color.rgb(37, 99, 235), Color.rgb(219, 234, 254)))

            addView(LinearLayout(this@MainActivity).apply {
                orientation = LinearLayout.VERTICAL
                gravity = Gravity.CENTER_VERTICAL
                setPadding(dp(10), 0, 0, 0)

                addView(textView(getString(R.string.seconds), 13f, true, Color.rgb(75, 85, 99)))

                delayInput = EditText(this@MainActivity).apply {
                    setText(formatDelay(ClickSettings.getDelaySeconds(this@MainActivity)))
                    inputType = InputType.TYPE_CLASS_NUMBER or InputType.TYPE_NUMBER_FLAG_DECIMAL
                    imeOptions = EditorInfo.IME_ACTION_DONE
                    setSingleLine(true)
                    textSize = 20f
                    setTextColor(Color.rgb(17, 24, 39))
                    setSelectAllOnFocus(true)
                    setPadding(0, 0, 0, 0)
                    background = null
                    setOnFocusChangeListener { _, hasFocus ->
                        if (!hasFocus) {
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
                addView(delayInput, LinearLayout.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    dp(34),
                ))
            }, LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.MATCH_PARENT, 1f))
        }
    }

    private fun createPermissionCell(
        label: String,
        iconRes: Int,
        onClick: View.OnClickListener,
    ): LinearLayout {
        return statusCell(
            label = label,
            status = getString(R.string.go_to_settings),
            iconRes = iconRes,
            iconColor = Color.rgb(22, 101, 52),
            iconBackgroundColor = Color.rgb(220, 252, 231),
            onClick = onClick,
        )
    }

    private fun statusCell(
        label: String,
        status: String,
        iconRes: Int,
        iconColor: Int,
        iconBackgroundColor: Int,
        onClick: View.OnClickListener?,
    ): LinearLayout {
        return LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            setPadding(dp(10), dp(8), dp(10), dp(8))
            background = getDrawable(R.drawable.button_secondary_background)
            if (onClick != null) {
                isClickable = true
                isFocusable = true
                setOnClickListener(onClick)
            }

            addView(iconView(iconRes, iconColor, iconBackgroundColor).apply {
                tag = ICON_TAG
            })

            addView(LinearLayout(this@MainActivity).apply {
                orientation = LinearLayout.VERTICAL
                gravity = Gravity.CENTER_VERTICAL
                setPadding(dp(10), 0, 0, 0)
                addView(textView(label, 13f, true, Color.rgb(75, 85, 99)))
                addView(textView(status, 16f, true, Color.rgb(75, 85, 99)).apply {
                    tag = STATUS_TAG
                    setPadding(0, dp(4), 0, 0)
                })
            }, LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.MATCH_PARENT, 1f))
        }
    }

    private fun iconView(iconRes: Int, iconColor: Int, backgroundColor: Int): ImageView {
        return ImageView(this).apply {
            setImageResource(iconRes)
            setColorFilter(iconColor)
            background = circleDrawable(backgroundColor)
            setPadding(dp(8), dp(8), dp(8), dp(8))
            layoutParams = LinearLayout.LayoutParams(dp(44), dp(44))
        }
    }

    private fun updateStatus() {
        val hasOverlayPermission = Settings.canDrawOverlays(this)
        val hasAccessibilityPermission = isAccessibilityServiceEnabled()

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
        statusText.setTextColor(if (isReady) Color.rgb(22, 101, 52) else Color.rgb(75, 85, 99))
        icon.setColorFilter(if (isReady) Color.rgb(22, 101, 52) else Color.rgb(75, 85, 99))
        icon.background = circleDrawable(
            if (isReady) Color.rgb(220, 252, 231) else Color.rgb(243, 244, 246),
        )
        cell.background = getDrawable(
            if (isReady) R.drawable.status_ready_background else R.drawable.status_missing_background,
        )
    }

    private fun applyDelayFromInput(showSuccessToast: Boolean): Boolean {
        val rawValue = delayInput.text.toString().trim()
        val value = rawValue.toFloatOrNull()
        if (value == null) {
            Toast.makeText(this, R.string.enter_seconds, Toast.LENGTH_SHORT).show()
            restoreSavedDelayText()
            return false
        }
        if (!ClickPolicy.isValidDelay(value)) {
            Toast.makeText(
                this,
                getString(
                    R.string.seconds_range,
                    ClickSettings.MIN_DELAY_SECONDS.toDouble(),
                    ClickSettings.MAX_DELAY_SECONDS.toDouble(),
                ),
                Toast.LENGTH_SHORT,
            ).show()
            restoreSavedDelayText()
            return false
        }

        ClickSettings.saveDelaySeconds(this, value)
        delayInput.setText(formatDelay(value))
        delayInput.setSelection(delayInput.text.length)
        if (showSuccessToast) {
            Toast.makeText(this, R.string.seconds_applied, Toast.LENGTH_SHORT).show()
        }
        return true
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
        color: Int = Color.rgb(17, 24, 39),
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

    private fun circleDrawable(color: Int): GradientDrawable {
        return GradientDrawable().apply {
            shape = GradientDrawable.OVAL
            setColor(color)
        }
    }

    private fun matchWrapParams(): LinearLayout.LayoutParams {
        return LinearLayout.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT,
            ViewGroup.LayoutParams.WRAP_CONTENT,
        )
    }

    private fun controlRow(
        topMargin: Int = 0,
        bottomMargin: Int = 0,
    ): LinearLayout {
        return LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            layoutParams = matchWrapParams().apply {
                setMargins(0, dp(topMargin), 0, dp(bottomMargin))
            }
        }
    }

    private fun weightedCellParams(
        height: Int = 82,
        startMargin: Int = 0,
        endMargin: Int = 0,
    ): LinearLayout.LayoutParams {
        return LinearLayout.LayoutParams(
            0,
            dp(height),
            1f,
        ).apply {
            setMargins(dp(startMargin), 0, dp(endMargin), 0)
        }
    }

    private fun formatDelay(value: Float): String {
        return "%.1f".format(value)
    }

    private companion object {
        const val ICON_TAG = "icon"
        const val STATUS_TAG = "status"
        const val DONATE_URL = "https://ko-fi.com/yihongcho"
    }
}

fun Context.dp(value: Int): Int {
    return (value * resources.displayMetrics.density).toInt()
}

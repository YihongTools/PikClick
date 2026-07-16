package com.pikclick.app

import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.res.Configuration
import android.graphics.Color
import android.graphics.PixelFormat
import android.graphics.Point
import android.os.Handler
import android.os.IBinder
import android.os.Looper
import android.provider.Settings
import android.view.Gravity
import android.view.MotionEvent
import android.view.WindowManager
import android.widget.TextView
import android.widget.Toast
import kotlin.math.abs
import kotlin.math.ceil

private class AccessibleBubbleView(context: Context) : TextView(context) {
    override fun performClick(): Boolean {
        super.performClick()
        return true
    }
}

class OverlayBubbleService : Service() {
    private val handler = Handler(Looper.getMainLooper())
    private lateinit var windowManager: WindowManager
    private var bubbleView: TextView? = null
    private var closeView: TextView? = null
    private var layoutParams: WindowManager.LayoutParams? = null
    private var closeLayoutParams: WindowManager.LayoutParams? = null
    private var pendingClick: Runnable? = null
    private var countdownTick: Runnable? = null
    private var isWaiting = false
    private val sequenceGate = ClickSequenceGate()
    private val latestClickTarget = LatestClickTarget<Point>()

    override fun onCreate() {
        super.onCreate()
        windowManager = getSystemService(WINDOW_SERVICE) as WindowManager
        if (!Settings.canDrawOverlays(this)) {
            stopSelf()
            return
        }
        isRunning = true
        showBubble()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        if (bubbleView == null && Settings.canDrawOverlays(this)) {
            showBubble()
        }
        if (intent?.action == ACTION_TEST_CLICK) {
            performTestClick()
        }
        return START_STICKY
    }

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onDestroy() {
        cancelPendingClick()
        bubbleView?.let { view ->
            runCatching { windowManager.removeView(view) }
        }
        closeView?.let { view ->
            runCatching { windowManager.removeView(view) }
        }
        bubbleView = null
        closeView = null
        layoutParams = null
        closeLayoutParams = null
        isRunning = false
        super.onDestroy()
    }

    override fun onConfigurationChanged(newConfig: Configuration) {
        super.onConfigurationChanged(newConfig)
        handler.post {
            val view = bubbleView ?: return@post
            val params = layoutParams ?: return@post
            clampBubblePosition(params, view)
            if (safeUpdateViewLayout(view, params)) {
                updateCloseButtonPosition(params, view.width)
                ClickSettings.saveBubblePosition(this, params.x, params.y)
            }
        }
    }

    private fun showBubble() {
        val size = dp(64)
        val view = AccessibleBubbleView(this).apply {
            text = getString(R.string.bubble_start)
            textSize = 13f
            gravity = Gravity.CENTER
            setTextColor(Color.WHITE)
            setTypeface(typeface, android.graphics.Typeface.BOLD)
            background = getDrawable(R.drawable.bubble_background)
            elevation = dp(8).toFloat()
            isClickable = true
            setOnClickListener { toggleClickCountdown() }
        }

        val params = WindowManager.LayoutParams(
            size,
            size,
            WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS,
            PixelFormat.TRANSLUCENT,
        ).apply {
            gravity = Gravity.TOP or Gravity.START
            val savedPosition = ClickSettings.getBubblePosition(this@OverlayBubbleService)
            if (savedPosition == null) {
                x = resources.displayMetrics.widthPixels / 2 - size / 2
                y = resources.displayMetrics.heightPixels / 2 - size / 2
            } else {
                x = savedPosition.first.coerceIn(0, (resources.displayMetrics.widthPixels - size).coerceAtLeast(0))
                y = savedPosition.second.coerceIn(0, (resources.displayMetrics.heightPixels - size).coerceAtLeast(0))
            }
        }

        attachDragAndClick(view, params)
        windowManager.addView(view, params)
        bubbleView = view
        layoutParams = params
        showCloseButton(params, size)
    }

    private fun showCloseButton(anchorParams: WindowManager.LayoutParams, bubbleSize: Int) {
        val size = dp(28)
        val view = TextView(this).apply {
            text = "×"
            textSize = 18f
            gravity = Gravity.CENTER
            setTextColor(Color.WHITE)
            setTypeface(typeface, android.graphics.Typeface.BOLD)
            background = getDrawable(R.drawable.close_bubble_background)
            elevation = dp(10).toFloat()
            setOnClickListener {
                closeBubbleAndReturnToMain()
            }
        }

        val params = WindowManager.LayoutParams(
            size,
            size,
            WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS,
            PixelFormat.TRANSLUCENT,
        ).apply {
            gravity = Gravity.TOP or Gravity.START
        }
        updateCloseButtonParams(params, anchorParams, bubbleSize)

        windowManager.addView(view, params)
        closeView = view
        closeLayoutParams = params
    }

    private fun attachDragAndClick(view: AccessibleBubbleView, params: WindowManager.LayoutParams) {
        var initialX = 0
        var initialY = 0
        var touchStartX = 0f
        var touchStartY = 0f
        var moved = false

        view.setOnTouchListener { _, event ->
            when (event.action) {
                MotionEvent.ACTION_DOWN -> {
                    initialX = params.x
                    initialY = params.y
                    touchStartX = event.rawX
                    touchStartY = event.rawY
                    moved = false
                    true
                }

                MotionEvent.ACTION_MOVE -> {
                    val dx = (event.rawX - touchStartX).toInt()
                    val dy = (event.rawY - touchStartY).toInt()
                    moved = moved || abs(dx) > dp(DRAG_THRESHOLD_DP) || abs(dy) > dp(DRAG_THRESHOLD_DP)
                    params.x = initialX + dx
                    params.y = initialY + dy
                    if (!safeUpdateViewLayout(view, params)) return@setOnTouchListener false
                    updateCloseButtonPosition(params, view.width)
                    true
                }

                MotionEvent.ACTION_UP -> {
                    clampBubblePosition(params, view)
                    if (!safeUpdateViewLayout(view, params)) return@setOnTouchListener false
                    updateCloseButtonPosition(params, view.width)
                    ClickSettings.saveBubblePosition(this, params.x, params.y)
                    if (!moved) {
                        view.performClick()
                    }
                    true
                }

                else -> false
            }
        }
    }

    private fun updateCloseButtonPosition(
        anchorParams: WindowManager.LayoutParams,
        bubbleSize: Int,
    ) {
        val closeParams = closeLayoutParams ?: return
        updateCloseButtonParams(closeParams, anchorParams, bubbleSize)
        closeView?.let { view ->
            runCatching { windowManager.updateViewLayout(view, closeParams) }
        }
    }

    private fun updateCloseButtonParams(
        closeParams: WindowManager.LayoutParams,
        anchorParams: WindowManager.LayoutParams,
        bubbleSize: Int,
    ) {
        val closeSize = if (closeParams.width > 0) closeParams.width else dp(28)
        closeParams.x = anchorParams.x + bubbleSize - dp(8)
        closeParams.y = anchorParams.y - closeSize / 2
    }

    private fun closeBubbleAndReturnToMain() {
        cancelPendingClick()
        val intent = Intent(this, MainActivity::class.java).apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP)
            addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP)
        }
        startActivity(intent)
        stopSelf()
    }

    private fun toggleClickCountdown() {
        if (isWaiting) {
            cancelPendingClick()
            Toast.makeText(this, R.string.sequence_cancelled, Toast.LENGTH_SHORT).show()
            return
        }
        if (!AutoClickAccessibilityService.isRunning) {
            Toast.makeText(this, R.string.enable_accessibility_first, Toast.LENGTH_SHORT).show()
            return
        }

        val delayMillis = (ClickSettings.getDelaySeconds(this) * 1000L).toLong()
            .coerceAtLeast((ClickSettings.MIN_DELAY_SECONDS * 1000L).toLong())

        isWaiting = true
        val sequenceId = sequenceGate.begin()
        bubbleView?.text = getString(R.string.bubble_clicking)
        performBubbleClick(
            sequenceId = sequenceId,
            successMessage = getString(R.string.sequence_started, delayMillis / 1000.0),
            failureMessage = getString(R.string.first_click_failed),
            onSuccess = {
                startCountdownLabel(delayMillis)
                scheduleFinalClick(sequenceId, delayMillis)
            },
        )
    }

    private fun scheduleFinalClick(sequenceId: Int, delayMillis: Long) {
        val task = Runnable {
            if (!isActiveSequence(sequenceId)) return@Runnable
            performBubbleClick(
                sequenceId = sequenceId,
                successMessage = getString(R.string.second_click_completed),
                failureMessage = getString(R.string.second_click_failed),
                onSuccess = { resetAfterClick(R.string.bubble_completed) },
            )
        }
        pendingClick = task
        handler.postDelayed(task, delayMillis)
    }

    private fun performBubbleClick(
        sequenceId: Int,
        successMessage: String,
        failureMessage: String,
        onSuccess: () -> Unit,
    ) {
        if (!hasRequiredPermissions()) {
            cancelPendingClick()
            Toast.makeText(this, R.string.permission_revoked_cancelled, Toast.LENGTH_SHORT).show()
            return
        }
        val clickPoint = currentBubbleCenter()
        if (clickPoint == null) {
            resetAfterClick(R.string.bubble_failed)
            Toast.makeText(this, R.string.bubble_position_missing, Toast.LENGTH_SHORT).show()
            return
        }

        setBubbleTouchable(false)
        scheduleBubbleTouchableReset(sequenceId)
        handler.postDelayed(
            {
                if (!isActiveSequence(sequenceId)) {
                    setBubbleTouchable(true)
                    return@postDelayed
                }
                AutoClickAccessibilityService.performClick(clickPoint) { success ->
                    handler.post {
                        setBubbleTouchable(true)
                        if (!isActiveSequence(sequenceId)) return@post
                        if (success) {
                            Toast.makeText(this, successMessage, Toast.LENGTH_SHORT).show()
                            onSuccess()
                        } else {
                            resetAfterClick(R.string.bubble_failed)
                            Toast.makeText(this, failureMessage, Toast.LENGTH_SHORT).show()
                        }
                    }
                }
            },
            CLICK_DISPATCH_DELAY_MS,
        )
    }

    private fun performTestClick() {
        if (isWaiting) {
            Toast.makeText(this, R.string.countdown_in_progress, Toast.LENGTH_SHORT).show()
            return
        }
        if (!AutoClickAccessibilityService.isRunning) {
            Toast.makeText(this, R.string.enable_accessibility_first, Toast.LENGTH_SHORT).show()
            return
        }
        val clickPoint = currentBubbleCenter()
        if (clickPoint == null) {
            Toast.makeText(this, R.string.bubble_center_missing, Toast.LENGTH_SHORT).show()
            return
        }

        val sequenceId = sequenceGate.begin()
        setBubbleTouchable(false)
        handler.postDelayed(
            {
                if (!sequenceGate.isCurrent(sequenceId)) {
                    setBubbleTouchable(true)
                    return@postDelayed
                }
                AutoClickAccessibilityService.performClick(clickPoint) { success ->
                    handler.post {
                        setBubbleTouchable(true)
                        if (!sequenceGate.isCurrent(sequenceId)) return@post
                        Toast.makeText(
                            this,
                            getString(if (success) R.string.test_click_success else R.string.test_click_failed),
                            Toast.LENGTH_SHORT,
                        ).show()
                    }
                }
            },
            CLICK_DISPATCH_DELAY_MS,
        )
    }

    private fun scheduleBubbleTouchableReset(sequenceId: Int) {
        handler.postDelayed(
            {
                if (isActiveSequence(sequenceId)) {
                    setBubbleTouchable(true)
                }
            },
            CLICK_TIMEOUT_MS,
        )
    }

    private fun cancelPendingClick() {
        sequenceGate.invalidate()
        setBubbleTouchable(true)
        pendingClick?.let { handler.removeCallbacks(it) }
        countdownTick?.let { handler.removeCallbacks(it) }
        pendingClick = null
        countdownTick = null
        isWaiting = false
        latestClickTarget.clear()
        bubbleView?.text = getString(R.string.bubble_start)
    }

    private fun resetAfterClick(finalLabelRes: Int = R.string.bubble_start) {
        val resetSequenceId = sequenceGate.begin()
        setBubbleTouchable(true)
        countdownTick?.let { handler.removeCallbacks(it) }
        pendingClick = null
        countdownTick = null
        isWaiting = false
        bubbleView?.text = getString(finalLabelRes)
        if (finalLabelRes != R.string.bubble_start) {
            handler.postDelayed(
                {
                    if (!isWaiting && sequenceGate.isCurrent(resetSequenceId)) {
                        bubbleView?.text = getString(R.string.bubble_start)
                    }
                },
                RESET_LABEL_DELAY_MS,
            )
        }
    }

    private fun isActiveSequence(sequenceId: Int): Boolean {
        return isWaiting && sequenceGate.isCurrent(sequenceId)
    }

    private fun setBubbleTouchable(isTouchable: Boolean) {
        val view = bubbleView ?: return
        val params = layoutParams ?: return
        val newFlags = if (isTouchable) {
            params.flags and WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE.inv()
        } else {
            params.flags or WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE
        }
        if (params.flags == newFlags) return
        params.flags = newFlags
        runCatching { windowManager.updateViewLayout(view, params) }
    }

    private fun startCountdownLabel(delayMillis: Long) {
        val startedAt = System.currentTimeMillis()
        val tick = object : Runnable {
            override fun run() {
                if (!isWaiting) return

                val elapsed = System.currentTimeMillis() - startedAt
                val remainingMillis = (delayMillis - elapsed).coerceAtLeast(0L)
                val remainingSeconds = ceil(remainingMillis / 1000.0).toInt().coerceAtLeast(1)
                bubbleView?.text = getString(R.string.bubble_countdown, remainingSeconds)

                if (remainingMillis > 0L) {
                    handler.postDelayed(this, COUNTDOWN_TICK_MS)
                }
            }
        }
        countdownTick = tick
        tick.run()
    }

    private fun clampBubblePosition(params: WindowManager.LayoutParams, view: TextView) {
        params.x = params.x.coerceIn(0, (resources.displayMetrics.widthPixels - view.width).coerceAtLeast(0))
        params.y = params.y.coerceIn(0, (resources.displayMetrics.heightPixels - view.height).coerceAtLeast(0))
    }

    private fun currentBubbleCenter(): Point? {
        val view = bubbleView ?: return null
        if (view.width <= 0 || view.height <= 0) return null

        val location = IntArray(2)
        view.getLocationOnScreen(location)
        val centerX = location[0] + view.width / 2
        val centerY = location[1] + view.height / 2
        val displayMetrics = resources.displayMetrics
        if (centerX !in 0..displayMetrics.widthPixels || centerY !in 0..displayMetrics.heightPixels) {
            return null
        }
        return latestClickTarget.update(Point(centerX, centerY))
    }

    private fun hasRequiredPermissions(): Boolean {
        return Settings.canDrawOverlays(this) && AutoClickAccessibilityService.isRunning
    }

    private fun safeUpdateViewLayout(view: TextView, params: WindowManager.LayoutParams): Boolean {
        return runCatching {
            windowManager.updateViewLayout(view, params)
            true
        }.getOrElse {
            cancelPendingClick()
            stopSelf()
            false
        }
    }

    companion object {
        const val ACTION_TEST_CLICK = "com.pikclick.app.action.TEST_CLICK"
        private const val DRAG_THRESHOLD_DP = 8
        private const val COUNTDOWN_TICK_MS = 250L
        private const val RESET_LABEL_DELAY_MS = 1000L
        private const val CLICK_DISPATCH_DELAY_MS = 80L
        private const val CLICK_TIMEOUT_MS = 1000L
        var isRunning = false
            private set
    }
}

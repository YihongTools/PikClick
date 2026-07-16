package com.pikclick.app

import android.accessibilityservice.AccessibilityService
import android.accessibilityservice.GestureDescription
import android.graphics.Path
import android.graphics.Point
import android.view.accessibility.AccessibilityEvent

class AutoClickAccessibilityService : AccessibilityService() {
    override fun onServiceConnected() {
        instance = this
    }

    override fun onAccessibilityEvent(event: AccessibilityEvent?) = Unit

    override fun onInterrupt() = Unit

    override fun onDestroy() {
        if (instance === this) {
            instance = null
        }
        super.onDestroy()
    }

    private fun clickAt(point: Point, callback: ClickCallback) {
        if (point.x < 0 || point.y < 0) {
            callback(false)
            return
        }

        val clickPath = Path().apply {
            moveTo(point.x.toFloat(), point.y.toFloat())
        }
        val gesture = GestureDescription.Builder()
            .addStroke(GestureDescription.StrokeDescription(clickPath, 0, CLICK_DURATION_MS))
            .build()

        dispatchGesture(
            gesture,
            object : GestureResultCallback() {
                override fun onCompleted(gestureDescription: GestureDescription?) {
                    callback(true)
                }

                override fun onCancelled(gestureDescription: GestureDescription?) {
                    callback(false)
                }
            },
            null,
        )
    }

    companion object {
        private const val CLICK_DURATION_MS = 50L
        private var instance: AutoClickAccessibilityService? = null

        val isRunning: Boolean
            get() = instance != null

        fun performClick(point: Point, callback: ClickCallback) {
            val service = instance
            if (service == null) {
                callback(false)
                return
            }
            service.clickAt(point, callback)
        }
    }
}

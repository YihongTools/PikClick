package com.pikclick.app

import android.content.Context

object ClickSettings {
    private const val PREFS_NAME = "click_settings"
    private const val KEY_DELAY_SECONDS = "delay_seconds"
    const val DEFAULT_DELAY_SECONDS = 3.5f
    const val MIN_DELAY_SECONDS = 3f
    const val MAX_DELAY_SECONDS = 10f
    private const val KEY_BUBBLE_X = "bubble_x"
    private const val KEY_BUBBLE_Y = "bubble_y"

    fun getDelaySeconds(context: Context): Float {
        return context
            .getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .getFloat(KEY_DELAY_SECONDS, DEFAULT_DELAY_SECONDS)
    }

    fun saveDelaySeconds(context: Context, seconds: Float) {
        val safeValue = ClickPolicy.safeDelay(seconds)
        context
            .getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .edit()
            .putFloat(KEY_DELAY_SECONDS, safeValue)
            .apply()
    }

    fun getBubblePosition(context: Context): Pair<Int, Int>? {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        if (!prefs.contains(KEY_BUBBLE_X) || !prefs.contains(KEY_BUBBLE_Y)) {
            return null
        }
        return prefs.getInt(KEY_BUBBLE_X, 0) to prefs.getInt(KEY_BUBBLE_Y, 0)
    }

    fun saveBubblePosition(context: Context, x: Int, y: Int) {
        context
            .getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .edit()
            .putInt(KEY_BUBBLE_X, x)
            .putInt(KEY_BUBBLE_Y, y)
            .apply()
    }
}

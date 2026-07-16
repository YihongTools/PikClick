package com.pikclick.app

object ClickPolicy {
    fun isValidDelay(seconds: Float): Boolean =
        seconds.isFinite() && seconds in ClickSettings.MIN_DELAY_SECONDS..ClickSettings.MAX_DELAY_SECONDS

    fun safeDelay(seconds: Float): Float = when {
        !seconds.isFinite() -> ClickSettings.DEFAULT_DELAY_SECONDS
        else -> seconds.coerceIn(ClickSettings.MIN_DELAY_SECONDS, ClickSettings.MAX_DELAY_SECONDS)
    }
}

/** Invalidates callbacks from an earlier click sequence after cancel/restart. */
class ClickSequenceGate {
    private var current = 0

    fun begin(): Int = ++current

    fun invalidate() {
        current++
    }

    fun isCurrent(token: Int): Boolean = token == current
}

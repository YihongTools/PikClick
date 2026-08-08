package com.pikclick.app

enum class PrimaryActionState {
    ENABLE_OVERLAY,
    ENABLE_ACCESSIBILITY,
    SHOW_BUTTON;

    companion object {
        @JvmStatic
        fun from(hasOverlayPermission: Boolean, hasAccessibilityPermission: Boolean): PrimaryActionState {
            return when {
                !hasOverlayPermission -> ENABLE_OVERLAY
                !hasAccessibilityPermission -> ENABLE_ACCESSIBILITY
                else -> SHOW_BUTTON
            }
        }
    }
}

package com.pikclick.app

fun interface ClickCallback {
    operator fun invoke(success: Boolean)
}

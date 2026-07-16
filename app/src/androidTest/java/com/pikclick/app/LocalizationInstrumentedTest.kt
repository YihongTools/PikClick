package com.pikclick.app

import android.content.Context
import android.content.res.Configuration
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith
import java.util.Locale

@RunWith(AndroidJUnit4::class)
class LocalizationInstrumentedTest {
    private val context: Context = ApplicationProvider.getApplicationContext()

    @Test
    fun loadsTraditionalChineseInterfaceFromSystemLocale() {
        val localized = context.forLocale("zh-Hant-TW")
        assertEquals("顯示圓點", localized.getString(R.string.show_bubble))
        assertEquals("無障礙", localized.getString(R.string.accessibility_permission))
    }

    @Test
    fun loadsEnglishInterfaceFromSystemLocale() {
        val localized = context.forLocale("en-US")
        assertEquals("Show button", localized.getString(R.string.show_bubble))
        assertEquals("Accessibility", localized.getString(R.string.accessibility_permission))
    }

    private fun Context.forLocale(languageTag: String): Context {
        val configuration = Configuration(resources.configuration).apply {
            setLocale(Locale.forLanguageTag(languageTag))
        }
        return createConfigurationContext(configuration)
    }
}

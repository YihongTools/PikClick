package com.pikclick.app

import android.app.Activity
import android.app.Instrumentation
import android.content.Intent
import android.view.View
import android.view.ViewGroup
import android.view.inputmethod.EditorInfo
import android.widget.EditText
import android.widget.TextView
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class MainScreenInstrumentedTest {
    @Test
    fun panelKeepsReferenceRatioAndDelayValidationAndSupportDestination() {
        val instrumentation = InstrumentationRegistry.getInstrumentation()
        val activity = instrumentation.startActivitySync(
            Intent(instrumentation.targetContext, MainActivity::class.java).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK),
        ) as MainActivity
        instrumentation.waitForIdleSync()
        val savedDelay = ClickSettings.getDelaySeconds(activity)
        var destination: String? = null
        val monitor = object : Instrumentation.ActivityMonitor() {
            override fun onStartActivity(intent: Intent): Instrumentation.ActivityResult? {
                if (intent.action != Intent.ACTION_VIEW) return null
                destination = intent.dataString
                return Instrumentation.ActivityResult(Activity.RESULT_OK, null)
            }
        }
        instrumentation.addMonitor(monitor)
        try {
            instrumentation.runOnMainSync {
                val views = descendants(activity.window.decorView)
                val panel = views.filterIsInstance<DelayPanel>().single()
                assertEquals(1.77f, panel.width.toFloat() / panel.height, .03f)
                val input = views.filterIsInstance<EditText>().single()
                input.setText("61")
                input.onEditorAction(EditorInfo.IME_ACTION_DONE)
                assertEquals(savedDelay, ClickSettings.getDelaySeconds(activity), 0f)
                assertTrue(views.filterIsInstance<TextView>().any {
                    it.visibility == View.VISIBLE && it.text.toString() == activity.getString(
                        R.string.seconds_range, ClickSettings.MIN_DELAY_SECONDS.toDouble(), ClickSettings.MAX_DELAY_SECONDS.toDouble(),
                    )
                })
                input.setText("5.0")
                input.onEditorAction(EditorInfo.IME_ACTION_DONE)
                assertEquals(5f, ClickSettings.getDelaySeconds(activity), 0f)
                views.single { it.isClickable && it.contentDescription == activity.getString(R.string.support_development) }.performClick()
                assertEquals("https://yihongcho.bobaboba.me", destination)
            }
        } finally {
            instrumentation.removeMonitor(monitor)
            instrumentation.runOnMainSync {
                ClickSettings.saveDelaySeconds(activity, savedDelay)
                activity.finish()
            }
        }
    }

    private fun descendants(view: View): List<View> = listOf(view) +
        if (view is ViewGroup) (0 until view.childCount).flatMap { descendants(view.getChildAt(it)) } else emptyList()
}

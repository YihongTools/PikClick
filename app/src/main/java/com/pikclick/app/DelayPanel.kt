package com.pikclick.app

import android.content.Context
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.drawable.GradientDrawable
import android.widget.FrameLayout
import kotlin.math.cos
import kotlin.math.max
import kotlin.math.roundToInt
import kotlin.math.sin

/** The scale is decorative; the editable number remains the only time control. */
class DelayPanel(context: Context) : FrameLayout(context) {
    private val tickPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        strokeWidth = context.dp(2).toFloat()
        strokeCap = Paint.Cap.ROUND
    }

    init {
        setWillNotDraw(false)
        background = GradientDrawable().apply {
            setColor(context.getColor(R.color.timer_surface))
            cornerRadius = context.dp(12).toFloat()
        }
    }

    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        // Large text can grow beyond the reference ratio rather than clip content.
        super.onMeasure(widthMeasureSpec, MeasureSpec.makeMeasureSpec(0, MeasureSpec.UNSPECIFIED))
        val height = max(measuredHeight, (measuredWidth / 1.77f).roundToInt())
        super.onMeasure(widthMeasureSpec, MeasureSpec.makeMeasureSpec(height, MeasureSpec.EXACTLY))
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        val radius = minOf(width * .40f, height - context.dp(30).toFloat())
        val centerX = width / 2f
        val centerY = radius + context.dp(20)
        for (index in 0..30) {
            val angle = Math.toRadians(180.0 + index * 6)
            val major = index % 5 == 0
            val length = context.dp(if (major) 12 else 8)
            tickPaint.color = context.getColor(when {
                index == 0 || index == 30 -> R.color.timer_yellow
                major -> R.color.brand_primary
                else -> R.color.timer_tick
            })
            canvas.drawLine(
                centerX + (cos(angle) * radius).toFloat(),
                centerY + (sin(angle) * radius).toFloat(),
                centerX + (cos(angle) * (radius - length)).toFloat(),
                centerY + (sin(angle) * (radius - length)).toFloat(), tickPaint,
            )
        }
    }
}

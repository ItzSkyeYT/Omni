package uk.akane.omni.ui.components

import android.content.Context
import android.content.res.Configuration
import android.graphics.Canvas
import android.graphics.Paint
import android.util.AttributeSet
import android.util.TypedValue
import android.view.View
import uk.akane.omni.logic.dpToPx
import com.google.android.material.color.MaterialColors
import uk.akane.omni.R

class RulerViewInch @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : View(context, attrs, defStyleAttr) {

    private val paintText = Paint().apply {
        color = MaterialColors.getColor(this@RulerViewInch, com.google.android.material.R.attr.colorOutline)
        strokeWidth = 2f.dpToPx(context)
        textSize = TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_SP, 20f, resources.displayMetrics)
        typeface = resources.getFont(R.font.hgm)
        isAntiAlias = true
    }

    private val paintMain = Paint().apply {
        color = MaterialColors.getColor(this@RulerViewInch, com.google.android.material.R.attr.colorOutline)
        strokeWidth = 2f.dpToPx(context)
        isAntiAlias = true
    }

    private val paintSide = Paint().apply {
        color = MaterialColors.getColor(this@RulerViewInch, com.google.android.material.R.attr.colorOutline)
        alpha = 127
        strokeWidth = 2f.dpToPx(context)
        isAntiAlias = true
    }

    // Calculate 1 inch in pixels
    private val inchToPx: Float = TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_IN, 1f, resources.displayMetrics)
    private val inchInterval: Float = inchToPx / 10f // 1 inch is divided into 10 intervals
    private val inchTextInterval: Int = 10 // Show text for every 10 intervals (1 inch)
    private val topPadding: Float = 24f.dpToPx(context)

    /** Mirrors [RulerView]: landscape measures along the long edge with upright numbers. */
    private val isHorizontal: Boolean
        get() = resources.configuration.orientation == Configuration.ORIENTATION_LANDSCAPE

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)

        val width = width.toFloat()
        val height = height.toFloat()
        val axisLength = if (isHorizontal) width else height
        val depth = if (isHorizontal) height else width

        val numInches = (axisLength - topPadding) / inchToPx
        val numIntervals = numInches * 10

        val longLineLength = depth * 0.53f
        val midLineLength = depth * 0.43f
        val shortLineLength = depth * 0.34f

        for (i in 0..numIntervals.toInt()) {
            val pos = topPadding + i * inchInterval
            val tick = { length: Float, paint: Paint ->
                if (isHorizontal) canvas.drawLine(pos, 0f, pos, length, paint)
                else canvas.drawLine(0f, pos, length, pos, paint)
            }
            when {
                i % inchTextInterval == 0 -> {
                    // Draw longer lines and numbers for every inch
                    tick(longLineLength, paintMain)
                    val text = (i / inchTextInterval).toString()
                    val textWidth = paintText.measureText(text)
                    val textHeight = paintText.descent() - paintText.ascent()
                    paintText.color = MaterialColors.getColor(this@RulerViewInch,
                        if ((i / inchTextInterval) % 12 == 0)
                            com.google.android.material.R.attr.colorOnSurface
                        else
                            com.google.android.material.R.attr.colorOutline
                    )
                    if (isHorizontal) {
                        canvas.drawText(text, pos - textWidth / 2,
                            (depth - longLineLength) / 2 + longLineLength + textHeight / 3, paintText)
                    } else {
                        canvas.drawText(text,
                            (depth - longLineLength) / 2 + longLineLength - textWidth / 2,
                            pos + textHeight / 3, paintText)
                    }
                }
                // Draw medium lines for every 5 intervals (0.5 inch)
                i % 5 == 0 -> tick(midLineLength, paintSide)
                // Draw shorter lines for other intervals
                else -> tick(shortLineLength, paintSide)
            }
        }
    }
}

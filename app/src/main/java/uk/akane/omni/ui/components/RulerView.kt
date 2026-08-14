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

class RulerView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : View(context, attrs, defStyleAttr) {

    private val paintText = Paint().apply {
        color = MaterialColors.getColor(this@RulerView, com.google.android.material.R.attr.colorOutline)
        strokeWidth = 2f.dpToPx(context)
        textSize = TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_SP, 20f, resources.displayMetrics)
        typeface = resources.getFont(R.font.hgm)
        isAntiAlias = true
    }

    private val paintMain = Paint().apply {
        color = MaterialColors.getColor(this@RulerView, com.google.android.material.R.attr.colorOutline)
        strokeWidth = 2f.dpToPx(context)
        isAntiAlias = true
    }

    private val paintSide = Paint().apply {
        color = MaterialColors.getColor(this@RulerView, com.google.android.material.R.attr.colorOutline)
        alpha = 127
        strokeWidth = 2f.dpToPx(context)
        isAntiAlias = true
    }

    // Calculate 1mm in pixels
    private val mmToPx: Float = TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_MM, 1f, resources.displayMetrics)
    private val topPadding: Float = 24f.dpToPx(context)

    /**
     * Landscape measures along the long edge, so the same ruler is drawn with its axes swapped:
     * ticks rise from the bottom edge and the numbers sit upright above them rather than beside.
     */
    private val isHorizontal: Boolean
        get() = resources.configuration.orientation == Configuration.ORIENTATION_LANDSCAPE

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)

        val width = width.toFloat()
        val height = height.toFloat()
        val axisLength = if (isHorizontal) width else height
        val depth = if (isHorizontal) height else width

        val numDivisions = ((axisLength - topPadding) / mmToPx).toInt()
        val longLineLength = depth * 0.53f
        val midLineLength = depth * 0.43f
        val shortLineLength = depth * 0.34f

        for (i in 0..numDivisions) {
            val pos = topPadding + i * mmToPx
            val tick = { length: Float, paint: Paint ->
                if (isHorizontal) canvas.drawLine(pos, depth - length, pos, depth, paint)
                else canvas.drawLine(depth - length, pos, depth, pos, paint)
            }
            when {
                i % 10 == 0 -> {
                    // Draw longer lines and numbers for every 10mm (1cm)
                    tick(longLineLength, paintMain)
                    val text = (i / 10).toString()
                    val textWidth = paintText.measureText(text)
                    val textHeight = paintText.descent() - paintText.ascent()
                    paintText.color = MaterialColors.getColor(this@RulerView,
                        if ((i / 10) % 5 == 0)
                            com.google.android.material.R.attr.colorOnSurface
                        else
                            com.google.android.material.R.attr.colorOutline
                    )
                    if (isHorizontal) {
                        canvas.drawText(text, pos - textWidth / 2,
                            (depth - longLineLength) / 2 + textHeight / 3, paintText)
                    } else {
                        canvas.drawText(text, (depth - longLineLength) / 2 - textWidth / 2,
                            pos + textHeight / 3, paintText)
                    }
                }
                // Draw medium lines for every 5mm (0.5cm)
                i % 5 == 0 -> tick(midLineLength, paintSide)
                // Draw shorter lines for other millimeters
                else -> tick(shortLineLength, paintSide)
            }
        }
    }
}

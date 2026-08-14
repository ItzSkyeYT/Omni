package uk.akane.omni.ui.components

import android.content.Context
import android.graphics.*
import android.os.SystemClock
import android.util.AttributeSet
import android.util.TypedValue
import android.view.View
import androidx.core.content.res.ResourcesCompat
import uk.akane.omni.R
import uk.akane.omni.logic.dpToPx
import com.google.android.material.color.MaterialColors
import kotlin.math.absoluteValue
import kotlin.math.exp
import kotlin.math.max
import kotlin.math.min
import kotlin.math.pow
import kotlin.math.sqrt

class SpiritLevelView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : View(context, attrs, defStyleAttr) {

    private var pitch: Float = 0f
    private var roll: Float = 0f
    private var balance: Float = 0f
    private var pitchAngle: Float = 0f

    private val containerPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.FILL
    }

    private val levelPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.FILL
    }

    private val textPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        textSize = TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_SP, 42f, resources.displayMetrics)
        textAlign = Paint.Align.CENTER
        typeface = resources.getFont(R.font.hgm)
    }

    private val outerLevelPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.FILL
        xfermode = PorterDuffXfermode(PorterDuff.Mode.SRC_IN)
    }

    private var colorPrimary: Int = 0
    private var colorOnPrimary: Int = 0
    private var colorTertiary: Int = 0
    private var colorOnTertiary: Int = 0
    private var colorSurface: Int = 0
    private var colorOnSurface: Int = 0
    private var colorPrimaryContainer: Int = 0
    private var colorOutline: Int = 0

    private val leftPolygon = ResourcesCompat.getDrawable(resources, R.drawable.ic_polygon_left, null)!!
    private val rightPolygon = ResourcesCompat.getDrawable(resources, R.drawable.ic_polygon_right, null)!!
    private val polygonHeight = 51.dpToPx(context)
    private val polygonWidth = 50.dpToPx(context)
    private val roundCorner = 16f.dpToPx(context)
    private val markEdgeGap = 24.dpToPx(context)

    init {
        colorPrimary = MaterialColors.getColor(this, com.google.android.material.R.attr.colorPrimary)
        colorOnPrimary = MaterialColors.getColor(this, com.google.android.material.R.attr.colorOnPrimary)
        colorTertiary = MaterialColors.getColor(this, com.google.android.material.R.attr.colorTertiary)
        colorOnTertiary = MaterialColors.getColor(this, com.google.android.material.R.attr.colorOnTertiary)
        colorSurface = MaterialColors.getColor(this, com.google.android.material.R.attr.colorSurface)
        colorOnSurface = MaterialColors.getColor(this, com.google.android.material.R.attr.colorOnSurface)
        colorPrimaryContainer = MaterialColors.getColor(this, com.google.android.material.R.attr.colorPrimaryContainer)
        colorOutline = MaterialColors.getColor(this, com.google.android.material.R.attr.colorOutline)
        containerPaint.color = colorSurface
        textPaint.color = colorOnSurface
    }

    private var levelRadius: Float = 0f
    private var translationRange: Float = 0f
    private var directionalLength: Float = 0f

    /**
     * How far the phone has to tilt before the bubble leaves the screen entirely and the vertical
     * slab takes over. Derived from the geometry rather than captured on the fly: the previous
     * version recorded the tilt at the moment of crossing and measured from there, which went
     * stale as soon as you came back to flat, so the second crossing snapped straight to the end.
     */
    private var verticalThresholdAngle: Float = 90f

    /** 0 = bubble, 1 = fully vertical. Eased and smoothed rather than jumping between modes. */
    private var transform: Float = 0f

    /**
     * Which way up the phone is being held, snapped to a quarter turn. The marks are the tool
     * body and must not follow the tilt, but they do have to follow the GRIP: held in landscape,
     * a plumb surface should still read zero. The original got this free from the OS rotating the
     * whole UI and remapping the sensor by the same amount; with the screen pinned that has to be
     * supplied here instead. Quantised, so it never absorbs any part of the tilt being measured.
     */
    private var gripAngle: Float = 0f
    private var displayedGripAngle: Float = 0f

    /**
     * 0 when held upright, 1 when held on its side. Everything drawn in the grip frame has to
     * swap the screen's extents with it: at a quarter turn the viewer's horizontal is the device's
     * height, so a block sized by the device width covers barely half the view and the marks sit
     * far inside the edges. Eased alongside the rotation so the resize glides with it.
     */
    private var gripLandscapeness: Float = 0f

    private val gripHorizontal: Float
        get() = width + (height - width) * gripLandscapeness
    private val gripVertical: Float
        get() = height + (width - height) * gripLandscapeness
    private var hasReading: Boolean = false
    private var lastUpdateNanos: Long = 0L

    override fun onSizeChanged(w: Int, h: Int, oldw: Int, oldh: Int) {
        super.onSizeChanged(w, h, oldw, oldh)
        // Sized off the shorter edge, not the width: keying it to the width alone made the bubble
        // more than twice as large in landscape as in portrait. The shorter edge is the same
        // number in both orientations, so the instrument keeps one size whichever way it is held.
        levelRadius = min(w, h) / 2 * 0.36f
        translationRange = max(w, h).toFloat()
        directionalLength = sqrt((w.toFloat().pow(2)) + (h.toFloat().pow(2))) / 2
        // The bubble sits translationRange * (tilt / 90) from the centre, so it clears the screen
        // at exactly this tilt. Solving for it here keeps the handover in one place.
        verticalThresholdAngle = if (translationRange > 0f) {
            90f * (directionalLength + levelRadius) / translationRange
        } else 90f
        // Mark bounds are set per frame in drawVerticalLayer: their spacing depends on which way
        // the phone is being held, though never on the tilt being measured.
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)

        // Calculates indicator center location.
        val cx = width / 2f
        val cy = height / 2f

        // Calculates horizontal indicator position.
        val levelCx = cx + (roll / 90) * translationRange
        val levelCy = cy - (pitch / 90) * translationRange

        // Smoothstep so the handover eases in and out instead of ramping linearly.
        val eased = transform * transform * (3f - 2f * transform)

        // One definition of "level" for the whole screen. Flat on a surface it means pitch and
        // roll are both near zero; stood on edge it means plumb, so the readout angle sits at 0
        // or 180. Previously the block used the flat test even when vertical, so on edge it never
        // reached the level colour while the arrows beside it already had, and the two disagreed.
        val isLevel = if (eased > 0.5f) {
            // Measured against the grip: the gap between the water line and the marks is the
            // reading, and both of those now share the same quarter-turn reference.
            degreesFrom(balance, gripAngle) <= LEVEL_TOLERANCE
        } else {
            pitch.absoluteValue <= LEVEL_TOLERANCE && roll.absoluteValue <= LEVEL_TOLERANCE
        }

        // Flat on a surface the disc itself signals level. On edge the block is the water and
        // stays its own colour, and the marks carry the signal instead.
        if (isLevel && eased <= 0.5f) {
            levelPaint.color = colorTertiary
            outerLevelPaint.color = colorOnTertiary
        } else {
            levelPaint.color = colorPrimary
            outerLevelPaint.color = colorOnPrimary
        }
        // Set before the container is drawn: the old code assigned this afterwards, so the
        // circle always rendered one frame behind the transition and never came back at all
        // once the slab had fully taken over.
        containerPaint.alpha = ((1f - eased) * 255).toInt().coerceIn(0, 255)

        drawHorizontalIndicators(cx, cy, levelCx, levelCy, canvas)
        drawVerticalLayer(canvas, balance, eased, isLevel)

        val saveCount = canvas.saveLayer(null, null)

        drawCenteredText(cx, cy, canvas, balance)
        drawInvertedTextLayer(levelCx, levelCy, canvas)
        drawRoundedRect(canvas, eased, balance, outerLevelPaint)

        canvas.restoreToCount(saveCount)
    }

    private fun drawHorizontalIndicators(
        cx: Float,
        cy: Float,
        levelCx: Float,
        levelCy: Float,
        canvas: Canvas
    ) {
        // Draws container radius.
        val bigCircleRadius = sqrt(
            (((cy - levelCy).absoluteValue).pow(2) + ((cx - levelCx).absoluteValue).pow(2))
        ) + levelRadius

        // Draws indicators
        canvas.drawCircle(cx, cy, bigCircleRadius, containerPaint)
        canvas.drawCircle(levelCx, levelCy, levelRadius, levelPaint)
    }

    private fun drawVerticalLayer(
        canvas: Canvas,
        angle: Float,
        eased: Float,
        isLevel: Boolean
    ) {
        if (eased <= 0.001f) return
        // Level is the one moment the marks matter most, and it is also the moment the block's
        // edge arrives underneath them, so they must not take the block's own colour or they
        // vanish into it. A grey reads against the block and against the background alike.
        val tint = if (isLevel) colorOutline else colorPrimaryContainer
        leftPolygon.setTint(tint)
        rightPolygon.setTint(tint)
        val alpha = (eased * 255).toInt().coerceIn(0, 255)
        leftPolygon.alpha = alpha
        rightPolygon.alpha = alpha
        drawRoundedRect(canvas, eased, angle, levelPaint)

        // Turned by the grip only, never by the tilt. The block's edge still sweeps across them
        // and that sweep is the reading; a reference that followed the thing it measures would
        // always read zero. Spacing follows the viewer's width so they sit near the edges however
        // the phone is held, with a margin so they never touch it.
        val cx = width / 2f
        val cy = height / 2f
        val markOffset = gripHorizontal / 2f - markEdgeGap - polygonWidth
        // Flat edge of each mark rests on the reference line rather than straddling it, so the
        // block's edge meets them cleanly at plumb instead of clipping through their middles.
        val markTop = (cy - polygonHeight).toInt()
        leftPolygon.setBounds(
            (cx - markOffset - polygonWidth).toInt(), markTop,
            (cx - markOffset).toInt(), markTop + polygonHeight
        )
        rightPolygon.setBounds(
            (cx + markOffset).toInt(), markTop,
            (cx + markOffset + polygonWidth).toInt(), markTop + polygonHeight
        )
        canvas.save()
        canvas.rotate(displayedGripAngle, cx, cy)
        leftPolygon.draw(canvas)
        rightPolygon.draw(canvas)
        canvas.restore()
    }

    private fun drawRoundedRect(
        canvas: Canvas,
        transformValue: Float,
        angle: Float,
        paint: Paint
    ) {
        val cx = width / 2f
        val cy = height / 2f
        // Sized by the viewer's extents, not the device's, so it fills the screen the same way
        // whichever way the phone is held. The top edge still lands exactly on the centre line at
        // full transform, which is the line the marks sit on.
        val halfWide = gripHorizontal / 2f
        val fillTop = cy + (gripVertical / 2f) * (1f - min(transformValue, 1f))
        canvas.save()
        canvas.rotate(angle, cx, cy)
        canvas.drawRoundRect(
            cx - halfWide,
            fillTop,
            cx + halfWide,
            cy + gripVertical,
            roundCorner,
            roundCorner,
            paint
        )
        canvas.restore()
    }

    private fun drawCenteredText(
        cx: Float,
        cy: Float,
        canvas: Canvas,
        textAngle: Float
    ) {
        canvas.save()
        // Follows gravity exactly, at every angle and in both modes. Snapping it to the grip
        // made it upright only at four orientations and jump between them; this tracks the true
        // screen-plane gravity direction, so it is upright continuously as the phone turns.
        canvas.rotate(textAngle, cx, cy)
        // On edge, report the distance from plumb. Reading the wrapped 0..360 angle directly
        // would print 359 one degree from level; taking the nearer of 0 and 180 matches the
        // isLevel test exactly, so the number hits zero as the marks change colour.
        val text =
            if (pitchAngle < verticalThresholdAngle)
                " ${pitchAngle.toInt().absoluteValue}°"
            else
                " ${degreesFrom(balance, gripAngle).toInt()}°"
        canvas.drawText(text, cx, cy + (textPaint.textSize / 4), textPaint)
        canvas.restore()
    }

    private fun drawInvertedTextLayer(
        levelCx: Float,
        levelCy: Float,
        canvas: Canvas
    ) {
        // Draws an overlay layer for the inverted text color.
        canvas.drawCircle(levelCx, levelCy, levelRadius, outerLevelPaint)
    }

    /**
     * The rotation vector arrives at the fastest sensor rate and is noisy, so it is low-passed
     * before it reaches the drawing code. The coefficient is derived from elapsed time rather
     * than applied per sample, otherwise the amount of smoothing would depend on how fast the
     * particular device happens to deliver events.
     */
    fun updatePitchAndRollAndBalance(pitch: Float, roll: Float, balance: Float) {
        // Passed straight through: the caller now derives this with atan2, so it is already
        // correct through a full turn and needs none of the old quadrant patching.
        val targetBalance = balance

        if (!hasReading) {
            hasReading = true
            this.pitch = pitch
            this.roll = roll
            this.balance = targetBalance
            lastUpdateNanos = SystemClock.elapsedRealtimeNanos()
        } else {
            val now = SystemClock.elapsedRealtimeNanos()
            val dt = ((now - lastUpdateNanos) / 1_000_000_000f).coerceIn(0f, 0.1f)
            lastUpdateNanos = now
            val angleAlpha = 1f - exp(-dt / ANGLE_TIME_CONSTANT)
            val transformAlpha = 1f - exp(-dt / TRANSFORM_TIME_CONSTANT)

            this.pitch += (pitch - this.pitch) * angleAlpha
            this.roll += (roll - this.roll) * angleAlpha
            // Balance is an angle: step the short way round so 359 to 1 does not sweep backwards.
            this.balance = wrapDegrees(this.balance + shortestDelta(this.balance, targetBalance) * angleAlpha)

            val target = if (verticalThresholdAngle <= 0f) 0f else
                ((this.pitchAngle - verticalThresholdAngle) / TRANSITION_DEGREES).coerceIn(0f, 1f)
            transform += (target - transform) * transformAlpha

            // Lying flat, world-up is perpendicular to the screen and this angle is pure noise,
            // so the grip holds its last value. Otherwise re-snap only once the phone is
            // decisively into the next quarter turn, or it would flicker when held near 45.
            if (this.pitchAngle > GRIP_MIN_TILT &&
                degreesFrom(this.balance, gripAngle) > GRIP_SWITCH_ANGLE) {
                gripAngle = wrapDegrees(Math.round(this.balance / 90f) * 90f)
            }
            displayedGripAngle = wrapDegrees(
                displayedGripAngle + shortestDelta(displayedGripAngle, gripAngle) * transformAlpha
            )
            val sideways = if (gripAngle == 90f || gripAngle == 270f) 1f else 0f
            gripLandscapeness += (sideways - gripLandscapeness) * transformAlpha
        }

        this.pitchAngle = sqrt(this.pitch.absoluteValue.pow(2) + this.roll.absoluteValue.pow(2))
        invalidate()
    }

    private fun shortestDelta(from: Float, to: Float): Float {
        var delta = (to - from) % 360f
        if (delta > 180f) delta -= 360f
        if (delta < -180f) delta += 360f
        return delta
    }

    private fun wrapDegrees(value: Float): Float = ((value % 360f) + 360f) % 360f

    /** Absolute angular distance, so a reading of 359 counts as one degree from zero. */
    private fun degreesFrom(value: Float, target: Float): Float =
        shortestDelta(target, value).absoluteValue

    companion object {
        /** Seconds for a step change to cover ~63% of the distance. Low enough to stay responsive. */
        private const val ANGLE_TIME_CONSTANT = 0.06f
        /** The mode handover is deliberately lazier than the needle so it cannot flicker. */
        private const val TRANSFORM_TIME_CONSTANT = 0.12f
        /** Degrees of tilt past the handover point over which the slab fully takes over. */
        private const val TRANSITION_DEGREES = 5f
        /** Degrees either side of true within which the instrument reads as level. */
        private const val LEVEL_TOLERANCE = 2f
        /** Past this far from the current quarter turn, the phone counts as held the other way. */
        private const val GRIP_SWITCH_ANGLE = 55f
        /** Below this tilt the screen-plane gravity direction is noise, so the grip is held. */
        private const val GRIP_MIN_TILT = 20f
    }
}

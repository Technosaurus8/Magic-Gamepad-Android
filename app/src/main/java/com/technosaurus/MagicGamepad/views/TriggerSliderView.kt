package com.technosaurus.MagicGamepad.views

import android.animation.ValueAnimator
import android.content.Context
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.RectF
import android.util.AttributeSet
import android.view.MotionEvent
import android.view.View
import android.view.animation.DecelerateInterpolator
import androidx.annotation.ColorInt
import com.technosaurus.MagicGamepad.R
import androidx.core.content.withStyledAttributes

class TriggerSliderView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : View(context, attrs, defStyleAttr) {

    var onTriggerChanged: ((value: Int) -> Unit)? = null

    private val trackPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = 0xFF2A2A2A.toInt()
        style = Paint.Style.FILL
    }
    private val fillPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = 0xFF4DA6FF.toInt()
        style = Paint.Style.FILL
    }

    var fillColor: Int
        get() = fillPaint.color
        set(@ColorInt value) { fillPaint.color = value; invalidate() }

    var trackColor: Int
        get() = trackPaint.color
        set(@ColorInt value) { trackPaint.color = value; invalidate() }

    init {
        attrs?.let {
            context.withStyledAttributes(it, R.styleable.TriggerSliderView) {
                fillColor = getColor(R.styleable.TriggerSliderView_fillColor, 0xFF4DA6FF.toInt())
                trackColor = getColor(R.styleable.TriggerSliderView_trackColor, 0xFF2A2A2A.toInt())
            }
        }
    }

    private val trackRect = RectF()
    private val fillRect = RectF()

    private var currentPct = 0f
    private var dragging = false
    private var dragStartY = 0f
    private var dragStartPct = 0f
    private var resetAnimator: ValueAnimator? = null

    val value get() = (currentPct * 255f).toInt().coerceIn(0, 255)

    override fun onDraw(canvas: Canvas) {
        val w = width.toFloat()
        val h = height.toFloat()
        val cornerR = dp(CORNER_R_DP)

        trackRect.set(0f, 0f, w, h)
        canvas.drawRoundRect(trackRect, cornerR, cornerR, trackPaint)

        fillRect.set(0f, h - currentPct * h, w, h)
        canvas.drawRoundRect(fillRect, cornerR, cornerR, fillPaint)
    }

    override fun onTouchEvent(event: MotionEvent): Boolean {
        val travel = height.toFloat()
        return when (event.action) {
            MotionEvent.ACTION_DOWN -> {
                cancelReset()
                dragging = true
                val tappedPct = (travel - event.y) / travel
                setPct(tappedPct)
                dragStartY = event.y
                dragStartPct = currentPct
                parent.requestDisallowInterceptTouchEvent(true)
                true
            }
            MotionEvent.ACTION_MOVE -> {
                if (!dragging) return false
                val delta = (dragStartY - event.y) / travel
                setPct(dragStartPct + delta)
                true
            }
            MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> {
                dragging = false
                parent.requestDisallowInterceptTouchEvent(false)
                animateReset()
                true
            }
            else -> super.onTouchEvent(event)
        }
    }

    private fun setPct(pct: Float) {
        currentPct = pct.coerceIn(0f, 1f)
        onTriggerChanged?.invoke(value)
        invalidate()
    }

    private fun animateReset() {
        resetAnimator = ValueAnimator.ofFloat(currentPct, 0f).apply {
            duration = 120
            interpolator = DecelerateInterpolator(2f)
            addUpdateListener { setPct(it.animatedValue as Float) }
            start()
        }
    }

    private fun cancelReset() {
        resetAnimator?.cancel()
    }

    private fun dp(dp: Float) = dp * resources.displayMetrics.density

    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        setMeasuredDimension(
            resolveSize(dp(TRACK_W_DP).toInt(), widthMeasureSpec),
            resolveSize(dp(200f).toInt(), heightMeasureSpec)
        )
    }

    companion object {
        private const val TRACK_W_DP = 64f
        private const val CORNER_R_DP = 32f
    }
}
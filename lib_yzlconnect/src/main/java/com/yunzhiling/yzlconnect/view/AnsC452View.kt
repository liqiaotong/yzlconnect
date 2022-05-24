package com.yunzhiling.yzlconnect.view

import android.animation.ValueAnimator
import android.content.Context
import android.graphics.*
import android.util.AttributeSet
import android.view.View
import android.view.animation.LinearInterpolator
import com.yunzhiling.yzlconnect.R
import kotlinx.android.synthetic.main.activity_ans_web.view.*
import kotlin.math.abs

class AnsC452View : View {

    private var defaultColor: Int = Color.parseColor("#FFFFFF")
    private var mPaint: Paint? = null
    private var mRectF: RectF? = null
    private var progress: Float = 0f
    private var duration: Long = 3000L
    private var pointNumber: Int = 3
    private var valueAnimator: ValueAnimator? = null
    private var interpolator = LinearInterpolator()

    constructor(context: Context, attrs: AttributeSet?, defStyleAttr: Int) : super(
        context,
        attrs,
        defStyleAttr
    ) {
        initResource(context, attrs, defStyleAttr)
    }

    constructor(context: Context) : this(context, null)
    constructor(context: Context, attrs: AttributeSet?) : this(context, attrs, -1)

    private fun initResource(context: Context?, attrs: AttributeSet?, defStyleAttr: Int) {
        context?.let {
            var a = context.obtainStyledAttributes(attrs, R.styleable.AnsC452View, defStyleAttr, 0)
            defaultColor = a.getColor(R.styleable.AnsC452View_ax_color, defaultColor)
            pointNumber = a.getColor(R.styleable.AnsC452View_ax_point_number, 3)
            duration = a.getFloat(R.styleable.AnsC452View_ax_duration, 3000f).toLong()
            a.recycle()
        }
        invalidate()
    }

    init {
        initPaint()
    }

    private fun initPaint() {
        mPaint = Paint()
        mPaint?.isAntiAlias = true
        mPaint?.color = defaultColor
    }


    fun startRun(duration: Long? = null) {
        startAnimation(duration)
    }

    fun endRun() {
        valueAnimator?.cancel()
        valueAnimator?.end()
    }

    private fun startAnimation(duration: Long? = null) {
        valueAnimator?.cancel()
        valueAnimator = ValueAnimator.ofFloat(0f, 1f)
        valueAnimator?.duration = duration ?: this.duration
        valueAnimator?.addUpdateListener {
            progress = it.animatedValue as Float
            invalidate()
        }
        valueAnimator?.repeatMode = ValueAnimator.RESTART
        valueAnimator?.repeatCount = ValueAnimator.INFINITE
        valueAnimator?.interpolator = interpolator
        valueAnimator?.start()
    }

    override fun onSizeChanged(width: Int, height: Int, oldWidth: Int, oldHeight: Int) {
        super.onSizeChanged(width, height, oldWidth, oldHeight)
        updateRectF(width, height)
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        if (mRectF == null) updateRectF()
        val centerY = mRectF?.centerY() ?: (height * 0.5f)
        val nWidth = mRectF?.right ?: width.toFloat()
        val rWidth = (nWidth * 0.15f) * 0.5f
        val space = nWidth / (pointNumber + 1)
        for (i in 1..(pointNumber)) {
            mPaint?.let {
                val pspace = 1f/(pointNumber+1)
                val vas =1f-((abs(pspace*i-progress)?.let { its-> if(its>pspace) pspace else its })/pspace)
                var offset = (155 * vas).toInt()
                if (offset < 0) offset = 0
                it.alpha = 100 + offset
                canvas?.drawCircle(space * i, centerY, rWidth, it)
            }
        }
    }

    private fun updateRectF(width: Int? = null, height: Int? = null) {
        val vWidth = width ?: this.width
        val vHeight = height ?: this.height
        mRectF = RectF(0f, 0f, vWidth?.toFloat(), vHeight?.toFloat())
    }

}
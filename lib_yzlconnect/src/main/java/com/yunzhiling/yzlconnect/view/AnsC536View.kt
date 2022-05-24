package com.yunzhiling.yzlconnect.view

import android.animation.ValueAnimator
import android.content.Context
import android.graphics.*
import android.util.AttributeSet
import android.view.View
import android.view.animation.LinearInterpolator
import com.yunzhiling.yzlconnect.R

class AnsC536View : View {

    private var defaultColor: Int = Color.parseColor("#54CB69")
    private var mPaint: Paint? = null
    private var startAngle: Float? = 0f
    private var endAngle: Float? = 90f
    private var mRectF: RectF? = null
    private var progress: Float = 0f
    private var duration: Long = 2000L
    private var valueAnimator: ValueAnimator? = null
    private var interpolator = LinearInterpolator()
    private var progressPaint: Paint? = null

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
            var a = context.obtainStyledAttributes(attrs, R.styleable.AnsC536View, defStyleAttr, 0)
            defaultColor = a.getColor(R.styleable.AnsC536View_ac_color, defaultColor)
            startAngle = a.getFloat(R.styleable.AnsC536View_ac_start_angle, 0f)
            endAngle = a.getFloat(R.styleable.AnsC536View_ac_end_angle, 90f)
            duration = a.getFloat(R.styleable.AnsC536View_ac_duration, 2000f).toLong()
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

        progressPaint = Paint()
        progressPaint?.isAntiAlias = true
        progressPaint?.style = Paint.Style.STROKE
        progressPaint?.strokeCap = Paint.Cap.ROUND
        progressPaint?.color = defaultColor
    }


    fun startRotate(duration: Long? = null) {
        startAnimation(duration)
    }

    fun endRotate() {
        valueAnimator?.cancel()
        valueAnimator?.end()
    }

    private fun startAnimation(duration: Long? = null) {
        valueAnimator?.cancel()
        valueAnimator = ValueAnimator.ofFloat(progress, progress+360f)
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
        val centerX = mRectF?.centerX() ?: (width * 0.5f)
        val centerY = mRectF?.centerY() ?: (height * 0.5f)
        val nWidth = mRectF?.right ?: width.toFloat()
        val nHeight = mRectF?.bottom ?: height.toFloat()
        val radius = if (nWidth > nHeight) nHeight * 0.5f else nWidth * 0.5f

        canvas?.rotate(progress,centerX,centerY)

        mPaint?.let {
            canvas?.drawCircle(centerX, centerY, radius * 0.88f, it)
        }
        //画进度条
        progressPaint?.let {
            //设置画笔宽度
            val strokeWidth = radius * 0.05f
            val strokeWidthR = strokeWidth * 0.5f
            progressPaint?.strokeWidth = strokeWidth
            val oval = RectF() //RectF对象
            oval.left = centerX - radius + strokeWidthR
            oval.top = centerY - radius + strokeWidthR
            oval.right = centerX + radius - strokeWidthR
            oval.bottom = centerY + radius - strokeWidthR
            canvas?.drawArc(oval, startAngle?:0f, endAngle?:90f, false, it)
        }
    }

    private fun updateRectF(width: Int? = null, height: Int? = null) {
        val vWidth = width ?: this.width
        val vHeight = height ?: this.height
        mRectF = RectF(0f, 0f, vWidth?.toFloat(), vHeight?.toFloat())
    }

}
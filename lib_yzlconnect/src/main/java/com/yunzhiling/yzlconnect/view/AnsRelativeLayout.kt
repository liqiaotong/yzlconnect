package com.yunzhiling.yzlconnect.view

import android.animation.ValueAnimator
import android.content.Context
import android.util.AttributeSet
import android.view.MotionEvent
import android.view.animation.DecelerateInterpolator
import android.widget.RelativeLayout
import com.yunzhiling.yzlconnect.R
import kotlin.math.abs

open class AnsRelativeLayout : RelativeLayout {

    private var isClickDown: Boolean = false
    private var ocl: OnClickListener? = null

    //按钮动画
    private var scaleStart: Float = 1f
    private var scaleEnd: Float = 0.95f
    private var progressStart: Float = 1f
    private var progressEnd: Float = 1f
    private var progress = 1f
    private var duration: Long = 130
    private var scale: ValueAnimator? = null

    private var pressDownX: Float = 0f
    private var pressDownY: Float = 0f
    private var scrollX: Float = 0f
    private var scrollY: Float = 0f
    private val cancelPressMaxScroll: Float = 10f

    init {
        setOnTouchListener { _, _ -> true }
    }

    constructor(context: Context?, attrs: AttributeSet?, defStyleAttr: Int) : super(
        context,
        attrs,
        defStyleAttr
    ) {
        initResource(context, attrs, defStyleAttr)
    }

    constructor(context: Context?) : this(context, null)
    constructor(context: Context?, attrs: AttributeSet?) : this(context, attrs, -1)

    private fun initResource(context: Context?, attrs: AttributeSet?, defStyleAttr: Int) {
        context?.let {
            var a =
                context.obtainStyledAttributes(attrs, R.styleable.AnView, defStyleAttr, 0)
            scaleStart = a.getFloat(R.styleable.AnView_av_scaleStart, 1f)
            scaleEnd = a.getFloat(R.styleable.AnView_av_scaleEnd, 0.95f)
            duration = a.getInt(R.styleable.AnView_av_duration, 130).toLong()
            a.recycle()
        }
    }

    override fun dispatchTouchEvent(event: MotionEvent?): Boolean {

        if (!isEnabled) return super.dispatchTouchEvent(event)

        when (event?.action) {
            MotionEvent.ACTION_MOVE -> {
                scrollX = event?.rawX - pressDownX
                scrollY = event?.rawY - pressDownY
                if (abs(scrollX) > cancelPressMaxScroll || abs(scrollY) > cancelPressMaxScroll) {
                    setClick(false)
                }
            }
            MotionEvent.ACTION_DOWN -> {
                pressDownX = event?.rawX
                pressDownY = event?.rawY
                scrollX = 0f
                scrollY = 0f
                setClick(true)
            }
            MotionEvent.ACTION_UP -> {
                if (abs(scrollX) > cancelPressMaxScroll || abs(scrollY) > cancelPressMaxScroll) {
                    setClick(false)
                } else {
                    setClick(isAnimationClick = false, isEventClick = true)
                }
            }
            MotionEvent.ACTION_CANCEL -> {
                setClick(false)
            }
        }
        return super.dispatchTouchEvent(event)
    }

    private fun setClick(isAnimationClick: Boolean, isEventClick: Boolean = false) {
        //点下状态
        if (isAnimationClick && !isClickDown) {
            isClickDown = isAnimationClick
            //开启点下动画
            startAnimation(true)
        }
        //松手状态
        else if (!isAnimationClick && isClickDown) {
            isClickDown = isAnimationClick
            //开启松手动画
            startAnimation(false)
            //松手才回调事件ACTION_UP
            if (isEventClick) {
                ocl?.onClick(this)
            }

        }
    }

    override fun setOnClickListener(l: OnClickListener?) {
        ocl = l
    }

    private fun startAnimation(isClick: Boolean) {

        scale?.cancel()

        if (isClick) {
            progressStart = progress
            progressEnd = scaleEnd
        } else {
            progressStart = progress
            progressEnd = scaleStart
        }

        var hs = if (isClick) {
            var s = scaleStart - scaleEnd
            var ps = progress - scaleEnd
            ps / s
        } else {
            var s = scaleStart - scaleEnd
            var ps = scaleStart - progress
            ps / s
        }

        scale = ValueAnimator.ofFloat(progressStart, progressEnd)
        scale?.interpolator = DecelerateInterpolator()
        scale?.addUpdateListener {
            var sx = it?.animatedValue as Float
            progress = sx
            //缩放按钮
            scaleX = sx
            scaleY = sx
        }
        scale?.duration = (duration * hs).toLong()
        scale?.start()

    }

}
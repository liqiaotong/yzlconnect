package com.yunzhiling.yzlconnect.view

import android.animation.ValueAnimator
import android.content.Context
import android.graphics.Color
import android.graphics.PorterDuff
import android.graphics.PorterDuffColorFilter
import android.util.AttributeSet
import android.view.animation.DecelerateInterpolator
import android.widget.FrameLayout
import android.widget.ProgressBar
import android.widget.TextView
import com.yunzhiling.yzlconnect.R


class AnsConfirmButton : FrameLayout {

    private var layout: AnsRelativeLayout? = null
    private var progressBar: ProgressBar? = null
    private var tips: TextView? = null
    private var enable: Boolean = true

    constructor(context: Context) : super(context) {
        initView()
    }

    constructor(context: Context, attrs: AttributeSet?) : super(context, attrs) {
        initView()
    }

    private fun initView() {
        inflate(context, R.layout.layout_ans_confirm_button, this)
        layout = findViewById(R.id.layout)
        progressBar = findViewById(R.id.progressBar)
        tips = findViewById(R.id.tips)
    }

    override fun setOnClickListener(l: OnClickListener?) {
        if (enable) {
            layout?.setOnClickListener(l)
        }
    }

    fun setTips(tips: String? = "确定") {
        this.tips?.text = tips
    }

    fun getTips():String {
       return tips?.text?.toString()?:""
    }

    fun setLoading(isLoading: Boolean, isAnimation: Boolean? = true) {
        selectedAnimation(isLoading,isAnimation)
    }

    fun setTextColor(color: String) {
        this.tips?.setTextColor(Color.parseColor(color))
    }

    fun setProgressBarColor(color: String) {
        progressBar?.indeterminateDrawable?.colorFilter = PorterDuffColorFilter(Color.parseColor(color), PorterDuff.Mode.SRC_ATOP)
    }

    override fun setBackgroundResource(resid: Int) {
        layout?.setBackgroundResource(resid)
    }

    override fun setEnabled(isEnaenablebled: Boolean) {
        this.enable = enable
        layout?.isEnabled = enable
    }

    fun isLoading():Boolean{
        return isStatusLoading
    }

    fun end() {
        animator?.end()
        isStatusLoading = false
    }

    private var duration = 200L
    private var isStatusLoading: Boolean = false
    private var animator: ValueAnimator? = null

    private fun selectedAnimation(isSelect: Boolean, isAnimation: Boolean? = true) {

        if (isStatusLoading == isSelect) return

        isStatusLoading = isSelect

        if (isAnimation == false) {
            changeStyle(-1f)
            return
        }

        if (animator?.isRunning == true) return

        animator?.cancel()

        animator = ValueAnimator.ofFloat(0f, 1f)
        animator?.interpolator = DecelerateInterpolator()
        animator?.addUpdateListener {
            val progress = it?.animatedFraction as Float
            changeStyle(progress)
            if (progress == 1f) {
                if (isStatusLoading != isSelect) {
                    selectedAnimation(isStatusLoading)
                }
            }
        }

        animator?.duration = duration
        animator?.start()
    }

    private fun changeStyle(progress: Float) {
        if (progress < 0) {
            if (isStatusLoading) {
                progressBar?.alpha = 1f
                tips?.alpha = 0f
            } else {
                progressBar?.alpha = 0f
                tips?.alpha = 1f
            }
        } else {
            if (isStatusLoading) {
                progressBar?.alpha = progress
                tips?.alpha = 1f - progress
            } else {
                progressBar?.alpha = 1 - progress
                tips?.alpha = progress
            }
        }
    }

}
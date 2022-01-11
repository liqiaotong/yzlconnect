package com.yunzhiling.yzlconnect.view

import android.content.Context
import android.util.AttributeSet
import android.view.MotionEvent
import androidx.viewpager.widget.ViewPager


class AnsScrollViewPager : ViewPager {

    private var isCanScroll = false

    constructor(context: Context?) : super(context!!)
    constructor(context: Context?, attrs: AttributeSet?) : super(context!!, attrs)

    fun setCanScroll(isCanScroll: Boolean) {
        this.isCanScroll = isCanScroll
    }

    override fun onTouchEvent(motionEvent: MotionEvent): Boolean {
        return if (isCanScroll) super.onTouchEvent(motionEvent) else false
    }

    override fun onInterceptTouchEvent(motionEvent: MotionEvent): Boolean {
        return if (isCanScroll) super.onInterceptTouchEvent(motionEvent) else false
    }

}
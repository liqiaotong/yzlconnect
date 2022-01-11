package com.yunzhiling.yzlconnect.adapter

import android.view.View
import android.view.ViewGroup
import androidx.viewpager.widget.PagerAdapter

class AnsViewPagerAdapter(views:MutableList<View>) : PagerAdapter() {

    private var views:MutableList<View> = views

    override fun getCount(): Int {
        return views.size
    }

    override fun isViewFromObject(view: View, any: Any): Boolean {
        return view === any
    }

    override fun instantiateItem(container: ViewGroup, position: Int): Any {
        container.addView(views[position])
        return views[position]
    }

    override fun destroyItem(container: ViewGroup, position: Int, `object`: Any) {
        container.removeView(views[position])
    }

}
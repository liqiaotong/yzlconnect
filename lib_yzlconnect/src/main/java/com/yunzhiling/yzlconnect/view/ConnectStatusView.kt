package com.yunzhiling.yzlconnect.view

import android.app.Activity
import android.content.Intent
import android.graphics.Color
import android.text.SpannableStringBuilder
import android.text.Spanned
import android.text.style.ForegroundColorSpan
import android.view.View
import android.widget.FrameLayout
import com.yunzhiling.yzlconnect.R
import com.yunzhiling.yzlconnect.activity.AnsWebActivity
import com.yunzhiling.yzlconnect.common.AnsConfig
import kotlinx.android.synthetic.main.layout_connect_status.view.*


class ConnectStatusView : FrameLayout {

    private var isViewShow = false
    private var activity: Activity? = null
    private var listener: OnConnectStatusListener? = null

    fun setListener(listener: OnConnectStatusListener?) {
        this.listener = listener
    }

    constructor(activity: Activity) : super(activity) {
        this.activity = activity
        inflate(activity, R.layout.layout_connect_status, this)
    }

    fun viewShow(isShow: Boolean) {
        if (isShow) {
            isViewShow = true
        } else {
            if (isViewShow) {
                isViewShow = false

            }
        }
    }

    fun setStatus(status: Int) {
        when (status) {
            0 -> {
                image?.setImageResource(R.mipmap.connect_success)
                tips?.text = "配网成功"
                //successDetailLayout?.visibility = View.VISIBLE
                detail?.visibility = View.VISIBLE
                val tipsContent = "设备播报\"联网成功\"，且右灯常亮蓝灯"
                val spannableString = SpannableStringBuilder(tipsContent)
                spannableString.setSpan(ForegroundColorSpan(Color.parseColor("#0A7FFF")), 4, tipsContent.length, Spanned.SPAN_EXCLUSIVE_INCLUSIVE)
                detail?.text = spannableString
                more?.visibility = View.GONE
                confirm?.setTips("完成")
                confirm?.setOnClickListener {
                    listener?.complete()
                }
            }
            1 -> {
                image?.setImageResource(R.mipmap.connect_fail)
                tips?.text = "配网失败"
                //successDetailLayout?.visibility = View.GONE
                detail?.visibility = View.VISIBLE
                detail?.text = "1、确认设备处于配网模式（三灯闪烁）\n2、核对WiFi密码是否正确\n3、确认WiFi是否为2.4G或混频频段"
                more?.visibility = View.VISIBLE
                confirm?.setTips("重新配网")
                confirm?.setOnClickListener {
                    listener?.complete(true)
                }
            }
            2 -> {
                image?.setImageResource(R.mipmap.connect_timeout)
                tips?.text = "配网超时"
                //successDetailLayout?.visibility = View.GONE
                detail?.visibility = View.VISIBLE
                val spannableString = SpannableStringBuilder("若设备 播报\"联网成功\"且右灯常亮蓝灯，表示设备已配网，可退出配置\n\n若设备 右灯不亮或闪烁，则需要重新配置 \n1、核对WiFi密码是否正确 \n2、确认WiFi是否为2.4G或混频频段")
                spannableString.setSpan(ForegroundColorSpan(Color.parseColor("#0A7FFF")), 4, 19, Spanned.SPAN_EXCLUSIVE_INCLUSIVE)
                spannableString.setSpan(ForegroundColorSpan(Color.parseColor("#0A7FFF")), 38, 46, Spanned.SPAN_EXCLUSIVE_INCLUSIVE)
                detail?.text = spannableString
                more?.visibility = View.VISIBLE
                confirm?.setTips("重新配网")
                confirm?.setOnClickListener {
                    listener?.complete(true)
                }
            }
        }

        more?.setOnClickListener {
            val intent = Intent(context, AnsWebActivity::class.java)
            intent.putExtra("title", "WiFi配置失败问题排查")
            intent.putExtra("url", AnsConfig.moreHelpUrl)
            context.startActivity(intent)
        }

    }

}

interface OnConnectStatusListener {
    fun complete(connectAgain: Boolean? = false)
}

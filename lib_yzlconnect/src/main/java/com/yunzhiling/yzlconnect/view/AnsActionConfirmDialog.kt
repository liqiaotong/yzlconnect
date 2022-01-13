package com.yunzhiling.yzlconnect.view

import android.app.Dialog
import android.content.Context
import android.os.Bundle
import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.view.WindowManager
import android.widget.TextView
import com.yunzhiling.yzlconnect.R

class ActionConfirmDialog : Dialog {

    private var view: View? = null
    private var confirm: AnsConfirmButton? = null
    private var title: TextView? = null
    private var content: TextView? = null
    private var isLoading: Boolean = false
    private var listener: OnActionConfirmDialogListener? = null
    private var isSystemDialog:Boolean? = false
    private var isCancelOnTouch:Boolean? = true

    constructor(context: Context,isSystemDialog:Boolean? = false,isCancelOnTouch:Boolean? = true) : super(context) {
        this.isSystemDialog = isSystemDialog
        this.isCancelOnTouch = isCancelOnTouch
        initView()
    }

    fun setContent(content: String?) {
        content?.let { this.content?.text = it }
    }

    fun setTitle(title: String?) {
        title?.let { this.title?.text = it }
    }

    fun setListener(listener: OnActionConfirmDialogListener?) {
        this.listener = listener
    }

    private fun initView() {
        view = context?.let{LayoutInflater.from(it).inflate(R.layout.dialog_ans_action_confirm, null)}
        title = view?.findViewById(R.id.title)
        content = view?.findViewById(R.id.content)
        confirm = view?.findViewById(R.id.confirm)
        confirm?.setOnClickListener {
            if(!isLoading) {
                listener?.confirm(this)
            }
        }
        view?.findViewById<AnsButton>(R.id.cancel)?.setOnClickListener {
            dismiss()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        view?.let { setContentView(it) }
        initDialog()
    }

    fun setLoading(isLoading: Boolean, isAnimation: Boolean? = true) {
        this.isLoading = isLoading
        confirm?.setLoading(isLoading,isAnimation)
    }

    private fun initDialog() {
        if(isSystemDialog == true) {
            window?.setType(WindowManager.LayoutParams.TYPE_SYSTEM_DIALOG)
        }
        if(isCancelOnTouch == true) {
            setCancelable(true)
            setCanceledOnTouchOutside(true)
        }else{
            setCancelable(!isLoading)
            setCanceledOnTouchOutside(!isLoading)
        }
        window?.setGravity(Gravity.CENTER)
        window?.setBackgroundDrawableResource(android.R.color.transparent)
        window?.decorView?.setPadding(0, 0, 0, 0)
    }

    override fun dismiss(){
        setLoading(isLoading = false, isAnimation = false)
        super.dismiss()
    }

}

interface OnActionConfirmDialogListener {
    fun confirm(dialog: ActionConfirmDialog)
}
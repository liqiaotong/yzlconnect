package com.yunzhiling.yzlconnect.activity

import android.content.Intent
import android.graphics.Color
import android.os.Bundle
import android.text.TextUtils
import android.view.KeyEvent
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.tencent.mmkv.MMKV
import com.yunzhiling.yzlconnect.R
import com.yunzhiling.yzlconnect.service.WifiManager
import kotlinx.android.synthetic.main.activity_connect.*
import kotlinx.android.synthetic.main.activity_mode_select.*
import kotlinx.android.synthetic.main.activity_mode_select.back
import kotlin.system.exitProcess

class ConnectModeSelectActivity : CommonActivtiy() {

    private var isAppMode = true

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_mode_select)
        initView()
    }

    private fun initView() {
        isAppMode = intent.getBooleanExtra("isAppMode", true)
        back?.visibility = if (isAppMode) View.GONE else View.VISIBLE
        back?.setOnClickListener { finish() }
        idea?.setColorFilter(Color.parseColor("#a6a6a6"))
        autoConnect?.setOnClickListener {
            val intent = Intent(this, ConnectActivity::class.java)
            intent.putExtra("isAutoConnectMode", true)
            startActivity(intent)
        }
        manualConnect?.setOnClickListener {
            startActivity(Intent(this, ConnectActivity::class.java))
        }
    }

    fun setVersion(versionText:String?){
        if(!TextUtils.isEmpty(versionText)){
            version?.text = versionText
            version?.visibility = View.VISIBLE
        }else{
            version?.visibility = View.GONE
        }
    }

    private var exitTime: Long = 0

    override fun onKeyDown(keyCode: Int, event: KeyEvent): Boolean {
        if (keyCode == KeyEvent.KEYCODE_BACK
            && event.action === KeyEvent.ACTION_DOWN
        ) {
            if(isAppMode) {
                if (System.currentTimeMillis() - exitTime > 2000) {
                    Toast.makeText(
                        this, "再次返回退出",
                        Toast.LENGTH_SHORT
                    ).show()
                    exitTime = System.currentTimeMillis()
                } else {
                    finish()
                    exitProcess(0)
                }
            }else{
                finish()
            }
            return true
        }
        return super.onKeyDown(keyCode, event)
    }

}
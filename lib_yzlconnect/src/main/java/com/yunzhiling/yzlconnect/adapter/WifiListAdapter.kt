package com.yunzhiling.yzlconnect.adapter

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.yunzhiling.yzlconnect.R
import com.yunzhiling.yzlconnect.entity.WifiEntity
import com.yunzhiling.yzlconnect.view.AnButton
import com.yunzhiling.yzlconnect.view.AnLinearLayout

class WifiListAdapter : RecyclerView.Adapter<RecyclerView.ViewHolder> {

    private var context: Context? = null
    private var list: MutableList<WifiEntity>? = null

    private lateinit var listener: OnWifiListAdapterListener

    fun setOnWifiListAdapterListener(onWifiListAdapterListener: OnWifiListAdapterListener) {
        this.listener = onWifiListAdapterListener
    }

    constructor(context: Context) {
        this.context = context
    }

    fun updateData(list: MutableList<WifiEntity>?) {
        this.list?.clear()
        if (this.list == null) this.list = ArrayList()
        list?.let {
            this.list?.addAll(it)
        }
        notifyDataSetChanged()
    }

    fun getData(): MutableList<WifiEntity>? {
        return list
    }

    fun cleanData() {
        list = null
        notifyDataSetChanged()
    }

    override fun getItemViewType(position: Int): Int {
        return if (list.isNullOrEmpty()) {
            -1
        } else {
            0
        }
    }

    override fun getItemCount(): Int {
        var size = list?.size ?: 0
        if (size <= 0) size = 1
        return size
    }

    class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        var name: TextView = view.findViewById(R.id.wifiName)
        var frequency: TextView = view.findViewById(R.id.frequency)
        var item: AnLinearLayout = view.findViewById(R.id.wifiItem)
        var wifiLevel: ImageView = view.findViewById(R.id.wifiLevel)
        var line: View = view.findViewById(R.id.line)
    }

    class EmptyViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        var retry: AnButton = view.findViewById(R.id.retry_tv)
    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {

        if (holder is ViewHolder) {

            var wifi = this.list?.get(position)
            holder.name.text = wifi?.ssid
            when {
                wifi?.isMix == true -> {
                    holder.frequency.text = "2.4G/5G"
                }
                wifi?.is5G == true -> {
                    holder.frequency.text = "5G"
                }
                wifi?.is2G == true -> {
                    holder.frequency.text = "2.4G"
                }
                else -> {
                    holder.frequency.text = ""
                }
            }
            holder.item.setOnClickListener {
                this.list?.get(position)?.let { its ->
                    listener.onItemClick(its)
                }
            }

            val wifiLevelResource = when {
                wifi?.level ?: 0 >= 80 -> {
                    R.mipmap.icon_wifi_leve_3
                }
                wifi?.level ?: 0 >= 40 -> {
                    R.mipmap.icon_wifi_leve_2
                }
                wifi?.level ?: 0 >= 15 -> {
                    R.mipmap.icon_wifi_leve_1
                }
                else -> {
                    R.mipmap.icon_wifi_leve_0
                }
            }

            holder.wifiLevel.setImageResource(wifiLevelResource)

            if ((position + 1) == (list?.size ?: 0)) {
                holder.line.visibility = View.INVISIBLE
            } else {
                holder.line.visibility = View.VISIBLE
            }
        } else if (holder is EmptyViewHolder) {
            holder.retry.setOnClickListener {
                listener?.onSearchAgain()
            }
        }

    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        return if (viewType == 0) {
            ViewHolder(LayoutInflater.from(context).inflate(R.layout.layout_wifi_list_item_yc, null))
        } else {
            EmptyViewHolder(LayoutInflater.from(context).inflate(R.layout.layout_wifi_list_empty, parent, false))
        }
    }

}

interface OnWifiListAdapterListener {
    fun onItemClick(wifiInfo: WifiEntity?)
    fun onSearchAgain()
}
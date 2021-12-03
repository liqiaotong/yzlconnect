package com.yunzhiling.yzlconnect.entity

class WifiEntity {
    var ssid: String? = null
    var frequency: Int? = null
    var level: Int? = null
    var is2G: Boolean? = null
    var is5G: Boolean? = null
    var isMix: Boolean? = null
        get() = (is2G == true) && (is5G == true)

    constructor()

    constructor(ssid: String?, frequency: Int?, level: Int?, is2G: Boolean? = null, is5G: Boolean? = null) {
        this.ssid = ssid
        this.frequency = frequency
        this.level = level
        this.is2G = is2G
        this.is5G = is5G
    }
}
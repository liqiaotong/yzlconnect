package com.yunzhiling.yzlconnect.utils

import android.content.SharedPreferences
import com.tencent.mmkv.MMKV

object SharedPreferenceUtils {

        private val preferences: SharedPreferences by lazy {
            MMKV.defaultMMKV()
        }

        fun getString(key:String):String{
           return preferences.getString(key,"")?:""
        }

        fun setString(key:String,value:String):Boolean{
            return preferences.edit().putString(key,value).commit()
        }

        fun getBoolean(key:String):Boolean{
            return preferences.getBoolean(key,false)
        }

        fun setBoolean(key:String,value:Boolean):Boolean{
            return preferences.edit().putBoolean(key,value).commit()
        }

        fun getInt(key:String):Int{
            return preferences.getInt(key,0)
        }

        fun setInt(key:String,value:Int):Boolean{
            return preferences.edit().putInt(key,value).commit()
        }

        fun getFloat(key:String):Float{
            return preferences.getFloat(key,0.0f)
        }

        fun setFloat(key:String,value:Float):Boolean{
            return preferences.edit().putFloat(key,value).commit()
        }

        fun getLong(key:String):Long{
            return preferences?.getLong(key,0L)
        }

        fun setLong(key:String,value:Long):Boolean{
            return preferences.edit().putLong(key,value).commit()
        }

        fun getStringSet(key:String):Set<String>{
            return preferences?.getStringSet(key, HashSet<String>())?:HashSet()
        }

        fun setStringSet(key:String,value:Set<String>):Boolean{
            return preferences.edit().putStringSet(key,value).commit()
        }

}
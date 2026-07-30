package com.diu.yk_games.line2box.util

import android.content.Context
import android.content.SharedPreferences
import android.util.Base64
import androidx.core.content.edit
import com.diu.yk_games.line2box.Line2BoxApplication
import com.diu.yk_games.line2box.R
import com.google.gson.Gson


val pref by lazy { Line2BoxApplication.instance.preference }

object PrefKeys {
    const val SETTINGS = "SETTINGS_KEY"
    const val MUTED = "muted"
    const val FIRST_RUN = "firstRun"
    const val SHOW_HADITH = "showHadith"
}

@Suppress("unused")
class SharedPreferenceUtils(applicationContext: Context) {
    val preferences: SharedPreferences by lazy {
        applicationContext.getSharedPreferences(applicationContext.getString(R.string.preference_file_key), Context.MODE_PRIVATE)
    }
    val gson by lazy { Gson() }

    inline fun <reified T> read(key: String, defValue: T): T {
        return when (T::class) {
            String::class -> preferences.getString(key, defValue as String?) as T
            Int::class -> preferences.getInt(key, defValue as Int) as T
            Long::class -> preferences.getLong(key, defValue as Long) as T
            Double::class -> preferences.getFloat(key, (defValue as Double).toFloat()).toDouble() as T
            Float::class -> preferences.getFloat(key, defValue as Float) as T
            Boolean::class -> preferences.getBoolean(key, defValue as Boolean) as T
            else -> tryGet {
                preferences.getString(key, null)?.let {
                    gson.fromJson(it, T::class.java)
                }
            } ?: defValue
        }
    }

    inline fun <reified T : Any> save(key: String, value: T) {
        preferences.edit {
            when (T::class) {
                String::class -> putString(key, value as String)
                Int::class -> putInt(key, value as Int)
                Long::class -> putLong(key, value as Long)
                Double::class -> putFloat(key, (value as Double).toFloat())
                Float::class -> putFloat(key, value as Float)
                Boolean::class -> putBoolean(key, value as Boolean)
                else -> putString(key, gson.toJson(value))
            }
        }
    }

    inline fun <reified T : Any> clear(vararg restore: Pair<String, T>) {
        clearAll()
        restore.forEach {
            save(it.first, it.second)
        }
    }

    fun remove(key: String) {
        preferences.edit { remove(key) }
    }

    fun clearAll() {
        preferences.edit { clear() }
    }

    fun encode(plain: String): String {
        val b64encoded = Base64.encodeToString(plain.toByteArray(), Base64.DEFAULT)

        // Reverse the string
        val reverse = StringBuffer(b64encoded).reverse().toString()

        val tmp = kotlin.text.StringBuilder()
        val offset = 4
        for (element in reverse) {
            tmp.append((element.code + offset).toChar())
        }
        return tmp.toString()
    }

    fun decode(secret: String): String {
        val tmp = kotlin.text.StringBuilder()
        val offset = 4
        for (element in secret) {
            tmp.append((element.code - offset).toChar())
        }

        val reversed = StringBuffer(tmp.toString()).reverse().toString()
        return String(Base64.decode(reversed, Base64.DEFAULT))
    }
}

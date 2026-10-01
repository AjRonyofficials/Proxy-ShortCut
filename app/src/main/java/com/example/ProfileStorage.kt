package com.example

import android.content.Context
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken

object ProfileStorage {
    private const val PREF_NAME = "proxy_pref"
    private const val KEY_PROFILES = "profiles_list"
    private const val KEY_ACTIVE_ID = "active_profile_id"
    private val gson = Gson()

    fun saveProfiles(context: Context, profiles: List<ProxyProfile>) {
        val json = gson.toJson(profiles)
        context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE).edit()
            .putString(KEY_PROFILES, json).apply()
    }

    fun getProfiles(context: Context): ArrayList<ProxyProfile> {
        val json = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE).getString(KEY_PROFILES, null)
        return if (json != null) {
            val type = object : TypeToken<ArrayList<ProxyProfile>>() {}.type
            gson.fromJson(json, type)
        } else {
            ArrayList()
        }
    }

    fun setActiveProfileId(context: Context, id: String) {
        context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE).edit()
            .putString(KEY_ACTIVE_ID, id).apply()
    }

    fun getActiveProfile(context: Context): ProxyProfile? {
        val id = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE).getString(KEY_ACTIVE_ID, null)
        val list = getProfiles(context)
        return list.find { it.id == id } ?: list.firstOrNull()
    }
}

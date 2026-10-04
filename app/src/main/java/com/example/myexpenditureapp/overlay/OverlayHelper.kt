package com.example.myexpenditureapp.overlay

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.provider.Settings

object OverlayHelper {
    private const val PREFS_NAME = "expenditure_overlay_prefs"
    private const val KEY_INSTANT_OVERLAY_ENABLED = "instant_overlay_enabled"

    fun canDrawOverlays(context: Context): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            Settings.canDrawOverlays(context)
        } else {
            true
        }
    }

    fun openOverlayPermissionSettings(context: Context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            val intent = Intent(
                Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                Uri.parse("package:${context.packageName}")
            )
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            context.startActivity(intent)
        }
    }

    fun isInstantOverlayEnabled(context: Context): Boolean {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        return prefs.getBoolean(KEY_INSTANT_OVERLAY_ENABLED, true)
    }

    fun setInstantOverlayEnabled(context: Context, enabled: Boolean) {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        prefs.edit().putBoolean(KEY_INSTANT_OVERLAY_ENABLED, enabled).apply()
    }

    fun getOverlaySettingDefault(): Boolean = true

    fun shouldLaunchOverlay(canDrawOverlays: Boolean, isUserSettingEnabled: Boolean): Boolean {
        return canDrawOverlays && isUserSettingEnabled
    }
}

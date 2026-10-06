package com.gravassist.actions

import android.content.Context
import android.content.Intent
import com.gravassist.services.LoggerService

object AppLaunchAction {
    private val PACKAGE_MAP = mapOf(
        "whatsapp" to "com.whatsapp",
        "youtube" to "com.google.android.youtube",
        "chrome" to "com.android.chrome",
        "maps" to "com.google.android.apps.maps",
        "gmail" to "com.google.android.gm",
        "settings" to "com.android.settings"
    )

    fun launchApp(context: Context, appName: String): Boolean {
        val cleanName = appName.lowercase().trim()
        val packageName = PACKAGE_MAP[cleanName] ?: cleanName

        return try {
            val intent = context.packageManager.getLaunchIntentForPackage(packageName)
            if (intent != null) {
                intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                context.startActivity(intent)
                LoggerService.log("AppLauncher", "Opened app: $appName")
                true
            } else {
                LoggerService.log("AppLauncher", "App not found: $appName", "FAILED")
                false
            }
        } catch (e: Exception) {
            LoggerService.log("AppLauncher", "Failed launching app $appName: ${e.localizedMessage}", "FAILED")
            false
        }
    }
}

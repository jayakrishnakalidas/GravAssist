package com.gravassist.actions

import android.content.Context
import android.content.Intent
import android.provider.AlarmClock
import com.gravassist.services.LoggerService

object AlarmAction {
    fun setAlarm(context: Context, hour: Int, minute: Int, label: String): Boolean {
        return try {
            val intent = Intent(AlarmClock.ACTION_SET_ALARM).apply {
                putExtra(AlarmClock.EXTRA_HOUR, hour)
                putExtra(AlarmClock.EXTRA_MINUTES, minute)
                putExtra(AlarmClock.EXTRA_MESSAGE, label)
                putExtra(AlarmClock.EXTRA_SKIP_UI, true)
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(intent)
            LoggerService.log("AlarmAction", "Set alarm for $hour:$minute ($label)")
            true
        } catch (e: Exception) {
            LoggerService.log("AlarmAction", "Failed setting alarm: ${e.localizedMessage}", "FAILED")
            false
        }
    }

    fun setTimer(context: Context, seconds: Int, label: String): Boolean {
        return try {
            val intent = Intent(AlarmClock.ACTION_SET_TIMER).apply {
                putExtra(AlarmClock.EXTRA_LENGTH, seconds)
                putExtra(AlarmClock.EXTRA_MESSAGE, label)
                putExtra(AlarmClock.EXTRA_SKIP_UI, true)
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(intent)
            LoggerService.log("AlarmAction", "Set timer for ${seconds}s ($label)")
            true
        } catch (e: Exception) {
            LoggerService.log("AlarmAction", "Failed setting timer: ${e.localizedMessage}", "FAILED")
            false
        }
    }
}

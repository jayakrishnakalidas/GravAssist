package com.gravassist.actions

import android.content.Context
import android.media.AudioManager
import com.gravassist.services.LoggerService

object VolumeAction {
    fun adjustVolume(context: Context, level: String): Boolean {
        return try {
            val audioManager = context.getSystemService(Context.AUDIO_SERVICE) as AudioManager
            when (level.lowercase()) {
                "up" -> audioManager.adjustStreamVolume(AudioManager.STREAM_MUSIC, AudioManager.ADJUST_RAISE, AudioManager.FLAG_SHOW_UI)
                "down" -> audioManager.adjustStreamVolume(AudioManager.STREAM_MUSIC, AudioManager.ADJUST_LOWER, AudioManager.FLAG_SHOW_UI)
                "mute" -> audioManager.adjustStreamVolume(AudioManager.STREAM_MUSIC, AudioManager.ADJUST_MUTE, AudioManager.FLAG_SHOW_UI)
                "max" -> {
                    val max = audioManager.getStreamMaxVolume(AudioManager.STREAM_MUSIC)
                    audioManager.setStreamVolume(AudioManager.STREAM_MUSIC, max, AudioManager.FLAG_SHOW_UI)
                }
            }
            LoggerService.log("VolumeAction", "Volume adjusted ($level)")
            true
        } catch (e: Exception) {
            LoggerService.log("VolumeAction", "Volume adjust failed: ${e.localizedMessage}", "FAILED")
            false
        }
    }
}

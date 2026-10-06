package com.gravassist.actions

import android.content.Context
import android.hardware.camera2.CameraManager
import com.gravassist.services.LoggerService

object TorchAction {
    fun execute(context: Context, turnOn: Boolean): Boolean {
        return try {
            val cameraManager = context.getSystemService(Context.CAMERA_SERVICE) as CameraManager
            val cameraId = cameraManager.cameraIdList[0]
            cameraManager.setTorchMode(cameraId, turnOn)
            val stateStr = if (turnOn) "ON" else "OFF"
            LoggerService.log("TorchAction", "Flashlight turned $stateStr")
            true
        } catch (e: Exception) {
            LoggerService.log("TorchAction", "Failed torch control: ${e.localizedMessage}", "FAILED")
            false
        }
    }
}

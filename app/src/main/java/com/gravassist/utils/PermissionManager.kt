package com.gravassist.utils

import android.Manifest
import android.app.Activity
import android.content.Context
import android.content.pm.PackageManager
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat

class PermissionManager(private val context: Context) {

    companion object {
        const val PERMISSION_REQUEST_CODE = 1001

        val REQUIRED_PERMISSIONS = arrayOf(
            Manifest.permission.CAMERA,
            Manifest.permission.RECORD_AUDIO,
            Manifest.permission.CALL_PHONE,
            Manifest.permission.SEND_SMS
        )
    }

    fun hasAllPermissions(): Boolean {
        for (permission in REQUIRED_PERMISSIONS) {
            if (ContextCompat.checkSelfPermission(context, permission) != PackageManager.PERMISSION_GRANTED) {
                return false
            }
        }
        return true
    }

    fun requestPermissions(activity: Activity) {
        ActivityCompat.requestPermissions(
            activity,
            REQUIRED_PERMISSIONS,
            PERMISSION_REQUEST_CODE
        )
    }
}

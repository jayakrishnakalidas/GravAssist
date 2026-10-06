package com.gravassist.actions

import android.content.Context
import android.content.Intent
import android.net.Uri
import com.gravassist.services.LoggerService

object CallAction {
    fun makeCall(context: Context, phoneNumber: String): Boolean {
        return try {
            val intent = Intent(Intent.ACTION_CALL).apply {
                data = Uri.parse("tel:$phoneNumber")
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(intent)
            LoggerService.log("CallAction", "Initiated phone call to $phoneNumber")
            true
        } catch (e: Exception) {
            LoggerService.log("CallAction", "Failed initiating call: ${e.localizedMessage}", "FAILED")
            false
        }
    }

    fun sendSms(context: Context, phoneNumber: String, message: String): Boolean {
        return try {
            val intent = Intent(Intent.ACTION_VIEW).apply {
                data = Uri.parse("smsto:$phoneNumber")
                putExtra("sms_body", message)
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(intent)
            LoggerService.log("CallAction", "Prepared SMS to $phoneNumber")
            true
        } catch (e: Exception) {
            LoggerService.log("CallAction", "Failed preparing SMS: ${e.localizedMessage}", "FAILED")
            false
        }
    }
}

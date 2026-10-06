package com.gravassist.services

import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

data class LogItem(
    val timestamp: String,
    val target: String,
    val action: String,
    val status: String
)

object LoggerService {
    private val logs = mutableListOf<LogItem>()
    private val listeners = mutableListOf<() -> Unit>()

    fun log(target: String, action: String, status: String = "SUCCESS") {
        val sdf = SimpleDateFormat("HH:mm:ss", Locale.getDefault())
        val time = sdf.format(Date())
        val item = LogItem(time, target, action, status)
        synchronized(logs) {
            logs.add(0, item) // newest first
        }
        notifyListeners()
    }

    fun getLogs(): List<LogItem> {
        synchronized(logs) {
            return logs.toList()
        }
    }

    fun clearLogs() {
        synchronized(logs) {
            logs.clear()
        }
        notifyListeners()
    }

    fun addListener(listener: () -> Unit) {
        listeners.add(listener)
    }

    fun removeListener(listener: () -> Unit) {
        listeners.remove(listener)
    }

    private fun notifyListeners() {
        for (listener in listeners) {
            listener.invoke()
        }
    }
}

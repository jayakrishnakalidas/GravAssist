package com.gravassist.ui.logs

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.RecyclerView
import com.gravassist.R
import com.gravassist.databinding.ItemLogEntryBinding
import com.gravassist.services.LogItem

class LogAdapter(private var logList: List<LogItem> = emptyList()) :
    RecyclerView.Adapter<LogAdapter.LogViewHolder>() {

    class LogViewHolder(val binding: ItemLogEntryBinding) :
        RecyclerView.ViewHolder(binding.root)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): LogViewHolder {
        val binding = ItemLogEntryBinding.inflate(
            LayoutInflater.from(parent.context), parent, false
        )
        return LogViewHolder(binding)
    }

    override fun onBindViewHolder(holder: LogViewHolder, position: Int) {
        val item = logList[position]
        holder.binding.tvLogTimestamp.text = item.timestamp
        holder.binding.tvLogTarget.text = "Target: ${item.target}"
        holder.binding.tvLogAction.text = "Action: ${item.action}"
        holder.binding.tvLogStatus.text = item.status

        val context = holder.itemView.context
        if (item.status == "SUCCESS") {
            holder.binding.tvLogStatus.setTextColor(ContextCompat.getColor(context, R.color.status_green))
        } else {
            holder.binding.tvLogStatus.setTextColor(ContextCompat.getColor(context, R.color.status_red))
        }
    }

    override fun getItemCount(): Int = logList.size

    fun updateLogs(newLogs: List<LogItem>) {
        logList = newLogs
        notifyDataSetChanged()
    }
}

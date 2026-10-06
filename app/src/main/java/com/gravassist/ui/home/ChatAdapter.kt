package com.gravassist.ui.home

import android.view.Gravity
import android.view.LayoutInflater
import android.view.ViewGroup
import android.widget.LinearLayout
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.RecyclerView
import com.gravassist.R
import com.gravassist.databinding.ItemChatMessageBinding

data class ChatMessage(
    val sender: String, // "user" or "ai"
    val text: String,
    val timestamp: String = ""
)

class ChatAdapter(private val messages: MutableList<ChatMessage> = mutableListOf()) :
    RecyclerView.Adapter<ChatAdapter.ChatViewHolder>() {

    class ChatViewHolder(val binding: ItemChatMessageBinding) :
        RecyclerView.ViewHolder(binding.root)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ChatViewHolder {
        val binding = ItemChatMessageBinding.inflate(
            LayoutInflater.from(parent.context), parent, false
        )
        return ChatViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ChatViewHolder, position: Int) {
        val message = messages[position]
        holder.binding.tvMessageBody.text = message.text
        holder.binding.tvTimestamp.text = message.timestamp

        val context = holder.itemView.context
        val containerParams = holder.binding.tvMessageBody.layoutParams as LinearLayout.LayoutParams

        if (message.sender == "user") {
            containerParams.gravity = Gravity.END
            holder.binding.tvMessageBody.background = ContextCompat.getDrawable(context, R.color.chat_user_bg)
            holder.binding.tvMessageBody.setTextColor(ContextCompat.getColor(context, R.color.white))
        } else {
            containerParams.gravity = Gravity.START
            holder.binding.tvMessageBody.background = ContextCompat.getDrawable(context, R.color.chat_ai_bg)
            holder.binding.tvMessageBody.setTextColor(ContextCompat.getColor(context, R.color.text_primary))
        }
        holder.binding.tvMessageBody.layoutParams = containerParams
    }

    override fun getItemCount(): Int = messages.size

    fun addMessage(message: ChatMessage) {
        messages.add(message)
        notifyItemInserted(messages.size - 1)
    }
}

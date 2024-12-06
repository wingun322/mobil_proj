package com.example.bitchat.adapter;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import com.example.bitchat.R;
import com.example.bitchat.model.ChatMessage;
import java.util.List;

public class ChatAdapter extends RecyclerView.Adapter<ChatAdapter.MessageViewHolder> {
    private List<ChatMessage> messages;
    private String currentUsername;

    public ChatAdapter(List<ChatMessage> messages, String currentUsername) {
        this.messages = messages;
        this.currentUsername = currentUsername;
    }

    @NonNull
    @Override
    public MessageViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_message, parent, false);
        return new MessageViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull MessageViewHolder holder, int position) {
        ChatMessage message = messages.get(position);
        message.setIsMyMessage(message.getUsername().equals(currentUsername));

        if (message.isMyMessage()) {
            holder.myMessageLayout.setVisibility(View.VISIBLE);
            holder.otherMessageLayout.setVisibility(View.GONE);
            holder.myMessageText.setText(message.getText());
            holder.myTimestamp.setText(message.getTimestamp());
        } else {
            holder.myMessageLayout.setVisibility(View.GONE);
            holder.otherMessageLayout.setVisibility(View.VISIBLE);
            holder.otherUsername.setText(message.getUsername());
            holder.otherMessageText.setText(message.getText());
            holder.otherTimestamp.setText(message.getTimestamp());
        }
    }

    @Override
    public int getItemCount() {
        return messages.size();
    }

    public void addMessage(ChatMessage message) {
        messages.add(message);
        notifyItemInserted(messages.size() - 1);
    }

    static class MessageViewHolder extends RecyclerView.ViewHolder {
        View myMessageLayout, otherMessageLayout;
        TextView myMessageText, myTimestamp;
        TextView otherUsername, otherMessageText, otherTimestamp;

        MessageViewHolder(View itemView) {
            super(itemView);
            myMessageLayout = itemView.findViewById(R.id.myMessageLayout);
            otherMessageLayout = itemView.findViewById(R.id.otherMessageLayout);
            myMessageText = itemView.findViewById(R.id.myMessageText);
            myTimestamp = itemView.findViewById(R.id.myTimestamp);
            otherUsername = itemView.findViewById(R.id.otherUsername);
            otherMessageText = itemView.findViewById(R.id.otherMessageText);
            otherTimestamp = itemView.findViewById(R.id.otherTimestamp);
        }
    }
}
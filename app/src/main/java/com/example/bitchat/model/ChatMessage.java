package com.example.bitchat.model;

public class ChatMessage {
    private String username;
    private String text;
    private String timestamp;
    private boolean isMyMessage;

    public ChatMessage(String username, String text, String timestamp) {
        this.username = username;
        this.text = text;
        this.timestamp = timestamp;
    }

    public String getUsername() {
        return username;
    }

    public String getText() {
        return text;
    }

    public String getTimestamp() {
        return timestamp;
    }

    public boolean isMyMessage() {
        return isMyMessage;
    }

    public void setIsMyMessage(boolean isMyMessage) {
        this.isMyMessage = isMyMessage;
    }
}
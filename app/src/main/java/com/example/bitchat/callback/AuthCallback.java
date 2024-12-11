package com.example.bitchat.callback;

public interface AuthCallback<T> {
    void onSuccess(T response);
    void onError(String error);
} 
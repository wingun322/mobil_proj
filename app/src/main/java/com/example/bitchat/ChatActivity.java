package com.example.bitchat;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.util.Log;
import android.view.inputmethod.EditorInfo;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import com.android.volley.DefaultRetryPolicy;
import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.toolbox.JsonArrayRequest;
import com.android.volley.toolbox.JsonObjectRequest;
import com.android.volley.toolbox.Volley;
import com.example.bitchat.adapter.ChatAdapter;
import com.example.bitchat.model.ChatMessage;
import io.socket.client.IO;
import io.socket.client.Socket;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import java.net.URISyntaxException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.TimeZone;

public class ChatActivity extends AppCompatActivity {
    private RecyclerView messageRecyclerView;
    private EditText messageInput;
    private ImageButton sendButton;
    private ChatAdapter chatAdapter;
    private Socket socket;
    private String username;
    private String cryptoId;
    private List<ChatMessage> messages = new ArrayList<>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_chat);

        initViews();
        setupSocket();
    }

    private void initViews() {
        messageRecyclerView = findViewById(R.id.messageRecyclerView);
        messageInput = findViewById(R.id.messageInput);
        sendButton = findViewById(R.id.sendButton);

        SharedPreferences prefs = getSharedPreferences("Auth", MODE_PRIVATE);
        String token = prefs.getString("token", "");
        cryptoId = getIntent().getStringExtra("cryptoId");

        // 서버에서 사용자 정보 가져오기
        if (token != null && !token.isEmpty()) {
            String url = "http://54.206.20.147:8080/api/auth/user";
            JsonObjectRequest request = new JsonObjectRequest(Request.Method.GET, url, null,
                response -> {
                    try {
                        JSONObject user = response.getJSONObject("user");
                        username = user.getString("username");
                        
                        // SharedPreferences에 username 저장
                        prefs.edit().putString("username", username).apply();
                        
                        // 채팅 어댑터 초기화
                        messages = new ArrayList<>();
                        chatAdapter = new ChatAdapter(messages, username);
                        LinearLayoutManager layoutManager = new LinearLayoutManager(this);
                        layoutManager.setStackFromEnd(true);
                        messageRecyclerView.setLayoutManager(layoutManager);
                        messageRecyclerView.setAdapter(chatAdapter);
                        
                        // 어댑터 초기화 후 메시지 로드
                        loadMessages();
                        
                    } catch (JSONException e) {
                        Log.e("ChatActivity", "Error parsing user info: " + e.getMessage());
                        finish();
                    }
                },
                error -> {
                    Log.e("ChatActivity", "Error fetching user info: " + error.toString());
                    finish();
                }
            ) {
                @Override
                public Map<String, String> getHeaders() {
                    Map<String, String> headers = new HashMap<>();
                    headers.put("Authorization", "Bearer " + token);
                    return headers;
                }
            };

            Volley.newRequestQueue(this).add(request);
        } else {
            Toast.makeText(this, "사용자 정보를 불러올 수 없습니다.", Toast.LENGTH_SHORT).show();
            startActivity(new Intent(this, LoginActivity.class));
            finish();
        }

        sendButton.setOnClickListener(v -> sendMessage());
        messageInput.setOnEditorActionListener((v, actionId, event) -> {
            if (actionId == EditorInfo.IME_ACTION_SEND) {
                sendMessage();
                return true;
            }
            return false;
        });
    }

   private void setupSocket() {
        try {
            IO.Options opts = new IO.Options();
            opts.forceNew = true;
            opts.reconnection = true;
            opts.reconnectionAttempts = 10;
            opts.reconnectionDelay = 1000;
            socket = IO.socket("http://54.206.20.147:8080", opts);

            socket.on(Socket.EVENT_CONNECT, args -> {
                Log.d("ChatActivity", "Socket connected");
                
                // 로그인 이벤트 발생
                SharedPreferences prefs = getSharedPreferences("Auth", MODE_PRIVATE);
                String userId = prefs.getString("userId", "");
                
                try {
                    JSONObject loginData = new JSONObject();
                    loginData.put("username", username);
                    loginData.put("userId", userId);
                    socket.emit("login", loginData);
                    Log.d("ChatActivity", "Login event emitted");

                    // 방 입장
                    if (username != null && cryptoId != null) {
                        socket.emit("joinRoom", cryptoId);
                        Log.d("ChatActivity", "Joined room: " + cryptoId);
                    }
                } catch (JSONException e) {
                    Log.e("ChatActivity", "Error creating login data: " + e.getMessage());
                }
            });

            socket.on("updateUserList", args -> {
                if (args.length > 0 && args[0] != null) {
                    JSONArray users = (JSONArray) args[0];
                    runOnUiThread(() -> {
                        // TODO: 사용자 목록 UI 업데이트
                        Log.d("ChatActivity", "User list updated: " + users.toString());
                    });
                }
            });

            socket.on("message", args -> {
                if (args.length > 0 && args[0] != null) {
                    JSONObject data = (JSONObject) args[0];
                    try {
                        String messageUsername = data.getString("username");
                        String messageText = data.getString("text");
                        String messageTime = data.getString("time");
                        
                        Log.d("ChatActivity", "Received message - From: " + messageUsername + ", Text: " + messageText);
                        
                        runOnUiThread(() -> {
                            ChatMessage message = new ChatMessage(messageUsername, messageText, messageTime);
                            message.setIsMyMessage(messageUsername.equals(username));
                            messages.add(message);
                            chatAdapter.notifyItemInserted(messages.size() - 1);
                            messageRecyclerView.smoothScrollToPosition(messages.size() - 1);
                        });
                    } catch (JSONException e) {
                        Log.e("ChatActivity", "Error parsing message: " + e.getMessage());
                    }
                }
            });

            socket.on("forced_logout", args -> {
                runOnUiThread(() -> {
                    Toast.makeText(this, "다른 기기에서 로그인되어 로그아웃됩니다.", Toast.LENGTH_LONG).show();
                    // TODO: 로그아웃 처리
                });
            });

            socket.connect();
            Log.d("ChatActivity", "Socket connection initiated");

        } catch (URISyntaxException e) {
            Log.e("ChatActivity", "Socket setup error: " + e.getMessage());
        }
    }

    private void loadMessages() {
        String url = "http://54.206.20.147:8080/api/chat/messages/" + cryptoId;
        String token = getSharedPreferences("Auth", MODE_PRIVATE).getString("token", "");

        Log.d("ChatActivity", "Loading messages for room: " + cryptoId);
        Log.d("ChatActivity", "Using token: " + token);

        JsonObjectRequest request = new JsonObjectRequest(Request.Method.GET, url, null,
                response -> {
                    try {
                        messages.clear();
                        JSONArray messagesArray = response.getJSONArray("messages");
                        for (int i = 0; i < messagesArray.length(); i++) {
                            JSONObject messageObj = messagesArray.getJSONObject(i);
                            ChatMessage message = new ChatMessage(
                                    messageObj.getString("username"),
                                    messageObj.getString("text"),
                                    messageObj.getString("time")
                            );
                            message.setIsMyMessage(messageObj.getString("username").equals(username)); // 추가
                            messages.add(message);
                            Log.d("ChatActivity", "Loaded message: " + message.getText());
                        }
                        chatAdapter.notifyDataSetChanged();
                        if (!messages.isEmpty()) {
                            messageRecyclerView.scrollToPosition(messages.size() - 1);
                        }
                    } catch (JSONException e) {
                        Log.e("ChatActivity", "JSON parsing error: " + e.getMessage());
                    }
                },
                error -> {
                    String errorMessage = error.toString();
                    if (error.networkResponse != null) {
                        errorMessage += " Status Code: " + error.networkResponse.statusCode;
                    }
                    Log.e("ChatActivity", "Error loading messages: " + errorMessage);
                    Toast.makeText(this, "메시지를 불러올 수 없습니다.", Toast.LENGTH_SHORT).show();
                }) {
            @Override
            public Map<String, String> getHeaders() {
                Map<String, String> headers = new HashMap<>();
                headers.put("Authorization", "Bearer " + token);
                return headers;
            }
        };

        request.setRetryPolicy(new DefaultRetryPolicy(
            30000,
            DefaultRetryPolicy.DEFAULT_MAX_RETRIES,
            DefaultRetryPolicy.DEFAULT_BACKOFF_MULT));

        Volley.newRequestQueue(this).add(request);
    }

    private void sendMessage() {
        String text = messageInput.getText().toString().trim();
        if (text.isEmpty()) return;

        try {
            SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'", Locale.US);
            sdf.setTimeZone(TimeZone.getTimeZone("UTC"));
            String isoDate = sdf.format(new Date());

            if (username == null || username.isEmpty()) {
                username = getSharedPreferences("Auth", MODE_PRIVATE)
                        .getString("username", "");
                if (username.isEmpty()) {
                    Toast.makeText(this, "사용자 정보를 불러올 수 없습니다.", Toast.LENGTH_SHORT).show();
                    return;
                }
            }

            JSONObject messageData = new JSONObject();
            messageData.put("room", cryptoId);
            messageData.put("username", username);
            messageData.put("text", text);
            messageData.put("time", isoDate);

            if (socket != null && socket.connected()) {
                socket.emit("message", messageData);
                messageInput.setText(""); // 메시지 입력창 초기화
            } else {
                Log.e("ChatActivity", "Socket is not connected");
                Toast.makeText(this, "서버와 연결이 끊어졌습니다.", Toast.LENGTH_SHORT).show();
                setupSocket();
            }
        } catch (JSONException e) {
            Log.e("ChatActivity", "Error creating message: " + e.getMessage());
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (socket != null && !socket.connected()) {
            socket.connect();
            Log.d("ChatActivity", "Socket reconnected in onResume");
        }
    }

    @Override
    protected void onPause() {
        super.onPause();
        if (socket != null && socket.connected()) {
            socket.disconnect();
            Log.d("ChatActivity", "Socket disconnected in onPause");
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (socket != null) {
            socket.off("message");
            socket.off(Socket.EVENT_CONNECT);
            socket.off(Socket.EVENT_DISCONNECT);
            socket.off(Socket.EVENT_CONNECT_ERROR);
            socket.disconnect();
            socket = null;
            Log.d("ChatActivity", "Socket cleaned up in onDestroy");
        }
    }
}
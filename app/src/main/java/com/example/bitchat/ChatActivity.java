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
            socket = IO.socket("http://54.206.20.147:8080", opts);

            socket.on(Socket.EVENT_CONNECT, args -> {
                Log.d("ChatActivity", "Socket connected");
                runOnUiThread(() -> {
                    socket.emit("joinRoom", cryptoId);
                });
            });

            socket.on("message", args -> {
                JSONObject data = (JSONObject) args[0];
                try {
                    String messageUsername = data.getString("username");
                    String messageText = data.getString("text");
                    String messageTime = data.getString("time");
                    
                    // 내가 보낸 메시지는 이미 로컬에 추가되어 있으��로 다른 사람의 메시지만 추가
                    if (!messageUsername.equals(username)) {
                        ChatMessage message = new ChatMessage(messageUsername, messageText, messageTime);
                        runOnUiThread(() -> {
                            messages.add(message);
                            chatAdapter.notifyItemInserted(messages.size() - 1);
                            messageRecyclerView.smoothScrollToPosition(messages.size() - 1);
                        });
                    }
                } catch (JSONException e) {
                    Log.e("ChatActivity", "Error parsing message: " + e.getMessage());
                }
            });

            socket.connect();

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
                Log.d("ChatActivity", "Sending message: " + messageData.toString());
                socket.emit("message", messageData);
                
                // 메시지를 로컬에도 추가
                ChatMessage message = new ChatMessage(username, text, isoDate);
                messages.add(message);
                chatAdapter.notifyItemInserted(messages.size() - 1);
                messageRecyclerView.smoothScrollToPosition(messages.size() - 1);
                
                messageInput.setText("");  // 메시지 입력창 초기화
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
    protected void onDestroy() {
        super.onDestroy();
        if (socket != null) {
            socket.disconnect();
        }
    }
}
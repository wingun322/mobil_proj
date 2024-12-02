package com.example.bitchat;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.ImageButton;
import androidx.drawerlayout.widget.DrawerLayout;
import androidx.appcompat.app.ActionBarDrawerToggle;
import androidx.core.view.GravityCompat;

import androidx.appcompat.app.AppCompatActivity;

import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.JsonArrayRequest;
import com.android.volley.toolbox.JsonObjectRequest;
import com.android.volley.toolbox.Volley;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import android.os.Handler;
import android.os.Looper;

public class MainActivity extends AppCompatActivity {

    private TextView usernameTextView;
    private CryptoAdapter adapter;
    private Button loginButton, signupButton, profileButton, logoutButton;
    private ListView cryptoListView;
    private EditText searchEditText;
    private RequestQueue requestQueue;
    private static final long FETCH_INTERVAL = 10 * 1000; // 10초 (밀리초 단위)
    private Handler handler = new Handler(Looper.getMainLooper());
    private Runnable fetchTask;
    private ArrayList<HashMap<String, String>> originalData = new ArrayList<>(); // 원본 데이터 저장용
    private ImageButton profileMenuButton;
    private DrawerLayout drawerLayout;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // Initialize UI components
        usernameTextView = findViewById(R.id.username);
        loginButton = findViewById(R.id.login_button);
        signupButton = findViewById(R.id.signup_button);
        profileButton = findViewById(R.id.profile_button);
        logoutButton = findViewById(R.id.logout_button);
        cryptoListView = findViewById(R.id.crypto_list);
        searchEditText = findViewById(R.id.search_bar);
        profileMenuButton = findViewById(R.id.profile_menu_button);
        drawerLayout = findViewById(R.id.drawer_layout);

        // Initialize Volley request queue
        requestQueue = Volley.newRequestQueue(this);

        // Set button listeners
        logoutButton.setOnClickListener(v -> logout());

        // Check login status
        checkLoginStatus();
        adapter = new CryptoAdapter(this, new ArrayList<>());
        cryptoListView.setAdapter(adapter);

        // Fetch crypto data
        startFetchingCryptoData();

        // Set search listener
        searchEditText.addTextChangedListener(new android.text.TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                filterCryptoList(s.toString());
            }

            @Override
            public void afterTextChanged(android.text.Editable s) {}
        });

        // 프로필 메뉴 버튼 클릭 리스너
        profileMenuButton.setOnClickListener(v -> {
            if (drawerLayout.isDrawerOpen(GravityCompat.END)) {
                drawerLayout.closeDrawer(GravityCompat.END);
            } else {
                drawerLayout.openDrawer(GravityCompat.END);
            }
        });
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        stopFetchingCryptoData();
    }

    private void startFetchingCryptoData() {
        fetchTask = new Runnable() {
            @Override
            public void run() {
                fetchCryptoData();
                handler.postDelayed(this, FETCH_INTERVAL);
            }
        };
        handler.post(fetchTask);
    }

    private void stopFetchingCryptoData() {
        if (fetchTask != null) {
            handler.removeCallbacks(fetchTask);
        }
    }

    private void checkLoginStatus() {
        String token = getSharedPreferences("BitChatPrefs", MODE_PRIVATE).getString("accessToken", null);

        if (token != null) {
            String url = "http://54.206.20.147:8080/api/auth/user";

            JsonObjectRequest jsonObjectRequest = new JsonObjectRequest(Request.Method.GET, url, null,
                    response -> {
                        try {
                            JSONObject user = response.getJSONObject("user");
                            String username = user.getString("username");
                            usernameTextView.setText(username);
                            findViewById(R.id.auth_links).setVisibility(View.GONE);
                            findViewById(R.id.user_info).setVisibility(View.VISIBLE);
                            drawerLayout.closeDrawer(GravityCompat.END);
                        } catch (JSONException e) {
                            e.printStackTrace();
                        }
                    },
                    error -> {
                        Toast.makeText(this, "Failed to check login status", Toast.LENGTH_SHORT).show();
                        findViewById(R.id.auth_links).setVisibility(View.VISIBLE);
                        findViewById(R.id.user_info).setVisibility(View.GONE);
                    }) {
                @Override
                public Map<String, String> getHeaders() {
                    Map<String, String> headers = new HashMap<>();
                    headers.put("Authorization", "Bearer " + token);
                    return headers;
                }
            };

            requestQueue.add(jsonObjectRequest);
        } else {
            findViewById(R.id.auth_links).setVisibility(View.VISIBLE);
            findViewById(R.id.user_info).setVisibility(View.GONE);
        }
    }

    private void fetchCryptoData() {
        String url = "http://54.206.20.147:8080/api/market";

        JsonArrayRequest jsonArrayRequest = new JsonArrayRequest(Request.Method.GET, url, null,
                response -> {
                    ArrayList<HashMap<String, String>> cryptoData = new ArrayList<>();
                    try {
                        for (int i = 0; i < response.length(); i++) {
                            JSONObject obj = response.getJSONObject(i);
                            HashMap<String, String> map = new HashMap<>();
                            map.put("name", obj.getString("koreanName"));
                            map.put("price", obj.getString("tradePrice") + " KRW");
                            map.put("color", obj.getString("priceColor"));
                            cryptoData.add(map);
                        }
                        
                        // 원본 데이터 저장
                        originalData.clear();
                        originalData.addAll(cryptoData);
                        
                        // 검색어가 있는 경우 필터링된 결과 표시
                        String currentSearch = searchEditText.getText().toString();
                        if (!currentSearch.isEmpty()) {
                            filterCryptoList(currentSearch);
                        } else {
                            adapter.updateData(cryptoData);
                        }

                    } catch (JSONException e) {
                        e.printStackTrace();
                    }
                },
                error -> Toast.makeText(this, "Failed to fetch cryptocurrency data", Toast.LENGTH_SHORT).show());

        requestQueue.add(jsonArrayRequest);
    }

    private void filterCryptoList(String searchTerm) {
        ArrayList<HashMap<String, String>> filteredData = new ArrayList<>();
        if (searchTerm.isEmpty()) {
            filteredData.addAll(originalData);
        } else {
            for (HashMap<String, String> item : originalData) {
                if (item.get("name").toLowerCase().contains(searchTerm.toLowerCase())) {
                    filteredData.add(item);
                }
            }
        }
        adapter.updateData(filteredData);
    }

    private void logout() {
        String url = "http://54.206.20.147:8080/api/auth/logout";

        JsonObjectRequest jsonObjectRequest = new JsonObjectRequest(Request.Method.POST, url, null,
                response -> {
                    getSharedPreferences("BitChatPrefs", MODE_PRIVATE).edit().remove("accessToken").apply();
                    Toast.makeText(this, "Logged out", Toast.LENGTH_SHORT).show();
                    findViewById(R.id.auth_links).setVisibility(View.VISIBLE);
                    findViewById(R.id.user_info).setVisibility(View.GONE);
                },
                error -> Toast.makeText(this, "Failed to logout", Toast.LENGTH_SHORT).show());

        requestQueue.add(jsonObjectRequest);
    }
}
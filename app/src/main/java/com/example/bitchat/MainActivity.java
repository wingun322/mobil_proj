package com.example.bitchat;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import androidx.drawerlayout.widget.DrawerLayout;
import androidx.core.view.GravityCompat;
import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.toolbox.JsonArrayRequest;
import com.android.volley.toolbox.JsonObjectRequest;
import com.android.volley.toolbox.Volley;
import com.example.bitchat.adapter.CryptoAdapter;

import org.json.JSONException;
import org.json.JSONObject;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

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
    private ArrayList<String> favorites = new ArrayList<>();
    private boolean showOnlyFavorites = false;
    private static final int SORT_NAME = 0;
    private static final int SORT_PRICE = 1;
    private static final int SORT_CHANGE_RATE = 2;
    private static final int SORT_CHANGE_PRICE = 3;
    private int currentSortMethod = SORT_NAME;
    private boolean isAscending = true;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        initViews();

        // 로그인 상태에 따라 UI 업데이트
        if (isLoggedIn()) {
            findViewById(R.id.auth_links).setVisibility(View.GONE);
            findViewById(R.id.user_info).setVisibility(View.VISIBLE);
            String username = getSharedPreferences("Auth", MODE_PRIVATE).getString("username", "");
            usernameTextView.setText(username);
            fetchFavorites();
        } else {
            findViewById(R.id.auth_links).setVisibility(View.VISIBLE);
            findViewById(R.id.user_info).setVisibility(View.GONE);
        }

        setupFilterButton();
    }

    @Override
    protected void onResume() {
        super.onResume();
        
        // 로그인 상태에 따라 UI 업데이트
        if (isLoggedIn()) {
            findViewById(R.id.auth_links).setVisibility(View.GONE);
            findViewById(R.id.user_info).setVisibility(View.VISIBLE);
            String username = getSharedPreferences("Auth", MODE_PRIVATE).getString("username", "");
            usernameTextView.setText(username);
            fetchFavorites();
        } else {
            findViewById(R.id.auth_links).setVisibility(View.VISIBLE);
            findViewById(R.id.user_info).setVisibility(View.GONE);
        }
    }

    private void initViews() {
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

        // 암호화폐 아이템 클릭 리스너 설정
        adapter.setOnItemClickListener(cryptoId -> {
            if (!isLoggedIn()) {
                Toast.makeText(this, "채팅에 참여하려면 로그인이 필요합니다.", Toast.LENGTH_SHORT).show();
                startActivity(new Intent(MainActivity.this, LoginActivity.class));
                return;
            }

            Intent intent = new Intent(MainActivity.this, ChatActivity.class);
            intent.putExtra("cryptoId", cryptoId);
            startActivity(intent);
        });

        profileButton.setOnClickListener(v -> {
            startActivity(new Intent(MainActivity.this, ProfileActivity.class));
        });

        // Fetch crypto data
        startFetchingCryptoData();

        // Set search listener
        searchEditText.addTextChangedListener(new android.text.TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                filterCryptoList();
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

        loginButton.setOnClickListener(v -> {
            startActivity(new Intent(MainActivity.this, LoginActivity.class));
        });

        signupButton.setOnClickListener(v -> {
            startActivity(new Intent(MainActivity.this, SignupActivity.class));
        });

        // 즐겨찾기 필터 버튼 추가
        Button filterButton = findViewById(R.id.filter_favorites);
        filterButton.setOnClickListener(v -> {
            showOnlyFavorites = !showOnlyFavorites;
            filterButton.setText(showOnlyFavorites ? "전체 보기" : "즐겨찾기만");
            filterCryptoList();
        });

        setupSortButtons();
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
        SharedPreferences prefs = getSharedPreferences("Auth", MODE_PRIVATE);
        String token = prefs.getString("token", null);

        if (token != null) {
            String url = "http://54.206.20.147:8080/api/auth/user";
            JsonObjectRequest request = new JsonObjectRequest(Request.Method.GET, url, null,
                response -> {
                    try {
                        // 서버 응답이 성공하면 로그인 상태 유지
                        prefs.edit().putBoolean("isLoggedIn", true).apply();
                        findViewById(R.id.auth_links).setVisibility(View.GONE);
                        findViewById(R.id.user_info).setVisibility(View.VISIBLE);
                        String username = response.getJSONObject("user").getString("username");
                        usernameTextView.setText(username);
                    } catch (JSONException e) {
                        // 서버 응답 파싱 실패시 로그아웃 처리
                        clearLoginState();
                    }
                },
                error -> {
                    // 서버 요청 실패시 로그아웃 처리
                    clearLoginState();
                }
            ) {
                @Override
                public Map<String, String> getHeaders() {
                    Map<String, String> headers = new HashMap<>();
                    headers.put("Authorization", "Bearer " + token);
                    return headers;
                }
            };
            requestQueue.add(request);
        } else {
            clearLoginState();
        }
    }

    private void clearLoginState() {
        SharedPreferences prefs = getSharedPreferences("Auth", MODE_PRIVATE);
        prefs.edit()
            .remove("username")
            .remove("token")
            .remove("isLoggedIn")
            .apply();
        findViewById(R.id.auth_links).setVisibility(View.VISIBLE);
        findViewById(R.id.user_info).setVisibility(View.GONE);
    }

    private void fetchCryptoData() {
        String url = "http://54.206.20.147:8080/api/market";
        JsonArrayRequest request = new JsonArrayRequest(Request.Method.GET, url, null,
            response -> {
                try {
                    originalData.clear();
                    for (int i = 0; i < response.length(); i++) {
                        JSONObject crypto = response.getJSONObject(i);
                        HashMap<String, String> item = new HashMap<>();
                        item.put("name", crypto.getString("koreanName"));
                        item.put("price", crypto.getString("tradePrice") + " KRW");
                        item.put("color", crypto.getString("priceColor"));
                        item.put("market", crypto.getString("market"));
                        item.put("changePrice", crypto.getString("signedChangePrice") + " KRW");
                        item.put("changeRate", String.format("%.2f%%", crypto.getDouble("signedChangeRate") * 100));
                        originalData.add(item);
                    }
                    filterCryptoList();
                } catch (JSONException e) {
                    e.printStackTrace();
                }
            },
            error -> Log.e("MainActivity", "Error fetching crypto data: " + error.toString())
        );
        requestQueue.add(request);
    }

    private void filterCryptoList() {
        ArrayList<HashMap<String, String>> filteredData = new ArrayList<>();
        String searchTerm = searchEditText.getText().toString().toLowerCase();

        for (HashMap<String, String> item : originalData) {
            boolean matchesSearch = item.get("name").toLowerCase().contains(searchTerm);
            boolean matchesFavorite = !showOnlyFavorites || isFavorite(item.get("market"));
            
            if (matchesSearch && matchesFavorite) {
                filteredData.add(item);
            }
        }

        // 정렬 적용
        sortCryptoList(filteredData);
        adapter.updateData(filteredData);
    }

    private void logout() {
        String url = "http://54.206.20.147:8080/api/auth/logout";

        JsonObjectRequest jsonObjectRequest = new JsonObjectRequest(Request.Method.POST, url, null,
                response -> {
                    // Auth SharedPreferences 사용
                    SharedPreferences prefs = getSharedPreferences("Auth", MODE_PRIVATE);
                    prefs.edit()
                        .remove("username")
                        .remove("token")
                        .remove("isLoggedIn")
                        .apply();
                    
                    Toast.makeText(this, "로그아웃되었습니다.", Toast.LENGTH_SHORT).show();
                    findViewById(R.id.auth_links).setVisibility(View.VISIBLE);
                    findViewById(R.id.user_info).setVisibility(View.GONE);
                    
                    // 어댑터 갱신
                    adapter.notifyDataSetChanged();
                    
                    // 액티비티 재시작하여 상태 완전히 초기화
                    Intent intent = new Intent(this, MainActivity.class);
                    intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
                    startActivity(intent);
                    finish();
                },
                error -> Toast.makeText(this, "로그아웃 실패", Toast.LENGTH_SHORT).show());

        requestQueue.add(jsonObjectRequest);
    }

    private boolean isLoggedIn() {
        SharedPreferences prefs = getSharedPreferences("Auth", MODE_PRIVATE);
        String token = prefs.getString("token", null);
        String username = prefs.getString("username", null);
        boolean isLoggedIn = prefs.getBoolean("isLoggedIn", false);
        
        return token != null && username != null && isLoggedIn;
    }

    public void toggleFavorite(String market) {
        String token = getSharedPreferences("Auth", MODE_PRIVATE).getString("token", "");
        if (token.isEmpty()) {
            Toast.makeText(this, "로그인이 필요한 서비스입니다.", Toast.LENGTH_SHORT).show();
            return;
        }

        String url = "http://54.206.20.147:8080/api/auth/favorites/" + 
            (isFavorite(market) ? "remove" : "add");

        JSONObject jsonBody = new JSONObject();
        try {
            jsonBody.put("cryptoId", market);
        } catch (JSONException e) {
            e.printStackTrace();
            return;
        }

        JsonObjectRequest request = new JsonObjectRequest(Request.Method.POST, url, jsonBody,
            response -> {
                try {
                    String message = response.getString("message");
                    Toast.makeText(this, message, Toast.LENGTH_SHORT).show();
                    fetchFavorites();
                } catch (JSONException e) {
                    e.printStackTrace();
                }
            },
            error -> Toast.makeText(this, "즐겨찾기 처리 중 오류가 발생했습니다.", Toast.LENGTH_SHORT).show()
        ) {
            @Override
            public Map<String, String> getHeaders() {
                Map<String, String> headers = new HashMap<>();
                headers.put("Authorization", "Bearer " + token);
                return headers;
            }
        };

        requestQueue.add(request);
    }

    private void fetchFavorites() {
        String token = getSharedPreferences("Auth", MODE_PRIVATE).getString("token", "");
        if (token.isEmpty()) return;

        String url = "http://54.206.20.147:8080/api/auth/favorites";

        JsonArrayRequest request = new JsonArrayRequest(Request.Method.GET, url, null,
            response -> {
                favorites.clear();
                for (int i = 0; i < response.length(); i++) {
                    try {
                        favorites.add(response.getString(i));
                    } catch (JSONException e) {
                        e.printStackTrace();
                    }
                }
                adapter.notifyDataSetChanged();
            },
            error -> Log.e("MainActivity", "Error fetching favorites: " + error.toString())
        ) {
            @Override
            public Map<String, String> getHeaders() {
                Map<String, String> headers = new HashMap<>();
                headers.put("Authorization", "Bearer " + token);
                return headers;
            }
        };

        requestQueue.add(request);
    }

    public boolean isFavorite(String market) {
        return favorites.contains(market);
    }

    private void setupFilterButton() {
        Button filterButton = findViewById(R.id.filter_favorites);
        filterButton.setOnClickListener(v -> {
            showOnlyFavorites = !showOnlyFavorites;
            filterButton.setText(showOnlyFavorites ? "전체 보기" : "즐겨찾기만");
            filterCryptoList();
        });
    }

    private void setupSortButtons() {
        Button sortNameBtn = findViewById(R.id.sort_name);
        Button sortPriceBtn = findViewById(R.id.sort_price);
        Button sortChangeRateBtn = findViewById(R.id.sort_change_rate);
        Button sortChangePriceBtn = findViewById(R.id.sort_change_price);

        View.OnClickListener sortClickListener = v -> {
            int previousSort = currentSortMethod;
            
            if (v.getId() == R.id.sort_name) {
                currentSortMethod = SORT_NAME;
            } else if (v.getId() == R.id.sort_price) {
                currentSortMethod = SORT_PRICE;
            } else if (v.getId() == R.id.sort_change_rate) {
                currentSortMethod = SORT_CHANGE_RATE;
            } else if (v.getId() == R.id.sort_change_price) {
                currentSortMethod = SORT_CHANGE_PRICE;
            }

            // 같은 버튼을 두 번 클릭하면 정렬 방향을 변경
            if (previousSort == currentSortMethod) {
                isAscending = !isAscending;
            } else {
                isAscending = true;
            }

            filterCryptoList();
        };

        sortNameBtn.setOnClickListener(sortClickListener);
        sortPriceBtn.setOnClickListener(sortClickListener);
        sortChangeRateBtn.setOnClickListener(sortClickListener);
        sortChangePriceBtn.setOnClickListener(sortClickListener);
    }

    private void sortCryptoList(ArrayList<HashMap<String, String>> list) {
        list.sort((item1, item2) -> {
            int result = 0;
            switch (currentSortMethod) {
                case SORT_NAME:
                    result = item1.get("name").compareTo(item2.get("name"));
                    break;
                case SORT_PRICE:
                    double price1 = Double.parseDouble(item1.get("price").replace(" KRW", "").replace(",", ""));
                    double price2 = Double.parseDouble(item2.get("price").replace(" KRW", "").replace(",", ""));
                    result = Double.compare(price1, price2);
                    break;
                case SORT_CHANGE_RATE:
                    double rate1 = Double.parseDouble(item1.get("changeRate").replace("%", ""));
                    double rate2 = Double.parseDouble(item2.get("changeRate").replace("%", ""));
                    result = Double.compare(rate1, rate2);
                    break;
                case SORT_CHANGE_PRICE:
                    double changePrice1 = Double.parseDouble(item1.get("changePrice").replace(" KRW", "").replace(",", ""));
                    double changePrice2 = Double.parseDouble(item2.get("changePrice").replace(" KRW", "").replace(",", ""));
                    result = Double.compare(changePrice1, changePrice2);
                    break;
            }
            return isAscending ? result : -result;
        });
    }
}
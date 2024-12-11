package com.example.bitchat;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.toolbox.JsonObjectRequest;
import com.android.volley.toolbox.Volley;
import org.json.JSONException;
import org.json.JSONObject;
import java.util.HashMap;
import java.util.Map;

public class ProfileActivity extends AppCompatActivity {
    private TextView usernameDisplay, emailDisplay;
    private EditText usernameInput, emailInput, newPasswordInput, confirmPasswordInput;
    private View usernameEditLayout, emailEditLayout;
    private Button editUsernameButton, updateUsernameButton;
    private Button editEmailButton, updateEmailButton;
    private Button updatePasswordButton;
    private RequestQueue requestQueue;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_profile);
        
        initViews();
        setupListeners();
        loadUserInfo();
    }

    private void initViews() {
        usernameDisplay = findViewById(R.id.username_display);
        emailDisplay = findViewById(R.id.email_display);
        
        usernameInput = findViewById(R.id.username_input);
        emailInput = findViewById(R.id.email_input);
        newPasswordInput = findViewById(R.id.new_password_input);
        confirmPasswordInput = findViewById(R.id.confirm_password_input);
        
        usernameEditLayout = findViewById(R.id.username_edit_layout);
        emailEditLayout = findViewById(R.id.email_edit_layout);
        
        editUsernameButton = findViewById(R.id.edit_username_button);
        updateUsernameButton = findViewById(R.id.update_username_button);
        editEmailButton = findViewById(R.id.edit_email_button);
        updateEmailButton = findViewById(R.id.update_email_button);
        updatePasswordButton = findViewById(R.id.update_password_button);
        
        requestQueue = Volley.newRequestQueue(this);
    }

    private void setupListeners() {
        editUsernameButton.setOnClickListener(v -> {
            usernameEditLayout.setVisibility(View.VISIBLE);
            usernameInput.setText(usernameDisplay.getText());
        });

        editEmailButton.setOnClickListener(v -> {
            emailEditLayout.setVisibility(View.VISIBLE);
            emailInput.setText(emailDisplay.getText());
        });

        updateUsernameButton.setOnClickListener(v -> updateUsername());
        updateEmailButton.setOnClickListener(v -> updateEmail());
        updatePasswordButton.setOnClickListener(v -> updatePassword());
    }

    private void loadUserInfo() {
        String token = getSharedPreferences("Auth", MODE_PRIVATE).getString("token", "");
        String url = "http://54.206.20.147:8080/api/auth/user";

        JsonObjectRequest request = new JsonObjectRequest(Request.Method.GET, url, null,
            response -> {
                try {
                    JSONObject user = response.getJSONObject("user");
                    usernameDisplay.setText(user.getString("username"));
                    emailDisplay.setText(user.getString("email"));
                } catch (JSONException e) {
                    Toast.makeText(this, "사용자 정보를 불러올 수 없습니다.", Toast.LENGTH_SHORT).show();
                }
            },
            error -> Toast.makeText(this, "사용자 정보를 불러올 수 없습니다.", Toast.LENGTH_SHORT).show()
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

    private void updateUsername() {
        String newUsername = usernameInput.getText().toString().trim();
        if (TextUtils.isEmpty(newUsername)) {
            Toast.makeText(this, "유저네임을 입력해주세요.", Toast.LENGTH_SHORT).show();
            return;
        }

        String token = getSharedPreferences("Auth", MODE_PRIVATE).getString("token", "");
        String url = "http://54.206.20.147:8080/api/auth/update";

        JSONObject jsonBody = new JSONObject();
        try {
            jsonBody.put("username", newUsername);
        } catch (JSONException e) {
            e.printStackTrace();
            return;
        }

        JsonObjectRequest request = new JsonObjectRequest(Request.Method.PUT, url, jsonBody,
            response -> {
                usernameDisplay.setText(newUsername);
                usernameEditLayout.setVisibility(View.GONE);
                Toast.makeText(this, "유저네임이 업데이트되었습니다.", Toast.LENGTH_SHORT).show();
                
                // Update SharedPreferences
                SharedPreferences.Editor editor = getSharedPreferences("Auth", MODE_PRIVATE).edit();
                editor.putString("username", newUsername);
                editor.apply();

                // MainActivity 새로고침을 위한 Intent 설정
                Intent intent = new Intent(this, MainActivity.class);
                intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
                startActivity(intent);
                finish();
            },
            error -> Toast.makeText(this, "유저네임 업데이트에 실패했습니다.", Toast.LENGTH_SHORT).show()
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

    private void updateEmail() {
        String newEmail = emailInput.getText().toString().trim();
        if (TextUtils.isEmpty(newEmail)) {
            Toast.makeText(this, "이메일을 입력해주세요.", Toast.LENGTH_SHORT).show();
            return;
        }

        if (!android.util.Patterns.EMAIL_ADDRESS.matcher(newEmail).matches()) {
            Toast.makeText(this, "올바른 이메일 형식이 아닙니다.", Toast.LENGTH_SHORT).show();
            return;
        }

        // 이메일 인증 코드 요청
        String token = getSharedPreferences("Auth", MODE_PRIVATE).getString("token", "");
        String verificationUrl = "http://54.206.20.147:8080/api/auth/sendverificationcode";

        JSONObject verificationBody = new JSONObject();
        try {
            verificationBody.put("email", newEmail);
        } catch (JSONException e) {
            e.printStackTrace();
            return;
        }

        JsonObjectRequest verificationRequest = new JsonObjectRequest(Request.Method.POST, verificationUrl, verificationBody,
            response -> {
                // 인증 코드 입력 다이얼로그 표시
                android.app.AlertDialog.Builder builder = new android.app.AlertDialog.Builder(this);
                View dialogView = getLayoutInflater().inflate(R.layout.dialog_verification_code, null);
                EditText codeInput = dialogView.findViewById(R.id.verification_code_input);
                
                builder.setView(dialogView)
                    .setTitle("이메일 인증")
                    .setMessage("입력하신 이메일로 전송된 인증번호를 입력해주세요.")
                    .setPositiveButton("확인", (dialog, which) -> {
                        String code = codeInput.getText().toString().trim();
                        verifyEmailCode(newEmail, code);
                    })
                    .setNegativeButton("취소", (dialog, which) -> dialog.cancel())
                    .show();
            },
            error -> Toast.makeText(this, "인증 코드 전송에 실패했습니다.", Toast.LENGTH_SHORT).show()
        ) {
            @Override
            public Map<String, String> getHeaders() {
                Map<String, String> headers = new HashMap<>();
                headers.put("Authorization", "Bearer " + token);
                return headers;
            }
        };

        requestQueue.add(verificationRequest);
    }

private void verifyEmailCode(String newEmail, String code) {
    String token = getSharedPreferences("Auth", MODE_PRIVATE).getString("token", "");
    String verifyUrl = "http://54.206.20.147:8080/api/auth/verifycode";

    JSONObject verifyBody = new JSONObject();
    try {
        verifyBody.put("email", newEmail);
        verifyBody.put("code", code);
    } catch (JSONException e) {
        e.printStackTrace();
        return;
    }

    JsonObjectRequest verifyRequest = new JsonObjectRequest(Request.Method.POST, verifyUrl, verifyBody,
        response -> {
            // 인증 성공 시 이메일 업데이트
            updateEmailFinal(newEmail);
        },
        error -> Toast.makeText(this, "인증 코드가 올바르지 않습니다.", Toast.LENGTH_SHORT).show()
    ) {
        @Override
        public Map<String, String> getHeaders() {
            Map<String, String> headers = new HashMap<>();
            headers.put("Authorization", "Bearer " + token);
            return headers;
        }
    };

    requestQueue.add(verifyRequest);
}

private void updateEmailFinal(String newEmail) {
    String token = getSharedPreferences("Auth", MODE_PRIVATE).getString("token", "");
    String url = "http://54.206.20.147:8080/api/auth/update";

    JSONObject jsonBody = new JSONObject();
    try {
        jsonBody.put("email", newEmail);
    } catch (JSONException e) {
        e.printStackTrace();
        return;
    }

    JsonObjectRequest request = new JsonObjectRequest(Request.Method.PUT, url, jsonBody,
        response -> {
            emailDisplay.setText(newEmail);
            emailEditLayout.setVisibility(View.GONE);
            Toast.makeText(this, "이메일이 업데이트되었습니다.", Toast.LENGTH_SHORT).show();
            
            // SharedPreferences 업데이트
            SharedPreferences.Editor editor = getSharedPreferences("Auth", MODE_PRIVATE).edit();
            editor.putString("email", newEmail);
            editor.apply();
        },
        error -> Toast.makeText(this, "이메일 업데이트에 실패했습니다.", Toast.LENGTH_SHORT).show()
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

    private void updatePassword() {
        String newPassword = newPasswordInput.getText().toString();
        String confirmPassword = confirmPasswordInput.getText().toString();

        if (!newPassword.equals(confirmPassword)) {
            Toast.makeText(this, "비밀번호가 일치하지 않습니다.", Toast.LENGTH_SHORT).show();
            return;
        }

        if (!isValidPassword(newPassword)) {
            Toast.makeText(this, "비밀번호는 8자 이상, 영문자, 숫자, 특수문자를 포함해야 합니다.", Toast.LENGTH_SHORT).show();
            return;
        }

        String token = getSharedPreferences("Auth", MODE_PRIVATE).getString("token", "");
        String url = "http://54.206.20.147:8080/api/auth/update-password";

        JSONObject jsonBody = new JSONObject();
        try {
            jsonBody.put("password", newPassword);
        } catch (JSONException e) {
            e.printStackTrace();
            return;
        }

        JsonObjectRequest request = new JsonObjectRequest(Request.Method.PUT, url, jsonBody,
            response -> {
                Toast.makeText(this, "비밀번호가 업데이트되었습니다.", Toast.LENGTH_SHORT).show();
                newPasswordInput.setText("");
                confirmPasswordInput.setText("");
            },
            error -> Toast.makeText(this, "비밀번호 업데이트에 실패했습니다.", Toast.LENGTH_SHORT).show()
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

    private boolean isValidPassword(String password) {
        return password.matches("^(?=.*[A-Za-z])(?=.*\\d)(?=.*[@$!%*#?&])[A-Za-z\\d@$!%*#?&]{8,}$");
    }
} 
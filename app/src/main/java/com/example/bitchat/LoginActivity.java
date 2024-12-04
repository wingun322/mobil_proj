package com.example.bitchat;

import android.os.Bundle;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.content.Intent;
import android.text.TextUtils;
import android.widget.Toast;
import android.util.Patterns;
import androidx.appcompat.app.AppCompatActivity;

import com.example.bitchat.callback.AuthCallback;
import com.example.bitchat.model.LoginRequest;
import com.example.bitchat.model.LoginResponse;


public class LoginActivity extends AppCompatActivity {
    private EditText emailInput;
    private EditText passwordInput;
    private Button loginButton;
    private TextView signupLink;
    private AuthRepository authRepository;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);
        
        initViews();
        setupListeners();
        authRepository = new AuthRepository(this);
    }

    private void initViews() {
        emailInput = findViewById(R.id.emailInput);
        passwordInput = findViewById(R.id.passwordInput);
        loginButton = findViewById(R.id.loginButton);
        signupLink = findViewById(R.id.signupLink);
    }

    private void setupListeners() {
        loginButton.setOnClickListener(v -> attemptLogin());
        signupLink.setOnClickListener(v -> {
            startActivity(new Intent(LoginActivity.this, SignupActivity.class));
        });
    }

    private void attemptLogin() {
        String email = emailInput.getText().toString().trim();
        String password = passwordInput.getText().toString().trim();

        if (validateInput(email, password)) {
            loginButton.setEnabled(false); // 버튼 비활성화
            login(email, password);
        }
    }

    private boolean validateInput(String email, String password) {
        if (TextUtils.isEmpty(email)) {
            emailInput.setError("이메일을 입력하세요");
            return false;
        }
        if (!Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            emailInput.setError("유효한 이메일을 입력하세요");
            return false;
        }
        if (TextUtils.isEmpty(password)) {
            passwordInput.setError("비밀번호를 입력하세요");
            return false;
        }
        return true;
    }

    private void login(String email, String password) {
        LoginRequest loginRequest = new LoginRequest(email, password);
        authRepository.login(loginRequest, new AuthCallback<LoginResponse>() {
            @Override
            public void onSuccess(LoginResponse response) {
                runOnUiThread(() -> {
                    Toast.makeText(LoginActivity.this, "로그인 성공", Toast.LENGTH_SHORT).show();
                    Intent intent = new Intent(LoginActivity.this, MainActivity.class);
                    intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                    startActivity(intent);
                    finish();
                });
            }

            @Override
            public void onError(String message) {
                runOnUiThread(() -> {
                    loginButton.setEnabled(true);
                    Toast.makeText(LoginActivity.this, message, Toast.LENGTH_LONG).show();
                });
            }
        });
    }
}
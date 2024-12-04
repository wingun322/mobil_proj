package com.example.bitchat;

import android.content.Intent;
import android.os.Bundle;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.text.TextWatcher;
import android.text.Editable;
import android.util.Patterns;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.example.bitchat.callback.AuthCallback;

public class SignupActivity extends AppCompatActivity {
    private EditText usernameInput, emailInput, passwordInput, confirmPasswordInput;
    private Button signupButton, checkUsernameButton, checkEmailButton;
    private TextView usernameMessage, emailMessage, passwordConfirmMessage;
    private boolean isUsernameValid = false;
    private boolean isEmailValid = false;
    private boolean isPasswordMatch = false;
    private AuthRepository authRepository;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_signup);
        
        authRepository = new AuthRepository(this);
        initViews();
        setupListeners();
        
        confirmPasswordInput = findViewById(R.id.confirmPasswordInput);
        passwordConfirmMessage = findViewById(R.id.passwordConfirmMessage);
        
        confirmPasswordInput.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {}

            @Override
            public void afterTextChanged(Editable s) {
                checkPasswordMatch();
            }
        });
    }

    private void initViews() {
        usernameInput = findViewById(R.id.usernameInput);
        emailInput = findViewById(R.id.emailInput);
        passwordInput = findViewById(R.id.passwordInput);
        confirmPasswordInput = findViewById(R.id.confirmPasswordInput);
        
        signupButton = findViewById(R.id.signupButton);
        checkUsernameButton = findViewById(R.id.checkUsernameButton);
        checkEmailButton = findViewById(R.id.checkEmailButton);
        
        usernameMessage = findViewById(R.id.usernameMessage);
        emailMessage = findViewById(R.id.emailMessage);
        passwordConfirmMessage = findViewById(R.id.passwordConfirmMessage);
        
        signupButton.setEnabled(false);
    }

    private void setupListeners() {
        checkUsernameButton.setOnClickListener(v -> checkUsername());
        checkEmailButton.setOnClickListener(v -> checkEmail());
        signupButton.setOnClickListener(v -> attemptSignup());
        
        TextWatcher textWatcher = new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {}

            @Override
            public void afterTextChanged(Editable s) {
                validateForm();
            }
        };

        usernameInput.addTextChangedListener(textWatcher);
        emailInput.addTextChangedListener(textWatcher);
        passwordInput.addTextChangedListener(textWatcher);
        confirmPasswordInput.addTextChangedListener(textWatcher);
    }

    private void checkUsername() {
        String username = usernameInput.getText().toString().trim();
        if (username.isEmpty()) {
            showMessage(usernameMessage, "유저네임을 입력해주세요.", false);
            return;
        }

        authRepository.checkUsername(username, new AuthCallback<String>() {
            @Override
            public void onSuccess(String message) {
                runOnUiThread(() -> {
                    isUsernameValid = true;
                    showMessage(usernameMessage, "사용 가능한 유저네임입니다.", true);
                    validateForm();
                });
            }

            @Override
            public void onError(String error) {
                runOnUiThread(() -> {
                    isUsernameValid = false;
                    showMessage(usernameMessage, error, false);
                    validateForm();
                });
            }
        });
    }

    private void checkEmail() {
        String email = emailInput.getText().toString().trim();
        if (!Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            showMessage(emailMessage, "올바른 이메일 형식이 아닙니다.", false);
            return;
        }

        authRepository.checkEmail(email, new AuthCallback<String>() {
            @Override
            public void onSuccess(String message) {
                runOnUiThread(() -> {
                    isEmailValid = true;
                    showMessage(emailMessage, "사용 가능한 이메일입니다.", true);
                    validateForm();
                });
            }

            @Override
            public void onError(String error) {
                runOnUiThread(() -> {
                    isEmailValid = false;
                    showMessage(emailMessage, error, false);
                    validateForm();
                });
            }
        });
    }

    private void checkPasswordMatch() {
        String password = passwordInput.getText().toString();
        String confirmPassword = confirmPasswordInput.getText().toString();
        
        if (confirmPassword.isEmpty()) {
            showMessage(passwordConfirmMessage, "", false);
            isPasswordMatch = false;
        } else if (password.equals(confirmPassword)) {
            showMessage(passwordConfirmMessage, "비밀번호가 일치합니다.", true);
            isPasswordMatch = true;
        } else {
            showMessage(passwordConfirmMessage, "비밀번호가 일치하지 않습니다.", false);
            isPasswordMatch = false;
        }
        validateForm();
    }

    private void validateForm() {
        String password = passwordInput.getText().toString();
        boolean isPasswordValid = password.length() >= 8 && 
            password.matches("^(?=.*[A-Za-z])(?=.*\\d)(?=.*[@$!%*#?&])[A-Za-z\\d@$!%*#?&]{8,}$");

        signupButton.setEnabled(isUsernameValid && isEmailValid && isPasswordValid && isPasswordMatch);
    }

    private void showMessage(TextView view, String message, boolean isSuccess) {
        runOnUiThread(() -> {
            view.setText(message);
            view.setTextColor(isSuccess ? 
                getColor(R.color.success_green) : 
                getColor(R.color.error_red));
        });
    }

    private void attemptSignup() {
        String username = usernameInput.getText().toString().trim();
        String email = emailInput.getText().toString().trim();
        String password = passwordInput.getText().toString().trim();

        if (!isUsernameValid) {
            Toast.makeText(this, "유저네임 중복확인이 필요합니다.", Toast.LENGTH_SHORT).show();
            return;
        }

        if (!isEmailValid) {
            Toast.makeText(this, "이메일 중복확인이 필요합니다.", Toast.LENGTH_SHORT).show();
            return;
        }

        if (!password.matches("^(?=.*[A-Za-z])(?=.*\\d)(?=.*[@$!%*#?&])[A-Za-z\\d@$!%*#?&]{8,}$")) {
            Toast.makeText(this, "비밀번호는 8자 이상, 영문자, 숫자, 특수문자를 포함해야 합니다.", Toast.LENGTH_SHORT).show();
            return;
        }

        if (!isPasswordMatch) {
            Toast.makeText(this, "비밀번호가 일치하지 않습니다.", Toast.LENGTH_SHORT).show();
            return;
        }

        signupButton.setEnabled(false);

        // 회원가입 요청
        authRepository.signup(username, email, password, new AuthCallback<String>() {
            @Override
            public void onSuccess(String response) {
                runOnUiThread(() -> {
                    Toast.makeText(SignupActivity.this, "회원가입이 완료되었습니다.", Toast.LENGTH_SHORT).show();
                    Intent intent = new Intent(SignupActivity.this, EmailVerificationActivity.class);
                    intent.putExtra("email", email);
                    startActivity(intent);
                    finish();
                });
            }

            @Override
            public void onError(String error) {
                runOnUiThread(() -> {
                    signupButton.setEnabled(true);
                    Toast.makeText(SignupActivity.this, error, Toast.LENGTH_SHORT).show();
                });
            }
        });
    }
}
package com.example.bitchat;

import android.os.Bundle;
import android.os.CountDownTimer;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.content.Intent;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import java.util.Locale;
import com.example.bitchat.callback.AuthCallback;

public class EmailVerificationActivity extends AppCompatActivity {
    private EditText verificationCodeInput;
    private TextView timerText;
    private Button verifyButton;
    private Button resendButton;
    private String email;
    private CountDownTimer countDownTimer;
    private static final long TIMER_DURATION = 600000; // 10분
    private AuthRepository authRepository;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_email_verification);

        authRepository = new AuthRepository(this);
        email = getIntent().getStringExtra("email");
        initViews();
        setupListeners();
        startTimer();
    }

    private void initViews() {
        verificationCodeInput = findViewById(R.id.verificationCodeInput);
        timerText = findViewById(R.id.timerText);
        verifyButton = findViewById(R.id.verifyButton);
        resendButton = findViewById(R.id.resendButton);
        
        TextView emailDisplay = findViewById(R.id.emailDisplay);
        emailDisplay.setText(email);
    }

    private void setupListeners() {
        verifyButton = findViewById(R.id.verifyButton);
        resendButton = findViewById(R.id.resendButton);
        timerText = findViewById(R.id.timerText);
        TextView emailDisplay = findViewById(R.id.emailDisplay);
        emailDisplay.setText(email);

        verifyButton.setOnClickListener(v -> verifyCode());
        resendButton.setOnClickListener(v -> resendVerificationCode());
    }

    private void startTimer() {
        if (countDownTimer != null) {
            countDownTimer.cancel();
        }

        resendButton.setEnabled(false);
        
        countDownTimer = new CountDownTimer(TIMER_DURATION, 1000) {
            @Override
            public void onTick(long millisUntilFinished) {
                long minutes = millisUntilFinished / 60000;
                long seconds = (millisUntilFinished % 60000) / 1000;
                String timeLeft = String.format(Locale.getDefault(), 
                    "%d:%02d", minutes, seconds);
                timerText.setText(timeLeft);
            }

            @Override
            public void onFinish() {
                timerText.setText("인증 시간이 만료되었습니다.");
                resendButton.setEnabled(true);
            }
        }.start();
    }

    private void verifyCode() {
        String code = verificationCodeInput.getText().toString();
        if (code.length() != 6) {
            Toast.makeText(this, "올바른 인증번호를 입력해주세요.", 
                Toast.LENGTH_SHORT).show();
            return;
        }

        authRepository.verifyEmail(email, code, new AuthCallback<String>() {
            @Override
            public void onSuccess(String response) {
                runOnUiThread(() -> {
                    Toast.makeText(EmailVerificationActivity.this, 
                        "이메일 인증이 완료되었습니다.", Toast.LENGTH_SHORT).show();
                    Intent intent = new Intent(EmailVerificationActivity.this, LoginActivity.class);
                    intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                    startActivity(intent);
                    finish();
                });
            }

            @Override
            public void onError(String error) {
                runOnUiThread(() -> Toast.makeText(EmailVerificationActivity.this, 
                    error, Toast.LENGTH_SHORT).show());
            }
        });
    }

    private void resendVerificationCode() {
        authRepository.resendVerificationCode(email, new AuthCallback<String>() {
            @Override
            public void onSuccess(String response) {
                runOnUiThread(() -> {
                    Toast.makeText(EmailVerificationActivity.this, 
                        "인증번호가 재전송되었습니다.", Toast.LENGTH_SHORT).show();
                    startTimer();
                });
            }

            @Override
            public void onError(String error) {
                runOnUiThread(() -> Toast.makeText(EmailVerificationActivity.this, 
                    error, Toast.LENGTH_SHORT).show());
            }
        });
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (countDownTimer != null) {
            countDownTimer.cancel();
        }
    }
}
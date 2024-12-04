package com.example.bitchat;

import android.content.Context;
import android.content.SharedPreferences;

import com.example.bitchat.callback.AuthCallback;
import com.example.bitchat.model.LoginRequest;

import org.json.JSONException;
import org.json.JSONObject;

import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import okhttp3.RequestBody;
import okhttp3.MediaType;

import com.example.bitchat.model.LoginResponse;
import com.google.gson.Gson;

public class AuthRepository {
    private static final String BASE_URL = "http://54.206.20.147:8080";
    private static final String PREF_NAME = "BitChatPrefs";
    private final Context context;
    private final OkHttpClient client;
    private final Gson gson;
    private static AuthRepository instance;

    public AuthRepository(Context context) {
        this.context = context;
        this.client = new OkHttpClient();
        this.gson = new Gson();
    }

    public static AuthRepository getInstance() {
        if (instance == null) {
            throw new IllegalStateException("AuthRepository must be initialized first");
        }
        return instance;
    }

    public void login(LoginRequest loginRequest, AuthCallback callback) {
        new Thread(() -> {
            try {
                String json = gson.toJson(loginRequest);
                RequestBody body = RequestBody.create(
                    MediaType.parse("application/json; charset=utf-8"), json);

                Request request = new Request.Builder()
                    .url(BASE_URL + "/api/auth/login")
                    .post(body)
                    .build();

                Response response = client.newCall(request).execute();
                String responseBody = response.body().string();

                if (response.isSuccessful()) {
                    LoginResponse loginResponse = gson.fromJson(responseBody, LoginResponse.class);
                    if (loginResponse.getAccessToken() != null) {
                        saveAuthToken(loginResponse.getAccessToken());
                        callback.onSuccess(loginResponse);
                    } else {
                        callback.onError("Invalid response from server");
                    }
                } else {
                    callback.onError("로그인 실패: " + responseBody);
                }
            } catch (Exception e) {
                e.printStackTrace();
                callback.onError("네트워크 오류: " + e.getMessage());
            }
        }).start();
    }

    private void saveAuthToken(String token) {
        SharedPreferences prefs = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
        prefs.edit()
            .putString("accessToken", token)
            .apply();
    }

    public void checkUsername(String username, AuthCallback<String> callback) {
        new Thread(() -> {
            try {
                JSONObject jsonBody = new JSONObject();
                jsonBody.put("username", username);

                RequestBody body = RequestBody.create(
                    MediaType.parse("application/json; charset=utf-8"),
                    jsonBody.toString());

                Request request = new Request.Builder()
                    .url(BASE_URL + "/api/auth/check-username")
                    .post(body)
                    .build();

                Response response = client.newCall(request).execute();
                String responseBody = response.body().string();

                if (response.code() == 200) {
                    callback.onSuccess("사용 가능한 유저네임입니다.");
                } else {
                    callback.onError("이미 사용중인 유저네임입니다.");
                }
            } catch (Exception e) {
                callback.onError("네트워크 오류: " + e.getMessage());
            }
        }).start();
    }

    public void checkEmail(String email, AuthCallback<String> callback) {
        new Thread(() -> {
            try {
                JSONObject jsonBody = new JSONObject();
                jsonBody.put("email", email);

                RequestBody body = RequestBody.create(
                    MediaType.parse("application/json; charset=utf-8"),
                    jsonBody.toString());

                Request request = new Request.Builder()
                    .url(BASE_URL + "/api/auth/check-email")
                    .post(body)
                    .build();

                Response response = client.newCall(request).execute();
                String responseBody = response.body().string();

                if (response.code() == 200) {
                    callback.onSuccess("사용 가능한 이메일입니다.");
                } else {
                    callback.onError("이미 사용중인 이메일입니다.");
                }
            } catch (Exception e) {
                callback.onError("네트워크 오류: " + e.getMessage());
            }
        }).start();
    }

    public void verifyEmail(String email, String code, AuthCallback<String> callback) {
        new Thread(() -> {
            try {
                JSONObject jsonBody = new JSONObject();
                jsonBody.put("email", email);
                jsonBody.put("verificationCode", code);

                RequestBody body = RequestBody.create(
                    MediaType.parse("application/json; charset=utf-8"),
                    jsonBody.toString());

                Request request = new Request.Builder()
                    .url(BASE_URL + "/api/auth/verify-code")
                    .post(body)
                    .build();

                Response response = client.newCall(request).execute();
                String responseBody = response.body().string();
                JSONObject jsonResponse = new JSONObject(responseBody);

                if (response.isSuccessful()) {
                    callback.onSuccess(jsonResponse.optString("message", "이메일 인증이 완료되었습니다."));
                } else {
                    callback.onError(jsonResponse.optString("error", "인증 실패"));
                }
            } catch (Exception e) {
                callback.onError("네트워크 오류: " + e.getMessage());
            }
        }).start();
    }

    public void resendVerificationCode(String email, AuthCallback<String> callback) {
        new Thread(() -> {
            try {
                JSONObject jsonBody = new JSONObject();
                jsonBody.put("email", email);
                RequestBody body = RequestBody.create(
                    MediaType.parse("application/json; charset=utf-8"),
                    jsonBody.toString());

                Request request = new Request.Builder()
                    .url(BASE_URL + "/api/auth/resend-verification")
                    .post(body)
                    .build();

                Response response = client.newCall(request).execute();
                if (response.isSuccessful()) {
                    callback.onSuccess("인증번호가 재전송되었습니다.");
                } else {
                    callback.onError("인증번호 재전송 실패");
                }
            } catch (Exception e) {
                callback.onError("네트워크 오류: " + e.getMessage());
            }
        }).start();
    }

    public void signup(String username, String email, String password, AuthCallback<String> callback) {
        new Thread(() -> {
            try {
                JSONObject jsonBody = new JSONObject();
                jsonBody.put("username", username);
                jsonBody.put("email", email);
                jsonBody.put("password", password);

                RequestBody body = RequestBody.create(
                    MediaType.parse("application/json; charset=utf-8"),
                    jsonBody.toString());

                Request request = new Request.Builder()
                    .url(BASE_URL + "/api/auth/register")
                    .post(body)
                    .build();

                Response response = client.newCall(request).execute();
                String responseBody = response.body().string();

                try {
                    JSONObject jsonResponse = new JSONObject(responseBody);
                    if (response.isSuccessful()) {
                        callback.onSuccess(jsonResponse.optString("message", "회원가입이 완료되었습니다."));
                    } else {
                        callback.onError(jsonResponse.optString("error", "회원가입 실패"));
                    }
                } catch (JSONException e) {
                    if (response.isSuccessful()) {
                        callback.onSuccess("회원가입이 완료되었습니다.");
                    } else {
                        callback.onError("회원가입 실패: " + responseBody);
                    }
                }
            } catch (Exception e) {
                callback.onError("네트워크 오류: " + e.getMessage());
            }
        }).start();
    }
}
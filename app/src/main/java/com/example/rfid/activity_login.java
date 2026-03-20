package com.example.rfid;

import androidx.appcompat.app.AppCompatActivity;

import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import com.android.volley.DefaultRetryPolicy;
import com.android.volley.NoConnectionError;
import com.android.volley.Request;
import com.android.volley.ServerError;
import com.android.volley.TimeoutError;
import com.android.volley.toolbox.StringRequest;
import com.android.volley.toolbox.Volley;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.card.MaterialCardView;

import org.json.JSONObject;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

public class activity_login extends AppCompatActivity {

    EditText etUsername, etPassword;
    MaterialButton btnLogin;
    dbclass db;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);

        db = new dbclass(this);

        MaterialCardView card = findViewById(R.id.loginCard);
        TextView title = findViewById(R.id.titleText);
        TextView subtitle = findViewById(R.id.subtitleText);

        etUsername = findViewById(R.id.etUsername);
        etPassword = findViewById(R.id.etPassword);
        btnLogin = findViewById(R.id.btnLogin);

        btnLogin.setOnClickListener(v -> login());

        // Start hidden
        card.setTranslationY(200f);
        card.setAlpha(0f);
        title.setAlpha(0f);
        subtitle.setAlpha(0f);
        btnLogin.setScaleX(0.9f);
        btnLogin.setScaleY(0.9f);

        // Animate in
        card.animate().translationY(0).alpha(1f).setDuration(600).start();
        title.animate().alpha(1f).setStartDelay(200).setDuration(400).start();
        subtitle.animate().alpha(1f).setStartDelay(300).setDuration(400).start();
        btnLogin.animate().scaleX(1f).scaleY(1f).setStartDelay(500).setDuration(300).start();
    }

    private void login() {

        String username = etUsername.getText().toString().trim();
        String password = etPassword.getText().toString().trim();

        if (username.isEmpty() || password.isEmpty()) {
            shake(etUsername);
            shake(etPassword);
            //Toast.makeText(this, "Username & Password required", Toast.LENGTH_SHORT).show();
            db.showScrollableErrorDialog(this, "Error","Username & Password required");
            return;
        }

        if (!isInternetAvailable()) {
            //Toast.makeText(this, "No Internet Connection", Toast.LENGTH_SHORT).show();
            db.showScrollableErrorDialog(this, "Error","No Internet Connection");
            return;
        }

        setLoading(true);

        if (isMobileLoginValid(username, password)) {
            startActivity(new Intent(this, rfidoops.class));
            finish();
        } else {
            authenticateFromServer(username, password);
        }
    }

    private void setLoading(boolean loading) {
        btnLogin.setEnabled(!loading);
        btnLogin.setText(loading ? "Signing in..." : "Login");
    }

    private boolean isInternetAvailable() {
        ConnectivityManager cm = (ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkInfo networkInfo = cm.getActiveNetworkInfo();
        return networkInfo != null && networkInfo.isConnected();
    }

    private void authenticateFromServer(String username, String password) {

        String url = ApiConfig.MOBILE_LOGIN;

        StringRequest request = new StringRequest(Request.Method.POST, url,
                response -> {
                    try {
                        JSONObject obj = new JSONObject(response);
                        boolean status = obj.optBoolean("status", false);
                        String message = obj.optString("message", "Unknown error");

                        if (status) {

                            JSONObject data = obj.getJSONObject("data");

                            saveLogin(
                                    username,
                                    password,
                                    data.getString("u_id"),
                                    data.getString("location_id"),
                                    data.getString("status")
                            );

                            startActivity(new Intent(this, sync.class));
                            finish();

                        } else {
                            setLoading(false);
                            shake(etUsername);
                            shake(etPassword);
                            //Toast.makeText(this, message, Toast.LENGTH_SHORT).show();
                            db.showScrollableErrorDialog(this, "Error",message);
                        }

                    } catch (Exception e) {
                        setLoading(false);
                        e.printStackTrace();
                    }
                },
                error -> {
                    //error.getMessage();
                    String errorMsg = "";
                    setLoading(false);
                    if (error.networkResponse != null) {
                        int statusCode = error.networkResponse.statusCode;

                        try {
                            String responseBody = new String(
                                    error.networkResponse.data,
                                    "UTF-8"
                            );

                            Log.e("LOGIN_ERROR", "Status: " + statusCode);
                            Log.e("LOGIN_ERROR", "Body: " + responseBody);

                            errorMsg = "Server error (" + statusCode + ")";
                        } catch (Exception e) {
                            Log.e("LOGIN_ERROR", "Error parsing error response", e);
                        }

                    } else if (error instanceof TimeoutError) {
                        errorMsg = "Request timeout. Check your internet.";
                    } else if (error instanceof NoConnectionError) {
                        errorMsg = "No internet connection.";
                    } else if (error instanceof ServerError) {
                        errorMsg = "Server error.";
                    }

                    //Toast.makeText(this, errorMsg, Toast.LENGTH_LONG).show();

                    db.showScrollableErrorDialog(this, "Error","Server error "+errorMsg);


                    //Toast.makeText(this, "Server error "+errorMsg, Toast.LENGTH_SHORT).show();
                }) {

            @Override
            protected Map<String, String> getParams() {
                Map<String, String> params = new HashMap<>();
                params.put("login", username);
                params.put("pass", password);
                params.put("u_id", "");
                params.put("version", getAppVersionName());
                return params;
            }

            @Override
            public Map<String, String> getHeaders() {
                Map<String, String> headers = new HashMap<>();
                headers.put("Authorization", "Bearer Mzt7vkcoPnxkqZq6vFW6fxP3e61b66nSlYkWNETTUHiN7VL5P8GJSIvzcioq");
                headers.put("Accept", "application/json");
                return headers;
            }
        };

        request.setRetryPolicy(new DefaultRetryPolicy(15000, 0, DefaultRetryPolicy.DEFAULT_BACKOFF_MULT));
        Volley.newRequestQueue(this).add(request);
    }

    private void saveLogin(String login, String password, String u_id, String locationId, String status) {
        SQLiteDatabase dbw = db.getWritableDatabase();
        ContentValues cv = new ContentValues();

        cv.put("login", login);
        cv.put("password", password);
        cv.put("u_id", u_id);
        cv.put("location_id", locationId);
        cv.put("status", status);
        cv.put("created_at", getCurrentDateTime());

        dbw.insertWithOnConflict("mobile_logins", null, cv, SQLiteDatabase.CONFLICT_REPLACE);
    }

    public static String getCurrentDateTime() {
        return new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault()).format(new Date());
    }

    private String getAppVersionName() {
        try {
            PackageInfo pInfo = getPackageManager().getPackageInfo(getPackageName(), 0);
            return pInfo.versionName;   // e.g. "1.0.3"
        } catch (PackageManager.NameNotFoundException e) {
            return "1.0";
        }
    }

    public boolean isMobileLoginValid(String username, String password) {

        SQLiteDatabase db = this.db.getReadableDatabase();
        Cursor c = db.rawQuery("SELECT status FROM mobile_logins WHERE login=? AND password=?",
                new String[]{username, password});

        if (c.moveToFirst()) {
            int status = c.getInt(c.getColumnIndexOrThrow("status"));
            c.close();
            return status == 1;
        }

        c.close();
        return false;
    }

    private void shake(View view) {
        view.animate().translationXBy(20).setDuration(60)
                .withEndAction(() -> view.animate().translationXBy(-40).setDuration(120)
                        .withEndAction(() -> view.animate().translationXBy(20).setDuration(60).start())
                        .start());
    }
/*
    @Override
    public void onBackPressed() {
        super.onBackPressed();
        finishAffinity();
    }*/
}

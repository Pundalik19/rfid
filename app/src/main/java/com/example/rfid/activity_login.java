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
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import com.android.volley.DefaultRetryPolicy;
import com.android.volley.Request;
import com.android.volley.toolbox.StringRequest;
import com.android.volley.toolbox.Volley;

import org.json.JSONObject;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

public class activity_login extends AppCompatActivity {
    EditText etUsername, etPassword;
    Button btnLogin;
    dbclass db;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);

        db = new dbclass(this);

        etUsername = findViewById(R.id.etUsername);
        etPassword = findViewById(R.id.etPassword);
        btnLogin = findViewById(R.id.btnLogin);

        btnLogin.setOnClickListener(v -> login());
    }

    private void login() {

        String username = etUsername.getText().toString().trim();
        String password = etPassword.getText().toString().trim();

        // 🔴 Blank check
        if (username.isEmpty() || password.isEmpty()) {
            Toast.makeText(this, "Username & Password required", Toast.LENGTH_SHORT).show();
            return;
        }

        // 🔴 Internet check
        if (!isInternetAvailable()) {
            Toast.makeText(this, "No Internet Connection", Toast.LENGTH_SHORT).show();
            return;
        }

        if(isMobileLoginValid(username, password))
        {
            startActivity(new Intent(this, setup.class));
            finish();
        }else {
            authenticateFromServer(username, password);
        }
    }

    private boolean isInternetAvailable() {
        ConnectivityManager cm =
                (ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkInfo networkInfo = cm.getActiveNetworkInfo();
        return networkInfo != null && networkInfo.isConnected();
    }

    private void authenticateFromServer(String username, String password) {

        String url = "http://100.168.10.75:8003/api/mobilelogin";

        StringRequest request = new StringRequest(
                Request.Method.POST,
                url,
                response -> {
                    try {
                        JSONObject obj = new JSONObject(response);
                        boolean status = obj.optBoolean("status", false);
                        String message = obj.optString("message", "Unknown error");

                        Log.e("LOGIN_ERROR", obj+" ");
                        if (status) {

                            JSONObject data = obj.getJSONObject("data");

                            saveLogin(
                                    username,
                                    password,
                                    data.getString("u_id"),
                                    data.getString("location_id"),
                                    data.getString("status")
                            );

                            startActivity(new Intent(this, setup.class));
                            finish();

                        } else {
                            Toast.makeText(this, message, Toast.LENGTH_SHORT).show();
                        }
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                },
                error -> {
                    if (error.networkResponse != null && error.networkResponse.data != null) {
                        Log.e("LOGIN_ERROR", new String(error.networkResponse.data));
                    }
                    Toast.makeText(this, "Server error", Toast.LENGTH_SHORT).show();
                }
        ) {

            // 🔹 POST form parameters
            @Override
            protected Map<String, String> getParams() {
                String versioncode = getVersionCode();

                Map<String, String> params = new HashMap<>();
                params.put("login", username);
                params.put("pass", password);
                params.put("u_id", "");
                params.put("version", versioncode);
                return params;
            }

            // 🔹 HTTP headers
            @Override
            public Map<String, String> getHeaders() {
                Map<String, String> headers = new HashMap<>();
                headers.put("Authorization",
                        "Bearer Mzt7vkcoPnxkqZq6vFW6fxP3e61b66nSlYkWNETTUHiN7VL5P8GJSIvzcioq");
                headers.put("Accept", "application/json");
                return headers;
            }
        };

        request.setRetryPolicy(new DefaultRetryPolicy(
                15000,
                0,
                DefaultRetryPolicy.DEFAULT_BACKOFF_MULT
        ));

        Volley.newRequestQueue(this).add(request);
    }

    private void saveLogin(String login, String password, String u_id, String locationId,String status) {
        SQLiteDatabase dbw = db.getWritableDatabase();

        ContentValues cv = new ContentValues();
        cv.put("login", login);
        cv.put("password", password);
        cv.put("u_id", u_id);
        cv.put("location_id", locationId);
        cv.put("status", status);
        cv.put("created_at", getCurrentDateTime());

        dbw.insert("mobile_logins", null, cv);
    }
    public static String getCurrentDateTime() {
        return new SimpleDateFormat(
                "yyyy-MM-dd HH:mm:ss",
                Locale.getDefault()
        ).format(new Date());
    }

    private String getVersionCode() {
        try {
            PackageInfo pInfo = getPackageManager()
                    .getPackageInfo(getPackageName(), 0);

            if (android.os.Build.VERSION.SDK_INT >= 28) {
                return String.valueOf(pInfo.getLongVersionCode());
            } else {
                return String.valueOf(pInfo.versionCode);
            }
        } catch (PackageManager.NameNotFoundException e) {
            return "1";
        }
    }

    public boolean isMobileLoginValid(String username, String password) {

        dbclass dbs = new dbclass(this);
        SQLiteDatabase db = dbs.getReadableDatabase();

        Cursor c = null;
        try {
            String sql = "SELECT status FROM mobile_logins WHERE login = ? AND password = ?";
            c = db.rawQuery(sql, new String[]{username, password});

            if (c.moveToFirst()) {
                int status = c.getInt(c.getColumnIndexOrThrow("status"));

                if (status == 1) {
                    // ✅ active user
                    return true;
                } else {
                    // ❌ inactive user
                    Toast.makeText(this, "User account is inactive", Toast.LENGTH_LONG).show();
                    return false;
                }
            } else {
                // ❌ username/password incorrect
                Toast.makeText(this, "Invalid username or password", Toast.LENGTH_LONG).show();
                return false;
            }

        } finally {
            if (c != null) c.close();
            db.close();
        }
    }

}
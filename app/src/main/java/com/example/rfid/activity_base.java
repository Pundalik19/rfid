package com.example.rfid;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

import android.animation.ObjectAnimator;
import android.animation.ValueAnimator;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.ConnectivityManager;
import android.net.Network;
import android.net.NetworkCapabilities;
import android.net.NetworkRequest;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.TextView;

public class activity_base extends AppCompatActivity {
    TextView txtUsername, txtSync;
    ImageView imgSync, btnLogout;
    TextView txtNetwork;
    View networkDot;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // Load base layout ONLY here
        super.setContentView(R.layout.activity_base);

        txtUsername = findViewById(R.id.txtUsername);
        txtSync = findViewById(R.id.txtSync);
        imgSync = findViewById(R.id.imgSync);
        btnLogout = findViewById(R.id.btnLogout);

        txtNetwork = findViewById(R.id.txtNetwork);
        networkDot = findViewById(R.id.networkDot);

        monitorNetwork();

        dbclass dbcl = new dbclass(this);
        dbcl.trustEveryone();
        dbcl.saveUidAndLocationToPrefs(this);

        SharedPreferences prefs = getSharedPreferences("APP_PREFS", MODE_PRIVATE);
        String username = prefs.getString("login", "");

        if (txtUsername != null) {
            txtUsername.setText(username);
        }

        // Logout
        btnLogout.setOnClickListener(v -> {

            new AlertDialog.Builder(this)
                    .setTitle("Logout")
                    .setMessage("Are you sure you want to logout?")
                    .setIcon(R.drawable.ic_logout)
                    .setCancelable(false)

                    .setPositiveButton("Logout", (dialog, which) -> {

                        // Clear session
                        SharedPreferences prefss = getSharedPreferences("APP_PREFS", MODE_PRIVATE);
                        prefss.edit().clear().apply();

                        // Go to login screen
                        Intent intent = new Intent(this, activity_login.class);
                        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                        startActivity(intent);
                    })

                    .setNegativeButton("Cancel", (dialog, which) -> dialog.dismiss())

                    .show();
        });
    }

    @Override
    public void setContentView(int layoutResID) {
        FrameLayout frame = findViewById(R.id.contentFrame);
        LayoutInflater.from(this).inflate(layoutResID, frame, true);
    }

    // 🔄 Update Sync UI
    public void updateSyncUI(int progress, String status) {
        txtSync.setText(status + " " + progress + "%");

        if (status.equals("Syncing")) {
            startRotate(imgSync);
        } else {
            imgSync.clearAnimation();
        }
    }

    private void startRotate(View v) {
        ObjectAnimator rotate = ObjectAnimator.ofFloat(v, "rotation", 0f, 360f);
        rotate.setDuration(800);
        rotate.setRepeatCount(ValueAnimator.INFINITE);
        rotate.start();
    }

    private void monitorNetwork() {

        ConnectivityManager cm =
                (ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE);

        NetworkRequest request = new NetworkRequest.Builder().build();

        cm.registerNetworkCallback(request, new ConnectivityManager.NetworkCallback() {

            @Override
            public void onAvailable(Network network) {
                runOnUiThread(() -> setOnlineUI(true));
            }

            @Override
            public void onLost(Network network) {
                runOnUiThread(() -> setOnlineUI(false));
            }
        });
    }

    private void setOnlineUI(boolean isOnline) {

        if (isOnline) {
            txtNetwork.setText("Online");
            networkDot.setBackgroundResource(R.drawable.bg_dot_green);

        } else {
            txtNetwork.setText("Offline");
            networkDot.setBackgroundResource(R.drawable.bg_dot_red);

            // Optional: update sync text
            txtSync.setText("Waiting for network");
            imgSync.clearAnimation();
        }
    }


    public boolean isInternetAvailable() {
        ConnectivityManager cm =
                (ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE);

        NetworkCapabilities capabilities =
                cm.getNetworkCapabilities(cm.getActiveNetwork());

        return capabilities != null &&
                capabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET);
    }
}
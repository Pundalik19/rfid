package com.example.rfid;

import android.content.Context;
import android.util.Log;

import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.toolbox.StringRequest;
import com.android.volley.toolbox.Volley;

import org.json.JSONException;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class ApiSequenceRunner {

    private Context context;
    private List<String> apiList;
    private int currentIndex = 0;

    public interface ApiSequenceCallback {

        void onApiSuccess(String response, String tableName, int index) throws JSONException;

        void onError(String error);

        void onCompleted();
    }

    private ApiSequenceCallback callback;
    private List<String> responses = new ArrayList<>();

    public ApiSequenceRunner(Context context, List<String> apiList, ApiSequenceCallback callback) {
        this.context = context;
        this.apiList = apiList;
        this.callback = callback;
    }

    public void start() throws JSONException {
        currentIndex = 0;
        callNextApi();
    }

    private void callNextApi() {

        // ✅ STOP condition FIRST
        if (currentIndex >= apiList.size()) {
            callback.onCompleted(); // all APIs done
            return;
        }

        // ✅ Get current entry
        String fullUrl = apiList.get(currentIndex);

        // ✅ Split URL and table name
        String[] parts = fullUrl.split("#");
        String url = parts[0];
        String tableName = parts.length > 1 ? parts[1] : "";

        Log.e("API_CALL", "Calling → " + url + " | Table → " + tableName);

        RequestQueue queue = Volley.newRequestQueue(context);

        StringRequest request = new StringRequest(
                Request.Method.GET,
                url,
                response -> {

                    // ✅ Send response + table name immediately
                    try {
                        callback.onApiSuccess(response, tableName, currentIndex);
                    } catch (JSONException e) {
                        throw new RuntimeException(e);
                    }

                    // ✅ Move to next API
                    currentIndex++;
                    callNextApi(); // recursive call
                },
                error -> {
                    callback.onError(
                            "API failed at index " + currentIndex +
                                    " | Table: " + tableName +
                                    " | Error: " + error.toString()
                    );
                }
        ) {
            @Override
            public Map<String, String> getHeaders() {
                Map<String, String> headers = new HashMap<>();
                headers.put("Authorization",
                        "Bearer Mzt7vkcoPnxkqZq6vFW6fxP3e61b66nSlYkWNETTUHiN7VL5P8GJSIvzcioq");
                headers.put("Accept", "application/json");
                return headers;
            }
        };

        queue.add(request);
    }
}


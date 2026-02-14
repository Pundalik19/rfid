package com.example.rfid;

import androidx.appcompat.app.AppCompatActivity;

import android.content.ContentValues;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.os.Bundle;
import android.util.Base64;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

public class sync extends AppCompatActivity {
    dbclass db;
    Button syncdata;
    ProgressBar syncProgress;
    TextView syncStatus;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_sync);
        db = new dbclass(this);
        syncdata = findViewById(R.id.sync);
        syncProgress = findViewById(R.id.syncProgress);
        syncStatus = findViewById(R.id.syncStatus);
        /*syncdata.setOnClickListener(v -> {

            List<String> apis = new ArrayList<>();
            apis.add("http://100.168.10.75:8003/api/getassets#asset_masters");
            apis.add("http://100.168.10.75:8003/api/getlocations#locations");
            apis.add("http://100.168.10.75:8003/api/getsublocations#sublocations");
            apis.add("http://100.168.10.75:8003/api/getroutes#route_masters");
            apis.add("http://100.168.10.75:8003/api/getvendors#vendor_masters");
            apis.add("http://100.168.10.75:8003/api/getores#ore_masters");

            ApiSequenceRunner runner = new ApiSequenceRunner(
                    sync.this,
                    apis,
                    new ApiSequenceRunner.ApiSequenceCallback() {

                        @Override
                        public void onApiSuccess(String response, String tableName, int index) throws JSONException
                        {
                            Log.d("SUCCESS", "Index: " + index + " Table: " + tableName);
                            saveJsonToSQLite(response, tableName);
                        }

                        @Override
                        public void onError(String error) {
                            Log.e("API_ERROR", error);
                        }

                        @Override
                        public void onCompleted() {
                            Log.d("API", "ALL APIs COMPLETED");
                        }
                    }
            );

            try {
                runner.start();
            } catch (JSONException e) {
                throw new RuntimeException(e);
            }

            new Thread(() -> {

                JSONArray tripsheets = db.getTripsheetsForUpload(this);

                if (tripsheets.length() == 0) {
                    Log.d("UPLOAD", "No pending tripsheets");
                    return;
                }

                boolean successup = db.uploadTripsheets(tripsheets);

                if (successup) {
                    List<Long> uploadedIds = new ArrayList<>();

                    for (int i = 0; i < tripsheets.length(); i++) {
                        try {
                            uploadedIds.add(tripsheets.getJSONObject(i).getLong("id"));
                        } catch (JSONException e) {
                            e.printStackTrace();
                        }
                    }

                    db.markTripsheetsUploaded(uploadedIds,this);

                    runOnUiThread(() ->
                            Toast.makeText(this, "Tripsheets uploaded", Toast.LENGTH_SHORT).show()
                    );
                } else
                {
                    runOnUiThread(() ->
                            Toast.makeText(this, "Upload failed", Toast.LENGTH_SHORT).show()
                    );
                }

            }).start();

        });*/
        syncdata.setOnClickListener(v -> {

            syncdata.setEnabled(false);
            syncProgress.setVisibility(View.VISIBLE);
            syncProgress.setProgress(0);

            List<String> apis = new ArrayList<>();
            apis.add("http://100.168.10.75:8003/api/getassets#asset_masters");
            apis.add("http://100.168.10.75:8003/api/getlocations#locations");
            apis.add("http://100.168.10.75:8003/api/getsublocations#sublocations");
            apis.add("http://100.168.10.75:8003/api/getroutes#route_masters");
            apis.add("http://100.168.10.75:8003/api/getvendors#vendor_masters");
            apis.add("http://100.168.10.75:8003/api/getores#ore_masters");

            int totalApis = apis.size();

            ApiSequenceRunner runner = new ApiSequenceRunner(
                    sync.this,
                    apis,
                    new ApiSequenceRunner.ApiSequenceCallback() {

                        @Override
                        public void onApiSuccess(String response, String tableName, int index) throws JSONException {
                            saveJsonToSQLite(response, tableName);

                            int progress = ((index + 1) * 100) / totalApis;

                            runOnUiThread(() -> syncProgress.setProgress(progress));
                        }

                        @Override
                        public void onError(String error) {
                            runOnUiThread(() -> {
                                Toast.makeText(sync.this, "Download failed", Toast.LENGTH_SHORT).show();
                                syncProgress.setVisibility(View.GONE);
                                syncdata.setEnabled(true);
                            });
                        }

                        @Override
                        public void onCompleted() {
                            runOnUiThread(() -> syncProgress.setProgress(80)); // download done

                            startUpload(); // call upload after download
                        }
                    }
            );

            try {
                runner.start();
            } catch (JSONException e) {
                e.printStackTrace();
            }
        });
    }

    private void startUpload() {

        new Thread(() -> {

            JSONArray tripsheets = db.getTripsheetsForUpload(this);

            if (tripsheets.length() == 0) {
                runOnUiThread(() -> {
                    syncProgress.setProgress(100);
                    syncProgress.setVisibility(View.GONE);
                    syncdata.setEnabled(true);
                    Toast.makeText(this, "Nothing to upload", Toast.LENGTH_SHORT).show();
                });
                return;
            }

            boolean successup = db.uploadTripsheets(tripsheets);

            runOnUiThread(() -> {
                if (successup) {
                    syncProgress.setProgress(100);
                    Toast.makeText(this, "Tripsheets uploaded", Toast.LENGTH_SHORT).show();
                } else {
                    Toast.makeText(this, "Upload failed", Toast.LENGTH_SHORT).show();
                }

                syncProgress.setVisibility(View.GONE);
                syncdata.setEnabled(true);
            });

        }).start();
    }
    public void saveJsonToSQLite(String json,String tableNm) throws JSONException {


        JSONObject root = new JSONObject(json);
        JSONArray dataArray = root.getJSONArray("data");

        for (int i = 0; i < dataArray.length(); i++) {
            JSONObject item = dataArray.getJSONObject(i);
            insertDynamicData(db, tableNm, item);
        }
    }

    public void insertDynamicData(dbclass db,
                                  String tableName,
                                  JSONObject jsonObject) throws JSONException {

        SQLiteDatabase sqliteDb = db.getdb();
        ContentValues cv = new ContentValues();

        Iterator<String> keys = jsonObject.keys();

        boolean isFirstKey = true;
        String rowId = null;

        while (keys.hasNext()) {

            String key = keys.next();
            String val = jsonObject.optString(key, null);

            // FIRST COLUMN → ID (Base64 decoded)
            if (isFirstKey) {
                key = "id";

                if (val != null) {
                    byte[] decodedBytes = Base64.decode(val, Base64.DEFAULT);
                    val = new String(decodedBytes);
                }

                rowId = val;
                isFirstKey = false;
            }

            // Skip unwanted column
            if ("document_data".equals(key)) {
                continue;
            }

            // Add column dynamically if missing
            if (!isColumnExists(sqliteDb, tableName, key)) {
                Log.w("DB", "Adding missing column: " + key);
                addColumn(sqliteDb, tableName, key);
            }

            cv.put(key, val);
            Log.e("DATA", key + " = " + val);
        }

        // Ensure audit columns
        if (!isColumnExists(sqliteDb, tableName, "created_by")) {
            addColumn(sqliteDb, tableName, "created_by");
        }
        if (!isColumnExists(sqliteDb, tableName, "updated_by")) {
            addColumn(sqliteDb, tableName, "updated_by");
        }

        boolean exists = rowId != null && isRowExists(
                sqliteDb,
                tableName,
                "id = ?",
                new String[]{rowId}
        );

        if (exists) {
            cv.put("updated_by", 0);
            sqliteDb.update(
                    tableName,
                    cv,
                    "id = ?",
                    new String[]{rowId}
            );
            Log.e("DB", "UPDATED row with id = " + rowId);
        } else {
            cv.put("created_by", 0);
            cv.put("updated_by", 0);
            sqliteDb.insert(tableName, null, cv);
            Log.e("DB", "INSERTED row with id = " + rowId);
        }
    }

    private boolean isRowExists(SQLiteDatabase db,
                                String tableName,
                                String whereClause,
                                String[] whereArgs) {

        Cursor cursor = db.query(
                tableName,
                new String[]{"id"},
                whereClause,
                whereArgs,
                null,
                null,
                null
        );

        boolean exists = cursor.moveToFirst();
        cursor.close();
        return exists;
    }

    public boolean isColumnExists(SQLiteDatabase db, String tableName, String columnName) {

        Cursor cursor = null;
        try {
            cursor = db.rawQuery(
                    "PRAGMA table_info(" + tableName + ")",
                    null
            );

            if (cursor != null) {
                while (cursor.moveToNext()) {
                    String existingColumn = cursor.getString(
                            cursor.getColumnIndexOrThrow("name")
                    );

                    if (existingColumn.equalsIgnoreCase(columnName)) {
                        return true;
                    }
                }
            }
        } finally {
            if (cursor != null) cursor.close();
        }
        return false;
    }

    public void addColumn(SQLiteDatabase db,
                          String tableName,
                          String columnName) {

        String sql = "ALTER TABLE " + tableName +
                " ADD COLUMN " + columnName + " TEXT";

        db.execSQL(sql);
    }
}
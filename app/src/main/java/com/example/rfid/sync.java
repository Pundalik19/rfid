package com.example.rfid;

import androidx.appcompat.app.AppCompatActivity;
import androidx.cardview.widget.CardView;

import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
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

public class sync extends activity_base {
    dbclass db;
    Button syncdata;
    ProgressBar syncProgress;
    TextView syncStatus;
    View overlay;
    CardView syncDialog;

    TextView syncSubStatus;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_sync);
        db = new dbclass(this);
        syncdata = findViewById(R.id.sync);
        syncProgress = findViewById(R.id.syncProgress);
        syncStatus = findViewById(R.id.syncStatus);
        overlay = findViewById(R.id.overlay);
        syncDialog = findViewById(R.id.syncDialog);
        ProgressBar syncProgress = findViewById(R.id.syncProgress);
        syncStatus = findViewById(R.id.syncStatus);
        syncSubStatus = findViewById(R.id.syncSubStatus);


        syncdata.setOnClickListener(v -> {

            syncdata.setEnabled(false);
            showSyncDialog("Downloading data...");
            syncProgress.setIndeterminate(false);
            syncProgress.setProgress(0);

            synconlinedatabase(syncProgress,syncdata,sync.this);
        });
    }

    public void synconlinedatabase(ProgressBar syncProgress,Button syncdata,Context context)
    {
        List<String> apis = new ArrayList<>();
        apis.add(ApiConfig.GET_ASSETS);
        apis.add(ApiConfig.GET_LOCATIONS);
        apis.add(ApiConfig.GET_SUBLOCATIONS);
        apis.add(ApiConfig.GET_ROUTES);
        apis.add(ApiConfig.GET_VENDORS);
        apis.add(ApiConfig.GET_ORES);
        apis.add(ApiConfig.MACHINEWORKINGHOURS);
        apis.add(ApiConfig.MOBILE_LOGINS);

        int totalApis = apis.size();

        ApiSequenceRunner runner = new ApiSequenceRunner(
                context,
                apis,
                new ApiSequenceRunner.ApiSequenceCallback() {

                    @Override
                    public void onApiSuccess(String response, String tableName, int index) throws JSONException {

                        saveJsonToSQLite(response, tableName);

                        int progress = ((index + 1) * 60) / totalApis; // 0–60% for download

                        if(syncProgress!= null)
                        {
                            runOnUiThread(() -> {
                                syncProgress.setProgress(progress);
                                syncSubStatus.setText("Downloaded: " + tableName);
                            });
                        }
                    }

                    @Override
                    public void onError(String error) {
                        runOnUiThread(() -> {
                            hideSyncDialog();
                            //Toast.makeText(sync.this, "Download failed", Toast.LENGTH_SHORT).show();
                            if(syncProgress!= null)
                            {
                                db.showScrollableErrorDialog(context, "Error", "Download failed");
                                syncdata.setEnabled(true);
                            }
                        });
                    }

                    @Override
                    public void onCompleted() {
                        if(syncProgress!= null)
                        {
                            runOnUiThread(() -> {
                                syncProgress.setProgress(60);
                                syncSubStatus.setText("Uploading tripsheets...");
                            });
                        }

                        startUpload(); // call upload after download
                    }
                }
        );

        try {
            runner.start();
        } catch (JSONException e) {
            e.printStackTrace();
            hideSyncDialog();
            if(syncProgress!= null)
            {
                syncdata.setEnabled(true);
            }
        }
    }

    @Override
    public void onBackPressed() {
        super.onBackPressed();
        startActivity(new Intent(this, MainActivity.class));
        finish();
    }
    private void showSyncDialog(String msg) {
        overlay.setVisibility(View.VISIBLE);
        syncDialog.setVisibility(View.VISIBLE);
        syncStatus.setText(msg);
    }

    private void hideSyncDialog() {
        overlay.setVisibility(View.GONE);
        syncDialog.setVisibility(View.GONE);
    }

    private void startUpload() {

        new Thread(() -> {

            JSONArray tripsheets = db.getTripsheetsForUpload(this);
            JSONArray hsdTransactions = db.getHsdTransactions(this);

            if (tripsheets.length() == 0 && hsdTransactions.length() == 0) {
                runOnUiThread(() -> {
                    syncProgress.setProgress(100);
                    syncSubStatus.setText("No pending uploads");
                    finishSync(true);
                });
                return;
            }

            String successup_tripsheets = db.uploadTripsheets(tripsheets,ApiConfig.TRIPSHEET_SAVE,"tripsheets");
            String successup_item_issues = db.uploadTripsheets(hsdTransactions,ApiConfig.HSD_TRANSACTION_SAVE,"item_issues");
            runOnUiThread(() -> {
                if ("success".equals(successup_tripsheets) && "success".equals(successup_item_issues) )
                {
                    syncProgress.setProgress(100);
                    syncSubStatus.setText("Upload complete");
                    finishSync(true);
                } else {
                    if (!"success".equals(successup_tripsheets))
                    {
                        syncSubStatus.setText("Tripsheet upload failed\n");
                    }
                    if (!"success".equals(successup_item_issues))
                    {
                        syncSubStatus.append("HSD transactions upload failed");
                    }
                    Log.e("successup_tripsheets",successup_tripsheets+" "+successup_item_issues);
                    finishSync(false);
                }
            });

        }).start();
    }

    private void finishSync(boolean success) {
        hideSyncDialog();
        syncdata.setEnabled(true);

        /*Toast.makeText(
                this,
                success ? "Sync completed successfully" : "Sync failed",
                Toast.LENGTH_SHORT
        ).show();*/

        String title ="";
        if(success)
        {
            title = "Success";
        }else
        {
            title = "Error";
        }
        db.showScrollableErrorDialog(sync.this, title, success ? "Sync completed successfully" : "Sync failed");
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
                                  JSONObject jsonObject) throws JSONException
    {

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
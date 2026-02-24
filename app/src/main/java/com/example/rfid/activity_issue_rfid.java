package com.example.rfid;

import static com.example.rfid.dbclass.showScrollableErrorDialog;

import androidx.appcompat.app.AppCompatActivity;
import androidx.cardview.widget.CardView;

import android.app.AlertDialog;
import android.app.PendingIntent;
import android.content.ContentValues;
import android.content.Intent;
import android.content.IntentFilter;
import android.graphics.drawable.Drawable;
import android.nfc.NfcAdapter;
import android.nfc.Tag;
import android.nfc.tech.MifareUltralight;
import android.nfc.tech.NfcA;
import android.os.Bundle;
import android.provider.Settings;
import android.text.method.ScrollingMovementMethod;
import android.util.Log;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.AutoCompleteTextView;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

public class activity_issue_rfid extends AppCompatActivity {

    AutoCompleteTextView etVehicleNo;
    CardView cardDetails;
    TextView tvOwner, tvVendor, tvType, tvRFID,tvTareweight;
    Button btnIssue;

    dbclass dbc;
    List<Asset> assetList = new ArrayList<>();
    Asset selectedAsset;
    NfcAdapter nfcAdapter;
    PendingIntent pendingIntent;

    private IntentFilter[] filters;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_issue_rfid);

        etVehicleNo = findViewById(R.id.etVehicleNo);
        cardDetails = findViewById(R.id.cardDetails);
        tvOwner = findViewById(R.id.tvOwner);
        tvVendor = findViewById(R.id.tvVendor);
        tvType = findViewById(R.id.tvType);
        tvRFID = findViewById(R.id.tvRFID);
        tvTareweight = findViewById(R.id.tvTareweight);
        btnIssue = findViewById(R.id.btnIssue);

        dbc = new dbclass(activity_issue_rfid.this);


        nfcAdapter = NfcAdapter.getDefaultAdapter(this);
        if (nfcAdapter == null) {
            Log.e("nfcAdapter","NFC NOT Supported on this device!");
            //finish();
            return;
        }else
        {
            Log.e("nfcAdapter","NFC Supported on this device!");
        }
        if (!nfcAdapter.isEnabled()) {
            Log.e("nfcAdapter","Enable NFC from Settings");
            startActivity(new Intent(Settings.ACTION_NFC_SETTINGS));
        }else
        {
            Log.e("nfcAdapter","Enabled");
        }

        fetchAssetMaster();

        etVehicleNo.setOnItemClickListener((parent, view, position, id) -> {
            String vehicleNo = (String) parent.getItemAtPosition(position);

            for (Asset a : assetList) {
                if (vehicleNo.equalsIgnoreCase(a.registration_no)) {
                    selectedAsset = a;
                    break;
                }
            }

            if (selectedAsset != null) {
                showAssetDetails();
            }
        });

        btnIssue.setOnClickListener(v -> {
            if (selectedAsset == null) {
                toast("Select vehicle first");
                return;
            }

            if (selectedAsset.rfid_uid != null && !selectedAsset.rfid_uid.isEmpty()) {
                toast("RFID already issued. Apply new card.");
                return;
            }

            toast("Tap RFID card...");
            // Start NFC write flow
        });

        pendingIntent = PendingIntent.getActivity(
                this, 0,
                new Intent(this, getClass()).addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP),
                PendingIntent.FLAG_MUTABLE);

        IntentFilter tech = new IntentFilter(NfcAdapter.ACTION_TECH_DISCOVERED);
        filters = new IntentFilter[]{tech};


    }
    @Override
    protected void onPause() {
        super.onPause();
        nfcAdapter.disableForegroundDispatch(this);
    }

    // ---------------- API CALL ------------------
    private void fetchAssetMaster() {
        new Thread(() -> {
            try {
                URL url = new URL("http://mssiot.in/vmsb/api/getassets");
                HttpURLConnection conn = (HttpURLConnection) url.openConnection();
                conn.setRequestMethod("GET");
                conn.setRequestProperty("Authorization", "Bearer Mzt7vkcoPnxkqZq6vFW6fxP3e61b66nSlYkWNETTUHiN7VL5P8GJSIvzcioq");
                conn.setRequestProperty("Accept", "application/json");
                conn.setRequestProperty("Content-Type", "application/json");

                BufferedReader br = new BufferedReader(new InputStreamReader(conn.getInputStream()));
                StringBuilder json = new StringBuilder();
                String line;
                while ((line = br.readLine()) != null) json.append(line);

                Log.e("fetchAssetMaster"," "+json.toString());

                parseAssetMaster(json.toString());

            } catch (Exception e) {
                Log.e("fetchAssetMaster","Exception "+e.toString());
                runOnUiThread(() -> toast("API error"));
            }
        }).start();
    }

    private void parseAssetMaster(String json) throws Exception {

        JSONObject root = new JSONObject(json);
        JSONArray arr = root.getJSONArray("data");

        List<String> vehicleNos = new ArrayList<>();
        assetList.clear();

        for (int i = 0; i < arr.length(); i++) {
            JSONObject o = arr.getJSONObject(i);

            Asset a = new Asset();
            a.assetId = o.getString("assetId");
            a.registration_no = o.getString("registration_no");
            a.ownerName = o.getString("owner_name");
            a.tare_weight = o.optString("tare_weight", "--");
            a.rfid_uid = o.optString("rfid_uid", null);
            a.assetType = o.getString("asset_type");
            a.vendor_id = o.optInt("vendor_id", 0);

            assetList.add(a);
            vehicleNos.add(a.registration_no);
        }

        runOnUiThread(() -> {
            ArrayAdapter<String> adapter = new ArrayAdapter<>(
                    this,
                    android.R.layout.simple_dropdown_item_1line,
                    vehicleNos
            );
            etVehicleNo.setAdapter(adapter);
            etVehicleNo.setThreshold(1);
        });
    }

    private void showAssetDetails() {
        cardDetails.setVisibility(View.VISIBLE);

        tvOwner.setText("Owner: " + selectedAsset.ownerName);
        tvVendor.setText("Registration No: " + selectedAsset.registration_no);
        tvTareweight.setText("Tare Weight: " + selectedAsset.tare_weight);
        tvType.setText("Type: " + selectedAsset.assetType);

        if (selectedAsset.rfid_uid == null || selectedAsset.rfid_uid.equals("null") || selectedAsset.rfid_uid.isEmpty()) {
            tvRFID.setText("RFID: Not Issued");
            btnIssue.setEnabled(true);
        } else {
            tvRFID.setText("RFID: " + selectedAsset.rfid_uid);
            btnIssue.setEnabled(false);
            toast("RFID already issued. Apply new card.");
        }
    }

    // ---------------- AFTER RFID WRITE ------------------
    public void onRfidWritten(String uid) {
        selectedAsset.rfid_uid = uid;
        sendRfidToApi(uid);
    }

    private void sendRfidToApi(String rfid) {
        new Thread(() -> {
            try {
                URL url = new URL("https://your-api.com/assign-rfid");
                HttpURLConnection conn = (HttpURLConnection) url.openConnection();
                conn.setRequestMethod("POST");
                conn.setDoOutput(true);
                conn.setRequestProperty("Content-Type", "application/json");

                JSONObject body = new JSONObject();
                body.put("asset_id", selectedAsset.assetId);
                body.put("rfid", rfid);

                OutputStream os = conn.getOutputStream();
                os.write(body.toString().getBytes());
                os.close();

                BufferedReader br = new BufferedReader(new InputStreamReader(conn.getInputStream()));
                StringBuilder res = new StringBuilder();
                String line;
                while ((line = br.readLine()) != null) res.append(line);

                runOnUiThread(() -> showScrollableDialog("Server Response:\n\n" + res.toString()));

            } catch (Exception e) {
                runOnUiThread(() -> toast("RFID sync failed"));
            }
        }).start();
    }

    // ---------------- UI HELPERS ------------------
    private void showScrollableDialog(String msg) {
        AlertDialog dialog = new AlertDialog.Builder(this)
                .setTitle("Server Response")
                .setMessage(msg)
                .setPositiveButton("OK", null)
                .create();
        dialog.show();

        ((TextView) dialog.findViewById(android.R.id.message))
                .setMovementMethod(new ScrollingMovementMethod());
    }

    private void toast(String msg) {
        Toast.makeText(this, msg, Toast.LENGTH_SHORT).show();
    }

    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);
        Tag tag = intent.getParcelableExtra(NfcAdapter.EXTRA_TAG);
        byte[] uid = tag.getId();   // 🔥 RFID UID
        //String rfid_card = bytesToHex(uid);
        String asset_number = "";
        if (tag == null) return;

        String error = "";
        Log.e("NFC", "Tag detected");
        //clearUltralight(tag);


        NfcA nfca = NfcA.get(tag);

        try {

            nfca.connect();


            String asset = selectedAsset.registration_no;
            String assetId = selectedAsset.assetId;
            int vendor_id = selectedAsset.vendor_id;
            int status = 1;
            String now = null;
            now = dbc.db_format_date_time(new Date());
            long now_in_seconds = dbc.secondsFromBaseDate();


            dbc.writeStringToTag(tag, asset.substring(0, 4), 22, 0);  // GA04
            dbc.writeStringToTag(tag, asset.substring(4, 8), 23, 0);  // T123
            dbc.writeStringToTag(tag, asset.substring(8),    24, 0);
            dbc.writeOrFail(activity_issue_rfid.this,tag,12,dbc.stringTo4Bytes(String.valueOf(now_in_seconds)));

            Log.e("NFC", "Tag detected");
        } catch (Exception e) {
            e.printStackTrace();
            showScrollableErrorDialog(activity_issue_rfid.this, "Error", e.getMessage());
        } finally {
            try {
                nfca.close();
            } catch (Exception ignored) {
                showScrollableErrorDialog(activity_issue_rfid.this, "Error", ignored.getMessage());
            }
        }

    }
}
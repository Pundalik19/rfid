package com.example.rfid;

import static com.example.rfid.dbclass.showScrollableErrorDialog;

import androidx.appcompat.app.AppCompatActivity;
import androidx.cardview.widget.CardView;

import android.app.AlertDialog;
import android.app.PendingIntent;
import android.content.Intent;
import android.content.IntentFilter;
import android.nfc.NfcAdapter;
import android.nfc.Tag;
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
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

public class rfid_issue extends AppCompatActivity {

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

    String rfid_card_number="";

    boolean isReadyToWrite = false;
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

        dbc = new dbclass(rfid_issue.this);


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

        fetchAssetMaster("");

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
                //toast("RFID already issued.");
                //return;
            }

            toast("Tap RFID card...");
            isReadyToWrite = true;
        });

        pendingIntent = PendingIntent.getActivity(
                this, 0,
                new Intent(this, getClass()).addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP),
                PendingIntent.FLAG_MUTABLE);

        IntentFilter tech = new IntentFilter(NfcAdapter.ACTION_TECH_DISCOVERED);
        filters = new IntentFilter[]{tech};


    }

    @Override
    protected void onResume() {
        super.onResume();
        if (nfcAdapter != null) {
            nfcAdapter.enableForegroundDispatch(this, pendingIntent, null, null);
        }
    }

    @Override
    protected void onPause() {
        super.onPause();
        if (nfcAdapter != null) {
            nfcAdapter.disableForegroundDispatch(this);
        }
    }

    // ---------------- API CALL ------------------
    private void fetchAssetMaster(String assetid) {
        new Thread(() -> {
            try {
                URL url = new URL(ApiConfig.GET_ASSETS);
                HttpURLConnection conn = (HttpURLConnection) url.openConnection();
                conn.setRequestMethod("GET");
                conn.setRequestProperty("Authorization", "Bearer Mzt7vkcoPnxkqZq6vFW6fxP3e61b66nSlYkWNETTUHiN7VL5P8GJSIvzcioq");
                conn.setRequestProperty("Accept", "application/json");
                conn.setRequestProperty("Content-Type", "application/json");

                if(!"".equals(assetid))
                {
                    JSONObject body = new JSONObject();
                    body.put("asset_id", dbc.base64Encode(assetid));

                    OutputStream os = conn.getOutputStream();
                    os.write(body.toString().getBytes());
                    os.close();
                }

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
            a.assetId = dbc.base64Decode(o.getString("assetId"));
            a.registration_no = o.getString("registration_no");
            a.ownerName = o.getString("owner_name");
            a.tare_weight = o.optString("tare_weight", "");
            a.tare_weight_wb = o.optString("tare_weight_wb", "");
            a.rfid_uid = o.optString("rfid_uid", null);
            a.assetType = o.getString("asset_type");
            a.vendor_id = o.optInt("vendor_id");
            a.tare_weight_time = o.optString("tare_weight_time", "");
            a.hsd = o.getDouble("hsd_balance");
            a.gross_weight_capacity = o.optInt("gross_weight_capacity");
            Log.e("gross_weight_capacity","get "+a.gross_weight_capacity);
            JSONArray docs = o.optJSONArray("document_data");

            if (docs != null) {
                for (int j = 0; j < docs.length(); j++) {

                    JSONObject d = docs.getJSONObject(j);

                    String docName = d.optString("document_name");
                    String fromDate = d.optString("from_date");
                    String toDate = d.optString("to_date");
                    if (docName.equalsIgnoreCase("FITNESS")) {
                        a.fitness_from = fromDate;
                        a.fitness_to = toDate;
                    }

                    else if (docName.equalsIgnoreCase("PUC")) {
                        a.puc_from = fromDate;
                        a.puc_to = toDate;
                    }

                    else if (docName.equalsIgnoreCase("INSURANCE")) {
                        a.insurance_from = fromDate;
                        a.insurance_to = toDate;
                    }

                    else if (docName.equalsIgnoreCase("ROADTAX")) {
                        a.roadtax_from = fromDate;
                        a.roadtax_to = toDate;
                    }
                }
            }


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

        } else {
            tvRFID.setText("RFID: " + selectedAsset.rfid_uid);
            btnIssue.setEnabled(false);
            toast("RFID already issued. Apply new card.");
        }
        btnIssue.setEnabled(true);
    }

   private void clearfields()
   {
       cardDetails.setVisibility(View.GONE);
       tvOwner.setText("");
       tvVendor.setText("");
       tvTareweight.setText("");
       tvType.setText("");
       tvRFID.setText("");
   }

    // ---------------- AFTER RFID WRITE ------------------
    public void onRfidWritten(String uid) {
        selectedAsset.rfid_uid = uid;

    }

    private void sendRfidToApi(String rfid) {
        new Thread(() -> {
            try {
                URL url = new URL(ApiConfig.SAVEASSETDETAILS);
                HttpURLConnection conn = (HttpURLConnection) url.openConnection();
                conn.setRequestMethod("POST");
                conn.setDoOutput(true);
                conn.setRequestProperty("Authorization", "Bearer Mzt7vkcoPnxkqZq6vFW6fxP3e61b66nSlYkWNETTUHiN7VL5P8GJSIvzcioq");
                conn.setRequestProperty("Accept", "application/json");
                conn.setRequestProperty("Content-Type", "application/json");

                JSONObject body = new JSONObject();
                body.put("asset_id", dbc.base64Encode(selectedAsset.assetId));
                body.put("rfid_uid", rfid);

                OutputStream os = conn.getOutputStream();
                os.write(body.toString().getBytes());
                os.close();

                BufferedReader br = new BufferedReader(new InputStreamReader(conn.getInputStream()));
                StringBuilder res = new StringBuilder();
                String line;
                while ((line = br.readLine()) != null) res.append(line);
                Log.e("sendRfidToApi",res.toString());

                JSONObject jsonObject = new JSONObject(res.toString());

                boolean status = jsonObject.getBoolean("status");
                String message = jsonObject.getString("message");

                if (status) {

                    runOnUiThread(() -> {
                       dbc.showScrollableErrorDialog(rfid_issue.this, "Success", message);
                        clearfields();
                        fetchAssetMaster("");
                        showAssetDetails();

                    });

                } else {
                    runOnUiThread(() ->dbc.showScrollableErrorDialog(rfid_issue.this, "Error", message));
                }



            } catch (Exception e) {
                runOnUiThread(() -> toast("RFID sync failed"+e.getMessage()));
                Log.e("sendRfidToApi",e.getMessage()+e.toString());
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

        if(isReadyToWrite)
        {
            Tag tag = intent.getParcelableExtra(NfcAdapter.EXTRA_TAG);
            byte[] uid = tag.getId();   // 🔥 RFID UID
            rfid_card_number = dbc.bytesToHex(uid);
            String asset_number = "";
            if (tag == null) return;

            String error = "";
            Log.e("NFC", "Tag detected");

            NfcA nfca = NfcA.get(tag);

            try {

                nfca.connect();
                byte[] pageData = dbc.readPage(nfca, 04);
                String asset_num_04 = new String(pageData, StandardCharsets.UTF_8).trim();
                pageData = dbc.readPage(nfca, 05);
                String asset_num_05 = new String(pageData, StandardCharsets.UTF_8).trim();
                pageData = dbc.readPage(nfca, 06);
                String asset_num_06 = new String(pageData, StandardCharsets.UTF_8).trim();

                asset_number = "";//asset_num_04+asset_num_05+asset_num_06;

                if(asset_number.trim().isEmpty())
                {
                    String asset = selectedAsset.registration_no;
                    String assetId = selectedAsset.assetId;

                    Log.e("assetId",assetId+" "+selectedAsset.gross_weight_capacity);

                    int vendor_id = selectedAsset.vendor_id;
                    int status = 1;
                    int asset_type = 0;
                    if ("TRUCK".equals(selectedAsset.assetType)) {
                        asset_type = 01;
                    } else if ("MACHINE".equals(selectedAsset.assetType)) {
                        asset_type = 02;
                    } else if ("BARGE".equals(selectedAsset.assetType)) {
                        asset_type = 03;
                    } else if ("BOWSER".equals(selectedAsset.assetType)) {
                        asset_type = 04;
                    }

                    int hsdval = (int) (selectedAsset.hsd * 100);

                    String fitness_to = selectedAsset.fitness_to;
                    String puc_to = selectedAsset.puc_to;
                    String insurance_to = selectedAsset.insurance_to;
                    String roadtax_to = selectedAsset.roadtax_to;

                    String now = null;
                    now = dbc.db_format_date_time(new Date());
                    long now_in_seconds = dbc.secondsFromBaseDate();

                    Log.e("asset_number",asset+" ");
                    nfca.close();

                    dbc.writeStringToTag(tag, asset.substring(0, 4), 04, 0);  // GA04
                    dbc.writeStringToTag(tag, asset.substring(4, 8), 05, 0);  // T123
                    dbc.writeStringToTag(tag, asset.substring(8),    06, 0);

                    nfca.close();
                    //dbc.writeOrFail(rfid_issue.this, tag, 14, dbc.stringTo4Bytes(dbc.secondsFromTargetDate(selectedAsset.tare_weight_time))) &&
                     //       dbc.writeOrFail(rfid_issue.this, tag, 15, dbc.combineByteArrays(dbc.stringTo2Bytes(String.valueOf(selectedAsset.tare_weight_wb)), dbc.stringTo2Bytes(selectedAsset.tare_weight))) &&


                            Log.e("secondsFromTargetDate"," "+dbc.secondsFromTargetDate(selectedAsset.tare_weight_time));
                            if(asset_type==4)
                            {
                                //dbc.writeOrFail(rfid_issue.this, tag, 22, dbc.combineByteArrays(dbc.stringTo3Bytes(String.valueOf("15046")),dbc.stringTo1Bytes("")));
                            }

                    boolean ok = dbc.writeOrFail(rfid_issue.this, tag, 10, dbc.combineByteArrays(dbc.stringTo2Bytes(String.valueOf(vendor_id)), dbc.stringTo2Bytes("00"))) &&
                            dbc.writeOrFail(rfid_issue.this, tag, 11, dbc.combineByteArrays(dbc.stringTo3Bytes(String.valueOf(assetId)), dbc.stringTo1Bytes(String.valueOf(status)))) &&
                            dbc.writeOrFail(rfid_issue.this, tag, 12, dbc.stringTo4Bytes(String.valueOf(now_in_seconds))) &&
                            dbc.writeOrFail(rfid_issue.this, tag, 13, dbc.combineByteArrays(dbc.stringTo2Bytes(String.valueOf(asset_type)), dbc.stringTo2Bytes(String.valueOf(selectedAsset.gross_weight_capacity)))) &&
                            dbc.writeOrFail(rfid_issue.this, tag, 16, dbc.combineByteArrays(dbc.stringTo2Bytes(String.valueOf(dbc.daysFromBaseDateInt(insurance_to))), dbc.stringTo2Bytes(String.valueOf(dbc.daysFromBaseDateInt(roadtax_to))))) &&
                            dbc.writeOrFail(rfid_issue.this, tag, 17, dbc.combineByteArrays(dbc.stringTo2Bytes(String.valueOf(dbc.daysFromBaseDateInt(fitness_to))), dbc.stringTo2Bytes(String.valueOf(dbc.daysFromBaseDateInt(puc_to))))) &&
                            dbc.writeOrFail(rfid_issue.this, tag, 18, dbc.combineByteArrays(dbc.stringTo3Bytes(String.valueOf(hsdval)),dbc.stringTo1Bytes(""))) &&
                            dbc.writeOrFail(rfid_issue.this, tag, 25, dbc.combineByteArrays(dbc.stringTo2Bytes("00"), dbc.stringTo2Bytes(String.valueOf("00")))) &&
                            dbc.writeOrFail(rfid_issue.this, tag, 26, dbc.stringTo4Bytes("00")) &&
                            dbc.writeOrFail(rfid_issue.this, tag, 27, dbc.combineByteArrays(dbc.stringTo2Bytes("00"), dbc.stringTo2Bytes("00"))) &&
                            dbc.writeOrFail(rfid_issue.this, tag, 28, dbc.stringTo4Bytes("00")) &&
                            dbc.writeOrFail(rfid_issue.this, tag, 29, dbc.combineByteArrays(dbc.stringTo2Bytes("00"), dbc.stringTo2Bytes("00"))) &&
                            dbc.writeOrFail(rfid_issue.this, tag, 30, dbc.stringTo4Bytes("00")) &&
                            dbc.writeOrFail(rfid_issue.this, tag, 31, dbc.combineByteArrays(dbc.stringTo2Bytes("00"), dbc.stringTo2Bytes("00"))) &&
                            dbc.writeOrFail(rfid_issue.this, tag, 32, dbc.stringTo4Bytes("00")) &&
                            dbc.writeOrFail(rfid_issue.this, tag, 33, dbc.combineByteArrays(dbc.stringTo2Bytes("00"), dbc.stringTo2Bytes("00"))) &&
                            dbc.writeOrFail(rfid_issue.this, tag, 34, dbc.combineByteArrays(dbc.stringTo2Bytes("00"), dbc.stringTo2Bytes("00"))) &&
                            dbc.writeOrFail(rfid_issue.this, tag, 35, dbc.stringTo4Bytes("00")) &&
                            dbc.writeOrFail(rfid_issue.this, tag, 36, dbc.stringTo4Bytes(String.valueOf(00))) &&
                            dbc.writeOrFail(rfid_issue.this, tag, 37, dbc.combineByteArrays(dbc.stringTo2Bytes(String.valueOf(00)), dbc.stringTo2Bytes("00")));

                    if (ok) {
                        sendRfidToApi(rfid_card_number);
                    }else
                    {
                       dbc.showScrollableErrorDialog(rfid_issue.this, "Error", "Failed to write card.");
                    }
                }else
                {
                   dbc.showScrollableErrorDialog(rfid_issue.this, "Error", "This RFID card is already issued to the vehicle number\n"+asset_number.trim()+"\nIf you want to reuse it again then please clear the card first and then use this option to rewrite the card again.");
                }
                Log.e("NFC", "Tag detected");
            } catch (Exception e) {
                e.printStackTrace();
               dbc.showScrollableErrorDialog(rfid_issue.this, "Error", e.getMessage());
            } finally
            {
                try
                {
                    //nfca.close();
                } catch (Exception ignored)
                {
                   dbc.showScrollableErrorDialog(rfid_issue.this, "Error", ignored.getMessage());
                }
            }
        }

    }
}
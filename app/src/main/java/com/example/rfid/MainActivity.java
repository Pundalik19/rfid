package com.example.rfid;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.cardview.widget.CardView;

import android.annotation.SuppressLint;
import android.app.AlertDialog;
import android.app.PendingIntent;
import android.app.ProgressDialog;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;

import java.lang.reflect.Array;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Calendar;
import java.text.SimpleDateFormat;
import java.util.Locale;

import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.ColorDrawable;
import android.graphics.drawable.Drawable;
import android.nfc.NdefMessage;
import android.nfc.NdefRecord;
import android.nfc.NfcAdapter;
import android.nfc.Tag;
import android.nfc.tech.IsoDep;
import android.nfc.tech.MifareUltralight;
import android.nfc.tech.Ndef;
import android.nfc.tech.NfcA;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.os.Parcelable;
import android.os.Vibrator;
import android.provider.Settings;
import android.util.Base64;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.AnimationUtils;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.ByteArrayOutputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.security.SecureRandom;
import java.security.cert.X509Certificate;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.TimeZone;
import java.util.concurrent.TimeUnit;

import javax.net.ssl.HostnameVerifier;
import javax.net.ssl.HttpsURLConnection;
import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLSession;
import javax.net.ssl.X509TrustManager;

import okhttp3.Call;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;

public class MainActivity extends AppCompatActivity {
    Button syncbutton;

    int trip_status_read;
    boolean gohead = false;
    NfcAdapter nfcAdapter;
    PendingIntent pendingIntent;

    dbclass dbcl;
    private IntentFilter[] filters;
    private String[][] techList;
    boolean clearRequested = false;
    boolean writeRequested = false;
    String dataToWrite = "";
    TextView readtext;
    TextView inputtext;
    long locationId;
    long sublocationId;
    long vendorId;
    long descriptionId;
    long destinationId;
    long routeId;

    String mobileuid ="";
    String TID ="";

    LinearLayout background_main;

    TextView tvRfid,tvTruckNo,tvVendor;
    Handler handler;
    Runnable hideRunnable;
    CardView cardDetails;
    LinearLayout detailsContainer;

    private static AlertDialog currentDialog;
    @SuppressLint("MissingInflatedId")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        dbcl = new dbclass(MainActivity.this);

        if (dbcl.isMobileLoginEmpty(MainActivity.this)) {
            Intent intent = new Intent(this, activity_login.class);
            startActivity(intent);
            finish();
            return;
        }



        dbcl.saveUidAndLocationToPrefs(MainActivity.this);

        SharedPreferences prefs = getSharedPreferences("APP_PREFS", MODE_PRIVATE);

        mobileuid = prefs.getString("UID", "");
        TID = prefs.getString("TID", "");
        String locationIdsp = prefs.getString("LOCATION_ID", "");

        setContentView(R.layout.activity_main);

        nfcAdapter = NfcAdapter.getDefaultAdapter(this);

        Button btnSetup = findViewById(R.id.btnSetup);
        Button btnIssuerfid = findViewById(R.id.btnIssuerfid);
        Button btnExit = findViewById(R.id.btnExit);
        syncbutton = findViewById(R.id.btnSync);

        background_main = findViewById(R.id.background_main);
        cardDetails = findViewById(R.id.cardDetails);

        handler = new Handler(Looper.getMainLooper());
        hideRunnable = () -> cardDetails.setVisibility(View.GONE);
        detailsContainer = findViewById(R.id.detailsContainer);



        btnSetup.setOnClickListener(v ->
                startActivity(new Intent(this, activity_login.class)));
        syncbutton.setOnClickListener(v ->
                startActivity(new Intent(this, sync.class)));
        btnIssuerfid.setOnClickListener(v ->
                startActivity(new Intent(this, activity_issue_rfid.class)));

        btnExit.setOnClickListener(v -> finish());

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

        pendingIntent = PendingIntent.getActivity(
                this, 0,
                new Intent(this, getClass()).addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP),
                PendingIntent.FLAG_MUTABLE);

        IntentFilter tech = new IntentFilter(NfcAdapter.ACTION_TECH_DISCOVERED);
        filters = new IntentFilter[]{tech};

        techList = new String[][]{
                new String[]{
                        NfcA.class.getName(),
                        MifareUltralight.class.getName(),
                }
        };
        Cursor cursor = dbcl.getSetupnames();  // your existing query
        showSetupDetails(cursor,this);

    }

    private void onRfidTapped(ContentValues values,String error) {

        handler.removeCallbacks(hideRunnable);

        detailsContainer.removeAllViews();

        for (Map.Entry<String, Object> entry : values.valueSet())
        {

            String key = entry.getKey();
            Object valObj = entry.getValue();
            String value = valObj == null ? "--" : String.valueOf(valObj);

            View row = dbcl.createRow(MainActivity.this,key, value,error);
            detailsContainer.addView(row);
        }

        /*if (!error.trim().isEmpty())
        {

            TextView errorView = new TextView(this);
            errorView.setText(error);
            errorView.setTextColor(Color.WHITE);
            errorView.setTextSize(18);
            errorView.setPadding(16, 16, 16, 16);

            detailsContainer.addView(errorView);

            cardDetails.setCardBackgroundColor(Color.parseColor("#D32F2F")); // red
        }else {
            cardDetails.setCardBackgroundColor(Color.WHITE); // or your normal color
        }*/
        cardDetails.startAnimation(
                AnimationUtils.loadAnimation(this, android.R.anim.slide_in_left)
        );
        cardDetails.setVisibility(View.VISIBLE);
        handler.postDelayed(hideRunnable, 5 * 60 * 1000);
    }
    @Override
    protected void onResume() {
        super.onResume();
        nfcAdapter.enableForegroundDispatch(this, pendingIntent, null, null);

        dbclass db = new dbclass(this);
        Cursor cursor = db.getSetupData();

        if (cursor != null && cursor.moveToFirst()) {

            locationId = cursor.getLong(cursor.getColumnIndexOrThrow("location_id"));
            sublocationId = cursor.getLong(cursor.getColumnIndexOrThrow("sublocation_id"));
            vendorId = cursor.getLong(cursor.getColumnIndexOrThrow("vendor_id"));
            descriptionId = cursor.getLong(cursor.getColumnIndexOrThrow("description_id"));
            destinationId = cursor.getLong(cursor.getColumnIndexOrThrow("destination_id"));
            routeId = cursor.getLong(cursor.getColumnIndexOrThrow("route_id"));
            Log.e("setup"," locationId "+locationId+" sublocationId "+sublocationId+" vendorId "+vendorId+" descriptionId "+descriptionId+" destinationId "+destinationId+" routeId "+routeId);
            cursor.close();
        }

        new Thread(() -> {

            JSONArray tripsheets = dbcl.getTripsheetsForUpload(this);

            if (tripsheets.length() == 0) {
                Log.d("UPLOAD", "No pending tripsheets");
                return;
            }

            boolean successup = dbcl.uploadTripsheets(tripsheets);

            if (successup) {
                List<Long> uploadedIds = new ArrayList<>();

                for (int i = 0; i < tripsheets.length(); i++) {
                    try {
                        uploadedIds.add(tripsheets.getJSONObject(i).getLong("id"));
                    } catch (JSONException e) {
                        e.printStackTrace();
                    }
                }

                dbcl.markTripsheetsUploaded(uploadedIds,MainActivity.this);

                runOnUiThread(() ->
                        Toast.makeText(this, "Tripsheets uploaded", Toast.LENGTH_SHORT).show()
                );
            } else {
                runOnUiThread(() ->
                        Toast.makeText(this, "Upload failed", Toast.LENGTH_SHORT).show()
                );
            }

        }).start();

    }

    @Override
    protected void onPause() {
        super.onPause();
        nfcAdapter.disableForegroundDispatch(this);
    }


    public long getOreMasterId(String name, String type, String size,
                               String grade, String description)
    {
        dbclass dbs = new dbclass(this);
        SQLiteDatabase db = dbs.getReadableDatabase();
        long oreMasterId = -1;

        Cursor cursor = db.rawQuery(
                "SELECT id FROM ore_masters WHERE name=? AND type=? AND size=? AND grade=? AND description=? LIMIT 1",
                new String[]{name, type, size, grade, description}
        );

        if (cursor.moveToFirst()) {
            oreMasterId = cursor.getLong(0);
        }

        cursor.close();
        return oreMasterId;
    }

    private int extractId(String text) {
        return Integer.parseInt(text.split("-")[0]);
    }
    @Override
    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);
        Tag tag = intent.getParcelableExtra(NfcAdapter.EXTRA_TAG);
        byte[] uid = tag.getId();   // 🔥 RFID UID
        String rfid_card = dbcl.bytesToHex(uid);
        String asset_number ="";
        if(tag == null) return;
        writeRequested = true;
        String error ="";
        Log.e("NFC","Tag detected");
        //clearUltralight(tag);
        Drawable originalBg = background_main.getBackground();
        ContentValues card_details = new ContentValues();
        if(writeRequested)
        {

            NfcA nfca = NfcA.get(tag);

            try {

                nfca.connect();
                //long seconds = bytesToSeconds(pageData);

                /***************READ CARD START ******************/
                String currentDateMONTH="";
                String monthyear="";
                String monthyearfull="";

                currentDateMONTH = new SimpleDateFormat("yyMM", Locale.getDefault()).format(new Date());
                monthyear = new SimpleDateFormat("dd", Locale.getDefault())
                        .format(new Date());
                monthyearfull = new SimpleDateFormat("yyyy", Locale.getDefault())
                        .format(new Date());
                Log.e("currentDateMONTH",currentDateMONTH+" ");

                String mob_id=mobileuid;
                String trip_status;
                String trip_status_save;
                String tripsrno_org = getNextTripSheetNo();

                String part1 = tripsrno_org.substring(0, 2);
                String part2 = tripsrno_org.substring(2, 4);

                String tripstarttime =  dbcl.db_format_date_time(new Date());
                String srctime =  dbcl.db_format_date_time(new Date());
                long seconds =  dbcl.secondsFromBaseDate();
                String tripsheetno = mob_id+currentDateMONTH+monthyear+tripsrno_org;
                String src_mob=TID;

                byte[] pageData = dbcl.readPage(nfca, 04);
                String asset_num_04 = new String(pageData, StandardCharsets.UTF_8).trim();
                pageData = dbcl.readPage(nfca, 05);
                String asset_num_05 = new String(pageData, StandardCharsets.UTF_8).trim();
                pageData = dbcl.readPage(nfca, 06);
                String asset_num_06 = new String(pageData, StandardCharsets.UTF_8).trim();

                asset_number = asset_num_04+asset_num_05+asset_num_06;
                Log.d("NFC_READ", "040506 : asset number "+asset_num_04+" "+asset_num_05+" "+asset_num_06);

                String truckid = dbcl.getAssetId_fromdb(asset_number);

                pageData = dbcl.readPage(nfca, 25);
                String tripsheet_no_last2  = new String(pageData, 0, 2);//bytesToString(pageData, 0, 2);
                trip_status_read  = Integer.parseInt(dbcl.bytesToString(pageData, 2, 2));

                Log.d("NFC_READ", "trip_status_read pageData"+trip_status_read+ " "+Arrays.toString(pageData)+" tripsheet_no_last2 "+tripsheet_no_last2);


                pageData = dbcl.readPage(nfca, 34);
                int src_mob_id_card  = Integer.parseInt(dbcl.bytesToString(pageData, 0, 2));

                ContentValues trip_close = new ContentValues();
                String now = null;

                now =  dbcl.db_format_date_time(new Date());

                pageData = dbcl.readPage(nfca, 14);
                int tare_wt_sec_card  = Integer.parseInt(dbcl.bytesToString(pageData, 0, 4));
                String tare_wt_time_card;
                Log.e("tare_wt_sec_card"," "+tare_wt_sec_card);
                if(tare_wt_sec_card > 0)
                {
                    tare_wt_time_card =  dbcl.db_format_date_time(dbcl.date_time_FromSeconds(tare_wt_sec_card));

                    Date todaysdatetm = new Date();

                    long diffMillis = todaysdatetm.getTime() - dbcl.date_time_FromSeconds(tare_wt_sec_card).getTime();
                    long diffDays = TimeUnit.MILLISECONDS.toDays(diffMillis);
                    if (diffDays > 30) {
                        error += "TARE WEIGHT WAS TAKEN "+diffDays+" DAYS BEFORE(VALID FOR 30 DAYS). KINDLY GET THE TARE WEIGHT DONE.\n";
                    }
                }else
                {
                    tare_wt_time_card = null;
                    error += "TARE WEIGHT TIME NOT FOUND\n";
                }

                pageData = dbcl.readPage(nfca, 15);
                int tare_wt_wb_log_card = Integer.parseInt(dbcl.bytesToString(pageData, 0, 2));
                int tare_wt_card = Integer.parseInt(dbcl.bytesToString(pageData, 2, 2));




                pageData = dbcl.readPage(nfca, 35);
                int dest_sec_card  = Integer.parseInt(dbcl.bytesToString(pageData, 0, 4));
                String dest_time_card = null;
                Log.e("dest_sec_card"," "+dest_sec_card);
                card_details.put("Card No",rfid_card);
                card_details.put("Vehicle No",asset_number);
                card_details.put("Src Tare Weight",tare_wt_card);
                card_details.put("Src Tare Weight Time", dbcl.db_format_date_time(dbcl.date_time_FromSeconds(tare_wt_sec_card)));

                if(trip_status_read == 1) //trip open
                {

                    pageData = dbcl.readPage(nfca, 18);
                    int HSD = Integer.parseInt(dbcl.bytesToString(pageData, 0, 3));

                    String HSD_BAL = String.valueOf(HSD/100);

                    Log.e("HSD_BAL"," "+HSD_BAL);

                    String closing_trip_num_card ="";
                    pageData = dbcl.readPage(nfca, 22);
                    Log.d("NFC_READ", "22  "+Arrays.toString(pageData));
                    String closing_trip_num_01 = new String(pageData, StandardCharsets.UTF_8).trim();
                    pageData = dbcl.readPage(nfca, 23);
                    Log.d("NFC_READ", "23  "+Arrays.toString(pageData));
                    String closing_trip_num_02 = new String(pageData, StandardCharsets.UTF_8).trim();
                    pageData = dbcl.readPage(nfca, 24);
                    Log.d("NFC_READ", "24  "+Arrays.toString(pageData));
                    String closing_trip_num_03 = new String(pageData, StandardCharsets.UTF_8).trim();


                    closing_trip_num_card = closing_trip_num_01+closing_trip_num_02+closing_trip_num_03+tripsheet_no_last2;
                    Log.d("NFC_READ", "closing_trip_num_card "+closing_trip_num_card+" ");

                    card_details.put("Trip No",closing_trip_num_card);


                    pageData = dbcl.readPage(nfca, 27);
                    int desc_id_card = Integer.parseInt(dbcl.bytesToString(pageData, 0, 2));
                    int route_id_card = Integer.parseInt(dbcl.bytesToString(pageData, 2, 2));
                    int src_subloc_id_trip = Integer.parseInt(dbcl.get_subloc_from_routeid(route_id_card,"source_sublocation"));//fetching src sublocation
                    int dest_subloc_id_trip = Integer.parseInt(dbcl.get_subloc_from_routeid(route_id_card,"destination_sublocation"));//fetching desr sublocation
                    Log.d("NFC_READ", "dest_subloc_id_trip "+dest_subloc_id_trip+" "+sublocationId);

                    card_details.put("Source Location",dbcl.get_subloc_name(src_subloc_id_trip));
                    card_details.put("Destination Location",dbcl.get_subloc_name(dest_subloc_id_trip));
                    card_details.put("Route",dbcl.get_route_name(route_id_card));

                    pageData = dbcl.readPage(nfca, 28);
                    int wb_src_sec_card  = Integer.parseInt(dbcl.bytesToString(pageData, 0, 4));
                    String wb_src_time_card;
                    if(wb_src_sec_card > 0)
                    {
                        wb_src_time_card =  dbcl.db_format_date_time(dbcl.date_time_FromSeconds(wb_src_sec_card));
                    } else {
                        wb_src_time_card = null;
                    }

                    pageData = dbcl.readPage(nfca, 29);
                    int src_gr_wt_card = Integer.parseInt(dbcl.bytesToString(pageData, 0, 2));
                    int wb_src_gr_login_card = Integer.parseInt(dbcl.bytesToString(pageData, 2, 2));

                    pageData = dbcl.readPage(nfca, 30);
                    int wb_dest_sec_card  = Integer.parseInt(dbcl.bytesToString(pageData, 0, 4));
                    String wb_dest_time_card;
                    if(wb_dest_sec_card > 0)
                    {
                        wb_dest_time_card =  dbcl.db_format_date_time(dbcl.date_time_FromSeconds(wb_dest_sec_card));
                    } else {
                        wb_dest_time_card = null;
                    }

                    pageData = dbcl.readPage(nfca, 31);
                    int dest_gr_wt_card = Integer.parseInt(dbcl.bytesToString(pageData, 0, 2));
                    int wb_dest_gr_login_card = Integer.parseInt(dbcl.bytesToString(pageData, 2, 2));

                    pageData = dbcl.readPage(nfca, 32);
                    int dest_tare_wt_sec_card  = Integer.parseInt(dbcl.bytesToString(pageData, 0, 4));
                    String dest_tare_wt_time_card;
                    if(dest_tare_wt_sec_card > 0)
                    {
                        dest_tare_wt_time_card =  dbcl.db_format_date_time(dbcl.date_time_FromSeconds(dest_tare_wt_sec_card));
                    } else {
                        dest_tare_wt_time_card = null;
                    }

                    pageData = dbcl.readPage(nfca, 33);
                    int dest_tare_wt_card = Integer.parseInt(dbcl.bytesToString(pageData, 0, 2));
                    int wb_dest_tare_login_card = Integer.parseInt(dbcl.bytesToString(pageData, 2, 2));

                    pageData = dbcl.readPage(nfca, 36);
                    int src_sec_card  = Integer.parseInt(dbcl.bytesToString(pageData, 0, 4));
                    String src_time_card;
                    if(src_sec_card > 0)
                    {
                        src_time_card =  dbcl.db_format_date_time(dbcl.date_time_FromSeconds(src_sec_card));

                    } else {
                        src_time_card = null;
                    }

                    pageData = dbcl.readPage(nfca, 37);
                    int vendor_id_card  = Integer.parseInt(dbcl.bytesToString(pageData, 0, 2));
                    //dest_subloc_id_trip= (int) sublocationId;
                    if(dest_subloc_id_trip==sublocationId && "".equals(error))
                    {

                        nfca.close();
                        trip_status= "02";
                        trip_status_save="CLOSED";

                        Log.e("stringTo2Bytes",Arrays.toString(dbcl.stringTo2Bytes(trip_status)));

                        ProgressDialog dialog = new ProgressDialog(this);
                        dialog.setMessage("Writing card...");
                        dialog.setCancelable(false);
                        dialog.show();

                        Vibrator vibrator = (Vibrator) getSystemService(Context.VIBRATOR_SERVICE);


                        String finalClosing_trip_num_card = closing_trip_num_card;
                        String finalAsset_number = asset_number;
                        String finalNow = now;
                        new Thread(() -> {

                            boolean success =
                                    dbcl.writeOrFail(this,tag,25, dbcl.combineByteArrays("70".getBytes(StandardCharsets.UTF_8),dbcl.stringTo2Bytes(trip_status))) &&
                                            dbcl.writeOrFail(this,tag, 34,dbcl.combineByteArrays(dbcl.stringTo2Bytes(String.valueOf(src_mob_id_card)), dbcl.stringTo2Bytes(TID))) &&
                                            dbcl.writeOrFail(this,tag, 35,dbcl.stringTo4Bytes(String.valueOf(seconds)));
                            runOnUiThread(() -> {
                                dialog.dismiss();

                                if (success) {
                                    vibrator.vibrate(100);
                                    Toast.makeText(this, "Card written successfully ✅", Toast.LENGTH_SHORT).show();

                                    trip_close.put("tripsheet_no", finalClosing_trip_num_card);
                                    trip_close.put("trip_type", "RFID");
                                    trip_close.put("initial_route_id", String.valueOf(route_id_card));
                                    trip_close.put("final_route_id", String.valueOf(route_id_card));
                                    trip_close.put("rfid_id", rfid_card);
                                    trip_close.put("truck_id", truckid);
                                    trip_close.put("truck_no", finalAsset_number);
                                    trip_close.put("vendor_id", String.valueOf(vendor_id_card));
                                    trip_close.put("ore_id", String.valueOf(desc_id_card));
                                    trip_close.put("status", trip_status_save);
                                    trip_close.put("src_time", src_time_card);
                                    trip_close.put("src_mobile", String.valueOf(src_mob_id_card));
                                    trip_close.put("dest_time", srctime);
                                    trip_close.put("dest_mobile", src_mob);
                                    trip_close.put("created_at", finalNow);
                                    trip_close.put("wb_src_time", wb_src_time_card);
                                    trip_close.put("wb_src_gross_login", wb_src_gr_login_card);
                                    trip_close.put("src_tare_wt_time", tare_wt_time_card);
                                    trip_close.put("wb_src_tare_login", tare_wt_wb_log_card);
                                    trip_close.put("wb_dest_time", wb_dest_time_card);
                                    trip_close.put("wb_dest_gross_login", wb_dest_gr_login_card);
                                    trip_close.put("dest_tare_wt_time", dest_tare_wt_time_card);
                                    trip_close.put("wb_dest_tare_login", wb_dest_tare_login_card);
                                    trip_close.put("src_gross_wt", src_gr_wt_card);
                                    trip_close.put("src_tare_wt", tare_wt_card);
                                    trip_close.put("dest_gross_wt", dest_gr_wt_card);
                                    trip_close.put("dest_tare_wt", dest_tare_wt_card);
                                    trip_close.put("hsd_bal", HSD_BAL);

                                    if (saveTripsheet(finalClosing_trip_num_card,trip_close))
                                    {
                                        Toast.makeText(this, "Tripsheet closed successfully", Toast.LENGTH_SHORT).show();
                                        showScrollableErrorDialog(MainActivity.this,"Success", "Tripsheet closed successfully");
                                        /*background_main.setBackgroundColor(Color.GREEN);

                                        new Handler(Looper.getMainLooper()).postDelayed(() -> {
                                            background_main.setBackground(originalBg);
                                        }, 5000);*/
                                        trip_status_read = 2;
                                    } else
                                    {
                                        Toast.makeText(this, "Failed to close tripsheet", Toast.LENGTH_SHORT).show();
                                        //background_main.setBackgroundColor(Color.RED);
                                        showScrollableErrorDialog(MainActivity.this,"Error", "Failed to close tripsheet");
                                        /*new Handler(Looper.getMainLooper()).postDelayed(() -> {
                                            background_main.setBackground(originalBg);
                                        }, 5000);*/
                                    }
                                }

                            });

                        }).start();

                    }else
                    {
                        trip_status_save = "OPEN";

                        error += "WRONG DESTINATION\nPROCEED TO :- \n\n"+dbcl.get_subloc_name(dest_subloc_id_trip);

                        //background_main.setBackgroundColor(Color.RED);
                        showScrollableErrorDialog(MainActivity.this,"Error",error);
                        /*new Handler(Looper.getMainLooper()).postDelayed(() -> {
                            background_main.setBackground(originalBg);
                        }, 5000);*/
                    }
                    nfca.close();

                }else
                {
                    trip_status_save = "OPEN";
                    trip_status = "01";

                    Log.e("dest_sec_card",dest_sec_card+" ");

                    if(dest_sec_card > 0)
                    {
                        Date desttimedate = dbcl.date_time_FromSeconds(dest_sec_card);

                        error += dbcl.checktime(desttimedate);

                    }

                    pageData = dbcl.readPage(nfca, 11);
                    String Asset_id  = dbcl.bytesToString(pageData, 0, 3);
                    int asset_status = Integer.parseInt(dbcl.bytesToString(pageData, 3, 1));

                    Log.d("NFC_READ", "11 : asset number "+Asset_id+" "+asset_status);

                    if(asset_status==0)
                    {
                        error += "VEHICLE IS INACTIVE\n";
                    }

                    pageData = dbcl.readPage(nfca, 16);
                    int ins_val_seconds  = Integer.parseInt(dbcl.bytesToString(pageData, 0, 2));
                    int rdt_val_seconds = Integer.parseInt(dbcl.bytesToString(pageData, 2, 2));

                    Date insurance_validity = null;
                    Date rdtax_validity = null;

                    Calendar todayCal = Calendar.getInstance(TimeZone.getTimeZone("Asia/Kolkata"));
                    Date today = todayCal.getTime();

                    if(ins_val_seconds > 0)
                    {
                        insurance_validity = dbcl.dateFromDays(ins_val_seconds);
                        if(insurance_validity.before(today))
                        {
                            error += "INSURANCE VALIDITY IS OVER\n";
                        }
                    }else
                    {
                        error += "INSURANCE VALIDITY DATE NOT FOUND\n";
                    }
                    Log.d("NFC_READ", "25 : ins_val_seconds "+ins_val_seconds+" insurance_validity"+insurance_validity);

                    if(rdt_val_seconds > 0)
                    {
                        rdtax_validity = dbcl.dateFromDays(rdt_val_seconds);
                        if(rdtax_validity.before(today))
                        {
                            error += "ROAD TAX VALIDITY IS OVER\n";
                        }
                    }else
                    {
                        error += "ROAD TAX VALIDITY DATE NOT FOUND\n";
                    }

                    Log.d("NFC_READ", "25 : rdt_val_seconds "+rdt_val_seconds+" rdtax_validity"+rdtax_validity);

                    pageData = dbcl.readPage(nfca, 17);
                    int fit_val_seconds  = Integer.parseInt(dbcl.bytesToString(pageData, 0, 2));
                    int puc_val_seconds = Integer.parseInt(dbcl.bytesToString(pageData, 2, 2));

                    Date fitness_validity = null;
                    Date puc_validity = null;

                    if(fit_val_seconds > 0)
                    {
                        fitness_validity = dbcl.dateFromDays(fit_val_seconds);
                        if(fitness_validity.before(today))
                        {
                            error += "FITNESS VALIDITY IS OVER\n";
                        }
                    }else
                    {
                        error += "FITNESS VALIDITY DATE NOT FOUND\n";
                    }

                    Log.d("NFC_READ", "25 : fit_val_seconds "+fit_val_seconds+" fitness_validity"+fitness_validity);

                    if(puc_val_seconds > 0)
                    {
                        puc_validity = dbcl.dateFromDays(puc_val_seconds);
                        if(puc_validity.before(today))
                        {
                            error += "PUC VALIDITY IS OVER\n";
                        }
                    }else
                    {
                        error += "PUC VALIDITY DATE NOT FOUND\n";
                    }

                    Log.d("NFC_READ", "25 : puc_val_seconds "+puc_val_seconds+" puc_validity"+puc_validity);

                    nfca.close();
                    //error="";
                    if(!"".equals(error))
                    {
                        //background_main.setBackgroundColor(Color.RED);
                        showScrollableErrorDialog(MainActivity.this,"Error",error);
                        /*new Handler(Looper.getMainLooper()).postDelayed(() -> {
                            background_main.setBackground(originalBg);
                        }, 5000);*/

                    }else
                    {


                        /***************READ CARD OVER ******************/
                        ProgressDialog dialog = new ProgressDialog(this);
                        dialog.setMessage("Writing card...");
                        dialog.setCancelable(false);
                        dialog.show();

                        Vibrator vibrator = (Vibrator) getSystemService(Context.VIBRATOR_SERVICE);
                        String finalCurrentDateMONTH = currentDateMONTH;
                        String finalMonthyear = monthyear;
                        String finalAsset_number1 = asset_number;
                        String finalNow1 = now;
                        new Thread(() -> {

                            runOnUiThread(() -> {
                                dbcl.writeStringToTag(tag, mob_id,22,0);
                                dbcl.writeStringToTag(tag, finalCurrentDateMONTH,23,0);
                                dbcl.writeStringToTag(tag, finalMonthyear +part1,24,0);
                                boolean success =
                                        dbcl.writeOrFail(this,tag, 25, dbcl.combineByteArrays(part2.getBytes(StandardCharsets.UTF_8), dbcl.stringTo2Bytes(trip_status))) &&
                                                dbcl.writeOrFail(this,tag, 26, dbcl.stringTo4Bytes(String.valueOf(seconds))) &&
                                                dbcl.writeOrFail(this,tag, 27, dbcl.combineByteArrays(dbcl.stringTo2Bytes(String.valueOf(descriptionId)), dbcl.stringTo2Bytes(String.valueOf(routeId)))) &&

                                                dbcl.writeOrFail(this,tag, 28, dbcl.stringTo4Bytes("00")) &&
                                                dbcl.writeOrFail(this,tag, 29, dbcl.combineByteArrays(dbcl.stringTo2Bytes("00"), dbcl.stringTo2Bytes("00"))) &&
                                                dbcl.writeOrFail(this,tag, 30, dbcl.stringTo4Bytes("00")) &&
                                                dbcl.writeOrFail(this,tag, 31, dbcl.combineByteArrays(dbcl.stringTo2Bytes("00"), dbcl.stringTo2Bytes("00"))) &&
                                                dbcl.writeOrFail(this,tag, 32, dbcl.stringTo4Bytes("00")) &&
                                                dbcl.writeOrFail(this,tag, 33, dbcl.combineByteArrays(dbcl.stringTo2Bytes("00"), dbcl.stringTo2Bytes("00"))) &&

                                                dbcl.writeOrFail(this,tag, 34, dbcl.combineByteArrays(dbcl.stringTo2Bytes(TID), dbcl.stringTo2Bytes("00"))) &&
                                                dbcl.writeOrFail(this,tag, 35, dbcl.stringTo4Bytes("00")) &&
                                                dbcl.writeOrFail(this,tag, 36, dbcl.stringTo4Bytes(String.valueOf(seconds))) &&
                                                dbcl.writeOrFail(this,tag, 37, dbcl.combineByteArrays(dbcl.stringTo2Bytes(String.valueOf(vendorId)), dbcl.stringTo2Bytes("00")));

                                dialog.dismiss();

                                if (success)
                                {
                                    vibrator.vibrate(100);
                                    Toast.makeText(this, "Card written successfully ✅", Toast.LENGTH_SHORT).show();

                                        trip_close.put("tripsheet_no", tripsheetno);
                                        trip_close.put("trip_type", "RFID");
                                        trip_close.put("initial_route_id", String.valueOf(routeId));
                                        trip_close.put("final_route_id", String.valueOf(routeId));
                                        trip_close.put("rfid_id", rfid_card);
                                        trip_close.put("truck_id", truckid);
                                        trip_close.put("truck_no", finalAsset_number1);
                                        trip_close.put("vendor_id", String.valueOf(vendorId));
                                        trip_close.put("ore_id", String.valueOf(descriptionId));
                                        trip_close.put("status", trip_status_save);
                                        trip_close.put("src_time", srctime);
                                        trip_close.put("src_mobile", String.valueOf(src_mob_id_card));
                                        trip_close.put("dest_time", srctime);
                                        trip_close.put("dest_mobile", src_mob);
                                        trip_close.put("created_at", finalNow1);
                                        trip_close.putNull("wb_src_time");
                                        trip_close.putNull("wb_src_gross_login");
                                        trip_close.putNull("src_tare_wt_time");
                                        trip_close.putNull("wb_src_tare_login");
                                        trip_close.putNull("wb_dest_time");
                                        trip_close.putNull("wb_dest_gross_login");
                                        trip_close.putNull("dest_tare_wt_time");
                                        trip_close.putNull("wb_dest_tare_login");
                                        trip_close.putNull("src_gross_wt");
                                        trip_close.putNull("src_tare_wt");
                                        trip_close.putNull("dest_gross_wt");
                                        trip_close.putNull("dest_tare_wt");

                                        Log.e("srctime",srctime+" "+tripstarttime);


                                        success = saveTripsheet(tripsheetno,trip_close);

                                        if (success)
                                        {
                                            Toast.makeText(this, "Tripsheet saved successfully", Toast.LENGTH_SHORT).show();
                                            showScrollableErrorDialog(MainActivity.this,"Success", "Tripsheet saved successfully");

                                        } else
                                        {
                                            Toast.makeText(this, "Failed to save tripsheet", Toast.LENGTH_SHORT).show();

                                            showScrollableErrorDialog(MainActivity.this,"Error", "Failed to save tripsheet");

                                        }
                                    }
                            });

                        }).start();
                    }
                }

                card_details.put("Trip Status",getTripStatusText(trip_status_read));
                onRfidTapped(card_details,error);

                Log.d("NFC_READ", "25 : trip_status_read "+trip_status_read+" ");

            } catch (Exception e)
            {
                e.printStackTrace();
                showScrollableErrorDialog(MainActivity.this,"Error", e.getMessage());
            } finally
            {
                try
                {
                    nfca.close();
                } catch (Exception ignored)
                {
                    showScrollableErrorDialog(MainActivity.this,"Error", ignored.getMessage());
                }
            }

            dbclass dbs = new dbclass(this);
            SQLiteDatabase db = dbs.getWritableDatabase();

            new Thread(() -> {

                JSONArray tripsheets = dbcl.getTripsheetsForUpload(this);

                if (tripsheets.length() == 0) {
                    Log.d("UPLOAD", "No pending tripsheets");
                    return;
                }

                boolean successup = dbcl.uploadTripsheets(tripsheets);

                if (successup) {
                    List<Long> uploadedIds = new ArrayList<>();

                    for (int i = 0; i < tripsheets.length(); i++) {
                        try {
                            uploadedIds.add(tripsheets.getJSONObject(i).getLong("id"));
                        } catch (JSONException e) {
                            e.printStackTrace();
                        }
                    }

                    dbcl.markTripsheetsUploaded(uploadedIds,this);

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

        }else if(clearRequested){
            dbcl.clearUltralight(tag);
            clearRequested = false;
        }

    }
    private String getTripStatusText(int trip_status_read) {

        switch (trip_status_read) {
            case 1:
                return "Open";
            case 2:
                return "Closed";
            default:
                return "Unknown (" + trip_status_read + ")";
        }
    }
    public static byte[] integerTo2Bytes(Integer value) {
        // max value : 65535 (0xFFFF)

        if (value == null) {
            return new byte[]{0, 0};
        }

        if (value < 0 || value > 0xFFFF) {
            throw new IllegalArgumentException("Value does not fit into 2 bytes.");
        }

        return new byte[]{
                (byte) ((value >> 8) & 0xFF),
                (byte) (value & 0xFF)
        };
    }
    public static void showScrollableErrorDialog(Context context, String title, String message) {

        if (currentDialog != null && currentDialog.isShowing()) {
            currentDialog.dismiss();
            currentDialog = null;
        }
        AlertDialog.Builder builder = new AlertDialog.Builder(context);
        builder.setTitle(title);

        // Create TextView
        TextView textView = new TextView(context);
        textView.setText(message);
        textView.setTextSize(20);
        textView.setTypeface(Typeface.DEFAULT_BOLD);
        textView.setPadding(40, 30, 40, 30);
        textView.setTextIsSelectable(true);

        // Wrap TextView in ScrollView
        ScrollView scrollView = new ScrollView(context);
        scrollView.addView(textView);

        builder.setView(scrollView);
        builder.setPositiveButton("OK", (dialog, which) -> dialog.dismiss());
        builder.setCancelable(false);

        currentDialog = builder.show();

        // 🎨 Color the WHOLE dialog window
        if (title != null) {
            if (title.equalsIgnoreCase("error")) {
                currentDialog.getWindow().setBackgroundDrawable(new ColorDrawable(Color.parseColor("#D32F2F"))); // red
                textView.setTextColor(Color.WHITE);
            } else if (title.equalsIgnoreCase("success")) {
                currentDialog.getWindow().setBackgroundDrawable(new ColorDrawable(Color.parseColor("#388E3C"))); // green
                textView.setTextColor(Color.WHITE);
            }
            // else: keep default theme background
        }
    }
    public boolean  saveTripsheet(String tripsheetno,ContentValues trip_data)
    {
        dbclass dbs = new dbclass(this);
        SQLiteDatabase db = dbs.getWritableDatabase();
        int rows = db.update(
                "tripsheets",
                trip_data,
                "tripsheet_no = ?",
                new String[]{ tripsheetno }
        );
        Log.e("saveTripsheet",rows+" rows");
        if (rows == 0) {
            long result = db.insert("tripsheets", null, trip_data);
            Log.e("saveTripsheet",result+" ");
            return result != -1;
        }
        return true;
    }

    public String getNextTripSheetNo()
    {
        String nextNo = "0001";
        dbclass dbs = new dbclass(this);
        SQLiteDatabase db = dbs.getReadableDatabase();
        Cursor cursor = db.rawQuery(
                "SELECT MAX(id) FROM tripsheets",
                null
        );

        if (cursor != null) {
            if (cursor.moveToFirst() && !cursor.isNull(0)) {
                int maxId = cursor.getInt(0);
                nextNo = String.format(Locale.US, "%04d", maxId + 1);
            }
            cursor.close();
        }

        return nextNo;
    }


    public void showSetupDetails(Cursor cursor,Context context) {

        CardView cardSetupDetails = findViewById(R.id.cardSetupDetails);
        LinearLayout container = findViewById(R.id.setupDetailsContainer);

        container.removeAllViews();

        if (cursor != null && cursor.moveToFirst()) {

            dbcl.addRow(context,container, "Sublocation", cursor.getString(cursor.getColumnIndexOrThrow("sublocation_name")));
            dbcl.addRow(context,container, "Vendor", cursor.getString(cursor.getColumnIndexOrThrow("company_name")));
            dbcl.addRow(context,container, "Ore", cursor.getString(cursor.getColumnIndexOrThrow("description")));
            dbcl.addRow(context,container, "Destination Location", cursor.getString(cursor.getColumnIndexOrThrow("location_name")));
            dbcl.addRow(context,container, "Destination Sublocation", cursor.getString(cursor.getColumnIndexOrThrow("dest_subloc")));
            dbcl.addRow(context,container, "Route", cursor.getString(cursor.getColumnIndexOrThrow("route_name")));

            cardSetupDetails.setVisibility(View.VISIBLE);
            cursor.close();
        } else {
            cardSetupDetails.setVisibility(View.GONE);
        }
    }

}
package com.example.rfid;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.cardview.widget.CardView;

import android.annotation.SuppressLint;
import android.app.AlertDialog;
import android.app.PendingIntent;
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
import android.provider.Settings;
import android.util.Base64;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;
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
    @SuppressLint("MissingInflatedId")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        if (isMobileLoginEmpty()) {
            Intent intent = new Intent(this, activity_login.class);
            startActivity(intent);
            finish();
            return;
        }

        dbcl = new dbclass(MainActivity.this);

        dbcl.saveUidAndLocationToPrefs(MainActivity.this);

        SharedPreferences prefs = getSharedPreferences("APP_PREFS", MODE_PRIVATE);

        mobileuid = prefs.getString("UID", "");
        TID = prefs.getString("TID", "");
        String locationIdsp = prefs.getString("LOCATION_ID", "");

        setContentView(R.layout.activity_main);

        nfcAdapter = NfcAdapter.getDefaultAdapter(this);

        Button btnSetup = findViewById(R.id.btnSetup);
        Button btnExit = findViewById(R.id.btnExit);
        syncbutton = findViewById(R.id.btnSync);

        background_main = findViewById(R.id.background_main);
        cardDetails = findViewById(R.id.cardDetails);

        handler = new Handler(Looper.getMainLooper());
        hideRunnable = () -> cardDetails.setVisibility(View.GONE);
        detailsContainer = findViewById(R.id.detailsContainer);

        nfcAdapter = NfcAdapter.getDefaultAdapter(this);


        btnSetup.setOnClickListener(v ->
                startActivity(new Intent(this, activity_login.class)));
        syncbutton.setOnClickListener(v ->
                startActivity(new Intent(this, sync.class)));

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


    }

    private void onRfidTapped(ContentValues values) {

        handler.removeCallbacks(hideRunnable);

        // Clear old views
        detailsContainer.removeAllViews();

        for (Map.Entry<String, Object> entry : values.valueSet()) {

            String key = entry.getKey();
            Object valObj = entry.getValue();
            String value = valObj == null ? "--" : String.valueOf(valObj);

            View row = createRow(key, value);
            detailsContainer.addView(row);
        }

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
        String rfid_card = bytesToHex(uid);
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

                String trip_type="RFID";
                String mob_id=mobileuid;
                String trip_status="01";
                String trip_status_save="OPEN";
                String tripsrno_org = getNextTripSheetNo();

                String part1 = tripsrno_org.substring(0, 2); // "01"
                String part2 = tripsrno_org.substring(2, 4); // "23"

                String tripstarttime = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault()).format(new Date());
                String srctime = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault()).format(new Date());
                long seconds = secondsFromBaseDate();
                String tripsheetno = mob_id+currentDateMONTH+monthyear+tripsrno_org;
                String src_mob=TID;

                byte[] pageData = readPage(nfca, 04);
                String asset_num_04 = new String(pageData, StandardCharsets.UTF_8).trim();
                pageData = readPage(nfca, 05);
                String asset_num_05 = new String(pageData, StandardCharsets.UTF_8).trim();
                pageData = readPage(nfca, 06);
                String asset_num_06 = new String(pageData, StandardCharsets.UTF_8).trim();

                asset_number = asset_num_04+asset_num_05+asset_num_06;
                Log.d("NFC_READ", "040506 : asset number "+asset_num_04+" "+asset_num_05+" "+asset_num_06);

                String truckid = dbcl.getAssetId_fromdb(asset_number);

                pageData = readPage(nfca, 25);
                String tripsheet_no_last2  = new String(pageData, 0, 2);//bytesToString(pageData, 0, 2);
                int trip_status_read  = Integer.parseInt(bytesToString(pageData, 2, 2));

                Log.d("NFC_READ", "trip_status_read pageData"+trip_status_read+ " "+Arrays.toString(pageData)+" tripsheet_no_last2 "+tripsheet_no_last2);


                pageData = readPage(nfca, 34);
                int src_mob_id_card  = Integer.parseInt(bytesToString(pageData, 0, 2));

                ContentValues trip_close = new ContentValues();
                String now = null;

                now = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault()).format(new Date());

                pageData = readPage(nfca, 14);
                int tare_wt_sec_card  = Integer.parseInt(bytesToString(pageData, 0, 4));
                String tare_wt_time_card = null;
                Log.e("tare_wt_sec_card"," "+tare_wt_sec_card);
                if(tare_wt_sec_card > 0)
                {
                    tare_wt_time_card = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault()).format(date_time_FromSeconds(tare_wt_sec_card));
                    SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault());
                    sdf.setLenient(false);
                    Date tareDate = sdf.parse(tare_wt_time_card);
                    Date todaysdatetm = new Date();

                    long diffMillis = todaysdatetm.getTime() - tareDate.getTime();
                    long diffDays = TimeUnit.MILLISECONDS.toDays(diffMillis);
                    if (diffDays > 30) {
                        error += "TARE WEIGHT WAS TAKEN BEFORE 30 DAYS("+diffDays+" DAYS BEFORE). KINDLY GET THE TARE WEIGHT DONE.";
                    }
                }else
                {
                    error += "TARE WEIGHT TIME NOT FOUND";
                }

                pageData = readPage(nfca, 15);
                int tare_wt_wb_log_card = Integer.parseInt(bytesToString(pageData, 0, 2));
                int tare_wt_card = Integer.parseInt(bytesToString(pageData, 2, 2));

                card_details.put("Card No",rfid_card);
                card_details.put("Vehicle No",asset_number);
                card_details.put("Trip Status",trip_status_read);
                card_details.put("Src Tare Weight",tare_wt_card);
                card_details.put("Src Tare Weight Time",tare_wt_time_card);
                //trip_status_read = 1;
                if(trip_status_read == 1) //trip open
                {



                    pageData = readPage(nfca, 18);
                    int HSD = Integer.parseInt(bytesToString(pageData, 0, 3));

                    String HSD_BAL = String.valueOf(HSD/100);

                    String closing_trip_num_card ="";
                    pageData = readPage(nfca, 22);
                    Log.d("NFC_READ", "22  "+Arrays.toString(pageData));
                    String closing_trip_num_01 = new String(pageData, StandardCharsets.UTF_8).trim();
                    pageData = readPage(nfca, 23);
                    Log.d("NFC_READ", "23  "+Arrays.toString(pageData));
                    String closing_trip_num_02 = new String(pageData, StandardCharsets.UTF_8).trim();
                    pageData = readPage(nfca, 24);
                    Log.d("NFC_READ", "24  "+Arrays.toString(pageData));
                    String closing_trip_num_03 = new String(pageData, StandardCharsets.UTF_8).trim();


                    closing_trip_num_card = closing_trip_num_01+closing_trip_num_02+closing_trip_num_03+tripsheet_no_last2;
                    Log.d("NFC_READ", "closing_trip_num_card "+closing_trip_num_card+" ");

                    card_details.put("Trip No",closing_trip_num_card);

                    pageData = readPage(nfca, 27);
                    int desc_id_card = Integer.parseInt(bytesToString(pageData, 0, 2));
                    int route_id_card = Integer.parseInt(bytesToString(pageData, 2, 2));
                    int dest_subloc_id_trip = Integer.parseInt(dbcl.get_dest_subloc_from_routeid(route_id_card));
                    Log.d("NFC_READ", "dest_subloc_id_trip "+dest_subloc_id_trip+" "+sublocationId);

                    pageData = readPage(nfca, 28);
                    int wb_src_sec_card  = Integer.parseInt(bytesToString(pageData, 0, 4));
                    String wb_src_time_card = null;
                    if(wb_src_sec_card > 0)
                    {
                        wb_src_time_card = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault()).format(date_time_FromSeconds(wb_src_sec_card));
                    }

                    pageData = readPage(nfca, 29);
                    int src_gr_wt_card = Integer.parseInt(bytesToString(pageData, 0, 2));
                    int wb_src_gr_login_card = Integer.parseInt(bytesToString(pageData, 2, 2));

                    pageData = readPage(nfca, 30);
                    int wb_dest_sec_card  = Integer.parseInt(bytesToString(pageData, 0, 4));
                    String wb_dest_time_card = null;
                    if(wb_dest_sec_card > 0)
                    {
                        wb_dest_time_card = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault()).format(date_time_FromSeconds(wb_dest_sec_card));
                    }

                    pageData = readPage(nfca, 31);
                    int dest_gr_wt_card = Integer.parseInt(bytesToString(pageData, 0, 2));
                    int wb_dest_gr_login_card = Integer.parseInt(bytesToString(pageData, 2, 2));

                    pageData = readPage(nfca, 32);
                    int dest_tare_wt_sec_card  = Integer.parseInt(bytesToString(pageData, 0, 4));
                    String dest_tare_wt_time_card = null;
                    if(dest_tare_wt_sec_card > 0)
                    {
                        dest_tare_wt_time_card = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault()).format(date_time_FromSeconds(dest_tare_wt_sec_card));
                    }

                    pageData = readPage(nfca, 33);
                    int dest_tare_wt_card = Integer.parseInt(bytesToString(pageData, 0, 2));
                    int wb_dest_tare_login_card = Integer.parseInt(bytesToString(pageData, 2, 2));

                    pageData = readPage(nfca, 35);
                    int dest_sec_card  = Integer.parseInt(bytesToString(pageData, 0, 4));
                    String dest_time_card = null;
                    Log.e("dest_sec_card"," "+dest_sec_card);
                    if(dest_sec_card > 0)
                    {
                        long destTimeMillis = dest_sec_card * 1000L;   // seconds → millis
                        long currentTimeMillis = System.currentTimeMillis();

                        long diffMillis = currentTimeMillis - destTimeMillis;
                        long minWaitMillis = 5 * 60 * 1000; // 5 minutes

                        if (diffMillis < minWaitMillis)
                        {

                            long remainingMillis = minWaitMillis - diffMillis;

                            long remainingSeconds = remainingMillis / 1000;
                            long remainingMinutes = remainingSeconds / 60;
                            long remainingSecs = remainingSeconds % 60;

                            String waitMsg;
                            if (remainingMinutes > 0) {
                                waitMsg = remainingMinutes + " min " + remainingSecs + " sec";
                            } else {
                                waitMsg = remainingSecs + " sec";
                            }

                            error += "PLEASE WAIT FOR" + waitMsg + " MORE BEFORE ISSUING NEW TRIP.";
                        }

                    }

                    pageData = readPage(nfca, 36);
                    int src_sec_card  = Integer.parseInt(bytesToString(pageData, 0, 4));
                    String src_time_card = null;
                    if(src_sec_card > 0)
                    {
                        src_time_card = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault()).format(date_time_FromSeconds(src_sec_card));

                    }

                    pageData = readPage(nfca, 37);
                    int vendor_id_card  = Integer.parseInt(bytesToString(pageData, 0, 2));

                    if(dest_subloc_id_trip==sublocationId && "".equals(error))
                    {
                        nfca.close();
                        trip_status= "02";
                        trip_status_save="CLOSED";

                        Log.e("stringTo2Bytes",Arrays.toString(stringTo2Bytes(trip_status)));

                        writePage(tag,25, combineByteArrays("70".getBytes(StandardCharsets.UTF_8),stringTo2Bytes(trip_status)));

                        writePage(tag, 34,combineByteArrays(stringTo2Bytes(String.valueOf(src_mob_id_card)), stringTo2Bytes(TID)));

                        writePage(tag, 35,stringTo4Bytes(String.valueOf(seconds)));

                        trip_close.put("tripsheet_no", closing_trip_num_card);
                        trip_close.put("trip_type", "RFID");
                        trip_close.put("initial_route_id", String.valueOf(route_id_card));
                        trip_close.put("final_route_id", String.valueOf(route_id_card));
                        trip_close.put("rfid_id", rfid_card);
                        trip_close.put("truck_id", truckid);
                        trip_close.put("truck_no", asset_number);
                        trip_close.put("vendor_id", String.valueOf(vendor_id_card));
                        trip_close.put("ore_id", String.valueOf(desc_id_card));
                        trip_close.put("status", trip_status_save);
                        trip_close.put("src_time", src_time_card);
                        trip_close.put("src_mobile", String.valueOf(src_mob_id_card));
                        trip_close.put("dest_time", srctime);
                        trip_close.put("dest_mobile", src_mob);
                        trip_close.put("created_at", now);
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

                        boolean success = saveTripsheet(closing_trip_num_card,trip_close);

                        if (success)
                        {
                            Toast.makeText(this, "Tripsheet closed successfully", Toast.LENGTH_SHORT).show();
                            showScrollableErrorDialog(MainActivity.this, "Tripsheet closed successfully");
                            background_main.setBackgroundColor(Color.GREEN);

                            new Handler(Looper.getMainLooper()).postDelayed(() -> {
                                background_main.setBackground(originalBg);
                            }, 5000);

                        } else
                        {
                            Toast.makeText(this, "Failed to close tripsheet", Toast.LENGTH_SHORT).show();
                            background_main.setBackgroundColor(Color.RED);
                            showScrollableErrorDialog(MainActivity.this, "Failed to close tripsheet");
                            new Handler(Looper.getMainLooper()).postDelayed(() -> {
                                background_main.setBackground(originalBg);
                            }, 5000);
                        }
                    }else
                    {
                        error += "WRONG DESTINATION\nPROCEED TO :- \n\n"+dbcl.get_dest_subloc_name(dest_subloc_id_trip);

                        background_main.setBackgroundColor(Color.RED);
                        showScrollableErrorDialog(MainActivity.this,error);
                        new Handler(Looper.getMainLooper()).postDelayed(() -> {
                            background_main.setBackground(originalBg);
                        }, 5000);
                    }
                    nfca.close();

                }else
                {
                    pageData = readPage(nfca, 11);
                    String Asset_id  = bytesToString(pageData, 0, 3);
                    int asset_status = Integer.parseInt(bytesToString(pageData, 3, 1));

                    Log.d("NFC_READ", "11 : asset number "+Asset_id+" "+asset_status);

                    if(asset_status==0)
                    {
                        error += "VEHICLE IS INACTIVE\n";
                    }


                    pageData = readPage(nfca, 16);
                    int ins_val_seconds  = Integer.parseInt(bytesToString(pageData, 0, 2));
                    int rdt_val_seconds = Integer.parseInt(bytesToString(pageData, 2, 2));

                    Date insurance_validity = null;
                    Date rdtax_validity = null;

                    Calendar todayCal = Calendar.getInstance(TimeZone.getTimeZone("Asia/Kolkata"));
                    Date today = todayCal.getTime();

                    if(ins_val_seconds > 0)
                    {
                        insurance_validity = dateFromSeconds(ins_val_seconds);
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
                        rdtax_validity = dateFromSeconds(rdt_val_seconds);
                        if(rdtax_validity.before(today))
                        {
                            error += "ROAD TAX VALIDITY IS OVER\n";
                        }
                    }else
                    {
                        error += "ROAD TAX VALIDITY DATE NOT FOUND\n";
                    }

                    Log.d("NFC_READ", "25 : rdt_val_seconds "+rdt_val_seconds+" rdtax_validity"+rdtax_validity);

                    pageData = readPage(nfca, 17);
                    int fit_val_seconds  = Integer.parseInt(bytesToString(pageData, 0, 2));
                    int puc_val_seconds = Integer.parseInt(bytesToString(pageData, 2, 2));

                    Date fitness_validity = null;
                    Date puc_validity = null;

                    if(fit_val_seconds > 0)
                    {
                        fitness_validity = dateFromSeconds(fit_val_seconds);
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
                        puc_validity = dateFromSeconds(puc_val_seconds);
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
                    error="";
                    if(!"".equals(error))
                    {
                        background_main.setBackgroundColor(Color.RED);
                        showScrollableErrorDialog(MainActivity.this,error);
                        new Handler(Looper.getMainLooper()).postDelayed(() -> {
                            background_main.setBackground(originalBg);
                        }, 5000);

                    }else
                    {


                        /***************READ CARD OVER ******************/

                        writeStringToTag(tag, mob_id,22,0);
                        writeStringToTag(tag, currentDateMONTH,23,0);
                        writeStringToTag(tag, monthyear+part1,24,0);

                        writePage(tag,25, combineByteArrays(part2.getBytes(StandardCharsets.UTF_8),stringTo2Bytes(trip_status)));
                        writePage(tag,26,stringTo4Bytes(String.valueOf(seconds)));
                        writePage(tag,27, combineByteArrays(stringTo2Bytes(String.valueOf(descriptionId)), stringTo2Bytes(String.valueOf(routeId))));

                        writePage(tag,28, stringTo4Bytes("00"));
                        writePage(tag,29, combineByteArrays(stringTo2Bytes("00"), stringTo2Bytes("00")));
                        writePage(tag,30, stringTo4Bytes("00"));
                        writePage(tag,31, combineByteArrays(stringTo2Bytes("00"), stringTo2Bytes("00")));
                        writePage(tag,32, stringTo4Bytes("00"));
                        writePage(tag,33, combineByteArrays(stringTo2Bytes("00"), stringTo2Bytes("00")));

                        writePage(tag, 34,combineByteArrays(stringTo2Bytes(TID), stringTo2Bytes("00")));

                        writePage(tag, 35,stringTo4Bytes("00"));

                        writePage(tag, 36,stringTo4Bytes(String.valueOf(seconds)));
                        writePage(tag, 37,combineByteArrays(stringTo2Bytes(String.valueOf(vendorId)), stringTo2Bytes("00")));

                        trip_close.put("tripsheet_no", tripsheetno);
                        trip_close.put("trip_type", "RFID");
                        trip_close.put("initial_route_id", String.valueOf(routeId));
                        trip_close.put("final_route_id", String.valueOf(routeId));
                        trip_close.put("rfid_id", rfid_card);
                        trip_close.put("truck_id", truckid);
                        trip_close.put("truck_no", asset_number);
                        trip_close.put("vendor_id", String.valueOf(vendorId));
                        trip_close.put("ore_id", String.valueOf(descriptionId));
                        trip_close.put("status", trip_status_save);
                        trip_close.put("src_time", srctime);
                        trip_close.put("src_mobile", String.valueOf(src_mob_id_card));
                        trip_close.put("dest_time", srctime);
                        trip_close.put("dest_mobile", src_mob);
                        trip_close.put("created_at", now);
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
                        onRfidTapped(trip_close);
                        Log.e("srctime",srctime+" "+tripstarttime);
                        Log.e("monthyear",monthyear+" ");


                        boolean success = saveTripsheet(tripsheetno,trip_close);

                        if (success)
                        {
                            Toast.makeText(this, "Tripsheet saved successfully", Toast.LENGTH_SHORT).show();
                            showScrollableErrorDialog(MainActivity.this, "Tripsheet saved successfully");
                            background_main.setBackgroundColor(Color.GREEN);

                            new Handler(Looper.getMainLooper()).postDelayed(() -> {
                                background_main.setBackground(originalBg);
                            }, 5000);

                        } else
                        {
                            Toast.makeText(this, "Failed to save tripsheet", Toast.LENGTH_SHORT).show();
                            background_main.setBackgroundColor(Color.RED);
                            showScrollableErrorDialog(MainActivity.this, "Failed to save tripsheet");
                            new Handler(Looper.getMainLooper()).postDelayed(() -> {
                                background_main.setBackground(originalBg);
                            }, 5000);
                        }
                    }
                }

                onRfidTapped(card_details);

                Log.d("NFC_READ", "25 : trip_status_read "+trip_status_read+" ");

            } catch (Exception e)
            {
                e.printStackTrace();
                showScrollableErrorDialog(MainActivity.this, e.getMessage());
            } finally
            {
                try
                {
                    nfca.close();
                } catch (Exception ignored)
                {
                    showScrollableErrorDialog(MainActivity.this, ignored.getMessage());
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
            clearUltralight(tag);
            clearRequested = false;
        }else{
            readMifareClassic(tag);     // normal reading
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
    public static void showScrollableErrorDialog(Context context, String message) {

        AlertDialog.Builder builder = new AlertDialog.Builder(context);
        builder.setTitle("Error");

        // Create TextView
        TextView textView = new TextView(context);
        textView.setText(message);
        textView.setTextSize(16);
        textView.setPadding(40, 30, 40, 30);
        textView.setTextIsSelectable(true);

        // Wrap TextView in ScrollView
        ScrollView scrollView = new ScrollView(context);
        scrollView.addView(textView);

        builder.setView(scrollView);

        builder.setPositiveButton("OK", (dialog, which) -> dialog.dismiss());

        builder.setCancelable(false);
        builder.show();
    }

    public static long bytesToSeconds(byte[] data) {

        if (data.length != 4) {
            throw new IllegalArgumentException("4 bytes required");
        }

        return ((long) (data[0] & 0xFF) << 24) |
                ((long) (data[1] & 0xFF) << 16) |
                ((long) (data[2] & 0xFF) << 8)  |
                ((long) (data[3] & 0xFF));
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
    public byte[] encodeTo3Bytes(int number)
    {

        if (number < 0 || number > 0xFFFFFF) {
            throw new IllegalArgumentException("Number out of 3-byte range");
        }

        return new byte[]{
                (byte) ((number >> 16) & 0xFF),
                (byte) ((number >> 8) & 0xFF),
                (byte) (number & 0xFF)
        };
    }

    public static long secondsFromBaseDate()
    {

        Calendar baseCal = Calendar.getInstance(TimeZone.getTimeZone("Asia/Kolkata"));
        baseCal.set(2026, Calendar.JANUARY, 1, 0, 0, 0);
        baseCal.set(Calendar.MILLISECOND, 0);

        long baseMillis = baseCal.getTimeInMillis();
        long currentMillis = System.currentTimeMillis();

        long diffMillis = currentMillis - baseMillis;

        return diffMillis / 1000; // seconds
    }

    public static Date dateFromSeconds(long seconds) {

        Calendar baseCal = Calendar.getInstance(TimeZone.getTimeZone("Asia/Kolkata"));
        baseCal.set(2026, Calendar.JANUARY, 1, 0, 0, 0);
        baseCal.set(Calendar.MILLISECOND, 0);

        long baseMillis = baseCal.getTimeInMillis();
        long targetMillis = baseMillis + (seconds * 1000);

        return new Date(targetMillis);
    }

    public static Date date_time_FromSeconds(long seconds) {

        Calendar baseCal = Calendar.getInstance(TimeZone.getTimeZone("Asia/Kolkata"));
        baseCal.set(2026, Calendar.JANUARY, 1, 0, 0, 0);
        baseCal.set(Calendar.MILLISECOND, 0);

        long baseMillis = baseCal.getTimeInMillis();
        long targetMillis = baseMillis + (seconds * 1000);

        return new Date(targetMillis);
    }

    public static String bytesToString(byte[] data, int start, int length) {

        long value = 0;
        for (int i = start; i < start + length; i++) {
            value = (value << 8) | (data[i] & 0xFF);
        }
        Log.e("bytesToString"," "+value);
        return String.valueOf(value);
    }
    public static byte[] stringTo2Bytes(String value) {

        long seconds = Long.parseLong(value); // decimal conversion

        byte[] data = new byte[2];
        data[0] = (byte) ((seconds >> 8) & 0xFF);
        data[1] = (byte) (seconds & 0xFF);

        return data;
    }

    public static byte[] intTo2Bytes(Integer value) {

        byte[] data = new byte[2];
        data[0] = (byte) ((value >> 8) & 0xFF);
        data[1] = (byte) (value & 0xFF);

        return data;
    }
    public static byte[] stringTo4Bytes(String value) {

        long seconds = Long.parseLong(value); // decimal conversion

        byte[] data = new byte[4];
        data[0] = (byte) ((seconds >> 24) & 0xFF);
        data[1] = (byte) ((seconds >> 16) & 0xFF);
        data[2] = (byte) ((seconds >> 8) & 0xFF);
        data[3] = (byte) (seconds & 0xFF);

        return data;
    }

    public static byte[] combineByteArrays(byte[] a, byte[] b) {

        byte[] result = new byte[a.length + b.length];

        System.arraycopy(a, 0, result, 0, a.length);
        System.arraycopy(b, 0, result, a.length, b.length);

        return result;
    }

    public static byte[] readPage(NfcA nfca, int page) throws IOException {

        byte[] readCmd = new byte[]{
                (byte) 0x30,   // READ command
                (byte) page    // page number
        };

        byte[] response = nfca.transceive(readCmd);

        // response length = 16 bytes (4 pages)
        if (response.length < 16) {
            throw new IOException("Invalid read response");
        }

        // Extract only requested page (first 4 bytes)
        byte[] pageData = new byte[4];
        System.arraycopy(response, 0, pageData, 0, 4);

        return pageData;
    }

    private void readMifareClassic(Tag tag) {
        try {
            MifareUltralight mu = MifareUltralight.get(tag);

            try {
                mu.connect();

                StringBuilder result = new StringBuilder();
                result.append("Mifare Ultralight Detected\n\n");

                // NTAG213 has 48 pages. Change if needed.
                for (int page = 0; page < 48; page += 4) {

                    byte[] response = mu.readPages(page);   // reads 4 pages at once

                    if (response != null && response.length == 16) {

                        for (int i = 0; i < 4; i++) {
                            int p = page + i;
                            byte[] pageData = Arrays.copyOfRange(response, i*4, (i+1)*4);

                            result.append("Page ")
                                    .append(p)
                                    .append(": ")
                                    .append(bytesToHex(pageData))
                                    .append(" | ")
                                    .append(new String(pageData, StandardCharsets.UTF_8))
                                    .append("\n");
                        }
                    }
                }
                readtext.setText(result.toString());
                Log.e("NFC", result.toString());
                mu.close();

            } catch (Exception e) {
                Log.e("NFC", "Error: " + e.toString());
            }
        } catch (Exception e) {
            Log.e("nfcAdapter","Error: " + e.getMessage());
        }

    }
    private String bytesToHex(byte[] bytes)
    {
        StringBuilder sb = new StringBuilder();
        for (byte b : bytes) {
            sb.append(String.format("%02X", b));
        }
        return sb.toString();
    }
    private String writePage(Tag tag, int page, byte[] data)
    {
        NfcA nfcA = NfcA.get(tag);

        try {
            nfcA.connect();

            byte[] cmd = new byte[]{
                    (byte) 0xA2,       // WRITE command
                    (byte) page,       // Page number
                    data[0],
                    data[1],
                    data[2],
                    data[3]
            };

            byte[] response = nfcA.transceive(cmd);
            nfcA.close();
            if (response[0] == (byte) 0x0A)
            {
                //showScrollableErrorDialog(MainActivity.this, "Page "+page+" - Write Complete "+Arrays.toString(data));
                Log.e("NFC", "Wrote Page result" + "Page "+page+" - Write Complete");
                return true;
            }else
            {
                showScrollableErrorDialog(MainActivity.this, "Page "+page+" - Write Failed "+Arrays.toString(data));
                Log.e("NFC", "Wrote Page result" + "Page "+page+" - Write Failed");
                return false;
            }
        } catch (Exception e) {
            Log.e("NFC", "Error writing tag: Page "+page+" - "+ e.getMessage());
            showScrollableErrorDialog(MainActivity.this, "Page "+page+" - "+Arrays.toString(data)+" "+e.getMessage());
            return false;
        }
    }


    private void writeStringToTag(Tag tag, String text,int page,int strtype) {
        NfcA nfcA = NfcA.get(tag);

        try {
            nfcA.connect();
            byte[] bytes;
            byte[] pageData;
            if(strtype==1)
            {
                byte[] threeBytes = encodeTo3Bytes(Integer.parseInt(text));

                bytes = new byte[]{
                        threeBytes[0],
                        threeBytes[1],
                        threeBytes[2],
                        (byte) 0x00 // padding
                };

            }else {
                bytes = text.getBytes(StandardCharsets.UTF_8);
            }

            int pad = 4 - (bytes.length % 4);
            if (pad != 4) {
                byte[] padded = new byte[bytes.length + pad];
                System.arraycopy(bytes, 0, padded, 0, bytes.length);
                bytes = padded;
            }

                byte[] cmd = new byte[]{
                        (byte) 0xA2,        // WRITE
                        (byte) page,        // page number
                        bytes[0],
                        bytes[1],
                        bytes[2],
                        bytes[3]
                };

                byte[] result = nfcA.transceive(cmd);
                Log.e("NFC", "Wrote Page " + page);

            nfcA.close();
            Log.e("NFC", "Write Complete");

        } catch (Exception e) {
            Log.e("NFC", "Error writing tag: " + e.getMessage());
        }
    }

    private void clearUltralight(Tag tag) {
        NfcA nfcA = NfcA.get(tag);

        try {
            nfcA.connect();

            int page = 4;          // First user page
            int lastPage = 39;     // Last user page for NTAG213

            while (page <= lastPage) {

                byte[] cmd = new byte[]{
                        (byte) 0xA2,     // WRITE
                        (byte) page,     // PAGE
                        0x00, 0x00, 0x00, 0x00
                };

                nfcA.transceive(cmd);
                Log.e("NFC", "Cleared Page " + page);
                page++;
            }

            nfcA.close();
            Log.e("NFC","RESET COMPLETE");

        } catch (Exception e) {
            Log.e("NFC","Reset failed: " + e.getMessage());
        }
    }

    public boolean isMobileLoginEmpty() {
        dbclass dbs = new dbclass(this);
        SQLiteDatabase db = dbs.getReadableDatabase();
        Cursor c = db.rawQuery("SELECT COUNT(*) FROM mobile_logins WHERE STATUS=1", null);
        boolean empty = true;
        if (c.moveToFirst()) {
            empty = c.getInt(0) == 0;
        }
        c.close();
        return empty;
    }

    private View createRow(String title, String value) {

        LinearLayout row = new LinearLayout(this);
        row.setOrientation(LinearLayout.HORIZONTAL);
        row.setPadding(0, 8, 0, 8);

        TextView tvTitle = new TextView(this);
        tvTitle.setLayoutParams(new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1));
        tvTitle.setText(formatTitle(title));
        tvTitle.setTypeface(null, Typeface.BOLD);
        tvTitle.setTextColor(Color.parseColor("#555555"));

        TextView tvValue = new TextView(this);
        tvValue.setLayoutParams(new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1));
        tvValue.setText(value);
        tvValue.setTextColor(Color.BLACK);

        row.addView(tvTitle);
        row.addView(tvValue);

        return row;
    }

    private String formatTitle(String key) {
        return key.replace("_", " ").toUpperCase(Locale.US);
    }
}
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
import java.util.LinkedHashMap;
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
import android.widget.Switch;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.android.material.switchmaterial.SwitchMaterial;

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

public class MainActivity extends activity_base {
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

    long old_route_id = -1;
    long new_route1_id = -1;
    long new_route2_id = -1;
    long new_route3_id = -1;
    long new_route4_id = -1;

    String mobileuid ="";
    String TID ="";
    long LOCATION_ID_MOBILE =0;
    LinearLayout background_main;
    TextView tvRfid,tvTruckNo,tvVendor;
    Handler handler;
    Runnable hideRunnable;
    CardView cardDetails;
    LinearLayout detailsContainer;
    String machine_id;
    String machine_start_date_time;
    int int_tare_validity_days;
    int ext_tare_validity_days;
    int screening_plant_id;
    int plant_input_product_id;
    private static AlertDialog currentDialog;
    SwitchMaterial switchMode;
    int operation_type = 0;//0-truck,1-barge
    @SuppressLint("MissingInflatedId")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        Button btnSetup = findViewById(R.id.btnSetup);
        Button btnIssuerfid = findViewById(R.id.btnIssuerfid);
        Button btnExit = findViewById(R.id.btnExit);
        syncbutton = findViewById(R.id.btnSync);

        background_main = findViewById(R.id.background_main);
        cardDetails = findViewById(R.id.cardDetails);

        handler = new Handler(Looper.getMainLooper());
        hideRunnable = () -> cardDetails.setVisibility(View.GONE);
        detailsContainer = findViewById(R.id.detailsContainer);

        dbcl = new dbclass(MainActivity.this);
        dbcl.trustEveryone();
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
        LOCATION_ID_MOBILE = prefs.getLong("LOCATION_ID", 0);

        Long locationIdsp = prefs.getLong("LOCATION_ID", 0);
        machine_id = prefs.getString("machine_id", "");
        machine_start_date_time = prefs.getString("date_time", "");
        int_tare_validity_days = prefs.getInt("int_tare_validity_days",0);
        ext_tare_validity_days = prefs.getInt("ext_tare_validity_days", 0);
        screening_plant_id = prefs.getInt("screening_plant_id", 0);
        plant_input_product_id = prefs.getInt("plant_input_product_id", 0);
        switchMode = findViewById(R.id.switchMode);



        nfcAdapter = NfcAdapter.getDefaultAdapter(this);



        btnSetup.setOnClickListener(v ->
                startActivity(new Intent(this, activity_login.class)));
        syncbutton.setOnClickListener(v ->
                startActivity(new Intent(this, sync.class)));
        btnIssuerfid.setOnClickListener(v ->
                startActivity(new Intent(this, rfidoops.class)));

        btnExit.setOnClickListener(v -> finish());

        if (nfcAdapter == null) {
            Log.e("nfcAdapter","NFC NOT Supported on this device!");
            dbcl.showScrollableErrorDialog(MainActivity.this, "Error", "NFC NOT Supported on this device!");
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

        switchMode.setOnCheckedChangeListener((buttonView, isChecked) -> {
            if (isChecked) {
                operation_type = 1;
                switchMode.setText("Barge");
                background_main.setBackgroundResource(R.drawable.barge_ops);
            } else {
                operation_type = 0;
                switchMode.setText("Truck");
                background_main.setBackgroundResource(R.drawable.truck_ops);
            }
        });
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (nfcAdapter != null)
        {
            nfcAdapter.enableForegroundDispatch(this, pendingIntent, null, null);
        }

        dbclass db = new dbclass(this);
        Cursor cursor = db.getSetupData();

        if (cursor != null && cursor.moveToFirst())
        {
            locationId = cursor.getLong(cursor.getColumnIndexOrThrow("location_id"));
            sublocationId = cursor.getLong(cursor.getColumnIndexOrThrow("sublocation_id"));
            vendorId = cursor.getLong(cursor.getColumnIndexOrThrow("vendor_id"));
            descriptionId = cursor.getLong(cursor.getColumnIndexOrThrow("description_id"));
            destinationId = cursor.getLong(cursor.getColumnIndexOrThrow("destination_id"));
            old_route_id = cursor.getLong(cursor.getColumnIndexOrThrow("old_route_id"));
            new_route1_id = cursor.getLong(cursor.getColumnIndexOrThrow("new_route1_id"));
            new_route2_id = cursor.getLong(cursor.getColumnIndexOrThrow("new_route2_id"));
            new_route3_id = cursor.getLong(cursor.getColumnIndexOrThrow("new_route3_id"));
            new_route4_id = cursor.getLong(cursor.getColumnIndexOrThrow("new_route4_id"));
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

            boolean successup = dbcl.uploadTripsheets(tripsheets,ApiConfig.TRIPSHEET_SAVE,"tripsheets");

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
        Cursor cursor1 = dbcl.getSetupnames();  // your existing query
        showSetupDetails(cursor1,this);
    }

    @Override
    protected void onPause() {
        super.onPause();
        if(nfcAdapter!=null)
        {
            nfcAdapter.disableForegroundDispatch(this);
        }

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

        if(writeRequested)
        {

            NfcA nfca = NfcA.get(tag);

            try {

                nfca.connect();

                String currentDateMONTH = "";
                String monthyear = "";
                String monthyearfull = "";

                currentDateMONTH = new SimpleDateFormat("yyMM", Locale.getDefault()).format(new Date());
                monthyear = new SimpleDateFormat("dd", Locale.getDefault())
                        .format(new Date());
                monthyearfull = new SimpleDateFormat("yyyy", Locale.getDefault())
                        .format(new Date());
                Log.e("currentDateMONTH", currentDateMONTH + " ");

                String mob_id = mobileuid;
                String trip_status;
                String trip_status_save;
                String tripsrno_org = getNextTripSheetNo();

                String part1 = tripsrno_org.substring(0, 2);
                String part2 = tripsrno_org.substring(2, 4);

                String tripstarttime = dbcl.db_format_date_time(new Date());
                String srctime = dbcl.db_format_date_time(new Date());
                long seconds = dbcl.secondsFromBaseDate();
                String tripsheetno = mob_id + currentDateMONTH + monthyear + tripsrno_org;
                String src_mob = TID;

                byte[] pageData = dbcl.readPage(nfca, 04);
                String asset_num_04 = new String(pageData, StandardCharsets.UTF_8).trim();
                pageData = dbcl.readPage(nfca, 05);
                String asset_num_05 = new String(pageData, StandardCharsets.UTF_8).trim();
                pageData = dbcl.readPage(nfca, 06);
                String asset_num_06 = new String(pageData, StandardCharsets.UTF_8).trim();

                asset_number = asset_num_04 + asset_num_05 + asset_num_06;
                Log.d("NFC_READ", "040506 : asset number " + asset_num_04 + " " + asset_num_05 + " " + asset_num_06);

                if (asset_number.trim().isEmpty()) {
                    error += "Vehicle number not found.Please get the card checked\n";
                    dbcl.showScrollableErrorDialog(MainActivity.this, "Error", error);
                    return;
                }

                String truckid = dbcl.getAssetId_fromdb(asset_number);

                pageData = dbcl.readPage(nfca, 25);
                String tripsheet_no_last2 = new String(pageData, 0, 2);//bytesToString(pageData, 0, 2);
                trip_status_read = Integer.parseInt(dbcl.bytesToString(pageData, 2, 2));

                Log.d("NFC_READ", "trip_status_read pageData" + trip_status_read + " " + Arrays.toString(pageData) + " tripsheet_no_last2 " + tripsheet_no_last2);

                pageData = dbcl.readPage(nfca, 10);
                int truck_vendor_id_card = Integer.parseInt(dbcl.bytesToString(pageData, 0, 2));

                pageData = dbcl.readPage(nfca, 34);
                int src_mob_id_card = Integer.parseInt(dbcl.bytesToString(pageData, 0, 2));

                ContentValues trip_close = new ContentValues();
                String now = null;

                pageData = dbcl.readPage(nfca, 13);
                int asset_type = Integer.parseInt(dbcl.bytesToString(pageData, 0, 2));
                int gross_wt_capacity = Integer.parseInt(dbcl.bytesToString(pageData, 2, 2));

                String asset_type_display="";
                if(asset_type==1)
                {
                    asset_type_display="Truck";
                }else if(asset_type==2)
                {
                    asset_type_display="Machine";
                }else if(asset_type==3)
                {
                    asset_type_display="Barge";
                }else if(asset_type==4)
                {
                    asset_type_display="Bowser";
                }else
                {
                    asset_type_display="Na";
                }

                now = dbcl.db_format_date_time(new Date());

                pageData = dbcl.readPage(nfca, 14);
                long tare_wt_sec_card = Long.parseLong(dbcl.bytesToString(pageData, 0, 4));
                String tare_wt_time_card;
                Log.e("tare_wt_sec_card", " " + tare_wt_sec_card);
                if (tare_wt_sec_card > 0) {
                    tare_wt_time_card = dbcl.db_format_date_time(dbcl.date_time_FromSeconds(tare_wt_sec_card));

                } else
                {
                    tare_wt_time_card = "";
                }

                pageData = dbcl.readPage(nfca, 15);
                int tare_wt_wb_log_card = Integer.parseInt(dbcl.bytesToString(pageData, 0, 2));
                int tare_wt_card = Integer.parseInt(dbcl.bytesToString(pageData, 2, 2));


                pageData = dbcl.readPage(nfca, 35);
                int dest_sec_card = Integer.parseInt(dbcl.bytesToString(pageData, 0, 4));
                String dest_time_card = null;
                Log.e("dest_sec_card", " " + dest_sec_card);

                LinkedHashMap<String, Object> card_details_map = new LinkedHashMap<>();
                card_details_map.put("Vehicle No", asset_number);
                card_details_map.put("Asset Type", asset_type_display);
                card_details_map.put("Card No", rfid_card);
                card_details_map.put("Trip Status",  dbcl.getTripStatusText(trip_status_read));
                card_details_map.put("Src Tare Weight", tare_wt_card);
                card_details_map.put("Src Tare Weight Time", dbcl.display_format_date_time(dbcl.date_time_FromSeconds(tare_wt_sec_card)));

                trip_status_read =1;
                if (asset_type == 1)
                {

                    if (trip_status_read == 1) //trip open
                    {
                        String closing_trip_num_card = "";
                        pageData = dbcl.readPage(nfca, 22);
                        Log.d("NFC_READ", "22  " + Arrays.toString(pageData));
                        String closing_trip_num_01 = new String(pageData, StandardCharsets.UTF_8).trim();
                        pageData = dbcl.readPage(nfca, 23);
                        Log.d("NFC_READ", "23  " + Arrays.toString(pageData));
                        String closing_trip_num_02 = new String(pageData, StandardCharsets.UTF_8).trim();
                        pageData = dbcl.readPage(nfca, 24);
                        Log.d("NFC_READ", "24  " + Arrays.toString(pageData));
                        String closing_trip_num_03 = new String(pageData, StandardCharsets.UTF_8).trim();


                        closing_trip_num_card = closing_trip_num_01 + closing_trip_num_02 + closing_trip_num_03 + tripsheet_no_last2;
                        Log.d("NFC_READ", "closing_trip_num_card " + closing_trip_num_card + " ");

                        card_details_map.put("Trip No", closing_trip_num_card);


                        pageData = dbcl.readPage(nfca, 27);
                        int desc_id_card = Integer.parseInt(dbcl.bytesToString(pageData, 0, 2));
                        int route_id_card = Integer.parseInt(dbcl.bytesToString(pageData, 2, 2));
                        int src_subloc_id_trip = Integer.parseInt(dbcl.get_subloc_from_routeid(route_id_card, "source_sublocation"));//fetching src sublocation
                        int dest_subloc_id_trip = Integer.parseInt(dbcl.get_subloc_from_routeid(route_id_card, "destination_sublocation"));//fetching desr sublocation
                        Log.d("NFC_READ", "dest_subloc_id_trip " + dest_subloc_id_trip + " " + sublocationId);

                        pageData = dbcl.readPage(nfca, 18);
                        //int HSD = Integer.parseInt(dbcl.bytesToString(pageData, 0, 3));
                        int HSD = dbcl.bytesToInt(pageData, 0, 3);
                        Double hsd_card = HSD / 100.0;
                        hsd_card = hsd_card + dbcl.route_consumption(route_id_card);

                        String HSD_BAL = String.valueOf(hsd_card);

                        long scaledValue = Math.round(hsd_card * 100.0);

                        String hsd_card_str = String.valueOf(scaledValue);

                        Log.e("hsd_card_str",HSD_BAL+" "+hsd_card_str);

                        card_details_map.put("Source Location", dbcl.get_subloc_name(src_subloc_id_trip));
                        card_details_map.put("Destination Location", dbcl.get_subloc_name(dest_subloc_id_trip));
                        card_details_map.put("Route", dbcl.get_route_name(route_id_card));

                        pageData = dbcl.readPage(nfca, 28);
                        int wb_src_sec_card = Integer.parseInt(dbcl.bytesToString(pageData, 0, 4));
                        String wb_src_time_card;
                        if (wb_src_sec_card > 0) {
                            wb_src_time_card = dbcl.db_format_date_time(dbcl.date_time_FromSeconds(wb_src_sec_card));
                        } else {
                            wb_src_time_card = null;
                        }

                        pageData = dbcl.readPage(nfca, 29);
                        int src_gr_wt_card = Integer.parseInt(dbcl.bytesToString(pageData, 0, 2));
                        int wb_src_gr_login_card = Integer.parseInt(dbcl.bytesToString(pageData, 2, 2));

                        pageData = dbcl.readPage(nfca, 30);
                        int wb_dest_sec_card = Integer.parseInt(dbcl.bytesToString(pageData, 0, 4));
                        String wb_dest_time_card;
                        if (wb_dest_sec_card > 0) {
                            wb_dest_time_card = dbcl.db_format_date_time(dbcl.date_time_FromSeconds(wb_dest_sec_card));
                        } else {
                            wb_dest_time_card = null;
                        }

                        pageData = dbcl.readPage(nfca, 31);
                        int dest_gr_wt_card = Integer.parseInt(dbcl.bytesToString(pageData, 0, 2));
                        int wb_dest_gr_login_card = Integer.parseInt(dbcl.bytesToString(pageData, 2, 2));

                        pageData = dbcl.readPage(nfca, 32);
                        int dest_tare_wt_sec_card = Integer.parseInt(dbcl.bytesToString(pageData, 0, 4));
                        String dest_tare_wt_time_card;
                        if (dest_tare_wt_sec_card > 0) {
                            dest_tare_wt_time_card = dbcl.db_format_date_time(dbcl.date_time_FromSeconds(dest_tare_wt_sec_card));
                        } else {
                            dest_tare_wt_time_card = null;
                        }

                        pageData = dbcl.readPage(nfca, 33);
                        int dest_tare_wt_card = Integer.parseInt(dbcl.bytesToString(pageData, 0, 2));
                        int wb_dest_tare_login_card = Integer.parseInt(dbcl.bytesToString(pageData, 2, 2));

                        pageData = dbcl.readPage(nfca, 36);
                        int src_sec_card = Integer.parseInt(dbcl.bytesToString(pageData, 0, 4));
                        String src_time_card;
                        if (src_sec_card > 0) {
                            src_time_card = dbcl.db_format_date_time(dbcl.date_time_FromSeconds(src_sec_card));

                        } else {
                            src_time_card = null;
                        }

                        pageData = dbcl.readPage(nfca, 37);
                        int vendor_id_card = Integer.parseInt(dbcl.bytesToString(pageData, 0, 2));
                        int machine_id_card = Integer.parseInt(dbcl.bytesToString(pageData, 2, 2));

                        pageData = dbcl.readPage(nfca, 38);
                        int screening_plant_id_card  = Integer.parseInt(dbcl.bytesToString(pageData, 0, 2));
                        int plant_input_product_id_card  = Integer.parseInt(dbcl.bytesToString(pageData, 2, 2));
                        //error = "testing";
                        //dest_subloc_id_trip= (int) sublocationId;
                        if ((dest_subloc_id_trip == sublocationId || route_id_card == old_route_id) && "".equals(error)) {

                            final long final_route_id;
                            long tempRouteId = route_id_card;

                            long[] routes = {old_route_id};

                            for (long r : routes) {
                                if (route_id_card == r)
                                {
                                    tempRouteId = new_route1_id;
                                    break;
                                }
                            }
                            final_route_id = tempRouteId; // ✅ now final
                            nfca.close();
                            trip_status = "02";
                            trip_status_save = "CLOSED";

                            Log.e("stringTo2Bytes", Arrays.toString(dbcl.stringTo2Bytes(trip_status)));

                            ProgressDialog dialog = new ProgressDialog(this);
                            dialog.setMessage("Writing card...");
                            dialog.setCancelable(false);
                            dialog.show();

                            Vibrator vibrator = (Vibrator) getSystemService(Context.VIBRATOR_SERVICE);


                            String finalClosing_trip_num_card = closing_trip_num_card;
                            String finalAsset_number = asset_number;
                            String finalNow = now;
                            String finalTrip_status1 = trip_status;
                            new Thread(() -> {

                                boolean success =
                                        dbcl.writeOrFail(this, tag, 18, dbcl.combineByteArrays(dbcl.stringTo3Bytes(hsd_card_str), dbcl.stringTo1Bytes("00"))) &&
                                        dbcl.writeOrFail(this, tag, 25, dbcl.combineByteArrays(tripsheet_no_last2.getBytes(StandardCharsets.UTF_8), dbcl.stringTo2Bytes(finalTrip_status1))) &&
                                        dbcl.writeOrFail(this, tag, 27, dbcl.combineByteArrays(dbcl.stringTo4Bytes(String.valueOf(desc_id_card)), dbcl.stringTo2Bytes(String.valueOf(final_route_id)))) &&
                                        dbcl.writeOrFail(this, tag, 34, dbcl.combineByteArrays(dbcl.stringTo2Bytes(String.valueOf(src_mob_id_card)), dbcl.stringTo2Bytes(TID))) &&
                                        dbcl.writeOrFail(this, tag, 35, dbcl.stringTo4Bytes(String.valueOf(seconds)));

                                runOnUiThread(() -> {
                                    dialog.dismiss();

                                    if (success) {
                                        vibrator.vibrate(100);
                                        Toast.makeText(this, "Card written successfully ✅", Toast.LENGTH_SHORT).show();

                                        trip_close.put("tripsheet_no", finalClosing_trip_num_card);
                                        trip_close.put("trip_type", "RFID");
                                        trip_close.put("initial_route_id", String.valueOf(route_id_card));
                                        trip_close.put("final_route_id", String.valueOf(final_route_id));
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
                                        trip_close.put("machine_id", machine_id_card);
                                        trip_close.put("screening_plant_id", screening_plant_id_card);
                                        trip_close.put("plant_input_product_id", plant_input_product_id_card);
                                        trip_close.put("truck_vendor_id", truck_vendor_id_card);
                                        trip_close.put("POS_UP_BIT", 1);

                                        /*if (saveTripsheet(finalClosing_trip_num_card, trip_close)) {
                                            Toast.makeText(this, "Tripsheet closed successfully", Toast.LENGTH_SHORT).show();
                                           dbcl.showScrollableErrorDialog(MainActivity.this, "Success", "Tripsheet closed successfully");

                                            trip_status_read = 2;
                                        } else {
                                            Toast.makeText(this, "Failed to close tripsheet", Toast.LENGTH_SHORT).show();
                                            //background_main.setBackgroundColor(Color.RED);
                                           dbcl.showScrollableErrorDialog(MainActivity.this, "Error", "Failed to close tripsheet");

                                        }*/
                                    }

                                });

                            }).start();

                        } else {

                            error += "WRONG DESTINATION\nPROCEED TO :- \n\n" + dbcl.get_subloc_name(dest_subloc_id_trip);

                           dbcl.showScrollableErrorDialog(MainActivity.this, "Error", error);

                        }
                        nfca.close();

                    } else {
                        trip_status_save = "OPEN";
                        trip_status = "01";

                        if(operation_type==1)
                        {
                            trip_status = "03";
                        }

                        if (tare_wt_time_card!=null) {

                            Date todaysdatetm = new Date();

                            long diffMillis = todaysdatetm.getTime() - dbcl.date_time_FromSeconds(tare_wt_sec_card).getTime();
                            long diffDays = TimeUnit.MILLISECONDS.toDays(diffMillis);
                            if (diffDays > ext_tare_validity_days) {
                                error += "TARE WEIGHT WAS TAKEN " + diffDays + " DAYS BEFORE(VALID FOR "+ext_tare_validity_days+" DAYS). KINDLY GET THE TARE WEIGHT DONE.\n";
                            }
                        } else
                        {
                            error += "TARE WEIGHT TIME NOT FOUND\n";
                        }

                        Log.e("dest_sec_card", dest_sec_card + " ");
                        Log.e("machine_start_date_time", machine_start_date_time + " ");


                        if (machine_id.isEmpty()) {
                            error += "MACHINE ID NOT FOUND.PLEASE TAP THE MACHINE CARD BEFORE ISSUING NEW TRIPS.\n";
                        }

                        if (!machine_start_date_time.isEmpty())
                        {
                            if (isToday(machine_start_date_time))
                            {
                                // ✅ OK
                            } else {
                                error += "PLEASE TAP THE MACHINE CARD BEFORE ISSUING NEW TRIPS\n";
                            }
                        } else {
                            error += "PLEASE TAP THE MACHINE CARD\n";
                        }

                        if (dest_sec_card > 0) {
                            Date desttimedate = dbcl.date_time_FromSeconds(dest_sec_card);

                            error += dbcl.checktime(desttimedate);

                        }


                        pageData = dbcl.readPage(nfca, 11);
                        String Asset_id = dbcl.bytesToString(pageData, 0, 3);
                        int asset_status = Integer.parseInt(dbcl.bytesToString(pageData, 3, 1));

                        Log.d("NFC_READ", "11 : asset number " + Asset_id + " " + asset_status);

                        if (asset_status == 0) {
                            //error += "VEHICLE IS INACTIVE\n";
                        }


                        Date insurance_validity = null;
                        Date rdtax_validity = null;
                        Date fitness_validity = null;
                        Date puc_validity = null;

                        Calendar todayCal = Calendar.getInstance(TimeZone.getTimeZone("Asia/Kolkata"));
                        Date today = todayCal.getTime();

                        pageData = dbcl.readPage(nfca, 16);
                        int ins_val_seconds = Integer.parseInt(dbcl.bytesToString(pageData, 0, 2));
                        int rdt_val_seconds = Integer.parseInt(dbcl.bytesToString(pageData, 2, 2));

                        pageData = dbcl.readPage(nfca, 17);
                        int fit_val_seconds = Integer.parseInt(dbcl.bytesToString(pageData, 0, 2));
                        int puc_val_seconds = Integer.parseInt(dbcl.bytesToString(pageData, 2, 2));

                        if (ins_val_seconds > 0)
                        {
                            insurance_validity = dbcl.dateFromDays(ins_val_seconds);
                            if (insurance_validity.before(today)) {
                                error += "INSURANCE VALIDITY IS OVER\n";
                            }
                        } else
                        {
                            error += "INSURANCE VALIDITY DATE NOT FOUND\n";
                        }
                        Log.d("NFC_READ", "25 : ins_val_seconds " + ins_val_seconds + " insurance_validity" + insurance_validity);

                        if(LOCATION_ID_MOBILE != destinationId )
                        {
                            if (rdt_val_seconds > 0) {
                                rdtax_validity = dbcl.dateFromDays(rdt_val_seconds);
                                if (rdtax_validity.before(today)) {
                                    error += "ROAD TAX VALIDITY IS OVER\n";
                                }
                            } else {
                                error += "ROAD TAX VALIDITY DATE NOT FOUND\n";
                            }

                            Log.d("NFC_READ", "25 : rdt_val_seconds " + rdt_val_seconds + " rdtax_validity" + rdtax_validity);


                            if (fit_val_seconds > 0) {
                                fitness_validity = dbcl.dateFromDays(fit_val_seconds);
                                if (fitness_validity.before(today)) {
                                    error += "FITNESS VALIDITY IS OVER\n";
                                }
                            } else {
                                error += "FITNESS VALIDITY DATE NOT FOUND\n";
                            }

                            Log.d("NFC_READ", "25 : fit_val_seconds " + fit_val_seconds + " fitness_validity" + fitness_validity);

                            if (puc_val_seconds > 0) {
                                puc_validity = dbcl.dateFromDays(puc_val_seconds);
                                if (puc_validity.before(today)) {
                                    error += "PUC VALIDITY IS OVER\n";
                                }
                            } else {
                                error += "PUC VALIDITY DATE NOT FOUND\n";
                            }

                            Log.d("NFC_READ", "25 : puc_val_seconds " + puc_val_seconds + " puc_validity" + puc_validity);
                        }
                        nfca.close();

                        if (!"".equals(error))
                        {
                           dbcl.showScrollableErrorDialog(MainActivity.this, "Error", error);

                        } else
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
                            String finalTrip_status = trip_status;
                            new Thread(() -> {

                                runOnUiThread(() -> {

                                    if(operation_type==0)
                                    {
                                        dbcl.writeStringToTag(tag, mob_id, 22, 0);
                                        dbcl.writeStringToTag(tag, finalCurrentDateMONTH, 23, 0);
                                        dbcl.writeStringToTag(tag, finalMonthyear + part1, 24, 0);
                                        //dbcl.writeOrFail(this, tag, 19, dbcl.combineByteArrays(dbcl.stringTo2Bytes(machine_id), dbcl.stringTo2Bytes("00"))) &&
                                        //dbcl.writeOrFail(this, tag, 20, dbcl.combineByteArrays(dbcl.stringTo2Bytes("00"), dbcl.stringTo2Bytes("00"))) &&
                                    }
                                    boolean success =

                                    dbcl.writeOrFail(this, tag, 25, dbcl.combineByteArrays(part2.getBytes(StandardCharsets.UTF_8), dbcl.stringTo2Bytes(finalTrip_status))) &&
                                            dbcl.writeOrFail(this, tag, 26, dbcl.stringTo4Bytes(String.valueOf(seconds))) &&
                                            dbcl.writeOrFail(this, tag, 27, dbcl.combineByteArrays(dbcl.stringTo2Bytes(String.valueOf(descriptionId)), dbcl.stringTo2Bytes(String.valueOf(routeId)))) &&

                                            dbcl.writeOrFail(this, tag, 28, dbcl.stringTo4Bytes("00")) &&
                                            dbcl.writeOrFail(this, tag, 29, dbcl.combineByteArrays(dbcl.stringTo2Bytes("00"), dbcl.stringTo2Bytes("00"))) &&
                                            dbcl.writeOrFail(this, tag, 30, dbcl.stringTo4Bytes("00")) &&
                                            dbcl.writeOrFail(this, tag, 31, dbcl.combineByteArrays(dbcl.stringTo2Bytes("00"), dbcl.stringTo2Bytes("00"))) &&
                                            dbcl.writeOrFail(this, tag, 32, dbcl.stringTo4Bytes("00")) &&
                                            dbcl.writeOrFail(this, tag, 33, dbcl.combineByteArrays(dbcl.stringTo2Bytes("00"), dbcl.stringTo2Bytes("00"))) &&

                                            dbcl.writeOrFail(this, tag, 34, dbcl.combineByteArrays(dbcl.stringTo2Bytes(TID), dbcl.stringTo2Bytes("00"))) &&
                                            dbcl.writeOrFail(this, tag, 35, dbcl.stringTo4Bytes("00")) &&
                                            dbcl.writeOrFail(this, tag, 36, dbcl.stringTo4Bytes(String.valueOf(seconds))) &&
                                            dbcl.writeOrFail(this, tag, 37, dbcl.combineByteArrays(dbcl.stringTo2Bytes(String.valueOf(vendorId)), dbcl.stringTo2Bytes(machine_id))) &&
                                            dbcl.writeOrFail(this, tag, 38, dbcl.combineByteArrays(dbcl.stringTo2Bytes(String.valueOf(screening_plant_id)), dbcl.stringTo2Bytes(String.valueOf(plant_input_product_id))));

                                    dialog.dismiss();

                                    if (success) {
                                        vibrator.vibrate(100);
                                        Toast.makeText(this, "Card written successfully ✅", Toast.LENGTH_SHORT).show();

                                        if(operation_type==0)
                                        {
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
                                            trip_close.put("src_mobile", TID);
                                            //trip_close.put("dest_time", srctime);
                                            //trip_close.put("dest_mobile", src_mob);
                                            trip_close.put("created_at", finalNow1);
                                            trip_close.put("machine_id", machine_id);
                                            trip_close.put("screening_plant_id", screening_plant_id);
                                            trip_close.put("plant_input_product_id", plant_input_product_id);
                                            trip_close.put("truck_vendor_id", truck_vendor_id_card);
                                            trip_close.put("POS_UP_BIT", 1);
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
                                            trip_close.putNull("dest_tare_wt");

                                            Log.e("srctime", srctime + " " + tripstarttime);


                                            boolean tripsuccess = saveTripsheet(tripsheetno, trip_close);

                                            if (tripsuccess) {
                                                Toast.makeText(this, "Tripsheet saved successfully", Toast.LENGTH_SHORT).show();
                                               dbcl.showScrollableErrorDialog(MainActivity.this, "Success", "Tripsheet saved successfully");

                                            } else {
                                                Toast.makeText(this, "Failed to save tripsheet", Toast.LENGTH_SHORT).show();

                                               dbcl.showScrollableErrorDialog(MainActivity.this, "Error", "Failed to save tripsheet");

                                            }
                                        }else
                                        {
                                           dbcl.showScrollableErrorDialog(MainActivity.this, "Success", "Trip successfully written on the card");
                                        }
                                    } else
                                    {
                                        Toast.makeText(this, "Failed to write card.Please retry again", Toast.LENGTH_SHORT).show();

                                       dbcl.showScrollableErrorDialog(MainActivity.this, "Error", "Failed to write card.Please retry again");

                                    }
                                });

                            }).start();
                        }
                    }

                    card_details_map.put("Trip Status", dbcl.getTripStatusText(trip_status_read));
                }else if (asset_type == 2)
                {
                    nfca.close();
                    String finalAsset_number2 = asset_number;
                    String finalNow2 = now;
                    new Thread(() -> {

                        boolean success =
                                dbcl.writeOrFail(this, tag, 19, dbcl.combineByteArrays(dbcl.stringTo2Bytes(String.valueOf(sublocationId)), dbcl.stringTo2Bytes("00"))) &&
                                dbcl.writeOrFail(this, tag, 20, dbcl.stringTo4Bytes(String.valueOf(seconds)));

                        runOnUiThread(() -> {


                            if (success) {

                                SQLiteDatabase dbinst = dbcl.getWritableDatabase();

                                ContentValues values = new ContentValues();
                                values.put("sublocation_id", sublocationId);
                                values.put("src_mobile", TID);
                                values.put("date_time", finalNow2);
                                values.put("registration_no", finalAsset_number2);
                                values.put("machine_id", truckid);
                                try
                                {
                                    long result = dbinst.insert("machinery", null, values);

                                    if (result == -1) {
                                        // ❌ Insert failed
                                       dbcl.showScrollableErrorDialog(MainActivity.this, "Error", "Failed to setup machinery");
                                    } else {
                                        // ✅ Insert success
                                       dbcl.showScrollableErrorDialog(MainActivity.this, "Success", "Machinery setup was successful");
                                        Cursor cursor = dbcl.getSetupnames();  // your existing query
                                        showSetupDetails(cursor,this);

                                        dbcl.saveUidAndLocationToPrefs(MainActivity.this);

                                        SharedPreferences prefs = getSharedPreferences("APP_PREFS", MODE_PRIVATE);

                                        mobileuid = prefs.getString("UID", "");
                                        TID = prefs.getString("TID", "");
                                        machine_id = prefs.getString("machine_id", "");
                                        machine_start_date_time = prefs.getString("date_time", "");
                                        int_tare_validity_days = prefs.getInt("int_tare_validity_days",0);
                                        ext_tare_validity_days = prefs.getInt("ext_tare_validity_days", 0);
                                        screening_plant_id = prefs.getInt("screening_plant_id", 0);
                                        plant_input_product_id = prefs.getInt("plant_input_product_id", 0);
                                    }
                                } catch (Exception e)
                                {
                                    Log.e("DB_EXCEPTION", e.getMessage());
                                   dbcl.showScrollableErrorDialog(MainActivity.this, "Exception", e.getMessage());
                                }
                            } else
                            {
                                // ✅ Insert success
                               dbcl.showScrollableErrorDialog(MainActivity.this, "Error", "Error while writing machinery card");
                            }

                        });

                    }).start();
                }
                dbcl.onRfidTapped(cardDetails,card_details_map,error,handler,hideRunnable,detailsContainer,MainActivity.this);
            } catch (Exception e)
            {
                e.printStackTrace();
               dbcl.showScrollableErrorDialog(MainActivity.this,"Error", e.getMessage());
            } finally
            {
                try
                {
                    nfca.close();
                } catch (Exception ignored)
                {
                   dbcl.showScrollableErrorDialog(MainActivity.this,"Error", ignored.getMessage());
                }
            }

            dbclass dbs = new dbclass(this);
            SQLiteDatabase db = dbs.getWritableDatabase();

            new Thread(() -> {

                JSONArray tripsheets = dbcl.getTripsheetsForUpload(this);

                if (tripsheets.length() == 0)
                {
                    Log.d("UPLOAD", "No pending tripsheets");
                    return;
                }

                boolean successup = dbcl.uploadTripsheets(tripsheets,ApiConfig.TRIPSHEET_SAVE,"tripsheets");

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
        CardView cardSetupDetails1 = findViewById(R.id.cardSetupDetails1);
        CardView cardSetupDetails2 = findViewById(R.id.cardSetupDetails2);
        LinearLayout container = findViewById(R.id.setupDetailsContainer);
        LinearLayout container1 = findViewById(R.id.setupDetailsContainer1);
        LinearLayout container2 = findViewById(R.id.setupDetailsContainer2);

        container.removeAllViews();
        container1.removeAllViews();
        container2.removeAllViews();

        if (cursor != null && cursor.moveToFirst()) {

            dbcl.addRow(context,container, "This Sublocation","");
            dbcl.addRow(context,container, "Sublocation", cursor.getString(cursor.getColumnIndexOrThrow("sublocation_name")));
            dbcl.addRow(context,container, "Vendor", cursor.getString(cursor.getColumnIndexOrThrow("company_name")));
            dbcl.addRow(context,container, "Ore", cursor.getString(cursor.getColumnIndexOrThrow("description")));
            dbcl.addRow(context,container, "Destination Location", cursor.getString(cursor.getColumnIndexOrThrow("location_name")));
            dbcl.addRow(context,container, "Destination Sublocation", cursor.getString(cursor.getColumnIndexOrThrow("dest_subloc")));
            dbcl.addRow(context,container, "Route", cursor.getString(cursor.getColumnIndexOrThrow("route_name")));
            dbcl.addRow(context,container, "Other sublocations", cursor.getString(cursor.getColumnIndexOrThrow("old_route")));
            dbcl.addRow(context,container, "New Route1", cursor.getString(cursor.getColumnIndexOrThrow("new_route1")));
            //dbcl.addRow(context,container, "Route2", cursor.getString(cursor.getColumnIndexOrThrow("new_route2")));
            //dbcl.addRow(context,container, "Route3", cursor.getString(cursor.getColumnIndexOrThrow("new_route3")));
            //dbcl.addRow(context,container, "Route4", cursor.getString(cursor.getColumnIndexOrThrow("new_route4")));
            dbcl.addRow(context,container1, "Machine", cursor.getString(cursor.getColumnIndexOrThrow("registration_no")));
            dbcl.addRow(context,container1, "Machine Start Time", dbcl.display_format_date_time(cursor.getString(cursor.getColumnIndexOrThrow("machinery_st_tm"))));
            dbcl.addRow(context,container2, "Screening plant", dbcl.display_format_date_time(cursor.getString(cursor.getColumnIndexOrThrow("screeningplant"))));
            dbcl.addRow(context,container2, "Plant input product ", dbcl.display_format_date_time(cursor.getString(cursor.getColumnIndexOrThrow("inputore"))));

            cardSetupDetails.setVisibility(View.VISIBLE);
            cardSetupDetails1.setVisibility(View.VISIBLE);
            cardSetupDetails2.setVisibility(View.VISIBLE);
            cursor.close();
        } else {
            cardSetupDetails.setVisibility(View.GONE);
            cardSetupDetails1.setVisibility(View.GONE);
            cardSetupDetails2.setVisibility(View.GONE);
        }
    }

    public boolean isToday(String machine_start_date_time) {
        if (machine_start_date_time == null || machine_start_date_time.isEmpty()) {
            return false;
        }

        try {
            // DB format
            SimpleDateFormat dbFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault());
            Date dbDate = dbFormat.parse(machine_start_date_time);

            // Get today's date
            Date today = new Date();

            // Compare only date (ignore time)
            SimpleDateFormat dateOnly = new SimpleDateFormat("yyyyMMdd", Locale.getDefault());

            return dateOnly.format(dbDate).equals(dateOnly.format(today));

        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }

}
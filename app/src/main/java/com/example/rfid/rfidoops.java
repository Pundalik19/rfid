package com.example.rfid;

import androidx.appcompat.app.AppCompatActivity;
import androidx.cardview.widget.CardView;

import android.app.AlertDialog;
import android.app.PendingIntent;
import android.content.ContentValues;
import android.content.Intent;
import android.content.IntentFilter;
import android.graphics.Color;
import android.graphics.Typeface;
import android.nfc.NfcAdapter;
import android.nfc.Tag;
import android.nfc.tech.NfcA;
import android.os.Bundle;
import android.os.Handler;
import android.provider.Settings;
import android.text.Spannable;
import android.text.SpannableStringBuilder;
import android.text.style.ForegroundColorSpan;
import android.text.style.StyleSpan;
import android.util.Log;
import android.view.View;
import android.view.animation.AnimationUtils;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Date;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.TimeZone;
import java.util.concurrent.TimeUnit;

public class rfidoops extends AppCompatActivity {
    Button syncbutton;
    LinkedHashMap<String, String> cardData = new LinkedHashMap<>();
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
    Button btnRead, btnClear, btnIssue,btnSetup;
    TextView txtData;

    Tag currentTag;
    private boolean isReadMode = false;
    private ProgressBar loadingBar;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_rfidoops);
        loadingBar = findViewById(R.id.loadingBar);
        btnRead = findViewById(R.id.btnRead);
        btnClear = findViewById(R.id.btnClear);
        btnIssue = findViewById(R.id.btnIssue);
        btnSetup = findViewById(R.id.btnSetup);
        txtData = findViewById(R.id.txtData);
        detailsContainer = findViewById(R.id.detailsCo);
        cardDetails = findViewById(R.id.cardDetails);

        dbcl = new dbclass(this);

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

        btnRead.setOnClickListener(v -> {

            txtData.setText("Tap card to read...");

            detailsContainer.removeAllViews();
            isReadMode = true;   // enable reading
        });

        btnClear.setOnClickListener(v -> {
            isReadMode = false;  // disable reading
            detailsContainer.removeAllViews();
            if(currentTag != null){

                new AlertDialog.Builder(this)
                        .setTitle("Confirm")
                        .setMessage("Are you sure you want to clear this card?")
                        .setPositiveButton("Yes", (dialog, which) -> {
                            clearCard(currentTag); // proceed
                        })
                        .setNegativeButton("No", (dialog, which) -> {
                            dialog.dismiss(); // cancel
                        })
                        .show();
            }
        });

        btnSetup.setOnClickListener(v -> {
            Intent i = new Intent(this, setup.class);
            startActivity(i);
        });

        btnIssue.setOnClickListener(v -> {
            Intent i = new Intent(this, rfid_issue.class);
            startActivity(i);
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
    @Override
    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);

        Tag tag = intent.getParcelableExtra(NfcAdapter.EXTRA_TAG);

        if (tag != null) {
            currentTag = tag;
            if (isReadMode) {
                readCard(tag);   // ✅ read only when button pressed
                isReadMode = false; // reset after reading (optional)
                btnClear.setEnabled(true);
            } else {
                // ❌ ignore scan if not in read mode
                Toast.makeText(this, "Press READ button first", Toast.LENGTH_SHORT).show();
            }
        }
    }

    private void readCard(Tag tag){

        try{
            NfcA nfca = NfcA.get(tag);
            nfca.connect();
            String asset_number ="";
            String error ="";

            byte[] pageData = dbcl.readPage(nfca, 04);
            String asset_num_04 = new String(pageData, StandardCharsets.UTF_8).trim();
            pageData = dbcl.readPage(nfca, 05);
            String asset_num_05 = new String(pageData, StandardCharsets.UTF_8).trim();
            pageData = dbcl.readPage(nfca, 06);
            String asset_num_06 = new String(pageData, StandardCharsets.UTF_8).trim();

            asset_number = asset_num_04+asset_num_05+asset_num_06;
            Log.d("NFC_READ", "040506 : asset number "+asset_num_04+" "+asset_num_05+" "+asset_num_06);

            String truckid = dbcl.getAssetId_fromdb(asset_number);

            pageData = dbcl.readPage(nfca, 11);
            String Asset_id  = dbcl.bytesToString(pageData, 0, 3);
            int asset_status = Integer.parseInt(dbcl.bytesToString(pageData, 3, 1));

            Log.d("NFC_READ", "11 : asset number "+Asset_id+" "+asset_status);

            String asset_status_display="";
            if(asset_status==0)
            {
                error += "VEHICLE IS INACTIVE\n";
                asset_status_display="Inactive";
            }else if(asset_status==1)
            {
                asset_status_display="Active";
            }

            pageData = dbcl.readPage(nfca, 12);
            int card_iss_sec_card  = Integer.parseInt(dbcl.bytesToString(pageData, 0, 4));
            String card_iss_time_card;
            Log.e("card_iss_time_card"," "+card_iss_sec_card);
            if(card_iss_sec_card > 0)
            {
                card_iss_time_card =  dbcl.display_format_date_time(dbcl.date_time_FromSeconds(card_iss_sec_card));
            }else
            {
                card_iss_time_card = null;
                error += "CARD ISSUE DATE TIME NOT FOUND\n";
            }

            pageData = dbcl.readPage(nfca, 13);
            int asset_type = Integer.parseInt(dbcl.bytesToString(pageData, 0, 2));
            int gross_wt_capacity = Integer.parseInt(dbcl.bytesToString(pageData, 2, 2));

            Log.d("NFC_READ", "11 : gross_wt_capacity"+gross_wt_capacity+" ");

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
            }else
            {
                asset_type_display="Na";
            }

            pageData = dbcl.readPage(nfca, 14);
            long tare_wt_sec_card  = Long.parseLong(dbcl.bytesToString(pageData, 0, 4));
            String tare_wt_time_card;
            Log.e("tare_wt_sec_card"," "+tare_wt_sec_card);
            if(tare_wt_sec_card > 0)
            {
                tare_wt_time_card =  dbcl.display_format_date_time(dbcl.date_time_FromSeconds(tare_wt_sec_card));

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

            pageData = dbcl.readPage(nfca, 25);
            String tripsheet_no_last2  = new String(pageData, 0, 2);//bytesToString(pageData, 0, 2);
            trip_status_read  = Integer.parseInt(dbcl.bytesToString(pageData, 2, 2));

            String trip_status_display="";
            if(trip_status_read==1)
            {
                trip_status_display="Open";
            }else if(trip_status_read==2)
            {
                trip_status_display="Closed";
            }
            Log.d("NFC_READ", "trip_status_read pageData"+trip_status_read+ " "+Arrays.toString(pageData)+" tripsheet_no_last2 "+tripsheet_no_last2);

            closing_trip_num_card = closing_trip_num_01+closing_trip_num_02+closing_trip_num_03+tripsheet_no_last2;
            Log.d("NFC_READ", "closing_trip_num_card "+closing_trip_num_card+" ");

            pageData = dbcl.readPage(nfca, 27);
            int desc_id_card = Integer.parseInt(dbcl.bytesToString(pageData, 0, 2));
            int route_id_card = Integer.parseInt(dbcl.bytesToString(pageData, 2, 2));
            int src_subloc_id_trip = Integer.parseInt(dbcl.get_subloc_from_routeid(route_id_card,"source_sublocation"));//fetching src sublocation
            int dest_subloc_id_trip = Integer.parseInt(dbcl.get_subloc_from_routeid(route_id_card,"destination_sublocation"));//fetching desr sublocation
            Log.d("NFC_READ", "dest_subloc_id_trip "+dest_subloc_id_trip+" "+sublocationId);

            pageData = dbcl.readPage(nfca, 28);
            int wb_src_sec_card  = Integer.parseInt(dbcl.bytesToString(pageData, 0, 4));
            String wb_src_time_card;
            if(wb_src_sec_card > 0)
            {
                wb_src_time_card =  dbcl.display_format_date_time(dbcl.date_time_FromSeconds(wb_src_sec_card));
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
                wb_dest_time_card =  dbcl.display_format_date_time(dbcl.date_time_FromSeconds(wb_dest_sec_card));
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
                dest_tare_wt_time_card =  dbcl.display_format_date_time(dbcl.date_time_FromSeconds(dest_tare_wt_sec_card));
            } else {
                dest_tare_wt_time_card = null;
            }

            pageData = dbcl.readPage(nfca, 33);
            int dest_tare_wt_card = Integer.parseInt(dbcl.bytesToString(pageData, 0, 2));
            int wb_dest_tare_login_card = Integer.parseInt(dbcl.bytesToString(pageData, 2, 2));


            pageData = dbcl.readPage(nfca, 34);
            int src_mob_id_card  = Integer.parseInt(dbcl.bytesToString(pageData, 0, 2));

            ContentValues trip_close = new ContentValues();
            String now = null;

            now =  dbcl.db_format_date_time(new Date());

            pageData = dbcl.readPage(nfca, 35);
            int dest_sec_card  = Integer.parseInt(dbcl.bytesToString(pageData, 0, 4));
            String dest_time_card = null;
            Log.e("dest_sec_card"," "+dest_sec_card);


            pageData = dbcl.readPage(nfca, 36);
            int src_sec_card  = Integer.parseInt(dbcl.bytesToString(pageData, 0, 4));
            String src_time_card;
            if(src_sec_card > 0)
            {
                src_time_card =  dbcl.display_format_date_time(dbcl.date_time_FromSeconds(src_sec_card));

            } else {
                src_time_card = null;
            }

            pageData = dbcl.readPage(nfca, 37);
            int vendor_id_card  = Integer.parseInt(dbcl.bytesToString(pageData, 0, 2));


            cardData.put("Asset Number", asset_number);
            cardData.put("Asset ID", Asset_id);
            cardData.put("Asset Status", asset_status_display);
            cardData.put("Asset Type", asset_type_display);
            cardData.put("Gross Weight Capacity", String.valueOf(gross_wt_capacity));

            cardData.put("Card Issue Date Time", card_iss_time_card);
            cardData.put("Tare Weight Time", tare_wt_time_card);
            cardData.put("Tare Weight WB", String.valueOf(tare_wt_wb_log_card));
            cardData.put("Tare Weight", String.valueOf(tare_wt_card));

            cardData.put("Insurance Validity", String.valueOf(dbcl.display_format_date(insurance_validity)));
            cardData.put("Road Tax Validity", String.valueOf(dbcl.display_format_date(rdtax_validity)));

            cardData.put("Fitness Validity", String.valueOf(dbcl.display_format_date(fitness_validity)));
            cardData.put("PUC Validity", String.valueOf(dbcl.display_format_date(puc_validity)));

            cardData.put("HSD Balance", HSD_BAL);

            cardData.put("Trip Sheet Number", closing_trip_num_card);
            cardData.put("Trip Status", trip_status_display);

            cardData.put("Route", dbcl.get_route_name(route_id_card));
            cardData.put("Product",dbcl.get_ore_name(desc_id_card));

            cardData.put("Source WB Time", wb_src_time_card);
            cardData.put("Source Gross Weight", String.valueOf(src_gr_wt_card));

            cardData.put("Destination WB Time", wb_dest_time_card);
            cardData.put("Destination Gross Weight", String.valueOf(dest_gr_wt_card));

            //cardData.put("Destination Tare Time", dest_tare_wt_time_card);
            //cardData.put("Destination Tare Weight", String.valueOf(dest_tare_wt_card));

            cardData.put("Source Mobile", dbcl.get_mob_name(src_mob_id_card));
            cardData.put("Vendor", dbcl.get_vendor_name(vendor_id_card));
            nfca.close();

            detailsContainer.removeAllViews();

            for (Map.Entry<String, String> entry : cardData.entrySet())
            {

                String key = entry.getKey();
                Object valObj = entry.getValue();
                String value = valObj == null ? "--" : String.valueOf(valObj);

                View row = dbcl.createRow(rfidoops.this,key, value,error);
                detailsContainer.addView(row);
            }

            cardDetails.startAnimation(
                    AnimationUtils.loadAnimation(this, android.R.anim.slide_in_left)
            );
            cardDetails.setVisibility(View.VISIBLE);

        }catch(Exception e){
            e.printStackTrace();
        }
    }

    private void clearCard(Tag tag){
        loadingBar.setVisibility(View.VISIBLE);
        txtData.setText("Clearing card...");
        new Thread(() ->
        {
        try{

                NfcA nfca = NfcA.get(tag);
                nfca.connect();

                for(int page=4; page<=37; page++){

                    byte[] cmd = new byte[]{
                            (byte)0xA2,
                            (byte)page,
                            0x00,
                            0x00,
                            0x00,
                            0x00
                    };

                    nfca.transceive(cmd);
                }

                nfca.close();
                runOnUiThread(() -> {
                    loadingBar.setVisibility(View.GONE);
                    txtData.setText("✅ Card Cleared");
                });


            } catch (Exception e) {
                e.printStackTrace();

                runOnUiThread(() -> {
                    loadingBar.setVisibility(View.GONE);
                    txtData.setText("❌ Error clearing card");
                });
            }
        }).start();
    }
}
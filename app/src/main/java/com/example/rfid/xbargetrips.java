package com.example.rfid;

import androidx.appcompat.app.AppCompatActivity;
import androidx.cardview.widget.CardView;

import android.app.AlertDialog;
import android.app.PendingIntent;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.database.sqlite.SQLiteDatabase;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.ColorDrawable;
import android.nfc.NfcAdapter;
import android.nfc.Tag;
import android.nfc.tech.NfcA;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.provider.Settings;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;

import java.nio.charset.StandardCharsets;
import java.util.Date;

public class xbargetrips extends AppCompatActivity {
    NfcAdapter nfcAdapter;
    private IntentFilter[] filters;
    private String[][] techList;
    Tag currentTag;
    Handler handler;
    Runnable hideRunnable;

    dbclass dbcl;
    private static AlertDialog currentDialog;
    private boolean isReadMode = false;
    PendingIntent pendingIntent;
    CardView cardDetails;
    LinearLayout detailsContainer;
    SQLiteDatabase db ;
    String Asset_id="";
    double hsdbalasset=0.00;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_xbargetrips);

        cardDetails = findViewById(R.id.cardDetails);
        detailsContainer = findViewById(R.id.detailsContainer);

        dbcl = new dbclass(this);

        handler = new Handler(Looper.getMainLooper());
        hideRunnable = () -> cardDetails.setVisibility(View.GONE);

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

        pendingIntent = PendingIntent.getActivity(
                this, 0,
                new Intent(this, getClass()).addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP),
                PendingIntent.FLAG_MUTABLE);

        IntentFilter tech = new IntentFilter(NfcAdapter.ACTION_TECH_DISCOVERED);
        filters = new IntentFilter[]{tech};
    }

    protected void onResume() {
        super.onResume();
        if (nfcAdapter != null) {
            nfcAdapter.enableForegroundDispatch(this, pendingIntent, null, null);
        }
        //String qty = dbcl.getBowserStock(db, 0);
        //hsdqty.setText(qty);
    }

    @Override
    protected void onPause() {
        super.onPause();
        if (nfcAdapter != null) {
            nfcAdapter.disableForegroundDispatch(this);
        }
    }

    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);

        Tag tag = intent.getParcelableExtra(NfcAdapter.EXTRA_TAG);

        if (tag != null) {
            currentTag = tag;
            readCard(currentTag);
        }
    }

    private void readCard(Tag tag){
        ContentValues card_details = new ContentValues();
        try{
            Asset_id="";
            hsdbalasset = 0.00;
            NfcA nfca = NfcA.get(tag);
            nfca.connect();
            String asset_number ="";
            String error ="";
            byte[] uid = tag.getId();   // 🔥 RFID UID
            String rfid_card = dbcl.bytesToHex(uid);

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
            Asset_id  = dbcl.bytesToString(pageData, 0, 3);
            int asset_status = Integer.parseInt(dbcl.bytesToString(pageData, 3, 1));

            Log.d("NFC_READ", "11 : asset number "+Asset_id+" "+asset_status);

            pageData = dbcl.readPage(nfca, 13);
            int asset_type = Integer.parseInt(dbcl.bytesToString(pageData, 0, 2));
            int gross_wt_capacity = Integer.parseInt(dbcl.bytesToString(pageData, 2, 2));

            pageData = dbcl.readPage(nfca, 18);
            int sublocation_id = Integer.parseInt(dbcl.bytesToString(pageData, 0, 2));


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

            pageData = dbcl.readPage(nfca, 18);
            int HSD = Integer.parseInt(dbcl.bytesToString(pageData, 0, 3));

            hsdbalasset = HSD/100.0;

            Log.e("HSD_BAL"," "+HSD+" "+hsdbalasset);

            pageData = dbcl.readPage(nfca, 22);
            int HSDCARD = Integer.parseInt(dbcl.bytesToString(pageData, 0, 3));

            double qtyValue = HSDCARD / 100.0;   // 👈 important: 100.0 (not 100)

            if(asset_type == 4)
            {
                if (qtyValue > 0)
                {
                    String QTY = String.valueOf(qtyValue);

                    ContentValues values = new ContentValues();
                    values.put("sublocation_id", sublocation_id);
                    values.put("bowser_id", Asset_id);
                    values.put("qty", QTY);
                    values.put("date_time", dbcl.db_format_date_time(new Date()));
                    values.put("pos_up_bit", 1);

                    nfca.close();
                    new Thread(() ->
                    {

                        boolean success =
                                dbcl.writeOrFail(this, tag, 22, dbcl.combineByteArrays(dbcl.stringTo3Bytes("00"),dbcl.stringTo1Bytes("00")));
                        runOnUiThread(() ->
                        {
                            if (success)
                            {

                                long result = db.insert("bowser_stock", null, values);

                                if (result != -1) {
                                    //Toast.makeText(this, "Setup successfully", Toast.LENGTH_SHORT).show();
                                   dbcl.showScrollableErrorDialog(this, "Success", "Bowser qty ("+QTY+") transferred successfully");
                                    //clearAllFields();
                                } else {
                                    //Toast.makeText(this, "Save failed", Toast.LENGTH_SHORT).show();
                                   dbcl.showScrollableErrorDialog(this, "Error", "Failed to transfer qty to mobile");
                                }
                            } else
                            {
                                Log.e("ERROR", "Failed to write bowser card");
                               dbcl.showScrollableErrorDialog(this, "Error", "Failed to write bowser card");
                            }
                        });
                    }).start();


                } else {
                    Log.e("ERROR", "Quantity must be greater than 0");
                   dbcl.showScrollableErrorDialog(this, "Error", "Bowser quantity must be greater than 0");
                }

            }else
            {
                nfca.close();

                card_details.put("Card No", rfid_card);
                card_details.put("Vehicle No", asset_number);
                card_details.put("Asset Type", asset_type_display);

                dbcl.onRfidTapped(cardDetails, card_details, error, handler, hideRunnable, detailsContainer,this );



            }

        }catch(Exception e){
            e.printStackTrace();
        }
    }

}
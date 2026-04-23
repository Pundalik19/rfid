package com.example.rfid;

import static com.example.rfid.dbclass.showScrollableErrorDialog;

import androidx.appcompat.app.AppCompatActivity;
import androidx.cardview.widget.CardView;

import android.app.AlertDialog;
import android.app.PendingIntent;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.database.Cursor;
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
import android.view.animation.AnimationUtils;
import android.widget.ArrayAdapter;
import android.widget.AutoCompleteTextView;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Date;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.TimeZone;
import java.util.concurrent.TimeUnit;


public class xbowser extends activity_base {
    NfcAdapter nfcAdapter;
    List<SpinnerItem> bowser_stock_list;
    private static AlertDialog currentDialog;
    private boolean isReadMode = false;
    PendingIntent pendingIntent;
    int bowser_stock_id =-1;
    dbclass dbcl;
    private IntentFilter[] filters;
    private String[][] techList;
    boolean clearRequested = false;
    boolean writeRequested = false;
    LinearLayout detailsContainer;
    Tag currentTag;
    SQLiteDatabase db ;
    TextView hsdqty,Refresh;
    CardView cardDetails;
    AutoCompleteTextView dispensing_subloc;

    Handler handler;
    Runnable hideRunnable;
    LinearLayout layoutQtySection;
    EditText editTextQty;
    Button btnSave,btnReset;
    String Asset_id="";;

    double hsdbalasset=0.00;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_bowser);

        hsdqty = findViewById(R.id.hsdqty);
        Refresh = findViewById(R.id.Refresh);
        dispensing_subloc = findViewById(R.id.dispensing_subloc);
        detailsContainer = findViewById(R.id.detailsContainer);
        cardDetails = findViewById(R.id.cardDetails);
        Button btnPlus = findViewById(R.id.btnPlus);
        Button btnMinus = findViewById(R.id.btnMinus);
        editTextQty = findViewById(R.id.editTextQty);
        layoutQtySection = findViewById(R.id.layoutQtySection);
        btnSave = findViewById(R.id.btnSave);
        btnReset = findViewById(R.id.btnReset);

        layoutQtySection.setVisibility(View.GONE);
        isReadMode = true;
        btnReset.setOnClickListener(v -> {
            isReadMode = true;
            layoutQtySection.setVisibility(View.GONE);
            readCard(currentTag);
        });

        btnSave.setOnClickListener(v -> {

            String fillerror = "";

            String qtyStr = editTextQty.getText().toString().trim();

            if(bowser_stock_id==-1)
            {
                fillerror += "Please select the stock location";
            }
            if (qtyStr.isEmpty()) {
                editTextQty.setError("Enter quantity");
                fillerror += "Enter quantity\n";
                return;
            }

            double qtyfilled = Double.parseDouble(qtyStr);

            if (qtyfilled <= 0) {
                editTextQty.setError("Qty must be greater than 0");
                fillerror += "Qty must be greater than 0\n";
                return;
            }

            // get current balance
            double currentBalance_stock = Double.parseDouble(dbcl.getBowserStock(bowser_stock_id));

            if(qtyfilled > currentBalance_stock)
            {
                fillerror += "Stock balance left is "+currentBalance_stock+"\n";
            }
            if(qtyfilled > hsdbalasset)
            {
                fillerror += "Asset hsd balance left  is "+hsdbalasset+"\n";
            }

            if(fillerror.isEmpty())
            {

                double newBalance = currentBalance_stock - qtyfilled;
                double card_hsdbalasset = hsdbalasset - qtyfilled;
                long scaledValue = Math.round(card_hsdbalasset * 100);
                String hsdbalasset_str = String.valueOf(scaledValue);

                Log.e("HSD","hsdbalasset" + card_hsdbalasset+" qtyfilled"+qtyfilled+" currentBalance_stock"+currentBalance_stock+" newBalance"+newBalance+" hsdbalasset_str"+hsdbalasset_str);

                boolean ok = dbcl.writeOrFail(xbowser.this, currentTag, 18, dbcl.combineByteArrays(dbcl.stringTo3Bytes(hsdbalasset_str), dbcl.stringTo1Bytes("00")));
                if (ok)
                {


                    ContentValues stockValues = new ContentValues();
                    stockValues.put("qty", newBalance);
                    stockValues.put("updated_time", dbcl.db_format_date_time(new Date()));
                    stockValues.put("pos_up_bit", 1);

                    int rows = db.update("bowser_stock", stockValues, "id=?",
                            new String[]{String.valueOf(bowser_stock_id)});

                    if (rows >0)
                    {
                        ContentValues values = new ContentValues();
                        values.put("asset_id", Asset_id);
                        values.put("bowser_stock_id", bowser_stock_id);
                        values.put("qty", qtyfilled); 
                        values.put("balance_qty", newBalance);
                        values.put("pos_up_bit", 1);
                        values.put("datetime", dbcl.db_format_date_time(new Date()));

                        long result = db.insert("hsd_transactions", null, values);

                        if (result != -1)
                        {
                            hsdqty.setText(dbcl.getBowserStock(bowser_stock_id));
                            // reset input
                            editTextQty.setText("");
                            Asset_id="";
                            hsdbalasset=0.00;
                            showScrollableErrorDialog(this, "Success", "Transaction saved successfully");
                            layoutQtySection.setVisibility(View.GONE);
                            isReadMode=true;
                        } else
                        {
                            showScrollableErrorDialog(this, "Error", "Transaction failed");
                        }
                    } else
                    {
                        showScrollableErrorDialog(this, "Error","Failed to update stock");
                    }
                }else
                {
                    showScrollableErrorDialog(this, "Error", "Failed to write card.");
                }

            }else
            {
                showScrollableErrorDialog(this, "Error", fillerror);
            }
        });

        double step = 1.0; // change to 0.5 or 0.1 if needed
        btnPlus.setOnClickListener(v -> {
            double value = getQtyValue(editTextQty);
            value += step;
            editTextQty.setText(String.format("%.2f", value));
        });

        btnMinus.setOnClickListener(v -> {
            double value = getQtyValue(editTextQty);

            if (value > step) {
                value -= step;
            } else {
                value = 0;
            }

            editTextQty.setText(String.format("%.2f", value));
        });
        btnPlus.setOnLongClickListener(v -> {
            // increase faster logic
            return true;
        });

        handler = new Handler(Looper.getMainLooper());
        hideRunnable = () -> cardDetails.setVisibility(View.GONE);

        dbcl = new dbclass(this);
        db = dbcl.getWritableDatabase();
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

        load_bowser_loc();
        showdropdowns(dispensing_subloc);

        Refresh.setOnClickListener(v -> {
            hsdqty.setText(dbcl.getBowserStock(bowser_stock_id));
        });


    }

    private void showdropdowns(AutoCompleteTextView actv)
    {
        actv.setOnClickListener(v -> actv.showDropDown());

    }

    private void load_bowser_loc() {
        Log.e("loadDestloc"," loadDestloc");
        hsdqty.setText("0");
        bowser_stock_list = dbcl.get_bowser_stock();

        ArrayAdapter<SpinnerItem> adapter =
                new ArrayAdapter<>(
                        this,
                        android.R.layout.simple_dropdown_item_1line,
                        bowser_stock_list
                );

        dispensing_subloc.setAdapter(adapter);
        dispensing_subloc.setThreshold(1);

        dispensing_subloc.setOnItemClickListener((parent, view, position, id) -> {

            SpinnerItem item = (SpinnerItem) parent.getItemAtPosition(position);
            bowser_stock_id = item.id;

            hsdqty.setText(dbcl.getBowserStock(item.id));
        });

        Log.e("getBowserStock", bowser_stock_id + " fun");

    }
    @Override
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
            /*if (isReadMode) {
                readCard(tag);   // ✅ read only when button pressed
                isReadMode = false; // reset after reading (optional)

            } else {
                // ❌ ignore scan if not in read mode
                Toast.makeText(this, "Press reset button to read the card again", Toast.LENGTH_SHORT).show();
            }*/
        }
    }

    private void readCard(Tag tag){

        LinkedHashMap<String, Object> card_details = new LinkedHashMap<>();

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
                                    showScrollableErrorDialog(this, "Success", "Bowser qty ("+QTY+") transferred successfully");
                                    //clearAllFields();
                                } else {
                                    //Toast.makeText(this, "Save failed", Toast.LENGTH_SHORT).show();
                                    showScrollableErrorDialog(this, "Error", "Failed to transfer qty to mobile");
                                }
                            } else
                            {
                                Log.e("ERROR", "Failed to write bowser card");
                                showScrollableErrorDialog(this, "Error", "Failed to write bowser card");
                            }
                        });
                    }).start();


                } else {
                    Log.e("ERROR", "Quantity must be greater than 0");
                    showScrollableErrorDialog(this, "Error", "Bowser quantity must be greater than 0");
                }

            }else
            {
                nfca.close();
                editTextQty.setText("");



                card_details.put("Card No", rfid_card);
                card_details.put("Vehicle No", asset_number);
                card_details.put("Asset Type", asset_type_display);
                card_details.put("HSD Balance", hsdbalasset);

                dbcl.onRfidTapped(cardDetails, card_details, error, handler, hideRunnable, detailsContainer,this );

                if(hsdbalasset>0)
                {
                    layoutQtySection.setAlpha(0f);
                    layoutQtySection.setVisibility(View.VISIBLE);
                    layoutQtySection.animate().alpha(1f).setDuration(300);

                }else
                {
                    layoutQtySection.setVisibility(View.GONE);
                    showScrollableErrorDialog(this, "Error", "Hsd balance is "+hsdbalasset);
                }

            }

        }catch(Exception e){
            e.printStackTrace();
        }
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
    private double getQtyValue(EditText editText) {
        try {
            String text = editText.getText().toString();
            if (text.isEmpty()) return 0.0;
            return Double.parseDouble(text);
        } catch (Exception e) {
            return 0.0;
        }
    }

}
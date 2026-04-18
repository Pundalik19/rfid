package com.example.rfid;

import androidx.appcompat.app.AppCompatActivity;

import android.app.AlertDialog;
import android.app.PendingIntent;
import android.content.ContentValues;
import android.content.Context;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.ColorDrawable;
import android.nfc.NfcAdapter;
import android.os.Bundle;
import android.util.Log;
import android.widget.ArrayAdapter;
import android.widget.AutoCompleteTextView;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ScrollView;
import android.widget.Spinner;
import android.widget.TextView;

import java.util.List;

public class change_dest extends AppCompatActivity {
    AutoCompleteTextView old_route,new_route1, new_route2, new_route3, new_route4;
    TextView txtStatus;
    EditText editMessage;
    Button btn_save_route,btnWRfid;
    private static AlertDialog currentDialog;
    NfcAdapter nfcAdapter;
    boolean writeMode = false;
    PendingIntent pendingIntent;

    Spinner spLocation, spSublocation, spProduct, spRoute,spDest_subloc;

    List<SpinnerItem> old_route_list,new_route_list;
    dbclass db;
    AutoCompleteTextView spVendor,spOreName, spType, spSize, spGrade, spDescription,spDestination;

    int old_route_id = -1;
    int sublocationId = -1;
    int productId = -1;
    int vendorId = -1;
    int destinationId = -1;
    int routeId = -1;

    int oreId = -1;
    int typeId = -1;
    int sizeId = -1;
    int gradeId = -1;
    int descriptionId = -1;
    int dest_subloc_id = -1;
    String mobileuid ="";

    int new_route1_id = -1;
    int new_route2_id = -1;
    int new_route3_id = -1;
    int new_route4_id = -1;

    int locationId = -1;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_change_dest);

        db = new dbclass(this);
        old_route = findViewById(R.id.old_route);
        new_route1 = findViewById(R.id.new_route1);
        new_route2 = findViewById(R.id.new_route2);
        new_route3 = findViewById(R.id.new_route3);
        new_route4 = findViewById(R.id.new_route4);
        btn_save_route = findViewById(R.id.btn_save_route);
        Button btnClear = findViewById(R.id.btnClear);

        btnClear.setOnClickListener(v -> clearAllFields());

        SharedPreferences prefs = getSharedPreferences("APP_PREFS", MODE_PRIVATE);
        mobileuid = prefs.getString("UID", "");

        Cursor cursor = db.getSetupData();
        if (cursor != null && cursor.moveToFirst()) {

            //saved_ocationId = cursor.getLong(cursor.getColumnIndexOrThrow("location_id"));
            sublocationId = cursor.getInt(cursor.getColumnIndexOrThrow("sublocation_id"));
            routeId = cursor.getInt(cursor.getColumnIndexOrThrow("route_id"));
            vendorId = cursor.getInt(cursor.getColumnIndexOrThrow("vendor_id"));
            oreId = cursor.getInt(cursor.getColumnIndexOrThrow("ore_id"));
            typeId = cursor.getInt(cursor.getColumnIndexOrThrow("type_id"));
            sizeId = cursor.getInt(cursor.getColumnIndexOrThrow("size_id"));
            gradeId = cursor.getInt(cursor.getColumnIndexOrThrow("grade_id"));
            destinationId = cursor.getInt(cursor.getColumnIndexOrThrow("destination_id"));
            descriptionId = cursor.getInt(cursor.getColumnIndexOrThrow("description_id"));
            dest_subloc_id = cursor.getInt(cursor.getColumnIndexOrThrow("dest_subloc_id"));

            Log.e("setup"," locationId "+locationId+" sublocationId "+sublocationId+" vendorId "+vendorId+" descriptionId "+descriptionId+" destinationId "+destinationId+" routeId "+routeId);

            cursor.close();

        }

        Long locationIdsp = prefs.getLong("LOCATION_ID", 0);
        Log.e("locationIdsp","mobileuid "+mobileuid+" "+locationIdsp);
        load_old_route(String.valueOf(locationIdsp),-1);
        showdropdowns(old_route);
        btn_save_route.setOnClickListener(v -> {

            Log.e("old_route_id"," "+old_route_id);
            Log.e("new_route1_id"," "+new_route1_id);
            Log.e("new_route2_id"," "+new_route2_id);
            Log.e("new_route3_id"," "+new_route3_id);
            Log.e("new_route4_id"," "+new_route4_id);

            if (old_route_id == -1 || (new_route1_id == -1 && new_route2_id == -1 && new_route3_id == -1 && new_route4_id == -1)) {

                //Toast.makeText(this, "Please complete all selections", Toast.LENGTH_SHORT).show();
                db.showScrollableErrorDialog(this, "Error","Please complete all selections");
                return;
            }

            SQLiteDatabase dbw = db.getWritableDatabase();
            ContentValues cv = new ContentValues();

            cv.put("old_route_id", old_route_id);
            cv.put("new_route1_id", new_route1_id);
            cv.put("new_route2_id", new_route2_id);
            cv.put("new_route3_id", new_route3_id);
            cv.put("new_route4_id", new_route4_id);
            cv.put("POS_UP_BIT", 1);

            int result = dbw.update(
                    "setup_data",
                    cv,
                    "id = (SELECT MAX(id) FROM setup_data)", // WHERE condition
                    null // which row to update
            );

            if (result >0) {
                //Toast.makeText(this, "Setup successfully", Toast.LENGTH_SHORT).show();
               db.showScrollableErrorDialog(this, "Success","Updated successful");
                //clearAllFields();
            } else {
                //Toast.makeText(this, "Save failed", Toast.LENGTH_SHORT).show();
                db.showScrollableErrorDialog(this, "Error","Updat failed");
            }

            dbw.close();
        });
    }

    private void showdropdowns(AutoCompleteTextView actv)
    {
        actv.setOnClickListener(v -> actv.showDropDown());

    }
    private void load_old_route(String mobile_destination_id,int selectedId) {
        Log.e("loadDestloc"," loadDestloc");
        old_route_list = db.getoldroutes(mobile_destination_id);

        ArrayAdapter<SpinnerItem> adapter =
                new ArrayAdapter<>(
                        this,
                        android.R.layout.simple_dropdown_item_1line,
                        old_route_list
                );

        old_route.setAdapter(adapter);
        old_route.setThreshold(1);

        old_route.setOnItemClickListener((parent, view, position, id) -> {
            new_route1.setText("");
            new_route2.setText("");
            new_route3.setText("");
            new_route4.setText("");

            SpinnerItem item = (SpinnerItem) parent.getItemAtPosition(position);
            old_route_id = item.id;
            Log.e("destinationId", "destinationId ID = " + item.id);
            load_new_route(mobile_destination_id, String.valueOf(old_route_id));
        });

        Log.e("loadDestloc", selectedId + " fun");

        // 🎯 Find item by ID
        SpinnerItem selectedItem = null;
        for (SpinnerItem item : old_route_list) {
            Log.e("loadDestloc", item.id + " " + selectedId);
            if (item.id == selectedId) {
                selectedItem = item;
                break;
            }
        }

        // ✅ Set selected item safely
        if (selectedItem != null) {
            old_route.setText(selectedItem.toString(), false);
            old_route_id = selectedItem.id; // keep state in sync
        }
    }
    private void load_new_route(String mobile_destination_id,String old_route_id) {
        Log.e("loadDestloc"," loadDestloc");
        new_route1.setText("");
        new_route2.setText("");
        new_route3.setText("");
        new_route4.setText("");

        new_route_list = db.get_new_routes(mobile_destination_id,old_route_id);

        ArrayAdapter<SpinnerItem> adapter =
                new ArrayAdapter<>(
                        this,
                        android.R.layout.simple_dropdown_item_1line,
                        new_route_list
                );

        new_route1.setAdapter(adapter);
        new_route2.setAdapter(adapter);
        new_route3.setAdapter(adapter);
        new_route4.setAdapter(adapter);
        new_route1.setThreshold(1);
        new_route2.setThreshold(1);
        new_route3.setThreshold(1);
        new_route4.setThreshold(1);

        new_route1.setOnItemClickListener((parent, view, position, id) -> {

            SpinnerItem item = (SpinnerItem) parent.getItemAtPosition(position);
            new_route1_id = item.id;
            Log.e("destinationId", "destinationId ID = " + item.id);

        });
        new_route2.setOnItemClickListener((parent, view, position, id) -> {

            SpinnerItem item = (SpinnerItem) parent.getItemAtPosition(position);
            new_route2_id = item.id;
            Log.e("destinationId", "destinationId ID = " + item.id);

        });

        new_route3.setOnItemClickListener((parent, view, position, id) -> {

            SpinnerItem item = (SpinnerItem) parent.getItemAtPosition(position);
            new_route3_id = item.id;
            Log.e("destinationId", "destinationId ID = " + item.id);

        });

        new_route4.setOnItemClickListener((parent, view, position, id) -> {

            SpinnerItem item = (SpinnerItem) parent.getItemAtPosition(position);
            new_route4_id = item.id;
            Log.e("destinationId", "destinationId ID = " + item.id);

        });

        showdropdowns(new_route1);
        showdropdowns(new_route2);
        showdropdowns(new_route3);
        showdropdowns(new_route4);

    }

    private void clearAllFields() {

        // Reset IDs
        old_route_id = -1;
        new_route1_id = -1;
        new_route2_id = -1;
        new_route3_id = -1;
        new_route4_id = -1;

        // Reset AutoCompleteTextViews
        new_route1.setText("");
        new_route2.setText("");
        new_route3.setText("");
        new_route4.setText("");
        old_route.setText("");

    }
}
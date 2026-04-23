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
import android.util.Base64;
import android.util.Log;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.AutoCompleteTextView;
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

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

public class setup extends activity_base {
    TextView txtStatus;
    EditText editMessage;
    Button btnWrite,btnWRfid;
    private static AlertDialog currentDialog;
    NfcAdapter nfcAdapter;
    boolean writeMode = false;
    PendingIntent pendingIntent;

    Spinner spLocation, spSublocation, spProduct, spRoute,spDest_subloc;

    List<SpinnerItem> locList, dest_sublist,subList, prodList, vendorList, destList, routeList,destloclist;
    dbclass db;
    AutoCompleteTextView spVendor,spOreName, spType, spSize, spGrade, spDescription,spDestination;

    int locationId = -1;
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

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_setup);

        db = new dbclass(this);
        spLocation = findViewById(R.id.spLocation);
        spSublocation = findViewById(R.id.spSublocation);
        spDest_subloc = findViewById(R.id.spDest_subloc);
        spProduct = findViewById(R.id.spProduct);
        //spVendor = findViewById(R.id.spVendor);
        spVendor = findViewById(R.id.spVendor);
        spDestination = findViewById(R.id.spDestination);
        spRoute = findViewById(R.id.spRoute);
        btnWRfid = findViewById(R.id.btnWriteRfid);
        spOreName = findViewById(R.id.spOreName);
        spType = findViewById(R.id.spType);
        spSize = findViewById(R.id.spSize);
        spGrade = findViewById(R.id.spGrade);
        spDescription = findViewById(R.id.spDescription);
        Button btnClear = findViewById(R.id.btnClear);

        btnClear.setOnClickListener(v -> clearAllFields());

        LinearLayout layoutOwnerBlock = findViewById(R.id.layoutOwnerBlock);
        Button btnToggle = findViewById(R.id.btnToggleOwner);

        db.saveUidAndLocationToPrefs(setup.this);

        SharedPreferences prefs = getSharedPreferences("APP_PREFS", MODE_PRIVATE);

        mobileuid = prefs.getString("UID", "");
        Log.e("locationIdsp","mobileuid "+mobileuid);
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

        Log.e("locationIdsp"," "+locationIdsp);

        loadLocations(String.valueOf(locationIdsp),spLocation);
        loadProducts(0);
        loadVendors(0,vendorId);
        loadDestloc(0, destinationId);
        load_destSublocations(destinationId,dest_subloc_id);
        //loadDestinations(0);
        loadOre(oreId);
        loadRoutes(sublocationId,dest_subloc_id,routeId);
        spLocation.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {

                SpinnerItem item = (SpinnerItem) parent.getItemAtPosition(position);

                loadSublocations(item.id,sublocationId);

            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {}
        });

        spDestination.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {

                SpinnerItem item = (SpinnerItem) parent.getItemAtPosition(position);

                load_destSublocations(item.id,dest_subloc_id);

            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {}
        });

        spSublocation.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {

                SpinnerItem item = (SpinnerItem) parent.getItemAtPosition(position);
                sublocationId = item.id;
                //loadRoutes(item.id,routeId);

            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {}
        });

        spDest_subloc.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {

                SpinnerItem item = (SpinnerItem) parent.getItemAtPosition(position);
                dest_subloc_id = item.id;
                loadRoutes(sublocationId,item.id,routeId);

            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {}
        });

        set_selection_dropdown(db.getTypes(spOreName.getText().toString()),typeId,spType,typeId);
        set_selection_dropdown(db.getSizes(spOreName.getText().toString(),spType.getText().toString()),sizeId,spSize,sizeId);
        set_selection_dropdown(db.getGrades(spOreName.getText().toString(),spType.getText().toString(),spSize.getText().toString()),gradeId,spGrade,gradeId);
        set_selection_dropdown(db.getDescriptions(spOreName.getText().toString(),spType.getText().toString(),spSize.getText().toString(),spGrade.getText().toString()),descriptionId,spDescription,descriptionId);

        showdropdowns(spDestination);
        showdropdowns(spVendor);
        showdropdowns(spOreName);
        showdropdowns(spType);
        showdropdowns(spSize);
        showdropdowns(spGrade);
        showdropdowns(spDescription);

        /*spType.setEnabled(false);
        spSize.setEnabled(false);
        spGrade.setEnabled(false);
        spDescription.setEnabled(false);*/



        spType.setOnItemClickListener((parent, view, position, id) -> {

            String oreName = spOreName.getText().toString();
            String type = spType.getText().toString();
            SpinnerItem item = (SpinnerItem) parent.getItemAtPosition(position);
            typeId = item.id;
            Log.e("spType"," "+oreName+type);

            spSize.setText("");
            spGrade.setText("");
            spDescription.setText("");

            spSize.setEnabled(true);
            spGrade.setEnabled(false);
            spDescription.setEnabled(false);

            spSize.setAdapter(new ArrayAdapter<>(
                    this,
                    android.R.layout.simple_dropdown_item_1line,
                    db.getSizes(oreName, type)
            ));

            spSize.showDropDown();
        });

        spSize.setOnItemClickListener((parent, view, position, id) -> {
            SpinnerItem item = (SpinnerItem) parent.getItemAtPosition(position);
            sizeId = item.id;
            String oreName = spOreName.getText().toString();
            String type = spType.getText().toString();
            String size = spSize.getText().toString();

            spGrade.setText("");
            spDescription.setText("");

            spGrade.setEnabled(true);
            spDescription.setEnabled(false);

            spGrade.setAdapter(new ArrayAdapter<>(
                    this,
                    android.R.layout.simple_dropdown_item_1line,
                    db.getGrades(oreName, type, size)
            ));

            spGrade.showDropDown();
        });

        spGrade.setOnItemClickListener((parent, view, position, id) -> {

            SpinnerItem item = (SpinnerItem) parent.getItemAtPosition(position);
            gradeId = item.id;
            String oreName = spOreName.getText().toString();
            String type = spType.getText().toString();
            String size = spSize.getText().toString();
            String grade = spGrade.getText().toString();

            List<SpinnerItem> spgrdaelist = db.getDescriptions(oreName, type, size, grade);

            spDescription.setText("");
            spDescription.setEnabled(true);

            spDescription.setAdapter(new ArrayAdapter<>(
                    this,
                    android.R.layout.simple_dropdown_item_1line,
                    db.getDescriptions(oreName, type, size, grade)
            ));

            spDescription.showDropDown();
        });

        spDescription.setOnItemClickListener((parent, view, position, id) -> {
            SpinnerItem item = (SpinnerItem) parent.getItemAtPosition(position);
            descriptionId = item.id;
        });

        btnWRfid.setOnClickListener(v -> {

            Log.e("locationId"," "+locationId);
            Log.e("sublocationId"," "+sublocationId);
            Log.e("vendorId"," "+vendorId);
            Log.e("oreId"," "+oreId);
            Log.e("typeId"," "+typeId);
            Log.e("sizeId"," "+sizeId);
            Log.e("gradeId"," "+gradeId);
            Log.e("descriptionId"," "+descriptionId);
            Log.e("destinationId"," "+destinationId);
            Log.e("routeId"," "+routeId);
            // || vendorId == -1 || descriptionId == -1 || routeId == -1
            if (sublocationId == -1) {
                //Toast.makeText(this, "Please complete all selections", Toast.LENGTH_SHORT).show();
                showScrollableErrorDialog(this, "Error","Please complete all selections");
                return;
            }

            SQLiteDatabase dbw = db.getWritableDatabase();
            ContentValues cv = new ContentValues();

            cv.put("location_id", locationId);
            cv.put("sublocation_id", sublocationId);
            cv.put("vendor_id", vendorId);
            cv.put("ore_id", oreId);
            cv.put("type_id", typeId);
            cv.put("size_id", sizeId);
            cv.put("grade_id", gradeId);
            cv.put("description_id", descriptionId);
            cv.put("destination_id", destinationId);
            cv.put("route_id", routeId);
            cv.put("dest_subloc_id", dest_subloc_id);
            cv.put("POS_UP_BIT", 1);

            long result = dbw.insert("setup_data", null, cv);

            if (result != -1) {
                //Toast.makeText(this, "Setup successfully", Toast.LENGTH_SHORT).show();
                showScrollableErrorDialog(this, "Success","Setup successful");
                //clearAllFields();
            } else {
                //Toast.makeText(this, "Save failed", Toast.LENGTH_SHORT).show();
                showScrollableErrorDialog(this, "Error","Setup failed");
            }

            dbw.close();
        });
    }


    private void clearAllFields() {

        // Reset IDs
        locationId = -1;
        sublocationId = -1;
        productId = -1;
        vendorId = -1;
        destinationId = -1;
        routeId = -1;

        oreId = -1;
        typeId = -1;
        sizeId = -1;
        gradeId = -1;
        descriptionId = -1;
        dest_subloc_id = -1;

        // Reset Spinners
        if (spLocation.getAdapter() != null) spLocation.setSelection(0);
        if (spSublocation.getAdapter() != null) spSublocation.setSelection(0);
        if (spDestination.getAdapter() != null) spDestination.setSelection(0);
        if (spRoute.getAdapter() != null) spRoute.setSelection(0);

        // Reset AutoCompleteTextViews
        spVendor.setText("");
        spOreName.setText("");
        spType.setText("");
        spSize.setText("");
        spGrade.setText("");
        spDescription.setText("");

        // Disable dependent dropdowns
        spType.setEnabled(false);
        spSize.setEnabled(false);
        spGrade.setEnabled(false);
        spDescription.setEnabled(false);
    }
    private void showdropdowns(AutoCompleteTextView actv)
    {
        actv.setOnClickListener(v -> actv.showDropDown());
        /*actv.setOnFocusChangeListener((v, hasFocus) -> {
            if (hasFocus) {
                actv.showDropDown();
            }
        });*/
    }

    private void clearSpinner(Spinner... spinners) {
        for (Spinner spinner : spinners) {
            spinner.setAdapter(null);
        }
    }

    private void loadLocations(String loc,Spinner spLocation) {
        locList = db.getLocations(loc);
        ArrayAdapter<SpinnerItem> adapter =
                new ArrayAdapter<>(this, android.R.layout.simple_spinner_item, locList);
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spLocation.setAdapter(adapter);
    }
    private void loadSublocations(int locId,int selected) {
        subList = db.getSublocations(locId);
        spSublocation.setAdapter(new ArrayAdapter<>(this, android.R.layout.simple_spinner_item, subList));
        Log.e("loadSublocations",selected+" fun");
        int position = 0;
        for (int i = 0; i < subList.size(); i++) {
            Log.e("loadSublocations",subList.get(i).id+" "+selected);
            if (subList.get(i).id == selected) {
                position = i;
                break;
            }
        }

        spSublocation.setSelection(position, false);
    }

    private void load_destSublocations(int locId,int selected) {
        dest_sublist = db.getSublocations(locId);
        spDest_subloc.setAdapter(new ArrayAdapter<>(this, android.R.layout.simple_spinner_item, dest_sublist));
        Log.e("loadSublocations",selected+" fun");
        int position = 0;
        for (int i = 0; i < dest_sublist.size(); i++) {
            Log.e("loadSublocations",dest_sublist.get(i).id+" "+selected);
            if (dest_sublist.get(i).id == selected) {
                position = i;
                break;
            }
        }

        spDest_subloc.setSelection(position, false);
    }

    private void loadProducts(int subId) {
        prodList = db.getProducts(subId);
        spProduct.setAdapter(new ArrayAdapter<>(this, android.R.layout.simple_spinner_item, prodList));

        /*spProduct.setOnItemClickListener((parent, view, position, id) -> {
            SpinnerItem item = (SpinnerItem) parent.getItemAtPosition(position);

            Log.e("Vendor", "Selected ID = " + item.id);
        });*/
    }

    private void loadVendors(int productId, int selectedId) {

        vendorList = db.getVendors(productId);

        ArrayAdapter<SpinnerItem> adapter =
                new ArrayAdapter<>(
                        this,
                        android.R.layout.simple_dropdown_item_1line,
                        vendorList
                );

        spVendor.setAdapter(adapter);
        spVendor.setThreshold(1);

        spVendor.setOnItemClickListener((parent, view, position, id) -> {

            SpinnerItem item = (SpinnerItem) parent.getItemAtPosition(position);
            vendorId = item.id;
            Log.e("Vendor", "Selected ID = " + item.id);
        });

        Log.e("loadVendors", selectedId + " fun");

        // 🎯 Find item by ID
        SpinnerItem selectedItem = null;
        for (SpinnerItem item : vendorList) {
            Log.e("loadVendors", item.id + " " + selectedId);
            if (item.id == selectedId) {
                selectedItem = item;
                break;
            }
        }

        // ✅ Set selected item safely
        if (selectedItem != null) {
            spVendor.setText(selectedItem.toString(), false);
            vendorId = selectedItem.id; // keep state in sync
        }
    }

    private void loadDestloc(int productId, int selectedId) {
        Log.e("loadDestloc"," loadDestloc");
        destloclist = db.getLocations("");

        ArrayAdapter<SpinnerItem> adapter =
                new ArrayAdapter<>(
                        this,
                        android.R.layout.simple_dropdown_item_1line,
                        destloclist
                );

        spDestination.setAdapter(adapter);
        spDestination.setThreshold(1);

        spDestination.setOnItemClickListener((parent, view, position, id) -> {

            SpinnerItem item = (SpinnerItem) parent.getItemAtPosition(position);
            destinationId = item.id;
            Log.e("destinationId", "destinationId ID = " + item.id);

            load_destSublocations(item.id,dest_subloc_id);
        });

        Log.e("loadDestloc", selectedId + " fun");

        // 🎯 Find item by ID
        SpinnerItem selectedItem = null;
        for (SpinnerItem item : destloclist) {
            Log.e("loadDestloc", item.id + " " + selectedId);
            if (item.id == selectedId) {
                selectedItem = item;
                break;
            }
        }

        // ✅ Set selected item safely
        if (selectedItem != null) {
            spDestination.setText(selectedItem.toString(), false);
            destinationId = selectedItem.id; // keep state in sync
        }
    }

    private void loadOre(int selectedId) {
        List<SpinnerItem> orelist = db.getOreNames();
        ArrayAdapter<SpinnerItem> adapter =new ArrayAdapter<>(this,
                android.R.layout.simple_dropdown_item_1line,
                orelist);
        spOreName.setAdapter(adapter);
        spOreName.setThreshold(1);

        Log.e("loadOre", "loadOre= " + db.getOreNames().toArray());
        spOreName.setOnItemClickListener((parent, view, position, id) -> {

            SpinnerItem item = (SpinnerItem) parent.getItemAtPosition(position);
            oreId = item.id;

            String oreName = spOreName.getText().toString();

            spType.setText("");
            spSize.setText("");
            spGrade.setText("");
            spDescription.setText("");

            spType.setEnabled(true);
            spSize.setEnabled(false);
            spGrade.setEnabled(false);
            spDescription.setEnabled(false);
            Log.e("spType"," "+oreName);

            spType.setAdapter(new ArrayAdapter<>(
                    this,
                    android.R.layout.simple_dropdown_item_1line,
                    db.getTypes(oreName)
            ));

            spType.showDropDown();
        });

        // 🎯 Find item by ID
        SpinnerItem selectedItem = null;
        for (SpinnerItem item : orelist) {
            Log.e("loadVendors", item.id + " " + selectedId);
            if (item.id == selectedId) {
                selectedItem = item;
                break;
            }
        }

        // ✅ Set selected item safely
        if (selectedItem != null) {
            spOreName.setText(selectedItem.toString(), false);
            oreId = selectedItem.id; // keep state in sync
        }

    }

    public void set_selection_dropdown(List<SpinnerItem> datalist,int selectedId,AutoCompleteTextView actv,long ddid)
    {
        SpinnerItem selectedItem = null;
        for (SpinnerItem item : datalist) {
            Log.e("set_selection_dropdown", item.id + " " + selectedId);
            if (item.id == selectedId) {
                selectedItem = item;
                break;
            }
        }

        // ✅ Set selected item safely
        if (selectedItem != null) {
            actv.setText(selectedItem.toString(), false);
            ddid = selectedItem.id; // keep state in sync
        }
        actv.setOnClickListener(v -> actv.showDropDown());
        actv.setOnFocusChangeListener((v, hasFocus) -> {
            if (hasFocus) {
                actv.showDropDown();
            }
        });
    }

    private void loadDestinations(int vendorId) {
        destList = db.getDestinationLocations(vendorId);
        spDestination.setAdapter(new ArrayAdapter<>(this, android.R.layout.simple_spinner_item, destList));
        spDestination.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                SpinnerItem item = (SpinnerItem) parent.getItemAtPosition(position);
                destinationId = item.id;
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {}
        });
    }

    private void loadRoutes(int src_sub_id,int destId,int selected) {
        Log.e("loadRoutes","loadRoutes");
        routeList = db.getRoutes(src_sub_id,destId);
        spRoute.setAdapter(new ArrayAdapter<>(this, android.R.layout.simple_spinner_item, routeList));
        spRoute.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                SpinnerItem item = (SpinnerItem) parent.getItemAtPosition(position);
                routeId = item.id;
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {}
        });

        Log.e("loadRoutes",selected+" fun");
        int position = 0;
        for (int i = 0; i < routeList.size(); i++) {
            Log.e("loadRoutes",routeList.get(i).id+" "+selected);
            if (routeList.get(i).id == selected) {
                position = i;
                break;
            }
        }

        spRoute.setSelection(position, false);

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


}
package com.example.rfid;

import androidx.appcompat.app.AppCompatActivity;

import android.app.PendingIntent;
import android.content.ContentValues;
import android.content.Context;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
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
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

public class setup extends AppCompatActivity {
    TextView txtStatus;
    EditText editMessage;
    Button btnWrite,btnWRfid;

    NfcAdapter nfcAdapter;
    boolean writeMode = false;
    PendingIntent pendingIntent;

    Spinner spLocation, spSublocation, spProduct,  spDestination, spRoute;

    List<SpinnerItem> locList, subList, prodList, vendorList, destList, routeList;
    dbclass db;
    AutoCompleteTextView spVendor,spOreName, spType, spSize, spGrade, spDescription;

    long locationId = -1;
    long sublocationId = -1;
    long productId = -1;
    long vendorId = -1;
    long destinationId = -1;
    long routeId = -1;

    long oreId = -1;
    long typeId = -1;
    long sizeId = -1;
    long gradeId = -1;
    long descriptionId = -1;

    String mobileuid ="";
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_setup);

        db = new dbclass(this);
        spLocation = findViewById(R.id.spLocation);
        spSublocation = findViewById(R.id.spSublocation);
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

        TextView tvTruckNo = findViewById(R.id.tvTruckNo);
        TextView tvOwnerName = findViewById(R.id.tvOwnerName);
        TextView tvTareWeight = findViewById(R.id.tvTareWeight);

// Fill details
        tvTruckNo.setText("Truck No: " );
        tvOwnerName.setText("Owner: " );
        tvTareWeight.setText("Tare Weight: " );


        db.saveUidAndLocationToPrefs(setup.this);

        SharedPreferences prefs = getSharedPreferences("APP_PREFS", MODE_PRIVATE);

        mobileuid = prefs.getString("UID", "");
        String locationIdsp = prefs.getString("LOCATION_ID", "");

        loadLocations(locationIdsp);
        loadSublocations(0);
        loadProducts(0);
        loadVendors(0);
        loadDestinations(0);
        loadOre();
        spLocation.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {

                SpinnerItem item = (SpinnerItem) parent.getItemAtPosition(position);

                loadSublocations(item.id);
                Log.e("loadSublocations",item.id+" ");
                //clearSpinner(spProduct, spVendor, spDestination, spRoute);
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {}
        });

        spSublocation.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {

                SpinnerItem item = (SpinnerItem) parent.getItemAtPosition(position);
                sublocationId = item.id;
                loadRoutes(item.id);

            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {}
        });

        showdropdowns(spVendor);
        showdropdowns(spOreName);
        showdropdowns(spType);
        showdropdowns(spSize);
        showdropdowns(spGrade);
        showdropdowns(spDescription);

        spType.setEnabled(false);
        spSize.setEnabled(false);
        spGrade.setEnabled(false);
        spDescription.setEnabled(false);

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

            if (sublocationId == -1 || vendorId == -1 || oreId == -1) {
                Toast.makeText(this, "Please complete all selections", Toast.LENGTH_SHORT).show();
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
            cv.put("POS_UP_BIT", 1);

            long result = dbw.insert("setup_data", null, cv);

            if (result != -1) {
                Toast.makeText(this, "Setup saved successfully", Toast.LENGTH_SHORT).show();
                clearAllFields();
            } else {
                Toast.makeText(this, "Save failed", Toast.LENGTH_SHORT).show();
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
        actv.setOnFocusChangeListener((v, hasFocus) -> {
            if (hasFocus) {
                actv.showDropDown();
            }
        });
    }

    private void clearSpinner(Spinner... spinners) {
        for (Spinner spinner : spinners) {
            spinner.setAdapter(null);
        }
    }

    private void loadLocations(String loc) {
        locList = db.getLocations(loc);
        ArrayAdapter<SpinnerItem> adapter =
                new ArrayAdapter<>(this, android.R.layout.simple_spinner_item, locList);
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spLocation.setAdapter(adapter);
    }

    private void loadSublocations(int locId) {
        subList = db.getSublocations(locId);
        spSublocation.setAdapter(new ArrayAdapter<>(this, android.R.layout.simple_spinner_item, subList));

    }

    private void loadProducts(int subId) {
        prodList = db.getProducts(subId);
        spProduct.setAdapter(new ArrayAdapter<>(this, android.R.layout.simple_spinner_item, prodList));

        /*spProduct.setOnItemClickListener((parent, view, position, id) -> {
            SpinnerItem item = (SpinnerItem) parent.getItemAtPosition(position);

            Log.e("Vendor", "Selected ID = " + item.id);
        });*/
    }

    private void loadVendors(int productId) {
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
            vendorId = item.id ;
            Log.e("Vendor", "Selected ID = " + item.id);
        });
    }

    private void loadOre() {
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

    private void loadRoutes(int destId) {
        routeList = db.getRoutes(destId);
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

    }




}
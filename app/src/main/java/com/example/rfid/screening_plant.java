package com.example.rfid;

import androidx.appcompat.app.AppCompatActivity;

import android.app.AlertDialog;
import android.content.ContentValues;
import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.ColorDrawable;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.AutoCompleteTextView;
import android.widget.Button;
import android.widget.ScrollView;
import android.widget.Spinner;
import android.widget.TextView;

import java.util.Date;
import java.util.List;

public class screening_plant extends activity_base {
    int screening_machine_id = -1;
    int oreId = -1;
    int typeId = -1;
    private static AlertDialog currentDialog;
    int sizeId = -1;
    int gradeId = -1;
    int descriptionId = -1;
    Spinner screening_machine;
    AutoCompleteTextView spOreName, spType, spSize, spGrade, spDescription;
    List<SpinnerItem> locList;
    dbclass db;
    Button btnSavesp,btnClear;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_screening_plant);

        db = new dbclass(this);
        screening_machine = findViewById(R.id.screening_machine);
        spOreName = findViewById(R.id.spOreName);
        spType = findViewById(R.id.spType);
        spSize = findViewById(R.id.spSize);
        spGrade = findViewById(R.id.spGrade);
        spDescription = findViewById(R.id.spDescription);
        btnSavesp = findViewById(R.id.btnsavesp);
        btnClear = findViewById(R.id.btnClear);

        load_screening_machine("",screening_machine);
        loadOre(0);

        screening_machine.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {

                SpinnerItem item = (SpinnerItem) parent.getItemAtPosition(position);
                screening_machine_id = item.id;
                //loadRoutes(item.id,routeId);

            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {}
        });

        set_selection_dropdown(db.getTypes(spOreName.getText().toString()),typeId,spType,typeId);
        set_selection_dropdown(db.getSizes(spOreName.getText().toString(),spType.getText().toString()),sizeId,spSize,sizeId);
        set_selection_dropdown(db.getGrades(spOreName.getText().toString(),spType.getText().toString(),spSize.getText().toString()),gradeId,spGrade,gradeId);
        set_selection_dropdown(db.getDescriptions(spOreName.getText().toString(),spType.getText().toString(),spSize.getText().toString(),spGrade.getText().toString()),descriptionId,spDescription,descriptionId);

        showdropdowns(spOreName);
        showdropdowns(spType);
        showdropdowns(spSize);
        showdropdowns(spGrade);
        showdropdowns(spDescription);

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

        btnSavesp.setOnClickListener(v -> {

            Log.e("machine_working_id"," "+screening_machine_id);
            Log.e("oreId"," "+oreId);
            Log.e("operation_date_time"," "+db.db_format_date_time(new Date()));

            if (screening_machine_id == -1 || descriptionId == -1) {
                //Toast.makeText(this, "Please complete all selections", Toast.LENGTH_SHORT).show();
                showScrollableErrorDialog(this, "Error","Please complete all the selections");
                return;
            }

            SQLiteDatabase dbw = db.getWritableDatabase();
            ContentValues cv = new ContentValues();


            cv.put("machine_working_id", screening_machine_id);
            cv.put("ore_id", descriptionId);
            cv.put("operation_date_time",db.db_format_date_time(new Date()));
            cv.put("POS_UP_BIT", 1);

            long result = dbw.insert("screening_plant", null, cv);

            if (result != -1) {
                //Toast.makeText(this, "Setup successfully", Toast.LENGTH_SHORT).show();
                showScrollableErrorDialog(this, "Success","Screening plant setup successful");
                //clearAllFields();
            } else {
                //Toast.makeText(this, "Save failed", Toast.LENGTH_SHORT).show();
                showScrollableErrorDialog(this, "Error","Screening plant setup failed");
            }

            dbw.close();
        });
    }

    private void load_screening_machine(String loc,Spinner spLocation) {
        locList = db.get_screening_plant_machine(loc);
        ArrayAdapter<SpinnerItem> adapter =
                new ArrayAdapter<>(this, android.R.layout.simple_spinner_item, locList);
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spLocation.setAdapter(adapter);
    }
    private void showdropdowns(AutoCompleteTextView actv)
    {
        actv.setOnClickListener(v -> actv.showDropDown());
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
    private void loadOre(int selectedId)
    {
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
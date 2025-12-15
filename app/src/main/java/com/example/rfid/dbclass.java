package com.example.rfid;

import android.content.Context;
import android.database.Cursor;
import android.database.DatabaseErrorHandler;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import java.util.ArrayList;
import java.util.List;

public class dbclass extends SQLiteOpenHelper {
    SQLiteDatabase db = getWritableDatabase();

    public dbclass(@Nullable Context context) {
        super(context, "vmsb.db", null, 1);
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        db.execSQL("CREATE TABLE IF NOT EXISTS tripsheets (id INTEGER PRIMARY KEY AUTOINCREMENT, tripsheet_no TEXT NOT NULL UNIQUE, initial_route_id INTEGER NOT NULL, final_route_id INTEGER NOT NULL, rfid_id TEXT, truck_id INTEGER NOT NULL, truck_no TEXT, vendor_id INTEGER, src_time TEXT, src_mobile TEXT, wb_src_time TEXT, wb_src_gross_login INTEGER, src_tare_wt_time TEXT, wb_src_tare_login INTEGER, wb_dest_time TEXT, wb_dest_gross_login INTEGER, dest_time TEXT, dest_mobile TEXT, dest_tare_wt_time TEXT, wb_dest_tare_login INTEGER, src_gross_wt INTEGER, src_tare_wt INTEGER, src_net_wt INTEGER, dest_gross_wt INTEGER, dest_tare_wt INTEGER, dest_net_wt INTEGER, approved_by INTEGER, approved_on TEXT, ore_id INTEGER, status TEXT, created_at TEXT, updated_at TEXT);");
        db.execSQL("CREATE TABLE IF NOT EXISTS asset_masters (id INTEGER PRIMARY KEY AUTOINCREMENT, registration_no TEXT NOT NULL, asset_code TEXT, asset_type TEXT NOT NULL, owner_name TEXT NOT NULL, owner_contact TEXT NOT NULL, rc_file TEXT, vendor_id INTEGER NOT NULL, group_code TEXT, tare_weight INTEGER, tare_weight_wb INTEGER, tare_weight_time TEXT, gross_weight_capacity INTEGER NOT NULL, hsd_balance REAL NOT NULL DEFAULT 0.00, retention_amt_perc REAL NOT NULL DEFAULT 0.00, retention_max_amt REAL, created_by INTEGER NOT NULL, updated_by INTEGER NOT NULL, created_at TEXT, updated_at TEXT);");
        db.execSQL("CREATE TABLE IF NOT EXISTS locations (id INTEGER PRIMARY KEY AUTOINCREMENT, location_name TEXT NOT NULL, address TEXT, type TEXT, location TEXT, radius REAL, colour TEXT DEFAULT '#FF0000', fill_colour TEXT DEFAULT '#FB6B72', opacity TEXT DEFAULT '0.5', created_by INTEGER, updated_by INTEGER, created_at TEXT NOT NULL, updated_at TEXT NOT NULL);");
        db.execSQL("CREATE TABLE IF NOT EXISTS route_masters (id INTEGER PRIMARY KEY AUTOINCREMENT, route_code_id INTEGER NOT NULL, route_name TEXT NOT NULL, source_sublocation INTEGER NOT NULL, destination_sublocation INTEGER NOT NULL, distance INTEGER NOT NULL, trip_time_min INTEGER NOT NULL, load_capacity INTEGER NOT NULL, created_by INTEGER NOT NULL, updated_by INTEGER, created_at TEXT, updated_at TEXT);");
        db.execSQL("CREATE TABLE IF NOT EXISTS sublocations (id INTEGER PRIMARY KEY AUTOINCREMENT, location_id INTEGER NOT NULL, sublocation_name TEXT NOT NULL, point TEXT, created_by INTEGER NOT NULL, updated_by INTEGER, created_at TEXT, updated_at TEXT);");
        db.execSQL("CREATE TABLE IF NOT EXISTS vendor_masters (id INTEGER PRIMARY KEY AUTOINCREMENT, vendor_id TEXT NOT NULL, company_name TEXT NOT NULL, address TEXT, mobile TEXT, email TEXT, bank_ac_no TEXT, bank_ac_type TEXT, bank_IFSC_code TEXT, pan_no TEXT, gst_no TEXT, status INTEGER NOT NULL DEFAULT 0, created_at TEXT, updated_at TEXT);");
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {

    }

    public SQLiteDatabase getdb()
    {
        return this.db;
    }

    public List<SpinnerItem> getLocations()
    {
        List<SpinnerItem> list = new ArrayList<>();
        SQLiteDatabase db = this.getReadableDatabase();

        Cursor c = db.rawQuery("SELECT id, location_name FROM locations", null);
        while (c.moveToNext()) {
            list.add(new SpinnerItem(c.getInt(0), c.getString(1)));
        }
        c.close();
        return list;
    }

    public List<SpinnerItem> getSublocations(int locationId) {
        List<SpinnerItem> list = new ArrayList<>();
        SQLiteDatabase db = this.getReadableDatabase();

        Cursor c = db.rawQuery(
                "SELECT id, sublocation_name FROM sublocations",
                null
        );

        while (c.moveToNext()) {
            list.add(new SpinnerItem(
                    c.getInt(0),        // id
                    c.getString(1)      // name
            ));
        }
        c.close();
        return list;
    }

    public List<SpinnerItem> getProducts(int sublocationId) {
        List<SpinnerItem> list = new ArrayList<>();
        SQLiteDatabase db = this.getReadableDatabase();

        Cursor c = db.rawQuery(
                "SELECT id, product_code FROM products",null
        );

        while (c.moveToNext()) {
            list.add(new SpinnerItem(
                    c.getInt(0),        // id
                    c.getString(1)      // product_code as name
            ));
        }
        c.close();
        return list;
    }

    public List<SpinnerItem> getVendors(int productId) {
        List<SpinnerItem> list = new ArrayList<>();
        SQLiteDatabase db = this.getReadableDatabase();

        Cursor c = db.rawQuery(
                "SELECT id, company_name FROM vendor_masters",null
        );

        while (c.moveToNext()) {
            list.add(new SpinnerItem(
                    c.getInt(0),        // id
                    c.getString(1)      // vendor name
            ));
        }
        c.close();
        return list;
    }

    public List<SpinnerItem> getDestinationLocations(int vendorId) {
        List<SpinnerItem> list = new ArrayList<>();
        SQLiteDatabase db = this.getReadableDatabase();

        Cursor c = db.rawQuery(
                "SELECT id, location_name FROM locations",null
        );

        while (c.moveToNext()) {
            list.add(new SpinnerItem(
                    c.getInt(0),        // id
                    c.getString(1)      // destination name
            ));
        }
        c.close();
        return list;
    }

    public List<SpinnerItem> getRoutes(int destinationId) {
        List<SpinnerItem> list = new ArrayList<>();
        SQLiteDatabase db = this.getReadableDatabase();

        Cursor c = db.rawQuery(
                "SELECT id, route_code_id,route_name FROM route_masters",null
        );

        while (c.moveToNext()) {
            list.add(new SpinnerItem(
                    c.getInt(0),        // id
                    c.getString(1)      // route name
            ));
        }
        c.close();
        return list;
    }
}

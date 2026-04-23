package com.example.rfid;

import android.app.AlertDialog;
import android.content.ContentValues;
import android.content.Context;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.ColorDrawable;
import android.nfc.Tag;
import android.nfc.tech.NfcA;
import android.os.Handler;
import android.os.Looper;
import android.util.Base64;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.AnimationUtils;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.cardview.widget.CardView;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.security.SecureRandom;
import java.security.cert.X509Certificate;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Date;
import java.util.LinkedHashMap;
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

public class dbclass extends SQLiteOpenHelper {
    SQLiteDatabase db = getWritableDatabase();
    private static AlertDialog currentDialog;
    public dbclass(@Nullable Context context) {
        super(context, "vmsb.db", null, 11);
    }

    @Override
    public void onCreate(SQLiteDatabase db)
    {
        db.execSQL("CREATE TABLE IF NOT EXISTS tripsheets (id INTEGER PRIMARY KEY AUTOINCREMENT,trip_type  NOT NULL, tripsheet_no TEXT NOT NULL UNIQUE, initial_route_id INTEGER NOT NULL, final_route_id INTEGER NOT NULL, rfid_id TEXT, truck_id INTEGER NOT NULL, truck_no TEXT, vendor_id INTEGER, src_time TEXT, src_mobile TEXT, wb_src_time TEXT, wb_src_gross_login INTEGER, src_tare_wt_time TEXT, wb_src_tare_login INTEGER, wb_dest_time TEXT, wb_dest_gross_login INTEGER, dest_time TEXT, dest_mobile TEXT, dest_tare_wt_time TEXT, wb_dest_tare_login INTEGER, src_gross_wt INTEGER, src_tare_wt INTEGER, src_net_wt INTEGER, dest_gross_wt INTEGER, dest_tare_wt INTEGER, dest_net_wt INTEGER, approved_by INTEGER, approved_on TEXT, ore_id INTEGER, status TEXT, created_at TEXT, updated_at TEXT,POS_UP_BIT INTEGER DEFAULT 1,asset_code TEXT DEFAULT NULL,machine_id  TEXT DEFAULT 0,screening_plant_id TEXT DEFAULT 0,plant_input_product_id TEXT DEFAULT 0,truck_vendor_id INTEGER DEFAULT NULL,hsd_bal DECIMAL(7,2));");
        db.execSQL("CREATE TABLE IF NOT EXISTS asset_masters (id INTEGER PRIMARY KEY AUTOINCREMENT, registration_no TEXT NOT NULL, asset_code TEXT, asset_type TEXT NOT NULL, owner_name TEXT NOT NULL, owner_contact TEXT NOT NULL, rc_file TEXT, vendor_id INTEGER NOT NULL, group_code TEXT, tare_weight INTEGER, tare_weight_wb INTEGER, tare_weight_time TEXT, gross_weight_capacity INTEGER NOT NULL, hsd_balance REAL NOT NULL DEFAULT 0.00, retention_amt_perc REAL NOT NULL DEFAULT 0.00, retention_max_amt REAL, created_by INTEGER NOT NULL, updated_by INTEGER NOT NULL, created_at TEXT, updated_at TEXT);");
        db.execSQL("CREATE TABLE IF NOT EXISTS locations (id INTEGER PRIMARY KEY AUTOINCREMENT, location_name TEXT NOT NULL, address TEXT, type TEXT, location TEXT, radius REAL, colour TEXT DEFAULT '#FF0000', fill_colour TEXT DEFAULT '#FB6B72', opacity TEXT DEFAULT '0.5', created_by INTEGER, updated_by INTEGER);");
        db.execSQL("CREATE TABLE IF NOT EXISTS route_masters (id INTEGER PRIMARY KEY AUTOINCREMENT, route_code_id INTEGER NOT NULL, route_name TEXT NOT NULL, source_sublocation INTEGER NOT NULL, destination_sublocation INTEGER NOT NULL, distance INTEGER NOT NULL, trip_time_min INTEGER NOT NULL, load_capacity INTEGER NOT NULL, created_by INTEGER NOT NULL, updated_by INTEGER, created_at TEXT, updated_at TEXT);");
        db.execSQL("CREATE TABLE IF NOT EXISTS sublocations (id INTEGER PRIMARY KEY AUTOINCREMENT, location_id INTEGER NOT NULL, sublocation_name TEXT NOT NULL, point TEXT, created_by INTEGER NOT NULL, updated_by INTEGER, created_at TEXT, updated_at TEXT);");
        db.execSQL("CREATE TABLE IF NOT EXISTS vendor_masters (id INTEGER PRIMARY KEY AUTOINCREMENT, vendor_id TEXT NOT NULL, company_name TEXT NOT NULL, address TEXT, mobile TEXT, email TEXT, bank_ac_no TEXT, bank_ac_type TEXT, bank_IFSC_code TEXT, pan_no TEXT, gst_no TEXT, status INTEGER NOT NULL DEFAULT 0, created_at TEXT, updated_at TEXT);");
        db.execSQL("CREATE TABLE IF NOT EXISTS ore_masters (id INTEGER PRIMARY KEY AUTOINCREMENT, name TEXT NOT NULL, type TEXT NOT NULL, size TEXT NOT NULL, grade TEXT NOT NULL, description TEXT NOT NULL, created_by INTEGER NOT NULL, updated_by INTEGER, created_at DATETIME DEFAULT CURRENT_TIMESTAMP, updated_at DATETIME DEFAULT CURRENT_TIMESTAMP);");
        db.execSQL("CREATE TABLE IF NOT EXISTS setup_data (id INTEGER PRIMARY KEY AUTOINCREMENT, location_id INTEGER NOT NULL, sublocation_id INTEGER NOT NULL, vendor_id INTEGER NOT NULL, ore_id INTEGER NOT NULL, type_id INTEGER NOT NULL, size_id INTEGER NOT NULL, grade_id INTEGER NOT NULL, description_id INTEGER NOT NULL, destination_id INTEGER NOT NULL, route_id INTEGER NOT NULL,dest_subloc_id INTEGER NOT NULL, created_at TEXT DEFAULT CURRENT_TIMESTAMP, updated_at TEXT,POS_UP_BIT INTEGER DEFAULT 1,old_route_id INTEGER,new_route1_id INTEGER,new_route2_id INTEGER,new_route3_id INTEGER,new_route4_id INTEGER,updated_date_time TEXT);");
        db.execSQL("CREATE TABLE IF NOT EXISTS mobile_logins (id INTEGER PRIMARY KEY AUTOINCREMENT, login TEXT NOT NULL UNIQUE, password TEXT NOT NULL, u_id TEXT UNIQUE, location_id INTEGER NOT NULL, int_tare_validity_days INTEGER NOT NULL, ext_tare_validity_days INTEGER NOT NULL, status INTEGER NOT NULL DEFAULT 1, version TEXT, created_by INTEGER, updated_by INTEGER, created_at TEXT, updated_at TEXT);");
        db.execSQL("CREATE TABLE IF NOT EXISTS machinery (id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, sublocation_id INTEGER, src_mobile INTEGER, date_time TEXT, registration_no TEXT, machine_id INTEGER, pos_up_bit INTEGER DEFAULT 1)");
        db.execSQL("CREATE TABLE IF NOT EXISTS machineWorkingdetails (id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, registration_no INTEGER, asset_id INTEGER)");
        db.execSQL("CREATE TABLE IF NOT EXISTS machineWorkingdetails (id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, registration_no INTEGER, asset_id INTEGER)");
        db.execSQL("CREATE TABLE IF NOT EXISTS screening_plant (id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,machine_working_id INTEGER,ore_id INTEGER,operation_date_time TEXT,pos_up_bit INTEGER DEFAULT 1)");
        db.execSQL("CREATE TABLE IF NOT EXISTS bowser_stock (id INTEGER PRIMARY KEY AUTOINCREMENT,sublocation_id TEXT,bowser_id TEXT,qty DECIMAL(10,2), date_time TEXT, updated_time DATETIME  DEFAULT NULL, pos_up_bit INTEGER DEFAULT 1 )");
        db.execSQL("CREATE TABLE IF NOT EXISTS hsd_transfer (id INTEGER PRIMARY KEY AUTOINCREMENT,sublocation_id TEXT,bowser_id TEXT,qty DECIMAL(10,2), date_time TEXT,asset_id,stock_id, updated_time DATETIME DEFAULT NULL, pos_up_bit INTEGER DEFAULT 1 )");
        db.execSQL("CREATE TABLE IF NOT EXISTS hsd_transactions (id INTEGER PRIMARY KEY AUTOINCREMENT,asset_id INTEGER NOT NULL,bowser_stock_id INTEGER NOT NULL,qty TEXT,balance_qty TEXT,datetime DATETIME DEFAULT CURRENT_TIMESTAMP,pos_up_bit INTEGER DEFAULT 1,pos_up_at DATETIME DEFAULT NULL)");

    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        if (oldVersion < 2) {

            db.beginTransaction();
            try {
                db.execSQL(
                        "ALTER TABLE tripsheets ADD COLUMN asset_code TEXT DEFAULT NULL"
                );

                db.execSQL(
                        "ALTER TABLE tripsheets ADD COLUMN truck_vendor_id INTEGER DEFAULT NULL"
                );

                db.execSQL(
                        "ALTER TABLE tripsheets ADD COLUMN hsd_bal DECIMAL(7,2)"
                );

                db.setTransactionSuccessful();
            } finally {
                db.endTransaction();
            }
        }
        if (oldVersion < 3) {

            db.beginTransaction();
            try {
                db.execSQL(
                        "ALTER TABLE setup_data ADD COLUMN dest_subloc_id INTEGER NOT NULL DEFAULT 0"
                );



                db.setTransactionSuccessful();
            } finally {
                db.endTransaction();
            }
        }
        if (oldVersion < 4) {

            db.beginTransaction();
            try {
                db.execSQL(
                        "CREATE TABLE  IF NOT EXISTS  machinery (id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, sublocation_id INTEGER, src_mobile INTEGER, date_time TEXT, registration_no TEXT, pos_up_bit INTEGER DEFAULT 1)"
                );


                db.setTransactionSuccessful();
            } finally {
                db.endTransaction();
            }
        }
        if (oldVersion < 5) {

            db.beginTransaction();
            try {

                db.execSQL(
                        "ALTER TABLE machinery ADD COLUMN machine_id INTEGER NOT NULL DEFAULT 0"
                );

                db.setTransactionSuccessful();
            } finally {
                db.endTransaction();
            }
        }
        if (oldVersion < 6) {

            db.beginTransaction();
            try {
                db.execSQL(
                        "ALTER TABLE tripsheets ADD COLUMN machine_id TEXT NOT NULL DEFAULT 0"
                );

                db.execSQL(
                        "ALTER TABLE tripsheets ADD COLUMN screening_plant_id TEXT DEFAULT 0"
                );

                db.execSQL(
                        "ALTER TABLE tripsheets ADD COLUMN plant_input_product_id TEXT DEFAULT 0"
                );

                db.execSQL(
                        "ALTER TABLE mobile_logins ADD COLUMN int_tare_validity_days INTEGER DEFAULT NULL"
                );

                db.execSQL(
                        "ALTER TABLE mobile_logins ADD COLUMN ext_tare_validity_days  INTEGER DEFAULT NULL"
                );
                db.setTransactionSuccessful();
            } finally {
                db.endTransaction();
            }
        }
        if (oldVersion < 7)
        {
            db.beginTransaction();
            try {
                db.execSQL("CREATE TABLE IF NOT EXISTS machineWorkingdetails (id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, registration_no INTEGER, asset_id INTEGER)");
                db.setTransactionSuccessful();
            } finally {
                db.endTransaction();
            }
        }
        if (oldVersion < 8)
        {
            db.beginTransaction();
            try {
                db.execSQL("CREATE TABLE IF NOT EXISTS screening_plant (id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,machine_working_id INTEGER,ore_id INTEGER,operation_date_time TEXT,pos_up_bit INTEGER DEFAULT 1)");
                db.setTransactionSuccessful();
            } finally {
                db.endTransaction();
            }
        }

        if (oldVersion < 9)
        {
            db.beginTransaction();
            try {
                db.execSQL("ALTER TABLE setup_data ADD COLUMN old_route_id INTEGER");
                db.execSQL("ALTER TABLE setup_data ADD COLUMN new_route1_id INTEGER");
                db.execSQL("ALTER TABLE setup_data ADD COLUMN new_route2_id INTEGER");
                db.execSQL("ALTER TABLE setup_data ADD COLUMN new_route3_id INTEGER");
                db.execSQL("ALTER TABLE setup_data ADD COLUMN new_route4_id INTEGER");
                db.execSQL("ALTER TABLE setup_data ADD COLUMN updated_date_time TEXT");
                db.setTransactionSuccessful();
            } finally {
                db.endTransaction();
            }
        }

        if (oldVersion < 10)
        {
            db.beginTransaction();
            try {
                db.execSQL("CREATE TABLE IF NOT EXISTS bowser_stock (id INTEGER PRIMARY KEY AUTOINCREMENT,sublocation_id TEXT,bowser_id TEXT,qty DECIMAL(10,2), date_time TEXT, updated_time DATETIME  DEFAULT NULL, pos_up_bit INTEGER DEFAULT 1 )");
                db.execSQL("CREATE TABLE IF NOT EXISTS hsd_transfer (id INTEGER PRIMARY KEY AUTOINCREMENT,sublocation_id TEXT,bowser_id TEXT,qty DECIMAL(10,2), date_time TEXT,asset_id,stock_id, updated_time DATETIME DEFAULT NULL, pos_up_bit INTEGER DEFAULT 1 )");

                db.setTransactionSuccessful();
            } finally {
                db.endTransaction();
            }
        }
        if (oldVersion < 11)
        {
            db.beginTransaction();
            try {
                db.execSQL("CREATE TABLE IF NOT EXISTS hsd_transactions (id INTEGER PRIMARY KEY AUTOINCREMENT,asset_id INTEGER NOT NULL,bowser_stock_id INTEGER NOT NULL,qty TEXT,balance_qty TEXT,datetime DATETIME DEFAULT CURRENT_TIMESTAMP,pos_up_bit INTEGER DEFAULT 1,pos_up_at DATETIME DEFAULT NULL)");
                db.setTransactionSuccessful();
            } finally {
                db.endTransaction();
            }
        }
    }

    public SQLiteDatabase getdb()
    {
        return this.db;
    }

    public List<SpinnerItem> getLocations(String loc)
    {
        List<SpinnerItem> list = new ArrayList<>();
        SQLiteDatabase db = this.getReadableDatabase();

        String query = "";
        if(!loc.isEmpty())
        {
            query = "WHERE id='"+loc+"'";
        }

        Log.e("query"," "+query);

        Cursor c = db.rawQuery("SELECT id, location_name FROM locations "+query, null);
        while (c.moveToNext()) {
            list.add(new SpinnerItem(c.getInt(0), c.getString(1)));
            Log.e("query"," "+c.getString(1));
        }
        c.close();
        return list;
    }

    public List<SpinnerItem> get_screening_plant_machine(String loc)
    {
        List<SpinnerItem> list = new ArrayList<>();
        SQLiteDatabase db = this.getReadableDatabase();

        String query = "";
        if(!loc.isEmpty())
        {
            query = "WHERE id='"+loc+"'";
        }

        Log.e("query"," "+query);

        Cursor c = db.rawQuery("SELECT id, registration_no FROM machineWorkingdetails "+query, null);
        while (c.moveToNext()) {
            list.add(new SpinnerItem(c.getInt(0), c.getString(1)));
            Log.e("query"," "+c.getString(1));
        }
        c.close();
        return list;
    }
    public List<SpinnerItem> getSublocations(int locationId) {
        List<SpinnerItem> list = new ArrayList<>();
        SQLiteDatabase db = this.getReadableDatabase();

        Cursor c = db.rawQuery(
                "SELECT id, sublocation_name FROM sublocations WHERE location_id='"+locationId+"'",
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
                "SELECT id, name FROM ore_masters",null
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

    public List<SpinnerItem> getRoutes(int sourcesubId,int destinationId) {
        List<SpinnerItem> list = new ArrayList<>();
        SQLiteDatabase db = this.getReadableDatabase();

        Cursor c = db.rawQuery(
                "SELECT  id,route_name FROM route_masters WHERE source_sublocation='"+sourcesubId+"' AND destination_sublocation='"+destinationId+"'",null
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

    public List<SpinnerItem> getoldroutes(String mobile_destination_id) {
        List<SpinnerItem> list = new ArrayList<>();
        SQLiteDatabase db = this.getReadableDatabase();

        Cursor c = db.rawQuery(
                "select r.id,r.route_name from route_masters r left join sublocations s on r.destination_sublocation=s.id where s.location_id=?",new String[]{mobile_destination_id}
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

    public List<SpinnerItem> get_new_routes(String mobile_location_id,String old_route_id) {
        List<SpinnerItem> list = new ArrayList<>();
        SQLiteDatabase db = this.getReadableDatabase();

        Cursor c = db.rawQuery(
                "SELECT r.id,r.route_name FROM route_masters r JOIN route_masters old ON old.id = ? LEFT JOIN sublocations s on r.destination_sublocation=s.id WHERE r.source_sublocation = old.source_sublocation AND s.location_id = ?;",new String[]{old_route_id,mobile_location_id}
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

    public List<SpinnerItem> getOreNames() {
        List<SpinnerItem> list = new ArrayList<>();
        Log.e("","SELECT DISTINCT name FROM ore_masters");
        Cursor c = getReadableDatabase().rawQuery(
                "SELECT id,name FROM ore_masters GROUP BY name", null);

        while (c.moveToNext()) {
            list.add(new SpinnerItem(c.getInt(0),c.getString(1)));
            Log.e(""," "+c.getString(1));
        }
        c.close();
        return list;
    }

    public List<SpinnerItem> getTypes(String oreName) {
        List<SpinnerItem> list = new ArrayList<>();
        Log.e("getTypes","SELECT id,type FROM ore_masters WHERE name="+oreName+" GROUP BY type");
        Cursor c = getReadableDatabase().rawQuery(
                "SELECT id,type FROM ore_masters WHERE name=? GROUP BY type",
                new String[]{oreName});

        while (c.moveToNext()) list.add(new SpinnerItem(c.getInt(0),c.getString(1)));
        c.close();
        return list;
    }

    public List<SpinnerItem> getSizes(String oreName, String type) {
        List<SpinnerItem> list = new ArrayList<>();
        Log.e("getSizes","SELECT id, size FROM ore_masters WHERE name="+oreName+" AND type="+type+" GROUP BY size");
        Cursor c = getReadableDatabase().rawQuery(
                "SELECT id, size FROM ore_masters WHERE name=? AND type=? GROUP BY size ",
                new String[]{oreName, type});

        while (c.moveToNext()) list.add(new SpinnerItem(c.getInt(0),c.getString(1)));
        c.close();
        return list;
    }

    public List<SpinnerItem> getGrades(String oreName, String type, String size) {
        List<SpinnerItem> list = new ArrayList<>();

        Log.e("getGrades","SELECT id, grade FROM ore_masters WHERE name="+oreName+" AND type="+type+"  AND size="+size+" GROUP BY grade");
        Cursor c = getReadableDatabase().rawQuery(
                "SELECT id, grade FROM ore_masters WHERE name=? AND type=? AND size=? GROUP BY grade",
                new String[]{oreName, type, size});

        while (c.moveToNext()) list.add(new SpinnerItem(c.getInt(0),c.getString(1)));
        c.close();
        return list;
    }

    public List<SpinnerItem> getDescriptions(String oreName, String type, String size, String grade) {
        List<SpinnerItem> list = new ArrayList<>();
        Log.e("getDescriptions","SELECT id, description FROM ore_masters WHERE name="+oreName+" AND type="+type+"  AND size="+size+" AND size="+grade+" GROUP BY grade");
        Cursor c = getReadableDatabase().rawQuery(
                "SELECT id, description FROM ore_masters WHERE name=? AND type=? AND size=? AND grade=?  GROUP BY description",
                new String[]{oreName, type, size, grade});

        while (c.moveToNext()) list.add(new SpinnerItem(c.getInt(0),c.getString(1)));
        c.close();
        return list;
    }

    public Cursor getSetupData() {
        SQLiteDatabase db = this.getReadableDatabase();
        return db.rawQuery(
                "SELECT * FROM setup_data ORDER BY id DESC LIMIT 1",
                null
        );
    }
    public Cursor getSetupnames() {
        SQLiteDatabase db = this.getReadableDatabase();
        return db.rawQuery(
                "SELECT l.sublocation_name,v.company_name,o.description,r.route_name,lo.location_name,sl.sublocation_name as dest_subloc,m.registration_no, m.date_time as machinery_st_tm,mw.registration_no as screeningplant,om.description as inputore,sr1.sublocation_name as old_route,r2.route_name as new_route1,r3.route_name as new_route2,r4.route_name as new_route3,r5.route_name as new_route4 FROM setup_data s left join sublocations l on s.sublocation_id=l.id left join vendor_masters v on s.vendor_id=v.id left join ore_masters o on s.description_id=o.id left join route_masters r on s.route_id=r.id left join sublocations sl on s.dest_subloc_id=sl.id  left join locations lo on s.destination_id=lo.id LEFT JOIN machinery m ON m.id = (SELECT MAX(id) FROM machinery) LEFT JOIN screening_plant sp ON sp.id = (SELECT MAX(id) FROM screening_plant) LEFT JOIN machineWorkingdetails mw on sp.machine_working_id=mw.id LEFT JOIN ore_masters om on sp.ore_id=om.id left join route_masters r1 on s.old_route_id=r1.id left join sublocations sr1 on r1.destination_sublocation=sr1.id left join route_masters r2 on s.new_route1_id=r2.id  left join route_masters r3 on s.new_route2_id=r3.id left join route_masters r4 on s.new_route3_id=r4.id left join route_masters r5 on s.new_route4_id=r5.id ORDER BY s.id DESC LIMIT 1",
                null
        );
    }
    public void saveUidAndLocationToPrefs(Context context) {


        SQLiteDatabase db = this.getReadableDatabase();

        Cursor cursor = db.rawQuery(
                "SELECT ml.id,ml.u_id,ml.login, ml.location_id,ml.int_tare_validity_days,ml.ext_tare_validity_days,m.machine_id,m.date_time,sp.machine_working_id,ore_id FROM mobile_logins ml LEFT JOIN machinery m ON m.id = (SELECT MAX(id) FROM machinery)  LEFT JOIN screening_plant sp ON sp.id = (SELECT MAX(id) FROM screening_plant) WHERE ml.status=1 AND ml.u_id IS NOT NULL",
                null
        );

        if (cursor != null && cursor.moveToFirst()) {

            String login = cursor.getString(cursor.getColumnIndexOrThrow("login"));
            String tid = cursor.getString(cursor.getColumnIndexOrThrow("id"));
            String uid = cursor.getString(cursor.getColumnIndexOrThrow("u_id"));
            Long locationId = cursor.getLong(cursor.getColumnIndexOrThrow("location_id"));
            String machine_id = cursor.getString(cursor.getColumnIndexOrThrow("machine_id"));
            String date_time = cursor.getString(cursor.getColumnIndexOrThrow("date_time"));
            int int_tare_validity_days = cursor.getInt(cursor.getColumnIndexOrThrow("int_tare_validity_days"));
            int ext_tare_validity_days = cursor.getInt(cursor.getColumnIndexOrThrow("ext_tare_validity_days"));
            int machine_working_id = cursor.getInt(cursor.getColumnIndexOrThrow("machine_working_id"));
            int ore_id = cursor.getInt(cursor.getColumnIndexOrThrow("ore_id"));

            // Save to SharedPreferences
            SharedPreferences prefs = context.getSharedPreferences(
                    "APP_PREFS",
                    Context.MODE_PRIVATE
            );

            SharedPreferences.Editor editor = prefs.edit();
            editor.putString("login", login);
            editor.putString("TID", tid);
            editor.putString("UID", uid);
            editor.putLong("LOCATION_ID", locationId);
            editor.putString("machine_id", machine_id);
            editor.putString("date_time", date_time);
            editor.putInt("int_tare_validity_days", int_tare_validity_days);
            editor.putInt("ext_tare_validity_days", ext_tare_validity_days);
            editor.putInt("screening_plant_id", machine_working_id);
            editor.putInt("plant_input_product_id", ore_id);
            editor.apply();
        }

        if (cursor != null) cursor.close();
        db.close();
    }

    public String getAssetId_fromdb(String regNo) {

        SQLiteDatabase db = this.getReadableDatabase();
        String assetId = "";

        Cursor cursor = db.rawQuery(
                "SELECT id FROM asset_masters WHERE registration_no = ?",
                new String[]{regNo}
        );

        if (cursor.moveToFirst()) {
            assetId = cursor.getString(0);
        }

        cursor.close();
        return assetId;
    }

    public String get_asset_number(String id) {

        SQLiteDatabase db = this.getReadableDatabase();
        String assetnum = "";

        Cursor cursor = db.rawQuery(
                "SELECT registration_no FROM asset_masters WHERE id = ?",
                new String[]{id}
        );

        if (cursor.moveToFirst()) {
            assetnum = cursor.getString(0);
        }

        cursor.close();
        return assetnum;
    }

    public String get_subloc_from_routeid(int id,String locname) {

        SQLiteDatabase db = this.getReadableDatabase();
        String subloc = "0";

        Cursor cursor = db.rawQuery(
                "SELECT "+locname+" FROM route_masters WHERE id = ?",
                new String[]{String.valueOf(id)}
        );

        if (cursor.moveToFirst()) {
            subloc = cursor.getString(0);
        }
        Log.e("subloc",subloc+" ");
        cursor.close();
        return subloc;
    }

    public String get_route_name(int id) {

        SQLiteDatabase db = this.getReadableDatabase();
        String name = "0";

        Cursor cursor = db.rawQuery(
                "SELECT route_name FROM route_masters WHERE id = ?",
                new String[]{String.valueOf(id)}
        );

        if (cursor.moveToFirst()) {
            name = cursor.getString(0);
        }
        Log.e("route_name",name+" ");
        cursor.close();
        return name;
    }

    public String get_ore_name(int id) {

        SQLiteDatabase db = this.getReadableDatabase();
        String name = "0";

        Cursor cursor = db.rawQuery(
                "SELECT name,type,size,grade FROM ore_masters WHERE id = ?",
                new String[]{String.valueOf(id)}
        );

        if (cursor.moveToFirst()) {
            name = cursor.getString(0)+"-"+cursor.getString(1)+"-"+cursor.getString(2)+"-"+cursor.getString(3);

        }
        Log.e("route_name",name+" ");
        cursor.close();
        return name;
    }
    public String get_vendor_name(int id) {

        SQLiteDatabase db = this.getReadableDatabase();
        String name = "0";

        Cursor cursor = db.rawQuery(
                "SELECT company_name FROM vendor_masters WHERE id = ?",
                new String[]{String.valueOf(id)}
        );

        if (cursor.moveToFirst()) {
            name = cursor.getString(0);

        }
        Log.e("route_name",name+" ");
        cursor.close();
        return name;
    }

    public String get_mob_name(int id) {

        SQLiteDatabase db = this.getReadableDatabase();
        String name = "0";

        Cursor cursor = db.rawQuery(
                "SELECT login FROM mobile_logins WHERE id = ?",
                new String[]{String.valueOf(id)}
        );

        if (cursor.moveToFirst()) {
            name = cursor.getString(0);

        }
        Log.e("route_name",name+" ");
        cursor.close();
        return name;
    }

    public String get_subloc_name(int id) {

        SQLiteDatabase db = this.getReadableDatabase();
        String dest_subloc = "";

        Cursor cursor = db.rawQuery(
                "SELECT sublocation_name FROM sublocations WHERE id = ?",
                new String[]{String.valueOf(id)}
        );

        if (cursor.moveToFirst()) {
            dest_subloc = cursor.getString(0);
        }
        Log.e("dest_subloc",dest_subloc+" ");
        cursor.close();
        return dest_subloc;
    }

    public void markTripsheetsUploaded(List<Long> ids,Context context) {
        dbclass dbs = new dbclass(context);
        SQLiteDatabase db = dbs.getWritableDatabase();

        ContentValues cv = new ContentValues();
        cv.put("POS_UP_BIT", 0);

        for (Long id : ids) {
            db.update(
                    "tripsheets",
                    cv,
                    "id = ?",
                    new String[]{String.valueOf(id)}
            );
        }
    }

    public boolean uploadTripsheets(JSONArray tripsheetsJson,String urlstr,String type) {
        trustEveryone();
        HttpURLConnection conn = null;
        Log.e("uploadTripsheets","uploadTripsheets");
        try {
            URL url = new URL(urlstr);
            conn = (HttpURLConnection) url.openConnection();

            conn.setRequestMethod("POST");
            conn.setConnectTimeout(15000);
            conn.setReadTimeout(15000);
            conn.setDoInput(true);
            conn.setDoOutput(true);

            conn.setRequestProperty("Authorization", "Bearer Mzt7vkcoPnxkqZq6vFW6fxP3e61b66nSlYkWNETTUHiN7VL5P8GJSIvzcioq");
            conn.setRequestProperty("Accept", "application/json");
            conn.setRequestProperty("Content-Type", "application/json");

            JSONObject payload = new JSONObject();
            payload.put(type, tripsheetsJson);

            Log.e("uploadTripsheets","uploadTripsheets"+ tripsheetsJson);

            OutputStream os = conn.getOutputStream();
            os.write(payload.toString().getBytes("UTF-8"));
            os.flush();
            os.close();

            int responseCode = conn.getResponseCode();
            Log.e("uploadTripsheets","responseCode"+ responseCode);
            if (responseCode == HttpURLConnection.HTTP_OK ||
                    responseCode == HttpURLConnection.HTTP_CREATED) {

                BufferedReader br = new BufferedReader(
                        new InputStreamReader(conn.getInputStream())
                );

                StringBuilder response = new StringBuilder();
                String line;
                while ((line = br.readLine()) != null) {
                    response.append(line);
                }
                br.close();

                Log.e("API_RESPONSE", response.toString());
                return true;
            }

        } catch (Exception e) {
            Log.e("UPLOAD_ERROR", e.getMessage(), e);
        } finally {
            if (conn != null) conn.disconnect();
        }

        return false;
    }

    public void trustEveryone() {
        try {
            HttpsURLConnection.setDefaultHostnameVerifier(new HostnameVerifier(){
                public boolean verify(String hostname, SSLSession session) {
                    return true;
                }});
            SSLContext context = SSLContext.getInstance("TLS");
            context.init(null, new X509TrustManager[]{new X509TrustManager(){
                public void checkClientTrusted(X509Certificate[] chain,
                                               String authType) {}
                public void checkServerTrusted(X509Certificate[] chain,
                                               String authType){}
                public X509Certificate[] getAcceptedIssuers() {
                    return new X509Certificate[0];
                }}}, new SecureRandom());
            HttpsURLConnection.setDefaultSSLSocketFactory(
                    context.getSocketFactory());
        } catch (Exception e) { // should never happen
            e.printStackTrace();
        }
    }

    public JSONArray getTripsheetsForUpload(Context context) {

        JSONArray jsonArray = new JSONArray();
        dbclass dbs = new dbclass(context);
        SQLiteDatabase db = dbs.getWritableDatabase();

        Cursor cursor = db.rawQuery(
                "SELECT * FROM tripsheets WHERE POS_UP_BIT = 1",
                null
        );

        if (cursor.moveToFirst()) {
            do {
                JSONObject obj = new JSONObject();

                try {
                    obj.put("id", cursor.getLong(cursor.getColumnIndexOrThrow("id")));
                    obj.put("trip_type", cursor.getString(cursor.getColumnIndexOrThrow("trip_type")));
                    obj.put("tripsheet_no", cursor.getString(cursor.getColumnIndexOrThrow("tripsheet_no")));

                    obj.put("initial_route_id",
                            base64Encode(cursor.getString(cursor.getColumnIndexOrThrow("initial_route_id"))));
                    obj.put("final_route_id",
                            base64Encode(cursor.getString(cursor.getColumnIndexOrThrow("final_route_id"))));

                    obj.put("rfid_id", cursor.getString(cursor.getColumnIndexOrThrow("rfid_id")));

                    obj.put("truck_id",
                            base64Encode(cursor.getString(cursor.getColumnIndexOrThrow("truck_id"))));
                    obj.put("truck_no", cursor.getString(cursor.getColumnIndexOrThrow("truck_no")));

                    obj.put("vendor_id",
                            base64Encode(cursor.getString(cursor.getColumnIndexOrThrow("vendor_id"))));

                    obj.put("src_time", cursor.getString(cursor.getColumnIndexOrThrow("src_time")));
                    obj.put("src_mobile",
                            base64Encode(cursor.getString(cursor.getColumnIndexOrThrow("src_mobile"))));

                    obj.put("wb_src_time", cursor.getString(cursor.getColumnIndexOrThrow("wb_src_time")));
                    obj.put("wb_src_gross_login",
                            base64Encode(cursor.getString(cursor.getColumnIndexOrThrow("wb_src_gross_login"))));

                    obj.put("src_tare_wt_time", cursor.getString(cursor.getColumnIndexOrThrow("src_tare_wt_time")));
                    obj.put("wb_src_tare_login",
                            base64Encode(cursor.getString(cursor.getColumnIndexOrThrow("wb_src_tare_login"))));

                    obj.put("wb_dest_time", cursor.getString(cursor.getColumnIndexOrThrow("wb_dest_time")));
                    obj.put("wb_dest_gross_login",
                            base64Encode(cursor.getString(cursor.getColumnIndexOrThrow("wb_dest_gross_login"))));

                    obj.put("dest_time", cursor.getString(cursor.getColumnIndexOrThrow("dest_time")));
                    obj.put("dest_mobile",
                            base64Encode(cursor.getString(cursor.getColumnIndexOrThrow("dest_mobile"))));

                    obj.put("dest_tare_wt_time", cursor.getString(cursor.getColumnIndexOrThrow("dest_tare_wt_time")));
                    obj.put("wb_dest_tare_login",
                            base64Encode(cursor.getString(cursor.getColumnIndexOrThrow("wb_dest_tare_login"))));

                    obj.put("src_gross_wt", cursor.getString(cursor.getColumnIndexOrThrow("src_gross_wt")));
                    obj.put("src_tare_wt", cursor.getString(cursor.getColumnIndexOrThrow("src_tare_wt")));
                    obj.put("src_net_wt", cursor.getString(cursor.getColumnIndexOrThrow("src_net_wt")));

                    obj.put("dest_gross_wt", cursor.getString(cursor.getColumnIndexOrThrow("dest_gross_wt")));
                    obj.put("dest_tare_wt", cursor.getString(cursor.getColumnIndexOrThrow("dest_tare_wt")));
                    obj.put("dest_net_wt", cursor.getString(cursor.getColumnIndexOrThrow("dest_net_wt")));

                    obj.put("approved_by",
                            base64Encode(cursor.getString(cursor.getColumnIndexOrThrow("approved_by"))));
                    obj.put("approved_on", cursor.getString(cursor.getColumnIndexOrThrow("approved_on")));

                    obj.put("ore_id",
                            base64Encode(cursor.getString(cursor.getColumnIndexOrThrow("ore_id"))));

                    obj.put("status", cursor.getString(cursor.getColumnIndexOrThrow("status")));

                    obj.put("created_at", cursor.getString(cursor.getColumnIndexOrThrow("created_at")));

                    obj.put("screening_plant_id",
                            base64Encode(cursor.getString(cursor.getColumnIndexOrThrow("screening_plant_id"))));

                    obj.put("plant_input_product_id",
                            base64Encode(cursor.getString(cursor.getColumnIndexOrThrow("plant_input_product_id"))));

                    obj.put("machine_id",
                            base64Encode(cursor.getString(cursor.getColumnIndexOrThrow("machine_id"))));

                    jsonArray.put(obj);

                } catch (JSONException e) {
                    e.printStackTrace();
                }

            } while (cursor.moveToNext());
        }

        cursor.close();
        return jsonArray;
    }

    public JSONArray getHsdTransactions(Context context) {

        JSONArray jsonArray = new JSONArray();
        dbclass dbs = new dbclass(context);
        SQLiteDatabase db = dbs.getWritableDatabase();

        Cursor cursor = db.rawQuery(
                "SELECT h.id,h.asset_id,b.sublocation_id,b.bowser_id,h.qty,h.datetime FROM hsd_transactions h left join bowser_stock b on h.bowser_stock_id=b.id WHERE h.POS_UP_BIT = 1",
                null
        );

        if (cursor.moveToFirst()) {
            do {
                JSONObject obj = new JSONObject();

                try {
                    obj.put("id", base64Encode(cursor.getString(cursor.getColumnIndexOrThrow("id"))));
                    obj.put("asset_id", base64Encode(cursor.getString(cursor.getColumnIndexOrThrow("asset_id"))));
                    obj.put("item_id", base64Encode("1"));
                    obj.put("sublocation_id", base64Encode(cursor.getString(cursor.getColumnIndexOrThrow("sublocation_id"))));
                    obj.put("bowser_id", base64Encode(cursor.getString(cursor.getColumnIndexOrThrow("bowser_id"))));
                    obj.put("qty", cursor.getString(cursor.getColumnIndexOrThrow("qty")));
                    obj.put("issue_date", cursor.getString(cursor.getColumnIndexOrThrow("datetime")));
                    obj.put("remark","");

                    jsonArray.put(obj);

                } catch (JSONException e) {
                    e.printStackTrace();
                }

            } while (cursor.moveToNext());
        }

        cursor.close();
        return jsonArray;
    }

    public static String base64Encode(String id) {
        if(id!=null)
        {
            return Base64.encodeToString(
                    id.getBytes(StandardCharsets.UTF_8),
                    Base64.NO_WRAP
            );
        }else
        {
            return "";
        }

    }

    public static String base64Decode(String base64) {
        if (base64 != null && !base64.isEmpty()) {
            byte[] decoded = Base64.decode(base64, Base64.NO_WRAP);
            return new String(decoded, StandardCharsets.UTF_8);
        } else {
            return "";
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

    public String db_format_date_time(Date date)
    {
        return new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault()).format(date);
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

    public static String secondsFromTargetDate(String dbDateTime) throws ParseException {

        if(!"null".equals(dbDateTime) && dbDateTime != null)
        {
            SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault());
            sdf.setTimeZone(TimeZone.getTimeZone("Asia/Kolkata"));

            Date targetDate = sdf.parse(dbDateTime);

            Calendar baseCal = Calendar.getInstance(TimeZone.getTimeZone("Asia/Kolkata"));
            baseCal.set(2026, Calendar.JANUARY, 1, 0, 0, 0);

            baseCal.set(Calendar.MILLISECOND, 0);

            long baseMillis = baseCal.getTimeInMillis();
            long targetMillis = targetDate.getTime();

            return String.valueOf(TimeUnit.MILLISECONDS.toSeconds(targetMillis - baseMillis));
        }else
        {
            return "0";
        }
    }

    public boolean writeStringToTag(Tag tag, String text, int page, int strtype) {

        Log.e("writeStringToTag",text+" ");
        NfcA nfcA = NfcA.get(tag);

        try {
            nfcA.connect();
            byte[] bytes;
            byte[] pageData;
            bytes = text.getBytes(StandardCharsets.UTF_8);

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
            return true;
        } catch (Exception e) {
            Log.e("NFC", "Error writing tag: " + e.getMessage());
            return false;
        }
    }

    public boolean writeOrFail(Context context,Tag tag, int page, byte[] data) {
        boolean ok = writeWithRetryAndVerify(context,tag, page, data, 3);
        if (!ok) {
            new Handler(Looper.getMainLooper()).post(() ->
                    Toast.makeText(context.getApplicationContext(),
                            "Failed writing page " + page,
                            Toast.LENGTH_LONG).show()
            );
        }
        return ok;
    }

    public boolean writeWithRetryAndVerify(Context context,Tag tag, int page, byte[] data, int retries) {
        for (int i = 0; i < retries; i++) {
            boolean written = writePage(context,tag, page, data);
            if (!written) continue;

            return true; // success

        }
        return false;
    }

    private boolean writePage(Context context,Tag tag, int page, byte[] data)
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
                showScrollableErrorDialog(context,"Error", "Page "+page+" - Write Failed "+ Arrays.toString(data));
                Log.e("NFC", "Wrote Page result" + "Page "+page+" - Write Failed");
                return false;
            }
        } catch (Exception e) {
            Log.e("NFC", "Error writing tag: Page "+page+" - "+ e.getMessage());
            showScrollableErrorDialog(context,"Error", "Page "+page+" - "+Arrays.toString(data)+" "+e.getMessage());
            return false;
        }
    }


    public void addRow(Context context, LinearLayout parent, String label, String value) {

        LayoutInflater inflater = LayoutInflater.from(context);
        View row = inflater.inflate(R.layout.row_setup_detail, parent, false);

        TextView tvLabel = row.findViewById(R.id.tvLabel);
        TextView tvValue = row.findViewById(R.id.tvValue);

        // Default styling
        tvLabel.setTypeface(null, Typeface.BOLD);
        tvLabel.setTextColor(Color.BLACK);
        tvLabel.setTextSize(16);

        tvValue.setTextColor(Color.BLACK);
        tvValue.setTextSize(16);

        tvLabel.setText(label);
        tvValue.setText(value == null ? "--" : value);

        // ✅ Special Highlight for Sublocation
        if (label != null && label.equalsIgnoreCase("Sublocation")) {
            tvLabel.setVisibility(View.GONE);
            tvLabel.setText("");
            tvLabel.setTextSize(20);
            tvValue.setTextSize(20);

            tvLabel.setTextColor(Color.parseColor("#1565C0")); // blue
            tvValue.setTextColor(Color.parseColor("#1565C0"));

            tvLabel.setTypeface(null, Typeface.BOLD_ITALIC);
            tvValue.setTypeface(null, Typeface.BOLD);

            // optional background highlight
            row.setBackgroundColor(Color.parseColor("#E3F2FD")); // light blue bg

            row.setPadding(12, 12, 12, 12);
        }

        parent.addView(row);
    }

    public static Date dateFromSeconds(long seconds) {

        Calendar baseCal = Calendar.getInstance(TimeZone.getTimeZone("Asia/Kolkata"));
        baseCal.set(2026, Calendar.JANUARY, 1, 0, 0, 0);
        baseCal.set(Calendar.MILLISECOND, 0);

        long baseMillis = baseCal.getTimeInMillis();
        long targetMillis = baseMillis + (seconds * 1000);

        return new Date(targetMillis);
    }

    public static Date dateFromDays(long days) {

        Calendar baseCal = Calendar.getInstance(TimeZone.getTimeZone("Asia/Kolkata"));
        baseCal.set(2026, Calendar.JANUARY, 1, 0, 0, 0);
        baseCal.set(Calendar.MILLISECOND, 0);

        long baseMillis = baseCal.getTimeInMillis();
        long targetMillis = baseMillis + (days * 24L * 60L * 60L * 1000L); // days → ms

        return new Date(targetMillis);
    }

    public static int daysFromBaseDateInt(String dbDateTime) throws ParseException {

        Log.e("daysFromBaseDateInt",dbDateTime+" ");

        if (dbDateTime == null || dbDateTime.trim().isEmpty()) {
            return 0;
        }

        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault());
        sdf.setTimeZone(TimeZone.getTimeZone("Asia/Kolkata"));

        Date targetDate = sdf.parse(dbDateTime);

        Calendar baseCal = Calendar.getInstance(TimeZone.getTimeZone("Asia/Kolkata"));
        baseCal.set(2026, Calendar.JANUARY, 1, 0, 0, 0);
        baseCal.set(Calendar.MILLISECOND, 0);

        long baseMillis = baseCal.getTimeInMillis();
        long targetMillis = targetDate.getTime();

        long diffMillis = targetMillis - baseMillis;

        return (int) TimeUnit.MILLISECONDS.toDays(diffMillis);
    }

    public static Date date_time_FromSeconds(long seconds) {

        Calendar baseCal = Calendar.getInstance(TimeZone.getTimeZone("Asia/Kolkata"));
        baseCal.set(2026, Calendar.JANUARY, 1, 0, 0, 0);
        baseCal.set(Calendar.MILLISECOND, 0);

        long baseMillis = baseCal.getTimeInMillis();
        long targetMillis = baseMillis + (seconds * 1000);

        return new Date(targetMillis);
    }

    public static int bytesToInt(byte[] data, int start, int length) {

        int value = 0;

        // Build unsigned value
        for (int i = start; i < start + length; i++) {
            value = (value << 8) | (data[i] & 0xFF);
        }

        // 🔥 Sign extension (for signed values)
        int signBit = 1 << (length * 8 - 1);

        if ((value & signBit) != 0) {
            value -= (1 << (length * 8));
        }

        Log.e("bytesToInt", " " + value);
        return value;
    }

    public static String bytesToString(byte[] data, int start, int length) {

        long value = 0;
        for (int i = start; i < start + length; i++) {
            value = (value << 8) | (data[i] & 0xFF);
        }
        Log.e("bytesToString"," "+value);
        return String.valueOf(value);
    }
    public static byte[] stringTo1Bytes(String value) {
        if (value == null || value.isEmpty() || "null".equals(value)) {
            value = "0";
        }
        long seconds = Long.parseLong(value); // decimal conversion

        byte[] data = new byte[1];
        data[0] = (byte) (seconds & 0xFF);

        return data;
    }
    public static byte[] stringTo2Bytes(String value) {
        if (value == null || value.isEmpty() || "null".equals(value)) {
            value = "0";
        }
        long seconds = Long.parseLong(value); // decimal conversion

        byte[] data = new byte[2];
        data[0] = (byte) ((seconds >> 8) & 0xFF);
        data[1] = (byte) (seconds & 0xFF);

        return data;
    }

    public static byte[] stringTo3Bytes(String value) {
        if (value == null || value.isEmpty() || "null".equals(value)) {
            value = "0";
        }
        long seconds = Long.parseLong(value); // decimal conversion

        byte[] data = new byte[3];
        data[0] = (byte) ((seconds >> 16) & 0xFF);
        data[1] = (byte) ((seconds >> 8) & 0xFF);
        data[2] = (byte) (seconds & 0xFF);

        return data;
    }
    public static byte[] stringTo4Bytes(String value) {
        if (value == null || value.isEmpty() || "null".equals(value)) {
            value = "0";
        }
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

    public String bytesToHex(byte[] bytes)
    {
        StringBuilder sb = new StringBuilder();
        for (byte b : bytes) {
            sb.append(String.format("%02X", b));
        }
        return sb.toString();
    }





    public void clearUltralight(Tag tag) {
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

    public boolean isMobileLoginEmpty(Context context) {
        dbclass dbs = new dbclass(context);
        SQLiteDatabase db = dbs.getReadableDatabase();
        Cursor c = db.rawQuery("SELECT COUNT(*) FROM mobile_logins WHERE STATUS=1 AND u_id IS NOT NULL", null);
        boolean empty = true;
        if (c.moveToFirst()) {
            empty = c.getInt(0) == 0;
        }
        c.close();
        return empty;
    }

    public View createRow(Context context,String title, String value, String error) {

        LinearLayout row = new LinearLayout(context);
        row.setOrientation(LinearLayout.HORIZONTAL);
        row.setPadding(0, 8, 0, 8);

        TextView tvTitle = new TextView(context);
        tvTitle.setLayoutParams(new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1));
        tvTitle.setText(formatTitle(title));
        tvTitle.setTypeface(null, Typeface.BOLD);
        tvTitle.setTextColor(Color.BLACK);
        tvTitle.setTextSize(16);

        TextView tvValue = new TextView(context);
        tvValue.setLayoutParams(new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1));
        tvValue.setText(value);
        tvValue.setTextColor(Color.BLACK);
        tvValue.setTextSize(16);

        row.addView(tvTitle);
        row.addView(tvValue);

        return row;
    }

    private String formatTitle(String key) {
        return key.replace("_", " ").toUpperCase(Locale.US);
    }

    public String checktime(Date date)
    {
        String error="";
        long currentTimeMillis = System.currentTimeMillis();

        long diffMillis = currentTimeMillis - date.getTime();

        long minWaitMillis = 5 * 60 * 1000; // 5 minutes
        Log.e("checktime",diffMillis+" "+minWaitMillis);
        if (diffMillis < minWaitMillis)
        {

            long remainingMillis = minWaitMillis - diffMillis;

            long remainingSeconds = remainingMillis / 1000;
            long remainingMinutes = remainingSeconds / 60;
            long remainingSecs = remainingSeconds % 60;

            String waitMsg;
            if (remainingMinutes > 0) {
                waitMsg = remainingMinutes + " MIN " + remainingSecs + " SEC";
            } else {
                waitMsg = remainingSecs + " SEC";
            }

            error += "LAST TRIP ISSUED AT "+display_format_date_time(date) +".\nPLEASE WAIT FOR " + waitMsg + " MORE BEFORE ISSUING NEW TRIP.\n";
        }

        return error;
    }

    public String db_format_date(Date date)
    {
        return new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(date);
    }

    public String display_format_date_time(Date date)
    {
        if(date!=null)
        {
            return new SimpleDateFormat("dd-MM-yyyy HH:mm:ss", Locale.getDefault()).format(date);
        }else
        {
            return "";
        }
    }

    public String display_format_date(Date date)
    {
        if(date!=null)
        {
            return new SimpleDateFormat("dd-MM-yyyy", Locale.getDefault()).format(date);
        }else
        {
            return "";
        }

    }
    public String display_format_date_time(String dbDate) {
        if (dbDate != null && !dbDate.isEmpty()) {
            try {
                SimpleDateFormat dbFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault());
                Date date = dbFormat.parse(dbDate);

                SimpleDateFormat displayFormat = new SimpleDateFormat("dd-MM-yyyy HH:mm:ss", Locale.getDefault());
                return displayFormat.format(date);

            } catch (Exception e) {
                e.printStackTrace();
                return dbDate; // fallback: return original if error
            }
        } else {
            return "";
        }
    }

    public String getBowserStock(long bowserId) {
        String qty = "0.00";
        SQLiteDatabase db = this.getReadableDatabase();
        Cursor cursor = db.rawQuery(
                "SELECT qty FROM bowser_stock WHERE id='"+bowserId+"'",null
        );

        if (cursor != null && cursor.moveToFirst()) {
            double value = cursor.getDouble(0); // qty column
            qty = String.format("%.2f", value);
            cursor.close();
        }

        return qty;
    }

    public List<SpinnerItem> get_bowser_stock() {
        List<SpinnerItem> list = new ArrayList<>();
        SQLiteDatabase db = this.getReadableDatabase();

        Cursor c = db.rawQuery(
                "select b.id,s.sublocation_name,b.qty from bowser_stock b left join sublocations s on b.sublocation_id=s.id order by b.date_time desc",null
        );

        while (c.moveToNext()) {
            list.add(new SpinnerItem(
                    c.getInt(0),        // id
                    c.getString(1)+" ("+ c.getString(2)+")"     // route name
            ));
        }
        c.close();
        return list;
    }

    void onRfidTapped(CardView cardDetails, LinkedHashMap<String, Object> card_details_map, String error, Handler handler, Runnable hideRunnable, LinearLayout detailsContainer, Context context) {

        handler.removeCallbacks(hideRunnable);

        detailsContainer.removeAllViews();

        for (Map.Entry<String, Object> entry : card_details_map.entrySet())
        {

            String key = entry.getKey();
            Object valObj = entry.getValue();
            String value = valObj == null ? "--" : String.valueOf(valObj);

            View row = createRow(context,key, value,error);
            detailsContainer.addView(row);
        }

        /*if (!error.trim().isEmpty())
        {

            TextView errorView = new TextView(this);
            errorView.setText(error);
            errorView.setTextColor(Color.WHITE);
            errorView.setTextSize(18);
            errorView.setPadding(16, 16, 16, 16);

            detailsContainer.addView(errorView);

            cardDetails.setCardBackgroundColor(Color.parseColor("#D32F2F")); // red
        }else {
            cardDetails.setCardBackgroundColor(Color.WHITE); // or your normal color
        }*/
        cardDetails.startAnimation(
                AnimationUtils.loadAnimation(context, android.R.anim.slide_in_left)
        );
        cardDetails.setVisibility(View.VISIBLE);
        handler.postDelayed(hideRunnable, 5 * 60 * 1000);
    }

    double route_consumption(int route_id_card)
    {
        SQLiteDatabase db = this.getReadableDatabase();

        double fuel_cons = 0.0;
        Cursor c = db.rawQuery(
                "select fuel_consumption from route_masters where id=?",new String[]{String.valueOf(route_id_card)}
        );

        if (c.moveToFirst()) {
            fuel_cons = c.getDouble(0);
        }
        c.close();
        return fuel_cons;
    }

    public String getTripStatusText(int trip_status_read) {

        switch (trip_status_read) {
            case 1:
                return "Open";
            case 2:
                return "Closed";
            default:
                return "Unknown (" + trip_status_read + ")";
        }
    }
}

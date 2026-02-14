package com.example.rfid;

import android.content.ContentValues;
import android.content.Context;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.database.DatabaseErrorHandler;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.util.Base64;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.security.SecureRandom;
import java.security.cert.X509Certificate;
import java.util.ArrayList;
import java.util.List;

import javax.net.ssl.HostnameVerifier;
import javax.net.ssl.HttpsURLConnection;
import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLSession;
import javax.net.ssl.X509TrustManager;

public class dbclass extends SQLiteOpenHelper {
    SQLiteDatabase db = getWritableDatabase();

    public dbclass(@Nullable Context context) {
        super(context, "vmsb.db", null, 2);
    }

    @Override
    public void onCreate(SQLiteDatabase db)
    {
        db.execSQL("CREATE TABLE IF NOT EXISTS tripsheets (id INTEGER PRIMARY KEY AUTOINCREMENT,trip_type  NOT NULL, tripsheet_no TEXT NOT NULL UNIQUE, initial_route_id INTEGER NOT NULL, final_route_id INTEGER NOT NULL, rfid_id TEXT, truck_id INTEGER NOT NULL, truck_no TEXT, vendor_id INTEGER, src_time TEXT, src_mobile TEXT, wb_src_time TEXT, wb_src_gross_login INTEGER, src_tare_wt_time TEXT, wb_src_tare_login INTEGER, wb_dest_time TEXT, wb_dest_gross_login INTEGER, dest_time TEXT, dest_mobile TEXT, dest_tare_wt_time TEXT, wb_dest_tare_login INTEGER, src_gross_wt INTEGER, src_tare_wt INTEGER, src_net_wt INTEGER, dest_gross_wt INTEGER, dest_tare_wt INTEGER, dest_net_wt INTEGER, approved_by INTEGER, approved_on TEXT, ore_id INTEGER, status TEXT, created_at TEXT, updated_at TEXT,POS_UP_BIT INTEGER DEFAULT 1);");
        db.execSQL("CREATE TABLE IF NOT EXISTS asset_masters (id INTEGER PRIMARY KEY AUTOINCREMENT, registration_no TEXT NOT NULL, asset_code TEXT, asset_type TEXT NOT NULL, owner_name TEXT NOT NULL, owner_contact TEXT NOT NULL, rc_file TEXT, vendor_id INTEGER NOT NULL, group_code TEXT, tare_weight INTEGER, tare_weight_wb INTEGER, tare_weight_time TEXT, gross_weight_capacity INTEGER NOT NULL, hsd_balance REAL NOT NULL DEFAULT 0.00, retention_amt_perc REAL NOT NULL DEFAULT 0.00, retention_max_amt REAL, created_by INTEGER NOT NULL, updated_by INTEGER NOT NULL, created_at TEXT, updated_at TEXT);");
        db.execSQL("CREATE TABLE IF NOT EXISTS locations (id INTEGER PRIMARY KEY AUTOINCREMENT, location_name TEXT NOT NULL, address TEXT, type TEXT, location TEXT, radius REAL, colour TEXT DEFAULT '#FF0000', fill_colour TEXT DEFAULT '#FB6B72', opacity TEXT DEFAULT '0.5', created_by INTEGER, updated_by INTEGER);");
        db.execSQL("CREATE TABLE IF NOT EXISTS route_masters (id INTEGER PRIMARY KEY AUTOINCREMENT, route_code_id INTEGER NOT NULL, route_name TEXT NOT NULL, source_sublocation INTEGER NOT NULL, destination_sublocation INTEGER NOT NULL, distance INTEGER NOT NULL, trip_time_min INTEGER NOT NULL, load_capacity INTEGER NOT NULL, created_by INTEGER NOT NULL, updated_by INTEGER, created_at TEXT, updated_at TEXT);");
        db.execSQL("CREATE TABLE IF NOT EXISTS sublocations (id INTEGER PRIMARY KEY AUTOINCREMENT, location_id INTEGER NOT NULL, sublocation_name TEXT NOT NULL, point TEXT, created_by INTEGER NOT NULL, updated_by INTEGER, created_at TEXT, updated_at TEXT);");
        db.execSQL("CREATE TABLE IF NOT EXISTS vendor_masters (id INTEGER PRIMARY KEY AUTOINCREMENT, vendor_id TEXT NOT NULL, company_name TEXT NOT NULL, address TEXT, mobile TEXT, email TEXT, bank_ac_no TEXT, bank_ac_type TEXT, bank_IFSC_code TEXT, pan_no TEXT, gst_no TEXT, status INTEGER NOT NULL DEFAULT 0, created_at TEXT, updated_at TEXT);");
        db.execSQL("CREATE TABLE IF NOT EXISTS ore_masters (id INTEGER PRIMARY KEY AUTOINCREMENT, name TEXT NOT NULL, type TEXT NOT NULL, size TEXT NOT NULL, grade TEXT NOT NULL, description TEXT NOT NULL, created_by INTEGER NOT NULL, updated_by INTEGER, created_at DATETIME DEFAULT CURRENT_TIMESTAMP, updated_at DATETIME DEFAULT CURRENT_TIMESTAMP);");
        db.execSQL("CREATE TABLE IF NOT EXISTS setup_data (id INTEGER PRIMARY KEY AUTOINCREMENT, location_id INTEGER NOT NULL, sublocation_id INTEGER NOT NULL, vendor_id INTEGER NOT NULL, ore_id INTEGER NOT NULL, type_id INTEGER NOT NULL, size_id INTEGER NOT NULL, grade_id INTEGER NOT NULL, description_id INTEGER NOT NULL, destination_id INTEGER NOT NULL, route_id INTEGER NOT NULL, created_at TEXT DEFAULT CURRENT_TIMESTAMP, updated_at TEXT,POS_UP_BIT INTEGER DEFAULT 1);");
        db.execSQL("CREATE TABLE IF NOT EXISTS mobile_logins (id INTEGER PRIMARY KEY AUTOINCREMENT, login TEXT NOT NULL UNIQUE, password TEXT NOT NULL, u_id TEXT UNIQUE, location_id INTEGER NOT NULL, status INTEGER NOT NULL DEFAULT 1, version TEXT, created_by INTEGER, updated_by INTEGER, created_at TEXT, updated_at TEXT);");

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
    }

    public SQLiteDatabase getdb()
    {
        return this.db;
    }

    public List<SpinnerItem> getLocations(String loc)
    {
        List<SpinnerItem> list = new ArrayList<>();
        SQLiteDatabase db = this.getReadableDatabase();

        Cursor c = db.rawQuery("SELECT id, location_name FROM locations WHERE id='"+loc+"'", null);
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

    public List<SpinnerItem> getRoutes(int destinationId) {
        List<SpinnerItem> list = new ArrayList<>();
        SQLiteDatabase db = this.getReadableDatabase();

        Cursor c = db.rawQuery(
                "SELECT  id,route_name FROM route_masters WHERE source_sublocation='"+destinationId+"'",null
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

    public void saveUidAndLocationToPrefs(Context context) {


        SQLiteDatabase db = this.getReadableDatabase();

        Cursor cursor = db.rawQuery(
                "SELECT id,u_id, location_id FROM mobile_logins WHERE status=1",
                null
        );

        if (cursor != null && cursor.moveToFirst()) {

            String tid = cursor.getString(cursor.getColumnIndexOrThrow("id"));
            String uid = cursor.getString(cursor.getColumnIndexOrThrow("u_id"));
            String locationId = cursor.getString(cursor.getColumnIndexOrThrow("location_id"));

            // Save to SharedPreferences
            SharedPreferences prefs = context.getSharedPreferences(
                    "APP_PREFS",
                    Context.MODE_PRIVATE
            );

            SharedPreferences.Editor editor = prefs.edit();
            editor.putString("TID", tid);
            editor.putString("UID", uid);
            editor.putString("LOCATION_ID", locationId);
            editor.apply(); // async & recommended
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

    public String get_dest_subloc_from_routeid(int id) {

        SQLiteDatabase db = this.getReadableDatabase();
        String dest_subloc = "0";

        Cursor cursor = db.rawQuery(
                "SELECT destination_sublocation FROM route_masters WHERE id = ?",
                new String[]{String.valueOf(id)}
        );

        if (cursor.moveToFirst()) {
            dest_subloc = cursor.getString(0);
        }
        Log.e("dest_subloc",dest_subloc+" ");
        cursor.close();
        return dest_subloc;
    }

    public String get_dest_subloc_name(int id) {

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

    public boolean uploadTripsheets(JSONArray tripsheetsJson) {
        trustEveryone();
        HttpURLConnection conn = null;
        Log.e("uploadTripsheets","uploadTripsheets");
        try {
            URL url = new URL("http://100.168.10.75:8003/api/tripsheet/save");
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
            payload.put("tripsheets", tripsheetsJson);

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
}

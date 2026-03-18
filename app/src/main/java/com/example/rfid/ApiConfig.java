package com.example.rfid;

public final class ApiConfig {

    private ApiConfig() {} // prevent instantiation

    //public static final String BASE_URL = "http://mssiot.in/vmsb/api/";
    public static final String BASE_URL = "http://100.168.10.75:8003/api/";
    public static final String GET_ASSETS      = BASE_URL + "getassets#asset_masters";
    public static final String GET_LOCATIONS   = BASE_URL + "getlocations#locations";
    public static final String GET_SUBLOCATIONS= BASE_URL + "getsublocations#sublocations";
    public static final String GET_ROUTES      = BASE_URL + "getroutes#route_masters";
    public static final String GET_VENDORS     = BASE_URL + "getvendors#vendor_masters";
    public static final String GET_ORES        = BASE_URL + "getores#ore_masters";
    public static final String MOBILE_LOGIN = BASE_URL + "mobilelogin";
    public static final String TRIPSHEET_SAVE = BASE_URL + "tripsheet/save";
    public static final String SAVEASSETDETAILS = BASE_URL + "saveassetdetails";
}

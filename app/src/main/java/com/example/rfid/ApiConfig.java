package com.example.rfid;

public final class ApiConfig {

    private ApiConfig() {
    } // prevent instantiation

    public static final String BASE_URL = "https://mssiot.in/vmsb/api/";
    //public static final String BASE_URL = "https://100.168.10.75:8003/api/";
    public static final String GET_ASSETS = BASE_URL + "getassets#asset_masters#GET";
    public static final String GET_LOCATIONS = BASE_URL + "getlocations#locations#GET";
    public static final String GET_SUBLOCATIONS = BASE_URL + "getsublocations#sublocations#GET";
    public static final String GET_ROUTES = BASE_URL + "getroutes#route_masters#GET";
    public static final String GET_VENDORS = BASE_URL + "getvendors#vendor_masters#GET";
    public static final String GET_ORES = BASE_URL + "getores#ore_masters#GET";
    public static final String MOBILE_LOGINS = BASE_URL + "mobilelogin#mobile_logins#POST";
    public static final String TRIPSHEET_SAVE = BASE_URL + "tripsheet/save";
    public static final String SAVEASSETDETAILS = BASE_URL + "saveassetdetails";
    public static final String MACHINEWORKINGHOURS = BASE_URL + "machineWorkingdetails#machineWorkingdetails#GET";

    public static final String HSD_TRANSACTION_SAVE = BASE_URL + "itemIssue/save";
}


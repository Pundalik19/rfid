package com.example.rfid;

public class Asset {
    public String assetId;       // API gives Base64 string
    public String registration_no;
    public String ownerName;
    public String tare_weight;
    public String rfid_uid;
    public String assetType;     // TRUCK / MACHINE
    public int vendor_id;
}

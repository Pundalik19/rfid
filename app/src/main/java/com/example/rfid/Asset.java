package com.example.rfid;

public class Asset {
    public String assetId;       // API gives Base64 string
    public String registration_no;
    public String ownerName;
    public String tare_weight;
    public String tare_weight_time;
    public String tare_weight_wb;
    public String rfid_uid;
    public String assetType;     // TRUCK / MACHINE
    public int vendor_id;
    public int gross_weight_capacity;
    public double hsd;

    public String fitness_from;
    public String fitness_to;

    public String puc_from;
    public String puc_to;

    public String insurance_from;
    public String insurance_to;

    public String roadtax_from;
    public String roadtax_to;
}

package com.example.rfid;

public class SpinnerItem {
    public int id;
    public String name;

    public SpinnerItem(int id, String name) {
        this.id = id;
        this.name = name;
    }

    @Override
    public String toString() {
        return name;   // Spinner shows only name
    }
}

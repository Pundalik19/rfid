package com.example.rfid;

import androidx.appcompat.app.AppCompatActivity;

import android.app.PendingIntent;
import android.content.Intent;
import android.nfc.NdefMessage;
import android.nfc.NdefRecord;
import android.nfc.NfcAdapter;
import android.nfc.Tag;
import android.nfc.tech.MifareClassic;
import android.nfc.tech.MifareUltralight;
import android.nfc.tech.Ndef;
import android.nfc.tech.NfcA;
import android.os.Bundle;
import android.os.Parcelable;
import android.provider.Settings;
import android.util.Log;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Spinner;
import android.widget.TextView;

import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.List;

public class MainActivity extends AppCompatActivity {
    TextView txtStatus;
    EditText editMessage;
    Button btnWrite;

    NfcAdapter nfcAdapter;
    boolean writeMode = false;
    PendingIntent pendingIntent;

    Spinner spLocation, spSublocation, spProduct, spVendor, spDestination, spRoute;

    List<SpinnerItem> locList, subList, prodList, vendorList, destList, routeList;
    dbclass db;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);



        txtStatus = findViewById(R.id.txtStatus);
        editMessage = findViewById(R.id.editMessage);
        btnWrite = findViewById(R.id.btnWrite);

        nfcAdapter = NfcAdapter.getDefaultAdapter(this);

        if (nfcAdapter == null) {
            txtStatus.setText("NFC NOT Supported on this device!");
            finish();
            return;
        }
        if (!nfcAdapter.isEnabled()) {
            txtStatus.setText("Enable NFC from Settings");
            startActivity(new Intent(Settings.ACTION_NFC_SETTINGS));
        }

        pendingIntent = PendingIntent.getActivity(
                this, 0,
                new Intent(this, getClass()).addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP),
                PendingIntent.FLAG_MUTABLE);

        btnWrite.setOnClickListener(v -> {
            writeMode = true;
            txtStatus.setText("Tap NFC Tag to Write...");
        });


        db = new dbclass(this);

        spLocation = findViewById(R.id.spLocation);
        spSublocation = findViewById(R.id.spSublocation);
        spProduct = findViewById(R.id.spProduct);
        spVendor = findViewById(R.id.spVendor);
        spDestination = findViewById(R.id.spDestination);
        spRoute = findViewById(R.id.spRoute);

        loadLocations();

        spLocation.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override public void onItemSelected(AdapterView<?> parent, View view, int pos, long id) {
                int locationId = locList.get(pos).id;
                loadSublocations(locationId);
            }
            @Override public void onNothingSelected(AdapterView<?> parent) {}
        });

        spSublocation.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override public void onItemSelected(AdapterView<?> parent, View view, int pos, long id) {
                int subId = subList.get(pos).id;
                loadProducts(subId);
            }
            @Override public void onNothingSelected(AdapterView<?> parent) {}
        });

        spProduct.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override public void onItemSelected(AdapterView<?> parent, View view, int pos, long id) {
                int productId = prodList.get(pos).id;
                loadVendors(productId);
            }
            @Override public void onNothingSelected(AdapterView<?> parent) {}
        });

        spVendor.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override public void onItemSelected(AdapterView<?> parent, View view, int pos, long id) {
                int vendorId = vendorList.get(pos).id;
                loadDestinations(vendorId);
            }
            @Override public void onNothingSelected(AdapterView<?> parent) {}
        });

        spDestination.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override public void onItemSelected(AdapterView<?> parent, View view, int pos, long id) {
                int destId = destList.get(pos).id;
                loadRoutes(destId);
            }
            @Override public void onNothingSelected(AdapterView<?> parent) {}
        });
    }

    @Override
    protected void onResume() {
        super.onResume();
        nfcAdapter.enableForegroundDispatch(this, pendingIntent, null, null);
    }

    @Override
    protected void onPause() {
        super.onPause();
        nfcAdapter.disableForegroundDispatch(this);
    }

    @Override
    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);
        Tag tag = intent.getParcelableExtra(NfcAdapter.EXTRA_TAG);

        String[] techList = tag.getTechList();
        Log.e("techList", Arrays.toString(techList) +" ");
        readMifareClassic(tag);
        //writeToTag(tag, editMessage.getText().toString());
        //readFromNdefTag(intent);
        /*if (Arrays.asList(techList).contains("android.nfc.tech.Ndef")) {

        }
        else if (Arrays.asList(techList).contains("android.nfc.tech.MifareClassic")) {*/
            //readMifareClassic(tag);
        //}
    }

    private void readFromNdefTag(Intent intent) {
        Parcelable[] msgs = intent.getParcelableArrayExtra(NfcAdapter.EXTRA_NDEF_MESSAGES);
        if (msgs != null) {
            NdefMessage msg = (NdefMessage) msgs[0];
            String text = new String(msg.getRecords()[0].getPayload());
            txtStatus.setText("Data: " + text);
        } else {
            txtStatus.setText("No NDEF Data Found!");
        }
    }

    private void writeToNdef(Tag tag, String message) {
        try {
            NdefRecord record = NdefRecord.createTextRecord("en", message);
            NdefMessage ndefMessage = new NdefMessage(new NdefRecord[]{record});

            Ndef ndef = Ndef.get(tag);
            if (ndef != null) {
                ndef.connect();
                ndef.writeNdefMessage(ndefMessage);
                ndef.close();
                txtStatus.setText("Write Successful!");
            } else {
                txtStatus.setText("NDEF Not Supported!");
            }
        } catch (Exception e) {
            txtStatus.setText("Write Error: " + e.getMessage());
        }
    }

    private void readMifareClassic(Tag tag) {

        NfcA nfca = NfcA.get(tag);

        try {
            nfca.connect();

            StringBuilder sb = new StringBuilder();
            sb.append("NfcA Tag Found\n");
            sb.append("SAK: ").append(nfca.getSak()).append("\n");
            sb.append("ATQA: ").append(bytesToHex(nfca.getAtqa())).append("\n\n");

            sb.append("Reading Pages...\n\n");

            for (int page = 0; page < 48; page++) {   // NTAG213 = 48 pages
                byte[] response = nfca.transceive(
                        new byte[]{0x30, (byte) page}
                );

                if (response != null && response.length == 16) {
                    String ascii = new String(response, StandardCharsets.UTF_8);
                    sb.append("Page ").append(page).append(" : ")

                            
                            .append(ascii)
                            .append("\n");
                }
            }

            txtStatus.setText(sb.toString());
            nfca.close();

        } catch (Exception e) {
            txtStatus.setText("Error: " + e.getMessage());
        }
        /*MifareUltralight mifare = MifareUltralight.get(tag);
        try {
            mifare.connect();
            int type = mifare.getType();
            Log.d("TAG_TYPE", String.valueOf(type));
            StringBuilder result = new StringBuilder();

            // Pages 4 to 15 are user readable/writable
            for (int i = 4; i <= 60; i++) {
                byte[] data = mifare.readPages(i);


                result.append("Page ").append(i).append(": ")
                        .append(new String(data, StandardCharsets.UTF_8)).append("\n");
            }

            mifare.close();
            txtStatus.setText(result.toString());

        } catch (Exception e) {
            txtStatus.setText("Error: " + e.getMessage());
        }*/
    }

    private String hexToAscii(String hex) {
        StringBuilder output = new StringBuilder();
        for (int i = 0; i < hex.length(); i += 2) {
            String str = hex.substring(i, i + 2);
            output.append((char) Integer.parseInt(str, 16));
        }
        return output.toString();
    }
    private String bytesToHex(byte[] bytes) {
        StringBuilder sb = new StringBuilder();
        for (byte b : bytes) {
            sb.append(String.format("%02X ", b));
        }
        return sb.toString();
    }

    // ---------- READ DATA ----------
    private void readFromTag(Intent intent) {
        Parcelable[] messages = intent.getParcelableArrayExtra(NfcAdapter.EXTRA_NDEF_MESSAGES);
        if (messages != null) {
            NdefMessage ndefMessage = (NdefMessage) messages[0];
            String text = new String(ndefMessage.getRecords()[0].getPayload());
            txtStatus.setText("Read Data: " + text);
        } else {
            txtStatus.setText("No NDEF Data Found");
        }
    }

    // ---------- WRITE DATA ----------
    private void writeToTag(Tag tag, String message) {
        try {
            NdefRecord record = NdefRecord.createTextRecord("en", message);
            NdefMessage ndefMessage = new NdefMessage(new NdefRecord[]{record});

            Ndef ndef = Ndef.get(tag);
            if (ndef != null) {
                ndef.connect();
                ndef.writeNdefMessage(ndefMessage);
                ndef.close();
                txtStatus.setText("Successfully Written!");
            } else {
                txtStatus.setText("NDEF NOT Supported!");
            }
        } catch (Exception e) {
            txtStatus.setText("Write Error: " + e.getMessage());
        }
        writeMode = false;
    }

    private int extractId(String text) {
        return Integer.parseInt(text.split("-")[0]);
    }

    private void loadLocations() {
        locList = db.getLocations();
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
    }

    private void loadVendors(int productId) {
        vendorList = db.getVendors(productId);
        spVendor.setAdapter(new ArrayAdapter<>(this, android.R.layout.simple_spinner_item, vendorList));
    }

    private void loadDestinations(int vendorId) {
        destList = db.getDestinationLocations(vendorId);
        spDestination.setAdapter(new ArrayAdapter<>(this, android.R.layout.simple_spinner_item, destList));
    }

    private void loadRoutes(int destId) {
        routeList = db.getRoutes(destId);
        spRoute.setAdapter(new ArrayAdapter<>(this, android.R.layout.simple_spinner_item, routeList));
    }
}
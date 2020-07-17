package com.example.nfctest;

import android.app.PendingIntent;
import android.content.Intent;
import android.content.IntentFilter;
import android.nfc.NdefMessage;
import android.nfc.NdefRecord;
import android.nfc.NfcAdapter;
import android.nfc.Tag;
import android.nfc.tech.Ndef;
import android.nfc.tech.NdefFormatable;
import android.os.Build;
import android.os.Bundle;
import android.os.Parcelable;
import android.util.Log;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.RequiresApi;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import java.util.ArrayList;
import java.util.Objects;


public class MainActivity extends AppCompatActivity {

    NfcAdapter nfcAdapter;
    RecyclerView mMessagesRecycler;

    ArrayList<String> messages = new ArrayList<>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);


        nfcAdapter = NfcAdapter.getDefaultAdapter(this);
    }


    @Override
    protected void onResume() {
        super.onResume();

        // read nfc tag
        if (getIntent().hasExtra(NfcAdapter.EXTRA_TAG)) {

            NdefMessage ndefMessage = this.getNdefMessageFromIntent(getIntent());

            if(ndefMessage.getRecords().length > 0){

                NdefRecord ndefRecord = ndefMessage.getRecords()[0];

                String payload = new String(ndefRecord.getPayload());

                Toast.makeText(this, payload, Toast.LENGTH_SHORT).show();

            }

        }

        enableForegroundDispatchSystem();
    }

    @Override
    protected void onPause() {
        super.onPause();

        disableForegroundDispatchSystem();
    }

    @Override
    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);
        mMessagesRecycler = findViewById(R.id.messages_recycler);
        mMessagesRecycler.setLayoutManager(new LinearLayoutManager(this));
        final DataAdapter dataAdapter = new DataAdapter(this, messages);
        mMessagesRecycler.setAdapter(dataAdapter);
        if (intent.hasExtra(NfcAdapter.EXTRA_TAG)) {
            Toast.makeText(this,  intent.getDataString(), Toast.LENGTH_SHORT).show();
            if (intent.getDataString() != null){
                mMessagesRecycler.smoothScrollToPosition(intent.getDataString().length());
                messages.add(intent.getDataString());
                Log.d("Полученный тэг", intent.getDataString());
            }
            Tag tag = intent.getParcelableExtra(NfcAdapter.EXTRA_TAG);

            // TODO: implement nfc message constructing logic here.
            byte[] payload = "my string".getBytes();

            NdefRecord ndefRecord = NdefRecord.createExternal("nfctutorials", "externaltype", payload);
            //

            NdefMessage ndefMessage = new NdefMessage(new NdefRecord[]{ndefRecord});

            writeNdefMessage(tag, ndefMessage);
        }
    }

    private void enableForegroundDispatchSystem() {

        Intent intent = new Intent(this, MainActivity.class).addFlags(Intent.FLAG_RECEIVER_REPLACE_PENDING);

        PendingIntent pendingIntent = PendingIntent.getActivity(this, 0, intent, 0);

        IntentFilter[] intentFilters = new IntentFilter[]{};

        nfcAdapter.enableForegroundDispatch(this, pendingIntent, intentFilters, null);
    }

    private void disableForegroundDispatchSystem() {
        nfcAdapter.disableForegroundDispatch(this);
    }

    private void formatTag(Tag tag, NdefMessage ndefMessage) {
        try {

            NdefFormatable ndefFormatable = NdefFormatable.get(tag);

            if (ndefFormatable == null) {
                Toast.makeText(this, "Tag is not ndef formatable!", Toast.LENGTH_SHORT).show();
                return;
            }


            ndefFormatable.connect();
            ndefFormatable.format(ndefMessage);
            ndefFormatable.close();

            Toast.makeText(this, "Tag writen!", Toast.LENGTH_SHORT).show();

        } catch (Exception e) {
            Log.e("formatTag", e.getMessage());
        }

    }

    private void writeNdefMessage(Tag tag, NdefMessage ndefMessage) {

        try {

            if (tag == null) {
                Toast.makeText(this, "Tag object cannot be null", Toast.LENGTH_SHORT).show();
                return;
            }

            Ndef ndef = Ndef.get(tag);

            if (ndef == null) {
                // format tag with the ndef format and writes the message.
                formatTag(tag, ndefMessage);
            } else {
                ndef.connect();

                if (!ndef.isWritable()) {
                    Toast.makeText(this, "Tag is not writable!", Toast.LENGTH_SHORT).show();

                    ndef.close();
                    return;
                }

                ndef.writeNdefMessage(ndefMessage);
                ndef.close();

                Toast.makeText(this, "Tag writen!", Toast.LENGTH_SHORT).show();

            }

        } catch (Exception e) {
            Log.e("writeNdefMessage", e.getMessage());
        }

    }

    public NdefMessage getNdefMessageFromIntent(Intent intent) {
        NdefMessage ndefMessage = null;
        Parcelable[] extra = intent.getParcelableArrayExtra(NfcAdapter.EXTRA_NDEF_MESSAGES);

        if (extra != null && extra.length > 0) {
            ndefMessage = (NdefMessage) extra[0];
        }
        return ndefMessage;
    }
}

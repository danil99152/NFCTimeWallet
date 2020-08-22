package com.example.nfctest;

import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.nfc.NdefMessage;
import android.nfc.NdefRecord;
import android.nfc.NfcAdapter;
import android.os.Bundle;
import android.os.Parcelable;
import android.os.SystemClock;
import android.view.Display;
import android.view.MotionEvent;
import android.view.View;
import android.view.WindowManager;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import java.util.Timer;
import java.util.TimerTask;


public class MainActivity extends AppCompatActivity implements OutcomingNfcManager.NfcActivity{

    NfcAdapter nfcAdapter;

    int secs = 0;
    int mins = 0;
    int hours = 24;
    int sendingTime = 0;
    String outMessage;

    TextView tvText;
    TextView tvSendingMoney;
    SensorManager sensorManager;
    Sensor sensorAccel;
    Sensor sensorMagnet;

    StringBuilder sb = new StringBuilder();

    Timer timer;
    Timer time;
    boolean isTimeRunning = false;

    int rotation;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.main);

        nfcAdapter = NfcAdapter.getDefaultAdapter(this);

        if (!isNfcSupported()) {
            Toast.makeText(this, "Nfc is not supported on this device", Toast.LENGTH_SHORT).show();
            finish();
        }
        if (!nfcAdapter.isEnabled()) {
            Toast.makeText(this, "NFC disabled on this device. Turn on to proceed", Toast.LENGTH_SHORT).show();
        }

        tvText = findViewById(R.id.tvText);
        tvSendingMoney = findViewById(R.id.sendingMoney);
        sensorManager = (SensorManager) getSystemService(SENSOR_SERVICE);
        sensorAccel = sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);
        sensorMagnet = sensorManager.getDefaultSensor(Sensor.TYPE_MAGNETIC_FIELD);

        // encapsulate sending logic in a separate class
        OutcomingNfcManager outcomingNfccallback = new OutcomingNfcManager(this);
        this.nfcAdapter.setOnNdefPushCompleteCallback(outcomingNfccallback, this);
        this.nfcAdapter.setNdefPushMessageCallback(outcomingNfccallback, this);

        //обработка зажатия нижней кнопки
        findViewById(R.id.zahzatie).setOnTouchListener(new View.OnTouchListener() {
            private static final long REPEAT_INTERVAL = 100L; // интервал повтора в миллисекундах
            private long lastAction = 0L;

            @Override
            public boolean onTouch(View v, MotionEvent event) {
                long currTime = SystemClock.uptimeMillis();
                switch (event.getActionMasked()) {
                    //Нажатие
                    case MotionEvent.ACTION_DOWN:
                        lastAction = currTime;
                        hours = hours + sendingTime;
                        sendingTime = 0;
                        break;
                    //Удержание
                    case MotionEvent.ACTION_MOVE:
                        if (currTime - lastAction >= REPEAT_INTERVAL) {
                            lastAction = currTime;
                            if (hours > 0) {
                                hours--;
                                sendingTime++;
                            }
                            else Toast.makeText(MainActivity.this, "Часов нет, но вы держитесь", Toast.LENGTH_SHORT).show();
                        }
                        break;
                }
                return true;
            }

        });
    }

    private boolean isNfcSupported() {
        this.nfcAdapter = NfcAdapter.getDefaultAdapter(this);
        return this.nfcAdapter != null;
    }

    @Override
    protected void onResume() {
        super.onResume();

        // read nfc tag
        if (getIntent().hasExtra(NfcAdapter.EXTRA_TAG)) {

            NdefMessage ndefMessage = this.getNdefMessageFromIntent(getIntent());

            if (ndefMessage.getRecords().length > 0){

                NdefRecord ndefRecord = ndefMessage.getRecords()[0];

                final String payload = new String(ndefRecord.getPayload());

                Toast.makeText(this, payload, Toast.LENGTH_SHORT).show();
            }

        }

        enableForegroundDispatchSystem();

        sensorManager.registerListener(listener, sensorAccel, SensorManager.SENSOR_DELAY_NORMAL);
        sensorManager.registerListener(listener, sensorMagnet, SensorManager.SENSOR_DELAY_NORMAL);

        timer = new Timer();
        TimerTask task = new TimerTask() {
            @Override
            public void run() {
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        getActualDeviceOrientation();
                        showInfo();
                        setOutGoingMessage();
                    }
                });
            }
        };
        timer.schedule(task, 0, 400);

        WindowManager windowManager = ((WindowManager) getSystemService(Context.WINDOW_SERVICE));
        Display display = windowManager.getDefaultDisplay();
        rotation = display.getRotation();

        if (!isTimeRunning)
          timer();
    }

    private void timer() {
        time = new Timer();
        isTimeRunning = true;
        TimerTask timeTask = new TimerTask() {
            @Override
            public void run() {
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        if (secs > 0){
                            secs--;
                        }
                        else {
                            secs = secs + 59;
                            if (mins > 0){
                                mins--;
                            }
                            else {
                                mins = mins + 59;
                                if (hours > 0)
                                    hours--;
                            }
                        }
                        if (secs <= 0 && mins <= 0 && hours <= 0 && sendingTime > 0)
                            sendingTime--;
                    }
                });
            }
        };
        time.schedule(timeTask, 0, 1000);
    }

    @Override
    protected void onPause() {
        super.onPause();

        disableForegroundDispatchSystem();
        sensorManager.unregisterListener(listener);
        timer.cancel();
    }

    void showInfo() {
        sb.setLength(0);
        sb.append(hours + ":" + mins + ":" + secs);
        tvText.setText(sb);
        tvSendingMoney.setText("Отправляемые часы: " + sendingTime);
    }

    float[] r = new float[9];

    void getActualDeviceOrientation() {
        SensorManager.getRotationMatrix(r, null, valuesAccel, valuesMagnet);
        SensorManager.getOrientation(r, valuesResult);

        valuesResult[0] = (float) Math.toDegrees(valuesResult[0]);
        valuesResult[1] = (float) Math.toDegrees(valuesResult[1]);
        valuesResult[2] = (float) Math.toDegrees(valuesResult[2]);
        return;
    }

    float[] valuesAccel = new float[3];
    float[] valuesMagnet = new float[3];
    float[] valuesResult = new float[3];


    SensorEventListener listener = new SensorEventListener() {

        @Override
        public void onAccuracyChanged(Sensor sensor, int accuracy) {
        }

        @Override
        public void onSensorChanged(SensorEvent event) {
            switch (event.sensor.getType()) {
                case Sensor.TYPE_ACCELEROMETER:
                    for (int i=0; i < 3; i++){
                        valuesAccel[i] = event.values[i];
                    }
                    break;
                case Sensor.TYPE_MAGNETIC_FIELD:
                    for (int i=0; i < 3; i++){
                        valuesMagnet[i] = event.values[i];
                    }
                    break;
            }
        }
    };

    @Override
    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);
        String action = intent.getAction();
        if (NfcAdapter.ACTION_NDEF_DISCOVERED.equals(action)) {
            Parcelable[] parcelables = intent.getParcelableArrayExtra(NfcAdapter.EXTRA_NDEF_MESSAGES);

            NdefMessage inNdefMessage = (NdefMessage) parcelables[0];
            NdefRecord[] inNdefRecords = inNdefMessage.getRecords();
            NdefRecord ndefRecord_0 = inNdefRecords[0];

            //Полученное сообщение
            final String inMessage = new String(ndefRecord_0.getPayload());
            int receivedSum = Integer.parseInt(inMessage);
            hours = hours + receivedSum;
            Toast.makeText(this, "Полученная сумма: " + receivedSum + " ч.", Toast.LENGTH_SHORT).show();
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

    public NdefMessage getNdefMessageFromIntent(Intent intent) {
        NdefMessage ndefMessage = null;
        Parcelable[] extra = intent.getParcelableArrayExtra(NfcAdapter.EXTRA_NDEF_MESSAGES);

        if (extra != null && extra.length > 0) {
            ndefMessage = (NdefMessage) extra[0];
        }
        return ndefMessage;
    }

    private void setOutGoingMessage() {
        if (valuesResult[1] >= -90.0 && valuesResult[1] <= 90.0 && valuesResult[2] >= -90.0 && valuesResult[2] <= 90.0) {
            outMessage = String.valueOf(sendingTime);
        }
    }

    @Override
    public String getOutcomingMessage() {
        return outMessage;
    }

    @Override
    public void signalResult() {
        sendingTime = 0;
    }
}
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

    int money = 100;
    int sendingMoney = 0;
    String outMessage;

    TextView tvText;
    TextView tvSendingMoney;
    SensorManager sensorManager;
    Sensor sensorAccel;
    Sensor sensorMagnet;

    StringBuilder sb = new StringBuilder();

    Timer timer;

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
                        money = money + sendingMoney;
                        sendingMoney = 0;
                        break;
                    //Удержание
                    case MotionEvent.ACTION_MOVE:
                        if (currTime - lastAction >= REPEAT_INTERVAL) {
                            lastAction = currTime;
                            if (money > 0) {
                                money--;
                                sendingMoney++;
                            }
                            else Toast.makeText(MainActivity.this, "Баланс отрицательный", Toast.LENGTH_SHORT).show();
                        }
//                        Toast.makeText(MainActivity.this, "Чтобы обнулить сумму, нажмите еще раз", Toast.LENGTH_SHORT).show();
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
//                        Log.d("наклон вдоль дисплея", String.valueOf(valuesResult[1]));
//                        Log.d("наклон поперек дисплея", String.valueOf(valuesResult[2]));
                        setOutGoingMessage();
                    }
                });
            }
        };
        timer.schedule(task, 0, 400);

        WindowManager windowManager = ((WindowManager) getSystemService(Context.WINDOW_SERVICE));
        Display display = windowManager.getDefaultDisplay();
        rotation = display.getRotation();

        timer = new Timer();
        TimerTask task2 = new TimerTask() {
            @Override
            public void run() {
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        if (money > 0)
                            money--;
                        else if (sendingMoney > 0)
                            sendingMoney--;
                    }
                });
            }
        };
        timer.schedule(task2, 0, 1000);
    }

    @Override
    protected void onPause() {
        super.onPause();

        disableForegroundDispatchSystem();
        sensorManager.unregisterListener(listener);
        timer.cancel();
    }

    //    String format(float values[]) {
//        return String.format("%1$.1f\t\t%2$.1f\t\t%3$.1f", values[0], values[1], values[2]);
//    }

    void showInfo() {
        sb.setLength(0);
        sb.append(money);
        tvText.setText(sb);
        tvSendingMoney.setText("Отправляемая сумма: " + sendingMoney);
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
            money = money + receivedSum;
            Toast.makeText(this, "Полученная сумма: " + receivedSum, Toast.LENGTH_SHORT).show();
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
            outMessage = String.valueOf(sendingMoney);
        }
    }

    @Override
    public String getOutcomingMessage() {
        return outMessage;
    }

    @Override
    public void signalResult() {
        sendingMoney = 0;
    }
}
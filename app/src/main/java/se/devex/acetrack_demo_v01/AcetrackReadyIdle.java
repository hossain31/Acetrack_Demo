package se.devex.acetrack_demo_v01;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;

import java.util.StringTokenizer;

import static se.devex.acetrack_demo_v01.DeviceControlActivity.mBluetoothLeService;

public class AcetrackReadyIdle extends AppCompatActivity {

    private final static String TAG = AcetrackReadyIdle.class.getSimpleName();

    //Declare TextView and Button
    TextView battery;
    TextView warmedText;
    TextView warmingText;
    TextView checkText;
    TextView acetrackRedoPic;
    Button mButtonWrite;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        Log.d(TAG, String.format("*** onCreate()"));
        setTheme(R.style.AppTheme);
        super.onCreate(savedInstanceState);
        setContentView(R.layout.acetrack_ready_idle);

        //Bind the BluetoothLeService class to stay connected with the GATT service
        Intent gattServiceIntent = new Intent(this, BluetoothLeService.class);
        startService(gattServiceIntent);

        battery = (TextView) findViewById(R.id.battryStatus);
        warmedText = (TextView) findViewById(R.id.warmedText);
        warmingText = (TextView) findViewById(R.id.warmingText);
        checkText = (TextView) findViewById(R.id.checkText);
        acetrackRedoPic = (TextView) findViewById(R.id.acetrackRedoPic);
        mButtonWrite = (Button)findViewById(R.id.CharRW_btnWrite);

        //Make invisible/disable at startup
        checkText.setVisibility(View.INVISIBLE);
        battery.setVisibility(View.INVISIBLE);
        acetrackRedoPic.setVisibility(View.INVISIBLE);
        //mButtonWrite.setVisibility(View.INVISIBLE);
        mButtonWrite.setEnabled(false);

        //Listener for the button mButtonWrite click
        mButtonWrite.setOnClickListener(new Button.OnClickListener(){
            public void onClick(View v) {
                //send START command to the device
                mBluetoothLeService.writeCharacteristic(DeviceControlActivity.mTargetCharacteristic, "START;5\r\n");
                //start new activity
                Intent intent= new Intent(AcetrackReadyIdle.this, AcetrackReadyAndWait.class);
                startActivity(intent);
            }
        });
    }


    @Override
    protected void onResume() {
        super.onResume();
        registerReceiver(mGattUpdateReceiver, makeGattUpdateIntentFilter());
    }


    @Override
    protected void onPause() {
        super.onPause();
        unregisterReceiver(mGattUpdateReceiver);
        //unbindService(mServiceConnection);
        //mBluetoothLeService = null;
    }


    //Remove all non-digits value from the string
    public static String stripNonDigits(final CharSequence input){
        final StringBuilder sb = new StringBuilder(input.length());
        for(int i = 0; i < input.length(); i++){
            final char c = input.charAt(i);
            if(c > 47 && c < 58){ //ASCII 48=0 and 57=9
                sb.append(c);
            }
        }
        return sb.toString();
    }


    //Receive data_stream from the device and take care of it
    String strCollect="";
    private void displayRxData(byte[] data) {
        if (data != null) {
            //declare StringBuilder
            final StringBuilder strData = new StringBuilder();

            for(byte byteChar : data)
                strData.append(String.format("%c", byteChar));
                Log.d(TAG, "***strData = "+strData);

            //check strData and combined them together into myString if it splited
            strCollect += strData.toString();
            Log.d(TAG, "***string to add = " + strData.toString());
            int index = strCollect.toString().indexOf("\n");
            if(index == -1) {
                return;
            }
            String myString = strCollect.substring(0, index+1);
            strCollect = strCollect.substring(index+1);
            Log.d(TAG, "***myString = " + myString + " " + myString.length());

            //declare Tokenizer
            StringTokenizer st = new StringTokenizer(myString,";");
            //1st token
            String stateChange = st.nextToken();
            Log.d(TAG, "stateChange = " + stateChange);
            //2nd token
            String stateStatus = st.nextToken();
            Log.d(TAG, "stateStatus = " + stateStatus);
            //stateStatus.replaceAll("[^0-9]", "");

            //remove non-digit char from the string
            final String batteryLevel = stripNonDigits(stateStatus);
            Log.d(TAG, "***Battery_Level = " + batteryLevel);

            //display battery level
            String stateBattery = "BATTERY";
            if(stateChange.equals(stateBattery)) {
                if (batteryLevel == "") {
                    battery.setText("Batteri: 0%");
                } else {
                    battery.setText("Batteri: " + batteryLevel + "%");
                }
            }

            //display device is warmed and Acetrack is ready to run
            checkText.setText(myString);
            Log.d(TAG, "displayWarming = " + checkText.getText().toString() + ":" + (checkText.getText().toString()).equals("SC;CONNECTED\r\n"));
            if((checkText.getText().toString()).equals("SC;CONNECTED\r\n")) {
                warmingText.setVisibility(View.INVISIBLE);
                warmedText.setText("Devicet är uppvärmd");
                mButtonWrite.setEnabled(true);
                //mButtonWrite.setVisibility(View.VISIBLE);
                acetrackRedoPic.setVisibility(View.VISIBLE);
                battery.setVisibility(View.VISIBLE);
            }
        }
    }


    // Handles various events fired by the Service.
    // ACTION_GATT_CONNECTED: connected to a GATT server.
    // ACTION_GATT_DISCONNECTED: disconnected from a GATT server.
    // ACTION_GATT_SERVICES_DISCOVERED: discovered GATT services.
    // ACTION_DATA_AVAILABLE: received data from the device.
    //                        This can be a result of read or notification operations.
    private final BroadcastReceiver mGattUpdateReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            final String action = intent.getAction();

            if (BluetoothLeService.ACTION_GATT_CONNECTED.equals(action)) {
            //put some code here
            }
            else if (BluetoothLeService.ACTION_GATT_DISCONNECTED.equals(action)) {
                // put some code here
                intent = new Intent(AcetrackReadyIdle.this, AcetrackError.class);
                startActivity(intent);
                finish(); //close the activity
            }
            else if (BluetoothLeService.ACTION_DATA_AVAILABLE.equals(action)) {
                displayRxData(intent.getByteArrayExtra(BluetoothLeService.EXTRA_DATA));
            }
            Log.d(TAG, "BroadcastReceiver.onReceive():action="+action);
        }
    };


    private static IntentFilter makeGattUpdateIntentFilter() {
        final IntentFilter intentFilter = new IntentFilter();
        intentFilter.addAction(BluetoothLeService.ACTION_GATT_CONNECTED);
        intentFilter.addAction(BluetoothLeService.ACTION_GATT_DISCONNECTED);
        intentFilter.addAction(BluetoothLeService.ACTION_GATT_SERVICES_DISCOVERED);
        intentFilter.addAction(BluetoothLeService.ACTION_DATA_AVAILABLE);
        return intentFilter;
    }
}


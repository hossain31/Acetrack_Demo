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

public class AcetrackResult  extends AppCompatActivity {

    private final static String TAG = AcetrackResult.class.getSimpleName();

    //Declare TextView and Button
    TextView mRxData;
    TextView rXResultStatus;
    Button mButtonWrite;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        Log.d(TAG, String.format("*** onCreate()"));
        setTheme(R.style.AppTheme);
        super.onCreate(savedInstanceState);
        setContentView(R.layout.acetrack_result);

        //Bind the BluetoothLeService class to stay connected with the GATT service
        Intent gattServiceIntent = new Intent(this, BluetoothLeService.class);
        startService(gattServiceIntent);

        mRxData =  (TextView) findViewById(R.id.CharRW_ReadData);
        rXResultStatus =  (TextView) findViewById(R.id.rXResultStatus);
        mButtonWrite = (Button)findViewById(R.id.charRW_btnWrite);

        //Listener for the button mButtonWrite click
        mButtonWrite.setOnClickListener(new Button.OnClickListener(){
            public void onClick(View v) {
                //send FINISHED command to the device
                mBluetoothLeService.writeCharacteristic(DeviceControlActivity.mTargetCharacteristic, "FINISHED\r\n");
                //start new activity
                Intent intent= new Intent(AcetrackResult.this, AcetrackReadyIdle.class);
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
            if(c > 45 && c < 58){ //ASCII 46=. and 57=9
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
            String acetonValue = st.nextToken();
            Log.d(TAG, "acetonValue = " + acetonValue);
            //remove non-digit char from the string
            final String acetonResult = stripNonDigits(acetonValue);
            Log.d(TAG, "***Aceton_result = " + acetonResult);

            //display aceton value
            String stateResult1 = "RESULT";
            String stateResult2 = "ESULT";
            if((stateChange.equals(stateResult1)) || (stateChange.equals(stateResult2))) {
                if (acetonResult == "") {
                    mRxData.setText("Något gick fel!");
                }else {
                    mRxData.setText(acetonResult + " ppm");
                }
            }else {
                mRxData.setText("Något gick fel!");
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

            }
            else if (BluetoothLeService.ACTION_GATT_DISCONNECTED.equals(action)) {
                //start new activity
                intent = new Intent(AcetrackResult.this, AcetrackError.class);
                startActivity(intent);
                finish(); //close the activity
            }
            else if (BluetoothLeService.ACTION_DATA_AVAILABLE.equals(action)) {
                displayRxData(intent.getByteArrayExtra(BluetoothLeService.EXTRA_DATA));
            }
            Log.d(TAG, "BroadcastReceiver.onReceive():action= "+action);
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

package se.devex.acetrack_demo_v01;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.View;
import android.widget.TextView;

import java.util.StringTokenizer;

public class AcetrackReadyAndWait extends AppCompatActivity {

    private final static String TAG = AcetrackReadyAndWait.class.getSimpleName();

    //Declare TextView and Button
    TextView ready_to_blow;
    TextView checkText;
    TextView mRxData;


    @Override
    protected void onCreate (Bundle saveInstanceState){
        Log.d(TAG, String.format("*** onCreate()"));
        setTheme(R.style.AppTheme);
        super.onCreate(saveInstanceState);
        setContentView(R.layout.acetrack_ready_and_wait);

        //Bind the BluetoothLeService class to stay connected with the GATT service
        Intent gattServiceIntent = new Intent(this, BluetoothLeService.class);
        startService(gattServiceIntent);

        ready_to_blow = (TextView)findViewById(R.id.ready_to_blow);
        checkText = (TextView) findViewById(R.id.display_time);
        checkText.setVisibility(View.INVISIBLE);
        mRxData = (TextView) findViewById(R.id.speedoRX_readData);
        mRxData.setVisibility(View.INVISIBLE);

        //Show the blowing value in Graphical mode and the corresponding msg.
        mRxData.addTextChangedListener(new TextWatcher() {
            public void afterTextChanged(Editable s) {
            }
            public void beforeTextChanged(CharSequence s, int start,
                                          int count, int after) {
            }
            public void onTextChanged(CharSequence s, int start,
                                      int before, int count) {
                final Speedometer speedometer = (Speedometer) findViewById(R.id.Speedometer);

                String msg1 = "För svagt, blås hårdare...";
                String msg2 = "Bra fortsätt...";
                String msg3 = "För hårt, lätta upp...";
                String msg4 = "Ingen blåsning...";

                //convert string value into int value
                int value = Integer.parseInt(String.valueOf(s));
                Log.d(TAG, "s_value = " + value);

                speedometer.onSpeedChanged(value);
                float currentSpeed = value;
                //blow lite
                if(currentSpeed>=1 && currentSpeed<60){
                    ready_to_blow.setText(msg1);
                }
                //blow perfect
                else if (currentSpeed>=60 && currentSpeed<=74){
                    ready_to_blow.setText(msg2);
                }
                //blow hard
                else if(currentSpeed>74 && currentSpeed<=99){
                    ready_to_blow.setText(msg3);
                }
                //no blow
                else ready_to_blow.setText(msg4);
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


    //Press the mobile's back button to close the app!
    @Override
    public void onBackPressed() {
        //finishAndRemoveTask (); //for API:21=+
        this.finishAffinity(); //for API:16 to 20
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
            //Log.d(TAG, "flowValue = " + stateStatus);

            //wait for msg from the device to change activity
            checkText.setText(stateStatus);
            Log.d(TAG, "display = " + checkText.getText().toString()+" :"+(checkText.getText().toString()).equals("ANALYZE\r\n"));
            if((checkText.getText().toString()).equals("ANALYZE\r\n")){
                //start new activity
                Intent intent= new Intent(AcetrackReadyAndWait.this, AcetrackAnalyzing.class);
                startActivity(intent);
            }

            //remove non-digit char from the string
            final String flowValue = stripNonDigits(stateStatus);
            Log.d(TAG, "***Flow_Value = " + flowValue);

            //display flow level
            String stateBlow = "FLOW";
            if(stateChange.equals(stateBlow)) {
                if (flowValue == "") {
                    mRxData.setText("0");
                }else {
                    mRxData.setText(flowValue);
                }
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
                // put some code here
                intent = new Intent(AcetrackReadyAndWait.this, AcetrackError.class);
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

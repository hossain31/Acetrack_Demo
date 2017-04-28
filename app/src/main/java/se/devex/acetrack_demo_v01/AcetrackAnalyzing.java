package se.devex.acetrack_demo_v01;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;
import android.os.CountDownTimer;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.View;
import android.widget.TextView;

import java.util.StringTokenizer;

public class AcetrackAnalyzing extends AppCompatActivity {

    private final static String TAG = AcetrackAnalyzing.class.getSimpleName();

    //Declare TextView and Button
    TextView txt1;
    TextView txt2;
    TextView txt3;
    TextView checkText;

    @Override
    protected void onCreate (Bundle saveInstanceState){
        Log.d(TAG, String.format("*** onCreate()"));
        setTheme(R.style.AppTheme);
        super.onCreate(saveInstanceState);
        setContentView(R.layout.acetrack_analyzing);

        //Bind the BluetoothLeService class to stay connected with the GATT service
        Intent gattServiceIntent = new Intent(this, BluetoothLeService.class);
        startService(gattServiceIntent);

        checkText= (TextView) findViewById(R.id.textView3);
        checkText.setVisibility(View.INVISIBLE);
        //timer while analyzing
        startTimer(totalSecond);
    }


    //Display visual presentation while waiting for Analyze
    long totalSecond = 30000; //1000=1sec
    CountDownTimer blinkTimer;
    //start timer
    void startTimer(final long totalSecond) {
         blinkTimer = new CountDownTimer(totalSecond, 1000) {
            public void onTick(long millisUntilFinished) {
                txt1= (TextView) findViewById(R.id.txt1);
                txt2= (TextView) findViewById(R.id.txt2);
                txt3= (TextView) findViewById(R.id.txt3);

                if (txt1.getVisibility() == View.VISIBLE) {
                    txt1.setVisibility(View.INVISIBLE);
                    txt2.setVisibility(View.VISIBLE);
                    txt3.setVisibility(View.INVISIBLE);
                } else if (txt2.getVisibility() == View.VISIBLE){
                    txt1.setVisibility(View.INVISIBLE);
                    txt2.setVisibility(View.INVISIBLE);
                    txt3.setVisibility(View.VISIBLE);
                } else if (txt3.getVisibility() == View.VISIBLE){
                    txt1.setVisibility(View.VISIBLE);
                    txt2.setVisibility(View.INVISIBLE);
                    txt3.setVisibility(View.INVISIBLE);
                } else {
                    txt1.setVisibility(View.VISIBLE);
                    txt2.setVisibility(View.VISIBLE);
                    txt3.setVisibility(View.VISIBLE);
                }
            }

            public void onFinish() {
                //cancel timer
                this.cancel();
                //start new activity
                Intent fintent= new Intent();
                fintent.setClassName("se.devex.acetrack_demo_v01","se.devex.acetrack_demo_v01.AcetrackResult");
                startActivityForResult(fintent, 0);
            }
        };
        blinkTimer.start();
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

            //wait for msg from the device to change activity
            checkText.setText(stateStatus);
            Log.d(TAG, "display = " + checkText.getText().toString()+" : " +(checkText.getText().toString()).equals("RESULT\r\n"));
            if((checkText.getText().toString()).equals("RESULT\r\n")){
                //cancel timer
                blinkTimer.cancel();
                //start new activity
                Intent intent= new Intent(AcetrackAnalyzing.this, AcetrackResult.class);
                startActivity(intent);
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
                intent = new Intent(AcetrackAnalyzing.this, AcetrackError.class);
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


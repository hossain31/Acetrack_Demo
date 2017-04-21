package se.devex.acetrack_demo_v01;

import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothGattService;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.ServiceConnection;
import android.os.Bundle;
import android.os.IBinder;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;

import java.util.List;

/**
 * Source: https://developer.android.com/samples/BluetoothLeGatt/src/com.example.android.bluetoothlegatt/DeviceControlActivity.html
 */

/**
 * For a given BLE device, this Activity provides the user interface to connect, display data,
 * and display GATT services and characteristics supported by the device.  The Activity
 * communicates with {@code BluetoothLeService}, which in turn interacts with the Bluetooth LE API.
 */
public class DeviceControlActivity extends AppCompatActivity {
    private final static String TAG = DeviceControlActivity.class.getSimpleName();

    public static final String EXTRAS_DEVICE_NAME = "DEVICE_NAME";
    public static final String EXTRAS_DEVICE_ADDRESS = "DEVICE_ADDRESS";
    public static BluetoothGattCharacteristic mTargetCharacteristic; //Common variable for Read/Write operation
    public static BluetoothLeService mBluetoothLeService; //BluetoothLE service
    public static String mDeviceAddress; //Device address
    public static boolean mConnected = false;  //Connection status

    private boolean mIndicate;
    private BluetoothGattCharacteristic mIndicateCharacteristic; //used for Bluetooth GATT Characteristic (Indicate)


    @Override
    public void onCreate(Bundle savedInstanceState) {
        setTheme(R.style.AppTheme);
        super.onCreate(savedInstanceState);

        final Intent intent = getIntent();
        mDeviceAddress = intent.getStringExtra(EXTRAS_DEVICE_ADDRESS);

        //Bind the BluetoothLeService class to stay connected with the GATT service
        Intent gattServiceIntent = new Intent(this, BluetoothLeService.class);
        bindService(gattServiceIntent, mServiceConnection, BIND_AUTO_CREATE);
    }


    @Override
    protected void onResume() {
        super.onResume();
        registerReceiver(mGattUpdateReceiver, makeGattUpdateIntentFilter());
        if (mBluetoothLeService != null) {
            final boolean result = mBluetoothLeService.connect(mDeviceAddress);
            Log.d(TAG, "Connect request result = " + result);
        }
    }


    @Override
    protected void onPause() {
        super.onPause();
        unregisterReceiver(mGattUpdateReceiver);
    }


    @Override
    protected void onDestroy() {
        super.onDestroy();
        unbindService(mServiceConnection);
        mBluetoothLeService = null;
    }


    // Demonstrates how to go through the particular supported GATT Services/Characteristics.
    private void displayGattServices(List<BluetoothGattService> gattServices) {
        if (gattServices == null) return;
        String uuid = null;

        // Loops through available GATT Services.
        for (BluetoothGattService gattService : gattServices) {
            uuid = gattService.getUuid().toString();

            if (uuid.equals("1d5688de-866d-3aa4-ec46-a1bddb37ecf6")) {
                Log.d(TAG, String.format("***Hittat Bluegiga_Service" + uuid));
                List<BluetoothGattCharacteristic> gattCharacteristics = gattService.getCharacteristics();

                // Loops through available Characteristics.
                for (BluetoothGattCharacteristic gattCharacteristic : gattCharacteristics) {
                    uuid = gattCharacteristic.getUuid().toString();

                    if (uuid.equals("af20fbac-2518-4998-9af7-af42540731b3")) {
                        Log.d(TAG, String.format("***Hittat Read_Write" + uuid));
                        mTargetCharacteristic = gattCharacteristic;
                    }
                }
            }
        }

        //-----------Start Indicate/Write-------
        if (((BluetoothGattCharacteristic.PROPERTY_INDICATE) > 0) || ((BluetoothGattCharacteristic.PROPERTY_WRITE) > 0)) {

            if (!mIndicate) {
                if ( DeviceControlActivity.mConnected) { //Check connection state before READ
                    mBluetoothLeService.readCharacteristic(mTargetCharacteristic);
                }
            }
            if (mTargetCharacteristic != null) {
                // if ( DeviceControlActivity.mConnected) { //Check connection state before READ
                if (mIndicateCharacteristic == null) {
                    // Enable Indicate
                    mIndicateCharacteristic = mTargetCharacteristic;
                    mBluetoothLeService.setCharacteristicIndication(mIndicateCharacteristic, true);
                    mBluetoothLeService.readCharacteristic(mIndicateCharacteristic); //read once to start indicate?
                    mIndicate = true;
                } else {
                    // Disable Indicate
                    mBluetoothLeService.setCharacteristicNotification(mIndicateCharacteristic, false);
                    mIndicateCharacteristic = null;
                    mIndicate = false;
                }
            }

            //Start new activity
            final Intent intent = new Intent(DeviceControlActivity.this, AcetrackReadyIdle.class);
            startActivity(intent);
        }
        //-----------End of Indicate/Write--------
    }


    // Code to manage Service lifecycle.
    private final ServiceConnection mServiceConnection = new ServiceConnection() {

        @Override
        public void onServiceConnected(ComponentName componentName, IBinder service) {
            mBluetoothLeService = ((BluetoothLeService.LocalBinder) service).getService();

            if (!mBluetoothLeService.initialize()) {
                Log.e(TAG, "Unable to initialize Bluetooth");
                finish();
            }
            //automatically connects to the device upon successful start-up initialization.
            mBluetoothLeService.connect(mDeviceAddress);
        }

        @Override
        public void onServiceDisconnected(ComponentName componentName) {
            mBluetoothLeService = null;
        }
    };


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
            if (action.equals(BluetoothLeService.ACTION_GATT_CONNECTED)) {
                mConnected = true;
            } else if (action.equals(BluetoothLeService.ACTION_GATT_DISCONNECTED)) {
                mConnected = false;
                mTargetCharacteristic = null;
            } else if (action.equals(BluetoothLeService.ACTION_GATT_SERVICES_DISCOVERED)) {
                // Show all the supported services and characteristics on the user interface.
                displayGattServices(mBluetoothLeService.getSupportedGattServices());
            } else if (action.equals(DeviceScanActivity.DEVICE_DATA_AVAILABLE)) {
                mDeviceAddress = intent.getStringExtra(EXTRAS_DEVICE_ADDRESS);
            } else if (action.equals(BluetoothLeService.ACTION_DATA_AVAILABLE)) {
                //displayRxData(intent.getByteArrayExtra(BluetoothLeService.EXTRA_DATA));
            }
            Log.d(TAG, "BroadcastReceiver.onReceive():action="+action);
        }
    };


    private static IntentFilter makeGattUpdateIntentFilter() {
        final IntentFilter intentFilter = new IntentFilter();
        intentFilter.addAction(BluetoothLeService.ACTION_GATT_CONNECTED);
        intentFilter.addAction(BluetoothLeService.ACTION_GATT_DISCONNECTED);
        intentFilter.addAction(BluetoothLeService.ACTION_GATT_SERVICES_DISCOVERED);
        intentFilter.addAction(DeviceScanActivity.DEVICE_DATA_AVAILABLE);
        intentFilter.addAction(BluetoothLeService.ACTION_DATA_AVAILABLE);
        return intentFilter;
    }
}
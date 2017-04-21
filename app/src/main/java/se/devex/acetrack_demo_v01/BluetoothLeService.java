package se.devex.acetrack_demo_v01;

import android.app.Service;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothGattCallback;
import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothGattDescriptor;
import android.bluetooth.BluetoothGattService;
import android.bluetooth.BluetoothManager;
import android.bluetooth.BluetoothProfile;
import android.content.Context;
import android.content.Intent;
import android.os.Binder;
import android.os.Handler;
import android.os.IBinder;
import android.util.Log;
import android.widget.Toast;

import java.util.Arrays;
import java.util.List;
import java.util.Timer;
import java.util.TimerTask;

/**
 * Source: https://developer.android.com/samples/BluetoothLeGatt/src/com.example.android.bluetoothlegatt/BluetoothLeService.html
 */

/**
 * Service for managing connection and data communication with a GATT server hosted on a
 * given Bluetooth LE device.
 */
public class BluetoothLeService extends Service {
    private final static String TAG = BluetoothLeService.class.getSimpleName();

    private BluetoothManager mBluetoothManager;
    private BluetoothAdapter mBluetoothAdapter;
    private String mBluetoothDeviceAddress;
    public  BluetoothGatt mBluetoothGatt;
    private int mConnectionState = STATE_DISCONNECTED;

    private static final int STATE_DISCONNECTED = 0;
    private static final int STATE_CONNECTING = 1;
    private static final int STATE_CONNECTED = 2;

    public final static String ACTION_GATT_CONNECTED =
            "Acetrack.ACTION_GATT_CONNECTED";
    public final static String ACTION_GATT_DISCONNECTED =
            "Acetrack.ACTION_GATT_DISCONNECTED";
    public final static String ACTION_GATT_SERVICES_DISCOVERED =
            "Acetrack.ACTION_GATT_SERVICES_DISCOVERED";
    public final static String ACTION_DATA_AVAILABLE =
            "Acetrack.ACTION_DATA_AVAILABLE";
    public final static String RSSI_DATA_AVAILABLE =
            "Acetrack.RSSI_DATA_AVAILABLE";
    public final static String EXTRA_DATA =
            "Acetrack.EXTRA_DATA";

    // Implements callback methods for GATT events that the app cares about.  For example,
    // connection change and services discovered.
    private Timer mRssiTimer;
    private boolean IsTimerShouldCancel;

    // for Dual Mode BLE module
    public static boolean IsDualMode =  false;    // BM77
    private static Handler mTimeoutHandler=null;
    private static Runnable mTimeoutRunnable=null;
    private final static int mDualModeDelay = 5000;

    //Long Packet
    static int packetStartPos = 0;
    static int packetLength = 0;
    byte[] packetPayload;


    public final BluetoothGattCallback mGattCallback = new BluetoothGattCallback() {
        @Override
        public void onConnectionStateChange(BluetoothGatt gatt, int status, int newState) {
            String intentAction;
            if (newState == BluetoothProfile.STATE_CONNECTED) {
                intentAction = ACTION_GATT_CONNECTED;
                mConnectionState = STATE_CONNECTED;
                broadcastUpdate(intentAction);
                Log.i(TAG, "Connected to GATT server.");

                // Attempts to discover services after successful connection.
                Boolean discoverResult = mBluetoothGatt.discoverServices();
                Log.i(TAG, "Start service discovery result: " + discoverResult);

                // Create Timer task to poll rssi
                IsTimerShouldCancel=false;
                TimerTask task = new TimerTask() {
                    @Override
                    public void run()
                    {
                        if (IsTimerShouldCancel) {
                            mRssiTimer.cancel();
                        } else {
                            mBluetoothGatt.readRemoteRssi();
                        }
                    }
                };
                mRssiTimer = new Timer();
                mRssiTimer.schedule(task, 1000, 1000);

            } else if (newState == BluetoothProfile.STATE_DISCONNECTED) {
                intentAction = ACTION_GATT_DISCONNECTED;
                mConnectionState = STATE_DISCONNECTED;
                Log.i(TAG, "Disconnected from GATT server.");
                broadcastUpdate(intentAction);

                // Cancel timer
                IsTimerShouldCancel=true;
            }
        }

        @Override
        public void onServicesDiscovered(BluetoothGatt gatt, int status) {
            if (status == BluetoothGatt.GATT_SUCCESS) {
                Log.d(TAG, "onServicesDiscovered success");
                if (IsDualMode) {
                    Log.d(TAG, "Dual Mode Services Discovered");
                    if (mTimeoutHandler!=null) {
                        mTimeoutHandler.removeCallbacks(mTimeoutRunnable);
                        mTimeoutRunnable = null;
                        mTimeoutHandler = null;
                    }
                } else {
                    // nothing
                }
                broadcastUpdate(ACTION_GATT_SERVICES_DISCOVERED);
            } else {
                Log.d(TAG, "onServicesDiscovered received: " + status);
            }
        }

        @Override
        public void onCharacteristicRead(BluetoothGatt gatt,
                                         BluetoothGattCharacteristic characteristic,
                                         int status) {
            if (status == BluetoothGatt.GATT_SUCCESS) {
                broadcastUpdate(ACTION_DATA_AVAILABLE, characteristic);
            }
        }

        @Override
        public void onCharacteristicWrite(BluetoothGatt gatt, BluetoothGattCharacteristic characteristic, int status) {
            //After writing the enable flag, next we read the initial value
            if (status == BluetoothGatt.GATT_SUCCESS) {
               //Continue to Write
                String payloadString = new String(packetPayload);
                Log.d(TAG," Packet payload= "+payloadString);
                packetStartPos += 20;
                if (packetStartPos<packetPayload.length) {
                    packetLength = (packetPayload.length - packetStartPos > 20) ? 20 : packetPayload.length - packetStartPos;
                    Log.d(TAG, "Total Length = " + packetPayload.length + ", Packet Length = " + packetLength);
                    byte[] packetBuffer = Arrays.copyOfRange(packetPayload, packetStartPos, packetStartPos+packetLength);
                     String packetString = new String(packetBuffer);
                     Log.d(TAG," Packet = "+packetString);
                     characteristic.setValue(packetBuffer);
                     boolean writeStatus = mBluetoothGatt.writeCharacteristic(characteristic);
                     Log.d(TAG, " Packet write  status=" + writeStatus);
                } else {
                    Log.d(TAG, " Packet Write Complete");
                }
            }
        }

        @Override
        public void onCharacteristicChanged(BluetoothGatt gatt,
                                            BluetoothGattCharacteristic characteristic) {
            broadcastUpdate(ACTION_DATA_AVAILABLE, characteristic);
        }

        @Override
        public void onReadRemoteRssi(BluetoothGatt gatt, int rssi, int status){
            Log.d("onReadRemoteRss()",String.format("rssi=%d, status=%d",rssi, status));
            super.onReadRemoteRssi(gatt, rssi, status);
            broadcastRSSIUpdate(RSSI_DATA_AVAILABLE,  rssi);
        }
    };


    private void broadcastUpdate(final String action) {
        final Intent intent = new Intent(action);
        sendBroadcast(intent);
    }


    private void broadcastRSSIUpdate(final String action, int Value)
    {
        Log.d(TAG, String.format("broadcastRSSIUpdate Received Value=%d", Value));

        final Intent intent = new Intent(action);
        intent.putExtra(EXTRA_DATA, String.valueOf(Value));
        sendBroadcast(intent);
    }


    private void broadcastUpdate(final String action,
                                 final BluetoothGattCharacteristic characteristic) {
        final Intent intent = new Intent(action);

        final byte[] data = characteristic.getValue();
        if (data != null && data.length > 0) {
            intent.putExtra(EXTRA_DATA, data);
        }

        sendBroadcast(intent);
    }


    public class LocalBinder extends Binder {
        BluetoothLeService getService() {
            return BluetoothLeService.this;
        }
    }


    @Override
    public IBinder onBind(Intent intent) {
        return mBinder;
    }


    @Override
    public boolean onUnbind(Intent intent) {
        // After using a given device, you should make sure that BluetoothGatt.close() is called
        // such that resources are cleaned up properly.  In this particular example, close() is
        // invoked when the UI is disconnected from the Service.
        close();
        return super.onUnbind(intent);
    }


    private final IBinder mBinder = new LocalBinder();


    /**
     * Initializes a reference to the local Bluetooth adapter.
     *
     * @return Return true if the initialization is successful.
     */
    public boolean initialize() {
        // For API level 18 and above, get a reference to BluetoothAdapter through BluetoothManager.
        if (mBluetoothManager == null) {
            mBluetoothManager = (BluetoothManager) getSystemService(Context.BLUETOOTH_SERVICE);
            if (mBluetoothManager == null) {
                Log.e(TAG, "Unable to initialize BluetoothManager.");
                return false;
            }
        }

        mBluetoothAdapter = mBluetoothManager.getAdapter();
        if (mBluetoothAdapter == null) {
            Log.e(TAG, "Unable to obtain a BluetoothAdapter.");
            return false;
        }
        return true;
    }


    /**
     * Connects to the GATT server hosted on the Bluetooth LE device.
     *
     * @param address The device address of the destination device.
     *
     * @return Return true if the connection is initiated successfully. The connection result
     *         is reported asynchronously through the
     *         {@code BluetoothGattCallback#onConnectionStateChange(android.bluetooth.BluetoothGatt, int, int)}
     *         callback.
     */
    public boolean connect(final String address) {
        if (mBluetoothAdapter == null || address == null) {
            Log.w(TAG, "BluetoothAdapter not initialized or unspecified address.");
            return false;
        }

        // Previously connected device.  Try to reconnect.
        if (mBluetoothDeviceAddress != null && address.equals(mBluetoothDeviceAddress)
                && mBluetoothGatt != null) {
            Log.d(TAG, "Trying to use an existing mBluetoothGatt for connection.");
            if (mBluetoothGatt.connect()) {
                mConnectionState = STATE_CONNECTING;
                return true;
            } else {
                return false;
            }
        }

        final BluetoothDevice device = mBluetoothAdapter.getRemoteDevice(address);
        if (device == null) {
            Log.w(TAG, "Device not found.  Unable to connect.");
            return false;
        }

        if (IsDualMode) {
            Log.d(TAG,"try connect Dual Mode");
            mBluetoothGatt = device.connectGatt(this, true, mGattCallback);  // for BM77 dual mode, autoconnect=true
            CreateTimeoutHandler();
        } else {
            // We want to directly connect to the device, so we are setting the autoConnect parameter to false.
            mBluetoothGatt = device.connectGatt(this, false, mGattCallback);
        }

        //mBluetoothGatt = device.connectGatt(this, true, mGattCallback); //for auto connect mode true
        Log.d(TAG, "Trying to create a new connection.");
        mBluetoothDeviceAddress = address;
        mConnectionState = STATE_CONNECTING;
        return true;
    }


    void CreateTimeoutHandler() {
        mTimeoutHandler = new Handler();
        mTimeoutRunnable = new Runnable() {
            @Override
            public void run() {
                Toast.makeText(getApplication(), "Try Connecting BLE", Toast.LENGTH_LONG).show();
                mBluetoothGatt.disconnect();
                Log.d(TAG, "run Dual Mode disconnect");
                mBluetoothGatt.connect();
                Log.d(TAG, "run Dual Mode connect");
            }
        };
        mTimeoutHandler.postDelayed(mTimeoutRunnable, mDualModeDelay);
    }


    /**
     * Disconnects an existing connection or cancel a pending connection. The disconnection result
     * is reported asynchronously through the
     * {@code BluetoothGattCallback#onConnectionStateChange(android.bluetooth.BluetoothGatt, int, int)}
     * callback.
     */
    public void disconnect() {
        Log.w(TAG, "BluetoothLeService.disconnect()");
        if (mBluetoothAdapter == null || mBluetoothGatt == null) {
            Log.w(TAG, "BluetoothAdapter not initialized");
            return;
        }
        mBluetoothGatt.disconnect();
        IsTimerShouldCancel=true;
    }


    /**
     * After using a given BLE device, the app must call this method to ensure resources are
     * released properly.
     */
    public void close() {
        Log.w(TAG, "BluetoothLeService.close()");
        if (mBluetoothGatt == null) {
            return;
        }
        mBluetoothGatt.disconnect(); // BLE is more stable ? see  http://stackoverflow.com/questions/17870189/android-4-3-bluetooth-low-energy-unstable
        mBluetoothGatt.close();  //original
        mBluetoothGatt = null;
        // Cancel timer
        IsTimerShouldCancel=true;
    }


    /**
     * Request a read on a given {@code BluetoothGattCharacteristic}. The read result is reported
     * asynchronously through the {@code BluetoothGattCallback#onCharacteristicRead(android.bluetooth.BluetoothGatt, android.bluetooth.BluetoothGattCharacteristic, int)}
     * callback.
     *
     * @param characteristic The characteristic to read from.
     */
    public void readCharacteristic(BluetoothGattCharacteristic characteristic) {
        Log.w(TAG, "BluetoothLeService.readCharacteristic()");
        if (mBluetoothAdapter == null || mBluetoothGatt == null) {
            Log.w(TAG, "BluetoothAdapter not initialized");
            return;
        }
        mBluetoothGatt.readCharacteristic(characteristic);
    }


    /**
     * Enables or disables notification on a give characteristic.
     *
     * @param characteristic Characteristic to act on.
     * @param enabled If true, enable notification.  False otherwise.
     */
    public void setCharacteristicNotification(BluetoothGattCharacteristic characteristic,
                                              boolean enabled) {
        Log.w(TAG, "BluetoothLeService.setCharacteristicNotification()");
        if (mBluetoothAdapter == null || mBluetoothGatt == null) {
            Log.w(TAG, "BluetoothAdapter not initialized");
            return;
        }
        mBluetoothGatt.setCharacteristicNotification(characteristic, enabled);

        BluetoothGattDescriptor descriptor = characteristic.getDescriptor(GattAttributes.UUID_CLIENT_CHARACTERISTIC_CONFIG);

        final StringBuilder strEnableNotifyValue = new DataManager().byteArrayToHex(BluetoothGattDescriptor.ENABLE_NOTIFICATION_VALUE);
        Log.d(TAG, String.format("enable notify value : %s ",strEnableNotifyValue));
        descriptor.setValue(BluetoothGattDescriptor.ENABLE_NOTIFICATION_VALUE);
        mBluetoothGatt.writeDescriptor(descriptor);
    }


    public void setCharacteristicIndication(BluetoothGattCharacteristic characteristic,
                                              boolean enabled) {
        Log.w(TAG, "BluetoothLeService.setCharacteristicIndication()");
        if (mBluetoothAdapter == null || mBluetoothGatt == null) {
            Log.w(TAG, "BluetoothAdapter not initialized");
            return;
        }
        mBluetoothGatt.setCharacteristicNotification(characteristic, enabled);

        BluetoothGattDescriptor descriptor = characteristic.getDescriptor(GattAttributes.UUID_CLIENT_CHARACTERISTIC_CONFIG);

        final StringBuilder strEnableNotifyValue = new DataManager().byteArrayToHex(BluetoothGattDescriptor.ENABLE_INDICATION_VALUE);
        Log.d(TAG, String.format("enable indication value : %s ",strEnableNotifyValue));

        descriptor.setValue(BluetoothGattDescriptor.ENABLE_INDICATION_VALUE);

        mBluetoothGatt.writeDescriptor(descriptor);
        //}
    }


    //writeCharacteristic(): General Read/Write
    public void writeCharacteristic(BluetoothGattCharacteristic characteristic, String value)
    {
        DeviceControlActivity.mTargetCharacteristic.setValue(String.valueOf(value));
        boolean writeStatus = mBluetoothGatt.writeCharacteristic(characteristic);
        Log.d(TAG, "write status = " + writeStatus);
    }


    /**
     * Retrieves a list of supported GATT services on the connected device. This should be
     * invoked only after {@code BluetoothGatt#discoverServices()} completes successfully.
     *
     * @return A {@code List} of supported services.
     */
    public List<BluetoothGattService> getSupportedGattServices() {
        Log.w(TAG, "BluetoothLeService.getSupportedGattServices()");
        if (mBluetoothGatt == null) return null;

        return mBluetoothGatt.getServices();
    }
}

package se.devex.acetrack_demo_v01;

import android.app.Activity;
import android.app.ListActivity;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothManager;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.os.Handler;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import java.util.ArrayList;

/**
 * Source: https://developer.android.com/samples/BluetoothLeGatt/src/com.example.android.bluetoothlegatt/DeviceScanActivity.html
 */

/**
 * Activity for scanning and displaying available Bluetooth LE devices.
 */
public class DeviceScanActivity extends ListActivity {
    private final static String TAG = DeviceScanActivity.class.getSimpleName();

    public final static String DEVICE_DATA_AVAILABLE =
            "se.devex.acetrack_demo_v01.DEVICE_DATA_AVAILABLE";

    class deviceInfo {
        public String Name;
        public String Address;
        public Integer RSSI;
        public int Type,BondState;
        public byte[] scanRecord;
    }
    private final static int MaxDeviceCount = 500; //Support Max 500 Bluetooth devices
    private LeDeviceListAdapter mLeDeviceListAdapter;
    private BluetoothAdapter mBluetoothAdapter;
    private BluetoothDevice mBluetoothDevice;
    private boolean mScanning;
    private Handler mHandler;

    private static final int REQUEST_ENABLE_BT = 1;
    // Stops scanning after 5 seconds.
    private static final long SCAN_PERIOD = 5000;

    //SYHO START
    private deviceInfo[] scanDevice=new deviceInfo[MaxDeviceCount];
    private Integer scanIndex=0;
    //SYHO END

    @Override
    public void onCreate(Bundle savedInstanceState) {
        Log.d(TAG, "*** onCreate()");
        super.onCreate(savedInstanceState);
        getActionBar().setTitle(R.string.title_activity_device_scan);
        mHandler = new Handler();

        // Use this check to determine whether BLE is supported on the device.  Then you can
        // selectively disable BLE-related features.
        if (!getPackageManager().hasSystemFeature(PackageManager.FEATURE_BLUETOOTH_LE)) {
            Toast.makeText(this, R.string.ble_not_supported, Toast.LENGTH_SHORT).show();
            finish();
        }

        // Initializes a Bluetooth adapter.  For API level 18 and above, get a reference to
        // BluetoothAdapter through BluetoothManager.
        final BluetoothManager bluetoothManager =  (BluetoothManager) getSystemService(Context.BLUETOOTH_SERVICE);
        mBluetoothAdapter = bluetoothManager.getAdapter();

        // Checks if Bluetooth is supported on the device.
        if (mBluetoothAdapter == null) {
            Toast.makeText(this, R.string.error_bluetooth_not_supported, Toast.LENGTH_SHORT).show();
            finish();
            return;
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        Log.d(TAG, "*** onCreateOptionsMenu()");
        getMenuInflater().inflate(R.menu.device_scan, menu);
        if (!mScanning) {
            menu.findItem(R.id.menu_stop).setVisible(false);
            menu.findItem(R.id.menu_scan).setVisible(true);
            menu.findItem(R.id.menu_refresh).setActionView(null);
        } else {
            menu.findItem(R.id.menu_stop).setVisible(true);
            menu.findItem(R.id.menu_scan).setVisible(false);
            menu.findItem(R.id.menu_refresh).setActionView(
            R.layout.actionbar_indeterminate_progress);
        }
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        Log.d(TAG, "*** onOptionsItemSelected()");
        switch (item.getItemId()) {
            case R.id.menu_scan:
                scanIndex = 0; //reset
                mLeDeviceListAdapter.clear();
                scanLeDevice(true);
                break;
            case R.id.menu_stop:
                scanLeDevice(false);
                break;
        }
        return true;
    }

    @Override
    protected void onResume() {
        Log.d(TAG, "*** onResume()");
        super.onResume();

        // Ensures Bluetooth is enabled on the device.  If Bluetooth is not currently enabled,
        // fire an intent to display a dialog asking the user to grant permission to enable it.
        if (!mBluetoothAdapter.isEnabled()) {
            if (!mBluetoothAdapter.isEnabled()) {
                Intent enableBtIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
                startActivityForResult(enableBtIntent, REQUEST_ENABLE_BT);
            }
        }

        // Initializes list view adapter.
        mLeDeviceListAdapter = new LeDeviceListAdapter();
        setListAdapter(mLeDeviceListAdapter);
        Log.d(TAG, "LeDeviceListAdapter Created");

        scanLeDevice(true);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        Log.d(TAG, "*** onActivityResult()");
        // User chose not to enable Bluetooth.
        if (requestCode == REQUEST_ENABLE_BT && resultCode == Activity.RESULT_CANCELED) {
            finish();
            return;
        }
        super.onActivityResult(requestCode, resultCode, data);
    }

    @Override
    protected void onPause() {
        Log.d(TAG, "*** onPause()");
        super.onPause();
        scanLeDevice(false);
        mLeDeviceListAdapter.clear();
    }

    @Override
    protected void onListItemClick(ListView l, View v, int position, long id) {
        Log.d(TAG, String.format("*** onListItemClick() position %d",position));

        mBluetoothDevice = mLeDeviceListAdapter.getDevice(position);
        if (mBluetoothDevice == null) return;
        //final Intent intent = new Intent(this, DeviceControlActivity.class);
        //intent.putExtra(DeviceControlActivity.EXTRAS_DEVICE_NAME, device.getName());
        //intent.putExtra(DeviceControlActivity.EXTRAS_DEVICE_ADDRESS, device.getAddress());
        if (mScanning) {
            mBluetoothAdapter.stopLeScan(mLeScanCallback);
            mScanning = false;
        }

        BluetoothLeService.IsDualMode = (scanDevice[position].Type == BluetoothDevice.DEVICE_TYPE_DUAL) ? true : false; //Select Single or Dual Mode
        if (BluetoothLeService.IsDualMode) {
            Toast.makeText(getApplication(), "Dual Mode", Toast.LENGTH_LONG).show();
        }
        final Intent intent = new Intent(DeviceScanActivity.this, DeviceControlActivity.class);
        //final Intent intent = new Intent(DeviceScanActivity.this, AcetrackReadyIdle.class);
        intent.putExtra(DeviceControlActivity.EXTRAS_DEVICE_NAME, mBluetoothDevice.getName());
        intent.putExtra(DeviceControlActivity.EXTRAS_DEVICE_ADDRESS, mBluetoothDevice.getAddress());
        startActivity(intent);
    }

    private void scanLeDevice(final boolean enable) {
        Log.d(TAG, "*** scanLeDevice()");
        if (enable) {
            // Stops scanning after a pre-defined scan period.
            mHandler.postDelayed(new Runnable() {
                @Override
                public void run() {
                    mScanning = false;
                    mBluetoothAdapter.stopLeScan(mLeScanCallback);
                    invalidateOptionsMenu();
                }
            }, SCAN_PERIOD);

            mScanning = true;
            mBluetoothAdapter.startLeScan(mLeScanCallback);
        } else {
            mScanning = false;
            mBluetoothAdapter.stopLeScan(mLeScanCallback);
        }
        invalidateOptionsMenu();
    }

    // Adapter for holding devices found through scanning.
    private class LeDeviceListAdapter extends BaseAdapter {
        private ArrayList<BluetoothDevice> mLeDevices;
        private LayoutInflater mInflator;

        public LeDeviceListAdapter() {
            super();
            Log.d(TAG, "*** LeDeviceListAdapter.LeDeviceListAdapter()");
            mLeDevices = new ArrayList<BluetoothDevice>();
            mInflator = DeviceScanActivity.this.getLayoutInflater();
        }

        public void addDevice(BluetoothDevice device) {
            Log.d(TAG, "*** LeDeviceListAdapter.addDevice()");
            if(!mLeDevices.contains(device)) {
                mLeDevices.add(device);
            }
        }

        public BluetoothDevice getDevice(int position) {
            Log.d(TAG, "*** LeDeviceListAdapter.getDevice()");
            return mLeDevices.get(position);
        }

        public void clear() {
            Log.d(TAG, "*** LeDeviceListAdapter.clear()");
            mLeDevices.clear();
        }

        @Override
        public int getCount() {
            Log.d(TAG, "*** LeDeviceListAdapter.getCount()");
            return mLeDevices.size();
        }

        @Override
        public Object getItem(int i) {
            Log.d(TAG, "*** LeDeviceListAdapter.getItem()");
            return mLeDevices.get(i);
        }

        @Override
        public long getItemId(int i) {
            Log.d(TAG, "*** LeDeviceListAdapter.getItemId()");
            return i;
        }

        @Override
        public View getView(int i, View view, ViewGroup viewGroup) {
            Log.d(TAG, String.format("*** LeDeviceListAdapter.getView() i=%d",i));
            ViewHolder viewHolder;
            // General ListView optimization code.
            if (view == null) {
                view = mInflator.inflate(R.layout.device_scan, null);
                viewHolder = new ViewHolder();
                viewHolder.deviceAddress = (TextView) view.findViewById(R.id.device_address);
                viewHolder.deviceName = (TextView) view.findViewById(R.id.device_name);
                viewHolder.deviceRSSI = (TextView) view.findViewById(R.id.scan_RSSI);
                viewHolder.deviceType = (TextView) view.findViewById(R.id.device_Type);
                viewHolder.deviceBoundState = (TextView) view.findViewById(R.id.device_BoundState);
                view.setTag(viewHolder);
            } else {
                viewHolder = (ViewHolder) view.getTag();
            }

            viewHolder.deviceName.setText(scanDevice[i].Name);
            viewHolder.deviceAddress.setText(scanDevice[i].Address);
            viewHolder.deviceRSSI.setText(String.format("%d dBm",scanDevice[i].RSSI));

            switch (scanDevice[i].Type) {
                case BluetoothDevice.DEVICE_TYPE_CLASSIC:
                    viewHolder.deviceType.setText("Classic");
                    break;
                case BluetoothDevice.DEVICE_TYPE_LE:
                    viewHolder.deviceType.setText("BLE");
                    break;
                case BluetoothDevice.DEVICE_TYPE_DUAL:
                    viewHolder.deviceType.setText("Dual");
                    break;
                case BluetoothDevice.DEVICE_TYPE_UNKNOWN:
                    viewHolder.deviceType.setText("Ukonown");
                    break;
                default:
                    viewHolder.deviceType.setText("Not defined");
                    break;
            }

            switch (scanDevice[i].BondState) {
                case BluetoothDevice.BOND_NONE:
                    viewHolder.deviceBoundState.setText("BondNone");
                    break;
                case BluetoothDevice.BOND_BONDED:
                    viewHolder.deviceBoundState.setText("Bonded");
                    break;
                case BluetoothDevice.BOND_BONDING:
                    viewHolder.deviceBoundState.setText("Bonding");
                    break;
            }
            return view;
        }
    }

    // Device scan callback.
    private BluetoothAdapter.LeScanCallback mLeScanCallback =
            new BluetoothAdapter.LeScanCallback() {

                @Override
                public void onLeScan(final BluetoothDevice device, int rssi, byte[] scanRecord) {
                    Log.d(TAG,  String.format("*** BluetoothAdapter.LeScanCallback.onLeScan(): *RSSI=%d",rssi));

                    final String deviceName = device.getName();
                    scanDevice[scanIndex]= new deviceInfo();
                    if (deviceName != null && deviceName.length() > 0) {
                        scanDevice[scanIndex].Name = deviceName;
                    }  else {
                        scanDevice[scanIndex].Name = "unknown device";
                    }
                    scanDevice[scanIndex].Address = device.getAddress();
                    scanDevice[scanIndex].RSSI = rssi;
                    scanDevice[scanIndex].Type = device.getType();
                    scanDevice[scanIndex].BondState = device.getBondState();

                    // Search for actual packet length
                    int packetLength=0;
                    while (scanRecord[packetLength]>0 && packetLength<scanRecord.length) {
                        packetLength += scanRecord[packetLength]+1;
                    }
                    scanDevice[scanIndex].scanRecord = new byte[packetLength];
                    System.arraycopy (scanRecord,0,scanDevice[scanIndex].scanRecord,0,packetLength);
                    Log.d(TAG, String.format("*** Scan Index=%d, Name=%s, Address=%s, RSSI=%d, scan result length=%d",
                            scanIndex, scanDevice[scanIndex].Name,scanDevice[scanIndex].Address,scanDevice[scanIndex].RSSI, packetLength ));
                    scanIndex++;
                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            Log.d(TAG, "*** BluetoothAdapter.LeScanCallback.runOnUiThread()");
                            mLeDeviceListAdapter.addDevice(device);
                            mLeDeviceListAdapter.notifyDataSetChanged();
                        }
                    });
                }
            };

    static class ViewHolder {
        TextView deviceName;
        TextView deviceAddress;
        TextView deviceRSSI;
        TextView deviceType;
        TextView deviceBoundState;
    }
}
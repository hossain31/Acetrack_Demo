package se.devex.acetrack_demo_v01;

/**
 * Source: https://developer.android.com/samples/BluetoothLeGatt/src/com.example.android.bluetoothlegatt/SampleGattAttributes.html
 */

import java.util.HashMap;
import java.util.UUID;

/**
 * This class includes a small subset of standard GATT attributes for demonstration purposes.
 */
public class GattAttributes {
    private final static String TAG = GattAttributes.class.getSimpleName();

    private static HashMap<String, String> attributes = new HashMap();

    public static UUID UUID_CLIENT_CHARACTERISTIC_CONFIG = UUID.fromString("00002902-0000-1000-8000-00805f9b34fb");

    public static String lookup(String uuid, String defaultName) {
        String name = attributes.get(uuid);
        return name == null ? defaultName : name;
    }

    static {
        // Sample Services.
        attributes.put("00001800-0000-1000-8000-00805f9b34fb", "Generic Access");
        attributes.put("00001801-0000-1000-8000-00805f9b34fb", "Generic Attribute");

        //Bluegiga BLE device
        attributes.put("af20fbac-2518-4998-9af7-af42540731b3", "Read_Write");
        attributes.put("1d5688de-866d-3aa4-ec46-a1bddb37ecf6", "Bluegiga_Service");
    }
}

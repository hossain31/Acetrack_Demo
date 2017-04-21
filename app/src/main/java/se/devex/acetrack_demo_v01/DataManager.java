package se.devex.acetrack_demo_v01;

import android.util.Log;

public class DataManager {
    private final static String TAG = DataManager.class.getSimpleName();

    public StringBuilder byteArrayToHex(byte data[]) {
        Log.d(TAG, "dumpHex()");
        final StringBuilder strResult = new StringBuilder();
        for (int i=0; i<data.length; i++)
            strResult.append(String.format("%02X ", data[i]));
        return strResult;
    }
}

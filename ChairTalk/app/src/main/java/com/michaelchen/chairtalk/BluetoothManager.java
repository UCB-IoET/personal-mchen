package com.michaelchen.chairtalk;

import android.app.Activity;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothGattService;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.ServiceConnection;
import android.os.IBinder;
import android.util.Log;

import java.util.List;

/**
 * Created by michael on 4/23/15.
 */
class BluetoothManager {

    private BluetoothLeService mBluetoothLeService;

    private BluetoothGattCharacteristic characteristic;
    private String mDeviceAddress;
    private MainActivity activity;
    private boolean mConnected;
    public final String TAG = "BluetoothManager";
    public static final String SERV_UUID = "0000ffe0-0000-1000-8000-00805f9b34fb";
    public static final String CHAR_UUID = "0000ffe1-0000-1000-8000-00805f9b34fb";


    private final ServiceConnection mServiceConnection = new ServiceConnection() {
        @Override
        public void onServiceConnected(ComponentName componentName, IBinder service) {
            mBluetoothLeService = ((BluetoothLeService.LocalBinder) service).getService();
            if (!mBluetoothLeService.initialize()) {
                Log.d("MainAct initBLE", "Unable to initialize Bluetooth");
            }
            // Automatically connects to the device upon successful start-up initialization.
            mBluetoothLeService.connect(mDeviceAddress);
        }
        @Override
        public void onServiceDisconnected(ComponentName componentName) {
            mBluetoothLeService = null;
        }
    };

    private final BroadcastReceiver mGattUpdateReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            final String action = intent.getAction();
            if (BluetoothLeService.ACTION_GATT_CONNECTED.equals(action)) {
                mConnected = true;
            } else if (BluetoothLeService.ACTION_GATT_DISCONNECTED.equals(action)) {
                mConnected = false;
                activity.disconnectBluetoothManager();
            } else if (BluetoothLeService.ACTION_DATA_AVAILABLE.equals(action)) {
                readData(intent.getByteArrayExtra(BluetoothLeService.EXTRA_DATA));
            } else if (BluetoothLeService.ACTION_GATT_SERVICES_DISCOVERED.equals(action)) {
                initBle();
            }
        }
    };

    BluetoothManager(MainActivity activity, String deviceAddress) {
        this.activity = activity;
        mDeviceAddress = deviceAddress;
        Intent gattServiceIntent = new Intent(activity, BluetoothLeService.class);
        activity.bindService(gattServiceIntent, mServiceConnection, activity.BIND_AUTO_CREATE);
        onResume();
    }

    private void initBle() {
        if (mBluetoothLeService != null) {
            String uuid;
            List<BluetoothGattService> services = mBluetoothLeService.getSupportedGattServices();
            for (BluetoothGattService service : services) {
                uuid = service.getUuid().toString();
                if (uuid.equals(SERV_UUID)) {
                    List<BluetoothGattCharacteristic> gattCharacteristics =
                            service.getCharacteristics();
                    for(BluetoothGattCharacteristic characteristic: gattCharacteristics) {
                        String charUuid = characteristic.getUuid().toString();
                        if (charUuid.equals(CHAR_UUID)) {
                            this.characteristic = characteristic;
                            startNotifications();
                            return;
                        }
                    }
                }
            }
        }
    }

    void startNotifications() {
        if (characteristic != null && mBluetoothLeService != null) {
            mBluetoothLeService.setCharacteristicNotification(characteristic, true);
        }
    }

    void onResume() {
        activity.registerReceiver(mGattUpdateReceiver, makeGattUpdateIntentFilter());
        if (mBluetoothLeService != null) {
            final boolean result = mBluetoothLeService.connect(mDeviceAddress);
            Log.d(TAG, "Connect request result=" + result);
        }
    }

    void onDestroy() {
        activity.unbindService(mServiceConnection);
        mBluetoothLeService = null;
    }

    void onPause() {
        activity.unregisterReceiver(mGattUpdateReceiver);
    }

    void readData(byte[] result) {
        // TODO: hand result to MainActivity
        Log.e(TAG, "read: " + result);
        activity.setBleStatus(result);
    }

    void writeData(byte[] data) {
        if (characteristic == null) {
            return;
        }

        characteristic.setValue(data);
        mBluetoothLeService.writeCharacteristic(characteristic);
    }


    private static IntentFilter makeGattUpdateIntentFilter() {
        final IntentFilter intentFilter = new IntentFilter();
        intentFilter.addAction(BluetoothLeService.ACTION_GATT_CONNECTED);
        intentFilter.addAction(BluetoothLeService.ACTION_GATT_DISCONNECTED);
        intentFilter.addAction(BluetoothLeService.ACTION_GATT_SERVICES_DISCOVERED);
        intentFilter.addAction(BluetoothLeService.ACTION_DATA_AVAILABLE);
        return intentFilter;
    }

    void disconnect() {
        onDestroy();
        characteristic = null;
    }
}

package com.michaelchen.chairtalk;

import android.bluetooth.BluetoothDevice;

import java.util.List;
import java.util.UUID;

/**
 * Created by michael on 4/22/15.
 */

public class BleAdvertisedData {
    private List<UUID> mUuids;
    private String mName;
    private BluetoothDevice device;
    public BleAdvertisedData(List<UUID> uuids, String name, BluetoothDevice device){
        mUuids = uuids;
        mName = name;
        this.device = device;
    }

    public BleAdvertisedData(List<UUID> uuids, String name){
        mUuids = uuids;
        mName = name;
    }

    public List<UUID> getUuids(){
        return mUuids;
    }

    public String getName(){
        return mName;
    }

    public BluetoothDevice getDevice() { return device; }

    void setDevice(BluetoothDevice device) {this.device = device;}

    public String getSimpleName() {
        if (device == null || device.getName() == null) {
            return mName;
        }
        return device.getName();
    }
}

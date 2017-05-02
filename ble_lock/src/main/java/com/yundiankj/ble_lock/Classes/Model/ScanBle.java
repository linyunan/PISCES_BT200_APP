package com.yundiankj.ble_lock.Classes.Model;

import android.bluetooth.BluetoothDevice;

import java.util.List;

/**
 * Created by hong on 2016/9/5.
 */
public class ScanBle {

    private List<BluetoothDevice> bluetoothDeviceList;
    private String ble_name;
    private int ble_rssi;

    public List<BluetoothDevice> getBluetoothDeviceList() {
        return bluetoothDeviceList;
    }

    public void setBluetoothDeviceList(List<BluetoothDevice> bluetoothDeviceList) {
        this.bluetoothDeviceList = bluetoothDeviceList;
    }

    public String getBle_name() {
        return ble_name;
    }

    public void setBle_name(String ble_name) {
        this.ble_name = ble_name;
    }

    public int getBle_rssi() {
        return ble_rssi;
    }

    public void setBle_rssi(int ble_rssi) {
        this.ble_rssi = ble_rssi;
    }
}

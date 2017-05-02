/*
 * Copyright 2015 Junk Chen
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.yundiankj.ble_lock.Resource;

import android.Manifest;
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
import android.content.pm.PackageManager;
import android.media.MediaPlayer;
import android.os.Binder;
import android.os.Build;
import android.os.Handler;
import android.os.IBinder;
import android.util.Log;
import android.widget.Toast;

import com.junkchen.blelib.BleListener;
import com.junkchen.blelib.Constants;
import com.junkchen.blelib.GattAttributes;
import com.yundiankj.ble_lock.Classes.Activity.MainActivity;
import com.yundiankj.ble_lock.Classes.Fragment.EquipMentFg;
import com.yundiankj.ble_lock.R;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;

/**
 * Created by JunkChen on 2015/9/11 0009.
 */
public class BleService extends Service implements Constants, BleListener {

    //Debug
    private static final String TAG = BleService.class.getName();

    //Member fields
    private BluetoothManager mBluetoothManager;
    private BluetoothAdapter mBluetoothAdapter;
    private BluetoothGatt mBluetoothGatt;
    private List<BluetoothDevice> mScanLeDeviceList;
    private boolean isScanning;
    private boolean isConnect;
    private String mBluetoothDeviceAddress;
    private int mConnState = STATE_DISCONNECTED;
    // Stop scanning after 10 seconds.
    private static final long SCAN_PERIOD = 200;
    private long mScanPeriod;

    private OnLeScanListener mOnLeScanListener;
    private OnConnectionStateChangeListener mOnConnectionStateChangeListener;
    private OnServicesDiscoveredListener mOnServicesDiscoveredListener;
    private OnDataAvailableListener mOnDataAvailableListener;
    private OnReadRemoteRssiListener mOnReadRemoteRssiListener;
    private OnMtuChangedListener mOnMtuChangedListener;

    private final IBinder mBinder = new LocalBinder();
    private static BleService instance = null;

    public String back_data = null;

    public List<String> send_phone_back_data;


    public boolean isconnect;
    //BLE回调得到的蓝牙RSSI值
    public static int BLERSSI;

    public final static UUID UUID_HEART_RATE_MEASUREMENT = UUID.fromString(GattAttributes.CHARACTERISTIC_HEART_RATE_MEASUREMENT);
    public final static String EXTRA_DATA = "com.example.bluetooth.le.EXTRA_DATA";
    public final static String ACTION_DATA_AVAILABLE = "com.example.bluetooth.le.ACTION_DATA_AVAILABLE";

    public static String model_data;

    public static String ble_connect;

    SharedPreferencesUtil sharedPreferencesUtil;
    private MediaPlayer music = null;// 播放器引用(提示音（连接和开锁）)


    public BleService() {
        instance = this;
        Log.d(TAG, "BleService initialized.");
    }


    public static BleService getInstance() {
        if (instance == null) throw new NullPointerException("BleService is not bind.");
        return instance;
    }

    @Override
    public IBinder onBind(Intent intent) {
        return mBinder;
    }

    @Override
    public boolean onUnbind(Intent intent) {
        close();
        instance = null;
        return super.onUnbind(intent);
    }

    public class LocalBinder extends Binder {
        public BleService getService() {
            return BleService.this;
        }
    }

    /**
     * Check for your device to support Ble
     *
     * @return true is support    false is not support
     */
    public boolean isSupportBle() {
        return getPackageManager().hasSystemFeature(PackageManager.FEATURE_BLUETOOTH_LE);
    }

    /**
     * Initializes a reference to the local Bluetooth adapter.
     *
     * @return If return true, the initialization is successful.
     */
    public boolean initialize() {
        sharedPreferencesUtil = SharedPreferencesUtil.getInstance(this);
        //For API level 18 and above, get a reference to BluetoothAdapter through BluetoothManager.
        if (mBluetoothManager == null) {
            mBluetoothManager = (BluetoothManager) getSystemService(Context.BLUETOOTH_SERVICE);
            if (mBluetoothManager == null) {
                Log.e(TAG, "Unable to initialize BluetoothManager.");
                return false;
            }
        }
        mBluetoothAdapter = mBluetoothManager.getAdapter();
        if (mBluetoothAdapter == null) {
            Log.e(TAG, "Unable to initialize BluetoothAdapter.");
            return false;
        }
        return true;
    }

    /**
     * Turn on or off the local Bluetooth adapter;do not use without explicit
     * user action to turn on Bluetooth.
     *
     * @param enable if open ble
     * @return if ble is open return true
     */
    public boolean enableBluetooth(boolean enable) {
        if (enable) {
            if (!mBluetoothAdapter.isEnabled()) {
                return mBluetoothAdapter.enable();
            }
            return true;
        } else {
            if (mBluetoothAdapter.isEnabled()) {
                return mBluetoothAdapter.disable();
            }
            return false;
        }
    }

    /**
     * Return true if Bluetooth is currently enabled and ready for use.
     *
     * @return true if the local adapter is turned on
     */
    public boolean isEnableBluetooth() {
        return mBluetoothAdapter.isEnabled();
    }

    /**
     * Scan Ble device.
     *
     * @param enable     If true, start scan ble device.False stop scan.
     * @param scanPeriod scan ble period time
     */
    public void scanLeDevice(final boolean enable, long scanPeriod) {
        if (isScanning) return;
        if (enable) {
            //Stop scanning after a predefined scan period.
            new Handler().postDelayed(new Runnable() {
                @Override
                public void run() {
                    isScanning = false;
                    mBluetoothAdapter.stopLeScan(mScanCallback);
                    broadcastUpdate(ACTION_SCAN_FINISHED);
                    if (mScanLeDeviceList != null) {
                        mScanLeDeviceList.clear();
                        mScanLeDeviceList = null;
                    }
//                    mBluetoothAdapter.getBluetoothLeScanner().stopScan(mLeScanCallback);
                }
            }, scanPeriod);
            if (mScanLeDeviceList == null) {
                mScanLeDeviceList = new ArrayList<>();
            }
            mScanLeDeviceList.clear();
            isScanning = true;
            mBluetoothAdapter.startLeScan(mScanCallback);
//            mBluetoothAdapter.getBluetoothLeScanner().startScan(mLeScanCallback);
        } else {
            isScanning = false;
            mBluetoothAdapter.stopLeScan(mScanCallback);
            broadcastUpdate(ACTION_SCAN_FINISHED);
            if (mScanLeDeviceList != null) {
                mScanLeDeviceList.clear();
                mScanLeDeviceList = null;
            }
//            mBluetoothAdapter.getBluetoothLeScanner().stopScan(mLeScanCallback);
        }
    }

    /**
     * Scan Ble device.
     *
     * @param enable If true, start scan ble device.False stop scan.
     */
    public void scanLeDevice(boolean enable) {
        this.scanLeDevice(enable, SCAN_PERIOD);
    }

    /**
     * If Ble is scaning return true, if not return false.
     *
     * @return ble whether scanning
     */
    public boolean isScanning() {
        return isScanning;
    }

    /**
     * Get scan ble devices
     *
     * @return scan le device list
     */
    public List<BluetoothDevice> getScanLeDevice() {
        return mScanLeDeviceList;
    }

    /**
     * Connects to the GATT server hosted on the Bluetooth LE device.
     *
     * @param address The device address of the destination device.
     * @return Return true if the connection is initiated successfully. The connection result
     * is reported asynchronously through the BluetoothGattCallback#onConnectionStateChange.
     */
    public boolean connect(final String address) {
        Log.e("url", "connect");
        if (isScanning) scanLeDevice(false);
        close();
        if (mBluetoothAdapter == null || address == null) {
            Log.w(TAG, "BluetoothAdapter not initialized or unspecified address.");
            return false;
        }
        //Previously connected device.  Try to reconnect.
        if (mBluetoothGatt != null && mBluetoothDeviceAddress != null
                && address.equals(mBluetoothDeviceAddress)) {
            Log.d(TAG, "Trying to use an existing mBluetoothGatt for connection.");
            if (mBluetoothGatt.connect()) {
                mConnState = STATE_CONNECTING;
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
        //We want to directly connect to the device, so we are setting the autoConnect
        // parameter to false.
        if (mBluetoothGatt != null) {
            mBluetoothGatt.close();
            //LogUtil.info("-------------关闭mBluetoothGatt");
        }

        mBluetoothGatt = device.connectGatt(this, false, mGattCallback);

        Log.d(TAG, "Trying to create a new connection.");
        mBluetoothDeviceAddress = address;
        mConnState = STATE_CONNECTING;
        return true;
    }

    /**
     * Disconnects an existing connection or cancel a pending connection. The disconnection result
     * is reported asynchronously through the BluetoothGattCallback#onConnectionStateChange.
     */
    public void disconnect() {
        if (mBluetoothAdapter == null || mBluetoothGatt == null) {
            Toast.makeText(this, "蓝牙断开了，请重新连接!", Toast.LENGTH_SHORT).show();
            Log.e(TAG, "BluetoothAdapter not initialized.");
            return;
        }
        mBluetoothGatt.disconnect();
    }

    /**
     * After using a given BLE device, the app must call this method to ensure resources are
     * released properly.
     */
    public void close() {
        isConnect = false;
        if (mBluetoothGatt == null) {
            return;
        }
        mBluetoothGatt.close();
        mBluetoothGatt = null;
    }

    /**
     * Request a read on a given {@code BluetoothGattCharacteristic}. The read result is reported
     * asynchronously through the BluetoothGattCallback#onCharacteristicRead.
     *
     * @param characteristic The characteristic to read from.
     */
    public void readCharacteristic(BluetoothGattCharacteristic characteristic) {
        if (mBluetoothAdapter == null || mBluetoothGatt == null) {
            Toast.makeText(this, "蓝牙断开了，请重新连接!", Toast.LENGTH_SHORT).show();
            Log.w(TAG, "BluetoothAdapter not initialized.");
            return;
        }
        mBluetoothGatt.readCharacteristic(characteristic);
    }

    /**
     * Request a read on a given {@code BluetoothGattCharacteristic}, specific service UUID
     * and characteristic UUID. The read result is reported asynchronously through the
     * {@code BluetoothGattCallback#onCharacteristicRead(android.bluetooth.BluetoothGatt,
     * android.bluetooth.BluetoothGattCharacteristic, int)} callback.
     *
     * @param serviceUUID        remote device service uuid
     * @param characteristicUUID remote device characteristic uuid
     */
    public void readCharacteristic(String serviceUUID, String characteristicUUID) {
        if (mBluetoothGatt != null) {
            BluetoothGattService service =
                    mBluetoothGatt.getService(UUID.fromString(serviceUUID));
            BluetoothGattCharacteristic characteristic =
                    service.getCharacteristic(UUID.fromString(characteristicUUID));
            mBluetoothGatt.readCharacteristic(characteristic);
        }
    }

    public void readCharacteristic(String address, String serviceUUID, String characteristicUUID) {
        if (mBluetoothGatt != null) {
            BluetoothGattService service =
                    mBluetoothGatt.getService(UUID.fromString(serviceUUID));
            BluetoothGattCharacteristic characteristic =
                    service.getCharacteristic(UUID.fromString(characteristicUUID));
            mBluetoothGatt.readCharacteristic(characteristic);
        }
    }

    /**
     * Write data to characteristic, and send to remote bluetooth le device.
     *
     * @param serviceUUID        remote device service uuid
     * @param characteristicUUID remote device characteristic uuid
     * @param value              Send to remote ble device data.
     */
    public void writeCharacteristic(String serviceUUID, String characteristicUUID, String value) {
        if (mBluetoothGatt != null) {
            BluetoothGattService service =
                    mBluetoothGatt.getService(UUID.fromString(serviceUUID));
            BluetoothGattCharacteristic characteristic =
                    service.getCharacteristic(UUID.fromString(characteristicUUID));
            characteristic.setWriteType(BluetoothGattCharacteristic.WRITE_TYPE_NO_RESPONSE);
            characteristic.setValue(value);
            mBluetoothGatt.writeCharacteristic(characteristic);
        }
    }

    public boolean writeCharacteristic(String serviceUUID, String characteristicUUID, byte[] value) {

        if (mBluetoothGatt != null && characteristicUUID != null) {
            BluetoothGattService service =
                    mBluetoothGatt.getService(UUID.fromString(serviceUUID));
            if (service != null) {
                BluetoothGattCharacteristic characteristic =
                        service.getCharacteristic(UUID.fromString(characteristicUUID));
                if (characteristic != null) {

//                    final int charaProp = characteristic.getProperties();
//                    if ((charaProp | BluetoothGattCharacteristic.PROPERTY_WRITE_NO_RESPONSE) > 0) {
//                        characteristic
//                                .setWriteType(BluetoothGattCharacteristic.WRITE_TYPE_NO_RESPONSE);
//                    } else {
//                        characteristic
//                                .setWriteType(BluetoothGattCharacteristic.WRITE_TYPE_DEFAULT);
//                    }
                    characteristic.setWriteType(BluetoothGattCharacteristic.WRITE_TYPE_NO_RESPONSE);
                    characteristic.setValue(value);
                    boolean write = mBluetoothGatt.writeCharacteristic(characteristic);
                    Log.e("url", "write==" + write + "   DATA1: " + Arrays.toString(characteristic.getValue()));
                    return write;
                }
            }
        }
        return false;
    }

    /**
     * Write value to characteristic, and send to remote bluetooth le device.
     *
     * @param characteristic remote device characteristic
     * @param value          New value for this characteristic
     * @return if write success return true
     */
    public boolean writeCharacteristic(BluetoothGattCharacteristic characteristic, String value) {

        if ((characteristic.getProperties() & BluetoothGattCharacteristic.PROPERTY_WRITE) == 0
                && (characteristic.getProperties() &
                BluetoothGattCharacteristic.PROPERTY_WRITE_NO_RESPONSE) == 0) return false;
        characteristic.setWriteType(BluetoothGattCharacteristic.WRITE_TYPE_NO_RESPONSE);
        return writeCharacteristic(characteristic, value.getBytes());
    }

    /**
     * Writes a given characteristic and its values to the associated remote device.
     *
     * @param characteristic remote device characteristic
     * @param value          New value for this characteristic
     * @return if write success return true
     */
    public boolean writeCharacteristic(BluetoothGattCharacteristic characteristic, byte[] value) {
        if (mBluetoothGatt != null) {
            characteristic.setWriteType(BluetoothGattCharacteristic.WRITE_TYPE_NO_RESPONSE);
            characteristic.setValue(value);
            return mBluetoothGatt.writeCharacteristic(characteristic);
        }
        return false;
    }

    /**
     * Enables or disables notification on a give characteristic.
     *
     * @param characteristic Characteristic to act on.
     * @param enabled        If true, enable notification.  False otherwise.
     */
    public void setCharacteristicNotification(BluetoothGattCharacteristic characteristic,
                                              boolean enabled) {
        if (mBluetoothAdapter == null || mBluetoothGatt == null) {
//            Toast.makeText(this, "蓝牙断开了，请重新连接!", Toast.LENGTH_SHORT).show();
            Log.w(TAG, "BluetoothAdapter not initialized");
            return;
        }
        mBluetoothGatt.setCharacteristicNotification(characteristic, enabled);

        BluetoothGattDescriptor descriptor = characteristic.getDescriptor(
                UUID.fromString(GattAttributes.DESCRIPTOR_CLIENT_CHARACTERISTIC_CONFIGURATION));
        descriptor.setValue(enabled ? BluetoothGattDescriptor.ENABLE_NOTIFICATION_VALUE :
                BluetoothGattDescriptor.DISABLE_NOTIFICATION_VALUE);
        mBluetoothGatt.writeDescriptor(descriptor);
    }

    public void setCharacteristicNotification(String serviceUUID, String characteristicUUID,
                                              boolean enabled) {
        if (mBluetoothAdapter == null || mBluetoothGatt == null) {
//            Looper.prepare();
//            Toast.makeText(this, "蓝牙断开了，请重新连接!", Toast.LENGTH_SHORT).show();
            Log.w(TAG, "BluetoothAdapter not initialized1111");
            ble_connect = "disconnect";
            return;
        }
        ble_connect = "connect";
        BluetoothGattService service =
                mBluetoothGatt.getService(UUID.fromString(serviceUUID));
        if (service != null) {
            BluetoothGattCharacteristic characteristic =
                    service.getCharacteristic(UUID.fromString(characteristicUUID));

            boolean success = mBluetoothGatt.setCharacteristicNotification(characteristic, enabled);
            Log.e(TAG, "success==" + success);
        }

//        List<BluetoothGattDescriptor> descriptors = characteristic.getDescriptors();
//        for (BluetoothGattDescriptor dp : descriptors) {
//            dp.setValue(BluetoothGattDescriptor.ENABLE_NOTIFICATION_VALUE);
//            mBluetoothGatt.writeDescriptor(dp);
//        }


//        BluetoothGattDescriptor descriptor = characteristic.getDescriptor(
//                UUID.fromString(GattAttributes.DESCRIPTOR_CLIENT_CHARACTERISTIC_CONFIGURATION));
//        descriptor.setValue(enabled ? BluetoothGattDescriptor.ENABLE_NOTIFICATION_VALUE :
//                BluetoothGattDescriptor.DISABLE_NOTIFICATION_VALUE);
//        mBluetoothGatt.writeDescriptor(descriptor);


    }

    /**
     * Reads the value for a given descriptor from the associated remote device.
     * <p/>
     * <p>Once the read operation has been completed, the
     * {@link BluetoothGattCallback#onDescriptorRead} callback is
     * triggered, signaling the result of the operation.
     * <p/>
     * <p>Requires {@link android.Manifest.permission#BLUETOOTH} permission.
     *
     * @param descriptor Descriptor value to read from the remote device
     * @return true, if the read operation was initiated successfully
     */
    public boolean readDescriptor(BluetoothGattDescriptor descriptor) {
        if (mBluetoothGatt == null) {
            Log.w(TAG, "BluetoothGatt is null");
            return false;
        }
        return mBluetoothGatt.readDescriptor(descriptor);
    }

    /**
     * Reads the value for a given descriptor from the associated remote device.
     *
     * @param serviceUUID        remote device service uuid
     * @param characteristicUUID remote device characteristic uuid
     * @param descriptorUUID     remote device descriptor uuid
     * @return true, if the read operation was initiated successfully
     */
    public boolean readDescriptor(String serviceUUID, String characteristicUUID,
                                  String descriptorUUID) {
        if (mBluetoothGatt == null) {
            Log.w(TAG, "BluetoothGatt is null");
            return false;
        }
//        try {
        BluetoothGattService service =
                mBluetoothGatt.getService(UUID.fromString(serviceUUID));
        BluetoothGattCharacteristic characteristic =
                service.getCharacteristic(UUID.fromString(characteristicUUID));
        BluetoothGattDescriptor descriptor =
                characteristic.getDescriptor(UUID.fromString(descriptorUUID));
        return mBluetoothGatt.readDescriptor(descriptor);
//        } catch (Exception e) {
//            Log.e(TAG, "read descriptor exception", e);
//            return false;
//        }
    }

    /**
     * Read the RSSI for a connected remote device.
     * <p/>
     * <p>The {@link BluetoothGattCallback#onReadRemoteRssi} callback will be
     * invoked when the RSSI value has been read.
     * <p/>
     * <p>Requires {@link android.Manifest.permission#BLUETOOTH} permission.
     *
     * @return true, if the RSSI value has been requested successfully
     */
    public boolean readRemoteRssi() {
        if (mBluetoothGatt == null) return false;
        return mBluetoothGatt.readRemoteRssi();
    }

    /**
     * Request an MTU size used for a given connection.
     * <p/>
     * <p>When performing a write request operation (write without response),
     * the data sent is truncated to the MTU size. This function may be used
     * to request a larger MTU size to be able to send more data at once.
     * <p/>
     * <p>A {@link BluetoothGattCallback#onMtuChanged} callback will indicate
     * whether this operation was successful.
     * <p/>
     * <p>Requires {@link Manifest.permission#BLUETOOTH} permission.
     *
     * @return true, if the new MTU value has been requested successfully
     */
    public boolean requestMtu(int mtu) {
        if (mBluetoothGatt == null) return false;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {//Android API level >= 21
            return mBluetoothGatt.requestMtu(mtu);
        } else {
            return false;
        }
    }

    public boolean isConnect() {
        return isConnect;
    }

    public BluetoothDevice getConnectDevice() {
        if (mBluetoothGatt == null) return null;
        return mBluetoothGatt.getDevice();
    }

    public BluetoothGatt getBluetoothGatt() {
        return mBluetoothGatt;
    }

    /**
     * Retrieves a list of supported GATT services on the connected device. This should be
     * invoked only after {@code BluetoothGatt#discoverServices()} completes successfully.
     *
     * @return A {@code List} of supported services.
     */
    public List<BluetoothGattService> getSupportedGattServices() {
        if (mBluetoothGatt == null) return null;
        return mBluetoothGatt.getServices();
    }

    public List<BluetoothDevice> getConnectDevices() {
        if (mBluetoothManager == null) return null;
        return mBluetoothManager.getConnectedDevices(BluetoothProfile.GATT);
    }

    /**
     * Device scan callback
     */
    private final BluetoothAdapter.LeScanCallback mScanCallback = new BluetoothAdapter.LeScanCallback() {
        @Override
        public void onLeScan(BluetoothDevice device, int rssi, byte[] scanRecord) {

//            try {
            if (mScanLeDeviceList.contains(device)) return;
            mScanLeDeviceList.add(device);
//            } catch (NullPointerException e) {
//                e.printStackTrace();
//            }

            if (mOnLeScanListener != null) {
                mOnLeScanListener.onLeScan(device, rssi, scanRecord);
            }

            Log.e("url", "Bleservice_rssi==" + rssi);
            BLERSSI = rssi;
            broadcastUpdate(ACTION_BLUETOOTH_DEVICE, device);
        }
    };

    /*private final ScanCallback mLeScanCallback = new ScanCallback() {
        @Override
        public void onScanResult(int callbackType, ScanResult result) {
            super.onScanResult(callbackType, result);
        }
    };*/


    /**
     * Implements callback methods for GATT events that the app cares about.  For example,
     * connection change and services discovered.
     */
    private final BluetoothGattCallback mGattCallback = new BluetoothGattCallback() {
        @Override
        public void onConnectionStateChange(BluetoothGatt gatt, int status, int newState) {
            Log.e("url", "onConnectionStateChange");
//            Log.e("url", "BleService_status=="+status);

            send_phone_back_data = new ArrayList<>();

            if (mOnConnectionStateChangeListener != null) {
                mOnConnectionStateChangeListener.onConnectionStateChange(gatt, status, newState);
            }
            String intentAction;
            String address = gatt.getDevice().getAddress();
            if (newState == BluetoothProfile.STATE_DISCONNECTED) {
                EquipMentFg.scanBleList.clear();
                MainActivity.is_scan = false;
                isconnect = false;

                Log.i(TAG, "onConnectionStateChange: DISCONNECTED: " + getConnectDevices().size());
                intentAction = ACTION_GATT_DISCONNECTED;
                isConnect = false;
                mConnState = STATE_DISCONNECTED;
                Log.i(TAG, "Disconnected from GATT server.");
                broadcastUpdate(intentAction, address);
                close();
            } else if (newState == BluetoothProfile.STATE_CONNECTING) {
                Log.i(TAG, "onConnectionStateChange: CONNECTING: " + getConnectDevices().size());
                intentAction = ACTION_GATT_CONNECTING;
                mConnState = STATE_CONNECTING;
                Log.i(TAG, "Connecting to GATT server.");
                broadcastUpdate(intentAction, address);
            } else if (newState == BluetoothProfile.STATE_CONNECTED) {

                Log.i(TAG, "onConnectionStateChange: CONNECTED: " + getConnectDevices().size());
                intentAction = ACTION_GATT_CONNECTED;
                isConnect = true;
                mConnState = STATE_CONNECTED;
                broadcastUpdate(intentAction, address);
                Log.i(TAG, "Connected to GATT server.");
                // Attempts to discover services after successful connection.
                Log.i(TAG, "Attempting to start service discovery:" +
                        mBluetoothGatt.discoverServices());

                if (getConnectDevices().size() > 0) {
                    MainActivity.is_scan = true;
                    isconnect = true;
                    Log.e("url", "连接音的 开关（on开，off关）== " + sharedPreferencesUtil.getConnectMusicSwitch());
                    Log.e("url", "连接音的类型（1：叮咚 2：已连接）== " + sharedPreferencesUtil.getConnectMusic());

                    if (sharedPreferencesUtil.getConnectMusicSwitch().equals("on")) {
                        if (sharedPreferencesUtil.getConnectMusic().equals("1")) {
                            music = MediaPlayer.create(instance, R.raw.connect_dingdong);
                            music.start();
                        } else if (sharedPreferencesUtil.getConnectMusic().equals("2")) {
                            music = MediaPlayer.create(instance, R.raw.connect_connect);
                            music.start();
                        }
                    }
                }

            } else if (newState == BluetoothProfile.STATE_DISCONNECTING) {
                Log.i(TAG, "onConnectionStateChange: DISCONNECTING: " + getConnectDevices().size());
                intentAction = ACTION_GATT_DISCONNECTING;
                mConnState = STATE_DISCONNECTING;
                Log.i(TAG, "Disconnecting from GATT server.");
                broadcastUpdate(intentAction, address);
            }
        }


        // New services discovered
        @Override
        public void onServicesDiscovered(BluetoothGatt gatt, int status) {
            Log.e("url", "BleService_status==" + status);

            if (mOnServicesDiscoveredListener != null) {
                mOnServicesDiscoveredListener.onServicesDiscovered(gatt, status);
            }
            if (status == BluetoothGatt.GATT_SUCCESS) {
                broadcastUpdate(ACTION_GATT_SERVICES_DISCOVERED);
            } else {
                Log.w(TAG, "onServicesDiscovered received: " + status);
            }
        }

        // Result of a characteristic read operation
        @Override
        public void onCharacteristicRead(BluetoothGatt gatt,
                                         BluetoothGattCharacteristic characteristic, int status) {
//            Log.e("tag", "onCharacteristicRead" );
            Log.e("tag", "read返回信息--> " + com.yundiankj.ble_lock.Resource.DigitalTrans.bytesToHexString(characteristic.getValue()));
            if (mOnDataAvailableListener != null) {
                mOnDataAvailableListener.onCharacteristicRead(gatt, characteristic, status);
            }

            if (status == BluetoothGatt.GATT_SUCCESS) {
                broadcastUpdate(ACTION_DATA_AVAILABLE, characteristic);
            }
        }

        @Override
        public void onCharacteristicWrite(BluetoothGatt gatt, BluetoothGattCharacteristic characteristic, int status) {
            super.onCharacteristicWrite(gatt, characteristic, status);
            Log.e("tag", "onCharacteristicWrite");
//
//            super.onCharacteristicWrite(gatt, characteristic, status);
//            String address = gatt.getDevice().getAddress();
//            for (int i = 0; i < characteristic.getValue().length; i++) {
//                Log.i(TAG, "address: " + address + ",Write: " + characteristic.getValue()[i]);
//            }
        }

        @Override
        public void onCharacteristicChanged(BluetoothGatt gatt,
                                            BluetoothGattCharacteristic characteristic) {
//            Log.e("tag", "返回信息111--> " +characteristic.getValue());

            Log.e("tag", "onCharacteristicChanged");
            Log.e("tag", "BleService返回信息（未解密）--> " + com.yundiankj.ble_lock.Resource.DigitalTrans.bytesToHexString(characteristic.getValue()));

//            String data = DigitalTrans.bytesToHexString(characteristic.getValue());

            String decrypt_back_data = null;
            try {
                decrypt_back_data = com.yundiankj.ble_lock.Resource.DigitalTrans.bytesToHexString(AesEntryDetry.decrypt(characteristic.getValue()));//解密
            } catch (Exception e) {
                e.printStackTrace();
            }
            Log.e("url", "BleService返回信息(解密后)-->" + decrypt_back_data);
            Log.e("tag", "onCharacteristicChanged");
            MainActivity.list_phone_user_info_32.add(decrypt_back_data);//添加手机普通用户
            MainActivity.list_admin_pw_info_32.add(decrypt_back_data);//添加管理员密码
            MainActivity.list_ordinary_user_info_32.add(decrypt_back_data);//添加键盘普通用户
            MainActivity.list_tm_card_user_info_32.add(decrypt_back_data);//添加TM卡用户
            MainActivity.list_unlock_record_info_32.add(decrypt_back_data);//添加开锁记录
            MainActivity.list_one_time_pw_info_32.add(decrypt_back_data);//添加一次性单次用户

//            Log.e("tag", "返回信息--> " + com.yundiankj.ble_lock.Resource.DigitalTrans.bytesToHexString(characteristic.getValue()));
//            String data = com.yundiankj.ble_lock.Resource.DigitalTrans.bytesToHexString(characteristic.getValue());

            String six_back_data = decrypt_back_data.substring(decrypt_back_data.length() - 32, decrypt_back_data.length() - 26);
            if (decrypt_back_data.length() == 32 && six_back_data.contains("534a")) {
                back_data = six_back_data;
            } else {
                back_data = decrypt_back_data;
            }

            if (six_back_data.equals("1f4b4f")) {
                MainActivity.is_upload = true;
                Log.e("url", "们开了");
                if (sharedPreferencesUtil.getUnlockMusicSwitch().equals("on")) {
                    if (sharedPreferencesUtil.getUnlockMusic().equals("1")) {
                        music = MediaPlayer.create(instance, R.raw.unlock_open_door);
                        music.start();
                    }
                }
            }

            String electricity_back_data = decrypt_back_data.substring(decrypt_back_data.length() - 32, decrypt_back_data.length() - 29);
            if ((electricity_back_data.contains("310")) || six_back_data.equals("02534a")) {
                send_phone_back_data.add(six_back_data);
            }


            if (mOnDataAvailableListener != null) {
                mOnDataAvailableListener.onCharacteristicChanged(gatt, characteristic);
            }

            broadcastUpdate(ACTION_DATA_AVAILABLE, characteristic);

        }

        @Override
        public void onDescriptorRead(BluetoothGatt gatt, BluetoothGattDescriptor descriptor, int status) {
            if (mOnDataAvailableListener != null) {
                mOnDataAvailableListener.onDescriptorRead(gatt, descriptor, status);
            }
        }

        @Override
        public void onReadRemoteRssi(BluetoothGatt gatt, int rssi, int status) {
            super.onReadRemoteRssi(gatt, rssi, status);
            //将回调的RSSI值赋值
            BLERSSI = rssi;
        }

        @Override
        public void onMtuChanged(BluetoothGatt gatt, int mtu, int status) {
            if (mOnMtuChangedListener != null) {
                mOnMtuChangedListener.onMtuChanged(gatt, mtu, status);
            }
        }


    };

    private void broadcastUpdate(final String action) {
        final Intent intent = new Intent(action);
        sendBroadcast(intent);
    }

    private void broadcastUpdate(final String action,
                                 final BluetoothGattCharacteristic characteristic) {
        final Intent intent = new Intent(action);

        // This is special handling for the Heart Rate Measurement profile.  Data parsing is
        // carried out as per profile specifications:
        // http://developer.bluetooth.org/gatt/characteristics/Pages/CharacteristicViewer.aspx?u=org.bluetooth.characteristic.heart_rate_measurement.xml
        if (UUID_HEART_RATE_MEASUREMENT.equals(characteristic.getUuid())) {
            int flag = characteristic.getProperties();
            int format = -1;
            if ((flag & 0x01) != 0) {
                format = BluetoothGattCharacteristic.FORMAT_UINT16;
                Log.d(TAG, "Heart rate format UINT16.");
            } else {
                format = BluetoothGattCharacteristic.FORMAT_UINT8;
                Log.d(TAG, "Heart rate format UINT8.");
            }
            final int heartRate = characteristic.getIntValue(format, 1);
            Log.d(TAG, String.format("Received heart rate: %d", heartRate));
            intent.putExtra(EXTRA_DATA, String.valueOf(heartRate));
        } else {
            // For all other profiles, writes the data formatted in HEX.

//            Log.e("tag", "返回信息--> " + DigitalTrans.bytesToHexString(characteristic.getValue()));
            intent.putExtra(EXTRA_DATA, com.yundiankj.ble_lock.Resource.DigitalTrans.bytesToHexString(characteristic.getValue()));
        }
        sendBroadcast(intent);
    }

    private void broadcastUpdate(final String action, final String address) {
        final Intent intent = new Intent(action);
        intent.putExtra("address", address);
        sendBroadcast(intent);
    }

    private void broadcastUpdate(final String action, BluetoothDevice device) {
        final Intent intent = new Intent(action);
        intent.putExtra("name", device.getName());
        intent.putExtra("address", device.getAddress());
        sendBroadcast(intent);
    }

    public void setOnLeScanListener(OnLeScanListener l) {
        mOnLeScanListener = l;
    }

    public void setOnConnectListener(OnConnectionStateChangeListener l) {
        mOnConnectionStateChangeListener = l;
    }

    public void setOnServicesDiscoveredListener(OnServicesDiscoveredListener l) {
        mOnServicesDiscoveredListener = l;
    }

    public void setOnDataAvailableListener(OnDataAvailableListener l) {
        mOnDataAvailableListener = l;
    }

    public void setOnReadRemoteRssiListener(OnReadRemoteRssiListener l) {
        mOnReadRemoteRssiListener = l;
    }

    public void setOnMtuChangedListener(OnMtuChangedListener l) {
        mOnMtuChangedListener = l;
    }


    public static short appData_Crc(short[] src, short crc, int len) {
        int i;
        short bb;
        for (int j = 0; j < len; j++) {
            bb = src[j];
            for (i = 8; i > 0; --i) {  //Boolean.parseBoolean(Integer.toBinaryString((bb & 0x01)^(crc &0x01)))
                if ((((bb ^ crc) & 0x01)) == 1) {     //判断与x7异或的结果(x8)((bb ^ crc) & 0x01)

                    crc ^= 0x18;               //反馈到x5   x4
                    crc >>= 1;                     //移位
                    crc |= 0x80;               //x7异或的结果送x0
                } else {
                    crc >>= 1;
                }
                bb >>= 1;
            }
        }
        return (crc);
    }

    //获取已经得到的RSSI值
    public static int getBLERSSI() {
        return BLERSSI;
    }
    //是都能读取到已连接设备的RSSI值
    //执行该方法一次，获得蓝牙回调onReadRemoteRssi（）一次

    /**
     * Read the RSSI for a connected remote device.
     */
    public boolean getRssiVal() {

        if (mBluetoothGatt == null)
            return false;
        return mBluetoothGatt.readRemoteRssi();

    }

    /**
     * Clears the internal cache and forces a refresh of the services from the
     * remote device.
     */
    public boolean refreshDeviceCache() {
        Log.i(TAG, "refreshDeviceCache");
        Log.i(TAG, "mBluetoothGatt==" + mBluetoothGatt);
        if (mBluetoothGatt != null) {
            try {
                BluetoothGatt localBluetoothGatt = mBluetoothGatt;
                Method localMethod = localBluetoothGatt.getClass().getMethod(
                        "refresh", new Class[0]);
                Log.i(TAG, "localMethod==" + localMethod);
                if (localMethod != null) {
                    boolean bool = ((Boolean) localMethod.invoke(
                            localBluetoothGatt, new Object[0])).booleanValue();
                    return bool;
                }
            } catch (Exception localException) {
                Log.i(TAG, "An exception occured while refreshing device");
            }
        }
        return false;
    }


}
package com.yundiankj.ble_lock.Classes.Fragment;

import android.app.Activity;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothGattDescriptor;
import android.bluetooth.BluetoothGattService;
import android.bluetooth.BluetoothManager;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.ServiceConnection;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.ColorDrawable;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.support.v4.app.Fragment;
import android.util.Log;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.widget.AdapterView;
import android.widget.BaseAdapter;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.PopupWindow;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.junkchen.blelib.BleListener;
import com.yundiankj.ble_lock.Classes.Activity.MainActivity;
import com.yundiankj.ble_lock.Classes.Adapter.PopTypeAdapter;
import com.yundiankj.ble_lock.Classes.Model.ParsedAd;
import com.yundiankj.ble_lock.Classes.Model.ScanBle;
import com.yundiankj.ble_lock.R;
import com.yundiankj.ble_lock.Resource.AesEntryDetry;
import com.yundiankj.ble_lock.Resource.BleService;
import com.yundiankj.ble_lock.Resource.DigitalTrans;
import com.yundiankj.ble_lock.Resource.MyGattAttributes;
import com.yundiankj.ble_lock.Resource.SQLite.BleSQLiteOpenHelper;
import com.yundiankj.ble_lock.Resource.SQLite.DisposableUserSQLiteOpenHelper;
import com.yundiankj.ble_lock.Resource.SQLite.PhoneUserSQLiteOpenHelper;
import com.yundiankj.ble_lock.Resource.SharedPreferencesUtil;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.UUID;

/**
 * Created by hong on 2016/7/18.
 * <p>
 * 发现fg
 */
public class FindFg extends Fragment {

    private View rootView;
    private Activity mActivity;
    private SharedPreferencesUtil sharedPreferencesUtil;
    private LinearLayout lly_content, lly_all_equipment;
    private RelativeLayout rly_all_equipment;
    private TextView tv_type;
    private ImageView img_down_up;
    private View main_darkview;
    private ListView lv_device;
    private TextView tv_scanBle;
    private LeDeviceListAdapter mLeDeviceListAdapter;//发现设备的适配器
    public static List<ScanBle> bleDataList;//存放蓝牙信息+rssi
    private List<ScanBle> scanBleList;
    private ScanBle scanBle;

    //选择产品类型popwindow
    private PopupWindow pop_chooce_product_type;
    private LinearLayout lly_find_door, lly_find_lock;

    //设备类型popwindow
    private PopupWindow pop_all_equipment;
    private ListView lv_type;
    private PopTypeAdapter popTypeAdapter;
    private String[] typeList = {"全部", "门锁", "挂锁"};
    private String type;//全部（all）,门锁（door）,挂锁(hand)
    private boolean is_show = false;

    //蓝牙
    private BluetoothAdapter mBluetoothAdapter;
    private boolean mScanning;
    private Handler mHandler;
    private static final int REQUEST_ENABLE_BT = 1;
    private static final long SCAN_PERIOD = 1000;// 1秒后停止查找搜索.
    private BleService mBleService;
    private StringBuffer phoneStringBuffer;
    public String handleString = "000";
    private String mDeviceAddress;
    private int RSSI;
    private String user_type;
    private boolean mIsBind;

    //数据库
    private BleSQLiteOpenHelper bleSQLiteOpenHelper;//蓝牙数据库
    private SQLiteDatabase ble_db;
    private PhoneUserSQLiteOpenHelper phoneUserSQLiteOpenHelper;//手机用户数据库
    private SQLiteDatabase phone_user_db;
    private DisposableUserSQLiteOpenHelper disposableUserSQLiteOpenHelper;//期限用户数据库
    private SQLiteDatabase disposable_user_db;

    private List<BluetoothGattService> gattServiceList;
    private List<String> serviceList;
    private List<String[]> characteristicList;

    private boolean isUpdate = false;//用来一直下发rssi

    private String add_back_data = "00";

    // 当创建fragment的UI被初始化时调用。
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        if (rootView == null) {
            rootView = inflater.inflate(R.layout.find_fragment, container, false);
        }
        ViewGroup parent = (ViewGroup) rootView.getParent();
        if (parent != null) {
            parent.removeView(rootView);
        }

        return rootView;
    }

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
        mActivity = getActivity();

        initBletooth();
        init();
    }

    /**
     * 检查设备上是否支持蓝牙
     */
    private void initBletooth() {
        mHandler = new Handler();

        // 检查当前手机是否支持ble 蓝牙,如果不支持退出程序
        if (!mActivity.getPackageManager().hasSystemFeature(PackageManager.FEATURE_BLUETOOTH_LE)) {
            Toast.makeText(mActivity, R.string.ble_not_supported, Toast.LENGTH_SHORT).show();
        }

        // 初始化 Bluetooth adapter, 通过蓝牙管理器得到一个参考蓝牙适配器(API必须在以上android4.3或以上和版本)
        final BluetoothManager bluetoothManager = (BluetoothManager) mActivity.getSystemService(Context.BLUETOOTH_SERVICE);
        mBluetoothAdapter = bluetoothManager.getAdapter();

        // 检查设备上是否支持蓝牙
        if (mBluetoothAdapter == null) {
            Toast.makeText(mActivity, R.string.error_bluetooth_not_supported, Toast.LENGTH_SHORT).show();
            return;
        }

        // 为了确保设备上蓝牙能使用, 如果当前蓝牙设备没启用,弹出对话框向用户要求授予权限来启用
        if (!mBluetoothAdapter.isEnabled()) {
            if (!mBluetoothAdapter.isEnabled()) {
                Intent enableBtIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
                startActivityForResult(enableBtIntent, REQUEST_ENABLE_BT);
            }
        }
    }

    private void init() {
        bleSQLiteOpenHelper = new BleSQLiteOpenHelper(mActivity, "BleDatabase.db", null, 2);
        bleSQLiteOpenHelper.getWritableDatabase();//创建连接过的蓝牙表
        phoneUserSQLiteOpenHelper = new PhoneUserSQLiteOpenHelper(mActivity, "UserDatabase.db", null, 2);
        phoneUserSQLiteOpenHelper.getWritableDatabase();//创建用户表
        disposableUserSQLiteOpenHelper = new DisposableUserSQLiteOpenHelper(mActivity, "DisposableUserDatabase.db", null, 2);
        disposableUserSQLiteOpenHelper.getWritableDatabase();//创建期限用户表
        sharedPreferencesUtil = SharedPreferencesUtil.getInstance(mActivity);

        tv_scanBle = (TextView) rootView.findViewById(R.id.tv_scanBle);
        lly_content = (LinearLayout) rootView.findViewById(R.id.lly_content);
        lly_all_equipment = (LinearLayout) rootView.findViewById(R.id.lly_all_equipment);
        rly_all_equipment = (RelativeLayout) rootView.findViewById(R.id.rly_all_equipment);
        tv_type = (TextView) rootView.findViewById(R.id.tv_type);
        img_down_up = (ImageView) rootView.findViewById(R.id.img_down_up);
        main_darkview = (View) rootView.findViewById(R.id.main_darkview);
        lv_device = (ListView) rootView.findViewById(R.id.lv_device);

        // 绑定服务
        Intent serviceIntent = new Intent(mActivity, BleService.class);
        mActivity.bindService(serviceIntent, mServiceConnection, Context.BIND_AUTO_CREATE);
        mActivity.registerReceiver(bleReceiver, makeIntentFilter());

        serviceList = new ArrayList<>();
        characteristicList = new ArrayList<>();
        scanBleList = new ArrayList<>();
        scanBle = new ScanBle();
        bleDataList = new ArrayList<>();

        WindowManager.LayoutParams lp;
        initChooseProductTypePopupWindow();
        //设置背景颜色变暗
        lp = mActivity.getWindow().getAttributes();
        lp.alpha = 0.5f;
        mActivity.getWindow().setAttributes(lp);
        pop_chooce_product_type.setAnimationStyle(R.style.PopupAnimation);
        pop_chooce_product_type.showAtLocation(rootView.findViewById(R.id.lly_main), Gravity.CENTER | Gravity.CENTER_HORIZONTAL, 0, 0);
        pop_chooce_product_type.update();

        initAllEquipmentPopupWindow();

        popTypeAdapter = new PopTypeAdapter(mActivity, typeList);
        lv_type.setAdapter(popTypeAdapter);

        rly_all_equipment.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (is_show == false) {
                    img_down_up.setImageDrawable(getResources().getDrawable(R.mipmap.btn_up));
                    is_show = true;
                    main_darkview.setVisibility(View.VISIBLE);

                    lv_type.setOnItemClickListener(new AdapterView.OnItemClickListener() {
                        public void onItemClick(AdapterView<?> arg0, View arg1, int arg2, long arg3) {
                            Log.e("url", "" + typeList[arg2]);
                            tv_type.setText(typeList[arg2]);
                            bleDataList.clear();

                            if (typeList[arg2].equals("全部")) {
                                type = "all";
                                for (int i = 0; i < EquipMentFg.scanBleList.size(); i++) {
                                    try {
                                        if (EquipMentFg.scanBleList.get(i).getBle_name().toString().contains("Gemini") ||
                                                EquipMentFg.scanBleList.get(i).getBle_name().toString().contains("Hemini")) {
                                            bleDataList.add(EquipMentFg.scanBleList.get(i));
                                        }
                                    } catch (NullPointerException e) {
                                        e.printStackTrace();
                                    }
                                }
                                Log.e("url", "pop_all_bbleDataList.size==" + bleDataList.size());
                            } else if (typeList[arg2].equals("门锁")) {
                                type = "door";
                                for (int i = 0; i < EquipMentFg.scanBleList.size(); i++) {
                                    try {
                                        if (EquipMentFg.scanBleList.get(i).getBle_name().toString().contains("Gemini")) {
                                            bleDataList.add(EquipMentFg.scanBleList.get(i));
                                        }
                                    } catch (NullPointerException e) {
                                        e.printStackTrace();
                                    }
                                }
                                Log.e("url", "pop_door_bbleDataList.size==" + bleDataList.size());
                            } else if (typeList[arg2].equals("挂锁")) {
                                type = "hand";
                                for (int i = 0; i < EquipMentFg.scanBleList.size(); i++) {

                                    try {
                                        if (EquipMentFg.scanBleList.get(i).getBle_name().contains("Hemini")) {
                                            bleDataList.add(EquipMentFg.scanBleList.get(i));
                                        }
                                    } catch (NullPointerException e) {
                                        e.printStackTrace();
                                    }
                                }
                                Log.e("url", "pop_hand_bbleDataList.size==" + bleDataList.size());
                            }

                            mLeDeviceListAdapter.notifyDataSetChanged();

                            pop_all_equipment.dismiss();
                            pop_all_equipment.setFocusable(false);
                        }
                    });

                    pop_all_equipment.setAnimationStyle(R.style.PopupAnimation);
                    pop_all_equipment.showAsDropDown(lly_all_equipment, 0, 0);
                    pop_all_equipment.update();
                } else {
                    is_show = false;
                    pop_all_equipment.dismiss();
                    pop_all_equipment.setFocusable(false);
                }
            }
        });

        tv_scanBle.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
//                BleService.getInstance().refreshDeviceCache();//清除蓝牙缓存
                mLeDeviceListAdapter.clear();
                EquipMentFg.scanBleList.clear();
                EquipMentFg.bluetoothDeviceList.clear();
                bleDataList.clear();
                scanLeDevice(true);
            }
        });

        Log.e("url", "EquipMentFg.scanBleList.size==" + EquipMentFg.scanBleList.size());
        Log.e("url", "bleDataList.size==" + bleDataList.size());
        mLeDeviceListAdapter = new LeDeviceListAdapter(bleDataList);//设备列表的适配器
        lv_device.setAdapter(mLeDeviceListAdapter);
    }

    /**
     * 初始化选择设备类型的popupWindow
     */
    private void initChooseProductTypePopupWindow() {
        LayoutInflater inflater = LayoutInflater.from(mActivity);
        View product_type_layout = inflater.inflate(R.layout.pop_choose_product_type, null);
        lly_find_door = (LinearLayout) product_type_layout.findViewById(R.id.lly_find_door);
        lly_find_lock = (LinearLayout) product_type_layout.findViewById(R.id.lly_find_lock);
        product_type_layout.invalidate();
        pop_chooce_product_type = new PopupWindow(product_type_layout, ViewGroup.LayoutParams.FILL_PARENT, ViewGroup.LayoutParams.FILL_PARENT);
        //实例化一个ColorDrawable颜色为半透明
        ColorDrawable dw = new ColorDrawable(0x00000000);
        //设置PopupWindow弹出窗体的背景
        pop_chooce_product_type.setBackgroundDrawable(dw);
        pop_chooce_product_type.setOutsideTouchable(true);
        pop_chooce_product_type.setFocusable(true);
        pop_chooce_product_type.setOnDismissListener(new PopupWindow.OnDismissListener() {
            @Override
            public void onDismiss() {
                WindowManager.LayoutParams lp = mActivity.getWindow().getAttributes();
                lp.alpha = 1;
                mActivity.getWindow().setAttributes(lp);
            }
        });

        //门锁
        lly_find_door.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                pop_chooce_product_type.dismiss();
                lly_content.setVisibility(View.VISIBLE);
                tv_type.setText("门锁");
                type = "door";
                Log.e("url", "door_ EquipMentFg.scanBleList==" + EquipMentFg.scanBleList.size());

//                mLeDeviceListAdapter.clear();
//                EquipMentFg.scanBleList.clear();
//                EquipMentFg.bluetoothDeviceList.clear();
//                bleDataList.clear();
//                scanLeDevice(true);

            }
        });

        //挂锁
        lly_find_lock.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                pop_chooce_product_type.dismiss();
                lly_content.setVisibility(View.VISIBLE);
                tv_type.setText("挂锁");
                type = "hand";

//                mLeDeviceListAdapter.clear();
//                EquipMentFg.scanBleList.clear();
//                EquipMentFg.bluetoothDeviceList.clear();
//                bleDataList.clear();
//                scanLeDevice(true);

            }
        });

        //蓝牙在扫描，更新发现蓝牙列表
        if (!updateUI.isAlive()) {
            isUpdate = true;
            updateUI.start();
        }
    }

    /**
     * 所有设备的popupwindow
     */
    private void initAllEquipmentPopupWindow() {
        LayoutInflater inflater = LayoutInflater.from(mActivity);
        View layout = inflater.inflate(R.layout.pop_had_connect_equipment, null);
        lv_type = (ListView) layout.findViewById(R.id.lv_type);
        layout.invalidate();
        pop_all_equipment = new PopupWindow(layout, ViewGroup.LayoutParams.FILL_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        pop_all_equipment.setOnDismissListener(new PopupWindow.OnDismissListener() {
            @Override
            public void onDismiss() {
                img_down_up.setImageDrawable(getResources().getDrawable(R.mipmap.btn_down));
                is_show = false;
                main_darkview.setVisibility(View.GONE);
            }
        });
        pop_all_equipment.setBackgroundDrawable(new BitmapDrawable());
        pop_all_equipment.setOutsideTouchable(true);
        pop_all_equipment.setFocusable(true);
    }

    /**
     * 扫描蓝牙设备
     *
     * @param enable
     */
    private void scanLeDevice(final boolean enable) {
        if (enable) {
            // Stops scanning after a pre-defined scan period.
            mHandler.postDelayed(new Runnable() {
                @Override
                public void run() {
                    mScanning = false;
                    mBluetoothAdapter.stopLeScan(mLeScanCallback);
                }
            }, SCAN_PERIOD);

            mScanning = true;
            mBluetoothAdapter.startLeScan(mLeScanCallback);
        } else {
            mScanning = false;
            mBluetoothAdapter.stopLeScan(mLeScanCallback);
        }
    }

    /**
     * 发现蓝牙设备的适配器
     */
    private class LeDeviceListAdapter extends BaseAdapter {
        private List<ScanBle> scanBleList;
        private ArrayList<BluetoothDevice> mLeDevices;
        private LayoutInflater mInflator;

        public LeDeviceListAdapter(List<ScanBle> scanBleList) {
            super();
            this.scanBleList = scanBleList;
            mLeDevices = new ArrayList<BluetoothDevice>();
            mInflator = mActivity.getLayoutInflater();
        }

        public Object getDevice(int position) {
            return scanBleList.get(position);
        }

        public void clear() {
            mLeDevices.clear();
        }

        @Override
        public int getCount() {
            return scanBleList.size();
        }

        @Override
        public Object getItem(int i) {
            return scanBleList.get(i);
        }

        @Override
        public long getItemId(int i) {
            return i;
        }

        @Override
        public View getView(final int position, View convertView, ViewGroup viewGroup) {
            final ViewHolder viewHolder;
            // General ListView optimization code.
            if (convertView == null) {
                convertView = mInflator.inflate(R.layout.find_fragment_listitem, null);
                viewHolder = new ViewHolder();
                viewHolder.tv_ble_name = (TextView) convertView.findViewById(R.id.tv_ble_name);
                viewHolder.tv_mac = (TextView) convertView.findViewById(R.id.tv_mac);
                viewHolder.tv_add_noadd = (TextView) convertView.findViewById(R.id.tv_add_noadd);
                viewHolder.img_enter = (ImageView) convertView.findViewById(R.id.img_enter);

                convertView.setTag(viewHolder);
            } else {
                viewHolder = (ViewHolder) convertView.getTag();
            }

            final BluetoothDevice device = scanBleList.get(position).getBluetoothDeviceList().get(position);
            final String deviceName = scanBleList.get(position).getBle_name();
            final String mac = device.getAddress();
            final String[] type = {"0"};

            Log.e("url", "deviceName==" + deviceName + "    mac==" + mac);
            viewHolder.tv_add_noadd.setText("非添加模式");
            viewHolder.img_enter.setVisibility(View.INVISIBLE);
            viewHolder.tv_mac.setText(mac);

            ble_db = bleSQLiteOpenHelper.getWritableDatabase();
            //游标查询每条数据
            Cursor cursor = ble_db.query("Ble_Table", null, "mac=?", new String[]{mac}, null, null, null);
            if (cursor.moveToNext()) {
                int count = cursor.getInt(0);
                if (count > 0) {//数据存在
                    viewHolder.tv_ble_name.setText(cursor.getString(1));
                }
            } else {
                if (deviceName != null && deviceName.length() > 0)
                    viewHolder.tv_ble_name.setText(deviceName);
                else
                    viewHolder.tv_ble_name.setText(R.string.unknown_device);
            }

            GetPhone(mac);//获取蓝牙数据库中的一些常用数据

            BleService.getInstance().connect(mac);//连接扫描到的蓝牙

            MainActivity.is_scan = true;
            BleService.getInstance().isconnect = true;

            mBleService.setOnServicesDiscoveredListener(new BleService.OnServicesDiscoveredListener() {
                @Override
                public void onServicesDiscovered(BluetoothGatt gatt, int status) {
                    if (status == BluetoothGatt.GATT_SUCCESS) {
                        gattServiceList = gatt.getServices();
                        serviceList.clear();
                        for (BluetoothGattService service :
                                gattServiceList) {
                            String serviceUuid = service.getUuid().toString();
                            if (containsString(serviceUuid, "ffe0")) {//保存ffe0的service
                                sharedPreferencesUtil.setService(serviceUuid);
                                Log.e("url", "serviceUuid==" + sharedPreferencesUtil.getService());
                            }

                            serviceList.add(MyGattAttributes.lookup(serviceUuid, "Unknown") + "\n" + serviceUuid);
                            Log.i("url", MyGattAttributes.lookup(serviceUuid, "Unknown") + "\n" + serviceUuid);

                            List<BluetoothGattCharacteristic> characteristics = service.getCharacteristics();
                            String[] charArra = new String[characteristics.size()];
                            for (int i = 0; i < characteristics.size(); i++) {
                                String charUuid = characteristics.get(i).getUuid().toString();
                                charArra[i] = MyGattAttributes.lookup(charUuid, "Unknown") + "\n" + charUuid;

                                if (containsString(charUuid, "ffe1")) {//保存ffe1的characteristics
                                    sharedPreferencesUtil.setCharFfe1(charUuid);
                                    Log.e("url", "FFE1_Uuid==" + sharedPreferencesUtil.getCharFfe1());
                                }
                                if (containsString(charUuid, "ffe2")) {//保存ffe2的characteristics
                                    sharedPreferencesUtil.setCharFfe2(charUuid);
                                    Log.e("url", "FFE2_Uuid==" + sharedPreferencesUtil.getCharFfe2());
                                }
                                if (containsString(charUuid, "ffe3")) {//保存ffe3的characteristics
                                    sharedPreferencesUtil.setCharFfe3(charUuid);
                                    Log.e("url", "FFE3_Uuid==" + sharedPreferencesUtil.getCharFfe3());
                                }
                            }
                            characteristicList.add(charArra);
                        }

                        sharedPreferencesUtil.setMAC(device.getAddress());

                        BleService.getInstance().readCharacteristic(sharedPreferencesUtil.getService(), sharedPreferencesUtil.getCharFfe2());
                        BleService.getInstance().setCharacteristicNotification(sharedPreferencesUtil.getService(), sharedPreferencesUtil.getCharFfe2(), true);

                        BleService.getInstance().setOnDataAvailableListener(new BleListener.OnDataAvailableListener() {
                            @Override
                            public void onCharacteristicRead(BluetoothGatt gatt, BluetoothGattCharacteristic characteristic, int status) {
                                String add_back_data_32 = DigitalTrans.bytesToHexString(characteristic.getValue());//014f4b

                                Log.e("url", "非添加-->添加的返回值_32(未解密)====" + add_back_data_32);
                                String decrypt_add_back_data_32 = null;
                                try {
                                    decrypt_add_back_data_32 = DigitalTrans.bytesToHexString(AesEntryDetry.decrypt(characteristic.getValue()));//解密
                                } catch (Exception e) {
                                    e.printStackTrace();
                                }
                                Log.e("url", "非添加-->添加的返回值_32(解密后)====" + decrypt_add_back_data_32);
                                add_back_data = decrypt_add_back_data_32.substring(decrypt_add_back_data_32.length() - 32, decrypt_add_back_data_32.length() - 26);//014f4b
                                Log.e("url", "add_back_data====" + add_back_data);
                                Log.e("url", "phone====" + sharedPreferencesUtil.getPhone());

                                if (add_back_data.equals("014f4b")) {
                                    mActivity.runOnUiThread(new Runnable() {
                                        @Override
                                        public void run() {
                                            tv_scanBle.setText("停止添加");
                                            viewHolder.tv_add_noadd.setText("添加模式");
                                            viewHolder.img_enter.setVisibility(View.VISIBLE);

                                            if (deviceName.contains("Gemini")) {
                                                type[0] = "door";
                                            }

                                            Log.e("url", " type[0] ====" +  type[0] );

                                            ble_db = bleSQLiteOpenHelper.getWritableDatabase();
                                            ContentValues values = new ContentValues();

                                            //游标查询每条数据
                                            Cursor cursor = ble_db.query("Ble_Table", null, "mac=?", new String[]{mac}, null, null, null);
                                            if (cursor.moveToNext()) {
                                                int count = cursor.getInt(0);
                                                if (count > 0) {//数据存在
                                                    Log.e("url", "蓝牙用户存在，进行更新！");
                                                    values.put("ble_name", deviceName);
                                                    values.put("electricity", 100);
                                                    values.put("unlock_method", "auto_unlock");
                                                    values.put("unlock_distance", "2");
                                                    values.put("is_invalid", "false");
                                                    ble_db.update("Ble_Table", values, "mac=?", new String[]{mac});
                                                }
                                            } else {
                                                values.put("ble_name", deviceName);
                                                values.put("mac", mac);
                                                values.put("type", type[0]);
                                                values.put("electricity", 100);
                                                values.put("unlock_method", "auto_unlock");
                                                values.put("unlock_distance", "2");
                                                values.put("admin_id", "0");
                                                values.put("admin_type", "0");
                                                values.put("admin_phone", sharedPreferencesUtil.getPhone());
                                                values.put("connect_music", "0");
                                                values.put("unlock_music", "0");
                                                values.put("connect_music_on_off", "off");
                                                values.put("unlock_music_on_off", "off");
                                                values.put("service", sharedPreferencesUtil.getService());
                                                values.put("char_ffe1", sharedPreferencesUtil.getCharFfe1());
                                                values.put("char_ffe2", sharedPreferencesUtil.getCharFfe2());
                                                values.put("char_ffe3", sharedPreferencesUtil.getCharFfe3());
                                                values.put("is_invalid", "false");

                                                ble_db.insert("Ble_Table", null, values);

                                                cursor.close();
                                                ble_db.close();
                                            }
                                        }
                                    });
                                }
                            }

                            @Override
                            public void onCharacteristicChanged(BluetoothGatt gatt, BluetoothGattCharacteristic characteristic) {
                                String add_back_data_32 = DigitalTrans.bytesToHexString(characteristic.getValue());//014f4b

                                Log.e("url", "onCharacteristicChanged_非添加-->添加的返回值_32(未解密)====" + add_back_data_32);
                            }

                            @Override
                            public void onDescriptorRead(BluetoothGatt gatt, BluetoothGattDescriptor descriptor, int status) {

                            }
                        });
                    }
                }
            });

            convertView.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    Log.e("url", "mDeviceAddress==" + mac);

                    if (viewHolder.tv_add_noadd.getText().toString().equals("添加模式")) {
                        ble_db = bleSQLiteOpenHelper.getWritableDatabase();
                        Cursor cursor2 = ble_db.query("Ble_Table", null, "mac=?", new String[]{mac}, null, null, null);

                        if (cursor2.moveToNext()) {
                            int count = cursor2.getInt(0);
                            final String admin_phone = cursor2.getString(9);
                            Log.e("url", "admin_phone==" + admin_phone);
                            if (count > 0) {
                                handleString = "0000000000000000000";
                                byte[] head = new byte[]{0x01};//固定的前面只是0x01
                                phoneStringBuffer = new StringBuffer(admin_phone);//追加手机号码
                                phoneStringBuffer.append(handleString);

                                String phone = String.valueOf(phoneStringBuffer);
                                byte[] body = DigitalTrans.hex2byte(phone);//十六进制串转化为byte数组
                                byte[] all = DigitalTrans.byteMerger(head, body);//把头部和手机号码加在一起
                                Log.e("url", "all==" + all.length);
                                byte[] encrypt_all = null;
                                //加密
                                try {
                                    encrypt_all = AesEntryDetry.encrypt(all);//加密
                                } catch (Exception e) {
                                    e.printStackTrace();
                                }

                                if (sharedPreferencesUtil.getService() != null) {
                                    BleService.getInstance().writeCharacteristic(sharedPreferencesUtil.getService(), sharedPreferencesUtil.getCharFfe1(), encrypt_all);
                                    BleService.getInstance().setCharacteristicNotification(sharedPreferencesUtil.getService(), sharedPreferencesUtil.getCharFfe2(), true);

                                } else {
                                    Toast.makeText(mActivity, "请重新连接蓝牙！", Toast.LENGTH_SHORT).show();
                                }

                                final String[] admin_id = new String[1];

                                BleService.getInstance().setOnDataAvailableListener(new BleListener.OnDataAvailableListener() {
                                    @Override
                                    public void onCharacteristicRead(BluetoothGatt gatt, BluetoothGattCharacteristic characteristic, int status) {

                                    }

                                    @Override
                                    public void onCharacteristicChanged(BluetoothGatt gatt, BluetoothGattCharacteristic characteristic) {
                                        //bdcc28b6a5e6989f0a6abc54c602815a
                                        Log.e("url", "返回值(未解密)==" + DigitalTrans.bytesToHexString(characteristic.getValue()));

                                        String decrypt_back_data=null;
                                        try {
                                            decrypt_back_data=DigitalTrans.bytesToHexString(AesEntryDetry.decrypt(characteristic.getValue()));//解密
                                        } catch (Exception e) {
                                            e.printStackTrace();
                                        }
                                        Log.e("url", "返回值(解密后)==" + decrypt_back_data);
                                        String set_success_data = decrypt_back_data.substring(decrypt_back_data.length() - 32, decrypt_back_data.length() - 30);//2f
                                        String electricity_back_data = decrypt_back_data.substring(decrypt_back_data.length() - 32, decrypt_back_data.length() - 26);//310100


                                        // 31 01 00 00000000000000000000000000
                                        if (electricity_back_data.contains("310")) {

                                            String electricity = electricity_back_data.substring(electricity_back_data.length() - 3, electricity_back_data.length());//获取电量值
                                            Log.e("url", "electricity==" + electricity);
                                            UpdateBleData1(sharedPreferencesUtil.getMAC(), electricity);
                                            return;
                                        }

                                        //21 11  4f00000000000000000000000000
                                        if (set_success_data.equals("21")) {

                                            mActivity.runOnUiThread(new Runnable() {
                                                @Override
                                                public void run() {
                                                    tv_scanBle.setText("添加设备");
                                                }
                                            });

                                            String data = decrypt_back_data.substring(decrypt_back_data.length() - 32, decrypt_back_data.length() - 28);//2111
                                            String admin_type = data.substring(data.length() - 2, data.length() - 0);//获取手机号码类型（11管理员，21普通用户）
                                            Log.e("url", "admin_type==" + admin_type);
                                            UpdateBleData3(mac, admin_type);
                                            return;
                                        }

                                        //2f 00 11 f2  ffa000000004005d150b0004
                                        if (set_success_data.equals("2f")) {

                                            mActivity.runOnUiThread(new Runnable() {
                                                @Override
                                                public void run() {
                                                    tv_scanBle.setText("添加设备");
                                                }
                                            });

                                            String data = decrypt_back_data.substring(decrypt_back_data.length() - 32, decrypt_back_data.length() - 24);
                                            admin_id[0] = data.substring(data.length() - 6, data.length() - 4);//获取手机号码id(00,01,02)
                                            String admin_type = data.substring(data.length() - 4, data.length() - 2);//获取手机号码类型（10管理员，20普通用户）
                                            Log.e("url", "admin_type==" + admin_type);
                                            if (admin_id[0].equals("00")) {
                                                user_type = "10";
                                            } else {
                                                user_type = "20";
                                            }

                                            Log.e("url", "   （2f 00 00 crc中）admin_id==" + admin_id[0]);
                                            UpdateBleData2(mac, admin_id[0], admin_type);
//                                            et_phone.setText("");

                                            return;
                                        }
                                    }

                                    @Override
                                    public void onDescriptorRead(BluetoothGatt gatt, BluetoothGattDescriptor descriptor, int status) {
                                    }
                                });
                            }
                        }
                        cursor2.close();
                        ble_db.close();
                    }
                }

            });

            return convertView;
        }

        class ViewHolder {
            TextView tv_ble_name, tv_mac, tv_add_noadd;
            ImageView img_enter;
        }
    }

    /**
     * 获取蓝牙数据库中的一些参数
     */
    private String GetPhone(String mac) {
        ble_db = bleSQLiteOpenHelper.getWritableDatabase();
        String phone = null;

        //游标查询每条数据
        Cursor cursor = ble_db.query("Ble_Table", null, "mac=?", new String[]{mac}, null, null, null);
        if (cursor.moveToNext()) {
            sharedPreferencesUtil.setBLENAME(cursor.getString(1));//蓝牙名称
            sharedPreferencesUtil.setUnlockmethod(cursor.getString(5));//开锁方式
            sharedPreferencesUtil.setAutoopendistance(cursor.getString(6));//自动开锁的距离
            user_type = cursor.getString(8);//用户类型（10还是20）
            sharedPreferencesUtil.setPhoneadd(cursor.getString(8));//下发手机号码后面加的值 11 或者21
            phone = cursor.getString(9);//用户手机
            sharedPreferencesUtil.setConnectMusic(cursor.getString(10));
            sharedPreferencesUtil.setUnlockMusic(cursor.getString(11));
            sharedPreferencesUtil.setConnectMusicSwitch(cursor.getString(12));
            sharedPreferencesUtil.setUnlockMusicSwitch(cursor.getString(13));
            Log.e("url", "unlock_method==" + sharedPreferencesUtil.getUnlockmethod() +
                    "   auto_open_distance==" + sharedPreferencesUtil.getAutoopendistance() +
                    "  user_type==" + user_type + "  phone==" + phone +
                    "   connect_music==" + sharedPreferencesUtil.getConnectMusic() +
                    "   unlock_music==" + sharedPreferencesUtil.getUnlockMusic() +
                    "   connect_music_switch==" + sharedPreferencesUtil.getConnectMusicSwitch() +
                    "   unlock_music_switch==" + sharedPreferencesUtil.getUnlockMusicSwitch());
        }else {
            sharedPreferencesUtil.setConnectMusic("");
            sharedPreferencesUtil.setUnlockMusic("");
            sharedPreferencesUtil.setConnectMusicSwitch("off");
            sharedPreferencesUtil.setUnlockMusicSwitch("off");
        }

        cursor.close();
        ble_db.close();
        return phone;
    }

    /**
     * 扫描蓝牙后的回调函数
     */
    private BluetoothAdapter.LeScanCallback mLeScanCallback = new BluetoothAdapter.LeScanCallback() {

        @Override
        public void onLeScan(final BluetoothDevice device, final int rssi, final byte[] scanRecord) {
            mActivity.runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    ParsedAd parsedAd = parseData(scanRecord);

                    Log.e("url", "FinfFg_ parsedAd.flags==" + parsedAd.flags);
                    Log.e("url", " FinfFg_ parsedAd.uuids==" + parsedAd.uuids);
                    Log.e("url", " FinfFg_ parsedAd.localName==" + parsedAd.localName);
                    Log.e("url", " FinfFg_ parsedAd.manufacturer==" + parsedAd.manufacturer);

                    Log.e("url", "device_mac==" + device.getAddress().toString());
                    Log.e("url", "rssi==" + rssi);
                    Log.e("url", " EquipMentFg.scanBleList.size111111==" + EquipMentFg.scanBleList.size());
                    if (!EquipMentFg.bluetoothDeviceList.contains(device)) {
                        try {
                            if (parsedAd.localName.toString().contains("Gemini") || parsedAd.localName.toString().contains("Hemini")) {
                                EquipMentFg.bluetoothDeviceList.add(device);
                                EquipMentFg.scanBle.setBluetoothDeviceList(EquipMentFg.bluetoothDeviceList);
                                EquipMentFg.scanBle.setBle_name(parsedAd.localName.toString());
                                EquipMentFg.scanBle.setBle_rssi(rssi);
                                EquipMentFg.scanBleList.add(EquipMentFg.scanBle);
                            }
                        } catch (NullPointerException e) {
                            e.printStackTrace();
                        }

                        Log.e("url", " EquipMentFg.scanBleList.size22222==" + EquipMentFg.scanBleList.size());
                        RSSI = rssi;

                        //全部
                        if (type.equals("all")) {
                            try {
                                if (parsedAd.localName.toString().contains("Gemini") || parsedAd.localName.toString().contains("Hemini")) {
                                    bleDataList.add(EquipMentFg.scanBle);
                                }
                            } catch (NullPointerException e) {
                                e.printStackTrace();
                            }
                        }

                        //门锁
                        if (type.equals("door")) {
                            try {
                                if (parsedAd.localName.toString().contains("Gemini")) {
                                    bleDataList.add(EquipMentFg.scanBle);
                                }
                            } catch (NullPointerException e) {
                                e.printStackTrace();
                            }
                        }

                        //挂锁
                        if (type.equals("hand")) {
                            try {
                                if (device.getName().toString().contains("Hemini")) {
                                    bleDataList.add(EquipMentFg.scanBle);
                                }
                            } catch (NullPointerException e) {
                                e.printStackTrace();
                            }
                        }
                    }
                    Log.e("url", " EquipMentFg.scanBleList.size33333==" + EquipMentFg.scanBleList.size());
                    mLeDeviceListAdapter.notifyDataSetChanged();
                }
            });
        }
    };

    // Code to manage Service lifecycle.
    private final ServiceConnection mServiceConnection = new ServiceConnection() {
        @Override
        public void onServiceConnected(ComponentName componentName, IBinder service) {
            mBleService = ((BleService.LocalBinder) service).getService();
            if (!mBleService.initialize()) {
                Log.e("url", "Unable to initialize Bluetooth");
            }
            // Automatically connects to the device upon successful start-up initialization.
//            mBleService.connect(mDeviceAddress);
        }

        @Override
        public void onServiceDisconnected(ComponentName componentName) {
            mBleService = null;
            mIsBind = false;
        }
    };

    private BroadcastReceiver bleReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            final String action = intent.getAction();
            if (intent.getAction().equals(BleService.ACTION_BLUETOOTH_DEVICE)) {
                String tmpDevName = intent.getStringExtra("name");
                String tmpDevAddress = intent.getStringExtra("address");
                Log.i("url", "name: " + tmpDevName + ", address: " + tmpDevAddress);
                HashMap<String, Object> deviceMap = new HashMap<>();
            } else if (intent.getAction().equals(BleService.ACTION_GATT_CONNECTED)) {
                Log.e("url", "连接成功！");
                mActivity.invalidateOptionsMenu();

            } else if (intent.getAction().equals(BleService.ACTION_GATT_DISCONNECTED)) {
            } else if (intent.getAction().equals(BleService.ACTION_SCAN_FINISHED)) {
            } else if (BleService.ACTION_DATA_AVAILABLE.equals(action)) {
                Log.e("url", "连接成功后的返回值==" + intent.getStringExtra(BleService.EXTRA_DATA));
            }
        }
    };

    private static IntentFilter makeIntentFilter() {
        final IntentFilter intentFilter = new IntentFilter();
        intentFilter.addAction(BleService.ACTION_BLUETOOTH_DEVICE);
        intentFilter.addAction(BleService.ACTION_GATT_CONNECTED);
        intentFilter.addAction(BleService.ACTION_GATT_DISCONNECTED);
        intentFilter.addAction(BleService.ACTION_SCAN_FINISHED);
        return intentFilter;
    }

    /**
     * 判断字符串是否包含一些字符 contains
     */
    public boolean containsString(String src, String dest) {
        boolean flag = false;
        if (src.contains(dest)) {
            flag = true;
        }
        return flag;
    }

    /**
     * 获取到电量后，更新蓝牙数据库
     *
     * @param mac
     * @param electricity
     */
    private void UpdateBleData1(String mac, String electricity) {
        Log.e("url", "FindFg中mac==" + mac + "  electricity==" + electricity);
        ble_db = bleSQLiteOpenHelper.getWritableDatabase();
        ContentValues values = new ContentValues();

        //游标查询每条数据
        Cursor cursor = ble_db.query("Ble_Table", null, "mac=?", new String[]{mac}, null, null, null);
        if (cursor.moveToNext()) {
            int count = cursor.getInt(0);
            if (count > 0) {//数据存在
                Log.e("url", "用户存在，更新蓝牙电量！");
                values.put("electricity", Integer.parseInt(electricity));
                ble_db.update("Ble_Table", values, "mac=?", new String[]{mac});
            }
        }
        cursor.close();
        ble_db.close();
    }

    /**
     * 获取到手机用户类型后，更新数据库
     *
     * @param mac
     * @param admin_id
     * @param admin_type
     */
    private void UpdateBleData2(String mac, String admin_id, String admin_type) {
        Log.e("url", "mac==" + mac + "  admin_id==" + admin_id + "  admin_type==" + admin_type);
        ble_db = bleSQLiteOpenHelper.getWritableDatabase();
        ContentValues values = new ContentValues();

        //游标查询每条数据
        Cursor cursor = ble_db.query("Ble_Table", null, "mac=?", new String[]{mac}, null, null, null);
        if (cursor.moveToNext()) {
            int count = cursor.getInt(0);
            if (count > 0) {//数据存在
                Log.e("url", "用户存在，更新手机用户类型！");
                values.put("admin_id", admin_id);
                values.put("admin_type", admin_type);
                ble_db.update("Ble_Table", values, "mac=?", new String[]{mac});
            }
        }
        cursor.close();
        ble_db.close();
    }

    /**
     * app被删后，重新连接，更新数据库
     *
     * @param mac
     * @param admin_type
     */
    private void UpdateBleData3(String mac,String admin_type) {
        Log.e("url", "mac==" + mac + "  admin_type==" + admin_type);
        ble_db = bleSQLiteOpenHelper.getWritableDatabase();
        ContentValues values = new ContentValues();

        //游标查询每条数据
        Cursor cursor = ble_db.query("Ble_Table", null, "mac=?", new String[]{mac}, null, null, null);
        if (cursor.moveToNext()) {
            int count = cursor.getInt(0);
            if (count > 0) {//数据存在
                Log.e("url", "用户存在，更新手机用户类型！");
                values.put("admin_type", admin_type);
                ble_db.update("Ble_Table", values, "mac=?", new String[]{mac});
            }
        }
        cursor.close();
        ble_db.close();
    }

    /**
     * 初始化键盘普通用户列表
     */
    private void AddKeyboardUserTableData() {
        disposable_user_db = disposableUserSQLiteOpenHelper.getWritableDatabase();
        ContentValues values = new ContentValues();

        //游标查询每条数据
        Cursor cursor = disposable_user_db.query("Disposable_User_Table", null, "mac=? and type=?", new String[]{sharedPreferencesUtil.getMAC(), "ordinary"}, null, null, null);
        if (cursor.moveToNext()) {
            int count = cursor.getInt(0);
            if (count > 0) {//数据存在
                Log.e("url", "普通用户已经存在！");
            }
        } else {
            for (int i = 0; i <= 9; i++) {
                values.put("mac", sharedPreferencesUtil.getMAC());
                values.put("password", "0");
                values.put("type", "ordinary");
                values.put("user_id", "0" + i);
                disposable_user_db.insert("Disposable_User_Table", null, values);
            }
        }
        cursor.close();
        disposable_user_db.close();
    }

    /**
     * 初始化手机普通用户列表
     */
    private void AddPhoneUserTableData() {
        phone_user_db = phoneUserSQLiteOpenHelper.getWritableDatabase();
        ContentValues values = new ContentValues();

        //游标查询每条数据
        Cursor cursor = phone_user_db.query("Phone_User_Table", null, "mac=?", new String[]{sharedPreferencesUtil.getMAC()}, null, null, null);
        if (cursor.moveToNext()) {
            int count = cursor.getInt(0);
            if (count > 0) {//数据存在
                Log.e("url", "手机普通用户已经存在！");
            }
        } else {
            Log.e("url", "添加手机普通用户");
            for (int i = 1; i <= 9; i++) {
                values.put("mac", sharedPreferencesUtil.getMAC());
                values.put("user_type", "21");
                values.put("user_id", "0" + i);
                values.put("user_num", "00000000000");
                values.put("str_user", "0");
                phone_user_db.insert("Phone_User_Table", null, values);
            }
        }
        cursor.close();
        phone_user_db.close();
    }

    /**
     * 主要是添加键盘管理员用户
     *
     * @param user_id
     */
    private void AddKeyboardAdminUserTableData(String user_id) {
        disposable_user_db = disposableUserSQLiteOpenHelper.getWritableDatabase();
        ContentValues values = new ContentValues();

        //游标查询每条数据
        Cursor cursor = disposable_user_db.query("Disposable_User_Table", null, "mac=? and type=?", new String[]{sharedPreferencesUtil.getMAC(), "admin"}, null, null, null);
        if (cursor.moveToNext()) {
            int count = cursor.getInt(0);
            if (count > 0) {//数据存在
                Log.e("url", user_id + "该用户已经存在！");
            }
        } else {
            values.put("mac", sharedPreferencesUtil.getMAC());
            values.put("password", "123456");
            values.put("type", "admin");
            values.put("user_id", "9999");
            disposable_user_db.insert("Disposable_User_Table", null, values);
        }
        cursor.close();
        disposable_user_db.close();
    }

    /**
     * 主要是添加键盘普通用户
     */
    private void AddDisposableUserTableData() {
        disposable_user_db = disposableUserSQLiteOpenHelper.getWritableDatabase();
        ContentValues values = new ContentValues();

        //游标查询每条数据
        Cursor cursor = disposable_user_db.query("Disposable_User_Table", null, "mac=? and type=?", new String[]{sharedPreferencesUtil.getMAC(), "once"}, null, null, null);
        if (cursor.moveToNext()) {
            int count = cursor.getInt(0);
            if (count > 0) {//数据存在
                Log.e("url", "期限用户已经存在！");
            }
        } else {
            for (int i = 1; i <= 4; i++) {
                for (int j = 0; j <= 9; j++) {
                    if (i == 1) {//单次用户
                        values.put("mac", sharedPreferencesUtil.getMAC());
                        values.put("password", "bbbbbbbbbbbb");
                        values.put("type", "once");
                        values.put("user_id", "0" + j);
                        values.put("start_time", "0");
                        values.put("end_time", "0");
                        values.put("str_user", "0");
                    }
                    if (i == 2) {//一天用户
                        values.put("mac", sharedPreferencesUtil.getMAC());
                        values.put("password", "bbbbbbbbbbbb");
                        values.put("type", "day");
                        values.put("user_id", "0" + j);
                        values.put("start_time", "0");
                        values.put("end_time", "0");
                        values.put("str_user", "0");
                    }
                    if (i == 3) {//一周用户
                        values.put("mac", sharedPreferencesUtil.getMAC());
                        values.put("password", "bbbbbbbbbbbb");
                        values.put("type", "week");
                        values.put("user_id", "0" + j);
                        values.put("start_time", "0");
                        values.put("end_time", "0");
                        values.put("str_user", "0");
                    }
                    if (i == 4) {//一个月用户
                        values.put("mac", sharedPreferencesUtil.getMAC());
                        values.put("password", "bbbbbbbbbbbb");
                        values.put("type", "month");
                        values.put("user_id", "0" + j);
                        values.put("start_time", "0");
                        values.put("end_time", "0");
                        values.put("str_user", "0");
                    }
                    disposable_user_db.insert("Disposable_User_Table", null, values);
                }
            }
        }
        cursor.close();
        disposable_user_db.close();
    }

    /**
     * 解析获取扫描到蓝牙后返回的广播包
     * @param adv_data
     * @return
     */
    public static ParsedAd parseData(byte[] adv_data) {
        ParsedAd parsedAd = new ParsedAd();
        ByteBuffer buffer = ByteBuffer.wrap(adv_data).order(ByteOrder.LITTLE_ENDIAN);
        Log.e("url","buffer=="+buffer);
        while (buffer.remaining() > 2) {
            byte length = buffer.get();
            Log.e("url","length=="+length);
            if (length == 0)
                break;

            byte type = buffer.get();
            length -= 1;
            switch (type) {
                case 0x01: // Flags
                    parsedAd.flags = buffer.get();
                    length--;
                    break;
                case 0x02: // Partial list of 16-bit UUIDs
                case 0x03: // Complete list of 16-bit UUIDs
                case 0x14: // List of 16-bit Service Solicitation UUIDs
                    while (length >= 2) {
                        parsedAd.uuids.add(UUID.fromString(String.format(
                                "%08x-0000-1000-8000-00805f9b34fb", buffer.getShort())));
                        length -= 2;
                    }
                    break;
                case 0x04: // Partial list of 32 bit service UUIDs
                case 0x05: // Complete list of 32 bit service UUIDs
                    while (length >= 4) {
                        parsedAd.uuids.add(UUID.fromString(String.format(
                                "%08x-0000-1000-8000-00805f9b34fb", buffer.getInt())));
                        length -= 4;
                    }
                    break;
                case 0x06: // Partial list of 128-bit UUIDs
                case 0x07: // Complete list of 128-bit UUIDs
                case 0x15: // List of 128-bit Service Solicitation UUIDs
                    while (length >= 16) {
                        long lsb = buffer.getLong();
                        long msb = buffer.getLong();
                        parsedAd.uuids.add(new UUID(msb, lsb));
                        length -= 16;
                    }
                    break;
                case 0x08: // Short local device name
                case 0x09: // Complete local device name
                    byte sb[] = new byte[length];
                    buffer.get(sb, 0, length);
                    length = 0;
                    parsedAd.localName = new String(sb).trim();
                    break;
                case (byte) 0xFF: // Manufacturer Specific Data
                    parsedAd.manufacturer = buffer.getShort();
                    length -= 2;
                    break;
                default: // skip
                    break;
            }
            if (length > 0) {
                Log.e("url","length2222222=="+length);
                buffer.position(buffer.position() + length);
            }
        }
        return parsedAd;
    }

    /**
     * 读取扫描蓝牙的线程
     */
    Thread updateUI = new Thread() {

        @Override
        public void run() {
            // TODO Auto-generated method stub
            super.run();
            while (isUpdate) {
                try {
                    sleep(100);
                } catch (InterruptedException e) {
                    // TODO Auto-generated catch block
                    e.printStackTrace();
                }
                mActivity.runOnUiThread(new Runnable() {
                    @Override
                    public void run() {

                        if (BleService.getInstance().isconnect == false) {
                            tv_scanBle.setText("添加设备");
                            Log.e("url", "updateUI");
                            bleDataList.clear();
//                            for (int i = 0; i < EquipMentFg.scanBleList.size(); i++) {
//                                Log.e("url", "EquipMentFg.scanBleList。size==" + EquipMentFg.scanBleList.size());
//                                try {
//                                    if (EquipMentFg.scanBleList.size() > 0) {
//                                        if (EquipMentFg.scanBleList.get(i).getBle_name().toString().contains("Gemini")) {
//                                            Log.e("url", "EquipMentFg.scanBleList。size222222==" + EquipMentFg.scanBleList.size());
//                                            if (EquipMentFg.scanBleList.size() > 0) {
//                                                Log.e("url", "EquipMentFg.scanBleList。name==" + EquipMentFg.scanBleList.get(i).getBle_name().toString());
//                                                bleDataList.add(EquipMentFg.scanBleList.get(i));
//                                            }
//                                        }
//                                    }
//                                } catch (NullPointerException e) {
//                                    e.printStackTrace();
//                                }
//                            }
                            mLeDeviceListAdapter.clear();
                            mLeDeviceListAdapter.notifyDataSetChanged();
                        }
                    }
                });
            }
        }
    };

    /**
     * 解绑服务
     */
    private void doUnBindService() {
        if (mIsBind) {
            mActivity.unbindService(mServiceConnection);
            mBleService = null;
            mIsBind = false;
        }
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        doUnBindService();
        mActivity.unregisterReceiver(bleReceiver);
        if (mBleService.isScanning()) {
//            mBleService.scanLeDevice(false);
            return;
        }
        //停止监听screen状态
//        mScreenObserver.stopScreenStateUpdate();
    }

    @Override
    public void onPause() {
        super.onPause();
        Log.e("url","FindFg_onPause");
        mLeDeviceListAdapter.clear();
        bleDataList.clear();
        mLeDeviceListAdapter.notifyDataSetChanged();

        if (!sharedPreferencesUtil.getMAC().equals("")) {
            AddPhoneUserTableData();//初始化手机普通用户列表
            AddKeyboardAdminUserTableData("admin");//初始化键盘用户列表（管理员用户）
            AddKeyboardUserTableData();//初始化键盘用户列表（普通用户）
            AddDisposableUserTableData();//初始化期限用户列表
        }
    }
}
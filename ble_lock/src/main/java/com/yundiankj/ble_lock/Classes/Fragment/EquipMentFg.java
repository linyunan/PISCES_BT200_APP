package com.yundiankj.ble_lock.Classes.Fragment;

import android.Manifest;
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
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.os.Message;
import android.support.annotation.NonNull;
import android.support.v4.app.ActivityCompat;
import android.support.v4.app.Fragment;
import android.support.v4.content.ContextCompat;
import android.text.format.DateUtils;
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

import com.handmark.pulltorefresh.library.PullToRefreshBase;
import com.handmark.pulltorefresh.library.PullToRefreshListView;
import com.junkchen.blelib.BleListener;
import com.yundiankj.ble_lock.Classes.Activity.BLEDetails;
import com.yundiankj.ble_lock.Classes.Activity.MainActivity;
import com.yundiankj.ble_lock.Classes.Adapter.PopTypeAdapter;
import com.yundiankj.ble_lock.Classes.Model.ParsedAd;
import com.yundiankj.ble_lock.Classes.Model.ScanBle;
import com.yundiankj.ble_lock.R;
import com.yundiankj.ble_lock.Resource.AesEntryDetry;
import com.yundiankj.ble_lock.Resource.BatteryView;
import com.yundiankj.ble_lock.Resource.BleService;
import com.yundiankj.ble_lock.Resource.Const;
import com.yundiankj.ble_lock.Resource.DigitalTrans;
import com.yundiankj.ble_lock.Resource.MyGattAttributes;
import com.yundiankj.ble_lock.Resource.SQLite.BleSQLiteOpenHelper;
import com.yundiankj.ble_lock.Resource.SQLite.DisposableUserSQLiteOpenHelper;
import com.yundiankj.ble_lock.Resource.SQLite.PhoneUserSQLiteOpenHelper;
import com.yundiankj.ble_lock.Resource.SQLite.TMCardUserSQLiteOpenHelper;
import com.yundiankj.ble_lock.Resource.SQLite.UnlockRecordSQLiteOpenHelper;
import com.yundiankj.ble_lock.Resource.SQLite.WaitSendDataSQLiteOpenHelper;
import com.yundiankj.ble_lock.Resource.ScreenObserver;
import com.yundiankj.ble_lock.Resource.SharedPreferencesUtil;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Timer;
import java.util.TimerTask;
import java.util.UUID;

/**
 * Created by hong on 2016/7/18.
 * <p/>
 * 设备Fg
 */
public class EquipMentFg extends Fragment {

    private static final String TAG = MainActivity.class.getSimpleName();
    private View rootView;
    private Activity mActivity;
    private SharedPreferencesUtil sharedPreferencesUtil;
    private LinearLayout lly_had_connect_equipment;
    private RelativeLayout rly_had_connect_equipment;
    private TextView tv_had_connect_type;
    private ImageView img_down_up;
    private View main_darkview;//弹出popWindow的阴影
    private PullToRefreshListView plv_had_connect_equipment;
    private CommonAdapter deviceAdapter;//设备列表的适配器
    public static List<Map<String, Object>> deviceList;//存放设备列表

    //连接过的设备的popWindow
    private PopupWindow pop_had_connect_equipment;
    private ListView lv_type;
    private PopTypeAdapter popTypeAdapter;
    private String[] typeList = {"全部", "门锁", "挂锁"};
    private String type = "all";//全部（all）,门锁（door）,挂锁(hand)
    private boolean is_show = false;//判断连接过的设备的popWindow的展示与收

    //长按删除的popupWindow
    private PopupWindow pop_delete;
    private TextView tv_ok, tv_cancel;

    //Constant
    public static final int SERVICE_BIND = 1;
    public static final int SERVICE_SHOW = 2;
    public static final int REQUEST_CODE_ACCESS_COARSE_LOCATION = 1;

    //蓝牙
    private BleService mBleService;
    private boolean mScanning;
    private BluetoothAdapter mBluetoothAdapter;
    private boolean mIsBind;
    private String connDeviceName;
    private String connDeviceAddress;
    private int RSSI;
    private static final int REQUEST_ENABLE_BT = 1;
    private static final long SCAN_PERIOD = 900;// 1秒后停止查找搜索.
    public static List<BluetoothDevice> bluetoothDeviceList;//存放蓝牙信息
    public static List<ScanBle> scanBleList;//存放蓝牙信息+rssi
    public static ScanBle scanBle;

    //方便上传用户信息的时crc检验
    public String str_phone_user_infos = "";  //手机用户列表组合
    public String str_admin_pw = "";  //管理员密码列表组合
    public String str_ordinary_user_info = "";//键盘普通用户列表组合
    public String str_tm_card_user_info = "";//TM卡用户列表组合
    public String str_unlock_record_info = "";//开锁记录列表组合
    public String str_one_time_pw_info = "";//期限密码中的单次密码列表组合

    private ScreenObserver mScreenObserver;//监听屏幕

    //数据库
    private BleSQLiteOpenHelper bleSQLiteOpenHelper;//蓝牙数据库
    private SQLiteDatabase ble_db;
    private PhoneUserSQLiteOpenHelper phoneUserSQLiteOpenHelper;//手机用户数据库
    private SQLiteDatabase phone_user_db;
    private TMCardUserSQLiteOpenHelper tmCardUserSQLiteOpenHelper;//TM卡用户数据库
    private SQLiteDatabase tm_card_db;
    private UnlockRecordSQLiteOpenHelper unlockRecordSQLiteOpenHelper; //开锁记录数据库
    private SQLiteDatabase unlock_record_db;
    private DisposableUserSQLiteOpenHelper disposableUserSQLiteOpenHelper;//期限用户数据库
    private SQLiteDatabase disposable_user_db;
    private WaitSendDataSQLiteOpenHelper waitSendDataSQLiteOpenHelper; //未发送成功的数据库
    private SQLiteDatabase wait_send_data_db;

    private List<Map<String, Object>> bleData;//定义一个列表储存 蓝牙数据
    private List<Map<String, Object>> waitSendData;//定义一个列表储存 未发送成功的数据

    private String user_type;
    private StringBuffer phoneStringBuffer;
    public String handleString = "000";//下发手机号码后面追加的值

    private boolean isReadRssi = false;//用来一直下发rssi
    private boolean isScanBle = false;//用来一直扫描蓝牙

    private List<BluetoothGattService> gattServiceList;
    private List<String> serviceList;//存放蓝牙的服务uuid
    private List<String[]> characteristicList;//存放蓝牙的特征值uuid

    private boolean is_user = false;
    private String upload_flag = "flag";
    private String str_02534a = "", str_164b4f = "0", str_1f4b4f = "", str_safe_distance = "";


    private ServiceConnection serviceConnection = new ServiceConnection() {
        @Override
        public void onServiceConnected(ComponentName name, IBinder service) {
            mBleService = ((BleService.LocalBinder) service).getService();
            if (mBleService != null) mHandler.sendEmptyMessage(SERVICE_BIND);
            if (mBleService.initialize()) {
                if (mBleService.enableBluetooth(true)) {
                    verifyIfRequestPermission();
                    Toast.makeText(mActivity, "Bluetooth was opened", Toast.LENGTH_SHORT).show();
                }
            } else {
                Toast.makeText(mActivity, "not support Bluetooth", Toast.LENGTH_SHORT).show();
            }
        }

        @Override
        public void onServiceDisconnected(ComponentName name) {
            mBleService = null;
            mIsBind = false;
        }
    };

    private Handler mHandler = new Handler() {
        @Override
        public void handleMessage(Message msg) {
            switch (msg.what) {
                case SERVICE_BIND:
//                    setBleServiceListener();
                    break;
                case 2110:
                    handleString = "010";
                    break;
            }
        }
    };

    // 当创建fragment的UI被初始化时调用。
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        if (rootView == null) {
            rootView = inflater.inflate(R.layout.equipment_fragment, container, false);
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
        Log.d("url", "onActivityCreated");

        init();
        initAdapter();
    }

    /**
     * 检查设备上是否支持蓝牙
     */
    private void initBletooth() {
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

        //初始化
        scanBleList = new ArrayList<ScanBle>();
        scanBle = new ScanBle();
        bluetoothDeviceList = new ArrayList<BluetoothDevice>();
    }

    private void init() {
        sharedPreferencesUtil = SharedPreferencesUtil.getInstance(mActivity);
        bleSQLiteOpenHelper = new BleSQLiteOpenHelper(mActivity, "BleDatabase.db", null, 2);
        bleSQLiteOpenHelper.getWritableDatabase();//创建连接过的蓝牙表
        phoneUserSQLiteOpenHelper = new PhoneUserSQLiteOpenHelper(mActivity, "UserDatabase.db", null, 2);
        phoneUserSQLiteOpenHelper.getWritableDatabase();//创建用户表
        disposableUserSQLiteOpenHelper = new DisposableUserSQLiteOpenHelper(mActivity, "DisposableUserDatabase.db", null, 2);
        disposableUserSQLiteOpenHelper.getWritableDatabase();//创建期限用户表
        tmCardUserSQLiteOpenHelper = new TMCardUserSQLiteOpenHelper(mActivity, "TMCardUserDatabase.db", null, 2);
        tmCardUserSQLiteOpenHelper.getWritableDatabase();//创建TM卡用户表
        unlockRecordSQLiteOpenHelper = new UnlockRecordSQLiteOpenHelper(mActivity, "UnlockRecordDatabase.db", null, 2);
        unlockRecordSQLiteOpenHelper.getWritableDatabase();//创建开锁记录表
        waitSendDataSQLiteOpenHelper = new WaitSendDataSQLiteOpenHelper(mActivity, "WaitSendDataDatabase.db", null, 2);
        waitSendDataSQLiteOpenHelper.getWritableDatabase();//创建未发送成功的数据表

        lly_had_connect_equipment = (LinearLayout) mActivity.findViewById(R.id.lly_had_connect_equipment);
        rly_had_connect_equipment = (RelativeLayout) mActivity.findViewById(R.id.rly_had_connect_equipment);
        tv_had_connect_type = (TextView) mActivity.findViewById(R.id.tv_had_connect_type);
        img_down_up = (ImageView) mActivity.findViewById(R.id.img_down_up);
        main_darkview = (View) rootView.findViewById(R.id.main_darkview);
        plv_had_connect_equipment = (PullToRefreshListView) rootView.findViewById(R.id.plv_had_connect_equipment);

        if (mIsBind == false) {
            doBindService();
        }

        ScreenObserve();//监听屏幕开关
        initHadConnectEquipmentPopupWindow();//初始化连接过的设备的popupwindow

        serviceList = new ArrayList<>();
        characteristicList = new ArrayList<>();

        popTypeAdapter = new PopTypeAdapter(mActivity, typeList);//类型的适配器
        lv_type.setAdapter(popTypeAdapter);

        //连接过设备popupWindow的点击事件
        rly_had_connect_equipment.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (is_show == false) {//popupwindow收缩的时刻
                    img_down_up.setImageDrawable(getResources().getDrawable(R.mipmap.btn_up));
                    is_show = true;//展开
                    main_darkview.setVisibility(View.VISIBLE);

                    lv_type.setOnItemClickListener(new AdapterView.OnItemClickListener() {
                        public void onItemClick(AdapterView<?> arg0, View arg1, int arg2, long arg3) {
                            Log.e("url", "选择的类型==" + typeList[arg2]);
                            tv_had_connect_type.setText(typeList[arg2]);

                            if (typeList[arg2].equals("全部")) {
                                type = "all";
                            } else if (typeList[arg2].equals("门锁")) {
                                type = "door";
                            } else if (typeList[arg2].equals("挂锁")) {
                                type = "hand";
                            }

                            deviceList = getBleData(type);//从数据库中获取连接过的蓝牙列表
                            deviceAdapter.notifyDataSetChanged();//刷新界面

                            pop_had_connect_equipment.dismiss();
                            main_darkview.setVisibility(View.GONE);
                            pop_had_connect_equipment.setFocusable(false);
                        }
                    });

                    pop_had_connect_equipment.setAnimationStyle(R.style.PopupAnimation);
                    pop_had_connect_equipment.showAsDropDown(lly_had_connect_equipment, 0, 0);
                    pop_had_connect_equipment.update();
                } else {//popupwindow展开的时候
                    is_show = false;//收缩
                    pop_had_connect_equipment.dismiss();
                    main_darkview.setVisibility(View.GONE);
                    pop_had_connect_equipment.setFocusable(false);
                }
            }
        });
    }

    private void initAdapter() {
        deviceList = new ArrayList<>();
        deviceList = getBleData(type);//从数据库中获取连接过的蓝牙列表
        deviceAdapter = new CommonAdapter();
        plv_had_connect_equipment.setAdapter(deviceAdapter);
        plv_had_connect_equipment.setMode(PullToRefreshBase.Mode.PULL_FROM_START);

        //下拉刷新
        plv_had_connect_equipment.setOnRefreshListener2(new PullToRefreshBase.OnRefreshListener2<ListView>() {
            @Override
            public void onPullDownToRefresh(PullToRefreshBase<ListView> refreshView) {
                String label = DateUtils.formatDateTime(mActivity.getApplicationContext(), System.currentTimeMillis(),
                        DateUtils.FORMAT_SHOW_TIME | DateUtils.FORMAT_SHOW_DATE | DateUtils.FORMAT_ABBREV_ALL);
                refreshView.getLoadingLayoutProxy().setLastUpdatedLabel(label);
                deviceList = getBleData(type);//从数据库中获取连接过的蓝牙列表
                Log.e("url", "size==" + deviceList.size());

                plv_had_connect_equipment.postDelayed(new Runnable() {
                    @Override
                    public void run() {
                        plv_had_connect_equipment.onRefreshComplete();
                        deviceAdapter.notifyDataSetChanged();//刷新界面
                        scanLeDevice(true);
                    }
                }, 500);
            }

            @Override
            public void onPullUpToRefresh(PullToRefreshBase<ListView> refreshView) {
                String label = DateUtils.formatDateTime(mActivity.getApplicationContext(), System.currentTimeMillis(),
                        DateUtils.FORMAT_SHOW_TIME | DateUtils.FORMAT_SHOW_DATE | DateUtils.FORMAT_ABBREV_ALL);
                refreshView.getLoadingLayoutProxy().setLastUpdatedLabel(label);
            }
        });
    }

    /**
     * 已连接设备的popupwindow
     */
    private void initHadConnectEquipmentPopupWindow() {
        LayoutInflater inflater = LayoutInflater.from(mActivity);
        View layout = inflater.inflate(R.layout.pop_had_connect_equipment, null);
        lv_type = (ListView) layout.findViewById(R.id.lv_type);
        layout.invalidate();
        pop_had_connect_equipment = new PopupWindow(layout, ViewGroup.LayoutParams.FILL_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        pop_had_connect_equipment.setOnDismissListener(new PopupWindow.OnDismissListener() {
            @Override
            public void onDismiss() {
                img_down_up.setImageDrawable(getResources().getDrawable(R.mipmap.btn_down));
                is_show = false;
                main_darkview.setVisibility(View.GONE);
            }
        });
        pop_had_connect_equipment.setBackgroundDrawable(new BitmapDrawable());
        pop_had_connect_equipment.setOutsideTouchable(true);
        pop_had_connect_equipment.setFocusable(true);
    }

    /**
     * 设备列表的适配器
     */
    private class CommonAdapter extends BaseAdapter {

        @Override
        public int getCount() {
            return deviceList.size();
        }

        @Override
        public Map<String, Object> getItem(int position) {
            return deviceList.get(position);
        }

        @Override
        public long getItemId(int position) {
            return position;
        }

        @Override
        public View getView(final int position, View convertView, ViewGroup parent) {
            ViewHolder vh = null;
            if (convertView == null) {
                convertView = LayoutInflater.from(mActivity).inflate(R.layout.equipment_fragment_listitem, null);
                vh = new ViewHolder();
                vh.img_type = (ImageView) convertView.findViewById(R.id.img_type);
                vh.tv_ble_name = (TextView) convertView.findViewById(R.id.tv_ble_name);
                vh.tv_mac = (TextView) convertView.findViewById(R.id.tv_mac);
                vh.battery_view = (BatteryView) convertView.findViewById(R.id.battery_view);
                vh.tv_phone_type = (TextView) convertView.findViewById(R.id.tv_phone_type);
            } else {
                vh = (ViewHolder) convertView.getTag();
            }
            convertView.setTag(vh);
            convertView.setClickable(true);

            if (getItem(position).get("is_invalid").equals("true")) {
                vh.tv_phone_type.setText("已失效");
            } else {
                if (getItem(position).get("admin_type").toString().equals("31")) {
                    vh.tv_phone_type.setText("待删除");
                } else {
                    String permission_unlock = getItem(position).get("admin_type").toString();
                    String permission = permission_unlock.substring(permission_unlock.length() - 2, permission_unlock.length() - 1);//2111
                    Log.e("url", "permission==" + permission);
                    if (permission.equals("1")) {
                        vh.tv_phone_type.setText("管理员");
                    } else if (permission.equals("2")) {
                        vh.tv_phone_type.setText("用户");
                    }
                }
            }
            Log.e("url", "类型==" + getItem(position).get("type").toString());
            if (getItem(position).get("type").toString().equals("door")) {
                vh.img_type.setImageDrawable(getResources().getDrawable(R.mipmap.icon_door));
            } else {
                vh.img_type.setImageDrawable(getResources().getDrawable(R.mipmap.icon_lock));
            }

            vh.tv_ble_name.setText("" + getItem(position).get("ble_name").toString());

            vh.battery_view.setPower(Integer.parseInt(getItem(position).get("electricity").toString()));
            vh.tv_mac.setText(getItem(position).get("mac").toString());

            final ViewHolder finalVh = vh;
            convertView.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    MainActivity.is_scan = true;//停止扫描
                    GetPhone(getItem(position).get("mac").toString());
                    sharedPreferencesUtil.setMAC(getItem(position).get("mac").toString());
                    Log.e("url", "mac==" + getItem(position).get("mac").toString());
                    Log.e("url", "临时的mac==" + sharedPreferencesUtil.getMAC());

                    Intent intent = new Intent(mActivity, BLEDetails.class);
                    intent.putExtra("ble_name", finalVh.tv_ble_name.getText().toString());
                    intent.putExtra("mac", getItem(position).get("mac").toString());
                    intent.putExtra("type", getItem(position).get("admin_id").toString());
                    intent.putExtra("user_type", getItem(position).get("admin_type").toString());
                    startActivity(intent);
                }
            });

            convertView.setOnLongClickListener(new View.OnLongClickListener() {//长按删除
                @Override
                public boolean onLongClick(View v) {
                    String mac = getItem(position).get("mac").toString();
                    String admin_id = getItem(position).get("admin_id").toString();
                    String admin_type = getItem(position).get("admin_type").toString();
                    String admin_phone = getItem(position).get("admin_phone").toString();
                    String is_invalid = getItem(position).get("is_invalid").toString();
                    Log.e("url", "position==" + position + "  mac==" + mac + "  admin_id==" + admin_id + "  admin_type==" + admin_type +
                            "  admin_phone==" + admin_phone + "   is_invalid" + is_invalid);

                    initDeletePopWindow(position, mac, admin_id, admin_phone, admin_type, is_invalid);

                    WindowManager.LayoutParams lp;
                    //设置背景颜色变暗
                    lp = mActivity.getWindow().getAttributes();
                    lp.alpha = 0.5f;
                    mActivity.getWindow().setAttributes(lp);
                    pop_delete.setAnimationStyle(R.style.PopupAnimation);
                    pop_delete.showAtLocation(rootView.findViewById(R.id.lly_main), Gravity.CENTER | Gravity.CENTER_HORIZONTAL, 0, 0);
                    pop_delete.update();

                    return false;
                }
            });
            return convertView;
        }

        private class ViewHolder {
            private ImageView img_type;
            private TextView tv_ble_name, tv_mac, tv_electricity, tv_phone_type;
            private BatteryView battery_view;
        }
    }

    /**
     * 初始化长按删除popwindow
     *
     * @param position
     * @param mac
     * @param admin_id
     * @param admin_phone
     * @param admin_type
     */
    private void initDeletePopWindow(final int position, final String mac, final String admin_id, final String admin_phone, final String admin_type, final String is_invalid) {
        LayoutInflater inflater = LayoutInflater.from(mActivity);
        View sounds_layout = inflater.inflate(R.layout.pop_delete, null);
        tv_ok = (TextView) sounds_layout.findViewById(R.id.tv_ok);
        tv_cancel = (TextView) sounds_layout.findViewById(R.id.tv_cancel);
        sounds_layout.invalidate();
        pop_delete = new PopupWindow(sounds_layout, ViewGroup.LayoutParams.FILL_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        //实例化一个ColorDrawable颜色为半透明
        ColorDrawable dw = new ColorDrawable(0x00000000);
        //设置PopupWindow弹出窗体的背景
        pop_delete.setBackgroundDrawable(dw);
        pop_delete.setOutsideTouchable(true);
        pop_delete.setFocusable(true);
        pop_delete.setOnDismissListener(new PopupWindow.OnDismissListener() {
            @Override
            public void onDismiss() {
                WindowManager.LayoutParams lp = mActivity.getWindow().getAttributes();
                lp.alpha = 1;
                mActivity.getWindow().setAttributes(lp);
            }
        });

        Log.e("url", "admin_type==" + admin_type);
        Log.e("url", "admin_id==" + admin_id);
        tv_ok.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

                if (is_invalid.equals("true")) {//已失效的情况
                    Log.e("url", "删除成功！");
                    UpdateBleData(mac);

                    deviceList = getBleData(type);
                    pop_delete.dismiss();
                    deviceAdapter.notifyDataSetChanged();
                    Toast.makeText(mActivity, "删除成功！", Toast.LENGTH_SHORT).show();
                } else {
                    if (admin_id.equals("00") || admin_type.equals("12") || admin_type.equals("11")) {
                        Toast.makeText(mActivity, "不能删除手机管理员！", Toast.LENGTH_SHORT).show();
                        pop_delete.dismiss();
                    } else {
                        MainActivity.is_upload = true;

//                    if (!admin_type.equals("20")) {
                        byte[] head = new byte[]{0x01};//固定的前面只是0x01

                        byte[] byte_admin_id = DigitalTrans.hex2byte(admin_id);//十六进制串转化为byte数组
                        String phone_type = admin_phone + handleString;
                        Log.e("url", "phone_type==" + phone_type);
                        byte[] byte_phone = DigitalTrans.hex2byte(phone_type);//十六进制串转化为byte数组
                        byte[] byte_id_phone = DigitalTrans.byteMerger(byte_admin_id, byte_phone);//把序号，手机号码加在一起

                        short[] short_id_phone = new short[byte_id_phone.length];
                        for (int i = 0; i < byte_id_phone.length; i++) {
                            short_id_phone[i] = byte_id_phone[i];
                            Log.e("url", "short_id_phone==" + short_id_phone[i]);
                        }

                        short crc1 = 0;
                        crc1 = mBleService.appData_Crc(short_id_phone, crc1, short_id_phone.length);
                        String str_crc = Integer.toHexString(crc1);

                        if (str_crc.length() == 1) {//如果长度为1，那么DigitalTrans.hex2byte(str_crc)的时候会报错
                            str_crc = 0 + str_crc;
                        }

                        Log.e("url", "删除手机用户_str_CRC==" + str_crc);
                        byte[] crc = DigitalTrans.hex2byte(str_crc);//十六进制串转化为byte数组
                        Log.e("url", "删除手机用户_byte_CRC==" + crc[0]);

                        byte[] serial_num = DigitalTrans.hex2byte(admin_id);//十六进制串转化为byte数组
                        byte[] head_serial_num = DigitalTrans.byteMerger(head, serial_num);//把头部和序号加在一起
                        byte[] delete_ordinary_user_data = DigitalTrans.byteMerger(head_serial_num, crc);//把头部和序号、crc加在一起
                        byte[] add_delete_ordinary_user_data = DigitalTrans.byteMerger(delete_ordinary_user_data, Const.add13);//补全16位

                        Log.e("url", "add_delete_ordinary_user_data==" + add_delete_ordinary_user_data.length);
                        byte[] encrypt_add_delete_ordinary_user_data = null;
                        //加密
                        try {
                            encrypt_add_delete_ordinary_user_data = AesEntryDetry.encrypt(add_delete_ordinary_user_data);//加密
                        } catch (Exception e) {
                            e.printStackTrace();
                        }

                        sharedPreferencesUtil.setPhoneadd("31");
                        UpdateBleData2(mac, "31");
                        deviceList = getBleData(type);
                        deviceAdapter.notifyDataSetChanged();
                        pop_delete.dismiss();

                    }
                }

            }
        });

        tv_cancel.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                pop_delete.dismiss();
            }
        });
    }

    /**
     * 扫描蓝牙
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
     * 扫描蓝牙后的回调
     * Device scan callback.
     */
    private BluetoothAdapter.LeScanCallback mLeScanCallback = new BluetoothAdapter.LeScanCallback() {

        @Override
        public void onLeScan(final BluetoothDevice device, final int rssi, final byte[] scanRecord) {
            mActivity.runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    Log.e("url", "device_size==" + scanRecord.length);

//                    for (int i = 0; i < scanRecord.length; i++) {
//                        Log.e("url", "scanRecord:  "+i+" ==" + scanRecord[i]);
//                    }
//                    byte[] scanRecord_name = new byte[16];
//                    int i_16 = 0;
//                    for (int j = 0; j < scanRecord.length; j++) {
//                        if (j < 23 & j >= 7) {
//                            scanRecord_name[i_16++] = scanRecord[j];//获取名称的那几位（7-22）
//                        }
//                    }
//
//                    try {
//                        scanRecord_name = AesEntryDetry.decrypt(scanRecord_name);//解密名称的那16位
//                    } catch (Exception e) {
//                        e.printStackTrace();
//                    }
//
//                    for (int j = 0; j < scanRecord_name.length; j++) {//把解密后的名称放入scanRecord中
//                        scanRecord[j + 7] = scanRecord_name[j];
//                    }
//                    for (int i = 21; i <= 61; i++) {//初始化后面的21-61位
//                        scanRecord[i] = 0;
//                    }

                    ParsedAd parsedAd = parseData(scanRecord);

                    Log.e("url", " parsedAd.flags==" + parsedAd.flags);
                    Log.e("url", " parsedAd.uuids==" + parsedAd.uuids);
                    Log.e("url", " parsedAd.localName==" + parsedAd.localName);//获取广播包中的名称
                    Log.e("url", " parsedAd.manufacturer==" + parsedAd.manufacturer);

                    //将扫描到的蓝牙存放到list中
                    if (!bluetoothDeviceList.contains(device)) {
                        try {
                            String device_name = parsedAd.localName;
                            if (device_name == null) {
                                device_name = device.getName().toString();//针对没加密版本的
                            }
                            if (device_name.contains("Gemini") || device_name.contains("Hemini")) {
                                bluetoothDeviceList.add(device);
                                scanBle.setBluetoothDeviceList(bluetoothDeviceList);
                                scanBle.setBle_name(device_name);
                                scanBle.setBle_rssi(rssi);
                                scanBleList.add(scanBle);
                                Log.e("url", "device_mac==" + device.getAddress().toString());
                                Log.e("url", "scanBle_ble_name==" + scanBle.getBle_name());
                                Log.e("url", "scanBleList.size==" + scanBleList.size());
                            }
                        } catch (NullPointerException e) {
                            e.printStackTrace();
                        }

                        RSSI = rssi;
                        mBleService.BLERSSI = RSSI;

                        for (int i = 0; i < deviceList.size(); i++) {
                            Log.e("url", "mac==" + device.getAddress());
                            //扫描到的蓝牙与数据库中的蓝牙列表对照（如果扫描到的蓝牙在数据库中有，就马上连接）
                            if (deviceList.get(i).get("mac").toString().equals(device.getAddress().toString())) {

                                Log.e("url", "sql_mac==" + deviceList.get(i).get("mac").toString() +
                                        "  scan_mac===" + device.getAddress().toString());

                                if (deviceList.get(i).get("is_invalid").equals("false")) {//判断是否失效

                                    final String phone = GetPhone(device.getAddress());//获取蓝牙数据库中一些值
                                    boolean is_connect = mBleService.connect(device.getAddress().toString());//l连接蓝牙
                                    Log.e("url", "is_connect==" + is_connect);

                                    final int finalI = i;
                                    mBleService.setOnServicesDiscoveredListener(new BleListener.OnServicesDiscoveredListener() {
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
                                                        Log.e("url", "serviceUuid_ffe0==" + serviceUuid);
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
                                                            Log.e("url", "FFE1_Uuid==" + charUuid);
                                                        }
                                                        if (containsString(charUuid, "ffe2")) {//保存ffe2的characteristics
                                                            sharedPreferencesUtil.setCharFfe2(charUuid);
                                                            Log.e("url", "FFE2_Uuid==" + charUuid);
                                                        }
                                                        if (containsString(charUuid, "ffe3")) {//保存ffe3的characteristics
                                                            sharedPreferencesUtil.setCharFfe3(charUuid);
                                                            Log.e("url", "FFE3_Uuid==" + charUuid);
                                                        }
                                                    }
                                                    characteristicList.add(charArra);
                                                }

                                                upload_flag = "";
                                                str_1f4b4f = "";
                                                setCharacteristicNotificationBack(device.getAddress(), sharedPreferencesUtil.getCharFfe2());//通知的回调
                                                sharedPreferencesUtil.setMAC(device.getAddress());
                                                scanLeDevice(false);

                                                /**
                                                 * 下发手机号码
                                                 */
                                                mActivity.runOnUiThread(new Runnable() {
                                                    @Override
                                                    public void run() {
                                                        MainActivity.is_upload = true;
                                                        byte[] head = new byte[]{0x01};//固定的前面只是0x01
                                                        phoneStringBuffer = new StringBuffer(phone);//追加手机号码

                                                        if (!(sharedPreferencesUtil.getPhoneadd().equals("") || sharedPreferencesUtil.getPhoneadd().equals("0"))) {
                                                            Log.e("url", "手机号码的尾随==" + sharedPreferencesUtil.getPhoneadd());
                                                            handleString = "0" + sharedPreferencesUtil.getPhoneadd() + "0000000000000000";
                                                        }
                                                        phoneStringBuffer.append(handleString);
                                                        String phone_add = String.valueOf(phoneStringBuffer);
                                                        Log.e("url", "phone_add==" + phone_add);
                                                        byte[] body = DigitalTrans.hex2byte(phone_add);//十六进制串转化为byte数组
                                                        byte[] all = DigitalTrans.byteMerger(head, body);//把头部和手机号码加在一起
                                                        Log.e("url", "all==" + all.length);
                                                        byte[] encrypt_all = null;
                                                        //加密
                                                        try {
                                                            encrypt_all = AesEntryDetry.encrypt(all);//加密
                                                        } catch (Exception e) {
                                                            e.printStackTrace();
                                                        }

                                                        //下发手机号码
                                                        mBleService.writeCharacteristic(sharedPreferencesUtil.getService(), sharedPreferencesUtil.getCharFfe1(), encrypt_all);
                                                        mBleService.setCharacteristicNotification(sharedPreferencesUtil.getService(), sharedPreferencesUtil.getCharFfe2(), true);
                                                    }
                                                });
                                            }
                                        }
                                    });
                                }
                            }
                        }
                    }
                }
            });
        }

    };

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
     * 更新手机用户数据库
     *
     * @param user_type
     * @param user_id
     * @param user_num
     * @param str_user
     */
    private void UpdatePhoneUserTableData(String user_type, String user_id, String user_num, String str_user) {
        Log.e("url", "UpdatePhoneUserTableData");
        phone_user_db = phoneUserSQLiteOpenHelper.getWritableDatabase();
        ContentValues values = new ContentValues();

        //游标查询每条数据
        Cursor cursor = phone_user_db.query("Phone_User_Table", null, "mac=? and user_id=?", new String[]{sharedPreferencesUtil.getMAC(), user_id}, null, null, null);
        if (cursor.moveToNext()) {
            int count = cursor.getInt(0);
            if (count > 0) {//数据存在
                Log.e("url", "用户存在，进行更新！");
                values.put("user_type", user_type);
                values.put("user_num", user_num);
                values.put("str_user", str_user);
                phone_user_db.update("Phone_User_Table", values, "mac=? and user_id=?", new String[]{sharedPreferencesUtil.getMAC(), user_id});
            }
        } else {
            values.put("mac", sharedPreferencesUtil.getMAC());
            values.put("user_type", user_type);
            values.put("user_id", user_id);
            values.put("user_num", user_num);
            values.put("str_user", str_user);
            phone_user_db.insert("Phone_User_Table", null, values);
        }

        cursor.close();
        phone_user_db.close();
    }

    /**
     * 更新键盘用户数据库
     *
     * @param user_id
     * @param password
     * @param str_user
     */
    private void UpdateKeyboardUserTableData(String user_id, String type, String password, String str_user) {
        disposable_user_db = disposableUserSQLiteOpenHelper.getWritableDatabase();
        ContentValues values = new ContentValues();

        Log.e("url", "UpdateKeyboardUserTableData普通用户");

        //游标查询每条数据
        Cursor cursor = disposable_user_db.query("Disposable_User_Table", null, "mac=? and type=? and user_id=?", new String[]{sharedPreferencesUtil.getMAC(), type, user_id}, null, null, null);
        if (cursor.moveToNext()) {
            int count = cursor.getInt(0);
            String user_id_column = cursor.getString(4);
            Log.e("url", "user_id_column==" + user_id_column + "  user_id==" + user_id);
            if (count > 0) {//数据存在
                Log.e("url", "用户存在，进行更新！");
                values.put("password", password);
                disposable_user_db.update("Disposable_User_Table", values, "mac=? and type=? and user_id=?", new String[]{sharedPreferencesUtil.getMAC(), type, user_id});
            }
        }

        cursor.close();
        disposable_user_db.close();
    }

    /**
     * 更新TM卡用户数据库
     *
     * @param user_id
     * @param user_num
     */
    private void UpdateTMCardUserTableData(String user_id, String user_num) {
        tm_card_db = tmCardUserSQLiteOpenHelper.getWritableDatabase();
        ContentValues values = new ContentValues();

        //游标查询每条数据
        Cursor cursor = tm_card_db.query("TM_Card_User_Table", null, "mac=? and user_id=?", new String[]{sharedPreferencesUtil.getMAC(), user_id}, null, null, null);
        if (cursor.moveToNext()) {
            int count = cursor.getInt(0);
            if (count > 0) {//数据存在
                Log.e("url", "用户存在，进行更新！");
                values.put("user_num", user_num);
                tm_card_db.update("TM_Card_User_Table", values, "user_id=?", new String[]{user_id});
            }
        } else {
            values.put("mac", sharedPreferencesUtil.getMAC());
            values.put("user_id", user_id);
            values.put("user_num", user_num);
            tm_card_db.insert("TM_Card_User_Table", null, values);
        }
        cursor.close();
        tm_card_db.close();
    }

    /**
     * 初始化开锁记录的数据库
     *
     * @param num
     * @param user_type
     * @param user_type_num
     * @param year_month
     * @param year
     * @param month
     * @param day
     * @param hour
     * @param minute
     * @param second
     */
    private void AddTableData(String num, String user_type, String user_type_num, String year_month, String year, String month, String day, String hour, String minute, String second) {
        unlock_record_db = unlockRecordSQLiteOpenHelper.getWritableDatabase();
        ContentValues values = new ContentValues();

        values.put("mac", sharedPreferencesUtil.getMAC());
        values.put("num", num);
        values.put("user_type", user_type);
        values.put("user_type_num", user_type_num);
        values.put("year_month", year_month);
        values.put("year", year);
        values.put("month", month);
        values.put("day", day);
        values.put("hour", hour);
        values.put("minute", minute);
        values.put("second", second);
        unlock_record_db.insert("Unlock_Record_Table", null, values);

        unlock_record_db.close();
    }

    /**
     * 初始化期限单次用户数据库
     */
    private void InitializeDisposableOnceUserTableData() {
        disposable_user_db = disposableUserSQLiteOpenHelper.getWritableDatabase();
        ContentValues values = new ContentValues();

        //游标查询每条数据
        Cursor cursor = disposable_user_db.query("Disposable_User_Table", null, "mac=? and type='once' ", new String[]{sharedPreferencesUtil.getMAC()}, null, null, null);

        while (cursor.moveToNext()) {
            values.put("password", "bbbbbbbbbbbb");
            disposable_user_db.update("Disposable_User_Table", values, "mac=? and type='once'", new String[]{sharedPreferencesUtil.getMAC()});
        }
        disposable_user_db.close();
    }

    /**
     * 更新期限单次用户数据库
     *
     * @param user_id
     * @param password
     */
    private void UpdateDisposableOnceUserTableData(String user_id, String password) {
        disposable_user_db = disposableUserSQLiteOpenHelper.getWritableDatabase();
        ContentValues values = new ContentValues();

        //游标查询每条数据
        Cursor cursor = disposable_user_db.query("Disposable_User_Table", null, "mac=? and type='once' AND user_id=? ", new String[]{sharedPreferencesUtil.getMAC(), user_id}, null, null, null);

        if (cursor.moveToNext()) {
            int count = cursor.getInt(0);
            if (count > 0) {
                values.put("password", password);
                disposable_user_db.update("Disposable_User_Table", values, "mac=? and type='once' AND user_id=?", new String[]{sharedPreferencesUtil.getMAC(), user_id});
            }
        }
        disposable_user_db.close();
    }

    /**
     * 长按设备列表删除数据，更新数据库
     *
     * @param mac
     */
    private void UpdateBleData(String mac) {
        Log.e("url", "mac==" + mac);
        ble_db = bleSQLiteOpenHelper.getWritableDatabase();
        ContentValues values = new ContentValues();

        //游标查询每条数据
        Cursor cursor = ble_db.query("Ble_Table", null, "mac=?", new String[]{mac}, null, null, null);

        if (cursor.moveToNext()) {
            int count = cursor.getInt(0);
            if (count > 0) {//数据存在
                values.put("electricity", "100");
                values.put("admin_id", "0");
                values.put("admin_type", "0");
                values.put("connect_music", "0");
                values.put("unlock_music", "0");
                values.put("connect_music_on_off", "off");
                values.put("unlock_music_on_off", "off");
                ble_db.update("Ble_Table", values, "mac=?", new String[]{mac});
            }
        }
        cursor.close();
        ble_db.close();
    }

    /**
     * 获取到电量后，更新蓝牙数据库
     *
     * @param mac
     * @param electricity
     */
    private void UpdateBleData1(String mac, String electricity) {

        Log.e("url", "mac==" + mac + "  电量electricity==" + electricity);
        ble_db = bleSQLiteOpenHelper.getWritableDatabase();
        ContentValues values = new ContentValues();

        //游标查询每条数据
        Cursor cursor = ble_db.query("Ble_Table", null, "mac=?", new String[]{mac}, null, null, null);

        if (cursor.moveToNext()) {
            int count = cursor.getInt(0);
            String id = cursor.getString(0);
            if (count > 0) {//数据存在
                Log.e("url", "用户存在，更新蓝牙电量！");
                values.put("electricity", Integer.parseInt(electricity));
                ble_db.update("Ble_Table", values, "mac=?", new String[]{mac});//
            }
        }
        cursor.close();
        ble_db.close();

        deviceList = getBleData(type);//从数据库中获取连接过的蓝牙列表

        mActivity.runOnUiThread(new Runnable() {
            @Override
            public void run() {
                deviceAdapter.notifyDataSetChanged();
            }
        });


    }

    /**
     * 手机普通用户失效后，更新蓝牙数据库（is_invalid）
     *
     * @param mac
     * @param is_invalid
     */
    private void UpdateBleInvalid(String mac, String is_invalid) {
        Log.e("url", "mac==" + mac + "  is_invalid==" + is_invalid);
        ble_db = bleSQLiteOpenHelper.getWritableDatabase();
        ContentValues values = new ContentValues();

        //游标查询每条数据
        Cursor cursor = ble_db.query("Ble_Table", null, "mac=?", new String[]{mac}, null, null, null);

        if (cursor.moveToNext()) {
            int count = cursor.getInt(0);
            if (count > 0) {//数据存在
                Log.e("url", "用户存在，更新蓝牙是否失效！");
                values.put("electricity", "100");
                values.put("is_invalid", is_invalid);
                values.put("connect_music", "0");
                values.put("unlock_music", "0");
                values.put("connect_music_on_off", "off");
                values.put("unlock_music_on_off", "off");
                ble_db.update("Ble_Table", values, "mac=?", new String[]{mac});
            }
        }
        cursor.close();
        ble_db.close();

        deviceList = getBleData(type);//从数据库中获取连接过的蓝牙列表
        mActivity.runOnUiThread(new Runnable() {
            @Override
            public void run() {
                deviceAdapter.notifyDataSetChanged();
            }
        });

    }

    /**
     * 获取到手机用户类型后，更新蓝牙数据库
     *
     * @param mac
     * @param admin_type
     */
    private void UpdateBleData2(String mac, String admin_type) {
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
     * 加载数据库的数据到List中(获取连接过的蓝牙)
     *
     * @param type
     * @return
     */
    private List<Map<String, Object>> getBleData(String type) {
        ble_db = bleSQLiteOpenHelper.getWritableDatabase();
        Cursor cursor;
        if (type.equals("all")) {
            //游标查询每条数据
            cursor = ble_db.query("Ble_Table", null, null, null, null, null, null);
            Log.e("url", "all");
        } else {
            //游标查询每条数据
            cursor = ble_db.query("Ble_Table", null, "type=?", new String[]{type}, null, null, null);
            Log.e("url", "door or hand");
        }

        //定义list存储数据
        List<Map<String, Object>> list = new ArrayList<Map<String, Object>>();

        //读取数据 游标移动到下一行
        while (cursor.moveToNext()) {
            String admin_type = cursor.getString(8);
            if (!admin_type.equals("0")) {
                Map<String, Object> map = new HashMap<String, Object>();
                map.put("id", cursor.getString(cursor.getColumnIndex("id")));
                map.put("ble_name", cursor.getString(cursor.getColumnIndex("ble_name")));
                map.put("mac", cursor.getString(cursor.getColumnIndex("mac")));
                map.put("type", cursor.getString(cursor.getColumnIndex("type")));
                map.put("electricity", cursor.getString(cursor.getColumnIndex("electricity")));
                map.put("unlock_method", cursor.getString(cursor.getColumnIndex("unlock_method")));
                map.put("unlock_distance", cursor.getString(cursor.getColumnIndex("unlock_distance")));
                map.put("admin_id", cursor.getString(cursor.getColumnIndex("admin_id")));
                map.put("admin_type", cursor.getString(cursor.getColumnIndex("admin_type")));
                map.put("admin_phone", cursor.getString(cursor.getColumnIndex("admin_phone")));
                map.put("connect_music", cursor.getString(cursor.getColumnIndex("connect_music")));
                map.put("unlock_music", cursor.getString(cursor.getColumnIndex("unlock_music")));
                map.put("connect_music_on_off", cursor.getString(cursor.getColumnIndex("connect_music_on_off")));
                map.put("unlock_music_on_off", cursor.getString(cursor.getColumnIndex("unlock_music_on_off")));
                map.put("is_invalid", cursor.getString(cursor.getColumnIndex("is_invalid")));
                list.add(map);
            }
        }

        cursor.close();
        ble_db.close();
        return list;
    }

    /**
     * 加载数据库的数据到List中(获取未发送成功的数据)
     */
    private List<Map<String, Object>> getWaitSendData() {
        wait_send_data_db = waitSendDataSQLiteOpenHelper.getWritableDatabase();
        Cursor cursor;
        cursor = wait_send_data_db.query("WaitSendData_Table", null, null, null, null, null, null);

        //定义list存储数据
        List<Map<String, Object>> list = new ArrayList<Map<String, Object>>();

        //读取数据 游标移动到下一行
        while (cursor.moveToNext()) {
            String mac = cursor.getString(1);
            if (mac.equals(sharedPreferencesUtil.getMAC())) {
                Map<String, Object> map = new HashMap<String, Object>();
                map.put("id", cursor.getString(cursor.getColumnIndex("id")));
                map.put("mac", cursor.getString(cursor.getColumnIndex("mac")));
                map.put("data", cursor.getString(cursor.getColumnIndex("data")));
                map.put("write_char", cursor.getString(cursor.getColumnIndex("write_char")));
                map.put("setnotice_char", cursor.getString(cursor.getColumnIndex("setnotice_char")));
                map.put("is_0A", cursor.getString(cursor.getColumnIndex("is_0A")));
                list.add(map);
            }
        }
        return list;
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
        }

        waitSendData = getWaitSendData();

        cursor.close();
        ble_db.close();
        return phone;
    }

    /**
     * 下发完待发送的数据后，清空表格
     */
    private void DeleteData() {
        Log.e("url", "清空离线数据库");
        wait_send_data_db = waitSendDataSQLiteOpenHelper.getWritableDatabase();
        wait_send_data_db.delete("WaitSendData_Table", null, null);//清空待发送的数据
        wait_send_data_db.close();
    }

    /**
     * 清空表格
     */
    private void DeletePhoneUserData() {
        phone_user_db = phoneUserSQLiteOpenHelper.getWritableDatabase();
        phone_user_db.delete("Phone_User_Table", null, null);//清空数据

        phone_user_db.close();
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
            Log.e("url", "添加键盘普通用户");
            for (int i = 0; i <= 9; i++) {
                values.put("mac", sharedPreferencesUtil.getMAC());
                values.put("password", "0");
                values.put("type", "ordinary");
                values.put("user_id", "0" + i);
                values.put("start_time", "0");
                values.put("end_time", "0");
                values.put("str_user", "0");
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
     * 初始化键盘管理员用户
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
            Log.e("url", "添加新管理员用户");
            values.put("mac", sharedPreferencesUtil.getMAC());
            values.put("password", "123456");
            values.put("type", "admin");
            values.put("user_id", "9999");
            values.put("start_time", "0");
            values.put("end_time", "0");
            values.put("str_user", "0");
            disposable_user_db.insert("Disposable_User_Table", null, values);
        }
        cursor.close();
        disposable_user_db.close();
    }

    /**
     * 初始化所有的期限用户
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
            Log.e("url", "添加期限用户");
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
     * 蓝牙连接后，发送 实时的RSSI 值
     */
    private void sendRSSI(int Rssi) {
        int rssi = Math.abs(Rssi);
        Log.e("url", "Rssi==" + Rssi + "    rssi==" + rssi);

        byte[] head = new byte[]{0x09};//固定的前面只是0x09
        String str_rssi = DigitalTrans.algorismToHEXString(rssi);//十进制转成十六进制
        if (str_rssi.length() == 1) {
            str_rssi = 0 + str_rssi;
        }
        byte[] byte_rssi = DigitalTrans.hex2byte(str_rssi);//十六进制串转化为byte数组
        byte[] rssi_data = DigitalTrans.byteMerger(head, byte_rssi);//把头部和rssi加在一起
        byte[] add_rssi_data = DigitalTrans.byteMerger(rssi_data, Const.add14);//补全16位

        Log.e("url", "add_rssi_data==" + add_rssi_data.length);
        byte[] encrypt_add_rssi_data = null;
        //加密
        try {
            encrypt_add_rssi_data = AesEntryDetry.encrypt(add_rssi_data);//加密
        } catch (Exception e) {
            e.printStackTrace();
        }

        if (sharedPreferencesUtil.getService() != null && sharedPreferencesUtil.getCharFfe3() != null) {

            try {
                Thread.sleep(500);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
            //下发 09 rssi
            mBleService.writeCharacteristic(sharedPreferencesUtil.getService(), sharedPreferencesUtil.getCharFfe3(), encrypt_add_rssi_data);
//            mBleService.setCharacteristicNotification(sharedPreferencesUtil.getService(), sharedPreferencesUtil.getCharFfe2(), true);

        } else {
            Toast.makeText(mActivity, "请重新连接蓝牙！", Toast.LENGTH_SHORT).show();
        }


    }

    /**
     * 监听屏幕的状态
     */
    private void ScreenObserve() {
        is_user = true;
        mScreenObserver = new ScreenObserver(mActivity);
        mScreenObserver.requestScreenStateUpdate(new ScreenObserver.ScreenStateListener() {
            @Override
            public void onScreenOn() {
                Log.e("url", "onScreenOn");
                if (sharedPreferencesUtil.getUnlockmethod().equals("light_screen")) {
                    byte[] data = new byte[]{0x10, 0x4f, 0x50};
                    byte[] add_data = DigitalTrans.byteMerger(data, Const.add13);//补全16位

                    Log.e("url", "add_data==" + add_data.length);
                    byte[] encrypt_add_data = null;
                    //加密
                    try {
                        encrypt_add_data = AesEntryDetry.encrypt(add_data);//加密
                    } catch (Exception e) {
                        e.printStackTrace();
                    }

                    MainActivity.is_upload = true;
                    mBleService.writeCharacteristic(sharedPreferencesUtil.getService(), sharedPreferencesUtil.getCharFfe1(), encrypt_add_data);
                    mBleService.setCharacteristicNotification(sharedPreferencesUtil.getService(), sharedPreferencesUtil.getCharFfe2(), true);

                    try {
                        Thread.sleep(800);
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                    MainActivity.is_upload = false;

                }
            }

            @Override
            public void onScreenOff() {
                Log.e("url", "onScreenOff");
                sharedPreferencesUtil.setScreenState("onScreenOff");
            }
        });
    }

    /**
     * 权限是否打开监听
     */
    private void verifyIfRequestPermission() {
        if (Build.VERSION.SDK_INT >= 23) {
            Log.i(TAG, "onCreate: checkSelfPermission");
            if (ContextCompat.checkSelfPermission(mActivity,
                    Manifest.permission.ACCESS_COARSE_LOCATION)
                    != PackageManager.PERMISSION_GRANTED) {
                Log.i(TAG, "onCreate: Android 6.0 动态申请权限");

                if (ActivityCompat.shouldShowRequestPermissionRationale(mActivity,
                        Manifest.permission.READ_CONTACTS)) {
                    Log.i(TAG, "*********onCreate: shouldShowRequestPermissionRationale**********");
                    Toast.makeText(mActivity, "只有允许访问位置才能搜索到蓝牙设备", Toast.LENGTH_SHORT).show();
                } else {
                    ActivityCompat.requestPermissions(mActivity,
                            new String[]{Manifest.permission.ACCESS_COARSE_LOCATION,
                                    Manifest.permission.ACCESS_FINE_LOCATION},
                            REQUEST_CODE_ACCESS_COARSE_LOCATION);
                }
            } else {
            }
        } else {
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        if (requestCode == REQUEST_CODE_ACCESS_COARSE_LOCATION) {
            Log.i(TAG, "onRequestPermissionsResult: permissions.length = " + permissions.length +
                    ", grantResults.length = " + grantResults.length);
            if (grantResults.length > 0
                    && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
            } else {
                Toast.makeText(mActivity, "位置访问权限被拒绝将无法搜索到ble设备", Toast.LENGTH_SHORT).show();
            }
        } else {
            super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        }
    }

    /**
     * 绑定服务
     */
    private void doBindService() {
        Log.e("url", "EquipmentFg_doBindService");
        Intent serviceIntent = new Intent(mActivity, BleService.class);
        mActivity.bindService(serviceIntent, serviceConnection, Context.BIND_AUTO_CREATE);
    }

    /**
     * 解绑服务
     */
    private void doUnBindService() {
        if (mIsBind) {
            mActivity.unbindService(serviceConnection);
            mBleService = null;
            mIsBind = false;
        }
    }

    private BroadcastReceiver bleReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            if (intent.getAction().equals(BleService.ACTION_BLUETOOTH_DEVICE)) {
                String tmpDevName = intent.getStringExtra("name");
                String tmpDevAddress = intent.getStringExtra("address");
                Log.i("url", "name: " + tmpDevName + ", address: " + tmpDevAddress);
                HashMap<String, Object> deviceMap = new HashMap<>();
                if (tmpDevName != null && containsString(tmpDevName, "Gemini")) {//扫描到蓝牙设备
                    Log.e("url", "name: " + tmpDevName + ", address: " + tmpDevAddress);

                    connDeviceAddress = tmpDevAddress;
                    connDeviceName = tmpDevName;
                }
            } else if (intent.getAction().equals(BleService.ACTION_GATT_CONNECTED)) {
                deviceAdapter.notifyDataSetChanged();
                Log.e("url", "连接成功！");

            } else if (intent.getAction().equals(BleService.ACTION_GATT_DISCONNECTED)) {
                deviceAdapter.notifyDataSetChanged();
            } else if (intent.getAction().equals(BleService.ACTION_SCAN_FINISHED)) {
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
     * 解析获取扫描到蓝牙后返回的广播包
     *
     * @param adv_data
     * @return
     */
    public static ParsedAd parseData(byte[] adv_data) {
        ParsedAd parsedAd = new ParsedAd();
        ByteBuffer buffer = ByteBuffer.wrap(adv_data).order(ByteOrder.LITTLE_ENDIAN);
        Log.e("url", "buffer==" + buffer);
        while (buffer.remaining() > 2) {
            byte length = buffer.get();
            Log.e("url", "length==" + length);
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
                Log.e("url", "length2222222==" + length);
                buffer.position(buffer.position() + length);
            }
        }
        return parsedAd;
    }

    /**
     * 读取蓝牙RSSI线程
     */
    Thread readRSSI = new Thread() {
        int Rssi = 0;

        @Override
        public void run() {
            // TODO Auto-generated method stub
            super.run();
            while (isReadRssi) {
                try {
                    sleep(1000);
                } catch (InterruptedException e) {
                    // TODO Auto-generated catch block
                    e.printStackTrace();
                }
                Log.e("url", "下发实时的rssi,is_upload===" + MainActivity.is_upload);

                if (!MainActivity.is_upload) {
                    // 如果读取蓝牙RSSi回调成功
                    if (mBleService.getRssiVal()) {
                        // 获取已经读到的RSSI值
                        Rssi = mBleService.getBLERSSI();
                        sendRSSI(Rssi);//当连接成功下发RSSI值
                        Log.e("url", "蓝牙连接后的rssi==" + Rssi);
                    }
                }
            }
        }
    };

    /**
     * 读取扫描蓝牙的线程
     */
    Thread scanDevice = new Thread() {

        @Override
        public void run() {
            // TODO Auto-generated method stub
            super.run();
            while (isScanBle) {
                try {
                    sleep(900);
                } catch (InterruptedException e) {
                    // TODO Auto-generated catch block
                    e.printStackTrace();
                }

                Log.i("url", "MainActivity.refresh==" + MainActivity.refresh);
                if (MainActivity.refresh == 1) {
                    deviceList = getBleData(type);
                    mActivity.runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            deviceAdapter.notifyDataSetChanged();//刷新界面
                            MainActivity.refresh = 0;
                        }
                    });

                }

                Log.e("url", "是否在设备界面==" + MainActivity.is_EquipMentFg + "    蓝牙连接情况==" + mBleService.isconnect + "    是否在扫描==" + MainActivity.is_scan
                        + "     是否在MainActivity界面==" + MainActivity.is_mainactivity);

                if (MainActivity.is_EquipMentFg == true && MainActivity.is_scan == false && mBleService.isconnect == false && MainActivity.is_mainactivity == true) {
//                    initBletooth();
                    Log.e("url", "进入扫描。");
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

                    //初始化
                    scanBleList = new ArrayList<ScanBle>();
                    scanBle = new ScanBle();
                    bluetoothDeviceList = new ArrayList<BluetoothDevice>();
                    scanLeDevice(true);
                }
            }
        }
    };


    @Override
    public void onResume() {
        super.onResume();
        Log.d("url", "EquipmentFg_onResume");
        bleSQLiteOpenHelper = new BleSQLiteOpenHelper(mActivity, "BleDatabase.db", null, 2);
        bleSQLiteOpenHelper.getWritableDatabase();//创建连接过的蓝牙表
        mActivity.registerReceiver(bleReceiver, makeIntentFilter());
        deviceList = getBleData(type);//从数据库中获取连接过的蓝牙列表
        deviceAdapter.notifyDataSetChanged();

        initBletooth();
        MainActivity.is_scan = false;

        //2秒扫描一次蓝牙
        if (!scanDevice.isAlive()) {
            isScanBle = true;
            scanDevice.start();
        }

        if (!sharedPreferencesUtil.getMAC().equals("")) {
            AddPhoneUserTableData();//初始化手机普通用户列表
            AddKeyboardAdminUserTableData("admin");//初始化键盘用户列表（管理员用户）
            AddKeyboardUserTableData();//初始化键盘用户列表（普通用户）
            AddDisposableUserTableData();//初始化期限用户列表
        }
    }

    @Override
    public void onPause() {
        super.onPause();
        Log.e("url", "EquipmentFg_onPause");
    }

    @Override
    public void onDestroy() {
        Log.e("url", "EquipmentFg_onDestroy");
        super.onDestroy();
        doUnBindService();
        mActivity.unregisterReceiver(bleReceiver);

        if (is_user == true) {
            //停止监听screen状态
            mScreenObserver.stopScreenStateUpdate();
            if (mBleService.isScanning()) {
                mBleService.scanLeDevice(false);
                return;
            }
        }
    }

    /**
     * 通知的回调
     *
     * @param mac
     * @param ffe2
     */
    private void setCharacteristicNotificationBack(String mac, String ffe2) {
        Log.e("url", "setCharacteristicNotificationBack------方法");
        mBleService.setOnDataAvailableListener(new BleListener.OnDataAvailableListener() {
            @Override
            public void onCharacteristicRead(BluetoothGatt gatt, BluetoothGattCharacteristic characteristic, int status) {

            }

            @Override
            public void onCharacteristicChanged(BluetoothGatt gatt, BluetoothGattCharacteristic characteristic) {

                Log.e("url", "setCharacteristicNotificationBack————设备==" + gatt.getDevice().getAddress() +
                        "   返回值(未解密)===" + DigitalTrans.bytesToHexString(characteristic.getValue()));

                String decrypt_back_data = null;
                try {
                    decrypt_back_data = com.yundiankj.ble_lock.Resource.DigitalTrans.bytesToHexString(AesEntryDetry.decrypt(characteristic.getValue()));//解密
                } catch (Exception e) {
                    e.printStackTrace();
                }
                Log.e("url", "setCharacteristicNotificationBack————返回值(解密后)-->" + decrypt_back_data);
                String six_back_data = decrypt_back_data.substring(decrypt_back_data.length() - 32, decrypt_back_data.length() - 26);//获取前六位
                Log.e("url", "setCharacteristicNotificationBack--返回值(解密后)————前六位-->" + six_back_data);

                //电量
                if (six_back_data.contains("310")) {
                    //离线删除设备的用户（待定）
                    if (sharedPreferencesUtil.getPhoneadd().equals("31")) {
                        sharedPreferencesUtil.setPhoneadd("00");
                        UpdateBleData(sharedPreferencesUtil.getMAC());
                        GetPhone(sharedPreferencesUtil.getMAC());
                        deviceList = getBleData(type);
                        mActivity.runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                deviceAdapter.notifyDataSetChanged();
                            }
                        });

                        return;
                    }
                    String electricity = six_back_data.substring(six_back_data.length() - 3, six_back_data.length());//获取电量值
                    Log.e("url", "electricity==" + electricity);

                    UpdateBleData1(sharedPreferencesUtil.getMAC(), electricity);

                    //下发当前时间（16（年）10（月） 20（日） 10（时） 10（分） 10（秒））
                    SimpleDateFormat format = new SimpleDateFormat("yyMMddHHmmss"); //获取当前时间
                    Date current_date = new Date();//取时间
                    String current_time;//当前时间
                    current_time = format.format(current_date);//当前时间
                    Log.e("url", "current_time==" + current_time);
                    byte[] time_head = new byte[]{0x08};//固定的前面只是0x08
                    byte[] byte_time = DigitalTrans.hex2byte(current_time);//十六进制串转化为byte数组
                    byte[] time = DigitalTrans.byteMerger(time_head, byte_time);//把头部和时间加在一起
                    byte[] add_time = DigitalTrans.byteMerger(time, Const.add9);//补全16位
                    Log.e("url", "add_time==" + add_time.length);

                    byte[] encrypt_add_time = null;
                    //加密
                    try {
                        encrypt_add_time = AesEntryDetry.encrypt(add_time);//加密
                    } catch (Exception e) {
                        e.printStackTrace();
                    }

                    mBleService.writeCharacteristic(sharedPreferencesUtil.getService(), sharedPreferencesUtil.getCharFfe1(), encrypt_add_time);
                }

                //手机管理员删除手机普通用户后
                if (decrypt_back_data.equals("cccccc00000000000000000000000000")) {
                    UpdateBleInvalid(sharedPreferencesUtil.getMAC(), "true");//该用户已经失效
                    mActivity.runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            deviceAdapter.notifyDataSetChanged();
                        }
                    });
                    return;
                }

                if (six_back_data.equals("164b4f")) {
                    str_164b4f = "164b4f";
                }

                if (str_164b4f.equals("164b4f")) {
                    if (waitSendData.size() > 0) {//下发离线数据
                        byte[] data = DigitalTrans.hex2byte(waitSendData.get(0).get("data").toString());//十六进制串转化为byte数组
                        String data_0A = waitSendData.get(0).get("data").toString();
                        String data_0A_2 = data_0A.substring(data_0A.length() - 32, data_0A.length() - 30);//获取前2位
                        Log.e("url", "下发的离线数据==" + waitSendData.get(0).get("data").toString());
                        byte[] encrypt_data = null;
                        //加密
                        try {
                            encrypt_data = AesEntryDetry.encrypt(data);//加密
                        } catch (Exception e) {
                            e.printStackTrace();
                        }
                        mBleService.writeCharacteristic(sharedPreferencesUtil.getService(), waitSendData.get(0).get("write_char").toString(), encrypt_data);
                        mBleService.setCharacteristicNotification(sharedPreferencesUtil.getService(), sharedPreferencesUtil.getCharFfe2(), true);

                        waitSendData.remove(0);//移除

//                        if (data_0A_2.equals("0A")) {
//                            Log.e("url", "下发了0a------------------");
//                            if (waitSendData.size() > 0) {//下发离线数据
//                                byte[] data1 = DigitalTrans.hex2byte(waitSendData.get(0).get("data").toString());//十六进制串转化为byte数组
//                                Log.e("url", "下发的离线数据2222==" + waitSendData.get(0).get("data").toString());
//                                byte[] encrypt_data1 = null;
//                                //加密
//                                try {
//                                    encrypt_data1 = AesEntryDetry.encrypt(data1);//加密
//                                } catch (Exception e) {
//                                    e.printStackTrace();
//                                }
//                                mBleService.writeCharacteristic(sharedPreferencesUtil.getService(), waitSendData.get(0).get("write_char").toString(), encrypt_data1);
//                                mBleService.setCharacteristicNotification(sharedPreferencesUtil.getService(), sharedPreferencesUtil.getCharFfe2(), true);
//                                waitSendData.remove(0);//移除
//                            } else {
//                                DeleteData();//清空待发送数据库中的表数据
//                                str_164b4f = "";
//                            }
//                        }
                    } else {
                        DeleteData();//清空待发送数据库中的表数据
                        str_164b4f = "";
                    }
                }

                //下发02534a
                if (six_back_data.equals("1f4b4f")) {
                    MainActivity.is_upload = true;
                }

                //下发02534a
                if (six_back_data.equals("02534a")) {
                    str_02534a = "02534a";
                }

                if ((!six_back_data.contains("310")) && str_164b4f.equals("") && (!six_back_data.equals("02534a")) && (!six_back_data.equals("1f4b4f"))) {//下发02534a
                    Log.e("url", "------------------------six_back_data==" + six_back_data + "   str_164b4f==" + str_164b4f);
                    if (str_02534a.equals("02534a")) {
                        str_1f4b4f = "02534a";
                        MainActivity.is_upload = true;
                        short[] upload = new short[]{0x02, 0x53, 0x4A};
                        short crc1 = 0;
                        crc1 = mBleService.appData_Crc(upload, crc1, upload.length);
                        String str_crc = Integer.toHexString(crc1);

                        if (str_crc.length() == 1) {//如果长度为1，那么DigitalTrans.hex2byte(str_crc)的时候会报错
                            str_crc = 0 + str_crc;
                        }

                        Log.e("url", "02_53_4a的str_CRC==" + str_crc);//55
                        byte[] crc = DigitalTrans.hex2byte(str_crc);//十六进制串转化为byte数组
                        Log.e("url", "02_53_4a的byte_crc==" + crc[0]);
                        byte[] head1 = new byte[]{0x02, 0x4F, 0x4B};

                        byte[] upload_data = DigitalTrans.byteMerger(head1, crc);//把头部和密码和crc加在一起
                        byte[] add_upload_data = DigitalTrans.byteMerger(upload_data, Const.add12);//补全16位

                        Log.e("url", "add_upload_data==" + add_upload_data.length);
                        byte[] encrypt_add_upload_data = null;
                        //加密
                        try {
                            encrypt_add_upload_data = AesEntryDetry.encrypt(add_upload_data);//加密
                        } catch (Exception e) {
                            e.printStackTrace();
                        }

                        //下发02 53 4a crc
                        mBleService.writeCharacteristic(sharedPreferencesUtil.getService(), sharedPreferencesUtil.getCharFfe1(), encrypt_add_upload_data);
                        str_02534a = "";
                    } else {
                        if (!str_1f4b4f.equals("02534a")) {
                            Log.e("url", "2------------------------");
                            MainActivity.is_upload = false;
                        }
                    }
                }

                //下发0x41, 0x53, 0x4A（上传手机用户账号）
                if (six_back_data.equals("41534a")) {
                    short[] user_info = new short[]{0x41, 0x53, 0x4A};
                    short crc1 = 0;
                    crc1 = mBleService.appData_Crc(user_info, crc1, user_info.length);
                    String str_crc = Integer.toHexString(crc1);

                    if (str_crc.length() == 1) {//如果长度为1，那么DigitalTrans.hex2byte(str_crc)的时候会报错
                        str_crc = 0 + str_crc;
                    }

                    Log.e("url", "41_53_4a的str_CRC==" + str_crc);

                    byte[] crc = DigitalTrans.hex2byte(str_crc);//十六进制串转化为byte数组
                    Log.e("url", "41_53_4a的byte_CRC====" + crc[0]);
                    byte[] head = new byte[]{0x03, 0x4F, 0x4B};
                    byte[] user_info_data1 = DigitalTrans.byteMerger(head, crc);//把头部和密码和crc加在一起
                    byte[] tail = new byte[]{0x41};
                    byte[] user_info_data = DigitalTrans.byteMerger(user_info_data1, tail);
                    byte[] add_user_info_data = DigitalTrans.byteMerger(user_info_data, Const.add11);//补全16位

                    Log.e("url", "add_user_info_data==" + add_user_info_data.length);
                    byte[] encrypt_add_user_info_data = null;
                    //加密
                    try {
                        encrypt_add_user_info_data = AesEntryDetry.encrypt(add_user_info_data);//加密
                    } catch (Exception e) {
                        e.printStackTrace();
                    }

                    str_phone_user_infos = "";
                    MainActivity.list_phone_user_info.clear();//数据加入数据库后，清空list

                    //下发03 4f 4b crc 41
                    mBleService.writeCharacteristic(sharedPreferencesUtil.getService(), sharedPreferencesUtil.getCharFfe1(), encrypt_add_user_info_data);

                }

                if (upload_flag.equals("41534a")) {//把手机用户保存在数据库
                    String phone_record_back_data = decrypt_back_data.substring(decrypt_back_data.length() - 32, decrypt_back_data.length() - 16);//前16位(0118965204111021)
                    str_phone_user_infos = str_phone_user_infos + phone_record_back_data;
//                    Log.e("url", "str_phone_user_infos==" +str_phone_user_infos);
                    MainActivity.list_phone_user_info.add(phone_record_back_data);
                    Log.e("url", "str_phone_user_infos.length()==" + str_phone_user_infos.length());
                    if (str_phone_user_infos.length() == 144) {

                        Log.e("url", " 手机用户账号的十组数据Str==" + str_phone_user_infos);

                        //下发上传手机用户账户，进行应答
                        byte[] head_check = new byte[]{0x02, 0x4F, 0x4B};
                        byte[] str_user_infos = DigitalTrans.hex2byte(str_phone_user_infos);//十六进制串转化为byte数组
                        short[] short_user_info_check = new short[str_user_infos.length];
                        for (int i = 0; i < str_user_infos.length; i++) {
                            short_user_info_check[i] = str_user_infos[i];
                        }

                        short crc1_check = 0;
                        crc1_check = mBleService.appData_Crc(short_user_info_check, crc1_check, short_user_info_check.length);
                        String str_crc_check = Integer.toHexString(crc1_check);

                        if (str_crc_check.length() == 1) {
                            str_crc_check = 0 + str_crc_check;
                        }

                        Log.e("url", "上传手机用户账号进行校验的str_CRC==" + str_crc_check);
                        byte[] crc_check = DigitalTrans.hex2byte(str_crc_check);//十六进制串转化为byte数组
                        Log.e("url", "上传手机用户账号进行校验的byte_CRC==" + crc_check[0]);

                        final byte[] user_info_check_data = DigitalTrans.byteMerger(head_check, crc_check);//把头部和密码和crc加在一起
                        byte[] add_user_info_check_data = DigitalTrans.byteMerger(user_info_check_data, Const.add12);//补全16位

                        Log.e("url", "add_user_info_check_data==" + add_user_info_check_data.length);
                        byte[] encrypt_add_user_info_check_data = null;
                        //加密
                        try {
                            encrypt_add_user_info_check_data = AesEntryDetry.encrypt(add_user_info_check_data);//加密
                        } catch (Exception e) {
                            e.printStackTrace();
                        }

                        //下发 02 4f 4b crc
                        mBleService.writeCharacteristic(sharedPreferencesUtil.getService(), sharedPreferencesUtil.getCharFfe1(), encrypt_add_user_info_check_data);

                        //手机用户
                        if (MainActivity.list_phone_user_info.size() > 0) {
                            DeletePhoneUserData();

                            for (int i = 0; i < MainActivity.list_phone_user_info.size(); i++) {
                                String str_data = MainActivity.list_phone_user_info.get(i);
                                String user_type = str_data.substring(str_data.length() - 2, str_data.length());//20是普通用户
                                String user_num = str_data.substring(str_data.length() - 14, str_data.length() - 3);//手机号码
                                String user_id = str_data.substring(str_data.length() - 16, str_data.length() - 14);//(手机用户（0#，1#。。）
                                String str_user = str_data;
                                Log.e("url", "  user_id==" + user_id + "  user_num==" + user_num + "  user_type==" + user_type + "  str_user==" + str_user);

                                UpdatePhoneUserTableData(user_type, user_id, user_num, str_user);//更新手机用户数据库
                            }
                        }
                        MainActivity.list_phone_user_info.clear();//数据加入数据库后，清空list
                        str_phone_user_infos = "";
                        upload_flag = "";
                    }
                    return;
                }

                //下发0x42, 0x53, 0x4A（上传管理员密码信息）
                if (six_back_data.equals("42534a")) {
                    short[] admin_pw_info = new short[]{0x42, 0x53, 0x4A};
                    short crc1 = 0;
                    crc1 = mBleService.appData_Crc(admin_pw_info, crc1, admin_pw_info.length);
                    String str_crc = Integer.toHexString(crc1);

                    if (str_crc.length() == 1) {//如果长度为1，那么DigitalTrans.hex2byte(str_crc)的时候会报错
                        str_crc = 0 + str_crc;
                    }

                    Log.e("url", "42_53_4a的str_CRC==" + str_crc);
                    byte[] crc = DigitalTrans.hex2byte(str_crc);//十六进制串转化为byte数组
                    Log.e("url", "42_53_4a的byte_CRC====" + crc[0]);
                    byte[] head = new byte[]{0x03, 0x4F, 0x4B};
                    byte[] admin_pw_info_data1 = DigitalTrans.byteMerger(head, crc);//把头部和密码和crc加在一起
                    byte[] tail = new byte[]{0x42};
                    byte[] admin_pw_info_data = DigitalTrans.byteMerger(admin_pw_info_data1, tail);
                    byte[] add_admin_pw_info_data = DigitalTrans.byteMerger(admin_pw_info_data, Const.add11);//补全16位

                    Log.e("url", "add_admin_pw_info_data==" + add_admin_pw_info_data.length);
                    byte[] encrypt_add_admin_pw_info_data = null;
                    //加密
                    try {
                        encrypt_add_admin_pw_info_data = AesEntryDetry.encrypt(add_admin_pw_info_data);//加密
                    } catch (Exception e) {
                        e.printStackTrace();
                    }

                    str_admin_pw = "";
                    MainActivity.list_admin_pw_info.clear();

                    //下发03 4f 4b crc 42
                    mBleService.writeCharacteristic(sharedPreferencesUtil.getService(), sharedPreferencesUtil.getCharFfe1(), encrypt_add_admin_pw_info_data);
                }

                if (upload_flag.equals("42534a")) {
                    String admin_pw__back_data = decrypt_back_data.substring(decrypt_back_data.length() - 32, decrypt_back_data.length() - 20);//前12位（343536373839）
                    Log.e("url", " 管理员密码==" + admin_pw__back_data);

                    str_admin_pw = str_admin_pw + admin_pw__back_data;
                    MainActivity.list_admin_pw_info.add(admin_pw__back_data);

                    if (str_admin_pw.length() == 12) {
                        Log.e("url", " 管理员密码数据Str==" + str_admin_pw);

                        //下发上传管理员密码信息后，进行应答
                        byte[] head_check = new byte[]{0x02, 0x4F, 0x4B};
                        byte[] str_admin_pw_info = DigitalTrans.hex2byte(str_admin_pw);//十六进制串转化为byte数组
                        short[] short_admin_pw_info_check = new short[str_admin_pw_info.length];
                        for (int i = 0; i < str_admin_pw_info.length; i++) {
                            short_admin_pw_info_check[i] = str_admin_pw_info[i];
                        }

                        short crc1_check = 0;
                        crc1_check = mBleService.appData_Crc(short_admin_pw_info_check, crc1_check, short_admin_pw_info_check.length);
                        String str_crc_check = Integer.toHexString(crc1_check);

                        if (str_crc_check.length() == 1) {
                            str_crc_check = 0 + str_crc_check;
                        }

                        Log.e("url", "上传管理员密码信息后进行校验的str_CRC==" + str_crc_check);//55
                        byte[] crc_check = DigitalTrans.hex2byte(str_crc_check);//十六进制串转化为byte数组
                        Log.e("url", "上传管理员密码信息后进行校验的byte_CRC==" + crc_check[0]);

                        final byte[] admin_pw_info_check_data = DigitalTrans.byteMerger(head_check, crc_check);//把头部和密码和crc加在一起
                        byte[] add_admin_pw_info_check_data = DigitalTrans.byteMerger(admin_pw_info_check_data, Const.add12);//补全16位

                        Log.e("url", "add_admin_pw_info_check_data==" + add_admin_pw_info_check_data.length);
                        byte[] encrypt_add_admin_pw_info_check_data = null;
                        //加密
                        try {
                            encrypt_add_admin_pw_info_check_data = AesEntryDetry.encrypt(add_admin_pw_info_check_data);//加密
                        } catch (Exception e) {
                            e.printStackTrace();
                        }

                        //下发 02 4f 4b crc
                        mBleService.writeCharacteristic(sharedPreferencesUtil.getService(), sharedPreferencesUtil.getCharFfe1(), encrypt_add_admin_pw_info_check_data);

                        //加载上传后的键盘管理员用户
                        if (MainActivity.list_admin_pw_info.size() > 0) {
                            for (int i = 0; i < MainActivity.list_admin_pw_info.size(); i++) {
                                String password = DigitalTrans.AsciiStringToString(MainActivity.list_admin_pw_info.get(i).toString());//ASCII码字符串转数字字符串
                                Log.e("url", "  password==" + password);

                                UpdateKeyboardUserTableData("9999", "admin", password, MainActivity.list_admin_pw_info.get(i).toString());//更新键盘用户数据库（管理员）
                            }
                        }
                        MainActivity.list_admin_pw_info.clear();//数据加入数据库后，清空list
                        str_admin_pw = "";
                        upload_flag = "";
                    }
                    return;
                }

                //键盘普通用户
                if (six_back_data.equals("43534a")) {
                    short[] keyboard_user_pw = new short[]{0x43, 0x53, 0x4A};
                    short crc1 = 0;
                    crc1 = mBleService.appData_Crc(keyboard_user_pw, crc1, keyboard_user_pw.length);
                    String str_crc = Integer.toHexString(crc1);

                    if (str_crc.length() == 1) {//如果长度为1，那么DigitalTrans.hex2byte(str_crc)的时候会报错
                        str_crc = 0 + str_crc;
                    }

                    Log.e("url", "43_53_4a的str_CRC==" + str_crc);//80

                    byte[] crc = DigitalTrans.hex2byte(str_crc);//十六进制串转化为byte数组
                    Log.e("url", "43_53_4a的byte_CRC==" + crc[0]);
                    byte[] head = new byte[]{0x03, 0x4F, 0x4B};
                    byte[] keyboard_user_pw_data1 = DigitalTrans.byteMerger(head, crc);//把头部和密码和crc加在一起
                    byte[] tail = new byte[]{0x43};
                    byte[] keyboard_user_pw_data = DigitalTrans.byteMerger(keyboard_user_pw_data1, tail);
                    byte[] add_keyboard_user_pw_data = DigitalTrans.byteMerger(keyboard_user_pw_data, Const.add11);//补全16位

                    Log.e("url", "add_keyboard_user_pw_data==" + add_keyboard_user_pw_data.length);
                    byte[] encrypt_add_keyboard_user_pw_data = null;
                    //加密
                    try {
                        encrypt_add_keyboard_user_pw_data = AesEntryDetry.encrypt(add_keyboard_user_pw_data);//加密
                    } catch (Exception e) {
                        e.printStackTrace();
                    }

                    str_ordinary_user_info = "";
                    MainActivity.list_ordinary_user_info.clear();

                    //下发 03 4f 4b crc 43
                    mBleService.writeCharacteristic(sharedPreferencesUtil.getService(), sharedPreferencesUtil.getCharFfe1(), encrypt_add_keyboard_user_pw_data);

                }

                if (upload_flag.equals("43534a")) {
                    String ordinary_user_back_data = decrypt_back_data.substring(decrypt_back_data.length() - 32, decrypt_back_data.length() - 18);//前14位（00313437323538）
                    Log.e("url", " 键盘用户密码==" + ordinary_user_back_data);

                    str_ordinary_user_info = str_ordinary_user_info + ordinary_user_back_data;
                    MainActivity.list_ordinary_user_info.add(ordinary_user_back_data);

                    Log.e("url", "str_ordinary_user_info.length()==" + str_ordinary_user_info.length());
                    if (str_ordinary_user_info.length() == 140) {
                        Log.e("url", "键盘用户密码信息十组数据的str==" + str_ordinary_user_info);

                        //下发上传键盘普通用户信息后，进行应答
                        byte[] head_check = new byte[]{0x02, 0x4F, 0x4B};
                        byte[] str_keyboard_user_pw_data = DigitalTrans.hex2byte(str_ordinary_user_info);//十六进制串转化为byte数组
                        short[] short_keyboard_user_pw_check = new short[str_keyboard_user_pw_data.length];
                        for (int i = 0; i < str_keyboard_user_pw_data.length; i++) {
                            short_keyboard_user_pw_check[i] = str_keyboard_user_pw_data[i];
                        }

                        short crc1_check = 0;
                        crc1_check = mBleService.appData_Crc(short_keyboard_user_pw_check, crc1_check, short_keyboard_user_pw_check.length);
                        String str_crc_check = Integer.toHexString(crc1_check);
                        Log.e("url", "上传键盘普通用户密码后进行校验的str_CRC==" + str_crc_check);
                        if (str_crc_check.length() == 1) {//如果长度为1，那么DigitalTrans.hex2byte(str_crc)的时候会报错
                            str_crc_check = 0 + str_crc_check;
                        }

                        byte[] crc_check = DigitalTrans.hex2byte(str_crc_check);//十六进制串转化为byte数组
                        Log.e("url", "上传键盘普通用户密码后进行校验的byter_CRC==" + crc_check);

                        final byte[] keyboard_user_pw_check_data = DigitalTrans.byteMerger(head_check, crc_check);//把头部和密码和crc加在一起
                        byte[] add_keyboard_user_pw_check_data = DigitalTrans.byteMerger(keyboard_user_pw_check_data, Const.add12);//补全16位

                        Log.e("url", "add_keyboard_user_pw_check_data==" + add_keyboard_user_pw_check_data.length);
                        byte[] encrypt_add_keyboard_user_pw_check_data = null;
                        //加密
                        try {
                            encrypt_add_keyboard_user_pw_check_data = AesEntryDetry.encrypt(add_keyboard_user_pw_check_data);//加密
                        } catch (Exception e) {
                            e.printStackTrace();
                        }

                        //下发 02 4f 4b crc
                        mBleService.writeCharacteristic(sharedPreferencesUtil.getService(), sharedPreferencesUtil.getCharFfe1(), encrypt_add_keyboard_user_pw_check_data);

                        //加载上传后的键盘普通用户
                        if (MainActivity.list_ordinary_user_info.size() > 0) {
                            disposable_user_db = disposableUserSQLiteOpenHelper.getWritableDatabase();
                            ContentValues values = new ContentValues();

                            //游标查询每条数据
                            Cursor cursor = disposable_user_db.query("Disposable_User_Table", null, "mac=? and type=?", new String[]{sharedPreferencesUtil.getMAC(), "ordinary"}, null, null, null);

                            while (cursor.moveToNext()) {
                                int count = cursor.getInt(0);
                                if (count > 0) {//数据存在
                                    values.put("password", "0");
                                    disposable_user_db.update("Disposable_User_Table", values, "mac=? and type=?", new String[]{sharedPreferencesUtil.getMAC(), "ordinary"});
                                }
                            }

                            for (int i = 0; i < MainActivity.list_ordinary_user_info.size(); i++) {
                                //01343536373839
                                String str_data = MainActivity.list_ordinary_user_info.get(i);
                                String ascii_pw = str_data.substring(str_data.length() - 12, str_data.length());//密码的ASCII值
                                String password = DigitalTrans.AsciiStringToString(ascii_pw);
                                String user_id = str_data.substring(str_data.length() - 14, str_data.length() - 12);//序号
                                String str_user = str_data;
                                Log.e("url", " 键盘普通用户： user_id==" + user_id + "  password==" + password + "  str_user==" + str_user);

                                UpdateKeyboardUserTableData(user_id, "ordinary", password, str_user);//更新键盘用户数据库（普通用户）
                            }
                        }
                        MainActivity.list_ordinary_user_info.clear();//数据加入数据库后，清空list
                        str_ordinary_user_info = "";
                        upload_flag = "";
                    }
                    return;
                }

                //下发0x44, 0x53, 0x4A（上传TM卡用户信息）
                if (six_back_data.equals("44534a")) {
                    short[] tm_card_user_info = new short[]{0x44, 0x53, 0x4A};
                    short crc1 = 0;
                    crc1 = mBleService.appData_Crc(tm_card_user_info, crc1, tm_card_user_info.length);
                    String str_crc = Integer.toHexString(crc1);

                    if (str_crc.length() == 1) {//如果长度为1，那么DigitalTrans.hex2byte(str_crc)的时候会报错
                        str_crc = 0 + str_crc;
                    }

                    Log.e("url", "44_53_4a的str_CRC==" + str_crc);

                    byte[] crc = DigitalTrans.hex2byte(str_crc);//十六进制串转化为byte数组
                    Log.e("url", "44_53_4a的byte_CRC==" + crc[0]);
                    byte[] head = new byte[]{0x03, 0x4F, 0x4B};
                    byte[] tm_card_user_info_data1 = DigitalTrans.byteMerger(head, crc);//把头部和密码和crc加在一起
                    byte[] tail = new byte[]{0x44};
                    byte[] tm_card_user_info_data = DigitalTrans.byteMerger(tm_card_user_info_data1, tail);
                    byte[] add_tm_card_user_info_data = DigitalTrans.byteMerger(tm_card_user_info_data, Const.add11);//补全16位

                    Log.e("url", "add_tm_card_user_info_data==" + add_tm_card_user_info_data.length);
                    byte[] encrypt_add_tm_card_user_info_data = null;
                    //加密
                    try {
                        encrypt_add_tm_card_user_info_data = AesEntryDetry.encrypt(add_tm_card_user_info_data);//加密
                    } catch (Exception e) {
                        e.printStackTrace();
                    }

                    str_tm_card_user_info = "";
                    MainActivity.list_tm_card_user_info.clear();

                    //下发 03 4f 4b crc 44
                    mBleService.writeCharacteristic(sharedPreferencesUtil.getService(), sharedPreferencesUtil.getCharFfe1(), encrypt_add_tm_card_user_info_data);
                }

                if (upload_flag.equals("44534a")) {
                    String tm_card_user_back_data = decrypt_back_data.substring(decrypt_back_data.length() - 32, decrypt_back_data.length() - 20);//前12位（00313437323538）
                    Log.e("url", "TM卡用户==" + tm_card_user_back_data);

                    str_tm_card_user_info = str_tm_card_user_info + tm_card_user_back_data;
                    MainActivity.list_tm_card_user_info.add(tm_card_user_back_data);
                    Log.e("url", "list_tm_card_user_info.size==" + MainActivity.list_tm_card_user_info.size());

                    if (str_tm_card_user_info.length() == 120) {
                        Log.e("url", "TM卡用户信息十组数据的str==" + str_tm_card_user_info);

                        //下发上传键盘普通用户信息后，进行应答
                        byte[] head_check = new byte[]{0x02, 0x4F, 0x4B};
                        byte[] str_tm_card_user_data = DigitalTrans.hex2byte(str_tm_card_user_info);//十六进制串转化为byte数组
                        short[] short_tm_card_user_check = new short[str_tm_card_user_data.length];
                        for (int i = 0; i < str_tm_card_user_data.length; i++) {
                            short_tm_card_user_check[i] = str_tm_card_user_data[i];
                        }

                        short crc1_check = 0;
                        crc1_check = mBleService.appData_Crc(short_tm_card_user_check, crc1_check, short_tm_card_user_check.length);
                        String str_crc_check = Integer.toHexString(crc1_check);
                        Log.e("url", "上传TM卡用户后进行校验的str_CRC==" + str_crc_check);

                        if (str_crc_check.length() == 1) {//如果长度为1，那么DigitalTrans.hex2byte(str_crc)的时候会报错
                            str_crc_check = 0 + str_crc_check;
                        }

                        byte[] crc_check = DigitalTrans.hex2byte(str_crc_check);//十六进制串转化为byte数组
                        Log.e("url", "上传TM卡用户后进行校验的byter_CRC==" + crc_check);

                        final byte[] tm_card_user__check_data = DigitalTrans.byteMerger(head_check, crc_check);//把头部和密码和crc加在一起
                        byte[] add_tm_card_user__check_data = DigitalTrans.byteMerger(tm_card_user__check_data, Const.add12);//补全16位

                        Log.e("url", "add_tm_card_user__check_data==" + add_tm_card_user__check_data.length);
                        byte[] encrypt_add_tm_card_user__check_data = null;
                        //加密
                        try {
                            encrypt_add_tm_card_user__check_data = AesEntryDetry.encrypt(add_tm_card_user__check_data);//加密
                        } catch (Exception e) {
                            e.printStackTrace();
                        }

                        //下发 02 4f 4b crc
                        mBleService.writeCharacteristic(sharedPreferencesUtil.getService(), sharedPreferencesUtil.getCharFfe1(), encrypt_add_tm_card_user__check_data);

                        //加载上传后的TM卡用户
                        if (MainActivity.list_tm_card_user_info.size() > 0) {
                            for (int i = 0; i < MainActivity.list_tm_card_user_info.size(); i++) {

                                String user_num = MainActivity.list_tm_card_user_info.get(i);
                                String user_id = i + "";
                                Log.e("url", "user_id==" + user_id + "  user_num==" + user_num);

                                UpdateTMCardUserTableData(user_id, user_num);//更新TM卡用户数据库
                            }
                        }
                        MainActivity.list_tm_card_user_info.clear();//数据加入数据库后，清空list
                        str_tm_card_user_info = "";
                        upload_flag = "";
                    }
                    return;
                }

                //下发0x45, 0x53, 0x4A（上传最近20组开门记录）
                if (six_back_data.equals("45534a")) {
                    short[] unlock_record = new short[]{0x45, 0x53, 0x4A};
                    short crc1 = 0;
                    crc1 = mBleService.appData_Crc(unlock_record, crc1, unlock_record.length);
                    String str_crc = Integer.toHexString(crc1);

                    if (str_crc.length() == 1) {//如果长度为1，那么DigitalTrans.hex2byte(str_crc)的时候会报错
                        str_crc = 0 + str_crc;
                    }

                    Log.e("url", "45_53_4a的str_CRC==" + str_crc);

                    byte[] crc = DigitalTrans.hex2byte(str_crc);//十六进制串转化为byte数组
                    Log.e("url", "45_53_4a的byte_CRC==" + crc[0]);
                    byte[] head = new byte[]{0x03, 0x4F, 0x4B};
                    byte[] unlock_record_data1 = DigitalTrans.byteMerger(head, crc);//把头部和密码和crc加在一起
                    byte[] tail = new byte[]{0x45};
                    final byte[] unlock_record_data = DigitalTrans.byteMerger(unlock_record_data1, tail);
                    byte[] add_unlock_record_data = DigitalTrans.byteMerger(unlock_record_data, Const.add11);//补全16位

                    Log.e("url", "add_unlock_record_data==" + add_unlock_record_data.length);
                    byte[] encrypt_add_unlock_record_data = null;
                    //加密
                    try {
                        encrypt_add_unlock_record_data = AesEntryDetry.encrypt(add_unlock_record_data);//加密
                    } catch (Exception e) {
                        e.printStackTrace();
                    }

                    str_unlock_record_info = "";
                    MainActivity.list_unlock_record_info.clear();

                    //下发 03 4f 4b crc 45
                    mBleService.writeCharacteristic(sharedPreferencesUtil.getService(), sharedPreferencesUtil.getCharFfe1(), encrypt_add_unlock_record_data);
                }

                if (upload_flag.equals("45534a")) {//保存开锁记录到数据库
                    String unlock_record_back_data = decrypt_back_data.substring(decrypt_back_data.length() - 32, decrypt_back_data.length() - 14);//前18位

                    Log.e("url", "开锁记录==" + unlock_record_back_data);

                    str_unlock_record_info = str_unlock_record_info + unlock_record_back_data;
                    MainActivity.list_unlock_record_info.add(unlock_record_back_data);
                    Log.e("url", "组数==" + MainActivity.list_unlock_record_info.size());

                    if (str_unlock_record_info.length() == 270) {

                        Log.e("url", " 开门记录20组数据的str==" + str_unlock_record_info);

                        //下发上传20组开门记录后，进行应答
                        byte[] head_check = new byte[]{0x02, 0x4F, 0x4B};
                        byte[] str_unlock_record = DigitalTrans.hex2byte(str_unlock_record_info);//十六进制串转化为byte数组
                        short[] short_unlock_record_check = new short[str_unlock_record.length];
                        for (int i = 0; i < str_unlock_record.length; i++) {
                            short_unlock_record_check[i] = str_unlock_record[i];
                        }

                        short crc1_check = 0;
                        crc1_check = mBleService.appData_Crc(short_unlock_record_check, crc1_check, short_unlock_record_check.length);
                        String str_crc_check = Integer.toHexString(crc1_check);
                        Log.e("url", "上传开锁记录后进行校验的str_CRC==" + str_crc_check);//80

                        if (str_crc_check.length() == 1) {
                            str_crc_check = 0 + str_crc_check;
                        }

                        byte[] crc_check = DigitalTrans.hex2byte(str_crc_check);//十六进制串转化为byte数组
                        Log.e("url", "上传开锁记录后进行校验的byte_CRC==" + crc_check[0]);

                        final byte[] user_info_check_data = DigitalTrans.byteMerger(head_check, crc_check);//把头部和密码和crc加在一起
                        byte[] add_user_info_check_data = DigitalTrans.byteMerger(user_info_check_data, Const.add12);//补全16位

                        Log.e("url", "add_user_info_check_data==" + add_user_info_check_data.length);
                        byte[] encrypt_add_user_info_check_data = null;
                        //加密
                        try {
                            encrypt_add_user_info_check_data = AesEntryDetry.encrypt(add_user_info_check_data);//加密
                        } catch (Exception e) {
                            e.printStackTrace();
                        }

                        //下发02 4f 4b crc
                        mBleService.writeCharacteristic(sharedPreferencesUtil.getService(), sharedPreferencesUtil.getCharFfe1(), encrypt_add_user_info_check_data);

                        //加载上传后的开锁记录
                        if (MainActivity.list_unlock_record_info.size() > 0) {
                            for (int i = 0; i < MainActivity.list_unlock_record_info.size(); i++) {
                                String str_data = MainActivity.list_unlock_record_info.get(i);
                                String second = str_data.substring(str_data.length() - 2, str_data.length());//秒
                                String minute = str_data.substring(str_data.length() - 4, str_data.length() - 2);//分
                                String hour = str_data.substring(str_data.length() - 6, str_data.length() - 4);//时
                                String day = str_data.substring(str_data.length() - 8, str_data.length() - 6);//日
                                String month = str_data.substring(str_data.length() - 10, str_data.length() - 8);//月
                                String year = str_data.substring(str_data.length() - 12, str_data.length() - 10);//年
                                String user_type_num = str_data.substring(str_data.length() - 14, str_data.length() - 12);//用户类型中的第几个用户
                                //用户类型（1、普通用户 2、手机 3、TM卡 4、单次 5、一天 6、一星期 7、一个月）
                                String user_type = str_data.substring(str_data.length() - 16, str_data.length() - 14);
                                String num = str_data.substring(str_data.length() - 18, str_data.length() - 16);//第几组开门记录

                                String year_month = "20" + year + "年" + month + "月";

                                if (!str_data.equals("000000000000000000")) {
                                    AddTableData(num, user_type, user_type_num, year_month, year, month, day, hour, minute, second);
                                }
                            }
                        }
                        MainActivity.list_unlock_record_info.clear();//数据加入数据库后，清空list
                        str_unlock_record_info = "";
                        upload_flag = "";
                    }
                    return;
                }

                //下发0x46, 0x53, 0x4A（上传期限密码中单次的十组密码）
                if (six_back_data.equals("46534a")) {
                    short[] one_time_pw = new short[]{0x46, 0x53, 0x4A};
                    short crc1 = 0;
                    crc1 = mBleService.appData_Crc(one_time_pw, crc1, one_time_pw.length);
                    String str_crc = Integer.toHexString(crc1);

                    if (str_crc.length() == 1) {//如果长度为1，那么DigitalTrans.hex2byte(str_crc)的时候会报错
                        str_crc = 0 + str_crc;
                    }

                    Log.e("url", "46_53_4a的str_CRC==" + str_crc);

                    byte[] crc = DigitalTrans.hex2byte(str_crc);//十六进制串转化为byte数组
                    Log.e("url", "46_53_4a的byte_CRC==" + crc[0]);
                    byte[] head = new byte[]{0x03, 0x4F, 0x4B};
                    byte[] one_time_pw_data1 = DigitalTrans.byteMerger(head, crc);//把头部和密码和crc加在一起
                    byte[] tail = new byte[]{0x46};
                    byte[] one_time_pw_data = DigitalTrans.byteMerger(one_time_pw_data1, tail);
                    byte[] add_one_time_pw_data = DigitalTrans.byteMerger(one_time_pw_data, Const.add11);//补全16位

                    Log.e("url", "add_one_time_pw_data==" + add_one_time_pw_data.length);
                    byte[] encrypt_add_one_time_pw_data = null;
                    //加密
                    try {
                        encrypt_add_one_time_pw_data = AesEntryDetry.encrypt(add_one_time_pw_data);//加密
                    } catch (Exception e) {
                        e.printStackTrace();
                    }

                    str_one_time_pw_info = "";
                    MainActivity.list_one_time_pw_info.clear();

                    //下发 03 4f 4b crc 46
                    mBleService.writeCharacteristic(sharedPreferencesUtil.getService(), sharedPreferencesUtil.getCharFfe1(), encrypt_add_one_time_pw_data);
                }

                if (upload_flag.equals("46534a")) {
                    String disposable_user_back_data = decrypt_back_data.substring(decrypt_back_data.length() - 32, decrypt_back_data.length() - 18);//前14位
                    Log.e("url", "期限单次用户==" + disposable_user_back_data);

                    str_one_time_pw_info = str_one_time_pw_info + disposable_user_back_data;
                    MainActivity.list_one_time_pw_info.add(disposable_user_back_data);
                    Log.e("url", "组数==" + MainActivity.list_one_time_pw_info.size());

                    if (str_one_time_pw_info.length() == 140) {
                        Log.e("url", " 单次密码十组数据的str==" + str_one_time_pw_info);

                        //下发上传10组开锁密码后，进行应答
                        byte[] head_check = new byte[]{0x02, 0x4F, 0x4B};
                        byte[] str_one_time_pw = DigitalTrans.hex2byte(str_one_time_pw_info);//十六进制串转化为byte数组
                        short[] short_one_time_pw_check = new short[str_one_time_pw.length];
                        for (int i = 0; i < str_one_time_pw.length; i++) {
                            short_one_time_pw_check[i] = str_one_time_pw[i];
//                Log.e("url", "short_one_time_pw_check==" + short_one_time_pw_check[i]);
                        }

                        short crc1_check = 0;
                        crc1_check = mBleService.appData_Crc(short_one_time_pw_check, crc1_check, short_one_time_pw_check.length);
                        String str_crc_check = Integer.toHexString(crc1_check);
                        Log.e("url", "上传期限密码中单次的10组密码后进行校验的str_CRC==" + str_crc_check);//80

                        if (str_crc_check.length() == 1) {
                            str_crc_check = 0 + str_crc_check;
                        }
                        byte[] crc_check = DigitalTrans.hex2byte(str_crc_check);//十六进制串转化为byte数组
                        Log.e("url", "上传期限密码中单次的10组密码后进行校验的byte_CRC==" + crc_check);

                        final byte[] one_time_pw_check_data = DigitalTrans.byteMerger(head_check, crc_check);//把头部和密码和crc加在一起
                        byte[] add_one_time_pw_check_data = DigitalTrans.byteMerger(one_time_pw_check_data, Const.add12);//补全16位

                        Log.e("url", "add_one_time_pw_check_data==" + add_one_time_pw_check_data.length);
                        byte[] encrypt_add_one_time_pw_check_data = null;
                        //加密
                        try {
                            encrypt_add_one_time_pw_check_data = AesEntryDetry.encrypt(add_one_time_pw_check_data);//加密
                        } catch (Exception e) {
                            e.printStackTrace();
                        }

                        //下发 02 4f 4b crc
                        mBleService.writeCharacteristic(sharedPreferencesUtil.getService(), sharedPreferencesUtil.getCharFfe1(), encrypt_add_one_time_pw_check_data);

                        //加载上传后的期限单次用户
                        if (MainActivity.list_one_time_pw_info.size() > 0) {
                            InitializeDisposableOnceUserTableData();

                            for (int i = 0; i < MainActivity.list_one_time_pw_info.size(); i++) {
                                //01343536373839
                                String str_data = MainActivity.list_one_time_pw_info.get(i);
                                String ascii_pw = str_data.substring(str_data.length() - 12, str_data.length());//密码的ASCII值
                                String password = DigitalTrans.AsciiStringToString(ascii_pw);
                                String user_id = str_data.substring(str_data.length() - 14, str_data.length() - 12);//序号
                                String str_user = str_data;
                                Log.e("url", "  user_id==" + user_id + "  password==" + password);

//                if (!password.equals("bbbbbbbbbbbb")) {
                                if (!(ascii_pw.equals("bbbbbbbbbbbb") | ascii_pw.equals("000000000000") | ascii_pw.equals("0000bbbbbbbb"))) {
                                    UpdateDisposableOnceUserTableData(user_id, password);
                                }
                            }
                        }
                        MainActivity.list_one_time_pw_info.clear();//数据加入数据库后，清空list
                        str_one_time_pw_info = "";
                        upload_flag = "";
                    }
                    return;
                }

                //检验和正确，上传结束
                if (six_back_data.equals("47534a")) {
//                    MainActivity.is_upload = false;
//
//                    try {
//                        Thread.sleep(2000);
//                    } catch (InterruptedException e) {
//                        e.printStackTrace();
//                    }

                    Log.e("url", "47534a--------------------------------------------");
                    short[] check_upload = new short[]{0x47, 0x53, 0x4A};
                    short crc1 = 0;
                    crc1 = mBleService.appData_Crc(check_upload, crc1, check_upload.length);
                    String str_crc = Integer.toHexString(crc1);

                    if (str_crc.length() == 1) {//如果长度为1，那么DigitalTrans.hex2byte(str_crc)的时候会报错
                        str_crc = 0 + str_crc;
                    }

                    Log.e("url", "校验上传是否成功的str_CRC==" + str_crc);//80

                    byte[] crc = DigitalTrans.hex2byte(str_crc);//十六进制串转化为byte数组
                    Log.e("url", "校验上传是否成功的byte_CRC==" + crc[0]);
                    byte[] head = new byte[]{0x03, 0x4F, 0x4B};
                    byte[] check_upload_data1 = DigitalTrans.byteMerger(head, crc);//把头部和密码和crc加在一起
                    byte[] tail = new byte[]{0x47};
                    final byte[] check_upload_data = DigitalTrans.byteMerger(check_upload_data1, tail);
                    byte[] add_check_upload_data = DigitalTrans.byteMerger(check_upload_data, Const.add11);//补全16位

                    Log.e("url", "add_check_upload_data==" + add_check_upload_data.length);
                    byte[] encrypt_add_check_upload_data = null;
                    //加密
                    try {
                        encrypt_add_check_upload_data = AesEntryDetry.encrypt(add_check_upload_data);//加密
                    } catch (Exception e) {
                        e.printStackTrace();
                    }

                    //下发 03 4f 4b crc 47
                    mBleService.writeCharacteristic(sharedPreferencesUtil.getService(), sharedPreferencesUtil.getCharFfe1(), encrypt_add_check_upload_data);

                }

                if(six_back_data.equals("112233")){
                    Log.e("url", "上传成功");
                    mActivity.runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            Toast.makeText(mActivity, "上传成功！", Toast.LENGTH_SHORT).show();
                            MainActivity.is_upload = false;
                            upload_flag = "flag";
                        }
                    });

                }

                //发送实时的RSSI
                if (!readRSSI.isAlive()) {
                    isReadRssi = true;
                    readRSSI.start();
                }

                upload_flag = six_back_data;
                Log.e("url", "upload_flag==" + upload_flag);
            }

            @Override
            public void onDescriptorRead(BluetoothGatt gatt, BluetoothGattDescriptor descriptor, int status) {

            }
        });
    }
}

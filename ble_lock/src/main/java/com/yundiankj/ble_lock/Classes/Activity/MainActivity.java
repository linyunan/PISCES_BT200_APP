package com.yundiankj.ble_lock.Classes.Activity;

import android.app.Activity;
import android.database.sqlite.SQLiteDatabase;
import android.os.Bundle;
import android.support.v4.app.FragmentActivity;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentTransaction;
import android.util.Log;
import android.view.KeyEvent;
import android.view.View;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.yundiankj.ble_lock.Classes.Fragment.EquipMentFg;
import com.yundiankj.ble_lock.Classes.Fragment.FindFg;
import com.yundiankj.ble_lock.Classes.Fragment.HelpFg;
import com.yundiankj.ble_lock.R;
import com.yundiankj.ble_lock.Resource.SQLite.DisposableUserSQLiteOpenHelper;
import com.yundiankj.ble_lock.Resource.SQLite.PhoneUserSQLiteOpenHelper;
import com.yundiankj.ble_lock.Resource.SharedPreferencesUtil;
import com.yundiankj.ble_lock.Resource.StatusBarUtil;

import java.util.ArrayList;
import java.util.List;

public class MainActivity extends FragmentActivity implements View.OnClickListener {

    private Activity mActivity;
    private FrameLayout fly_main;
    private SharedPreferencesUtil sharedPreferencesUtil;
    private LinearLayout lly_equipment, lly_find, lly_help;
    private ImageView img_equipment, img_find, img_help;
    private TextView tv_equipment, tv_find, tv_help;
    private FragmentManager fragmentManager;
    private EquipMentFg equipMentFg;
    private FindFg findFg;
    private HelpFg helpFg;

    //数据库
    private PhoneUserSQLiteOpenHelper phoneUserSQLiteOpenHelper;//手机用户数据库
    private SQLiteDatabase phone_user_db;
    private DisposableUserSQLiteOpenHelper disposableUserSQLiteOpenHelper;//期限用户数据库
    private SQLiteDatabase disposable_user_db;

    //手机用户列表
    public static List<String> list_phone_user_info = new ArrayList<String>();
    public static List<String> list_phone_user_info_32 = new ArrayList<String>();
    //管理员密码列表
    public static List<String> list_admin_pw_info = new ArrayList<String>();
    public static List<String> list_admin_pw_info_32 = new ArrayList<String>();
    //键盘普通用户列表
    public static List<String> list_ordinary_user_info = new ArrayList<String>();
    public static List<String> list_ordinary_user_info_32 = new ArrayList<String>();
    //TM卡用户列表
    public static List<String> list_tm_card_user_info = new ArrayList<String>();
    public static List<String> list_tm_card_user_info_32 = new ArrayList<String>();
    //开锁记录列表
    public static List<String> list_unlock_record_info = new ArrayList<String>();
    public static List<String> list_unlock_record_info_32 = new ArrayList<String>();
    //期限密码中的单次密码列表
    public static List<String> list_one_time_pw_info = new ArrayList<String>();
    public static List<String> list_one_time_pw_info_32 = new ArrayList<String>();

    public static boolean is_upload=false;//false 下发实时的rssi       true 停止下发实时的rssi
    public static boolean is_scan=false;//false 扫描蓝牙     true 停止扫描蓝牙
    public static boolean send_phone=true;//下发手机
    public static boolean is_mainactivity=true;
    public static boolean is_EquipMentFg=true;
    public static int refresh=0;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        mActivity = this;
        initWindow();//沉浸式状态栏
        init();

    }

    /**
     * 头部沉浸式状态栏
     */
    private void initWindow() {
        StatusBarUtil.setColor(mActivity, getResources().getColor(R.color.A04a8ec), 0);
    }

    private void init() {
        sharedPreferencesUtil = SharedPreferencesUtil.getInstance(mActivity);

        phoneUserSQLiteOpenHelper = new PhoneUserSQLiteOpenHelper(mActivity, "UserDatabase.db", null, 2);
        phoneUserSQLiteOpenHelper.getWritableDatabase();//创建用户表
        disposableUserSQLiteOpenHelper = new DisposableUserSQLiteOpenHelper(mActivity, "DisposableUserDatabase.db", null, 2);
        disposableUserSQLiteOpenHelper.getWritableDatabase();//创建期限用户表

        fly_main = (FrameLayout) mActivity.findViewById(R.id.fly_main);
        lly_equipment = (LinearLayout) mActivity.findViewById(R.id.lly_equipment);
        lly_find = (LinearLayout) mActivity.findViewById(R.id.lly_find);
        lly_help = (LinearLayout) mActivity.findViewById(R.id.lly_help);
        img_equipment = (ImageView) mActivity.findViewById(R.id.img_equipment);
        img_find = (ImageView) mActivity.findViewById(R.id.img_find);
        img_help = (ImageView) mActivity.findViewById(R.id.img_help);
        tv_equipment = (TextView) mActivity.findViewById(R.id.tv_equipment);
        tv_find = (TextView) mActivity.findViewById(R.id.tv_find);
        tv_help = (TextView) mActivity.findViewById(R.id.tv_help);

        fragmentManager = getSupportFragmentManager();
        FragmentTransaction ft = fragmentManager.beginTransaction();

        equipMentFg = new EquipMentFg();//设备
        findFg = new FindFg();//发现
        helpFg = new HelpFg();//帮助

        lly_equipment.setSelected(true);
        tv_equipment.setSelected(true);
        ft.add(R.id.fly_main, equipMentFg, "one");
        ft.commitAllowingStateLoss();

        lly_equipment.setOnClickListener(this);
        lly_find.setOnClickListener(this);
        lly_help.setOnClickListener(this);

    }

    /**
     * 点击响应事件
     * @param v
     */
    @Override
    public void onClick(View v) {
        FragmentTransaction ft;
        switch (v.getId()) {
            case R.id.lly_equipment://设备
                refresh=1;
                is_EquipMentFg=true;
                Log.e("url","点击“设备”"+"          refresh=="+refresh);
                send_phone=true;
                ft = fragmentManager.beginTransaction();
                if (lly_equipment.isSelected()) {
                    return;
                }
                if (findFg.isAdded()) {
                    ft.hide(findFg);
                    findFg.onPause();
                }
                if (helpFg.isAdded()) {
                    ft.hide(helpFg);
                    helpFg.onPause();
                }
                ft.show(equipMentFg);
                ft.commit();

                lly_equipment.setSelected(true);
                lly_find.setSelected(false);
                lly_help.setSelected(false);
                img_equipment.setImageResource(R.mipmap.nav_shebei);
                img_find.setImageResource(R.mipmap.nav_no_find);
                img_help.setImageResource(R.mipmap.nav_no_help);
                tv_equipment.setSelected(true);
                tv_find.setSelected(false);
                tv_help.setSelected(false);

                break;
            case R.id.lly_find://发现
                Log.e("url","发现----------------------------");
                send_phone=false;
                EquipMentFg.scanBleList.clear();
                EquipMentFg.bluetoothDeviceList.clear();
                is_EquipMentFg=false;
                ft = fragmentManager.beginTransaction();
                if (lly_find.isSelected()) {
                    return;
                }
                if (equipMentFg.isAdded()) {
                    ft.hide(equipMentFg);
                    equipMentFg.onPause();
                }
                if (helpFg.isAdded()) {
                    ft.hide(helpFg);
                    helpFg.onPause();
                }
                if (findFg.isAdded()) {
                    ft.show(findFg);
                } else {
                    ft.add(R.id.fly_main, findFg, "two");
                    ft.show(findFg);
                }
                ft.commit();

                lly_equipment.setSelected(false);
                lly_find.setSelected(true);
                lly_help.setSelected(false);
                img_equipment.setImageResource(R.mipmap.nav_no_shebei);
                img_find.setImageResource(R.mipmap.nav_find);
                img_help.setImageResource(R.mipmap.nav_no_help);
                tv_equipment.setSelected(false);
                tv_find.setSelected(true);
                tv_help.setSelected(false);
                break;
            case R.id.lly_help://帮助
                send_phone=false;
                ft = fragmentManager.beginTransaction();
                if (lly_help.isSelected()) {
                    return;
                }
                if (equipMentFg.isAdded()) {
                    ft.hide(equipMentFg);
                    equipMentFg.onPause();
                }
                if (findFg.isAdded()) {
                    ft.hide(findFg);
                    findFg.onPause();
                }
                if (helpFg.isAdded()) {
                    ft.show(helpFg);
                } else {
                    ft.add(R.id.fly_main, helpFg, "three");
                    ft.show(helpFg);
                }
                ft.commit();

                lly_equipment.setSelected(false);
                lly_find.setSelected(false);
                lly_help.setSelected(true);
                img_equipment.setImageResource(R.mipmap.nav_no_shebei);
                img_find.setImageResource(R.mipmap.nav_no_find);
                img_help.setImageResource(R.mipmap.nav_help);
                tv_equipment.setSelected(false);
                tv_find.setSelected(false);
                tv_help.setSelected(true);
                break;
        }
    }

    /**
     * 双击返回两次退出程序
     */
    private long exitTime = 0;
    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {
        if (keyCode == KeyEvent.KEYCODE_BACK) {
            exit();
            return false;
        }
        return super.onKeyDown(keyCode, event);
    }

    public void exit() {
        if ((System.currentTimeMillis() - exitTime) > 2000) {
            Toast.makeText(getApplicationContext(), "再按一次退出程序",
                    Toast.LENGTH_SHORT).show();
            exitTime = System.currentTimeMillis();
        } else {
            finish();
            System.exit(0);
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        Log.e("url","MainActivity_onResume");
        is_mainactivity=true;
    }
}

package com.yundiankj.ble_lock.Classes.Activity;

import android.app.Activity;
import android.content.ContentValues;
import android.content.Intent;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.graphics.drawable.ColorDrawable;
import android.media.MediaPlayer;
import android.os.Bundle;
import android.util.Log;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.PopupWindow;
import android.widget.RelativeLayout;
import android.widget.SeekBar;
import android.widget.TextView;

import com.yundiankj.ble_lock.Resource.AesEntryDetry;
import com.yundiankj.ble_lock.Resource.BleService;
import com.yundiankj.ble_lock.Classes.Activity.PassWordManage.PassWordManage;
import com.yundiankj.ble_lock.Classes.Activity.UserManage.UserManage;
import com.yundiankj.ble_lock.R;
import com.yundiankj.ble_lock.Resource.Const;
import com.yundiankj.ble_lock.Resource.DigitalTrans;
import com.yundiankj.ble_lock.Resource.SQLite.BleSQLiteOpenHelper;
import com.yundiankj.ble_lock.Resource.SQLite.DisposableUserSQLiteOpenHelper;
import com.yundiankj.ble_lock.Resource.SQLite.PhoneUserSQLiteOpenHelper;
import com.yundiankj.ble_lock.Resource.SQLite.WaitSendDataSQLiteOpenHelper;
import com.yundiankj.ble_lock.Resource.ScreenObserver;
import com.yundiankj.ble_lock.Resource.SharedPreferencesUtil;
import com.yundiankj.ble_lock.Resource.StatusBarUtil;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Created by hong on 2016/7/18.
 * <p/>
 * 蓝牙详情
 */
public class BLEDetails extends Activity implements View.OnClickListener, SeekBar.OnSeekBarChangeListener {

    private Activity mActivity;
    private SharedPreferencesUtil sharedPreferencesUtil;
    private ImageView img_back, img_link_sounds, img_unlock_sounds, img_unlock_way;
    private TextView tv_equipment_name, tv_auto_unlock, tv_unlock_distance, tv_m, tv_light_screen;
    private LinearLayout lly_unlock_way, lly_admin_power;
    private RelativeLayout rly_user_manage, rly_password_manage, rly_unlock_record, rly_about_equipment,
            rly_unlock_way, rly_auto_unlock, rly_light_screen_unlock;

    //连接提示音popwindow
    private PopupWindow pop_connect_sounds;
    private LinearLayout lly_connect_no_music, lly_connect_music1, lly_connect_music2;
    private ImageView img_connect_music_icon, img_connect_music_icon1, img_connect_music_icon2;
    private TextView tv_connect_music_name, tv_connect_music_name1, tv_connect_music_name2;
    private TextView tv_connect_ok;
    private boolean isLink_sounds = false;//连接提示音
    private String coneect_music_type = "0";

    //开锁提示音popwindow
    private PopupWindow pop_unlock_sounds;
    private LinearLayout lly_unlock_no_music, lly_unlock_music1;
    private ImageView img_unlock_music_icon, img_unlock_music_icon1;
    private TextView tv_unlock_music_name, tv_unlock_music_name1;
    private TextView tv_unlock_ok;
    private boolean isUnlock_sounds = false;//开锁提示音
    private String unlock_music_type = "0";

    //设备感应距离popwindow
    private PopupWindow pop_response_distance;
    private TextView tv_distance, tv_start_distance, tv_end_distance, tv_response_distance_ok;
    private SeekBar sb_distance;
    private int start_distance = 1, end_distance = 10;
    private int current_distance;

    //返回首页popWindow
    private PopupWindow pop_back;
    private TextView tv_cancel, tv_ok;

    //数据库
    private DisposableUserSQLiteOpenHelper disposableUserSQLiteOpenHelper;//期限用户数据库
    private SQLiteDatabase disposable_user_db;
    private WaitSendDataSQLiteOpenHelper waitSendDataSQLiteOpenHelper; //未发送成功的数据库
    private SQLiteDatabase wait_send_data_db;
    private BleSQLiteOpenHelper bleSQLiteOpenHelper;//蓝牙数据库
    private SQLiteDatabase ble_db;
    private PhoneUserSQLiteOpenHelper phoneUserSQLiteOpenHelper;//手机用户数据库
    private SQLiteDatabase phone_user_db;

    private List<Map<String, Object>> adminUserData;//定义一个列表储存 管理员用户数据
    private List<Map<String, Object>> disposableUserData; //定义一个列表储存 用户数据
    private List<Map<String, Object>> bleData;//定义一个列表储存 蓝牙数据


    private ScreenObserver mScreenObserver;//监听屏幕
    private MediaPlayer music = null;// 播放器引用
    public static String ble_name, mac;
    private String user_type, type;
    private boolean isUnlock_way = true;//开锁方式
    String wait_send_data;

    private List<Map<String, Object>> phoneUserData;  //定义一个列表储存 手机用户数据

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.ble_details);

        mActivity = this;
        initWindow();//沉浸式状态栏
        init();
    }

    private void initWindow() {
        StatusBarUtil.setColor(mActivity, getResources().getColor(R.color.A04a8ec), 0);
    }

    private void init() {
        sharedPreferencesUtil = SharedPreferencesUtil.getInstance(mActivity);
        waitSendDataSQLiteOpenHelper = new WaitSendDataSQLiteOpenHelper(mActivity, "WaitSendDataDatabase.db", null, 2);
        waitSendDataSQLiteOpenHelper.getWritableDatabase();//创建未发送成功的数据表
        disposableUserSQLiteOpenHelper = new DisposableUserSQLiteOpenHelper(mActivity, "DisposableUserDatabase.db", null, 2);
        disposableUserSQLiteOpenHelper.getWritableDatabase();//创建期限用户表
        bleSQLiteOpenHelper = new BleSQLiteOpenHelper(mActivity, "BleDatabase.db", null, 2);
        bleSQLiteOpenHelper.getWritableDatabase();//创建连接过的蓝牙表
        phoneUserSQLiteOpenHelper = new PhoneUserSQLiteOpenHelper(mActivity, "UserDatabase.db", null, 2);
        phoneUserSQLiteOpenHelper.getWritableDatabase();//创建用户表

        img_back = (ImageView) mActivity.findViewById(R.id.img_back);
        img_link_sounds = (ImageView) mActivity.findViewById(R.id.img_link_sounds);
        img_unlock_sounds = (ImageView) mActivity.findViewById(R.id.img_unlock_sounds);
        img_unlock_way = (ImageView) mActivity.findViewById(R.id.img_unlock_way);
        tv_equipment_name = (TextView) mActivity.findViewById(R.id.tv_equipment_name);
        tv_auto_unlock = (TextView) mActivity.findViewById(R.id.tv_auto_unlock);
        tv_unlock_distance = (TextView) mActivity.findViewById(R.id.tv_unlock_distance);
        tv_m = (TextView) mActivity.findViewById(R.id.tv_m);
        tv_light_screen = (TextView) mActivity.findViewById(R.id.tv_light_screen);
        lly_unlock_way = (LinearLayout) mActivity.findViewById(R.id.lly_unlock_way);
        lly_admin_power = (LinearLayout) mActivity.findViewById(R.id.lly_admin_power);
        rly_user_manage = (RelativeLayout) mActivity.findViewById(R.id.rly_user_manage);
        rly_password_manage = (RelativeLayout) mActivity.findViewById(R.id.rly_password_manage);
        rly_unlock_record = (RelativeLayout) mActivity.findViewById(R.id.rly_unlock_record);
        rly_about_equipment = (RelativeLayout) mActivity.findViewById(R.id.rly_about_equipment);
        rly_unlock_way = (RelativeLayout) mActivity.findViewById(R.id.rly_unlock_way);
        rly_auto_unlock = (RelativeLayout) mActivity.findViewById(R.id.rly_auto_unlock);
        rly_light_screen_unlock = (RelativeLayout) mActivity.findViewById(R.id.rly_light_screen_unlock);

        if (this.getIntent().getExtras() != null) {
            ble_name = this.getIntent().getExtras().getString("ble_name");
            mac = this.getIntent().getExtras().getString("mac");
            user_type = this.getIntent().getExtras().getString("user_type");
            type = this.getIntent().getExtras().getString("type");

            Log.e("url", "BleDetails_bleName==" + this.getIntent().getExtras().getString("ble_name") + "   mac==" + mac + "   user_type==" + user_type + "  type==" + type);
        }

        phoneUserData = getData2();
        Log.e("url", "phoneUserData.size==" + phoneUserData.size());
        for (int i = 0; i < phoneUserData.size(); i++) {
            Log.e("url", "手机用户==" + phoneUserData.get(i).get("user_num").toString());
        }

        //蓝牙名称
        if (!sharedPreferencesUtil.getBLENANE().toString().equals("")) {
            tv_equipment_name.setText(sharedPreferencesUtil.getBLENANE().toString());
        } else {
            tv_equipment_name.setText(ble_name);
        }

        String permission = user_type.substring(user_type.length() - 2, user_type.length() - 1);//2111
        Log.e("url", "permission(权限位)==" + permission);
        //管理员与普通用户界面不一样
        if (permission.equals("1")) {
            lly_admin_power.setVisibility(View.VISIBLE);
        } else if (permission.equals("2")) {
            lly_admin_power.setVisibility(View.GONE);
        }

        GetBleData(mac);//获取蓝牙数据库中的信息

        //判断连接提示音开关
        if (sharedPreferencesUtil.getConnectMusicSwitch().equals("on")) {
            isUnlock_sounds = true;
            img_link_sounds.setImageResource(R.mipmap.btn_on);
        } else {
            img_link_sounds.setImageResource(R.mipmap.btn_off);
        }

        //判断开锁提示音开关
        if (sharedPreferencesUtil.getUnlockMusicSwitch().equals("on")) {
            isLink_sounds = true;
            img_unlock_sounds.setImageResource(R.mipmap.btn_on);
        } else {
            img_unlock_sounds.setImageResource(R.mipmap.btn_off);
        }

        img_back.setOnClickListener(this);
        rly_user_manage.setOnClickListener(this);
        rly_password_manage.setOnClickListener(this);
        rly_unlock_record.setOnClickListener(this);
        rly_about_equipment.setOnClickListener(this);
        img_link_sounds.setOnClickListener(this);
        img_unlock_sounds.setOnClickListener(this);
        rly_unlock_way.setOnClickListener(this);
        rly_auto_unlock.setOnClickListener(this);
        rly_light_screen_unlock.setOnClickListener(this);
    }

    /**
     * 获取蓝牙数据库中的信息
     */
    private void GetBleData(String mac) {
        ble_db = bleSQLiteOpenHelper.getWritableDatabase();
        String phone = null;
        //游标查询每条数据
        Cursor cursor = ble_db.query("Ble_Table", null, "mac=?", new String[]{mac}, null, null, null);

        if (cursor.moveToNext()) {
            sharedPreferencesUtil.setUnlockmethod(cursor.getString(5));//开锁方式
            sharedPreferencesUtil.setAutoopendistance(cursor.getString(6));//自动开锁的距离
            sharedPreferencesUtil.setConnectMusic(cursor.getString(10));
            sharedPreferencesUtil.setUnlockMusic(cursor.getString(11));
            sharedPreferencesUtil.setConnectMusicSwitch(cursor.getString(12));
            sharedPreferencesUtil.setUnlockMusicSwitch(cursor.getString(13));
            sharedPreferencesUtil.setService(cursor.getString(14));
            sharedPreferencesUtil.setCharFfe1(cursor.getString(15));
            sharedPreferencesUtil.setCharFfe2(cursor.getString(16));
            sharedPreferencesUtil.setCharFfe3(cursor.getString(17));
            Log.e("url", "unlock_method==" + sharedPreferencesUtil.getUnlockmethod() +
                    "   auto_open_distance==" + sharedPreferencesUtil.getAutoopendistance() +
                    "  user_type==" + user_type + "  phone==" + phone +
                    "   connect_music==" + sharedPreferencesUtil.getConnectMusic() +
                    "   unlock_music==" + sharedPreferencesUtil.getUnlockMusic() +
                    "   connect_music_switch==" + sharedPreferencesUtil.getConnectMusicSwitch() +
                    "   unlock_music_switch==" + sharedPreferencesUtil.getUnlockMusicSwitch() +
                    "   service==" + sharedPreferencesUtil.getService() +
                    "   char_ffe1==" + sharedPreferencesUtil.getCharFfe1() +
                    "   char_ffe2==" + sharedPreferencesUtil.getCharFfe2() +
                    "   char_ffe3==" + sharedPreferencesUtil.getCharFfe3() +
                    "   is_invalid(是否失效)==" + cursor.getString(18));
        }

        cursor.close();
        ble_db.close();
    }

    @Override
    public void onClick(View v) {
        Intent intent;
        WindowManager.LayoutParams lp;
        switch (v.getId()) {
            case R.id.img_back://返回
//                intent = new Intent(mActivity, MainActivity.class);
//                intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);//关闭所有的activity，返回到首页
//                startActivity(intent);
                finish();
                break;
            case R.id.rly_user_manage://用户管理
                intent = new Intent(mActivity, UserManage.class);
                startActivity(intent);
                break;
            case R.id.rly_password_manage://密码管理
                intent = new Intent(mActivity, PassWordManage.class);
                startActivity(intent);
                break;
            case R.id.rly_unlock_record://开锁记录
                intent = new Intent(mActivity, UnlockRecord.class);
                startActivity(intent);
                break;
            case R.id.rly_about_equipment://关于设备
                intent = new Intent(mActivity, AboutEquipment.class);
                intent.putExtra("mac", mac);
                startActivity(intent);
                break;
            case R.id.img_link_sounds://连接提示音

                if (sharedPreferencesUtil.getConnectMusicSwitch().equals("on")) {
                    img_link_sounds.setImageResource(R.mipmap.btn_off);
                    sharedPreferencesUtil.setConnectMusicSwitch("off");
                    UpdateMusicBleData(mac, "connect_music", "off", "0");
                } else {

                    sharedPreferencesUtil.setConnectMusicSwitch("on");
                    initConnectSoundsPopupWindow("on");//初始化连接提示音

                    //设置背景颜色变暗
                    lp = getWindow().getAttributes();
                    lp.alpha = 0.5f;
                    getWindow().setAttributes(lp);

                    isLink_sounds = true;
                    img_link_sounds.setImageResource(R.mipmap.btn_on);
                    pop_connect_sounds.setAnimationStyle(R.style.PopupAnimation);
                    pop_connect_sounds.showAtLocation(mActivity.findViewById(R.id.lly_main), Gravity.CENTER | Gravity.CENTER_HORIZONTAL, 0, 0);
                    pop_connect_sounds.update();
                }

                break;
            case R.id.img_unlock_sounds://开锁提示音

                if (sharedPreferencesUtil.getUnlockMusicSwitch().equals("on")) {
                    isUnlock_sounds = false;
                    img_unlock_sounds.setImageResource(R.mipmap.btn_off);
                    sharedPreferencesUtil.setUnlockMusicSwitch("off");
                    UpdateMusicBleData(mac, "unlock_music", "off", "0");
                } else {
                    sharedPreferencesUtil.setUnlockMusicSwitch("on");
                    initUnlockSoundsPopupWindow("on");//开锁提示音
                    lp = getWindow().getAttributes();
                    lp.alpha = 0.5f;
                    getWindow().setAttributes(lp);
                    isUnlock_sounds = true;
                    img_unlock_sounds.setImageResource(R.mipmap.btn_on);

                    pop_unlock_sounds.setAnimationStyle(R.style.PopupAnimation);
                    pop_unlock_sounds.showAtLocation(mActivity.findViewById(R.id.lly_main), Gravity.CENTER | Gravity.CENTER_HORIZONTAL, 0, 0);
                    pop_unlock_sounds.update();
                }

                break;
            case R.id.rly_unlock_way://开锁方式
                if (isUnlock_way == true) {
                    isUnlock_way = false;
                    img_unlock_way.setImageResource(R.mipmap.btn_down);
                    lly_unlock_way.setVisibility(View.GONE);
                } else {
                    isUnlock_way = true;
                    img_unlock_way.setImageResource(R.mipmap.btn_up);
                    lly_unlock_way.setVisibility(View.VISIBLE);
                }
                break;
            case R.id.rly_auto_unlock://自动开锁
                sharedPreferencesUtil.setUnlockmethod("auto_unlock");
                initDistancePopupWindow();
                //设置背景颜色变暗
                lp = getWindow().getAttributes();
                lp.alpha = 0.5f;
                getWindow().setAttributes(lp);
                pop_response_distance.setAnimationStyle(R.style.PopupAnimation);
                pop_response_distance.showAtLocation(mActivity.findViewById(R.id.lly_main), Gravity.CENTER | Gravity.CENTER_HORIZONTAL, 0, 0);
                pop_response_distance.update();

                break;
            case R.id.rly_light_screen_unlock://点亮屏幕开锁
                sharedPreferencesUtil.setUnlockmethod("light_screen");
                tv_auto_unlock.setTextColor(getResources().getColor(R.color.A333333));
                tv_unlock_distance.setTextColor(getResources().getColor(R.color.A333333));
                tv_m.setTextColor(getResources().getColor(R.color.A333333));
                tv_light_screen.setTextColor(getResources().getColor(R.color.A04a8ec));

                if (sharedPreferencesUtil.getPhoneadd().equals("21")) {
                    sharedPreferencesUtil.setPhoneadd("22");
                    UpdateBleData(mac, "light_screen", "22");
                } else if (sharedPreferencesUtil.getPhoneadd().equals("11")) {
                    sharedPreferencesUtil.setPhoneadd("12");
                    UpdateBleData(mac, "light_screen", "12");
                }

                break;
        }
    }

    /**
     * 初始化连接提示音popWindow
     */
    private void initConnectSoundsPopupWindow(final String connect_music_switch) {

        LayoutInflater inflater = LayoutInflater.from(mActivity);
        View connect_sounds_layout = inflater.inflate(R.layout.pop_connect_sounds, null);
        lly_connect_no_music = (LinearLayout) connect_sounds_layout.findViewById(R.id.lly_connect_no_music);
        lly_connect_music1 = (LinearLayout) connect_sounds_layout.findViewById(R.id.lly_connect_music1);
        lly_connect_music2 = (LinearLayout) connect_sounds_layout.findViewById(R.id.lly_connect_music2);
        img_connect_music_icon = (ImageView) connect_sounds_layout.findViewById(R.id.img_connect_music_icon);
        img_connect_music_icon1 = (ImageView) connect_sounds_layout.findViewById(R.id.img_connect_music_icon1);
        img_connect_music_icon2 = (ImageView) connect_sounds_layout.findViewById(R.id.img_connect_music_icon2);
        tv_connect_music_name = (TextView) connect_sounds_layout.findViewById(R.id.tv_connect_music_name);
        tv_connect_music_name1 = (TextView) connect_sounds_layout.findViewById(R.id.tv_connect_music_name1);
        tv_connect_music_name2 = (TextView) connect_sounds_layout.findViewById(R.id.tv_connect_music_name2);
        tv_connect_ok = (TextView) connect_sounds_layout.findViewById(R.id.tv_connect_ok);
        connect_sounds_layout.invalidate();
        pop_connect_sounds = new PopupWindow(connect_sounds_layout, ViewGroup.LayoutParams.FILL_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        //实例化一个ColorDrawable颜色为半透明
        ColorDrawable dw = new ColorDrawable(0x00000000);
        //设置PopupWindow弹出窗体的背景
        pop_connect_sounds.setBackgroundDrawable(dw);
        pop_connect_sounds.setOutsideTouchable(true);
        pop_connect_sounds.setFocusable(true);
        pop_connect_sounds.setOnDismissListener(new PopupWindow.OnDismissListener() {
            @Override
            public void onDismiss() {
                WindowManager.LayoutParams lp = mActivity.getWindow().getAttributes();
                lp.alpha = 1;
                mActivity.getWindow().setAttributes(lp);
            }
        });

        UpdateMusicBleData(mac, "connect_music", connect_music_switch, coneect_music_type);


        //"无"点击事件
        lly_connect_no_music.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                img_connect_music_icon.setImageResource(R.mipmap.btn_music);
                tv_connect_music_name.setTextColor(getResources().getColor(R.color.A04a8ec));
                img_connect_music_icon1.setImageResource(R.mipmap.btn_no_music);
                tv_connect_music_name1.setTextColor(getResources().getColor(R.color.A333333));
                img_connect_music_icon2.setImageResource(R.mipmap.btn_no_music);
                tv_connect_music_name2.setTextColor(getResources().getColor(R.color.A333333));

                coneect_music_type = "0";

            }
        });


        //"铃声1"点击事件
        lly_connect_music1.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                img_connect_music_icon.setImageResource(R.mipmap.btn_no_music);
                tv_connect_music_name.setTextColor(getResources().getColor(R.color.A333333));
                img_connect_music_icon1.setImageResource(R.mipmap.btn_music);
                tv_connect_music_name1.setTextColor(getResources().getColor(R.color.A04a8ec));
                img_connect_music_icon2.setImageResource(R.mipmap.btn_no_music);
                tv_connect_music_name2.setTextColor(getResources().getColor(R.color.A333333));

                coneect_music_type = "1";

                music = MediaPlayer.create(mActivity, R.raw.connect_dingdong);
                music.start();
            }
        });


        //"铃声2"点击事件
        lly_connect_music2.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                img_connect_music_icon.setImageResource(R.mipmap.btn_no_music);
                tv_connect_music_name.setTextColor(getResources().getColor(R.color.A333333));
                img_connect_music_icon1.setImageResource(R.mipmap.btn_no_music);
                tv_connect_music_name1.setTextColor(getResources().getColor(R.color.A333333));
                img_connect_music_icon2.setImageResource(R.mipmap.btn_music);
                tv_connect_music_name2.setTextColor(getResources().getColor(R.color.A04a8ec));

                music = MediaPlayer.create(mActivity, R.raw.connect_connect);
                music.start();

                coneect_music_type = "2";
            }
        });

        //"确定"点击事件
        tv_connect_ok.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                sharedPreferencesUtil.setConnectMusic(coneect_music_type);
                UpdateMusicBleData(mac, "connect_music", connect_music_switch, coneect_music_type);
                pop_connect_sounds.dismiss();
            }
        });
    }

    /**
     * 初始化开锁提示音popWindow
     */
    private void initUnlockSoundsPopupWindow(final String unlock_music_switch) {

        LayoutInflater inflater = LayoutInflater.from(mActivity);
        View sounds_layout = inflater.inflate(R.layout.pop_unlock_sounds, null);
        lly_unlock_no_music = (LinearLayout) sounds_layout.findViewById(R.id.lly_unlock_no_music);
        lly_unlock_music1 = (LinearLayout) sounds_layout.findViewById(R.id.lly_unlock_music1);
        img_unlock_music_icon = (ImageView) sounds_layout.findViewById(R.id.img_unlock_music_icon);
        img_unlock_music_icon1 = (ImageView) sounds_layout.findViewById(R.id.img_unlock_music_icon1);
        tv_unlock_music_name = (TextView) sounds_layout.findViewById(R.id.tv_unlock_music_name);
        tv_unlock_music_name1 = (TextView) sounds_layout.findViewById(R.id.tv_unlock_music_name1);
        tv_unlock_ok = (TextView) sounds_layout.findViewById(R.id.tv_unlock_ok);
        sounds_layout.invalidate();
        pop_unlock_sounds = new PopupWindow(sounds_layout, ViewGroup.LayoutParams.FILL_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        //实例化一个ColorDrawable颜色为半透明
        ColorDrawable dw = new ColorDrawable(0x00000000);
        //设置PopupWindow弹出窗体的背景
        pop_unlock_sounds.setBackgroundDrawable(dw);
        pop_unlock_sounds.setOutsideTouchable(true);
        pop_unlock_sounds.setFocusable(true);
        pop_unlock_sounds.setOnDismissListener(new PopupWindow.OnDismissListener() {
            @Override
            public void onDismiss() {
                WindowManager.LayoutParams lp = mActivity.getWindow().getAttributes();
                lp.alpha = 1;
                mActivity.getWindow().setAttributes(lp);
            }
        });

        UpdateMusicBleData(mac, "unlock_music", unlock_music_switch, unlock_music_type);

        //"无"点击事件
        lly_unlock_no_music.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                img_unlock_music_icon.setImageResource(R.mipmap.btn_music);
                tv_unlock_music_name.setTextColor(getResources().getColor(R.color.A04a8ec));
                img_unlock_music_icon1.setImageResource(R.mipmap.btn_no_music);
                tv_unlock_music_name1.setTextColor(getResources().getColor(R.color.A333333));

                unlock_music_type = "0";
            }
        });


        //"铃声1"点击事件
        lly_unlock_music1.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

                img_unlock_music_icon.setImageResource(R.mipmap.btn_no_music);
                tv_unlock_music_name.setTextColor(getResources().getColor(R.color.A333333));
                img_unlock_music_icon1.setImageResource(R.mipmap.btn_music);
                tv_unlock_music_name1.setTextColor(getResources().getColor(R.color.A04a8ec));

                music = MediaPlayer.create(mActivity, R.raw.unlock_open_door);
                music.start();
                unlock_music_type = "1";
            }
        });

        //“确定”点击事件
        tv_unlock_ok.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                UpdateMusicBleData(mac, "unlock_music", unlock_music_switch, unlock_music_type);
                pop_unlock_sounds.dismiss();
            }
        });
    }

    //初始化设备感应距离popWindow
    private void initDistancePopupWindow() {
        LayoutInflater inflater = LayoutInflater.from(mActivity);
        View response_distance_layout = inflater.inflate(R.layout.pop_response_distance, null);
        tv_distance = (TextView) response_distance_layout.findViewById(R.id.tv_distance);
        tv_start_distance = (TextView) response_distance_layout.findViewById(R.id.tv_start_distance);
        tv_end_distance = (TextView) response_distance_layout.findViewById(R.id.tv_end_distance);
        tv_response_distance_ok = (TextView) response_distance_layout.findViewById(R.id.tv_response_distance_ok);
        sb_distance = (SeekBar) response_distance_layout.findViewById(R.id.sb_distance);

        response_distance_layout.invalidate();
        pop_response_distance = new PopupWindow(response_distance_layout, ViewGroup.LayoutParams.FILL_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        //实例化一个ColorDrawable颜色为半透明
        ColorDrawable dw = new ColorDrawable(0x00000000);
        //设置PopupWindow弹出窗体的背景
        pop_response_distance.setBackgroundDrawable(dw);
        pop_response_distance.setOutsideTouchable(true);
        pop_response_distance.setFocusable(true);
        pop_response_distance.setOnDismissListener(new PopupWindow.OnDismissListener() {
            @Override
            public void onDismiss() {
                WindowManager.LayoutParams lp = mActivity.getWindow().getAttributes();
                lp.alpha = 1;
                mActivity.getWindow().setAttributes(lp);
            }
        });

        current_distance = Integer.parseInt(tv_unlock_distance.getText().toString());//获取当前值
        tv_start_distance.setText(start_distance + "m");
        tv_end_distance.setText(end_distance + "m");
        sb_distance.setMax(end_distance);//拖动条最高值

        sb_distance.setProgress(current_distance);
        tv_distance.setText(current_distance + "m");

        sb_distance.setOnSeekBarChangeListener(this);

        tv_response_distance_ok.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                pop_response_distance.dismiss();

                sendSaveDistanceRSSI(current_distance);

                tv_unlock_distance.setText(current_distance + "");
                tv_auto_unlock.setTextColor(getResources().getColor(R.color.A04a8ec));
                tv_unlock_distance.setTextColor(getResources().getColor(R.color.A04a8ec));
                tv_m.setTextColor(getResources().getColor(R.color.A04a8ec));
                tv_light_screen.setTextColor(getResources().getColor(R.color.A333333));

                Log.e("url", "sharedPreferencesUtil.getPhoneadd()==" + sharedPreferencesUtil.getPhoneadd());

                if (BleService.ble_connect.equals("disconnect")) {
                    SaveWaitSendDataToTable("is_0A", wait_send_data, sharedPreferencesUtil.getCharFfe3(), sharedPreferencesUtil.getCharFfe2());//把未发送成功的数据存储到表中

                    if (sharedPreferencesUtil.getPhoneadd().equals("22")) {
                        sharedPreferencesUtil.setPhoneadd("21");
                        UpdateBleData(mac, "auto_unlock", "21");
                    } else if (sharedPreferencesUtil.getPhoneadd().equals("12")) {
                        sharedPreferencesUtil.setPhoneadd("11");
                        UpdateBleData(mac, "auto_unlock", "11");
                    }
                } else {
                    if (sharedPreferencesUtil.getPhoneadd().equals("22")) {
                        sharedPreferencesUtil.setPhoneadd("21");
                        UpdateBleData(mac, "auto_unlock", "21");
                    } else if (sharedPreferencesUtil.getPhoneadd().equals("12")) {
                        sharedPreferencesUtil.setPhoneadd("11");
                        UpdateBleData(mac, "auto_unlock", "11");
                    }

                }
            }
        });
    }


    @Override
    public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
        if (progress == 0) {
            progress = 1;
        }

        current_distance = progress;  //获取当前值

        if (current_distance > 3) {
//            seekBar.setcolor(getResources().getColor(R.color.A0fb818));
        }
        tv_distance.setText(current_distance + "m");

    }

    @Override
    public void onStartTrackingTouch(SeekBar seekBar) {

    }

    @Override
    public void onStopTrackingTouch(SeekBar seekBar) {

    }

    /**
     * 加载数据库的数据到List中(获取管理员密码)
     *
     * @return
     */
    private List<Map<String, Object>> getData() {
        disposable_user_db = disposableUserSQLiteOpenHelper.getWritableDatabase();
        //游标查询每条数据
        Cursor cursor = disposable_user_db.query("Disposable_User_Table", null, "mac=? and type='admin'", new String[]{mac}, null, null, null);

        //定义list存储数据
        List<Map<String, Object>> list = new ArrayList<Map<String, Object>>();

        //读取数据 游标移动到下一行
        while (cursor.moveToNext()) {
            Map<String, Object> map = new HashMap<String, Object>();
            map.put("id", cursor.getString(cursor.getColumnIndex("id")));
            map.put("mac", cursor.getString(cursor.getColumnIndex("mac")));
            map.put("password", cursor.getString(cursor.getColumnIndex("password")));
            map.put("type", cursor.getString(cursor.getColumnIndex("type")));
            map.put("user_id", cursor.getString(cursor.getColumnIndex("user_id")));
            list.add(map);
        }
        cursor.close();
        disposable_user_db.close();
        return list;
    }

    /**
     * 删除数据库数据
     *
     * @return
     */
    private List<Map<String, Object>> getData2() {
        phone_user_db = phoneUserSQLiteOpenHelper.getWritableDatabase();
        //游标查询每条数据
        Cursor cursor = phone_user_db.query("Phone_User_Table", null, "mac=? and user_num<>00000000000", new String[]{BLEDetails.mac}, null, null, null);

        //定义list存储数据
        List<Map<String, Object>> list = new ArrayList<Map<String, Object>>();

        //读取数据 游标移动到下一行
        while (cursor.moveToNext()) {
            String user_num = cursor.getString(4);

            if (!user_num.equals("00000000000")) {
                Map<String, Object> map = new HashMap<String, Object>();
                map.put("id", cursor.getString(cursor.getColumnIndex("id")));
                map.put("user_type", cursor.getString(cursor.getColumnIndex("user_type")));
                map.put("user_id", cursor.getString(cursor.getColumnIndex("user_id")));
                map.put("user_num", cursor.getString(cursor.getColumnIndex("user_num")));
                map.put("str_user", cursor.getString(cursor.getColumnIndex("str_user")));
                list.add(map);
            }
        }
        cursor.close();
        phone_user_db.close();
        return list;
    }

    /**
     * 检查期限用户中一天、一周、一个月用户是否过期,如果过期，password=0，start_time=0，end_time=0
     */
    private void QueryDisposableUserIsOutoftime() {
        //获取当前时间
        SimpleDateFormat format = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
        Date current_date = new Date();//取时间
        Calendar calendar = Calendar.getInstance();
        String current_time;//当前时间
        current_time = format.format(current_date);//当前时间
        Log.e("url", "current_time==" + current_time);

        disposable_user_db = disposableUserSQLiteOpenHelper.getWritableDatabase();
        ContentValues values = new ContentValues();

        //游标查询每条数据
        Cursor cursor = disposable_user_db.query("Disposable_User_Table", null, "(type='day' or type='week' or type='month') and mac=?  AND end_time<>'0' AND end_time<?", new String[]{mac, current_time}, null, null, null);

        while (cursor.moveToNext()) {
            int count = cursor.getInt(0);
            Log.e("url", "count==" + count);
            if (count > 0) {//数据存在
                values.put("password", "0");
                values.put("start_time", "0");
                values.put("end_time", "0");
                disposable_user_db.update("Disposable_User_Table", values, "mac=? and id=?", new String[]{mac, count + ""});
            }
        }
        cursor.close();
        disposable_user_db.close();
    }

    /**
     * 加载数据库的数据到List中（期限单次用户列表）
     *
     * @return
     */
    private List<Map<String, Object>> getDisposableUserData() {
        disposable_user_db = disposableUserSQLiteOpenHelper.getWritableDatabase();
        //游标查询每条数据
        Cursor cursor = disposable_user_db.query("Disposable_User_Table", null, "(type='once' or type='day' or type='week' or type='month') and mac=? ", new String[]{mac}, null, null, null);
        //定义list存储数据
        List<Map<String, Object>> list = new ArrayList<Map<String, Object>>();

        //读取数据 游标移动到下一行
        while (cursor.moveToNext()) {
            String password = cursor.getString(2);//password列
            if (!password.equals("bbbbbbbbbbbb")) {
                Map<String, Object> map = new HashMap<String, Object>();
                map.put("id", cursor.getString(cursor.getColumnIndex("id")));
                map.put("mac", cursor.getString(cursor.getColumnIndex("mac")));
                map.put("password", cursor.getString(cursor.getColumnIndex("password")));
                map.put("type", cursor.getString(cursor.getColumnIndex("type")));
                map.put("user_id", cursor.getString(cursor.getColumnIndex("user_id")));
                map.put("start_time", cursor.getString(cursor.getColumnIndex("start_time")));
                map.put("end_time", cursor.getString(cursor.getColumnIndex("end_time")));
                map.put("str_user", cursor.getString(cursor.getColumnIndex("str_user")));
                list.add(map);
            }
        }
        cursor.close();
        disposable_user_db.close();
        return list;
    }

    /**
     * 发送安全距离的rssi
     *
     * @param safe_distance
     */
    private void sendSaveDistanceRSSI(int safe_distance) {
        int rssi;
        Log.e("url", "safe_distance==" + safe_distance);
        rssi = (int) (Math.log10(safe_distance) * (10 * 2.0) + 80);

        Log.e("url", "safe_rssi==" + rssi);

        byte[] head = new byte[]{0x0A};//固定的前面只是0x0A
        String str_rssi = DigitalTrans.algorismToHEXString(rssi);
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

        wait_send_data = "0A" + str_rssi + "0000000000000000000000000000";
        Log.e("url", "wait_send_data==" + wait_send_data);
        Log.e("url", "wait_send_data.size==" + wait_send_data.length());

        //下发 0a rssi
        BleService.getInstance().writeCharacteristic(sharedPreferencesUtil.getService(), sharedPreferencesUtil.getCharFfe3(), encrypt_add_rssi_data);
        BleService.getInstance().setCharacteristicNotification(sharedPreferencesUtil.getService(), sharedPreferencesUtil.getCharFfe2(), true);

//        try {
//            Thread.sleep(1000);
//        } catch (InterruptedException e) {
//            e.printStackTrace();
//        }

        synchronized (this) {
            try {
                wait(1000);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }

    }

    /**
     * 把未发送成功的数据存储到表中
     */
    private void SaveWaitSendDataToTable(String is_0A, String wait_send_data, String write_char, String setnotice_char) {
        wait_send_data_db = waitSendDataSQLiteOpenHelper.getWritableDatabase();
        ContentValues values = new ContentValues();

//        //游标查询每条数据
//        Cursor cursor = wait_send_data_db.query("WaitSendData_Table", null, "mac=? and is_0A=?", new String[]{mac, is_0A}, null, null, null);
//
//        if (cursor.moveToNext()) {
//            int count = cursor.getInt(0);
//            if (count > 0) {//数据存在
//                Log.e("url","数据存在，进行更新");
//                values.put("wait_send_data", wait_send_data);
//                wait_send_data_db.update("WaitSendData_Table", values, "mac=? and is_0A=?", new String[]{mac, is_0A});
//            }
//        }else {
            values.put("mac", sharedPreferencesUtil.getMAC());
            values.put("data", wait_send_data);
            values.put("write_char", write_char);
            values.put("setnotice_char", setnotice_char);
            values.put("is_0A", "is_0A");
            wait_send_data_db.insert("WaitSendData_Table", null, values);
//        }
    }

    /**
     * 查询蓝牙数据库中的自动开锁方式方法
     *
     * @param mac
     * @return
     */
    private String SearchBleData(String mac) {
        String unlock_method = "auto_unlock";
        ble_db = bleSQLiteOpenHelper.getWritableDatabase();
        ContentValues values = new ContentValues();

        //游标查询每条数据
        Cursor cursor = ble_db.query("Ble_Table", null, "mac=?", new String[]{mac}, null, null, null);

        if (cursor.moveToNext()) {
            unlock_method = cursor.getString(5);
            current_distance = Integer.parseInt(cursor.getString(6));
            tv_unlock_distance.setText(current_distance + "");
            Log.e("url", "unlock_method==" + unlock_method + "  current_distance==" + current_distance);
        }
        cursor.close();
        ble_db.close();
        return unlock_method;
    }

    /**
     * 修改开锁方式后，更新数据库
     *
     * @param mac
     * @param unlock_method
     */
    private void UpdateBleData(String mac, String unlock_method, String admin_type) {
        Log.e("url", "unlock_method==" + unlock_method + "  current_distance==" + current_distance + "  admin_type==" + admin_type);

        ble_db = bleSQLiteOpenHelper.getWritableDatabase();
        ContentValues values = new ContentValues();

        //游标查询每条数据
        Cursor cursor = ble_db.query("Ble_Table", null, "mac=?", new String[]{mac}, null, null, null);

        if (cursor.moveToNext()) {
            int count = cursor.getInt(0);
            if (count > 0) {//数据存在
                if (unlock_method.equals("auto_unlock")) {
                    values.put("unlock_method", unlock_method);
                    values.put("unlock_distance", current_distance);
                    values.put("admin_type", admin_type);
                } else {
                    values.put("unlock_method", unlock_method);
                    values.put("admin_type", admin_type);
                }
                ble_db.update("Ble_Table", values, "mac=?", new String[]{mac});
            }
        }
        cursor.close();
        ble_db.close();
    }

    /**
     * 修改提示音后，更新数据库
     *
     * @param mac
     * @param type
     * @param music_switch
     * @param music
     */
    private void UpdateMusicBleData(String mac, String type, String music_switch, String music) {
        Log.e("url", "music==" + music + "   music_switch==" + music_switch);

        ble_db = bleSQLiteOpenHelper.getWritableDatabase();
        ContentValues values = new ContentValues();

        //游标查询每条数据
        Cursor cursor = ble_db.query("Ble_Table", null, "mac=?", new String[]{mac}, null, null, null);

        if (cursor.moveToNext()) {
            int count = cursor.getInt(0);
            if (count > 0) {//数据存在
                if (type.equals("connect_music")) {
                    Log.e("url", "connect_music更新");
                    values.put("connect_music", music);
                    values.put("connect_music_on_off", music_switch);
                    ble_db.update("Ble_Table", values, "mac=?", new String[]{mac});
                } else if (type.equals("unlock_music")) {
                    Log.e("url", "unlock_music更新");
                    values.put("unlock_music", music);
                    values.put("unlock_music_on_off", music_switch);
                    ble_db.update("Ble_Table", values, "mac=?", new String[]{mac});
                }
            }
        }
        cursor.close();
        ble_db.close();
    }

    @Override
    protected void onResume() {
        super.onResume();
        MainActivity.is_mainactivity = false;
        MainActivity.is_scan = true;//停止扫描

        if (!sharedPreferencesUtil.getBLENANE().toString().equals("")) {
            tv_equipment_name.setText(sharedPreferencesUtil.getBLENANE().toString());
        } else {
            tv_equipment_name.setText(ble_name);
        }

        QueryDisposableUserIsOutoftime();

        Log.e("url", "onResume_mac" + mac);

        String unlock_method = SearchBleData(mac);

        if (unlock_method.equals("auto_unlock")) {
            tv_auto_unlock.setTextColor(getResources().getColor(R.color.A04a8ec));
            tv_unlock_distance.setTextColor(getResources().getColor(R.color.A04a8ec));
            tv_m.setTextColor(getResources().getColor(R.color.A04a8ec));
            tv_light_screen.setTextColor(getResources().getColor(R.color.A333333));
        } else if (unlock_method.equals("light_screen")) {
            tv_auto_unlock.setTextColor(getResources().getColor(R.color.A333333));
            tv_unlock_distance.setTextColor(getResources().getColor(R.color.A333333));
            tv_m.setTextColor(getResources().getColor(R.color.A333333));
            tv_light_screen.setTextColor(getResources().getColor(R.color.A04a8ec));
        }

        adminUserData = getData();
        for (int i = 0; i < adminUserData.size(); i++) {
            Log.e("url", "id==" + adminUserData.get(i).get("id").toString() +
                    "  mac==" + adminUserData.get(i).get("mac").toString() +
                    "  password==" + adminUserData.get(i).get("password").toString() +
                    "  type==" + adminUserData.get(i).get("type").toString() +
                    "  user_id==" + adminUserData.get(i).get("user_id").toString());
        }

        disposableUserData = getDisposableUserData();
        for (int i = 0; i < disposableUserData.size(); i++) {
            Log.e("url", "id==" + disposableUserData.get(i).get("id").toString() +
                    "  password==" + disposableUserData.get(i).get("password").toString() +
                    "  type==" + disposableUserData.get(i).get("type").toString() +
                    "  user_id==" + disposableUserData.get(i).get("user_id").toString() +
                    "  start_time==" + disposableUserData.get(i).get("start_time").toString() +
                    "  end_time==" + disposableUserData.get(i).get("end_time").toString());
        }
    }
}

package com.yundiankj.ble_lock.Classes.Activity.UserManage;

import android.app.Activity;
import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothGattDescriptor;
import android.content.ContentValues;
import android.content.Intent;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.graphics.drawable.ColorDrawable;
import android.os.Bundle;
import android.util.Log;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.widget.ImageView;
import android.widget.PopupWindow;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.junkchen.blelib.BleListener;
import com.yundiankj.ble_lock.Resource.AesEntryDetry;
import com.yundiankj.ble_lock.Resource.BleService;
import com.yundiankj.ble_lock.Classes.Activity.BLEDetails;
import com.yundiankj.ble_lock.Classes.Activity.MainActivity;
import com.yundiankj.ble_lock.R;
import com.yundiankj.ble_lock.Resource.Const;
import com.yundiankj.ble_lock.Resource.DigitalTrans;
import com.yundiankj.ble_lock.Resource.SQLite.DisposableUserSQLiteOpenHelper;
import com.yundiankj.ble_lock.Resource.SQLite.WaitSendDataSQLiteOpenHelper;
import com.yundiankj.ble_lock.Resource.SharedPreferencesUtil;
import com.yundiankj.ble_lock.Resource.StatusBarUtil;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Created by hong on 2016/8/11.
 * <p>
 * (键盘用户中的) 管理员用户
 */
public class KeyBordAdminUser extends Activity {

    private Activity mActivity;
    private SharedPreferencesUtil sharedPreferencesUtil;
    private ImageView img_back;
    private TextView tv_admin_name,tv_pw;
    private RelativeLayout rly_admin_user;

    private DisposableUserSQLiteOpenHelper keyboardUserSQLiteOpenHelper; //键盘用户数据库
    private SQLiteDatabase db;
    private WaitSendDataSQLiteOpenHelper waitSendDataSQLiteOpenHelper;//未发送成功的数据表
    private SQLiteDatabase wait_send_data_db;

    private List<Map<String, Object>> adminUserData; //定义一个列表储存 管理员用户数据

    //长按删除popupwindow
    private PopupWindow pop_delete;
    private TextView tv_ok, tv_cancel;

    //返回首页popWindow
    private PopupWindow pop_back;
    private TextView tv_back_cancel, tv_back_ok;

    String wait_send_data;
    String admin_pw;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.keyboard_admin_user);
        mActivity = this;
        initWindow();//沉浸式状态栏
        init();
    }

    private void initWindow() {
        StatusBarUtil.setColor(mActivity, getResources().getColor(R.color.A04a8ec), 0);
    }

    private void init() {

        sharedPreferencesUtil = SharedPreferencesUtil.getInstance(mActivity);
        keyboardUserSQLiteOpenHelper = new DisposableUserSQLiteOpenHelper(mActivity, "DisposableUserDatabase.db", null, 2);
        keyboardUserSQLiteOpenHelper.getWritableDatabase();//创建键盘用户表
        waitSendDataSQLiteOpenHelper = new WaitSendDataSQLiteOpenHelper(mActivity, "WaitSendDataDatabase.db", null, 2);
        waitSendDataSQLiteOpenHelper.getWritableDatabase();//创建未发送成功的数据表

        img_back = (ImageView) mActivity.findViewById(R.id.img_back);
        tv_admin_name = (TextView) mActivity.findViewById(R.id.tv_admin_name);
        tv_pw= (TextView) mActivity.findViewById(R.id.tv_pw);
        rly_admin_user = (RelativeLayout) mActivity.findViewById(R.id.rly_admin_user);

        adminUserData = getData();
        Log.e("url", "adminUserData.size==" + adminUserData.size());
        Log.e("url", "pw==" + adminUserData.get(0).get("password").toString());
//        for (int i=0;i<adminUserData.size();i++){
//            Log.e("url", "pw==" + adminUserData.get(i).get("password").toString());
//            if (adminUserData.get(i).get("type").toString().equals("admin")){
//                admin_pw=adminUserData.get(i).get("password").toString();
//            }
//        }

        tv_pw.setText(adminUserData.get(0).get("password").toString());
        if (adminUserData.get(0).get("type").toString().equals("admin")) {
            tv_admin_name.setText("管理员用户");
        }

        //管理员用户长按点击事件
        rly_admin_user.setOnLongClickListener(new View.OnLongClickListener() {
            @Override
            public boolean onLongClick(View v) {
                initDeletePopWindow();
                WindowManager.LayoutParams lp;
                //设置背景颜色变暗
                lp = getWindow().getAttributes();
                lp.alpha = 0.5f;
                getWindow().setAttributes(lp);

                pop_delete.setAnimationStyle(R.style.PopupAnimation);
                pop_delete.showAtLocation(mActivity.findViewById(R.id.lly_main), Gravity.CENTER | Gravity.CENTER_HORIZONTAL, 0, 0);
                pop_delete.update();

                return false;
            }
        });

        //返回点击事件
        img_back.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                finish();
            }
        });

    }

    /**
     * 加载数据库的数据到List中
     *
     * @return
     */
    private List<Map<String, Object>> getData() {
        db = keyboardUserSQLiteOpenHelper.getWritableDatabase();
        //游标查询每条数据
        Cursor cursor = db.query("Disposable_User_Table", null, "mac=? and type=?", new String[]{BLEDetails.mac, "admin"}, null, null, null);

        //定义list存储数据
        List<Map<String, Object>> list = new ArrayList<Map<String, Object>>();
        //读取数据 游标移动到下一行
        while (cursor.moveToNext()) {
            String user_type = cursor.getString(3);//user_type列
            if (user_type.equals("admin")) {
                Map<String, Object> map = new HashMap<String, Object>();
                map.put("id", cursor.getString(cursor.getColumnIndex("id")));
                map.put("password", cursor.getString(cursor.getColumnIndex("password")));
                map.put("type", cursor.getString(cursor.getColumnIndex("type")));
                map.put("user_id", cursor.getString(cursor.getColumnIndex("user_id")));
                list.add(map);
            }
        }
        cursor.close();
        db.close();
        return list;
    }


    /**
     * 初始化长按删除popwindow
     */
    private void initDeletePopWindow() {
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

        tv_ok.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

                String password = adminUserData.get(0).get("password").toString();
                Log.e("url", "password==" + password);
                if (password.equals("123456")){
                    Toast.makeText(mActivity, "删除管理员成功，管理员密码初始化！", Toast.LENGTH_SHORT).show();
                }else {
                    String str_pw = DigitalTrans.str2HexStr(password);//字符串转换成十六进制字符串
                    byte[] byte_pw = DigitalTrans.hex2byte(str_pw);//十六进制串转化为byte数组

                    short[] short_pw = new short[byte_pw.length];
                    for (int i = 0; i < byte_pw.length; i++) {
                        short_pw[i] = byte_pw[i];
                    }
                    //校验crc
                    short short_crc = 0;
                    short_crc = BleService.getInstance().appData_Crc(short_pw, short_crc, short_pw.length);
                    String str_crc = Integer.toHexString(short_crc);

                    if (str_crc.length() == 1) {
                        str_crc = 0 + str_crc;
                    }

                    byte[] crc = DigitalTrans.hex2byte(str_crc);//十六进制串转化为byte数组
                    Log.e("url", "edit_admin_pw_CRC==" + str_crc);//(111111  28)  (123456 ec)
                    Log.e("url", "crc==" + crc[0]);//(111111  28)  (123456 ec)

                    byte[] head = new byte[]{0x02};
                    byte[] delete_pw_data = DigitalTrans.byteMerger(head, crc);//把头部和crc加在一起
                    byte[] add_delete_pw_data=DigitalTrans.byteMerger(delete_pw_data, Const.add14);//补全16位

                    Log.e("url", "add_delete_pw_data==" + add_delete_pw_data.length);
                    byte[] encrypt_add_delete_pw_data= null;
                    //加密
                    try {
                        encrypt_add_delete_pw_data= AesEntryDetry.encrypt(add_delete_pw_data);//加密
                    } catch (Exception e) {
                        e.printStackTrace();
                    }

                    wait_send_data = "02" + str_crc+"0000000000000000000000000000";
                    Log.e("url", "wait_send_data==" + wait_send_data);
                    Log.e("url", "wait_send_data.length==" + wait_send_data.length());

                    BleService.getInstance().writeCharacteristic(sharedPreferencesUtil.getService(), sharedPreferencesUtil.getCharFfe3(), encrypt_add_delete_pw_data);
                    BleService.getInstance().setCharacteristicNotification(sharedPreferencesUtil.getService(), sharedPreferencesUtil.getCharFfe2(), true);

                    if (BleService.ble_connect.equals("disconnect")) {
                        SaveWaitSendDataToTable(wait_send_data, sharedPreferencesUtil.getCharFfe3(), sharedPreferencesUtil.getCharFfe2());//把未发送成功的数据存储到表中
                        Toast.makeText(mActivity, "删除管理员成功，管理员密码初始化！", Toast.LENGTH_SHORT).show();
                        pop_delete.dismiss();
                        UpdateAdminUserTableData("123456");
//                    pop_delete.dismiss();
//                    initBackPopWindow();
//                    WindowManager.LayoutParams lp;
//                    //设置背景颜色变暗
//                    lp = getWindow().getAttributes();
//                    lp.alpha = 0.5f;
//                    getWindow().setAttributes(lp);
//
//                    pop_back.setAnimationStyle(R.style.PopupAnimation);
//                    pop_back.showAtLocation(mActivity.findViewById(R.id.lly_main), Gravity.CENTER | Gravity.CENTER_HORIZONTAL, 0, 0);
//                    pop_back.update();
                    } else {
                        final String finalStr_crc = str_crc;
                        BleService.getInstance().setOnDataAvailableListener(new BleListener.OnDataAvailableListener() {
                            @Override
                            public void onCharacteristicRead(BluetoothGatt gatt, BluetoothGattCharacteristic characteristic, int status) {

                            }

                            @Override
                            public void onCharacteristicChanged(BluetoothGatt gatt, BluetoothGattCharacteristic characteristic) {
                                //04ec 0e00ffa000000005000400122c00
//                                String back_data = DigitalTrans.bytesToHexString(characteristic.getValue());
                                String decrypt_back_data=null;
                                try {
                                    decrypt_back_data=DigitalTrans.bytesToHexString(AesEntryDetry.decrypt(characteristic.getValue()));//解密
                                } catch (Exception e) {
                                    e.printStackTrace();
                                }
                                Log.e("url", "返回值(解密后)==" + decrypt_back_data);
                                String delete_admin_back_data = decrypt_back_data.substring(decrypt_back_data.length() - 32, decrypt_back_data.length() - 28);//04ec
                                Log.e("url", "delete_admin_back_data==" + delete_admin_back_data);

                                String back = "04" + finalStr_crc;
                                if (delete_admin_back_data.equals(back)) {

                                    mActivity.runOnUiThread(new Runnable() {
                                        @Override
                                        public void run() {
                                            Toast.makeText(mActivity, "删除管理员成功，管理员密码初始化！", Toast.LENGTH_SHORT).show();
                                            pop_delete.dismiss();
                                            UpdateAdminUserTableData("123456");
                                        }
                                    });


                                } else if (delete_admin_back_data.equals("51ff")) {

                                    mActivity.runOnUiThread(new Runnable() {
                                        @Override
                                        public void run() {
                                            Toast.makeText(mActivity, "删除管理员失败，重新操作！", Toast.LENGTH_SHORT).show();
                                        }
                                    });

                                }
                            }

                            @Override
                            public void onDescriptorRead(BluetoothGatt gatt, BluetoothGattDescriptor descriptor, int status) {

                            }
                        });
                    }
                    MainActivity.is_scan=true;
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
     * 蓝牙断开，返回首页重新连接
     */
    private void initBackPopWindow() {
        LayoutInflater inflater = LayoutInflater.from(mActivity);
        View back_layout = inflater.inflate(R.layout.pop_back_main, null);
        tv_back_cancel = (TextView) back_layout.findViewById(R.id.tv_cancel);
        tv_back_ok = (TextView) back_layout.findViewById(R.id.tv_ok);
        back_layout.invalidate();
        pop_back = new PopupWindow(back_layout, ViewGroup.LayoutParams.FILL_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        //实例化一个ColorDrawable颜色为半透明
        ColorDrawable dw = new ColorDrawable(0x00000000);
        //设置PopupWindow弹出窗体的背景
        pop_back.setBackgroundDrawable(dw);
        pop_back.setOutsideTouchable(true);
        pop_back.setFocusable(true);
        pop_back.setOnDismissListener(new PopupWindow.OnDismissListener() {
            @Override
            public void onDismiss() {
                WindowManager.LayoutParams lp = mActivity.getWindow().getAttributes();
                lp.alpha = 1;
                mActivity.getWindow().setAttributes(lp);
            }
        });

        tv_back_cancel.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                pop_back.dismiss();
            }
        });

        tv_back_ok.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                pop_back.dismiss();
                Intent intent = new Intent(mActivity, MainActivity.class);
                intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
                startActivity(intent);
            }
        });
    }

    /**
     * 把未发送成功的数据存储到表中
     */
    private void SaveWaitSendDataToTable(String wait_send_data, String write_char, String setnotice_char) {
        wait_send_data_db = waitSendDataSQLiteOpenHelper.getWritableDatabase();
        ContentValues values = new ContentValues();

        values.put("mac", sharedPreferencesUtil.getMAC());
        values.put("data", wait_send_data);
        values.put("write_char", write_char);
        values.put("setnotice_char", setnotice_char);
        values.put("is_0A", "not_0A");
        wait_send_data_db.insert("WaitSendData_Table", null, values);
    }

    /**
     * 更新键盘用户表中管理员的密码
     *
     * @param pw
     */
    private void UpdateAdminUserTableData(String pw) {
        db = keyboardUserSQLiteOpenHelper.getWritableDatabase();
        ContentValues values = new ContentValues();

        //游标查询每条数据
        Cursor cursor = db.query("Disposable_User_Table", null, "mac=? and type=?", new String[]{BLEDetails.mac, "admin"}, null, null, null);

        if (cursor.moveToNext()) {
            int count = cursor.getInt(0);
            if (count > 0) {//数据存在
                values.put("password", pw);
                db.update("Disposable_User_Table", values, "mac=? and type=?", new String[]{BLEDetails.mac, "admin"});
            }
        }
        cursor.close();
        db.close();
    }

    @Override
    protected void onResume() {
        super.onResume();
        MainActivity.is_scan = true;//停止扫描
    }

}

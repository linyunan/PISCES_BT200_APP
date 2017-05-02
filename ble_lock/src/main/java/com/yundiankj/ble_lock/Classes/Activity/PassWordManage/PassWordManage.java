package com.yundiankj.ble_lock.Classes.Activity.PassWordManage;

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
import android.widget.EditText;
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
 * Created by hong on 2016/7/19.
 * <p>
 * 密码管理
 */
public class PassWordManage extends Activity implements View.OnClickListener {

    private Activity mActivity;
    private SharedPreferencesUtil sharedPreferencesUtil;
    private ImageView img_back;
    private TextView tv_edit;
    private RelativeLayout rly_normal_user_pw, rly_disposable_user_pw;

    private DisposableUserSQLiteOpenHelper keyboardUserSQLiteOpenHelper;   //键盘用户数据库
    private SQLiteDatabase db;
    private WaitSendDataSQLiteOpenHelper waitSendDataSQLiteOpenHelper;//未发送成功的数据表
    private SQLiteDatabase wait_send_data_db;

    private List<Map<String, Object>> adminUserData; //定义一个列表储存 管理员用户数据

    //修改管理员密码popWindow
    private PopupWindow pop_editAdminPassWord;
    private EditText et_old_pw, et_new_pw, et_confirm_pw;
    private TextView tv_save;

    //返回首页popWindow
    private PopupWindow pop_back;
    private TextView tv_back_cancel, tv_back_ok;

    String wait_send_data;//保存未发送数据的数组

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.password_manage);

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
        tv_edit = (TextView) mActivity.findViewById(R.id.tv_edit);
        rly_normal_user_pw = (RelativeLayout) mActivity.findViewById(R.id.rly_normal_user_pw);
        rly_disposable_user_pw = (RelativeLayout) mActivity.findViewById(R.id.rly_disposable_user_pw);

        adminUserData = getData();
        Log.e("url", "adminUserData.size==" + adminUserData.size());

        img_back.setOnClickListener(this);
        tv_edit.setOnClickListener(this);
        rly_normal_user_pw.setOnClickListener(this);
        rly_disposable_user_pw.setOnClickListener(this);
    }

    @Override
    public void onClick(View v) {
        Intent intent;
        switch (v.getId()) {
            case R.id.img_back://返回
                finish();
                break;
            case R.id.tv_edit://修改
                Log.e("url", "bleService.back_data==" + BleService.getInstance().back_data);

                initEditAdminPassWordPopWindow();
                WindowManager.LayoutParams lp;
                //设置背景颜色变暗
                lp = getWindow().getAttributes();
                lp.alpha = 0.5f;
                getWindow().setAttributes(lp);

                pop_editAdminPassWord.setAnimationStyle(R.style.PopupAnimation);
                pop_editAdminPassWord.showAtLocation(mActivity.findViewById(R.id.lly_main), Gravity.CENTER | Gravity.CENTER_HORIZONTAL, 0, 0);
                pop_editAdminPassWord.update();
                break;
            case R.id.rly_normal_user_pw://普通用户密码
                intent = new Intent(mActivity, OrdinaryUserPassWord.class);
                startActivity(intent);
                break;
            case R.id.rly_disposable_user_pw://期限用户密码
                intent = new Intent(mActivity, DisposableUserPassWord.class);
                startActivity(intent);
                break;
        }
    }

    /**
     * 初始化修改管理员密码的popwindow
     */
    private void initEditAdminPassWordPopWindow() {
        LayoutInflater inflater = LayoutInflater.from(mActivity);
        View edit_layout = inflater.inflate(R.layout.pop_edit_admin_pw, null);
        et_old_pw = (EditText) edit_layout.findViewById(R.id.et_old_pw);
        et_new_pw = (EditText) edit_layout.findViewById(R.id.et_new_pw);
        et_confirm_pw = (EditText) edit_layout.findViewById(R.id.et_confirm_pw);
        tv_save = (TextView) edit_layout.findViewById(R.id.tv_save);
        edit_layout.invalidate();
        pop_editAdminPassWord = new PopupWindow(edit_layout, ViewGroup.LayoutParams.FILL_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        //实例化一个ColorDrawable颜色为半透明
        ColorDrawable dw = new ColorDrawable(0x00000000);
        //设置PopupWindow弹出窗体的背景
        pop_editAdminPassWord.setBackgroundDrawable(dw);
        pop_editAdminPassWord.setOutsideTouchable(true);
        pop_editAdminPassWord.setFocusable(true);
        pop_editAdminPassWord.setOnDismissListener(new PopupWindow.OnDismissListener() {
            @Override
            public void onDismiss() {
                WindowManager.LayoutParams lp = mActivity.getWindow().getAttributes();
                lp.alpha = 1;
                mActivity.getWindow().setAttributes(lp);
            }
        });

        tv_save.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {//保存

                Log.e("url", "  et_old_pw.getText().length()==" + et_old_pw.getText().length());

                Log.e("url", "  数据库中的管理员密码==" + adminUserData.get(0).get("password").toString());
                if (et_old_pw.getText().toString().equals("")) {
                    Toast.makeText(mActivity, "旧密码不能为空！", Toast.LENGTH_SHORT).show();
                } else if (!et_old_pw.getText().toString().equals(adminUserData.get(0).get("password").toString())) {
                    Toast.makeText(mActivity, "旧密码输入不正确，请重新输入！", Toast.LENGTH_SHORT).show();
                } else if (et_new_pw.getText().toString().equals("")) {
                    Toast.makeText(mActivity, "新密码不能为空！", Toast.LENGTH_SHORT).show();
                } else if (et_confirm_pw.getText().toString().equals("")) {
                    Toast.makeText(mActivity, "确认密码不能为空！", Toast.LENGTH_SHORT).show();
                } else if (!et_new_pw.getText().toString().equals(et_confirm_pw.getText().toString())) {
                    Toast.makeText(mActivity, "新密码与确认密码不一致，请重新输入！", Toast.LENGTH_SHORT).show();
                } else {

                    byte[] head = new byte[]{0x05};//固定的前面只是0x05
                    String str_pw = DigitalTrans.str2HexStr(et_new_pw.getText().toString());//字符串转换成十六进制字符串
                    byte[] byte_pw = DigitalTrans.hex2byte(str_pw);//十六进制串转化为byte数组

                    short[] short_pw = new short[byte_pw.length];
                    for (int i = 0; i < byte_pw.length; i++) {
                        short_pw[i] = byte_pw[i];
                    }
                    //校验crc
                    short short_crc = 0;
                    short_crc = BleService.getInstance().appData_Crc(short_pw, short_crc, short_pw.length);
                    String str_crc = Integer.toHexString(short_crc);

                    if (str_crc.length() == 1) {//如果长度为1，那么DigitalTrans.hex2byte(str_crc)的时候会报错
                        str_crc = 0 + str_crc;
                    }

                    byte[] crc = DigitalTrans.hex2byte(str_crc);//十六进制串转化为byte数组
                    Log.e("url", "edit_admin_pw_CRC==" + str_crc);//(111111  28)  (123456 ec)
                    Log.e("url", "crc==" + crc[0]);//(111111  28)  (123456 ec)

                    byte[] data1 = DigitalTrans.byteMerger(head, byte_pw);//把头部和密码加在一起
                    byte[] data = DigitalTrans.byteMerger(data1, crc);//把头部和密码和crc加在一起
                    byte[] add_data=DigitalTrans.byteMerger(data, Const.add8);//补全16位

                    Log.e("url", "add_data==" + add_data.length);
                    byte[] encrypt_add_data= null;
                    //加密
                    try {
                        encrypt_add_data= AesEntryDetry.encrypt(add_data);//加密
                    } catch (Exception e) {
                        e.printStackTrace();
                    }

                    wait_send_data = "05" + str_pw + str_crc+"0000000000000000";
                    Log.e("url", "wait_send_data==" + wait_send_data);
                    Log.e("url", "wait_send_data.length==" + wait_send_data.length());

                    BleService.getInstance().writeCharacteristic(sharedPreferencesUtil.getService(), sharedPreferencesUtil.getCharFfe3(), encrypt_add_data);
                    BleService.getInstance().setCharacteristicNotification(sharedPreferencesUtil.getService(), sharedPreferencesUtil.getCharFfe2(), true);
//                    pop_editAdminPassWord.dismiss();

                    if (BleService.ble_connect.equals("disconnect")) {
                        SaveWaitSendDataToTable(wait_send_data, sharedPreferencesUtil.getCharFfe3(), sharedPreferencesUtil.getCharFfe2());//把未发送成功的数据存储到表中
                        Toast.makeText(mActivity, "修改管理员密码成功！", Toast.LENGTH_SHORT).show();
                        pop_editAdminPassWord.dismiss();
                        UpdateAdminUserTableData(et_new_pw.getText().toString());

//                        pop_editAdminPassWord.dismiss();
//                        initBackPopWindow();
//                        WindowManager.LayoutParams lp;
//                        //设置背景颜色变暗
//                        lp = getWindow().getAttributes();
//                        lp.alpha = 0.5f;
//                        getWindow().setAttributes(lp);
//
//                        pop_back.setAnimationStyle(R.style.PopupAnimation);
//                        pop_back.showAtLocation(mActivity.findViewById(R.id.lly_main), Gravity.CENTER | Gravity.CENTER_HORIZONTAL, 0, 0);
//                        pop_back.update();
                    } else {

                        BleService.getInstance().setOnDataAvailableListener(new BleListener.OnDataAvailableListener() {
                            @Override
                            public void onCharacteristicRead(BluetoothGatt gatt, BluetoothGattCharacteristic characteristic, int status) {

                            }

                            @Override
                            public void onCharacteristicChanged(BluetoothGatt gatt, BluetoothGattCharacteristic characteristic) {
                                //                                String back_data = DigitalTrans.bytesToHexString(characteristic.getValue());//074b0a00ffa000000004004c140b0004
                                String decrypt_back_data=null;
                                try {
                                    decrypt_back_data=DigitalTrans.bytesToHexString(AesEntryDetry.decrypt(characteristic.getValue()));//解密
                                } catch (Exception e) {
                                    e.printStackTrace();
                                }
                                Log.e("url", "返回值(解密后)==" + decrypt_back_data);
                                String edit_admin_pw_back_data = decrypt_back_data.substring(decrypt_back_data.length() - 32, decrypt_back_data.length() - 28);//074b

                                if (edit_admin_pw_back_data.equals("074b")) {
                                    mActivity.runOnUiThread(new Runnable() {
                                        @Override
                                        public void run() {
                                            Toast.makeText(mActivity, "修改管理员密码成功！", Toast.LENGTH_SHORT).show();
                                            pop_editAdminPassWord.dismiss();
                                        }
                                    });

                                    UpdateAdminUserTableData(et_new_pw.getText().toString());

                                } else if (edit_admin_pw_back_data.equals("54ff")) {

                                    mActivity.runOnUiThread(new Runnable() {
                                        @Override
                                        public void run() {
                                            Toast.makeText(mActivity, "修改管理员密码失败，重新设置！", Toast.LENGTH_SHORT).show();
                                        }
                                    });
                                }
                            }

                            @Override
                            public void onDescriptorRead(BluetoothGatt gatt, BluetoothGattDescriptor descriptor, int status) {

                            }
                        });
                    }
                }
                MainActivity.is_scan=true;
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
                intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);//关闭所有的activity，返回到首页
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
     * 加载数据库的数据到List中(获取管理员密码)
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
            Log.e("url", "user_type==" + user_type);
            if (user_type.equals("admin")) {
                Map<String, Object> map = new HashMap<String, Object>();
                map.put("id", cursor.getString(cursor.getColumnIndex("id")));
                map.put("mac", cursor.getString(cursor.getColumnIndex("mac")));
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
//            Log.e("url", "count==" + count);
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

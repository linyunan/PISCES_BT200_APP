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
import android.widget.BaseAdapter;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.PopupWindow;
import android.widget.TextView;
import android.widget.Toast;

import com.junkchen.blelib.BleListener;
import com.yundiankj.ble_lock.Classes.Activity.BLEDetails;
import com.yundiankj.ble_lock.Classes.Activity.MainActivity;
import com.yundiankj.ble_lock.R;
import com.yundiankj.ble_lock.Resource.AesEntryDetry;
import com.yundiankj.ble_lock.Resource.BleService;
import com.yundiankj.ble_lock.Resource.Const;
import com.yundiankj.ble_lock.Resource.DigitalTrans;
import com.yundiankj.ble_lock.Resource.SQLite.PhoneUserSQLiteOpenHelper;
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
 * 手机用户管理
 */
public class PhoneUserManage extends Activity {
    private Activity mActivity;
    private SharedPreferencesUtil sharedPreferencesUtil;
    private ImageView img_back;
    private ListView lv_phone_user_manage;
    private PhoneUserManageAdapter phoneUserManageAdapter;

    private PhoneUserSQLiteOpenHelper phoneUserSQLiteOpenHelper; //手机用户数据库
    private SQLiteDatabase db;
    private WaitSendDataSQLiteOpenHelper waitSendDataSQLiteOpenHelper;//未发送成功的数据数据库
    private SQLiteDatabase wait_send_data_db;

    private List<Map<String, Object>> phoneUserData;  //定义一个列表储存 手机用户数据

    //删除popwindow
    private PopupWindow pop_delete;
    private TextView tv_ok, tv_cancel;

    //返回首页popWindow
    private PopupWindow pop_back;
    private TextView tv_back_cancel, tv_back_ok;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.phone_user_manage);

        mActivity = this;
        initWindow();//沉浸式状态栏
        init();
    }

    private void initWindow() {
        StatusBarUtil.setColor(mActivity, getResources().getColor(R.color.A04a8ec), 0);
    }

    private void init() {
        sharedPreferencesUtil = SharedPreferencesUtil.getInstance(mActivity);

        phoneUserSQLiteOpenHelper = new PhoneUserSQLiteOpenHelper(mActivity, "UserDatabase.db", null, 2);
        phoneUserSQLiteOpenHelper.getWritableDatabase();//创建用户表
        waitSendDataSQLiteOpenHelper = new WaitSendDataSQLiteOpenHelper(mActivity, "WaitSendDataDatabase.db", null, 2);
        waitSendDataSQLiteOpenHelper.getWritableDatabase();//创建未发送成功的数据表

        img_back = (ImageView) mActivity.findViewById(R.id.img_back);
        lv_phone_user_manage = (ListView) mActivity.findViewById(R.id.lv_phone_user_manage);


        phoneUserData = getData();
        Log.e("url", "phoneUserData.size==" + phoneUserData.size());

        phoneUserManageAdapter = new PhoneUserManageAdapter();
        lv_phone_user_manage.setAdapter(phoneUserManageAdapter);

        img_back.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                finish();
            }
        });
    }

    //初始化长按删除popwindow
    private void initDeletePopWindow(final int position, final String user_id, final String user_num) {
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

                Log.e("url", "序号==" + Integer.parseInt(user_id));
//                String str_delete_user = BleService.getInstance().user_info.get(Integer.parseInt(user_id) - 1).toString();
                Log.e("url", "选中要删除的值==" + phoneUserData.get(position).get("str_user").toString());

                byte[] delete_phone = DigitalTrans.hex2byte(phoneUserData.get(position).get("str_user").toString());//十六进制串转化为byte数组
                short[] short_delete_phone = new short[delete_phone.length];
                for (int i = 0; i < delete_phone.length; i++) {
                    short_delete_phone[i] = delete_phone[i];
//                    Log.e("url", "short_delete_phone==" + short_delete_phone[i]);
                }
                short crc1 = 0;
                crc1 = BleService.getInstance().appData_Crc(short_delete_phone, crc1, short_delete_phone.length);
                String str_crc = Integer.toHexString(crc1);

                if (str_crc.length() == 1) {//如果长度为1，那么DigitalTrans.hex2byte(str_crc)的时候会报错
                    str_crc = 0 + str_crc;
                }

                Log.e("url", "删除手机用户_CRC==" + str_crc);
                byte[] crc = DigitalTrans.hex2byte(str_crc);//十六进制串转化为byte数组

                byte[] head = new byte[]{0x01};
                byte[] serial_num = DigitalTrans.hex2byte(user_id);//十六进制串转化为byte数组
                byte[] head_serial_num = DigitalTrans.byteMerger(head, serial_num);//把头部和序号加在一起
                byte[] delete_phone_data = DigitalTrans.byteMerger(head_serial_num, crc);//把头部和序号加在一起
                byte[] add_delete_phone_data=DigitalTrans.byteMerger(delete_phone_data, Const.add13);//补全16位

                Log.e("url", "add_delete_phone_data==" + add_delete_phone_data.length);
                byte[] encrypt_add_delete_phone_data= null;
                //加密
                try {
                    encrypt_add_delete_phone_data= AesEntryDetry.encrypt(add_delete_phone_data);//加密
                } catch (Exception e) {
                    e.printStackTrace();
                }

                String wait_send_data = "01" + user_id + str_crc+"00000000000000000000000000";
                Log.e("url", "wait_send_data==" + wait_send_data);
                Log.e("url", "wait_send_data.length==" + wait_send_data.length());

                BleService.getInstance().writeCharacteristic(sharedPreferencesUtil.getService(), sharedPreferencesUtil.getCharFfe3(), encrypt_add_delete_phone_data);
                BleService.getInstance().setCharacteristicNotification(sharedPreferencesUtil.getService(), sharedPreferencesUtil.getCharFfe2(), true);

                if (BleService.ble_connect.equals("disconnect")) {
                    SaveWaitSendDataToTable(wait_send_data, sharedPreferencesUtil.getCharFfe3(), sharedPreferencesUtil.getCharFfe2());//把未发送成功的数据存储到表中
                    Toast.makeText(mActivity, "删除成功！", Toast.LENGTH_SHORT).show();
                    pop_delete.dismiss();
                    DeleteTableData(user_id);
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
                            //03022e00ffa000000006000400122c00
//                            String back_data = DigitalTrans.bytesToHexString(characteristic.getValue());
                            String decrypt_back_data=null;
                            try {
                                decrypt_back_data=DigitalTrans.bytesToHexString(AesEntryDetry.decrypt(characteristic.getValue()));//解密
                            } catch (Exception e) {
                                e.printStackTrace();
                            }
                            Log.e("url", "返回值(解密后)==" + decrypt_back_data);
                            String delete_phone_user_back_data = decrypt_back_data.substring(decrypt_back_data.length() - 32, decrypt_back_data.length() - 26);//03022e
                            Log.e("url", "delete_phone_user_back_data==" + delete_phone_user_back_data);

                            String success_data = "03" + user_id + finalStr_crc;
                            Log.e("url", "success_data==" + success_data);
                            if (delete_phone_user_back_data.equals(success_data)) {

                                mActivity.runOnUiThread(new Runnable() {
                                    @Override
                                    public void run() {
                                        Log.e("url", "删除成功！");
                                        Toast.makeText(mActivity, "删除成功！", Toast.LENGTH_SHORT).show();
                                        pop_delete.dismiss();
                                        DeleteTableData(user_id);
                                    }
                                });

                            } else {
                                mActivity.runOnUiThread(new Runnable() {
                                    @Override
                                    public void run() {
                                        Toast.makeText(mActivity, "删除失败！请重新操作！", Toast.LENGTH_SHORT).show();
                                    }
                                });

                            }
                        }

                        @Override
                        public void onDescriptorRead(BluetoothGatt gatt, BluetoothGattDescriptor descriptor, int status) {

                        }
                    });
                }

                MainActivity.is_scan = true;//停止扫描
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
     * 更新手机用户列表
     *
     * @param user_type
     * @param user_id
     * @param user_num
     * @param str_user
     */
    private void UpdatePhoneUserTableData(String user_type, String user_id, String user_num, String str_user) {
        db = phoneUserSQLiteOpenHelper.getWritableDatabase();
        ContentValues values = new ContentValues();

        //游标查询每条数据
        Cursor cursor = db.query("Phone_User_Table", null, "mac=? and user_id=?", new String[]{BLEDetails.mac, user_id}, null, null, null);

        if (cursor.moveToNext()) {
            int count = cursor.getInt(0);
            if (count > 0) {//数据存在
                Log.e("url", "用户存在，进行更新！");
                values.put("user_type", user_type);
                values.put("user_num", user_num);
                values.put("str_user", str_user);
                db.update("Phone_User_Table", values, "mac=? and user_id=?", new String[]{BLEDetails.mac, user_id});
            }
        } else {
            values.put("mac", BLEDetails.mac);
            values.put("user_type", user_type);
            values.put("user_id", user_id);
            values.put("user_num", user_num);
            values.put("str_user", str_user);
            db.insert("Phone_User_Table", null, values);
        }
        cursor.close();
        db.close();
    }

    /**
     * 删除数据库数据
     *
     * @param user_id
     */
    private void DeleteTableData(String user_id) {
        db = phoneUserSQLiteOpenHelper.getWritableDatabase();
        ContentValues values = new ContentValues();
        values.put("user_num", "00000000000");
//        db.update("Phone_User_Table", values, new String("id IN (SELECT id FROM Phone_User_Table WHERE user_id= " + user_id+")"), null);
        db.update("Phone_User_Table", values, "mac=? and user_id=?", new String[]{BLEDetails.mac, user_id});

        phoneUserData = getData();
        phoneUserManageAdapter.notifyDataSetChanged();

        Log.e("url", "p删除后的honeUserData.size()==" + phoneUserData.size());
        for (int i = 0; i < phoneUserData.size(); i++) {
            Log.e("url", "删除后的phoneUserData==" + phoneUserData.get(i).get("user_num"));
        }
        db.close();
    }

    /**
     * 删除数据库数据
     *
     * @return
     */
    private List<Map<String, Object>> getData() {
        db = phoneUserSQLiteOpenHelper.getWritableDatabase();
        //游标查询每条数据
        Cursor cursor = db.query("Phone_User_Table", null, "mac=? and user_num<>00000000000", new String[]{BLEDetails.mac}, null, null, null);

        //定义list存储数据
        List<Map<String, Object>> list = new ArrayList<Map<String, Object>>();

        //读取数据 游标移动到下一行
        while (cursor.moveToNext()) {
            String user_num = cursor.getString(4);
            Log.e("url", "user_num==" + user_num);

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
        db.close();
        return list;
    }

    private class PhoneUserManageAdapter extends BaseAdapter {

        @Override
        public int getCount() {
            return phoneUserData.size();
        }

        @Override
        public Map<String, Object> getItem(int position) {
            return phoneUserData.get(position);
        }

        @Override
        public long getItemId(int position) {
            return position;
        }

        @Override
        public View getView(final int position, View convertView, ViewGroup parent) {
            ViewHolder vh = null;
            if (convertView == null) {
                convertView = LayoutInflater.from(mActivity).inflate(R.layout.phone_user_manage_listitem, null);
                vh = new ViewHolder();
                vh.tv_phone_name = (TextView) convertView.findViewById(R.id.tv_phone_name);
                vh.tv_phone_account = (TextView) convertView.findViewById(R.id.tv_phone_account);
            } else {
                vh = (ViewHolder) convertView.getTag();
            }
            convertView.setTag(vh);
            convertView.setClickable(true);

            vh.tv_phone_name.setText("手机用户" + Integer.parseInt(getItem(position).get("user_id").toString()));
            vh.tv_phone_account.setText(getItem(position).get("user_num").toString());

//            //将手机号中间隐藏为星号（*）
//            if (!TextUtils.isEmpty(vh.tv_phone_account.getText().toString()) && vh.tv_phone_account.getText().toString().length() > 6) {
//                StringBuilder sb = new StringBuilder();
//                for (int i = 0; i < vh.tv_phone_account.getText().toString().length(); i++) {
//                    char c = vh.tv_phone_account.getText().toString().charAt(i);
//                    if (i >= 3 && i <= vh.tv_phone_account.getText().toString().length() - 3) {
//                        sb.append('*');
//                    } else {
//                        sb.append(c);
//                    }
//                }
//                vh.tv_phone_account.setText(sb.toString());
//            }

            convertView.setOnLongClickListener(new View.OnLongClickListener() {
                @Override
                public boolean onLongClick(View v) {

                    Log.e("url", "position==" + position);
                    Log.e("url", "删除手机用户序号==" + getItem(position).get("user_id").toString());

                    initDeletePopWindow(position, getItem(position).get("user_id").toString(), getItem(position).get("user_num").toString());
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

            return convertView;
        }

        private class ViewHolder {
            private TextView tv_phone_name, tv_phone_account;
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        MainActivity.is_scan = true;//停止扫描
    }
}

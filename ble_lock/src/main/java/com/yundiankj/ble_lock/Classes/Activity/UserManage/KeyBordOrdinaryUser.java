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
import com.yundiankj.ble_lock.Resource.SQLite.DisposableUserSQLiteOpenHelper;
import com.yundiankj.ble_lock.Resource.SQLite.WaitSendDataSQLiteOpenHelper;
import com.yundiankj.ble_lock.Resource.SharedPreferencesUtil;
import com.yundiankj.ble_lock.Resource.StatusBarUtil;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Created by hong on 2016/8/12.
 * <p>
 * (键盘用户中的) 普通用户
 */
public class KeyBordOrdinaryUser extends Activity {
    private Activity mActivity;
    private SharedPreferencesUtil sharedPreferencesUtil;
    private ImageView img_back;
    private TextView tv_title;
    private ListView lv_ordinaryUser;
    private OrdinaryUserAdapter ordinaryUserAdapter;

    private DisposableUserSQLiteOpenHelper keyboardUserSQLiteOpenHelper;//键盘用户数据库
    private SQLiteDatabase db;
    private WaitSendDataSQLiteOpenHelper waitSendDataSQLiteOpenHelper;//未发送成功的数据表
    private SQLiteDatabase wait_send_data_db;

    private List<Map<String, Object>> ordinaryUserData;//定义一个列表储存 普通用户数据

    //长按删除popWindow
    private PopupWindow pop_delete;
    private TextView tv_ok, tv_cancel;

    //返回首页popWindow
    private PopupWindow pop_back;
    private TextView tv_back_cancel, tv_back_ok;

    String wait_send_data;

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
        keyboardUserSQLiteOpenHelper = new DisposableUserSQLiteOpenHelper(mActivity, "DisposableUserDatabase.db", null, 2);
        keyboardUserSQLiteOpenHelper.getWritableDatabase();//创建键盘用户表
        waitSendDataSQLiteOpenHelper = new WaitSendDataSQLiteOpenHelper(mActivity, "WaitSendDataDatabase.db", null, 2);
        waitSendDataSQLiteOpenHelper.getWritableDatabase();//创建未发送成功的数据表

        img_back = (ImageView) mActivity.findViewById(R.id.img_back);
        tv_title = (TextView) mActivity.findViewById(R.id.tv_title);
        lv_ordinaryUser = (ListView) mActivity.findViewById(R.id.lv_phone_user_manage);

        tv_title.setText("普通用户");
        ordinaryUserData = getData();
        Log.e("url", "adminUserData.size==" + ordinaryUserData.size());

        for (int i = 0; i < ordinaryUserData.size(); i++) {
            Log.e("url", "type==" + ordinaryUserData.get(i).get("type").toString()
                    + "    user_id==" + ordinaryUserData.get(i).get("user_id").toString()
                    + "    password==" + ordinaryUserData.get(i).get("password").toString());
        }

        ordinaryUserAdapter = new OrdinaryUserAdapter();
        lv_ordinaryUser.setAdapter(ordinaryUserAdapter);

        img_back.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                finish();
            }
        });
    }

    //初始化长按删除popwindow
    private void initDeletePopWindow(final int position, final String user_id, final String pw) {
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

                byte[] head = new byte[]{0x03};//固定的前面只是0x03,

                byte[] serial_num = DigitalTrans.hex2byte(user_id);//十六进制串转化为byte数组

                Log.e("url", "serial_num==" + serial_num[0]);

                String str_pw = DigitalTrans.str2HexStr(pw);//字符串转换成十六进制字符串
                byte[] byte_pw = DigitalTrans.hex2byte(str_pw);//十六进制串转化为byte数组(序号)
                byte[] serial_num_pw = DigitalTrans.byteMerger(serial_num, byte_pw);//把次数和序号，密码加在一起

                short[] short_delete_ordinary_user = new short[serial_num_pw.length];
                for (int i = 0; i < serial_num_pw.length; i++) {
                    short_delete_ordinary_user[i] = serial_num_pw[i];
                    Log.e("url", "short_delete_phone==" + short_delete_ordinary_user[i]);
                }
                short crc1 = 0;
                crc1 = BleService.getInstance().appData_Crc(short_delete_ordinary_user, crc1, short_delete_ordinary_user.length);
                String str_crc = Integer.toHexString(crc1);

                if (str_crc.length() == 1) {//如果长度为1，那么DigitalTrans.hex2byte(str_crc)的时候会报错
                    str_crc = 0 + str_crc;
                }

                Log.e("url", "删除普通用户_str_CRC==" + str_crc);
                byte[] crc = DigitalTrans.hex2byte(str_crc);//十六进制串转化为byte数组
                Log.e("url", "删除普通用户_byte_CRC==" + crc[0]);

                byte[] head_serial_num = DigitalTrans.byteMerger(head, serial_num);//把头部和序号加在一起
                byte[] delete_ordinary_user_data = DigitalTrans.byteMerger(head_serial_num, crc);//把头部和序号、crc加在一起
                byte[] add_delete_ordinary_user_data=DigitalTrans.byteMerger(delete_ordinary_user_data, Const.add13);//补全16位

                Log.e("url", "add_delete_ordinary_user_data==" + add_delete_ordinary_user_data.length);
                byte[] encrypt_add_delete_ordinary_user_data= null;
                //加密
                try {
                    encrypt_add_delete_ordinary_user_data= AesEntryDetry.encrypt(add_delete_ordinary_user_data);//加密
                } catch (Exception e) {
                    e.printStackTrace();
                }

                wait_send_data = "03" + user_id + str_crc+"00000000000000000000000000";
                Log.e("url", "wait_send_data==" + wait_send_data);
                Log.e("url", "wait_send_data.length==" + wait_send_data.length());

                BleService.getInstance().writeCharacteristic(sharedPreferencesUtil.getService(), sharedPreferencesUtil.getCharFfe3(), encrypt_add_delete_ordinary_user_data);
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
                            //05001e00ffa000000006000400122c00
//                            String back_data = DigitalTrans.bytesToHexString(characteristic.getValue());
                            String decrypt_back_data=null;
                            try {
                                decrypt_back_data=DigitalTrans.bytesToHexString(AesEntryDetry.decrypt(characteristic.getValue()));//解密
                            } catch (Exception e) {
                                e.printStackTrace();
                            }
                            Log.e("url", "返回值(解密后)==" + decrypt_back_data);
                            String delete_ordinary_user_back_data = decrypt_back_data.substring(decrypt_back_data.length() - 32, decrypt_back_data.length() - 26);//05001e
                            Log.e("url", "delete_ordinary_user_back_data==" + delete_ordinary_user_back_data);

                            String success_data = "05" + user_id + finalStr_crc;
                            Log.e("url", "success_data==" + success_data);

                            if (delete_ordinary_user_back_data.equals(success_data)) {
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
                intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);//关闭所有的activity，回到首页
                startActivity(intent);
            }
        });
    }

    private class OrdinaryUserAdapter extends BaseAdapter {

        @Override
        public int getCount() {
            return ordinaryUserData.size();
        }

        @Override
        public Map<String, Object> getItem(int position) {
            return ordinaryUserData.get(position);
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
                vh.tv_user_name = (TextView) convertView.findViewById(R.id.tv_phone_name);
                vh.tv_user_account = (TextView) convertView.findViewById(R.id.tv_phone_account);
            } else {
                vh = (ViewHolder) convertView.getTag();
            }
            convertView.setTag(vh);
            convertView.setClickable(true);

            vh.tv_user_name.setText("普通用户" + (Integer.parseInt(getItem(position).get("user_id").toString())));
            vh.tv_user_account.setText(getItem(position).get("password").toString());

            convertView.setOnLongClickListener(new View.OnLongClickListener() {
                @Override
                public boolean onLongClick(View v) {

                    String user_id = getItem(position).get("user_id").toString();
                    String pw = getItem(position).get("password").toString();

                    Log.e("url", "position==" + position);
                    Log.e("url", "要删除普通用户的user_id==" + user_id);
                    Log.e("url", "要删除普通用户的密码==" + pw);

                    initDeletePopWindow(position, user_id, pw);
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
            private TextView tv_user_name, tv_user_account;
        }
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
     * 删除数据库数据
     *
     * @param user_id
     */
    private void DeleteTableData(String user_id) {
        db = keyboardUserSQLiteOpenHelper.getWritableDatabase();
        ContentValues values = new ContentValues();
        values.put("password", "0");
//        db.update("Phone_User_Table", values, new String("id IN (SELECT id FROM Phone_User_Table WHERE user_id= " + user_id+")"), null);
        db.update("Disposable_User_Table", values, "mac=? and type=? and user_id=?", new String[]{BLEDetails.mac, "ordinary", user_id});

        ordinaryUserData = getData();
        ordinaryUserAdapter.notifyDataSetChanged();

        Log.e("url", "p删除后的ordinaryUserData.size()==" + ordinaryUserData.size());
        for (int i = 0; i < ordinaryUserData.size(); i++) {
            Log.e("url", "删除后的ordinaryUserData==" + ordinaryUserData.get(i).get("password"));
        }
        db.close();
    }

    /**
     * 加载数据库的数据到List中
     *
     * @return
     */
    private List<Map<String, Object>> getData() {
        db = keyboardUserSQLiteOpenHelper.getWritableDatabase();
        //游标查询每条数据
        Cursor cursor = db.query("Disposable_User_Table", null, "mac=? and type=?", new String[]{BLEDetails.mac, "ordinary"}, null, null, null);

        //定义list存储数据
        List<Map<String, Object>> list = new ArrayList<Map<String, Object>>();

        //读取数据 游标移动到下一行
        while (cursor.moveToNext()) {
            String user_type = cursor.getString(3);//user_type列
            String password = cursor.getString(2);//password列
            if (!user_type.equals("admin") && (!password.equals("0"))) {
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

    @Override
    protected void onResume() {
        super.onResume();
        MainActivity.is_scan = true;//停止扫描
    }
}

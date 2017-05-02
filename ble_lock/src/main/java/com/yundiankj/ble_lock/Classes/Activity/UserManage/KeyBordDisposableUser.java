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

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Created by hong on 2016/7/19.
 * <p>
 * 键盘用户（期限用户）
 */
public class KeyBordDisposableUser extends Activity {
    private Activity mActivity;
    private SharedPreferencesUtil sharedPreferencesUtil;
    private ImageView img_back;
    private TextView tv_title;
    private ListView lv_disposableUser;
    private DisposableUserAdapter disposableUserAdapter;

    private DisposableUserSQLiteOpenHelper disposableUserSQLiteOpenHelper;  //期限用户数据库
    private SQLiteDatabase db;
    private WaitSendDataSQLiteOpenHelper waitSendDataSQLiteOpenHelper;//未发送成功的数据表
    private SQLiteDatabase wait_send_data_db;

    private List<Map<String, Object>> disposableUserData;//定义一个列表储存 用户数据

    //删除popwindow
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
        disposableUserSQLiteOpenHelper = new DisposableUserSQLiteOpenHelper(mActivity, "DisposableUserDatabase.db", null, 2);
        disposableUserSQLiteOpenHelper.getWritableDatabase();//创建期限用户表
        waitSendDataSQLiteOpenHelper = new WaitSendDataSQLiteOpenHelper(mActivity, "WaitSendDataDatabase.db", null, 2);
        waitSendDataSQLiteOpenHelper.getWritableDatabase();//创建未发送成功的数据表

        img_back = (ImageView) mActivity.findViewById(R.id.img_back);
        tv_title = (TextView) mActivity.findViewById(R.id.tv_title);
        lv_disposableUser = (ListView) mActivity.findViewById(R.id.lv_phone_user_manage);

        tv_title.setText("期限用户");

        CheckOutOfTime();//检查一天、一周、一个月用户的end_time是否过期

        disposableUserData = getData();
        Log.e("url", "disposableUserData==" + disposableUserData.size());
        for (int i = 0; i < disposableUserData.size(); i++) {
            Log.e("url", "id==" + disposableUserData.get(i).get("id").toString() +
                    "   password==" + disposableUserData.get(i).get("password").toString() +
                    "  type==" + disposableUserData.get(i).get("type").toString() +
                    "   user_id==" + disposableUserData.get(i).get("user_id").toString() +
                    "  start_time==" + disposableUserData.get(i).get("start_time").toString() +
                    "  end_time==" + disposableUserData.get(i).get("end_time").toString() +
                    "  str_user==" + disposableUserData.get(i).get("str_user").toString());
        }

        disposableUserAdapter = new DisposableUserAdapter();
        lv_disposableUser.setAdapter(disposableUserAdapter);

        img_back.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                finish();
            }
        });
    }

    /**
     * 初始化长按删除popwindow
     *
     * @param pw
     * @param type
     * @param user_id
     */
    private void initDeletePopWindow(final String pw, final String type, final String user_id) {
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

                String str_time = null;
                //0x31(单次),0x32(一天),0x33(一星期),0x34(一个月)
                byte[] time = new byte[0];//(次数)
                if (type.equals("once")) {
                    time = new byte[]{0x31};
                    str_time = "31";
                } else if (type.equals("day")) {
                    time = new byte[]{0x32};
                    str_time = "32";
                } else if (type.equals("week")) {
                    time = new byte[]{0x33};
                    str_time = "33";
                } else if (type.equals("month")) {
                    time = new byte[]{0x34};
                    str_time = "34";
                }
                Log.e("url", "time==" + time);

                byte[] serial_num = DigitalTrans.hex2byte(user_id);//十六进制串转化为byte数组

                String str_pw = DigitalTrans.str2HexStr(pw);//字符串转换成十六进制字符串
                byte[] byte_pw = DigitalTrans.hex2byte(str_pw);//十六进制串转化为byte数组

                byte[] serial_num_pw = DigitalTrans.byteMerger(serial_num, byte_pw);//把序号、密码加在一起

                short[] short_delete_pw = new short[serial_num_pw.length];
                for (int i = 0; i < serial_num_pw.length; i++) {
                    short_delete_pw[i] = serial_num_pw[i];
                    Log.e("url", "short_delete_pw==" + short_delete_pw[i]);
                }
                short crc1 = 0;
                crc1 = BleService.getInstance().appData_Crc(short_delete_pw, crc1, short_delete_pw.length);
                String str_crc = Integer.toHexString(crc1);

                if (str_crc.length() == 1) {
                    str_crc = 0 + str_crc;
                }

                Log.e("url", "删除期限用户_CRC==" + str_crc);
                byte[] crc = DigitalTrans.hex2byte(str_crc);//十六进制串转化为byte数组

                byte[] head = new byte[]{0x08};
                byte[] head_time = DigitalTrans.byteMerger(head, time);//把头部和次数加在一起
                byte[] head_time_serial_num = DigitalTrans.byteMerger(head_time, serial_num);//把头部和次数、序号加在一起
                byte[] delete_disposable_user_data = DigitalTrans.byteMerger(head_time_serial_num, crc);//把头部和序号加在一起
                byte[] add_delete_disposable_user_data=DigitalTrans.byteMerger(delete_disposable_user_data, Const.add12);//补全16位

                Log.e("url", "add_delete_disposable_user_data==" + add_delete_disposable_user_data.length);
                byte[] encrypt_add_delete_disposable_user_data= null;
                //加密
                try {
                    encrypt_add_delete_disposable_user_data= AesEntryDetry.encrypt(add_delete_disposable_user_data);//加密
                } catch (Exception e) {
                    e.printStackTrace();
                }

                wait_send_data = "08" + str_time + user_id + str_crc+"000000000000000000000000";
                Log.e("url", "wait_send_data==" + wait_send_data);
                Log.e("url", "wait_send_data.length==" + wait_send_data.length());

                BleService.getInstance().writeCharacteristic(sharedPreferencesUtil.getService(), sharedPreferencesUtil.getCharFfe3(), encrypt_add_delete_disposable_user_data);
                BleService.getInstance().setCharacteristicNotification(sharedPreferencesUtil.getService(), sharedPreferencesUtil.getCharFfe2(), true);

                if (BleService.ble_connect.equals("disconnect")) {
                    SaveWaitSendDataToTable(wait_send_data, sharedPreferencesUtil.getCharFfe3(), sharedPreferencesUtil.getCharFfe3());//把未发送成功的数据存储到表中
                    Toast.makeText(mActivity, "删除成功！", Toast.LENGTH_SHORT).show();
                    pop_delete.dismiss();
                    DeleteTableData(type, user_id);
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
                    BleService.getInstance().setOnDataAvailableListener(new BleListener.OnDataAvailableListener() {
                        @Override
                        public void onCharacteristicRead(BluetoothGatt gatt, BluetoothGattCharacteristic characteristic, int status) {

                        }

                        @Override
                        public void onCharacteristicChanged(BluetoothGatt gatt, BluetoothGattCharacteristic characteristic) {
                            // 0a4b0a00ffa000000004006814070004
//                            String back_data = DigitalTrans.bytesToHexString(characteristic.getValue());
                            String decrypt_back_data=null;
                            try {
                                decrypt_back_data=DigitalTrans.bytesToHexString(AesEntryDetry.decrypt(characteristic.getValue()));//解密
                            } catch (Exception e) {
                                e.printStackTrace();
                            }
                            Log.e("url", "返回值(解密后)==" + decrypt_back_data);
                            String delete_disposable_user_back_data = decrypt_back_data.substring(decrypt_back_data.length() - 32, decrypt_back_data.length() - 28);//0a4b
                            Log.e("url", "delete_disposable_user_back_data==" + delete_disposable_user_back_data);

                            if (delete_disposable_user_back_data.equals("0a4b")) {
                                mActivity.runOnUiThread(new Runnable() {
                                    @Override
                                    public void run() {
                                        Log.e("url", "删除成功！");
                                        Toast.makeText(mActivity, "删除成功！", Toast.LENGTH_SHORT).show();
                                        pop_delete.dismiss();
                                        DeleteTableData(type, user_id);
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

    private class DisposableUserAdapter extends BaseAdapter {

        String user_type;

        @Override
        public int getCount() {
            return disposableUserData.size();
        }

        @Override
        public Map<String, Object> getItem(int position) {
            return disposableUserData.get(position);
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

            if (getItem(position).get("type").toString().equals("once")){
                user_type="单次";
            }else  if (getItem(position).get("type").toString().equals("day")){
                user_type="一天";
            }else if (getItem(position).get("type").toString().equals("week")){
                user_type="一星期";
            }else  if (getItem(position).get("type").toString().equals("month")){
                user_type="一个月";
            }

            vh.tv_user_name.setText("期限用户"+Integer.parseInt(getItem(position).get("user_id").toString())+"(" + user_type+")");
            vh.tv_user_account.setText(getItem(position).get("password").toString());

            convertView.setOnLongClickListener(new View.OnLongClickListener() {
                @Override
                public boolean onLongClick(View v) {
                    String pw = getItem(position).get("password").toString();
                    String type = getItem(position).get("type").toString();
                    String user_id = getItem(position).get("user_id").toString();

                    Log.e("url", "position==" + position);
                    Log.e("url", "删除期限用户：密码（password）==" + pw +
                            "   类型(type)==" + type +
                            "   序号（user_id）==" + user_id);

                    initDeletePopWindow(pw, type, user_id);
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
     * 检查一天、一周、一个月用户的end_time是否过期
     */
    private void CheckOutOfTime() {
        db = disposableUserSQLiteOpenHelper.getWritableDatabase();
        ContentValues values = new ContentValues();

        //游标查询每条数据
        Cursor cursor = db.query("Disposable_User_Table", null, "mac=? and type<>'once' and (type<>'admin' or type<>'ordinary') and end_time<>'0'", new String[]{BLEDetails.mac}, null, null, null);

        // 当前日期
        SimpleDateFormat format = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
        Date current_date = new Date();//取时间
        Calendar calendar = Calendar.getInstance();
        calendar.set(Calendar.DAY_OF_MONTH, calendar.get(Calendar.DAY_OF_MONTH) + 1);//时间推迟一天
        String current_time = format.format(current_date);
//        String current_time = "2016-09-13 14:10:02";
        Log.e("url", "当前时间==" + current_time);

        while (cursor.moveToNext()) {
            int count = cursor.getInt(0);
            if (count > 0) {
                String type = cursor.getString(3);
                String user_id = cursor.getString(4);
                String end_time = cursor.getString(6);
                Log.e("url", "type==" + type + "  user_id==" + user_id + "  end_time==" + end_time);
                Calendar c1 = Calendar.getInstance();
                Calendar c2 = Calendar.getInstance();
                try {
                    c1.setTime(format.parse(current_time));//当前时间
                    c2.setTime(format.parse(end_time));//结束时间
                } catch (java.text.ParseException e) {
                    System.err.println("格式不正确");
                }
                int result = c1.compareTo(c2);//result=0(c1相等c2),result<0(c1小于c2),result>0(c1大于c2)
                Log.e("url", "result==" + result);
                if (result > 0) {//过期
                    UpdateTableData2(type, user_id);//处理一些end_time过期的用户
                }
            }
        }
        cursor.close();
        db.close();
    }

    /**
     * 处理一些end_time过期的用户
     */
    private void UpdateTableData2(String type, String user_id) {
        db = disposableUserSQLiteOpenHelper.getWritableDatabase();
        ContentValues values = new ContentValues();

//        //游标查询每条数据
        Cursor cursor = db.query("Disposable_User_Table", null, "mac=? and type=? AND user_id=?", new String[]{BLEDetails.mac, type, user_id}, null, null, null);

        if (cursor.moveToNext()) {
            int count = cursor.getInt(0);
            if (count > 0) {//修改数据
                values.put("password", "0");
                values.put("start_time", "0");
                values.put("end_time", "0");
                db.update("Disposable_User_Table", values, "mac=? and type=? AND user_id=?", new String[]{BLEDetails.mac, type, user_id});
            }
        }
        cursor.close();
        db.close();
    }


    /**
     * 加载数据库的数据到List中
     *
     * @return
     */
    private List<Map<String, Object>> getData() {
        db = disposableUserSQLiteOpenHelper.getWritableDatabase();
        //游标查询每条数据
        Cursor cursor = db.query("Disposable_User_Table", null, "(type='once' or type='day' or type='week' or type='month') and mac=?  ", new String[]{BLEDetails.mac}, null, null, null);
        //定义list存储数据
        List<Map<String, Object>> list = new ArrayList<Map<String, Object>>();

        //读取数据 游标移动到下一行
        while (cursor.moveToNext()) {

            String password = cursor.getString(2);//password列
            if (!(password.equals("bbbbbbbbbbbb") | password.equals("0") | password.contains("b"))) {
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
        return list;
    }

    /**
     * 删除数据库数据
     *
     * @param type
     * @param user_id
     */
    private void DeleteTableData(String type, String user_id) {
        db = disposableUserSQLiteOpenHelper.getWritableDatabase();
        ContentValues values = new ContentValues();
        values.put("password", "bbbbbbbbbbbb");
        db.update("Disposable_User_Table", values, "mac=? and type=? AND user_id=?", new String[]{BLEDetails.mac, type, user_id});

        disposableUserData = getData();
        disposableUserAdapter.notifyDataSetChanged();

        Log.e("url", "p删除后的disposableUserData.size()==" + disposableUserData.size());
        for (int i = 0; i < disposableUserData.size(); i++) {
            Log.e("url", "删除后的disposableUserData: password==" + disposableUserData.get(i).get("password") +
                    "  type==" + disposableUserData.get(i).get("type") +
                    "   user_id==" + disposableUserData.get(i).get("user_id"));
        }
        db.close();
    }

    @Override
    protected void onResume() {
        super.onResume();
        MainActivity.is_scan = true;//停止扫描
    }
}

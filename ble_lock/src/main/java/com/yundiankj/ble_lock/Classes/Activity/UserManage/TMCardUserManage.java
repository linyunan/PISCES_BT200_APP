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
import com.yundiankj.ble_lock.Resource.SQLite.TMCardUserSQLiteOpenHelper;
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
 * TM卡用户管理
 */
public class TMCardUserManage extends Activity {

    private Activity mActivity;
    private SharedPreferencesUtil sharedPreferencesUtil;
    private ImageView img_back;
    private TextView tv_title;
    private ListView lv_tm_card_user_manage;
    private TMCardUserManageAdapter tmCardUserManageAdapter;

    private TMCardUserSQLiteOpenHelper tmCardUserSQLiteOpenHelper;//TM卡用户数据库
    private SQLiteDatabase db;
    private WaitSendDataSQLiteOpenHelper waitSendDataSQLiteOpenHelper;//未发送成功的数据表
    private SQLiteDatabase wait_send_data_db;

    private List<Map<String, Object>> tm_cardUserData;//定义一个列表储存 TM卡用户数据

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
        tmCardUserSQLiteOpenHelper = new TMCardUserSQLiteOpenHelper(mActivity, "TMCardUserDatabase.db", null, 2);
        tmCardUserSQLiteOpenHelper.getWritableDatabase();//创建TM卡用户表
        waitSendDataSQLiteOpenHelper = new WaitSendDataSQLiteOpenHelper(mActivity, "WaitSendDataDatabase.db", null, 2);
        waitSendDataSQLiteOpenHelper.getWritableDatabase();//创建未发送成功的数据表

        img_back = (ImageView) mActivity.findViewById(R.id.img_back);
        tv_title = (TextView) mActivity.findViewById(R.id.tv_title);
        lv_tm_card_user_manage = (ListView) mActivity.findViewById(R.id.lv_phone_user_manage);

        tv_title.setText("TM卡用户管理");

        tm_cardUserData = getData();
        Log.e("url", "tm_cardUserData.size==" + tm_cardUserData.size());

        tmCardUserManageAdapter = new TMCardUserManageAdapter();
        lv_tm_card_user_manage.setAdapter(tmCardUserManageAdapter);

        img_back.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                finish();
            }
        });
    }

    //初始化长按删除popwindow
    private void initDeletePopWindow(final int position, final String user_num) {
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
                Log.e("url", "选中要删除的卡号==" + tm_cardUserData.get(position).get("user_num").toString());

                byte[] head = new byte[]{0x04};
                byte[] delete_tm_card = DigitalTrans.hex2byte(tm_cardUserData.get(position).get("user_num").toString());//十六进制串转化为byte数组
                byte[] delete_tm_card_data = DigitalTrans.byteMerger(head, delete_tm_card);//把头部和序号加在一起
                byte[] add_delete_tm_card_data=DigitalTrans.byteMerger(delete_tm_card_data, Const.add9);//补全16位

                Log.e("url", "add_delete_tm_card_data==" + add_delete_tm_card_data.length);
                byte[] encrypt_add_delete_tm_card_data= null;
                //加密
                try {
                    encrypt_add_delete_tm_card_data= AesEntryDetry.encrypt(add_delete_tm_card_data);//加密
                } catch (Exception e) {
                    e.printStackTrace();
                }

                wait_send_data = "04" + tm_cardUserData.get(position).get("user_num").toString()+"000000000000000000";
                Log.e("url", "wait_send_data==" + wait_send_data);
                Log.e("url", "wait_send_data.length==" + wait_send_data.length());

                BleService.getInstance().writeCharacteristic(sharedPreferencesUtil.getService(), sharedPreferencesUtil.getCharFfe3(), encrypt_add_delete_tm_card_data);
                BleService.getInstance().setCharacteristicNotification(sharedPreferencesUtil.getService(), sharedPreferencesUtil.getCharFfe2(), true);

                if (BleService.ble_connect.equals("disconnect")) {
                    SaveWaitSendDataToTable(wait_send_data, sharedPreferencesUtil.getCharFfe3(), sharedPreferencesUtil.getCharFfe3());//把未发送成功的数据存储到表中
                    Toast.makeText(mActivity, "删除成功！", Toast.LENGTH_SHORT).show();
                    pop_delete.dismiss();
                    DeleteTableData(user_num);
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

                            //064b0a00ffa0000000040068140a0004
//                            String back_data = DigitalTrans.bytesToHexString(characteristic.getValue());
                            String decrypt_back_data=null;
                            try {
                                decrypt_back_data=DigitalTrans.bytesToHexString(AesEntryDetry.decrypt(characteristic.getValue()));//解密
                            } catch (Exception e) {
                                e.printStackTrace();
                            }
                            Log.e("url", "返回值(解密后)==" + decrypt_back_data);
                            String delete_tm_card_user_back_data = decrypt_back_data.substring(decrypt_back_data.length() - 32, decrypt_back_data.length() - 28);//064b
                            Log.e("url", "delete_tm_card_user_back_data==" + delete_tm_card_user_back_data);

                            if (delete_tm_card_user_back_data.equals("064b")) {

                                mActivity.runOnUiThread(new Runnable() {
                                    @Override
                                    public void run() {
                                        Log.e("url", "删除成功！");
                                        Toast.makeText(mActivity, "删除成功！", Toast.LENGTH_SHORT).show();
                                        pop_delete.dismiss();
                                        DeleteTableData(user_num);
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


    private class TMCardUserManageAdapter extends BaseAdapter {

        @Override
        public int getCount() {
            return tm_cardUserData.size();
        }

        @Override
        public Map<String, Object> getItem(int position) {
            return tm_cardUserData.get(position);
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
                vh.tv_tm_card_name = (TextView) convertView.findViewById(R.id.tv_phone_name);
                vh.tv_tm_card_account = (TextView) convertView.findViewById(R.id.tv_phone_account);
            } else {
                vh = (ViewHolder) convertView.getTag();
            }
            convertView.setTag(vh);
            convertView.setClickable(true);

            vh.tv_tm_card_name.setText("TM卡用户" + getItem(position).get("user_id"));
            vh.tv_tm_card_account.setText(getItem(position).get("user_num").toString());

            convertView.setOnLongClickListener(new View.OnLongClickListener() {
                @Override
                public boolean onLongClick(View v) {

                    Log.e("url", "position==" + position);
                    Log.e("url", "删除TM卡用户的卡号==" + getItem(position).get("user_num").toString());

                    initDeletePopWindow(position, getItem(position).get("user_num").toString());
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
            private TextView tv_tm_card_name, tv_tm_card_account;
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
     * 更新TM卡用户列表
     *
     * @param user_id
     * @param user_num
     */
    private void UpdateTMCardUserTableData(String user_id, String user_num) {
        db = tmCardUserSQLiteOpenHelper.getWritableDatabase();
        ContentValues values = new ContentValues();

        //游标查询每条数据
        Cursor cursor = db.query("TM_Card_User_Table", null, "mac=? and user_id=?", new String[]{BLEDetails.mac, user_id}, null, null, null);

        if (cursor.moveToNext()) {
            int count = cursor.getInt(0);
            if (count > 0) {//数据存在
                Log.e("url", "用户存在，进行更新！");
                values.put("user_num", user_num);
                db.update("TM_Card_User_Table", values, "user_id=?", new String[]{user_id});
            }
        } else {
            values.put("mac", BLEDetails.mac);
            values.put("user_id", user_id);
            values.put("user_num", user_num);
            db.insert("TM_Card_User_Table", null, values);
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
        db = tmCardUserSQLiteOpenHelper.getWritableDatabase();
        //游标查询每条数据
        Cursor cursor = db.query("TM_Card_User_Table", null, "mac=?", new String[]{BLEDetails.mac}, null, null, null);

        //定义list存储数据
        List<Map<String, Object>> list = new ArrayList<Map<String, Object>>();

        //读取数据 游标移动到下一行
        while (cursor.moveToNext()) {
            String user_num = cursor.getString(3);
            Log.e("url", "user_num==" + user_num);
            if (!user_num.equals("ffffffffffff")) {
                Map<String, Object> map = new HashMap<String, Object>();
                map.put("id", cursor.getString(cursor.getColumnIndex("id")));
                map.put("mac", cursor.getString(cursor.getColumnIndex("mac")));
                map.put("user_id", cursor.getString(cursor.getColumnIndex("user_id")));
                map.put("user_num", cursor.getString(cursor.getColumnIndex("user_num")));
                list.add(map);
            }
        }
        cursor.close();
        db.close();
        return list;
    }

    /**
     * 删除数据库数据
     *
     * @param user_num
     */
    private void DeleteTableData(String user_num) {
        db = tmCardUserSQLiteOpenHelper.getWritableDatabase();
        ContentValues values = new ContentValues();
        values.put("user_num", "ffffffffffff");
        db.update("TM_Card_User_Table", values, "mac=? and user_num=?", new String[]{BLEDetails.mac, user_num});

        tm_cardUserData = getData();
        tmCardUserManageAdapter.notifyDataSetChanged();

        Log.e("url", "删除后的tm_cardUserData.size()==" + tm_cardUserData.size());
        for (int i = 0; i < tm_cardUserData.size(); i++) {
            Log.e("url", "删除后的tm_cardUserData==" + tm_cardUserData.get(i).get("user_num"));
        }
        db.close();
    }

    @Override
    protected void onResume() {
        super.onResume();
        MainActivity.is_scan = true;//停止扫描
    }

}

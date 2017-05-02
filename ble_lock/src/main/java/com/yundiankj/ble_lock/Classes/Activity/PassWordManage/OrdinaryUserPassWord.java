package com.yundiankj.ble_lock.Classes.Activity.PassWordManage;

import android.app.Activity;
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
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.PopupWindow;
import android.widget.TextView;
import android.widget.Toast;

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
 * <p/>
 * 普通用户密码
 */
public class OrdinaryUserPassWord extends Activity implements View.OnClickListener {

    private Activity mActivity;
    private SharedPreferencesUtil sharedPreferencesUtil;
    private ImageView img_back;
    private TextView tv_main_title, tv_add;
    private ListView lv_ordinary_user_pw;
    private OrdinaryUserPassWordAdapter ordinaryUserPassWordAdapter;


    private DisposableUserSQLiteOpenHelper keyboardUserSQLiteOpenHelper;//键盘用户表
    private SQLiteDatabase db;
    private WaitSendDataSQLiteOpenHelper waitSendDataSQLiteOpenHelper;//未发送成功的数据表
    private SQLiteDatabase wait_send_data_db;

    private List<Map<String, Object>> ordinaryUserData;  //定义一个列表储存 普通用户数据

    //修改普通用户密码popWindow
    private PopupWindow pop_editOrdinaryUserPassWord;
    private LinearLayout lly_old_pw;
    private EditText et_old_pw, et_new_pw, et_confirm_pw;
    private TextView tv_title, tv_save;

    //添加popWindow
    private PopupWindow pop_add;
    private EditText et_password;
    private TextView tv_pop_add;

    //返回首页popWindow
    private PopupWindow pop_back;
    private TextView tv_cancel, tv_ok;

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
        tv_main_title = (TextView) mActivity.findViewById(R.id.tv_title);
        tv_add = (TextView) mActivity.findViewById(R.id.tv_add);
        lv_ordinary_user_pw = (ListView) mActivity.findViewById(R.id.lv_phone_user_manage);

        tv_main_title.setText("普通用户密码");
        tv_add.setVisibility(View.VISIBLE);

        ordinaryUserData = getData();
        Log.e("url", "ordinaryUserData.size==" + ordinaryUserData.size());

        for (int i = 0; i < ordinaryUserData.size(); i++) {
            Log.e("url", "password==" + ordinaryUserData.get(i).get("password").toString() +
                    "    type==" + ordinaryUserData.get(i).get("type").toString() +
                    "    user_id==" + ordinaryUserData.get(i).get("user_id").toString());
        }

        ordinaryUserPassWordAdapter = new OrdinaryUserPassWordAdapter();
        lv_ordinary_user_pw.setAdapter(ordinaryUserPassWordAdapter);

        img_back.setOnClickListener(this);
        tv_add.setOnClickListener(this);

    }

    /**
     * 初始化修改普通用户密码的popwindow
     *
     * @param position
     */
    private void initEditAdminPassWordPopWindow(final int position) {
        LayoutInflater inflater = LayoutInflater.from(mActivity);
        View edit_layout = inflater.inflate(R.layout.pop_edit_admin_pw, null);
        lly_old_pw = (LinearLayout) edit_layout.findViewById(R.id.lly_old_pw);
        et_old_pw = (EditText) edit_layout.findViewById(R.id.et_old_pw);
        et_new_pw = (EditText) edit_layout.findViewById(R.id.et_new_pw);
        et_confirm_pw = (EditText) edit_layout.findViewById(R.id.et_confirm_pw);
        tv_title = (TextView) edit_layout.findViewById(R.id.tv_title);
        tv_save = (TextView) edit_layout.findViewById(R.id.tv_save);
        tv_title.setText("修改普通用户密码");
        lly_old_pw.setVisibility(View.GONE);
        edit_layout.invalidate();
        pop_editOrdinaryUserPassWord = new PopupWindow(edit_layout, ViewGroup.LayoutParams.FILL_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        //实例化一个ColorDrawable颜色为半透明
        ColorDrawable dw = new ColorDrawable(0x00000000);
        //设置PopupWindow弹出窗体的背景
        pop_editOrdinaryUserPassWord.setBackgroundDrawable(dw);
        pop_editOrdinaryUserPassWord.setOutsideTouchable(true);
        pop_editOrdinaryUserPassWord.setFocusable(true);
        pop_editOrdinaryUserPassWord.setOnDismissListener(new PopupWindow.OnDismissListener() {
            @Override
            public void onDismiss() {
                WindowManager.LayoutParams lp = mActivity.getWindow().getAttributes();
                lp.alpha = 1;
                mActivity.getWindow().setAttributes(lp);
            }
        });

        tv_save.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

                String pw = ordinaryUserData.get(position).get("password").toString();
                final String user_id = ordinaryUserData.get(position).get("user_id").toString();

                if (et_new_pw.getText().toString().equals("")) {
                    Toast.makeText(mActivity, "新密码不能为空！", Toast.LENGTH_SHORT).show();
                } else if (et_confirm_pw.getText().toString().equals("")) {
                    Toast.makeText(mActivity, "确认密码不能为空！", Toast.LENGTH_SHORT).show();
                } else if (!et_new_pw.getText().toString().equals(et_confirm_pw.getText().toString())) {
                    Toast.makeText(mActivity, "新密码与确认密码不一致，请重新输入！", Toast.LENGTH_SHORT).show();
                } else if (et_new_pw.getText().toString().length() < 6) {
                    Toast.makeText(mActivity, "新密码位数不能少于6位", Toast.LENGTH_SHORT).show();
                }else if (et_new_pw.getText().toString().equals(pw)) {
                    Toast.makeText(mActivity, "新密码与原密码一样！", Toast.LENGTH_SHORT).show();
                } else {

                    Log.e("url", "要修改的密码==" + pw);
                    Log.e("url", "要修改的密码的user_id==" + user_id);

                    db = keyboardUserSQLiteOpenHelper.getWritableDatabase();
                    ContentValues values = new ContentValues();

                    //游标查询每条数据
                    Cursor cursor = db.query("Disposable_User_Table", null, "mac=? and password=?", new String[]{BLEDetails.mac, et_confirm_pw.getText().toString()}, null, null, null);

                    if (cursor.moveToNext()) {
                        int count = cursor.getInt(0);
                        Log.e("url", "count==" + count);
                        if (count > 0) {//数据存在
                            Toast.makeText(mActivity, "该用户已经存在!", Toast.LENGTH_SHORT).show();
                            Log.e("url", "该用户已经存在！");
                        }
                        cursor.close();
                    }else {
                        BleService.getInstance().back_data = "";
                        UpdateBLEData(et_new_pw.getText().toString(), user_id);

                        if (BleService.ble_connect.equals("disconnect")) {
                            SaveWaitSendDataToTable(wait_send_data, sharedPreferencesUtil.getCharFfe3(), sharedPreferencesUtil.getCharFfe2());//把未发送成功的数据存储到表中
                            UpdateOrdinaryUserTableData(user_id, et_new_pw.getText().toString());
                            Toast.makeText(mActivity, "修改密码成功！", Toast.LENGTH_SHORT).show();
                            pop_editOrdinaryUserPassWord.dismiss();
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

                            tv_save.postDelayed(new Runnable() {
                                @Override
                                public void run() {
                                    String back_data = BleService.getInstance().back_data;
                                    if (back_data.length() == 32) {
                                        String edit_ordinary_user_back_data = back_data.substring(back_data.length() - 32, back_data.length() - 28);//084b
                                        Log.e("url", "edit_ordinary_user_back_data==" + edit_ordinary_user_back_data);
                                        if (edit_ordinary_user_back_data.equals("084b")) {

                                            UpdateOrdinaryUserTableData(user_id, et_new_pw.getText().toString());
                                            Toast.makeText(mActivity, "修改密码成功！", Toast.LENGTH_SHORT).show();
                                            pop_editOrdinaryUserPassWord.dismiss();
                                        } else if (edit_ordinary_user_back_data.equals("55ff")) {
                                            Toast.makeText(mActivity, "修改密码失败，重新设置！", Toast.LENGTH_SHORT).show();
                                        }
                                    }
                                }
                            }, 500);
                        }
                    }
                }
                MainActivity.is_scan=true;
            }
        });

    }

    @Override
    public void onClick(View v) {
        switch (v.getId()) {
            case R.id.img_back://返回
                finish();
                break;
            case R.id.tv_add://添加
                initAddPopWindow();
                WindowManager.LayoutParams lp;
                //设置背景颜色变暗
                lp = getWindow().getAttributes();
                lp.alpha = 0.5f;
                getWindow().setAttributes(lp);

                pop_add.setAnimationStyle(R.style.PopupAnimation);
                pop_add.showAtLocation(mActivity.findViewById(R.id.lly_main), Gravity.CENTER | Gravity.CENTER_HORIZONTAL, 0, 0);
                pop_add.update();
                break;
        }
    }

    /**
     * 适配器
     */
    private class OrdinaryUserPassWordAdapter extends BaseAdapter {

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
                convertView = LayoutInflater.from(mActivity).inflate(R.layout.ordinary_user_pw_listitem, null);
                vh = new ViewHolder();
                vh.tv_name = (TextView) convertView.findViewById(R.id.tv_name);
                vh.tv_edit = (TextView) convertView.findViewById(R.id.tv_edit);
            } else {
                vh = (ViewHolder) convertView.getTag();
            }
            convertView.setTag(vh);
            convertView.setClickable(true);

            vh.tv_name.setText("用户" + (Integer.parseInt(getItem(position).get("user_id").toString())));

            vh.tv_edit.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {

                    Log.e("url", "position==" + position);
                    Log.e("url", "选中的密码==" + getItem(position).get("password").toString());

                    initEditAdminPassWordPopWindow(position);
                    WindowManager.LayoutParams lp;
                    //设置背景颜色变暗
                    lp = getWindow().getAttributes();
                    lp.alpha = 0.5f;
                    getWindow().setAttributes(lp);

                    pop_editOrdinaryUserPassWord.setAnimationStyle(R.style.PopupAnimation);
                    pop_editOrdinaryUserPassWord.showAtLocation(mActivity.findViewById(R.id.lly_main), Gravity.CENTER | Gravity.CENTER_HORIZONTAL, 0, 0);
                    pop_editOrdinaryUserPassWord.update();
                }
            });

            return convertView;
        }

        private class ViewHolder {
            private TextView tv_name, tv_edit;
        }
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

    /**
     * 初始化添加的popwindow
     */
    private void initAddPopWindow() {
        LayoutInflater inflater = LayoutInflater.from(mActivity);
        View edit_layout = inflater.inflate(R.layout.pop_add_ordinary_user, null);
        et_password = (EditText) edit_layout.findViewById(R.id.et_password);
        tv_pop_add = (TextView) edit_layout.findViewById(R.id.tv_pop_add);
        edit_layout.invalidate();
        pop_add = new PopupWindow(edit_layout, ViewGroup.LayoutParams.FILL_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        //实例化一个ColorDrawable颜色为半透明
        ColorDrawable dw = new ColorDrawable(0x00000000);
        //设置PopupWindow弹出窗体的背景
        pop_add.setBackgroundDrawable(dw);
        pop_add.setOutsideTouchable(true);
        pop_add.setFocusable(true);
        pop_add.setOnDismissListener(new PopupWindow.OnDismissListener() {
            @Override
            public void onDismiss() {
                WindowManager.LayoutParams lp = mActivity.getWindow().getAttributes();
                lp.alpha = 1;
                mActivity.getWindow().setAttributes(lp);
            }
        });

        tv_pop_add.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

                if (et_password.getText().toString().equals("")) {
                    Toast.makeText(mActivity, "密码不能为空！", Toast.LENGTH_SHORT).show();
                } else if (et_password.getText().toString().length() < 6) {
                    Toast.makeText(mActivity, "密码不能少于6位！", Toast.LENGTH_SHORT).show();
                } else {
                    AddOrdinaryUserTableData(et_password.getText().toString());
                }

            }
        });
    }

    /**
     * 判断数据库是否有该用户
     *
     * @param pw
     */
    private void AddOrdinaryUserTableData(String pw) {
        db = keyboardUserSQLiteOpenHelper.getWritableDatabase();
        ContentValues values = new ContentValues();

        //游标查询每条数据
        Cursor cursor = db.query("Disposable_User_Table", null, "mac=? and password=?", new String[]{BLEDetails.mac, pw}, null, null, null);

        if (cursor.moveToNext()) {
            int count = cursor.getInt(0);
            Log.e("url", "count==" + count);
            if (count > 0) {//数据存在
                Toast.makeText(mActivity, "该用户已经存在!", Toast.LENGTH_SHORT).show();
                Log.e("url", "该用户已经存在！");
            }
        } else {
            String sql2 = "SELECT user_id FROM Disposable_User_Table WHERE mac='" + BLEDetails.mac + "' and password = '0' and type='ordinary' ORDER BY id ASC LIMIT 0,1";

            Cursor cursor2 = db.rawQuery(sql2, null);
            if (cursor2.moveToNext()) {
                int id_index = cursor2.getColumnIndex("user_id");
                String user_id = cursor2.getString(id_index);
                Log.e("url", "id_index==" + id_index);
                Log.e("url", "user_id==" + user_id);

                BleService.getInstance().back_data = "";

                UpdateBLEData(pw, user_id);//添加用户后更新蓝牙用户信息

                if (BleService.ble_connect.equals("disconnect")) {
                    SaveWaitSendDataToTable(wait_send_data, sharedPreferencesUtil.getCharFfe3(), sharedPreferencesUtil.getCharFfe2());//把未发送成功的数据存储到表中
                    Toast.makeText(mActivity, "添加添加普通用户密码成功！", Toast.LENGTH_SHORT).show();
                    UpdateTableData(pw, user_id);
                    pop_add.dismiss();
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

//                    try {
//                        Thread.sleep(500);
//                    } catch (InterruptedException e) {
//                        e.printStackTrace();
//                    }

                    synchronized (this) {
                        try {
                            wait(500);
                        } catch (InterruptedException e) {
                            e.printStackTrace();
                        }
                    }

                    String back_data = BleService.getInstance().back_data;

                    if (back_data.length() == 32) {
                        String add_ordinary_user_back_data = back_data.substring(back_data.length() - 32, back_data.length() - 28);//084b
                        Log.e("url", "add_ordinary_user_back_data==" + add_ordinary_user_back_data);

                        if (add_ordinary_user_back_data.equals("084b")) {
                            Toast.makeText(mActivity, "添加添加普通用户密码成功！", Toast.LENGTH_SHORT).show();
                            UpdateTableData(pw, user_id);
                            pop_add.dismiss();
                        } else if (add_ordinary_user_back_data.equals("55ff")) {
                            Toast.makeText(mActivity, "添加添加普通用户密码失败，重新设置！", Toast.LENGTH_SHORT).show();
                        }
                    }
                }

            }

//        //游标查询每条数据
//        Cursor cursor2 = db.query("KeyboardUser_Table", null, "password=?", new String[]{pw}, null, null, null);
//        if (cursor2.moveToNext()) {
//            int id_index = cursor2.getColumnIndex("user_id");
//            String user_id = cursor2.getString(id_index);
//            Log.e("url", "id_index==" + id_index);
//            Log.e("url", "user_id==" + user_id);
//
//            UpdateBLEData(pw, user_id);//添加用户后更新蓝牙用户信息
//        }
        }
        MainActivity.is_scan=true;
        cursor.close();
        db.close();
    }

    /**
     * 更新键盘普通用户的数据
     *
     * @param pw
     * @param user_id
     */
    private void UpdateBLEData(String pw, String user_id) {
        Log.e("url", "pw==" + pw);
        Log.e("url", "user_id==" + user_id);

        byte[] head = new byte[]{0x06};//固定的前面只是0x06,

        byte[] serial_num = DigitalTrans.hex2byte(user_id);//十六进制串转化为byte数组

        Log.e("url", "serial_num==" + serial_num);

        String str_pw = DigitalTrans.str2HexStr(pw);//字符串转换成十六进制字符串
        byte[] byte_pw = DigitalTrans.hex2byte(str_pw);//十六进制串转化为byte数组(序号)
        byte[] serial_num_pw = DigitalTrans.byteMerger(serial_num, byte_pw);//把次数和序号，密码加在一起

        short[] short_pw = new short[serial_num_pw.length];
        for (int i = 0; i < serial_num_pw.length; i++) {
            short_pw[i] = serial_num_pw[i];
            Log.e("url", "serial_num_pw==" + serial_num_pw[i]);
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

        byte[] data1 = DigitalTrans.byteMerger(head, serial_num_pw);//把头部和密码加在一起
        byte[] data = DigitalTrans.byteMerger(data1, crc);//把头部和密码加在一起
        byte[] add_data=DigitalTrans.byteMerger(data, Const.add7);//补全16位

        Log.e("url", "add_data==" + add_data.length);
        byte[] encrypt_add_data= null;
        //加密
        try {
            encrypt_add_data= AesEntryDetry.encrypt(add_data);//加密
        } catch (Exception e) {
            e.printStackTrace();
        }
        wait_send_data = "06" + user_id + str_pw + str_crc+"00000000000000";
        Log.e("url", "wait_send_data==" + wait_send_data);
        Log.e("url", "wait_send_data.length==" + wait_send_data.length());

        BleService.getInstance().writeCharacteristic(sharedPreferencesUtil.getService(), sharedPreferencesUtil.getCharFfe3(), encrypt_add_data);
        BleService.getInstance().setCharacteristicNotification(sharedPreferencesUtil.getService(), sharedPreferencesUtil.getCharFfe2(), true);

    }

    /**
     * 蓝牙断开，重新返回首页连接蓝牙
     */
    private void initBackPopWindow() {
        LayoutInflater inflater = LayoutInflater.from(mActivity);
        View back_layout = inflater.inflate(R.layout.pop_back_main, null);
        tv_cancel = (TextView) back_layout.findViewById(R.id.tv_cancel);
        tv_ok = (TextView) back_layout.findViewById(R.id.tv_ok);
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

        tv_cancel.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                pop_back.dismiss();
            }
        });

        tv_ok.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                pop_back.dismiss();
                Intent intent = new Intent(mActivity, MainActivity.class);
                intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);//关闭所有的activity,返回首页
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
     * 更新数据库数据
     *
     * @param pw
     * @param user_id
     */
    private void UpdateTableData(String pw, String user_id) {
        db = keyboardUserSQLiteOpenHelper.getWritableDatabase();
        ContentValues values = new ContentValues();

        values.put("password", pw);
        db.update("Disposable_User_Table", values, "mac=? and type=? and user_id=?", new String[]{BLEDetails.mac, "ordinary", user_id});

        ordinaryUserData = getData();
        ordinaryUserPassWordAdapter.notifyDataSetChanged();

        db.close();
    }

    /**
     * 修改密码成功后，修改表数据
     *
     * @param user_id
     * @param new_pw
     */
    private void UpdateOrdinaryUserTableData(String user_id, String new_pw) {
        Log.e("url", "要修改的user_id==" + user_id);
        Log.e("url", "新密码==" + new_pw);

        db = keyboardUserSQLiteOpenHelper.getWritableDatabase();
        ContentValues values = new ContentValues();
        values.put("password", new_pw);
        db.update("Disposable_User_Table", values, "mac=? and type=? and user_id=?", new String[]{BLEDetails.mac, "ordinary", user_id});

        ordinaryUserData = getData();
        ordinaryUserPassWordAdapter.notifyDataSetChanged();

        for (int i = 0; i < ordinaryUserData.size(); i++) {
            Log.e("url", "修改成功后的pw==" + ordinaryUserData.get(i).get("password"));
        }
        db.close();
    }

    @Override
    protected void onResume() {
        super.onResume();
        MainActivity.is_scan = true;//停止扫描
    }
}

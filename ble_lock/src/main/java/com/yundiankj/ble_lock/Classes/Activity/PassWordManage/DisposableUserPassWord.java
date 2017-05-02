package com.yundiankj.ble_lock.Classes.Activity.PassWordManage;

import android.app.Activity;
import android.content.ContentValues;
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
import android.widget.ListView;
import android.widget.PopupWindow;
import android.widget.RadioButton;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.yundiankj.ble_lock.Classes.Activity.BLEDetails;
import com.yundiankj.ble_lock.Classes.Activity.MainActivity;
import com.yundiankj.ble_lock.Classes.Model.DisposableDataInfo;
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
import java.util.List;

/**
 * Created by hong on 2016/7/19.
 * <p>
 * 期限用户密码
 */
public class DisposableUserPassWord extends Activity {

    private Activity mActivity;
    private SharedPreferencesUtil sharedPreferencesUtil;
    private ImageView img_back;
    private TextView tv_title, tv_add;
    private ListView lv_phone_user_manage;
    private DisposableUserAdapter disposableUserAdapter;//适配器

    //数据库
    private DisposableUserSQLiteOpenHelper disposableUserSQLiteOpenHelper; //期限用户数据库
    private SQLiteDatabase db;
    private WaitSendDataSQLiteOpenHelper waitSendDataSQLiteOpenHelper;//未发送成功的数据表
    private SQLiteDatabase wait_send_data_db;

    private List<DisposableDataInfo> disposableUserData; //定义一个列表储存 用户数据

    //添加popWindow
    private PopupWindow pop_add;
    private EditText et_password;
    private RadioButton rb_one_time, rb_one_day, rb_one_week, rb_one_month;
    private TextView tv_pop_add;

    //返回首页popWindow
    private PopupWindow pop_back;
    private TextView tv_cancel,tv_ok;

    String type = null;//1,单次  2，一天  3，一星期 4，一个月

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
        tv_add = (TextView) mActivity.findViewById(R.id.tv_add);
        lv_phone_user_manage = (ListView) mActivity.findViewById(R.id.lv_phone_user_manage);

        tv_title.setText("期限用户密码");
        tv_add.setVisibility(View.VISIBLE);

        CheckOutOfTime();//检查一天、一周、一个月用户的end_time是否过期

        disposableUserData = getData();
        Log.e("url", "disposableUserData==" + disposableUserData.size());

//        for (int i = 0; i < disposableUserData.size(); i++) {
//            Log.e("url", "id==" + disposableUserData.get(i).get("id").toString() +
//                    "   password==" + disposableUserData.get(i).get("password").toString() +
//                    "  type==" + disposableUserData.get(i).get("type").toString() +
//                    "   user_id==" + disposableUserData.get(i).get("user_id").toString() +
//                    "  start_time==" + disposableUserData.get(i).get("start_time").toString() +
//                    "  end_time==" + disposableUserData.get(i).get("end_time").toString() +
//                    "  str_user==" + disposableUserData.get(i).get("str_user").toString());
//
//        }

        for (int i = 0; i < disposableUserData.size(); i++) {
            Log.e("url", "id==" + disposableUserData.get(i).getId().toString() +
                    "   password==" + disposableUserData.get(i).getPassword().toString() +
                    "  type==" + disposableUserData.get(i).getType().toString() +
                    "   user_id==" + disposableUserData.get(i).getUser_id().toString() +
                    "  start_time==" + disposableUserData.get(i).getStart_time().toString() +
                    "  end_time==" + disposableUserData.get(i).getEnd_time().toString() +
                    "  str_user==" + disposableUserData.get(i).getStr_user().toString());

        }

        disposableUserAdapter = new DisposableUserAdapter();
        lv_phone_user_manage.setAdapter(disposableUserAdapter);

        img_back.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                finish();
            }
        });

        //添加
        tv_add.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                initAddPopWindow();
                WindowManager.LayoutParams lp;
                //设置背景颜色变暗
                lp = getWindow().getAttributes();
                lp.alpha = 0.5f;
                getWindow().setAttributes(lp);

                pop_add.setAnimationStyle(R.style.PopupAnimation);
                pop_add.showAtLocation(mActivity.findViewById(R.id.lly_main), Gravity.CENTER | Gravity.CENTER_HORIZONTAL, 0, 0);
                pop_add.update();
            }
        });

    }

    //初始化添加的popwindow
    private void initAddPopWindow() {
        LayoutInflater inflater = LayoutInflater.from(mActivity);
        View edit_layout = inflater.inflate(R.layout.pop_add_disposable_user, null);
        et_password = (EditText) edit_layout.findViewById(R.id.et_password);
        rb_one_time = (RadioButton) edit_layout.findViewById(R.id.rb_one_time);
        rb_one_day = (RadioButton) edit_layout.findViewById(R.id.rb_one_day);
        rb_one_week = (RadioButton) edit_layout.findViewById(R.id.rb_one_week);
        rb_one_month = (RadioButton) edit_layout.findViewById(R.id.rb_one_month);
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
                if (rb_one_time.isChecked()) {
                    type = "once";
                    Log.e("url", "单次");
                } else if (rb_one_day.isChecked()) {
                    type = "day";
                    Log.e("url", "一天");
                } else if (rb_one_week.isChecked()) {
                    type = "week";
                    Log.e("url", "一星期");
                } else if (rb_one_month.isChecked()) {
                    type = "month";
                    Log.e("url", "一个月");
                }

                Log.e("url", "type==" + type);

                if (type == null) {
                    Toast.makeText(mActivity, "请选择用户类型！", Toast.LENGTH_SHORT).show();
                } else {
                    //先进行判断是否存在和是否有效（还有获取序列号）
                    if (et_password.getText().toString().length() < 6) {
                        Toast.makeText(mActivity, "密码不能少于6位数！", Toast.LENGTH_SHORT).show();
                    } else {
                        AddDisposableUserTableData(et_password.getText().toString(), type);
                    }
                    type = null;
                }


            }
        });
    }

    /**
     * 适配器
     */
    private class DisposableUserAdapter extends BaseAdapter {

        private boolean is_open = false;
        String user_type;

        @Override
        public int getCount() {
            return disposableUserData.size();
        }

        @Override
        public DisposableDataInfo getItem(int position) {
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
                convertView = LayoutInflater.from(mActivity).inflate(R.layout.disposable_user_listitem, null);
                vh = new ViewHolder();
                vh.rly_disposable_user = (RelativeLayout) convertView.findViewById(R.id.rly_disposable_user);
                vh.rly_time_detail = (RelativeLayout) convertView.findViewById(R.id.rly_time_detail);
                vh.img_down_up = (ImageView) convertView.findViewById(R.id.img_down_up);
                vh.img_shadow_head = (ImageView) convertView.findViewById(R.id.img_shadow_head);
                vh.img_shadow_tail = (ImageView) convertView.findViewById(R.id.img_shadow_tail);
                vh.tv_disposable_user_name = (TextView) convertView.findViewById(R.id.tv_disposable_user_name);
                vh.tv_disposable_user_pw = (TextView) convertView.findViewById(R.id.tv_disposable_user_pw);
                vh.tv_times = (TextView) convertView.findViewById(R.id.tv_times);
                vh.tv_divide = (TextView) convertView.findViewById(R.id.tv_divide);

            } else {
                vh = (ViewHolder) convertView.getTag();
            }
            convertView.setTag(vh);
            convertView.setClickable(true);

//            vh.tv_disposable_user_name.setText("用户" + (position));

            if (getItem(position).getType().toString().equals("once")){
                user_type="单次";
            }else  if (getItem(position).getType().toString().equals("day")){
                user_type="一天";
            }else if (getItem(position).getType().toString().equals("week")){
                user_type="一星期";
            }else  if (getItem(position).getType().toString().equals("month")){
                user_type="一个月";
            }

//            vh.tv_disposable_user_name.setText("期限用户"+Integer.parseInt(getItem(position).get("user_id").toString())+"(" + user_type+")");
            vh.tv_disposable_user_name.setText("期限用户"+Integer.parseInt(getItem(position).getUser_id().toString())+"(" + getItem(position).getCn_time().toString()+")");

            vh.tv_disposable_user_pw.setText(getItem(position).getPassword().toString());
            vh.tv_times.setText( getItem(position).getCn_time().toString());

            final ViewHolder finalVh = vh;
            vh.rly_disposable_user.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
//                    Log.e("url","is_open=="+getItem(position).getIs_open().toString());
//                    if (getItem(position).getType().toString().equals("once")){
//                        user_type="单次";
//                    }else  if (getItem(position).getType().toString().equals("day")){
//                        user_type="一天";
//                    }else if (getItem(position).getType().toString().equals("week")){
//                        user_type="一星期";
//                    }else  if (getItem(position).getType().toString().equals("month")){
//                        user_type="一个月";
//                    }
//                    finalVh.tv_disposable_user_name.setText("期限用户"+Integer.parseInt(getItem(position).getUser_id().toString())+"(" +  getItem(position).getCn_time().toString()+")");
                    if (getItem(position).getIs_open().toString().equals("true")) {
                        getItem(position).setIs_open("false");

                        finalVh.img_down_up.setImageResource(R.mipmap.btn_down);
                        finalVh.tv_divide.setVisibility(View.VISIBLE);
                        finalVh.rly_time_detail.setVisibility(View.GONE);
                        finalVh.img_shadow_head.setVisibility(View.GONE);
                        finalVh.img_shadow_tail.setVisibility(View.GONE);
                    } else {
                        getItem(position).setIs_open("true");
                        finalVh.tv_divide.setVisibility(View.GONE);
                        finalVh.img_shadow_head.setVisibility(View.VISIBLE);
                        finalVh.img_shadow_tail.setVisibility(View.VISIBLE);
                        finalVh.img_down_up.setImageResource(R.mipmap.btn_up);
                        finalVh.rly_time_detail.setVisibility(View.VISIBLE);
                        Log.e("url", "password==" + getItem(position).getPassword().toString() +
                                "  type==" + getItem(position).getType().toString() +
                                "  user_id==" + getItem(position).getUser_id().toString() +
                                "  start_time==" + getItem(position).getStart_time().toString() +
                                "  end_time==" + getItem(position).getEnd_time().toString());

                    }

                    disposableUserAdapter.notifyDataSetChanged();
                }
            });

            return convertView;
        }

        private class ViewHolder {
            private RelativeLayout rly_disposable_user, rly_time_detail;
            private ImageView img_down_up, img_shadow_head, img_shadow_tail;
            private TextView tv_disposable_user_name, tv_disposable_user_pw, tv_times, tv_divide;
        }
    }

    /**
     * 检查一天、一周、一个月用户的end_time是否过期
     */
    private void CheckOutOfTime() {
        db = disposableUserSQLiteOpenHelper.getWritableDatabase();

        //游标查询每条数据
        Cursor cursor = db.query("Disposable_User_Table", null, "(type='day' or type='week' or type='month') and mac=? and end_time<>'0'", new String[]{BLEDetails.mac}, null, null, null);

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
     * 查询数据是否存在
     *
     * @param pw
     * @param type
     */
    private void AddDisposableUserTableData(String pw, String type) {
        db = disposableUserSQLiteOpenHelper.getWritableDatabase();

        //游标查询每条数据
        Cursor cursor = db.query("Disposable_User_Table", null, "mac=? and password=?", new String[]{BLEDetails.mac, pw}, null, null, null);

        if (cursor.moveToNext()) {
            int count = cursor.getInt(0);
            Log.e("url", "count==" + count);
            if (count > 0) {//数据存在
                Toast.makeText(mActivity, "该用户已经存在且是有效的", Toast.LENGTH_SHORT).show();
                Log.e("url", "该用户已经存在且是有效的！");
            }
            cursor.close();
        }else {
            String sql2 = "select user_id  FROM Disposable_User_Table where mac='" + BLEDetails.mac + "' and password ='bbbbbbbbbbbb'  AND  type='" + type + "' ORDER BY id ASC LIMIT 0,1";

            Cursor cursor2 = db.rawQuery(sql2, null);

            if (cursor2.moveToNext()) {
                int count = cursor2.getCount();
                Log.e("url", "count==" + count);
                int id_index = cursor2.getColumnCount();
                Log.e("url", "id_index==" + id_index);
                String user_id = cursor2.getString(0);
                Log.e("url", "user_id==" + user_id);

                UpdateBLEData(pw, type, user_id);//添加用户后更新蓝牙用户信息
            }
            cursor2.close();
            db.close();
        }


    }

    /**
     * 更新期限用户数据库
     *
     * @param pw
     * @param type
     * @param user_id
     */
    private void UpdateBLEData(String pw, String type, String user_id) {
        Log.e("url", "pw==" + pw);
        Log.e("url", "type==" + type);
        Log.e("url", "user_id==" + user_id);

        byte[] head = new byte[]{0x07};//固定的前面只是0x07,

        //0x31(单次),0x32(一天),0x33(一星期),0x34(一个月)
        byte[] time = new byte[0];//(次数)
        String str_time = null;
        if (type.equals("once")) {
            time = new byte[]{0x31};
            str_time = "31";
        } else if (type.equals("day")) {
            time = new byte[]{0x32};
            str_time = "32";
        } else if (type.equals("week")) {
            str_time = "33";
            time = new byte[]{0x33};
        } else if (type.equals("month")) {
            str_time = "34";
            time = new byte[]{0x34};
        }
        Log.e("url", "time==" + time);

        byte[] serial_num = DigitalTrans.hex2byte(user_id);//十六进制串转化为byte数组

        Log.e("url", "serial_num==" + serial_num);

        byte[] time_serial_num = DigitalTrans.byteMerger(time, serial_num);//把次数和序号加在一起
        String str_pw = DigitalTrans.str2HexStr(et_password.getText().toString());//字符串转换成十六进制字符串
        byte[] byte_pw = DigitalTrans.hex2byte(str_pw);//十六进制串转化为byte数组(序号)

        byte[] time_serial_num_pw = DigitalTrans.byteMerger(time_serial_num, byte_pw);//把次数和序号，密码加在一起

        short[] short_pw = new short[time_serial_num_pw.length];
        for (int i = 0; i < time_serial_num_pw.length; i++) {
            short_pw[i] = time_serial_num_pw[i];
            Log.e("url", "time_serial_num_pw==" + time_serial_num_pw[i]);
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
        Log.e("url", "crc==" + crc);//(111111  28)  (123456 ec)

        byte[] data1 = DigitalTrans.byteMerger(head, time_serial_num_pw);//把头部和密码加在一起
        byte[] data = DigitalTrans.byteMerger(data1, crc);//把头部和密码和crc加在一起
        byte[] add_data=DigitalTrans.byteMerger(data, Const.add6);//补全16位

        Log.e("url", "add_data==" + add_data.length);
        byte[] encrypt_add_data= null;
        //加密
        try {
            encrypt_add_data= AesEntryDetry.encrypt(add_data);//加密
        } catch (Exception e) {
            e.printStackTrace();
        }

        String wait_send_data = "07" + str_time + user_id + str_pw + str_crc+"000000000000";
        Log.e("url", "wait_send_data==" + wait_send_data);
        Log.e("url", "wait_send_data.length==" + wait_send_data.length());

        BleService.getInstance().writeCharacteristic(sharedPreferencesUtil.getService(), sharedPreferencesUtil.getCharFfe3(), encrypt_add_data);
        BleService.getInstance().setCharacteristicNotification(sharedPreferencesUtil.getService(), sharedPreferencesUtil.getCharFfe2(), true);

        if (BleService.ble_connect.equals("disconnect")) {
            SaveWaitSendDataToTable(wait_send_data, sharedPreferencesUtil.getCharFfe3(), sharedPreferencesUtil.getCharFfe2());//把未发送成功的数据存储到表中
            Toast.makeText(mActivity, "添加期限用户密码成功！", Toast.LENGTH_SHORT).show();
            UpdateTableData(pw, type, user_id);
            pop_add.dismiss();
        }else {

//            try {
//                Thread.sleep(500);
//            } catch (InterruptedException e) {
//                e.printStackTrace();
//            }

            synchronized (this) {
                try {
                    wait(500);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }

            String back_data = BleService.getInstance().back_data;

            if (back_data!=null&&back_data.length() == 32) {
                //094b1600ff913e03000d000400122c00
                String add_disposable_user_back_data = back_data.substring(back_data.length() - 32, back_data.length() - 28);//094b
                Log.e("url", "add_disposable_user_back_data==" + add_disposable_user_back_data);
                if (add_disposable_user_back_data.equals("094b")) {
                    Toast.makeText(mActivity, "添加期限用户密码成功！", Toast.LENGTH_SHORT).show();
                    UpdateTableData(pw, type, user_id);
                    pop_add.dismiss();
                    disposableUserData = getData();
                    Log.e("url", "disposableUserData==" + disposableUserData.size());

                } else if (add_disposable_user_back_data.equals("56ff")) {
                    Toast.makeText(mActivity, "添加期限用户密码失败，重新设置！", Toast.LENGTH_SHORT).show();
                }
            }
        }
        MainActivity.is_scan=true;
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

    //添加成功后给数据库添加数据
    private void UpdateTableData(String pw, String type, String user_id) {
        db = disposableUserSQLiteOpenHelper.getWritableDatabase();
        ContentValues values = new ContentValues();

//        //游标查询每条数据
        Cursor cursor = db.query("Disposable_User_Table", null, "mac=? and type=? AND user_id=?", new String[]{BLEDetails.mac, type, user_id}, null, null, null);

//        Cursor cursor = db.rawQuery(sql2, null);
        if (cursor.moveToNext()) {
            int count = cursor.getInt(0);
            String times = cursor.getString(3);
            Log.e("url", "times==" + times);
            if (count > 0) {//数据存在
                if (times.equals("day")) {//一天用户
                    // 推迟日期
                    SimpleDateFormat format = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
                    Date current_date = new Date();//取时间
                    Calendar calendar = Calendar.getInstance();
                    String current_time;//当前时间
                    String late_time;//推迟后的时间
                    Date late_date;
                    calendar.set(Calendar.DAY_OF_MONTH, calendar.get(Calendar.DAY_OF_MONTH) + 1);//时间推迟一天
                    late_date = calendar.getTime();
                    current_time = format.format(current_date);
                    late_time = format.format(late_date);
                    Log.e("url", "当前时间==" + current_time);
                    Log.e("url", "推迟的时间==" + late_time);

                    values.put("password", pw);
                    values.put("start_time", current_time);
                    values.put("end_time", late_time);
                    db.update("Disposable_User_Table", values, "mac=? and type=? AND user_id=?", new String[]{BLEDetails.mac, type, user_id});

                } else if (times.equals("week")) {//一周用户
                    // 推迟日期
                    SimpleDateFormat format = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
                    Date current_date = new Date();//取时间
                    Calendar calendar = Calendar.getInstance();
                    String current_time;//当前时间
                    String late_time;//推迟后的时间
                    Date late_date;
                    calendar.set(Calendar.DAY_OF_MONTH, calendar.get(Calendar.DAY_OF_MONTH) + 7);//时间推迟一周
                    late_date = calendar.getTime();
                    current_time = format.format(current_date);
                    late_time = format.format(late_date);
                    Log.e("url", "当前时间==" + current_time);
                    Log.e("url", "推迟的时间==" + late_time);

                    values.put("password", pw);
                    values.put("start_time", current_time);
                    values.put("end_time", late_time);
                    db.update("Disposable_User_Table", values, "mac=? and type=? AND user_id=?", new String[]{BLEDetails.mac, type, user_id});

                } else if (times.equals("month")) {//一个月用户
                    // 推迟日期
                    SimpleDateFormat format = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
                    Date current_date = new Date();//取时间
                    Calendar calendar = Calendar.getInstance();
                    String current_time;//当前时间
                    String late_time;//推迟后的时间
                    Date late_date;
                    calendar.set(Calendar.MONTH, calendar.get(Calendar.MONTH) + 1);//时间推迟一个月
                    late_date = calendar.getTime();
                    current_time = format.format(current_date);
                    late_time = format.format(late_date);
                    Log.e("url", "当前时间==" + current_time);
                    Log.e("url", "推迟的时间==" + late_time);

                    values.put("password", pw);
                    values.put("start_time", current_time);
                    values.put("end_time", late_time);
                    db.update("Disposable_User_Table", values, "mac=? and type=? AND user_id=?", new String[]{BLEDetails.mac, type, user_id});

                } else {//单次用户
                    values.put("password", pw);
                    db.update("Disposable_User_Table", values, "mac=? and type=? AND user_id=?", new String[]{BLEDetails.mac, type, user_id});
                }

            }
        }
        disposableUserData = getData();
        disposableUserAdapter.notifyDataSetChanged();

//        for (int i = 0; i < disposableUserData.size(); i++) {
//            Log.e("url", "id==" + disposableUserData.get(i).get("id").toString() +
//                    "  password==" + disposableUserData.get(i).get("password").toString() +
//                    "  type==" + disposableUserData.get(i).get("type").toString() +
//                    "  user_id==" + disposableUserData.get(i).get("user_id").toString() +
//                    "  start_time==" + disposableUserData.get(i).get("start_time").toString() +
//                    "  end_time==" + disposableUserData.get(i).get("end_time").toString());
//        }

        for (int i = 0; i < disposableUserData.size(); i++) {
            Log.e("url", "id==" + disposableUserData.get(i).getId().toString() +
                    "  password==" + disposableUserData.get(i).getPassword().toString() +
                    "  type==" + disposableUserData.get(i).getType().toString() +
                    "  user_id==" + disposableUserData.get(i).getUser_id().toString() +
                    "  start_time==" + disposableUserData.get(i).getStart_time().toString() +
                    "  end_time==" + disposableUserData.get(i).getEnd_time().toString());
        }

        cursor.close();
        db.close();
    }

    //加载数据库的数据到List中
    private List<DisposableDataInfo> getData() {
        db = disposableUserSQLiteOpenHelper.getWritableDatabase();
        //游标查询每条数据
        Cursor cursor = db.query("Disposable_User_Table", null, " (type='once' or type='day' or type='week' or type='month') and mac=? ", new String[]{BLEDetails.mac}, null, null, null);
        //定义list存储数据
        List<DisposableDataInfo> list = new ArrayList<DisposableDataInfo>();

        //读取数据 游标移动到下一行
        while (cursor.moveToNext()) {

            String password = cursor.getString(2);//password列
            if (!(password.equals("bbbbbbbbbbbb") | password.equals("0") | password.contains("b"))) {
                DisposableDataInfo disposableDataInfo = new DisposableDataInfo();
                disposableDataInfo.setId(cursor.getString(cursor.getColumnIndex("id")));
                disposableDataInfo.setPassword(cursor.getString(cursor.getColumnIndex("password")));
                disposableDataInfo.setType(cursor.getString(cursor.getColumnIndex("type")));
                disposableDataInfo.setUser_id(cursor.getString(cursor.getColumnIndex("user_id")));
                disposableDataInfo.setStart_time(cursor.getString(cursor.getColumnIndex("start_time")));
                disposableDataInfo.setEnd_time(cursor.getString(cursor.getColumnIndex("end_time")));
                disposableDataInfo.setStr_user(cursor.getString(cursor.getColumnIndex("str_user")));
                disposableDataInfo.setIs_open("false");
                if (cursor.getString(cursor.getColumnIndex("type")).toString().equals("once")){
                    disposableDataInfo.setCn_time("单次");
                }else  if (cursor.getString(cursor.getColumnIndex("type")).toString().equals("day")){
                    disposableDataInfo.setCn_time("一天");
                }else if (cursor.getString(cursor.getColumnIndex("type")).toString().equals("week")){
                    disposableDataInfo.setCn_time("一星期");
                }else  if (cursor.getString(cursor.getColumnIndex("type")).toString().equals("month")){
                    disposableDataInfo.setCn_time("一个月");
                }


//                map.put("id", cursor.getString(cursor.getColumnIndex("id")));
//                map.put("password", cursor.getString(cursor.getColumnIndex("password")));
//                map.put("type", cursor.getString(cursor.getColumnIndex("type")));
//                map.put("user_id", cursor.getString(cursor.getColumnIndex("user_id")));
//                map.put("start_time", cursor.getString(cursor.getColumnIndex("start_time")));
//                map.put("end_time", cursor.getString(cursor.getColumnIndex("end_time")));
//                map.put("str_user", cursor.getString(cursor.getColumnIndex("str_user")));
//                map.put("is_open","false");
                list.add(disposableDataInfo);
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

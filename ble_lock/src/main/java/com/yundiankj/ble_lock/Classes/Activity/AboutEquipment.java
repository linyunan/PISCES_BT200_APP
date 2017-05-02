package com.yundiankj.ble_lock.Classes.Activity;

import android.app.Activity;
import android.content.ContentValues;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.graphics.drawable.BitmapDrawable;
import android.os.Bundle;
import android.text.Editable;
import android.text.Selection;
import android.text.TextWatcher;
import android.util.Log;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.PopupWindow;
import android.widget.TextView;
import android.widget.Toast;

import com.yundiankj.ble_lock.R;
import com.yundiankj.ble_lock.Resource.SQLite.BleSQLiteOpenHelper;
import com.yundiankj.ble_lock.Resource.SharedPreferencesUtil;
import com.yundiankj.ble_lock.Resource.StatusBarUtil;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Created by hong on 2016/7/20.
 *
 * 关于设备
 */
public class AboutEquipment extends Activity{

    private Activity mActivity;
    private SharedPreferencesUtil sharedPreferencesUtil;
    private ImageView img_back;
    private TextView tv_equipment_name,tv_product_serial_number,tv_product_type;

    //修改设备名称的popwindow
    private PopupWindow pop_edit_ble_name;
    private EditText et_ble_name;
    private TextView tv_ok;

    private BleSQLiteOpenHelper bleSQLiteOpenHelper;    //蓝牙列表
    private SQLiteDatabase ble_db;

    private String mac;
    private List<Map<String, Object>> bleData;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.about_equipment);

        mActivity=this;
        initWindow();//沉浸式状态栏
        init();
    }

    private void initWindow() {
        StatusBarUtil.setColor(mActivity, getResources().getColor(R.color.A04a8ec), 0);
    }

    private void init(){

        if (this.getIntent().getExtras()!=null){
            mac=this.getIntent().getExtras().getString("mac");
        }

        bleSQLiteOpenHelper = new BleSQLiteOpenHelper(mActivity, "BleDatabase.db", null, 2);
        bleSQLiteOpenHelper.getWritableDatabase();//创建连接过的蓝牙表

        bleData = getData(mac);

        sharedPreferencesUtil=SharedPreferencesUtil.getInstance(mActivity);
        img_back= (ImageView) mActivity.findViewById(R.id.img_back);
        tv_equipment_name= (TextView) mActivity.findViewById(R.id.tv_equipment_name);
        tv_product_serial_number= (TextView) mActivity.findViewById(R.id.tv_product_serial_number);
        tv_product_type= (TextView) mActivity.findViewById(R.id.tv_product_type);

        tv_equipment_name.setText(bleData.get(0).get("ble_name").toString());
        tv_product_serial_number.setText(bleData.get(0).get("mac").toString());
        if (bleData.get(0).get("type").toString().equals("door")){
            tv_product_type.setText("家用门锁");
        }else {
            tv_product_type.setText("挂锁");
        }

        img_back.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                finish();
            }
        });

        tv_equipment_name.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                initEditBleNamePopupWindow();

                WindowManager.LayoutParams lp;
                //设置背景颜色变暗
                lp = mActivity.getWindow().getAttributes();
                lp.alpha = 0.5f;
                mActivity.getWindow().setAttributes(lp);
                pop_edit_ble_name.setAnimationStyle(R.style.PopupAnimation);
                pop_edit_ble_name.showAtLocation(mActivity.findViewById(R.id.lly_main), Gravity.CENTER | Gravity.CENTER_HORIZONTAL, 0, 0);
                pop_edit_ble_name.update();
            }
        });
    }

    /**
     * 加载数据库的数据到List中(获取蓝牙信息)
     * @param mac
     * @return
     */
    private List<Map<String, Object>> getData(String mac) {
        ble_db = bleSQLiteOpenHelper.getWritableDatabase();
        //游标查询每条数据
        Cursor cursor = ble_db.query("Ble_Table", null, "mac=?", new String[]{mac}, null, null, null);

        //定义list存储数据
        List<Map<String, Object>> list = new ArrayList<Map<String, Object>>();

        //读取数据 游标移动到下一行
        while (cursor.moveToNext()) {
            Map<String, Object> map = new HashMap<String, Object>();
            map.put("id", cursor.getString(cursor.getColumnIndex("id")));
            map.put("ble_name", cursor.getString(cursor.getColumnIndex("ble_name")));
            map.put("mac", cursor.getString(cursor.getColumnIndex("mac")));
            map.put("type", cursor.getString(cursor.getColumnIndex("type")));
            map.put("electricity", cursor.getString(cursor.getColumnIndex("electricity")));
            map.put("admin_id", cursor.getString(cursor.getColumnIndex("admin_id")));
            map.put("admin_type", cursor.getString(cursor.getColumnIndex("admin_type")));
            map.put("admin_phone", cursor.getString(cursor.getColumnIndex("admin_phone")));
            map.put("connect_music", cursor.getString(cursor.getColumnIndex("connect_music")));
            map.put("unlock_music", cursor.getString(cursor.getColumnIndex("unlock_music")));
            map.put("connect_music_on_off", cursor.getString(cursor.getColumnIndex("connect_music_on_off")));
            map.put("unlock_music_on_off", cursor.getString(cursor.getColumnIndex("unlock_music_on_off")));

            list.add(map);
        }
        cursor.close();
        ble_db.close();
        return list;
    }

    /**
     * 修改设备名称的popupwindow
     */
    private void initEditBleNamePopupWindow() {
        LayoutInflater inflater = LayoutInflater.from(mActivity);
        View edit_ble_name_layout = inflater.inflate(R.layout.pop_edit_ble_name, null);
        et_ble_name = (EditText) edit_ble_name_layout.findViewById(R.id.et_ble_name);
        tv_ok = (TextView) edit_ble_name_layout.findViewById(R.id.tv_ok);
        edit_ble_name_layout.invalidate();
        pop_edit_ble_name = new PopupWindow(edit_ble_name_layout, ViewGroup.LayoutParams.FILL_PARENT, ViewGroup.LayoutParams.FILL_PARENT);
        pop_edit_ble_name.setBackgroundDrawable(new BitmapDrawable());
        pop_edit_ble_name.setOutsideTouchable(true);
        pop_edit_ble_name.setFocusable(true);

        Editable etext = et_ble_name.getText();
        Selection.setSelection(etext, etext.length());//让光标在文字后面

        pop_edit_ble_name.setOnDismissListener(new PopupWindow.OnDismissListener() {
            @Override
            public void onDismiss() {
                WindowManager.LayoutParams lp = mActivity.getWindow().getAttributes();
                lp.alpha = 1;
                mActivity.getWindow().setAttributes(lp);
            }
        });

        et_ble_name.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {

            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                if (s.length()>=7){
                    Toast.makeText(mActivity,"设备名称长度不得超过7个字符！",Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void afterTextChanged(Editable s) {

            }
        });

        tv_ok.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (et_ble_name.getText().length()==0){
                    Toast.makeText(mActivity,"设备名称不能为空！",Toast.LENGTH_SHORT).show();
                }else {
                    pop_edit_ble_name.dismiss();
                    tv_equipment_name.setText(et_ble_name.getText().toString());
                    sharedPreferencesUtil.setBLENAME(et_ble_name.getText().toString());
                    Log.e("url","修改名称的mac=="+sharedPreferencesUtil.getMAC());
                    UpdateBleData(sharedPreferencesUtil.getMAC(),et_ble_name.getText().toString());
                }

            }
        });
    }

    /**
     * 添加完手机管理员后，更新数据库
     * @param mac
     * @param ble_name
     */
    private void UpdateBleData(String mac,String ble_name) {
        Log.e("url", "mac==" + mac );
        ble_db = bleSQLiteOpenHelper.getWritableDatabase();
        ContentValues values = new ContentValues();

        //游标查询每条数据
        Cursor cursor = ble_db.query("Ble_Table", null, "mac=?", new String[]{mac}, null, null, null);

        if (cursor.moveToNext()) {
            int count = cursor.getInt(0);
            if (count > 0) {//数据存在
                values.put("ble_name",ble_name );
                ble_db.update("Ble_Table", values, "mac=?", new String[]{mac});
            }
        }
        cursor.close();
        ble_db.close();
    }

    @Override
    protected void onResume() {
        super.onResume();
        MainActivity.is_scan = true;//停止扫描
    }
}

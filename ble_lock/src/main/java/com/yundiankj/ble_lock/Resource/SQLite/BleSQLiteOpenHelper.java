package com.yundiankj.ble_lock.Resource.SQLite;

import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.widget.Toast;

/**
 * Created by hong on 2016/7/27.
 * <p/>
 * 连接过的蓝牙表
 * <p/>
 * 0  id (自增变量)
 * 1  ble_name（蓝牙名称）
 * 2  mac (蓝牙的序列号)
 * 3  type (蓝牙类型 ：door门锁，hand挂锁)
 * 4  electricity (电量)
 * 5  unlock_method(开锁方式：自动开锁（auto_unlock）,点亮屏幕开锁（light_screen）)
 * 6  unlock_distance（安全距离）
 * 7  admin_id ( 用户手机的id，00,01,02,03.....)
 * 8  admin_type(管理员:10、普通用户:20)
 * 9  admin_phone （用户手机）
 * 10  connect_music (连接提示音：无、0   铃声1：1   铃声2:2 )
 * 11  unlock_music (开锁提示音：无、0   铃声1：1   )
 * 12  connect_music_on_off (连接提示音的开关：开：on  关：off)
 * 13  unlock_music_on_off (开锁提示音的开关：开：on  关：off)
 * 14  service （服务的uuid）
 * 15  char_ffe1 （特征值ffe1的uuid）
 * 16  char_ffe2（特征值ffe2的uuid）
 * 17  char_ffe3（特征值ffe3的uuid）
 * 18 is_invalid (手机普通用户是否有效)
 */
public class BleSQLiteOpenHelper extends SQLiteOpenHelper {

    public Context mContext;

    public static final String createTableStu = "create table Ble_Table (" +
            "id integer primary key AUTOINCREMENT, " +
            "ble_name text, " +
            "mac text, " +
            "type text, " +
            "electricity text, " +
            "unlock_method text, " +
            "unlock_distance text, " +
            "admin_id text, " +
            "admin_type text, " +
            "admin_phone text, " +
            "connect_music text, " +
            "unlock_music text," +
            "connect_music_on_off text, " +
            "unlock_music_on_off text,"+
            "service text, "+
            "char_ffe1 text, "+
            "char_ffe2 text, "+
            "char_ffe3 text, "+
            "is_invalid text)";

    //抽象类 必须定义显示的构造函数 重写方法
    public BleSQLiteOpenHelper(Context context, String name, SQLiteDatabase.CursorFactory factory,
                               int version) {
        super(context, name, factory, version);
        mContext = context;
    }

    @Override
    public void onCreate(SQLiteDatabase arg0) {
        // TODO Auto-generated method stub
        arg0.execSQL(createTableStu);
//        Toast.makeText(mContext, "Created", Toast.LENGTH_SHORT).show();
    }

    @Override
    public void onUpgrade(SQLiteDatabase arg0, int arg1, int arg2) {
        // TODO Auto-generated method stub
        arg0.execSQL("drop table if exists Ble_Table");
        onCreate(arg0);
        Toast.makeText(mContext, "Upgraged", Toast.LENGTH_SHORT).show();
    }
}

package com.yundiankj.ble_lock.Resource.SQLite;

import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.widget.Toast;

/**
 * Created by hong on 2016/7/26.
 *
 * 手机用户表
 *
 *  id (自增)
 *  mac （mac地址）
 *  user_type(20：普通用户)
 *  user_id (手机用户（0#，1#。。）01,02,03.。。）
 *  user_num (手机号码）
 *  str_user (蓝牙返回数据，方便删除数据)
 */
public class PhoneUserSQLiteOpenHelper extends SQLiteOpenHelper {

    public Context mContext;

    public static final String createTableStu = "create table Phone_User_Table (" +
            "id integer primary key AUTOINCREMENT, " +
            "mac text, " +
            "user_type text, " +
            "user_id text, " +
            "user_num text, " +
            "str_user text)";

    //抽象类 必须定义显示的构造函数 重写方法
    public PhoneUserSQLiteOpenHelper(Context context, String name, SQLiteDatabase.CursorFactory factory,
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
        arg0.execSQL("drop table if exists Phone_User_Table");
        onCreate(arg0);
        Toast.makeText(mContext, "Upgraged", Toast.LENGTH_SHORT).show();
    }
}

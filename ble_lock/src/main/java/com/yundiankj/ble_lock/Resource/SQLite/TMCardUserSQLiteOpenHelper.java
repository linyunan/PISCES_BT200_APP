package com.yundiankj.ble_lock.Resource.SQLite;

import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.widget.Toast;

/**
 * Created by hong on 2016/7/26.
 *
 * TM卡用户表
 *
 *  id (自增)
 *  mac（mac地址）
 *  user_id (0,1,2。。）
 *  user_num (TM卡用户号码）
 *
 */
public class TMCardUserSQLiteOpenHelper extends SQLiteOpenHelper {

    public Context mContext;

    public static final String createTableStu = "create table TM_Card_User_Table (" +
            "id integer primary key AUTOINCREMENT, " +
            "mac text, " +
            "user_id text, " +
            "user_num text)";

    //抽象类 必须定义显示的构造函数 重写方法
    public TMCardUserSQLiteOpenHelper(Context context, String name, SQLiteDatabase.CursorFactory factory,
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
        arg0.execSQL("drop table if exists TM_Card_User_Table");
        onCreate(arg0);
        Toast.makeText(mContext, "Upgraged", Toast.LENGTH_SHORT).show();
    }
}

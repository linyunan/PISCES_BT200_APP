package com.yundiankj.ble_lock.Resource.SQLite;

import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.widget.Toast;

/**
 * Created by hong on 2016/7/26.
 *
 * 开锁记录表
 *   id (第几组开门记录)
 *   mac（mac地址）
 *   user_type (用户类型：1、普通永久用户（密码）  2、手机  3、TM卡  4、单次  5、一天  6、一星期 7、一个月)
 *   user_type_num (用户类型中的第几个用户)
 *   year (年)
 *   month (月)
 *   day (日)
 *   hour (时)
 *   minute (分)
 *   second （秒）
 *
 *   */
public class UnlockRecordSQLiteOpenHelper extends SQLiteOpenHelper {

    public Context mContext;

    //创建开锁记录表
    public static final String createTableStu = "create table Unlock_Record_Table (" +
            "id integer primary key AUTOINCREMENT, " +
            "mac text, " +
            "num text, " +
            "user_type text, " +
            "user_type_num text, " +
            "year_month text, " +
            "year text, " +
            "month text, " +
            "day text, " +
            "hour text, " +
            "minute text, " +
            "second text)";

    //抽象类 必须定义显示的构造函数 重写方法
    public UnlockRecordSQLiteOpenHelper(Context context, String name, SQLiteDatabase.CursorFactory factory,
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
        arg0.execSQL("drop table if exists Unlock_Record_Table");
        onCreate(arg0);
        Toast.makeText(mContext, "Upgraged", Toast.LENGTH_SHORT).show();
    }
}

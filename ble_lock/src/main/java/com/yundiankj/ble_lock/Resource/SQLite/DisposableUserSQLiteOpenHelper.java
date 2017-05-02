package com.yundiankj.ble_lock.Resource.SQLite;

import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.widget.Toast;

/**
 * Created by hong on 2016/7/27.
 *
 * 键盘用户表（管理员用户、普通用户、一次性用户）
 *
 * 1  id (自增)
 * 2  mac （mac地址）
 * 3  password(密码)
 * 4  type(1、管理员（admin）  2、普通用户（ordinary） 3、一次性用户（1.单次（once），2.一天(day)，3.一星期(week)，4.一个月(month)）)
 * 5  user_id （管理员（9999）、普通用户（00,01，...09）、一次性用户（00,01...09））
 * 6  start_time(起始时间)
 * 7  end_time(结束时间)）
 * 8  str_user (蓝牙返回数据，方便删除数据)
 *
 */
public class DisposableUserSQLiteOpenHelper extends SQLiteOpenHelper {

    public Context mContext;

    public static final String createTableStu = "create table Disposable_User_Table (" +
            "id integer primary key AUTOINCREMENT, " +
            "mac text, " +
            "password text, " +
            "type text, " +
            "user_id text, " +
            "start_time text, " +
            "end_time text, " +
            "str_user text)";

    //抽象类 必须定义显示的构造函数 重写方法
    public DisposableUserSQLiteOpenHelper(Context context, String name, SQLiteDatabase.CursorFactory factory,
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
        arg0.execSQL("drop table if exists Disposable_User_Table");
        onCreate(arg0);
        Toast.makeText(mContext, "Upgraged", Toast.LENGTH_SHORT).show();
    }
}

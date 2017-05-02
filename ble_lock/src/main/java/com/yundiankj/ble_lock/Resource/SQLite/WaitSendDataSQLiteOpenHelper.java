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
 * 2  mac（mac地址）
 * 3  data(未发送出去的数据)
 * 4  write_char (写的特征值)
 * 5  setnotice_char (通知的特征值)
 * 6  is_0A (判断是否是0a)
 *
 */
public class WaitSendDataSQLiteOpenHelper extends SQLiteOpenHelper {

    public Context mContext;

    public static final String createTableStu = "create table WaitSendData_Table (" +
            "id integer primary key AUTOINCREMENT, " +
            "mac text, " +
            "data text, " +
            "write_char text, " +
            "setnotice_char text, " +
            "is_0A text)";

    //抽象类 必须定义显示的构造函数 重写方法
    public WaitSendDataSQLiteOpenHelper(Context context, String name, SQLiteDatabase.CursorFactory factory,
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
        arg0.execSQL("drop table if exists WaitSendData_Table");
        onCreate(arg0);
        Toast.makeText(mContext, "Upgraged", Toast.LENGTH_SHORT).show();
    }
}

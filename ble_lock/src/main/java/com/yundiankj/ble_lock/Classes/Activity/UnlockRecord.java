package com.yundiankj.ble_lock.Classes.Activity;

import android.app.Activity;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseExpandableListAdapter;
import android.widget.ExpandableListView;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.TextView;

import com.yundiankj.ble_lock.Classes.Model.UnlockRecordEntityClass;
import com.yundiankj.ble_lock.R;
import com.yundiankj.ble_lock.Resource.SQLite.UnlockRecordSQLiteOpenHelper;
import com.yundiankj.ble_lock.Resource.StatusBarUtil;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by hong on 2016/7/20.
 * <p>
 * 开锁记录
 */
public class UnlockRecord extends Activity {

    private Activity mActivity;
    private ImageView img_back;
    private ExpandableListView exlv_unlock_record;
    private UnlockRecordAdapter unlockRecordAdapter;

    private UnlockRecordSQLiteOpenHelper unlockRecordSQLiteOpenHelper;//开锁记录表
    private SQLiteDatabase db;

    //定义一个列表储存 开锁记录数据
    private UnlockRecordEntityClass unlockRecordEntityClass;//实体类
    private List<UnlockRecordEntityClass> unlockRecordData;//所有数据的集合
    private List<UnlockRecordEntityClass> groupListData;// 头容器的集合
    private List<List<UnlockRecordEntityClass>> childListData;//子容器集合

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.unlock_record);

        mActivity = this;
        initWindow();//沉浸式状态栏
        init();
    }

    private void initWindow() {
        StatusBarUtil.setColor(mActivity, getResources().getColor(R.color.A04a8ec), 0);
    }

    private void init() {

        unlockRecordSQLiteOpenHelper = new UnlockRecordSQLiteOpenHelper(this, "UnlockRecordDatabase.db", null, 2);
        unlockRecordSQLiteOpenHelper.getWritableDatabase();//创建开锁记录表
        img_back = (ImageView) mActivity.findViewById(R.id.img_back);
        exlv_unlock_record = (ExpandableListView) mActivity.findViewById(R.id.exlv_unlock_record);
        exlv_unlock_record.setGroupIndicator(null);

        groupListData = new ArrayList<>();
        childListData = new ArrayList<>();

        unlockRecordData = getData();
        Log.e("url", "unlockRecordData.size==" + unlockRecordData.size());
        for (int i = 0; i < unlockRecordData.size(); i++) {
            Log.e("url", "unlockRecordData:   id===" + unlockRecordData.get(i).getId() +
                    "   mac==" + unlockRecordData.get(i).getMac() +
                    "   num==" + unlockRecordData.get(i).getNum() +
                    "   user_type==" + unlockRecordData.get(i).getUser_type() +
                    "  user_type_num==" + unlockRecordData.get(i).getUser_type_num() +
                    "  year_month==" + unlockRecordData.get(i).getYear_month() +
                    "  year==" + unlockRecordData.get(i).getYear() +
                    "  month==" + unlockRecordData.get(i).getMonth() +
                    "  day==" + unlockRecordData.get(i).getDay() +
                    "   hour==" + unlockRecordData.get(i).getHour() +
                    "  minute==" + unlockRecordData.get(i).getMinute() +
                    "  second==" + unlockRecordData.get(i).getSecond());
        }

        /**
         * 初始化数据
         */
        getGroupsFromList(unlockRecordData);
        getChildList(unlockRecordData);

        Log.e("url", "groupListData.size==" + groupListData.size());
        Log.e("url", "childListData.size==" + childListData.size());
        for (int i = 0; i < groupListData.size(); i++) {
            Log.e("url", "group_list===" + groupListData.get(i).getYear_month());
        }

        unlockRecordAdapter = new UnlockRecordAdapter();
        exlv_unlock_record.setAdapter(unlockRecordAdapter);

        img_back.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                finish();
            }
        });
    }

    /**
     * 加载数据库的数据到List中
     *
     * @return
     */
    private List<UnlockRecordEntityClass> getData() {
        db = unlockRecordSQLiteOpenHelper.getWritableDatabase();
        //游标查询每条数据
        Cursor cursor = db.query("Unlock_Record_Table", null, "mac=?", new String[]{BLEDetails.mac}, null, null, null);
        //定义list存储数据
        List<UnlockRecordEntityClass> list = new ArrayList<UnlockRecordEntityClass>();

        //读取数据 游标移动到下一行
        while (cursor.moveToNext()) {
            unlockRecordEntityClass = new UnlockRecordEntityClass();
            unlockRecordEntityClass.setId(cursor.getString(cursor.getColumnIndex("id")));
            unlockRecordEntityClass.setMac(cursor.getString(cursor.getColumnIndex("mac")));
            unlockRecordEntityClass.setNum(cursor.getString(cursor.getColumnIndex("num")));
            unlockRecordEntityClass.setUser_type(cursor.getString(cursor.getColumnIndex("user_type")));
            unlockRecordEntityClass.setUser_type_num(cursor.getString(cursor.getColumnIndex("user_type_num")));
            unlockRecordEntityClass.setYear_month(cursor.getString(cursor.getColumnIndex("year_month")));
            unlockRecordEntityClass.setYear(cursor.getString(cursor.getColumnIndex("year")));
            unlockRecordEntityClass.setMonth(cursor.getString(cursor.getColumnIndex("month")));
            unlockRecordEntityClass.setDay(cursor.getString(cursor.getColumnIndex("day")));
            unlockRecordEntityClass.setHour(cursor.getString(cursor.getColumnIndex("hour")));
            unlockRecordEntityClass.setMinute(cursor.getString(cursor.getColumnIndex("minute")));
            unlockRecordEntityClass.setSecond(cursor.getString(cursor.getColumnIndex("second")));
            list.add(unlockRecordEntityClass);
        }
        cursor.close();
        db.close();
        return list;
    }

    //头容器数据
    public void getGroupsFromList(List<UnlockRecordEntityClass> unlockRecordData) {
        for (int i = 0; i < unlockRecordData.size(); i++) {
            boolean flag = false;
            UnlockRecordEntityClass totalData = unlockRecordData.get(i);
            if (groupListData.size() > 0) {
                for (int j = 0; j < groupListData.size(); j++) {
                    UnlockRecordEntityClass groopUser = groupListData.get(j);
                    if (totalData.getYear_month().equals(groopUser.getYear_month())) {
                        flag = true;
                    }
                }
                if (flag == false) {
                    groupListData.add(totalData);
                }
            } else {
                groupListData.add(totalData);
            }
        }
    }

    //子容器数据
    public void getChildList(List<UnlockRecordEntityClass> unlockRecordData) {
        for (int i = 0; i < groupListData.size(); i++) {
            UnlockRecordEntityClass groopUser = groupListData.get(i);
            List<UnlockRecordEntityClass> item = new ArrayList<UnlockRecordEntityClass>();
            for (int j = 0; j < unlockRecordData.size(); j++) {
                UnlockRecordEntityClass totalUser = unlockRecordData.get(j);
                if (totalUser.getYear_month().equals(groopUser.getYear_month())) {
                    item.add(totalUser);
                }
            }
            childListData.add(item);
        }
    }

    /**
     * expandableListView适配器
     */
    public class UnlockRecordAdapter extends BaseExpandableListAdapter {

        @Override
        public int getGroupCount() {
            return groupListData.size();
        }

        @Override
        public int getChildrenCount(int groupPosition) {
            return childListData.get(groupPosition).size();
        }

        @Override
        public Object getGroup(int groupPosition) {
            return groupListData.get(groupPosition);
        }

        @Override
        public Object getChild(int groupPosition, int childPosition) {
            return childListData.get(childPosition);
        }

        @Override
        public long getGroupId(int groupPosition) {
            return groupPosition;
        }

        @Override
        public long getChildId(int groupPosition, int childPosition) {
            return childPosition;
        }

        @Override
        public boolean hasStableIds() {
            return false;
        }

        /**
         * 显示：group
         */
        @Override
        public View getGroupView(int groupPosition, boolean isExpanded,
                                 View convertView, ViewGroup parent) {
            ViewHolder holder;
            if (convertView == null) {
                convertView = LayoutInflater.from(mActivity).inflate(R.layout.unlock_record_groupitem, null);
                holder = new ViewHolder();
                holder.rly_user_manage = (RelativeLayout) convertView.findViewById(R.id.rly_user_manage);
                holder.tv_group_time = (TextView) convertView.findViewById(R.id.tv_group_time);
                holder.tv_magin_15 = (TextView) convertView.findViewById(R.id.tv_magin_15);
                holder.img_down_up = (ImageView) convertView.findViewById(R.id.img_down_up);
                holder.img_shadow_head1 = (ImageView) convertView.findViewById(R.id.img_shadow_head1);

                convertView.setTag(holder);
            } else {
                holder = (ViewHolder) convertView.getTag();
            }
            holder.tv_group_time.setText(groupListData.get(groupPosition).getYear_month());

            if (isExpanded) {
                holder.img_down_up.setImageResource(R.mipmap.btn_up);
                holder.img_shadow_head1.setVisibility(View.VISIBLE); //头部阴影部分
                holder.tv_magin_15.setVisibility(View.GONE);
            } else {
                holder.img_down_up.setImageResource(R.mipmap.btn_down);
                holder.img_shadow_head1.setVisibility(View.GONE);
                holder.tv_magin_15.setVisibility(View.VISIBLE);
            }

            return convertView;
        }

        /**
         * 显示：child
         */
        @Override
        public View getChildView(int groupPosition, int childPosition,
                                 boolean isLastChild, View convertView, ViewGroup parent) {
            ViewHolder holder;
            if (convertView == null) {
                convertView = LayoutInflater.from(mActivity).inflate(R.layout.unlock_record_childitem, null);
                holder = new ViewHolder();
                holder.lly_unlock_record = (LinearLayout) convertView.findViewById(R.id.lly_unlock_record);
                holder.tv_child_name = (TextView) convertView.findViewById(R.id.tv_child_name);
                holder.tv_unlock_time = (TextView) convertView.findViewById(R.id.tv_unlock_time);
                holder.tv_divide = (TextView) convertView.findViewById(R.id.tv_divide);
                holder.img_shadow_head = (ImageView) convertView.findViewById(R.id.img_shadow_head);
                holder.img_shadow_tail = (ImageView) convertView.findViewById(R.id.img_shadow_tail);

//                if (isLastChild) {
//                    holder.img_shadow_tail.setVisibility(View.VISIBLE);
//                    holder.tv_divide.setVisibility(View.GONE);
//                }
                convertView.setTag(holder);
            } else {
                holder = (ViewHolder) convertView.getTag();
            }

            Log.e("url", "childPosition==" + childPosition);
            Log.e("url", "childListData.xize==" + childListData.get(groupPosition).size());

            Log.e("url", "isLastChild==" + isLastChild);

            if (isLastChild) {
                holder.img_shadow_tail.setVisibility(View.VISIBLE);
                holder.tv_divide.setVisibility(View.GONE);
            } else {
                holder.img_shadow_tail.setVisibility(View.GONE);
                holder.tv_divide.setVisibility(View.VISIBLE);
            }

            if (childListData.get(groupPosition).get(childPosition).getUser_type().equals("01")) {
                holder.tv_child_name.setText("普通用户" + Integer.parseInt(childListData.get(groupPosition).get(childPosition).getUser_type_num()));
            } else if (childListData.get(groupPosition).get(childPosition).getUser_type().equals("02")) {
                holder.tv_child_name.setText("手机用户" + Integer.parseInt(childListData.get(groupPosition).get(childPosition).getUser_type_num()));
            } else if (childListData.get(groupPosition).get(childPosition).getUser_type().equals("03")) {
                holder.tv_child_name.setText("TM卡用户" + Integer.parseInt(childListData.get(groupPosition).get(childPosition).getUser_type_num()));
            } else if (childListData.get(groupPosition).get(childPosition).getUser_type().equals("04")) {
                holder.tv_child_name.setText("期限用户" + Integer.parseInt(childListData.get(groupPosition).get(childPosition).getUser_type_num()) + "(单次)");
            } else if (childListData.get(groupPosition).get(childPosition).getUser_type().equals("05")) {
                holder.tv_child_name.setText("期限用户" + Integer.parseInt(childListData.get(groupPosition).get(childPosition).getUser_type_num()) + "(一天)");
            } else if (childListData.get(groupPosition).get(childPosition).getUser_type().equals("06")) {
                holder.tv_child_name.setText("期限用户" + Integer.parseInt(childListData.get(groupPosition).get(childPosition).getUser_type_num()) + "(一星期)");
            } else if (childListData.get(groupPosition).get(childPosition).getUser_type().equals("07")) {
                holder.tv_child_name.setText("期限用户" + Integer.parseInt(childListData.get(groupPosition).get(childPosition).getUser_type_num()) + "(一个月)");
            } else {
                holder.tv_child_name.setText("未知用户");
            }

            holder.tv_unlock_time.setText(childListData.get(groupPosition).get(childPosition).getMonth() + " - "
                    + childListData.get(groupPosition).get(childPosition).getDay() + "  "
                    + childListData.get(groupPosition).get(childPosition).getHour() + ":"
                    + childListData.get(groupPosition).get(childPosition).getMinute());

            return convertView;
        }

        class ViewHolder {
            //头容器
            private RelativeLayout rly_user_manage;
            private TextView tv_group_time, tv_magin_15;
            private ImageView img_down_up, img_shadow_head1;

            //子容器
            private LinearLayout lly_unlock_record;
            private TextView tv_child_name, tv_unlock_time, tv_divide;
            private ImageView img_shadow_head, img_shadow_tail;
        }

        @Override
        public boolean isChildSelectable(int groupPosition, int childPosition) {
            return false;
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        MainActivity.is_scan = true;//停止扫描
    }
}

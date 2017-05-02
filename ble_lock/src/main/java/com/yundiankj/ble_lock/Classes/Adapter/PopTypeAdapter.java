package com.yundiankj.ble_lock.Classes.Adapter;

import android.content.Context;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.TextView;

import com.yundiankj.ble_lock.R;

/**
 * 用于已连接设备中的popwindow(类型选择的适配器)
 */
public class PopTypeAdapter extends BaseAdapter {

    private Context context;
    private String[] typeList;
    private LayoutInflater mInflater;
    TextView textView = null;

    public PopTypeAdapter(Context context, String[] type) {
        this.context = context;
        this.typeList = type;
        mInflater = LayoutInflater.from(context);
    }

    public int getCount() {
        // TODO Auto-generated method stub
        return typeList.length;
    }

    @Override
    public String getItem(int position) {
        // TODO Auto-generated method stub
        return typeList[position];
    }

    @Override
    public long getItemId(int position) {
        // TODO Auto-generated method stub
        return position;
    }

    public View getView(int position, View convertView, ViewGroup parent) {

        if (convertView == null) {
            textView = new TextView(context);
        } else {
            textView = (TextView) convertView;
        }
        textView.setText(typeList[position]);
        textView.setTextSize(14);
        textView.setGravity(Gravity.CENTER);
        textView.setPadding(0, 30, 0,30);
        textView.setTextColor(context.getResources().getColor(R.color.A333333));
        textView.setBackgroundColor(context.getResources().getColor(R.color.ffffff));
        return textView;
    }
}

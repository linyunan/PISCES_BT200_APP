package com.yundiankj.ble_lock.Classes.Activity.UserManage;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.ImageView;
import android.widget.RelativeLayout;

import com.yundiankj.ble_lock.Classes.Activity.MainActivity;
import com.yundiankj.ble_lock.R;
import com.yundiankj.ble_lock.Resource.StatusBarUtil;

/**
 * Created by hong on 2016/7/19.
 * <p>
 * 用户管理
 */
public class UserManage extends Activity implements View.OnClickListener {

    private Activity mActivity;
    private ImageView img_back;
    private RelativeLayout rly_phone_user_manage, rly_keyboard_user_manage, rly_tm_card_user_manage;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.user_manage);

        mActivity = this;
        initWindow();//沉浸式状态栏
        init();
    }

    private void initWindow() {
        StatusBarUtil.setColor(mActivity, getResources().getColor(R.color.A04a8ec), 0);
    }

    private void init() {
        img_back = (ImageView) mActivity.findViewById(R.id.img_back);
        rly_phone_user_manage = (RelativeLayout) mActivity.findViewById(R.id.rly_phone_user_manage);
        rly_keyboard_user_manage = (RelativeLayout) mActivity.findViewById(R.id.rly_keyboard_user_manage);
        rly_tm_card_user_manage = (RelativeLayout) mActivity.findViewById(R.id.rly_tm_card_user_manage);

        img_back.setOnClickListener(this);
        rly_phone_user_manage.setOnClickListener(this);
        rly_keyboard_user_manage.setOnClickListener(this);
        rly_tm_card_user_manage.setOnClickListener(this);
    }

    @Override
    public void onClick(View v) {
        Intent intent;
        switch (v.getId()){
            case R.id.img_back://返回
                finish();
                break;
            case R.id.rly_phone_user_manage://手机用户管理
                intent=new Intent(mActivity,PhoneUserManage.class);
                startActivity(intent);
                break;
            case R.id.rly_keyboard_user_manage://键盘用户管理
                intent=new Intent(mActivity,KeyboardUserManage.class);
                startActivity(intent);
                break;
            case R.id.rly_tm_card_user_manage://TM卡用户管理
                intent=new Intent(mActivity,TMCardUserManage.class);
                startActivity(intent);
                break;
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        MainActivity.is_scan = true;//停止扫描
    }
}

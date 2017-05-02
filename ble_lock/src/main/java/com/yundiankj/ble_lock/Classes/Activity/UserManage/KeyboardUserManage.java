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
 * <p/>
 * 键盘用户管理
 */
public class KeyboardUserManage extends Activity implements View.OnClickListener {

    private Activity mActivity;
    private ImageView img_back;
    private RelativeLayout rly_admin_manage, rly_ordinary_user, rly_disposable_user;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.keyboard_user_manage);

        mActivity = this;
        initWindow();//沉浸式状态栏
        init();
    }

    private void initWindow() {
        StatusBarUtil.setColor(mActivity, getResources().getColor(R.color.A04a8ec), 0);
    }

    private void init() {

        img_back = (ImageView) mActivity.findViewById(R.id.img_back);
        rly_admin_manage = (RelativeLayout) mActivity.findViewById(R.id.rly_admin_manage);
        rly_ordinary_user = (RelativeLayout) mActivity.findViewById(R.id.rly_ordinary_user);
        rly_disposable_user = (RelativeLayout) mActivity.findViewById(R.id.rly_disposable_user);


        img_back.setOnClickListener(this);
        rly_admin_manage.setOnClickListener(this);
        rly_ordinary_user.setOnClickListener(this);
        rly_disposable_user.setOnClickListener(this);
    }

    @Override
    public void onClick(View v) {
        Intent intent;
        switch (v.getId()) {
            case R.id.img_back://返回
                finish();
                break;
            case R.id.rly_admin_manage://管理员管理
                intent = new Intent(mActivity,KeyBordAdminUser.class);
                startActivity(intent);
                break;
            case R.id.rly_ordinary_user://普通用户
                intent = new Intent(mActivity, KeyBordOrdinaryUser.class);
                startActivity(intent);
                break;
            case R.id.rly_disposable_user://期限用户
                intent = new Intent(mActivity, KeyBordDisposableUser.class);
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

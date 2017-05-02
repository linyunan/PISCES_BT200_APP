package com.yundiankj.ble_lock.Classes.Activity.HelpCenter;

import android.app.Activity;
import android.os.Bundle;
import android.view.View;
import android.widget.ImageView;

import com.yundiankj.ble_lock.R;
import com.yundiankj.ble_lock.Resource.StatusBarUtil;

/**
 * Created by hong on 2016/8/30.
 *
 * 关于我们
 */
public class AboutUs extends Activity {

    private Activity mActivity;
    private ImageView img_back;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.help_about_us);

        mActivity = this;
        initWindow();//沉浸式状态栏
        init();

    }

    private void initWindow() {
        StatusBarUtil.setColor(mActivity, getResources().getColor(R.color.A04a8ec), 0);
    }

    private void init() {
        img_back= (ImageView) mActivity.findViewById(R.id.img_back);
        img_back.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                finish();
            }
        });
    }
}
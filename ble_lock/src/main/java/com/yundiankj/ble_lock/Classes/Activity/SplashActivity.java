package com.yundiankj.ble_lock.Classes.Activity;

import android.app.Activity;
import android.content.Intent;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.util.Log;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.yundiankj.ble_lock.R;
import com.yundiankj.ble_lock.Resource.SharedPreferencesUtil;

/**
 * Created by hong on 2016/10/9.
 * <p/>
 * 启动页
 */
public class SplashActivity extends Activity {

    private Activity mActivity;
    private SharedPreferencesUtil sharedPreferencesUtil;
    private FrameLayout fly_phone;
    private EditText et_phone;
    private TextView tv_ok;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_splash);
        mActivity = this;
        initWindow();//沉浸式的头部
        init();
    }

    private void initWindow() {

        final int sdk = Build.VERSION.SDK_INT;
        Window window = mActivity.getWindow();
        WindowManager.LayoutParams params = window.getAttributes();

        if (sdk >= Build.VERSION_CODES.KITKAT) {
            int bits = WindowManager.LayoutParams.FLAG_TRANSLUCENT_STATUS;
            // 设置透明状态栏
            if ((params.flags & bits) == 0) {
                params.flags |= bits;
                window.setAttributes(params);
            }
        }
    }

    private void init() {
        sharedPreferencesUtil = SharedPreferencesUtil.getInstance(mActivity);
        fly_phone = (FrameLayout) mActivity.findViewById(R.id.fly_phone);
        et_phone = (EditText) mActivity.findViewById(R.id.et_phone);
        tv_ok = (TextView) mActivity.findViewById(R.id.tv_ok);

        if (sharedPreferencesUtil.getFirstTime() == 0) {
            fly_phone.setVisibility(View.VISIBLE);

            tv_ok.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    if (et_phone.getText().length() == 0) {
                        Toast.makeText(mActivity, "请输入手机号码！", Toast.LENGTH_SHORT).show();
                    } else if (et_phone.getText().length() < 11) {
                        Toast.makeText(mActivity, "请输入11位的手机号码！", Toast.LENGTH_SHORT).show();
                    } else {
                        sharedPreferencesUtil.setFirstTime(1);
                        sharedPreferencesUtil.setPhone(et_phone.getText().toString());
                        pushActivity();
                    }
                }
            });
        } else {
            fly_phone.setVisibility(View.GONE);
            new Handler().postDelayed(new Runnable() {
                public void run() {
                    pushActivity();
                }
            }, 500); //1000 for release
        }
    }

    public void pushActivity() {
        Log.e("url", "本机的手机号码==" + sharedPreferencesUtil.getPhone());
        Intent intent = new Intent(mActivity, MainActivity.class);
        startActivity(intent);
        finish();
        overridePendingTransition(R.anim.fade_in, R.anim.fade_out);
    }
}

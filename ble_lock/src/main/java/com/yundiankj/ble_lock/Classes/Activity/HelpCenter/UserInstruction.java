package com.yundiankj.ble_lock.Classes.Activity.HelpCenter;

import android.app.Activity;
import android.os.Bundle;
import android.view.View;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.TextView;

import com.yundiankj.ble_lock.R;
import com.yundiankj.ble_lock.Resource.StatusBarUtil;

/**
 * Created by hong on 2016/8/30.
 *
 * （帮助fg）使用说明
 */
public class UserInstruction extends Activity implements View.OnClickListener {


    private Activity mActivity;
    private ImageView img_back;
    private RelativeLayout rly_question_1, rly_question_2, rly_question_3, rly_question_4, rly_question_5;
    private ImageView img_down_up_1, img_down_up_2, img_down_up_3, img_down_up_4, img_down_up_5;
    private LinearLayout lly_answer_1, lly_answer_2, lly_answer_3, lly_answer_4, lly_answer_5;
    private TextView tv_divide_1, tv_divide_2, tv_divide_3, tv_divide_4;
    private boolean question_1_is_open = true, question_2_is_open = true, question_3_is_open = true,
            question_4_is_open = true, question_5_is_open = true;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.help_user_instruction);

        mActivity = this;
        initWindow();//沉浸式状态栏
        init();

    }

    private void initWindow() {
        StatusBarUtil.setColor(mActivity, getResources().getColor(R.color.A04a8ec), 0);
    }

    private void init() {
        img_back = (ImageView) mActivity.findViewById(R.id.img_back);
        rly_question_1 = (RelativeLayout) mActivity.findViewById(R.id.rly_question_1);
        rly_question_2 = (RelativeLayout) mActivity.findViewById(R.id.rly_question_2);
        rly_question_3 = (RelativeLayout) mActivity.findViewById(R.id.rly_question_3);
        rly_question_4 = (RelativeLayout) mActivity.findViewById(R.id.rly_question_4);
        rly_question_5 = (RelativeLayout) mActivity.findViewById(R.id.rly_question_5);
        img_down_up_1 = (ImageView) mActivity.findViewById(R.id.img_down_up_1);
        img_down_up_2 = (ImageView) mActivity.findViewById(R.id.img_down_up_2);
        img_down_up_3 = (ImageView) mActivity.findViewById(R.id.img_down_up_3);
        img_down_up_4 = (ImageView) mActivity.findViewById(R.id.img_down_up_4);
        img_down_up_5 = (ImageView) mActivity.findViewById(R.id.img_down_up_5);
        lly_answer_1 = (LinearLayout) mActivity.findViewById(R.id.lly_answer_1);
        lly_answer_2 = (LinearLayout) mActivity.findViewById(R.id.lly_answer_2);
        lly_answer_3 = (LinearLayout) mActivity.findViewById(R.id.lly_answer_3);
        lly_answer_4 = (LinearLayout) mActivity.findViewById(R.id.lly_answer_4);
        lly_answer_5 = (LinearLayout) mActivity.findViewById(R.id.lly_answer_5);
        tv_divide_1 = (TextView) mActivity.findViewById(R.id.tv_divide_1);
        tv_divide_2 = (TextView) mActivity.findViewById(R.id.tv_divide_2);
        tv_divide_3 = (TextView) mActivity.findViewById(R.id.tv_divide_3);
        tv_divide_4 = (TextView) mActivity.findViewById(R.id.tv_divide_4);

        img_back.setOnClickListener(this);
        rly_question_1.setOnClickListener(this);
        rly_question_2.setOnClickListener(this);
        rly_question_3.setOnClickListener(this);
        rly_question_4.setOnClickListener(this);
        rly_question_5.setOnClickListener(this);
    }

    @Override
    public void onClick(View v) {
        switch (v.getId()) {
            case R.id.img_back://返回
                finish();
                break;
            case R.id.rly_question_1:
                if (question_1_is_open == true) {//收缩
                    lly_answer_1.setVisibility(View.GONE);
                    img_down_up_1.setImageResource(R.mipmap.btn_down);
                    tv_divide_1.setVisibility(View.VISIBLE);
                    question_1_is_open = false;
                } else {
                    lly_answer_1.setVisibility(View.VISIBLE);
                    img_down_up_1.setImageResource(R.mipmap.btn_up);
                    tv_divide_1.setVisibility(View.GONE);
                    question_1_is_open = true;
                }
                break;
            case R.id.rly_question_2:
                if (question_2_is_open == true) {//收缩
                    lly_answer_2.setVisibility(View.GONE);
                    img_down_up_2.setImageResource(R.mipmap.btn_down);
                    tv_divide_2.setVisibility(View.VISIBLE);
                    question_2_is_open = false;
                } else {
                    lly_answer_2.setVisibility(View.VISIBLE);
                    img_down_up_2.setImageResource(R.mipmap.btn_up);
                    tv_divide_2.setVisibility(View.GONE);
                    question_2_is_open = true;
                }
                break;
            case R.id.rly_question_3:
                if (question_3_is_open == true) {//收缩
                    lly_answer_3.setVisibility(View.GONE);
                    img_down_up_3.setImageResource(R.mipmap.btn_down);
                    tv_divide_3.setVisibility(View.VISIBLE);
                    question_3_is_open = false;
                } else {
                    lly_answer_3.setVisibility(View.VISIBLE);
                    img_down_up_3.setImageResource(R.mipmap.btn_up);
                    tv_divide_3.setVisibility(View.GONE);
                    question_3_is_open = true;
                }
                break;

            case R.id.rly_question_4:
                if (question_4_is_open == true) {//收缩
                    lly_answer_4.setVisibility(View.GONE);
                    img_down_up_4.setImageResource(R.mipmap.btn_down);
                    tv_divide_4.setVisibility(View.VISIBLE);
                    question_4_is_open = false;
                } else {
                    lly_answer_4.setVisibility(View.VISIBLE);
                    img_down_up_4.setImageResource(R.mipmap.btn_up);
                    tv_divide_4.setVisibility(View.GONE);
                    question_4_is_open = true;
                }
                break;
            case R.id.rly_question_5:
                if (question_5_is_open == true) {//收缩
                    lly_answer_5.setVisibility(View.GONE);
                    img_down_up_5.setImageResource(R.mipmap.btn_down);
                    question_5_is_open = false;
                } else {
                    lly_answer_5.setVisibility(View.VISIBLE);
                    img_down_up_5.setImageResource(R.mipmap.btn_up);
                    question_5_is_open = true;
                }
                break;

        }
    }
}
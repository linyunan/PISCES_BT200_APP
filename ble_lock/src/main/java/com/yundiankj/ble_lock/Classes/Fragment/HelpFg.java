package com.yundiankj.ble_lock.Classes.Fragment;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.RelativeLayout;

import com.yundiankj.ble_lock.Classes.Activity.HelpCenter.AboutUs;
import com.yundiankj.ble_lock.Classes.Activity.HelpCenter.BasicIntroduction;
import com.yundiankj.ble_lock.Classes.Activity.HelpCenter.FunctionSolution;
import com.yundiankj.ble_lock.Classes.Activity.HelpCenter.SafyQuestion;
import com.yundiankj.ble_lock.Classes.Activity.HelpCenter.TroubleRemove;
import com.yundiankj.ble_lock.Classes.Activity.HelpCenter.UserInstruction;
import com.yundiankj.ble_lock.R;

/**
 * Created by hong on 2016/7/18.
 * <p>
 * 帮助Fg
 */
public class HelpFg extends Fragment implements View.OnClickListener {

    private View rootView;
    private Activity mActivity;
    private RelativeLayout rly_basic_introduction, rly_user_instruction, rly_safy_question, rly_function_solution, rly_trouble_remove, rly_about_us;

    // 当创建fragment的UI被初始化时调用。
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        if (rootView == null) {
            rootView = inflater.inflate(R.layout.help_fragment, container, false);
        }
        ViewGroup parent = (ViewGroup) rootView.getParent();
        if (parent != null) {
            parent.removeView(rootView);
        }

        return rootView;
    }

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
        mActivity = getActivity();
        init();
    }

    private void init() {
        rly_basic_introduction = (RelativeLayout) rootView.findViewById(R.id.rly_basic_introduction);
        rly_user_instruction = (RelativeLayout) rootView.findViewById(R.id.rly_user_instruction);
        rly_safy_question = (RelativeLayout) rootView.findViewById(R.id.rly_safy_question);
        rly_function_solution = (RelativeLayout) rootView.findViewById(R.id.rly_function_solution);
        rly_trouble_remove = (RelativeLayout) rootView.findViewById(R.id.rly_trouble_remove);
        rly_about_us = (RelativeLayout) rootView.findViewById(R.id.rly_about_us);

        rly_basic_introduction.setOnClickListener(this);
        rly_user_instruction.setOnClickListener(this);
        rly_safy_question.setOnClickListener(this);
        rly_function_solution.setOnClickListener(this);
        rly_trouble_remove.setOnClickListener(this);
        rly_about_us.setOnClickListener(this);
    }

    @Override
    public void onClick(View v) {
        switch (v.getId()) {
            case R.id.rly_basic_introduction://基本介绍
                startActivity(new Intent(mActivity, BasicIntroduction.class));
                break;
            case R.id.rly_user_instruction://使用说明
                startActivity(new Intent(mActivity, UserInstruction.class));
                break;
            case R.id.rly_safy_question://安全问题
                startActivity(new Intent(mActivity, SafyQuestion.class));
                break;
            case R.id.rly_function_solution://功能解答
                startActivity(new Intent(mActivity, FunctionSolution.class));
                break;
            case R.id.rly_trouble_remove://故障排除
                startActivity(new Intent(mActivity, TroubleRemove.class));

                break;
            case R.id.rly_about_us://关于我们
                startActivity(new Intent(mActivity, AboutUs.class));
                break;
        }

    }
}

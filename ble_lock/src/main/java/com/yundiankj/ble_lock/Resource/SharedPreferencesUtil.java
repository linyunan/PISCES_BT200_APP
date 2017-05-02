package com.yundiankj.ble_lock.Resource;

import android.content.Context;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;

/**
 * SharedPreferences工具�?
 *
 * @author Administrator
 */
public class SharedPreferencesUtil {

    public static final String FILENAME = "SharedPreferences";

    //蓝牙名称
    public static final String BLENAME = "blename";

    //蓝牙MAC(序列号)
    public static final String MAC = "mac";

    public static final String ID = "id";

    //蓝牙类型（门锁、挂锁）
    public static final String TYPE = "type";

    //服务值
    public static final String SERVICE = "SERVICE";
    //特征值
    public static final String CHAR_FFE1 = "CHAR_FFE1";
    public static final String CHAR_FFE2 = "CHAR_FFE2";
    public static final String CHAR_FFE3= "CHAR_FFE3";

    //第一次安装app
    public static final String FIRSTTIME = "FIRSTTIME";

    public static final String PHONE="PHONE";

    //开锁方式（auto_unlock自动开锁，light_screen点亮屏幕开锁）
    public static final String UNLOCKMETHOD = "UNLOCKMETHOD";

   //开锁方式---自动开锁的距离
    public static final String AUTOOPENDISTANCE = "AUTOOPENDISTANCE";

    //连接提示音
    public static final String CONNECT_MUSIC = "CONNECT_MUSIC";

   //开锁提示音
    public static final String UNLOCK_MUSIC = "UNLOCK_MUSIC";

   //连接提示音的开关
    public static final String CONNECT_MUSIC_SWITCH  = "CONNECT_MUSIC_SWITCH";

    //开锁提示音的开关
    public static final String UNLOCK_MUSIC_SWITCH = "UNLOCK_MUSIC_SWITCH";

    //屏幕状态
    public static final String SCREEN_STATE = "SCREEN_STATE";

    //手机号码后面的数 11 或者21
    public static final String PHONEADD = "PHONEADD";

    /**
     * 系统消息
     */
    public static final String SYSTEMMSG = "systemmsg";

    /**
     * 圈子消息
     */
    public static final String CIRCLEMSG = "circlemsg";

    /**
     * 好友消息
     */
    public static final String FRIENDMSG = "friendmsg";

    /**
     * 声音
     */
    public static final String VOICE = "voice";

    /**
     * 震动
     */
    public static final String VIBRATE = "vibrate";

    /**
     * 密码
     */
    public static final String CITYCODE = "citycode";

    /**
     * access_token
     */
    public static final String ACCESS_TOKEN = "access_token";

    /**
     * 城市编码
     */
    public static final String CITYNAME = "cityname";

    /**
     * 城市编码
     */

    public static final String COOKIE = "cookie";

    private static SharedPreferencesUtil instance;
    private SharedPreferences settings;

    private Editor editor;

    private SharedPreferencesUtil(Context context) {
        settings = context.getSharedPreferences(FILENAME, Context.MODE_PRIVATE);
        editor = settings.edit();
    }

    /**
     * 配置�?要区分用户名存储�?
     *
     * @param context
     * @return
     */
    public static SharedPreferencesUtil getInstance(Context context) {
        if (instance == null) {
            instance = new SharedPreferencesUtil(context);
        }
        return instance;
    }

    public SharedPreferences getSharedPreferences() {
        return settings;
    }

    public Editor getEditor() {
        return editor;
    }

    /**
     * 清空�?有数�?
     */
    public void clearAllData() {
        editor.clear();
        editor.commit();
    }

    public String getCitycode() {
        return settings.getString(CITYCODE, "");// 厦门编号16278
    }

    public void setCitycode(String citycode) {
        editor.putString(CITYCODE, citycode);
        editor.commit();
    }

    public String getCityname() {
        return settings.getString(CITYNAME, "");
    }

    public void setCityname(String cityname) {
        editor.putString(CITYNAME, cityname);
        editor.commit();
    }

    public String getAccessToken() {
        return settings.getString(ACCESS_TOKEN, "");
    }

    public void setAccessToken(String accessToken) {
        editor.putString(ACCESS_TOKEN, accessToken);
        editor.commit();
    }

    public String getType() {
        return settings.getString(TYPE, "");
    }

    public void setType(String type) {
        editor.putString(TYPE, type);
        editor.commit();
    }

    public void setService(String service) {
        editor.putString(SERVICE, service);
        editor.commit();
    }

    public String getService() {
        return settings.getString(SERVICE, "0");
    }

    public void setCharFfe1(String charFfe1) {
        editor.putString(CHAR_FFE1, charFfe1);
        editor.commit();
    }

    public String getCharFfe1() {
        return settings.getString(CHAR_FFE1, "0");
    }

    public void setCharFfe2(String charFfe2) {
        editor.putString(CHAR_FFE2, charFfe2);
        editor.commit();
    }

    public String getCharFfe2() {
        return settings.getString(CHAR_FFE2, "0");
    }

    public void setCharFfe3(String charFfe3) {
        editor.putString(CHAR_FFE3, charFfe3);
        editor.commit();
    }

    public String getCharFfe3() {
        return settings.getString(CHAR_FFE3, "0");
    }

    public String getId() {
        return settings.getString(ID, "");
    }

    public void setId(String id) {
        editor.putString(ID, id);
        editor.commit();
    }

    public int getFirstTime() {
        return settings.getInt(FIRSTTIME, 0);
    }

    public void setFirstTime(int firstTime) {
        editor.putInt(FIRSTTIME, firstTime);
        editor.commit();
    }

    public String getPhone() {
        return settings.getString(PHONE, "0");
    }

    public void setPhone(String phone) {
        editor.putString(PHONE, phone);
        editor.commit();
    }

    public String getUnlockmethod() {
        return settings.getString(UNLOCKMETHOD, "");
    }

    public void setUnlockmethod(String unlockmethod) {
        editor.putString(UNLOCKMETHOD, unlockmethod);
        editor.commit();
    }

    public String getAutoopendistance() {
        return settings.getString(AUTOOPENDISTANCE, "");
    }

    public void setAutoopendistance(String auto_open_distance) {
        editor.putString(AUTOOPENDISTANCE, auto_open_distance);
        editor.commit();
    }

    public String getConnectMusic() {
        return settings.getString(CONNECT_MUSIC, "");
    }

    public void setConnectMusic(String connectMusic) {
        editor.putString(CONNECT_MUSIC, connectMusic);
        editor.commit();
    }


    public String getUnlockMusic() {
        return settings.getString(UNLOCK_MUSIC, "");
    }

    public void setUnlockMusic(String unlock_music) {
        editor.putString(UNLOCK_MUSIC, unlock_music);
        editor.commit();
    }

    public String getConnectMusicSwitch() {
        return settings.getString(CONNECT_MUSIC_SWITCH, "off");
    }

    public void setConnectMusicSwitch(String connect_music_switch) {
        editor.putString(CONNECT_MUSIC_SWITCH, connect_music_switch);
        editor.commit();
    }

    public String getUnlockMusicSwitch() {
        return settings.getString(UNLOCK_MUSIC_SWITCH, "off");
    }

    public void setUnlockMusicSwitch(String unlock_music_switch) {
        editor.putString(UNLOCK_MUSIC_SWITCH, unlock_music_switch);
        editor.commit();
    }

    public String getScreenState() {
        return settings.getString(SCREEN_STATE, "");
    }

    public void setScreenState(String screen_state) {
        editor.putString(SCREEN_STATE, screen_state);
        editor.commit();
    }

    public String getPhoneadd() {
        return settings.getString(PHONEADD, "");
    }

    public void setPhoneadd(String phoneadd) {
        editor.putString(PHONEADD, phoneadd);
        editor.commit();
    }


    public Boolean getSystemMsg() {
        return settings.getBoolean(SYSTEMMSG, true);
    }

    public void setSystemMsg(Boolean systemmsg) {
        editor.putBoolean(SYSTEMMSG, systemmsg);
        editor.commit();
    }

    public Boolean getCircleMsg() {
        return settings.getBoolean(CIRCLEMSG, true);
    }

    public void setCircleMsg(Boolean circlemsg) {
        editor.putBoolean(CIRCLEMSG, circlemsg);
        editor.commit();
    }

    public Boolean getFriendMsg() {
        return settings.getBoolean(FRIENDMSG, true);
    }

    public void setFriendMsg(Boolean friendmsg) {
        editor.putBoolean(FRIENDMSG, friendmsg);
        editor.commit();
    }

    public Boolean getVoice() {
        return settings.getBoolean(VOICE, true);
    }

    public void setVoice(Boolean voice) {
        editor.putBoolean(VOICE, voice);
        editor.commit();
    }

    public Boolean getVibrate() {
        return settings.getBoolean(VIBRATE, true);
    }

    public void setVibrate(Boolean vibrate) {
        editor.putBoolean(VIBRATE, vibrate);
        editor.commit();
    }

    public String getCookie() {
        return settings.getString(COOKIE, "");
    }

    public void setCookie(String cookie) {
        editor.putString(COOKIE, cookie);
        editor.commit();
    }

    public String getBLENANE() {
        return settings.getString(BLENAME, "");
    }

    public void setBLENAME(String blename) {
        editor.putString(BLENAME, blename);
        editor.commit();
    }

    public String getMAC() {
        return settings.getString(MAC, "");
    }

    public void setMAC(String mac) {
        editor.putString(MAC, mac);
        editor.commit();
    }
}

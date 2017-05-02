package com.yundiankj.ble_lock.Classes.Model;

/**
 * Created by hong on 2016/11/21.
 */
public class DisposableDataInfo {

    private String id,password,type,user_id,start_time,end_time,str_user,is_open,cn_time;


    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public String getUser_id() {
        return user_id;
    }

    public void setUser_id(String user_id) {
        this.user_id = user_id;
    }

    public String getStart_time() {
        return start_time;
    }

    public void setStart_time(String start_time) {
        this.start_time = start_time;
    }

    public String getEnd_time() {
        return end_time;
    }

    public void setEnd_time(String end_time) {
        this.end_time = end_time;
    }

    public String getStr_user() {
        return str_user;
    }

    public void setStr_user(String str_user) {
        this.str_user = str_user;
    }

    public String getIs_open() {
        return is_open;
    }

    public void setIs_open(String is_open) {
        this.is_open = is_open;
    }

    public String getCn_time() {
        return cn_time;
    }

    public void setCn_time(String cn_time) {
        this.cn_time = cn_time;
    }
}

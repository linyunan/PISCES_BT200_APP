package com.yundiankj.ble_lock.Classes.Model;

import java.util.ArrayDeque;
import java.util.UUID;

/**
 * Created by hong on 2016/12/1.
 */
public class ParsedAd {
    
    public byte flags;
    public ArrayDeque<UUID> uuids=new ArrayDeque<>();
    public String localName;
    public short manufacturer;
}

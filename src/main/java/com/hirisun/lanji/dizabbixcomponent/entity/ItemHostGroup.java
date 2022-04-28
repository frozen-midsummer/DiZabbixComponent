package com.hirisun.lanji.dizabbixcomponent.entity;

import java.util.HashMap;

public class ItemHostGroup {
    private String hostid;
    private String hostname;
    private String hostip;
    private long time;
    private HashMap<String,HashMap<String,String>> items;

    public String getHostid() {
        return hostid;
    }

    public void setHostid(String hostid) {
        this.hostid = hostid;
    }

    public String getHostname() {
        return hostname;
    }

    public void setHostname(String hostname) {
        this.hostname = hostname;
    }

    public String getHostip() {
        return hostip;
    }

    public void setHostip(String hostip) {
        this.hostip = hostip;
    }

    public HashMap<String, HashMap<String, String>> getItems() {
        return items;
    }

    public void setItems(HashMap<String, HashMap<String, String>> items) {
        this.items = items;
    }

    public long getTime() {
        return time;
    }

    public void setTime(long time) {
        this.time = time;
    }

    public ItemHostGroup(String hostid, String hostname, String hostip, HashMap<String, HashMap<String, String>> items) {
        this.hostid = hostid;
        this.hostname = hostname;
        this.hostip = hostip;
        this.items = items;
    }
}

package com.example.demo3.entity;

public class ZabbixInfo {// 前端返回数据

    private String entranceAddress;
    private String usrname;
    private String psd;

    public void setEntranceAddress(String entranceAddress) {
        this.entranceAddress = entranceAddress;
    }
    public String getEntranceAddress() {
        return entranceAddress;
    }

    public void setUsrname(String usrname) {
        this.usrname = usrname;
    }
    public String getUsrname() {
        return usrname;
    }

    public void setPsd(String psd) {
        this.psd = psd;
    }
    public String getPsd() {
        return psd;
    }

    public ZabbixInfo() {
    }
}
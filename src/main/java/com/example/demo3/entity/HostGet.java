package com.example.demo3.entity;

import java.util.List;

public class HostGet {

    private String jsonrpc;
    private List<HostGetResult> result;
    private int id;

    public void setJsonrpc(String jsonrpc) {
        this.jsonrpc = jsonrpc;
    }

    public String getJsonrpc() {
        return jsonrpc;
    }

    public void setResult(List<HostGetResult> result) {
        this.result = result;
    }

    public List<HostGetResult> getResult() {
        return result;
    }

    public void setId(int id) {
        this.id = id;
    }

    public int getId() {
        return id;
    }

}

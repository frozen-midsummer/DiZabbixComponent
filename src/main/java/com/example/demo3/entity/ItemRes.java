package com.example.demo3.entity;

import java.util.List;

public class ItemRes {

        private String jsonrpc;
        private List<ItemResResult> result;
        private int id;
        public void setJsonrpc(String jsonrpc) {
            this.jsonrpc = jsonrpc;
        }
        public String getJsonrpc() {
            return jsonrpc;
        }

        public void setResult(List<ItemResResult> result) {
            this.result = result;
        }
        public List<ItemResResult> getResult() {
            return result;
        }

        public void setId(int id) {
            this.id = id;
        }
        public int getId() {
            return id;
        }

}

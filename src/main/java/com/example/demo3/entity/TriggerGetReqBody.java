package com.example.demo3.entity;

public class TriggerGetReqBody {
    private long beginDate;
    private long endDate;

    public long getBeginDate() {
        return beginDate;
    }

    public void setBeginDate(long beginDate) {
        this.beginDate = beginDate;
    }

    public long getEndDate() {
        return endDate;
    }

    public void setEndDate(long endDate) {
        this.endDate = endDate;
    }

    public TriggerGetReqBody() {
    }


    public TriggerGetReqBody(long endDate) {
        this.endDate = endDate;
    }

    public TriggerGetReqBody(long beginDate, long endDate) {
        this.beginDate = beginDate;
        this.endDate = endDate;
    }
}

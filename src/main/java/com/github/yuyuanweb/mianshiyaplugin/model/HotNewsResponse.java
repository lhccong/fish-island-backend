package com.github.yuyuanweb.mianshiyaplugin.model;

import java.util.List;

public class HotNewsResponse {
    private int code;
    private List<HotNewsCategory> data;

    public int getCode() {
        return code;
    }

    public void setCode(int code) {
        this.code = code;
    }

    public List<HotNewsCategory> getData() {
        return data;
    }

    public void setData(List<HotNewsCategory> data) {
        this.data = data;
    }
} 
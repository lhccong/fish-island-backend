package com.github.yuyuanweb.mianshiyaplugin.adapter;

import cn.hutool.core.date.DatePattern;
import cn.hutool.core.date.DateUtil;
import com.google.gson.TypeAdapter;
import com.google.gson.stream.JsonReader;
import com.google.gson.stream.JsonWriter;

import java.io.IOException;
import java.util.Date;

/**
 * 处理日期解析异常
 *
 * @author pine
 */
public class DateTypeAdapter extends TypeAdapter<Date> {

    @Override
    public void write(JsonWriter out, Date value) throws IOException {
        if (value == null) {
            out.nullValue();
        } else {
            out.value(DateUtil.format(value, DatePattern.NORM_DATETIME_PATTERN));
        }
    }

    @Override
    public Date read(JsonReader in) throws IOException {
        String dateStr = in.nextString();
        return DateUtil.parse(dateStr);
    }
}

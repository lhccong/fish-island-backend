package com.cong.fishisland.common;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.JsonSerializer;
import com.fasterxml.jackson.databind.SerializerProvider;

import java.io.IOException;
import java.math.BigDecimal;
import java.text.DecimalFormat;

/**
 * BigDecimal 两位小数序列化器（Jackson版本）
 * 确保序列化后的数字始终保留两位小数，例如：5 -> "5.00", 0.6 -> "0.60"
 *
 * @author shing
 */
public class BigDecimalTwoDecimalSerializer extends JsonSerializer<BigDecimal> {

    private static final DecimalFormat DECIMAL_FORMAT = new DecimalFormat("0.00");

    @Override
    public void serialize(BigDecimal value, JsonGenerator gen, SerializerProvider serializers) throws IOException {
        if (value == null) {
            gen.writeNull();
            return;
        }
        gen.writeString(DECIMAL_FORMAT.format(value));
    }
}

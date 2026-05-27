package com.cong.fishisland.model.enums.fund;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;

/**
 * 基金/指数业务常量
 */
public final class FundConstants {

    private FundConstants() {
    }

    /** 默认指数：上证指数 */
    public static final String DEFAULT_INDEX_CODE = "sh000001";

    /** 当前支持交易的指数：代码 -> 名称 */
    public static final Map<String, String> SUPPORTED_INDICES;

    public static final Set<String> SUPPORTED_INDEX_CODES;

    static {
        Map<String, String> map = new LinkedHashMap<>();
        map.put("sh000001", "上证指数");
        map.put("sz399001", "深证成指");
        map.put("sz399006", "创业板指");
        map.put("sh000300", "沪深300");
        map.put("sh000016", "上证50");
        SUPPORTED_INDICES = Collections.unmodifiableMap(map);
        SUPPORTED_INDEX_CODES = Collections.unmodifiableSet(map.keySet());
    }

    /**
     * 规范化指数代码（去空格、转小写；空值时使用默认指数）
     */
    public static String normalizeIndexCode(String indexCode) {
        if (indexCode == null || indexCode.trim().isEmpty()) {
            return DEFAULT_INDEX_CODE;
        }
        return indexCode.trim().toLowerCase();
    }

    public static String getIndexName(String indexCode) {
        return SUPPORTED_INDICES.getOrDefault(normalizeIndexCode(indexCode), indexCode);
    }

    public static boolean isSupportedIndex(String indexCode) {
        return SUPPORTED_INDEX_CODES.contains(normalizeIndexCode(indexCode));
    }
}

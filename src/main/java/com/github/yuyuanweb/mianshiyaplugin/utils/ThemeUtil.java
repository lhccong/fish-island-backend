package com.github.yuyuanweb.mianshiyaplugin.utils;

import com.intellij.openapi.editor.colors.EditorColorsManager;

import java.awt.*;

/**
 * 主题工具类
 *
 * @author pine
 */
public class ThemeUtil {

    private static final String DARK = "dark";

    private static final String LIGHT = "light";

    /**
     * 判断当前颜色是不是暗色
     */
    public static boolean isDarkColor(Color color) {
        // 使用 RGB 值计算亮度
        double brightness = (0.299 * color.getRed() + 0.587 * color.getGreen() + 0.114 * color.getBlue());
        // 128 是亮度的阈值
        return brightness < 128;
    }

    /**
     * 获取该用的主题
     */
    public static String getTheme() {
        Color background = EditorColorsManager.getInstance().getGlobalScheme().getDefaultBackground();
        boolean darkColor = ThemeUtil.isDarkColor(background);
        if (darkColor) {
            return DARK;
        } else {
            return LIGHT;
        }
    }

}

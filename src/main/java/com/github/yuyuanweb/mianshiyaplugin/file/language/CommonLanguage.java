package com.github.yuyuanweb.mianshiyaplugin.file.language;

import com.github.yuyuanweb.mianshiyaplugin.constant.KeyConstant;
import com.intellij.lang.Language;

/**
 * @author pine
 */
public class CommonLanguage extends Language {

    public static final String LANGUAGE_NAME = KeyConstant.EDITOR_FILE_POSTFIX + "QUESTION";

    public static final CommonLanguage INSTANCE = new CommonLanguage();

    protected CommonLanguage() {
        super(LANGUAGE_NAME);
    }
}

package com.github.yuyuanweb.mianshiyaplugin.file.language;

import com.github.yuyuanweb.mianshiyaplugin.constant.KeyConstant;
import com.intellij.lang.Language;

/**
 * @author pine
 */
public class MsycLanguage extends Language {

    public static final String LANGUAGE_NAME = KeyConstant.EDITOR_FILE_POSTFIX_CONTENT + "QUESTION";

    public static final MsycLanguage INSTANCE_C = new MsycLanguage();

    protected MsycLanguage() {
        super(LANGUAGE_NAME);
    }
}

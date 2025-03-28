package com.github.yuyuanweb.mianshiyaplugin.file.type;

import com.github.yuyuanweb.mianshiyaplugin.constant.KeyConstant;
import com.github.yuyuanweb.mianshiyaplugin.file.language.CommonLanguage;
import com.intellij.icons.AllIcons;
import com.intellij.openapi.fileTypes.LanguageFileType;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;

/**
 * @author pine
 */
public class CommonFileType extends LanguageFileType {

    public static final CommonFileType INSTANCE = new CommonFileType();

    private CommonFileType() {
        super(CommonLanguage.INSTANCE);
    }

    @NotNull
    @Override
    public String getName() {
        return KeyConstant.EDITOR_FILE_POSTFIX + "QUESTION";
    }

    @NotNull
    @Override
    public String getDescription() {
        return KeyConstant.EDITOR_FILE_POSTFIX;
    }

    @NotNull
    @Override
    public String getDefaultExtension() {
        return KeyConstant.EDITOR_FILE_POSTFIX;
    }

    @Nullable
    @Override
    public Icon getIcon() {
        return AllIcons.Actions.AddFile;
    }
}
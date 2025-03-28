package com.github.yuyuanweb.mianshiyaplugin.file.provider;

import com.github.yuyuanweb.mianshiyaplugin.constant.ViewConstant;
import com.github.yuyuanweb.mianshiyaplugin.file.type.CommonFileType;
import com.github.yuyuanweb.mianshiyaplugin.file.preview.CommonPreview;
import com.intellij.openapi.fileEditor.FileEditor;
import com.intellij.openapi.fileEditor.FileEditorPolicy;
import com.intellij.openapi.fileEditor.WeighedFileEditorProvider;
import com.intellij.openapi.fileTypes.FileType;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.ui.jcef.JBCefApp;
import org.jetbrains.annotations.NotNull;

/**
 * @author pine
 */
public class CommonProvider extends WeighedFileEditorProvider {


    @Override
    public boolean accept(@NotNull Project project, @NotNull VirtualFile file) {
        FileType fileType = file.getFileType();
        return fileType == CommonFileType.INSTANCE && JBCefApp.isSupported();
    }

    @Override
    public @NotNull FileEditor createEditor(@NotNull Project project, @NotNull VirtualFile file) {
        return new CommonPreview(project, file);
    }

    @Override
    public @NotNull String getEditorTypeId() {
        return ViewConstant.COMMON_PROVIDER;
    }

    @Override
    public @NotNull FileEditorPolicy getPolicy() {
        return FileEditorPolicy.HIDE_DEFAULT_EDITOR;
    }
}

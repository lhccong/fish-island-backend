package com.github.yuyuanweb.mianshiyaplugin.file.provider;

import com.github.yuyuanweb.mianshiyaplugin.file.preview.BrowserFileEditor;
import com.intellij.openapi.fileEditor.FileEditor;
import com.intellij.openapi.fileEditor.FileEditorPolicy;
import com.intellij.openapi.fileEditor.FileEditorProvider;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.vfs.VirtualFile;
import org.jetbrains.annotations.NotNull;

/**
 * 内嵌浏览器 提供者
 *
 * @author pine
 */
public class BrowserFileEditorProvider implements FileEditorProvider {

    @Override
    public boolean accept(@NotNull Project project, @NotNull VirtualFile file) {
        return true;
    }

    @Override
    public @NotNull FileEditor createEditor(@NotNull Project project, @NotNull VirtualFile file) {
        return new BrowserFileEditor(project, file);
    }

    @Override
    public @NotNull String getEditorTypeId() {
        return "browser-file-editor";
    }

    @Override
    public @NotNull FileEditorPolicy getPolicy() {
        return FileEditorPolicy.PLACE_AFTER_DEFAULT_EDITOR;
    }
}

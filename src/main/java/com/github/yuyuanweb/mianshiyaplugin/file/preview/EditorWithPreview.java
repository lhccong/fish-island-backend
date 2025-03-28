package com.github.yuyuanweb.mianshiyaplugin.file.preview;

import com.github.yuyuanweb.mianshiyaplugin.constant.ViewConstant;
import com.github.yuyuanweb.mianshiyaplugin.utils.LogUtils;
import com.intellij.codeHighlighting.BackgroundEditorHighlighter;
import com.intellij.ide.structureView.StructureViewBuilder;
import com.intellij.openapi.actionSystem.ActionGroup;
import com.intellij.openapi.actionSystem.DefaultActionGroup;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.fileEditor.*;
import com.intellij.openapi.util.Key;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * 分栏 文件编辑器
 *
 * @author pine
 */
public class EditorWithPreview extends TextEditorWithPreview {

    public static final Key<EditorWithPreview> PARENT_SPLIT_EDITOR_KEY = Key.create(ViewConstant.PARENT_SPLIT_EDITOR);

    public EditorWithPreview(@NotNull TextEditor editor, @NotNull FileEditor preview) {
        super(editor, preview, "Question Editor", Layout.SHOW_EDITOR);
        editor.putUserData(PARENT_SPLIT_EDITOR_KEY, this);
        preview.putUserData(PARENT_SPLIT_EDITOR_KEY, this);

        // 将 UI 更新放到 invokeLater 中
        ApplicationManager.getApplication().invokeLater(() -> {
            try {
                // 删除工具栏
                getComponent().remove(0);
                getComponent().revalidate();
                getComponent().repaint();
            } catch (Exception e) {
                // 处理异常
                LogUtils.LOG.error(e.getMessage());
            }
        });
    }

    @NotNull
    @Override
    protected ActionGroup createViewActionGroup() {
        return new DefaultActionGroup(
                getShowEditorAction()
        );
    }

    @Nullable
    @Override
    public BackgroundEditorHighlighter getBackgroundHighlighter() {
        return getTextEditor().getBackgroundHighlighter();
    }

    @Nullable
    @Override
    public FileEditorLocation getCurrentLocation() {
        return getTextEditor().getCurrentLocation();
    }

    @Nullable
    @Override
    public StructureViewBuilder getStructureViewBuilder() {
        return getTextEditor().getStructureViewBuilder();
    }


    @NotNull
    @Override
    public TextEditor getTextEditor() {
        if (((TextEditor) myPreview).getEditor() == null) {
            return myEditor;
        }
        return (TextEditor) myPreview;
    }

    @NotNull
    public FileEditor getPreviewEditor() {
        return myPreview == getTextEditor() ? myEditor : myPreview;
    }
}

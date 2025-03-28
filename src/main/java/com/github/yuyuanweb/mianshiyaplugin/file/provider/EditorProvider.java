package com.github.yuyuanweb.mianshiyaplugin.file.provider;

import com.github.yuyuanweb.mianshiyaplugin.constant.KeyConstant;
import com.github.yuyuanweb.mianshiyaplugin.model.enums.WebTypeEnum;
import com.github.yuyuanweb.mianshiyaplugin.file.preview.EditorWithPreview;
import com.github.yuyuanweb.mianshiyaplugin.file.type.MsycFileType;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.fileEditor.AsyncFileEditorProvider;
import com.intellij.openapi.fileEditor.FileEditor;
import com.intellij.openapi.fileEditor.FileEditorProvider;
import com.intellij.openapi.fileEditor.TextEditor;
import com.intellij.openapi.fileEditor.impl.text.PsiAwareTextEditorProvider;
import com.intellij.openapi.fileTypes.FileType;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.ui.jcef.JBCefApp;
import com.intellij.util.keyFMap.KeyFMap;
import org.jetbrains.annotations.NotNull;

/**
 * @author pine
 */
public class EditorProvider extends SplitTextEditorProvider {

    public static final Logger LOG = Logger.getInstance("#com.github.yuyuanweb.mianshiyaplugin");

    public EditorProvider() {
        super(new PsiAwareTextEditorProvider(), new ConvergeProvider(new FileEditorProvider[]{new OuterBrowserFileEditorProvider(WebTypeEnum.QUESTION), new OuterBrowserFileEditorProvider(WebTypeEnum.ANSWER), new OuterBrowserFileEditorProvider(WebTypeEnum.COMMENT)}, new String[]{"题目", "推荐答案", "讨论区"}));
    }

    @Override
    public boolean accept(@NotNull Project project, @NotNull VirtualFile file) {
        FileType fileType = file.getFileType();
        if (fileType == MsycFileType.INSTANCE_C && JBCefApp.isSupported()) {
            return true;
        }
        try {
            if (!file.exists()) {
                return false;
            }
            KeyFMap keyFMap = file.get();
            Long questionId = keyFMap.get(KeyConstant.QUESTION_ID_KEY);
            if (questionId == null) {
                return false;
            }
        } catch (Throwable e) {
            return false;
        }
        return true;
    }

    @NotNull
    @Override
    public AsyncFileEditorProvider.Builder createEditorAsync(@NotNull Project project, @NotNull VirtualFile file) {

        final AsyncFileEditorProvider.Builder firstBuilder = getBuilderFromEditorProvider(this.myFirstProvider, project, file);
        final AsyncFileEditorProvider.Builder secondBuilder = getBuilderFromEditorProvider(this.mySecondProvider, project, file);
        return new AsyncFileEditorProvider.Builder() {
            @NotNull
            @Override
            public FileEditor build() {
                return createSplitEditor(firstBuilder.build(), secondBuilder.build());
            }
        };
    }

    @Override
    protected FileEditor createSplitEditor(@NotNull FileEditor firstEditor, @NotNull FileEditor secondEditor) {

        return new EditorWithPreview((TextEditor) secondEditor, firstEditor);
    }


}

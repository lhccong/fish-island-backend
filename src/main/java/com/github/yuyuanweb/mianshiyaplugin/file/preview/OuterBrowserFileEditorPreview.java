package com.github.yuyuanweb.mianshiyaplugin.file.preview;

import com.github.yuyuanweb.mianshiyaplugin.config.GlobalState;
import com.github.yuyuanweb.mianshiyaplugin.constant.KeyConstant;
import com.github.yuyuanweb.mianshiyaplugin.constant.TextConstant;
import com.github.yuyuanweb.mianshiyaplugin.constant.ViewConstant;
import com.github.yuyuanweb.mianshiyaplugin.model.enums.WebTypeEnum;
import com.github.yuyuanweb.mianshiyaplugin.model.response.User;
import com.github.yuyuanweb.mianshiyaplugin.file.provider.BrowserFileEditorProvider;
import com.github.yuyuanweb.mianshiyaplugin.utils.FileUtils;
import com.github.yuyuanweb.mianshiyaplugin.view.LoginPanel;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.fileEditor.*;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.project.ProjectManager;
import com.intellij.openapi.util.Disposer;
import com.intellij.openapi.util.UserDataHolderBase;
import com.intellij.openapi.vfs.LocalFileSystem;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.ui.components.JBLabel;
import com.intellij.ui.components.JBScrollPane;
import com.intellij.util.keyFMap.KeyFMap;
import com.intellij.util.ui.JBUI;
import com.intellij.util.ui.components.BorderLayoutPanel;
import lombok.Getter;
import org.jetbrains.annotations.Nls;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;
import java.beans.PropertyChangeListener;
import java.io.File;

/**
 * 内嵌浏览器外层 文件编辑器
 *
 * @author pine
 */
public class OuterBrowserFileEditorPreview extends UserDataHolderBase implements FileEditor {

    private final Project project;

    @Override
    public @Nullable FileEditorLocation getCurrentLocation() {
        return null;
    }

    private final VirtualFile file;
    private JBScrollPane jbScrollPane;
    private BorderLayoutPanel myComponent;
    private FileEditor fileEditor;

    @Getter
    private FileEditor newEditor;

    private boolean isLoad = false;

    private final WebTypeEnum webTypeEnum;

    public OuterBrowserFileEditorPreview(Project project, VirtualFile file, WebTypeEnum webTypeEnum) {
        this.project = project;
        this.file = file;
        this.webTypeEnum = webTypeEnum;
        if (!WebTypeEnum.COMMENT.equals(webTypeEnum)) {
            initComponent();
        }
    }

    @Override
    public @NotNull JComponent getComponent() {
        if (myComponent == null) {
            myComponent = JBUI.Panels.simplePanel();
            jbScrollPane = new JBScrollPane(JBScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED, JBScrollPane.HORIZONTAL_SCROLLBAR_AS_NEEDED);
            myComponent.addToCenter(jbScrollPane);
            // COMMENT 类型不会在构造函数中初始化
            if (isLoad && WebTypeEnum.COMMENT.equals(webTypeEnum)) {
                initComponent();
            }
        }
        return myComponent;
    }

    private void initComponent() {
        isLoad = true;
        ApplicationManager.getApplication().executeOnPooledThread(() -> {
            try {
                KeyFMap keyFMap = file.get();
                Long questionId = keyFMap.get(KeyConstant.QUESTION_ID_KEY);
                Long questionBankId = keyFMap.get(KeyConstant.QUESTION_BANK_ID_KEY);
                ApplicationManager.getApplication().invokeLater(() -> {
                    if (questionId == null) {
                        jbScrollPane.setViewportView(new JBLabel(TextConstant.LOGIN));
                        return;
                    }
                    this.openArticle(questionId, questionBankId);
                });
            } catch (Exception e) {
                e.printStackTrace();
            }
        });
    }

    private void openArticle(Long questionId, Long questionBankId) {
        ApplicationManager.getApplication().executeOnPooledThread(() -> {
            User loginUser = GlobalState.getInstance().getSavedUser();
            ApplicationManager.getApplication().invokeLater(() -> {
                if (loginUser == null && WebTypeEnum.QUESTION.equals(webTypeEnum)) {
                    LoginPanel loginPanel = new LoginPanel(ProjectManager.getInstance().getDefaultProject());
                    loginPanel.showAndGet();
                }
                ApplicationManager.getApplication().executeOnPooledThread(() -> {
                    File file = null;
                    file = FileUtils.openArticle(project, false);
                    if (!file.exists()) {
                        myComponent.addToCenter(new JBLabel("No solution"));
                    } else {
                        VirtualFile vf = LocalFileSystem.getInstance().refreshAndFindFileByIoFile(file);
                        if (vf != null) {
                            KeyFMap map = KeyFMap.EMPTY_MAP.plus(KeyConstant.QUESTION_ID_KEY, questionId);
                            map = map.plus(KeyConstant.WEB_TYPE_KEY, webTypeEnum);
                            map = map.plus(KeyConstant.QUESTION_BANK_ID_KEY, questionBankId);
                            vf.set(map);
                            BrowserFileEditorProvider contentProvider = new BrowserFileEditorProvider();
                            newEditor = contentProvider.createEditor(project, vf);
                            ApplicationManager.getApplication().invokeLater(() -> {
                                if (fileEditor != null) {
                                    jbScrollPane.setViewportView(new JBLabel("Loading......"));
                                    FileEditor temp = fileEditor;
                                    Disposer.dispose(temp);
                                }
                                fileEditor = newEditor;
                                Disposer.register(this, fileEditor);
                                BorderLayoutPanel browserComponent = JBUI.Panels.simplePanel(newEditor.getComponent());
                                browserComponent.addToCenter(newEditor.getComponent());
                                jbScrollPane.setViewportView(browserComponent);
                            });
                        }
                    }
                });
            });
        });
    }

    @Override
    public @Nullable JComponent getPreferredFocusedComponent() {
        return myComponent;
    }

    @Override
    public @Nls(capitalization = Nls.Capitalization.Title) @NotNull String getName() {
        return ViewConstant.BEST_QUESTION_ANSWER_PREVIEW;
    }

    @Override
    public void setState(@NotNull FileEditorState state) {
        if (state instanceof ConvergePreview.TabFileEditorState) {
            if (!isLoad && ((ConvergePreview.TabFileEditorState) state).isLoad()) {
                initComponent();
            }
        } else if (state instanceof ConvergePreview.TabSelectFileEditorState) {
            if (!isLoad) {
                initComponent();
            }
        }
    }

    @Override
    public boolean isModified() {
        return false;
    }

    @Override
    public boolean isValid() {
        return false;
    }

    @Override
    public void addPropertyChangeListener(@NotNull PropertyChangeListener listener) {

    }

    @Override
    public void removePropertyChangeListener(@NotNull PropertyChangeListener listener) {

    }

    @Override
    public void dispose() {
        if (fileEditor != null) {
            Disposer.dispose(fileEditor);
        }
    }

    @Override
    public @Nullable VirtualFile getFile() {
        if (fileEditor != null) {
            return fileEditor.getFile();
        } else {
            return null;
        }
    }
}

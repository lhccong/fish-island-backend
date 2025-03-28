package com.github.yuyuanweb.mianshiyaplugin.file.preview;

import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.util.ObjUtil;
import com.github.yuyuanweb.mianshiyaplugin.config.ApiConfig;
import com.github.yuyuanweb.mianshiyaplugin.constant.CommonConstant;
import com.github.yuyuanweb.mianshiyaplugin.constant.KeyConstant;
import com.github.yuyuanweb.mianshiyaplugin.constant.ViewConstant;
import com.github.yuyuanweb.mianshiyaplugin.model.common.BaseResponse;
import com.github.yuyuanweb.mianshiyaplugin.model.dto.DoQuestionInfoVO;
import com.github.yuyuanweb.mianshiyaplugin.utils.ThemeUtil;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.editor.Document;
import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.editor.EditorFactory;
import com.intellij.openapi.fileEditor.*;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.Messages;
import com.intellij.openapi.ui.Splitter;
import com.intellij.openapi.util.Disposer;
import com.intellij.openapi.util.UserDataHolderBase;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.openapi.wm.IdeFocusManager;
import com.intellij.openapi.wm.WindowManager;
import com.intellij.pom.Navigatable;
import com.intellij.ui.tabs.TabInfo;
import com.intellij.ui.tabs.TabsListener;
import com.intellij.ui.tabs.impl.JBEditorTabs;
import com.intellij.util.keyFMap.KeyFMap;
import com.intellij.util.ui.JBUI;
import com.intellij.util.ui.components.BorderLayoutPanel;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.Getter;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import retrofit2.Response;

import javax.swing.*;
import java.awt.event.ActionListener;
import java.beans.PropertyChangeListener;
import java.io.IOException;
import java.util.List;

import static com.github.yuyuanweb.mianshiyaplugin.constant.SearchConstant.QUESTION_BANK_NULL_ID;


/**
 * 聚合多个编辑器 文件编辑器
 *
 * @author pine
 */
public class ConvergePreview extends UserDataHolderBase implements TextEditor {

    private static final Logger logger = Logger.getInstance(ConvergePreview.class);

    private final Project project;
    private final FileEditor[] fileEditors;
    private final String[] names;
    private final TabInfo[] tabInfos;
    private final VirtualFile file;
    private final Document document;
    private final Editor myEditor;


    private JComponent myComponent;
    private JBEditorTabs jbEditorTabs;

    public ConvergePreview(@NotNull FileEditor[] fileEditors, String[] names, Project project, VirtualFile file) {
        this.project = project;
        this.fileEditors = fileEditors;
        this.names = names;
        this.tabInfos = new TabInfo[names.length];
        this.file = file;
        logger.warn("mianshiya log before create document");
        document = FileDocumentManager.getInstance().getDocument(file);
        logger.warn("mianshiya log after create document: " + document);

        // 如果没有现有的编辑器，创建一个新的 EditorImpl 实例
        myEditor = EditorFactory.getInstance().createEditor(document, project);
        logger.warn("mianshiya log after create myEditor: " + myEditor);
    }

    @Override
    public @NotNull JComponent getComponent() {
        if (myComponent == null) {

            // 创建第一个面板
            JComponent firstEditorComponent = fileEditors[0].getComponent();
            BorderLayoutPanel firstComponent = JBUI.Panels.simplePanel(firstEditorComponent);

            jbEditorTabs = new JBEditorTabs(project, IdeFocusManager.getInstance(project), this);
            BorderLayoutPanel secondComponent = JBUI.Panels.simplePanel(jbEditorTabs);

            // 为第二和第三个内容创建标签
            for (int i = 1; i < fileEditors.length; i++) {
                TabInfo tabInfo = new TabInfo(fileEditors[i].getComponent());
                tabInfo.setText(names[i]);
                tabInfos[i] = tabInfo;
                jbEditorTabs.addTab(tabInfo);
            }

            jbEditorTabs.addListener(new TabsListener() {
                @Override
                public void selectionChanged(TabInfo oldSelection, TabInfo newSelection) {
                    // 从 1 开始，因为 0 已经被上部分使用
                    for (int i = 1; i < names.length; i++) {
                        if (newSelection.getText().equals(names[i])) {
                            fileEditors[i].setState(TabFileEditorState.TabFileEditorLoadState);
                            break;
                        }
                    }
                }
            });

            // 使用 Splitter 来分隔上下部分
            // false 表示垂直分隔，0.2f 表示初始比例
            Long questionBankId = file.get().get(KeyConstant.QUESTION_BANK_ID_KEY);
            boolean nullQuestionBankId = QUESTION_BANK_NULL_ID.equals(questionBankId);
            JFrame frame = WindowManager.getInstance().getFrame(project);
            float proportion = 0;
            if (frame != null && !nullQuestionBankId) {
                int height = frame.getHeight();
                // 处理获取的高度
                proportion = 200.0f / height;
            } else {
                proportion = 0.2f;
            }
            Splitter splitter = new Splitter(true, proportion);
            splitter.setFirstComponent(firstComponent);
            splitter.setSecondComponent(secondComponent);

            myComponent = JBUI.Panels.simplePanel(splitter);

            // 如果 questionBankId 为 null，则不需要下一题按钮，直接返回组件
            if (nullQuestionBankId) {
                return myComponent;
            }

            // 下一题按钮
            String nextQuestionText = "下一题";
            JButton nextQuestionButton = new JButton(nextQuestionText);
            secondComponent.addToBottom(nextQuestionButton);
            // 监听按钮事件
            ActionListener nextQuestionListener = event -> ApplicationManager.getApplication().executeOnPooledThread(() -> {
                Long questionId = file.get().get(KeyConstant.QUESTION_ID_KEY);
                if (questionBankId == null || questionId == null) {
                    logger.warn("questionBankId 或 questionId 为空，questionBankId: " + questionBankId + ", questionId: " + questionId);
                    return;
                }
                BaseResponse<DoQuestionInfoVO> questionInfo = this.getQuestionInfo(questionBankId, questionId);
                if (ObjUtil.hasNull(questionInfo, questionInfo.getData())) {
                    logger.warn("questionInfo 或 questionInfo.getData() 为空，questionInfo: " + questionInfo + ", questionInfo.getData(): " + questionInfo.getData());
                    return;
                }
                DoQuestionInfoVO questionInfoVO = questionInfo.getData();
                Integer currentQuestionIndex = questionInfoVO.getCurrentQuestionIndex();
                List<Long> questionIdList = questionInfoVO.getQuestionIdList();
                if (ObjUtil.hasNull(currentQuestionIndex, questionIdList)) {
                    logger.warn("currentQuestionIndex 或 questionIdList 为空，currentQuestionIndex: " + currentQuestionIndex + ", questionIdList: " + questionIdList);
                    return;
                }
                int curIndex = currentQuestionIndex + 1;
                if (curIndex >= CollUtil.size(questionIdList)) {
                    logger.warn("curIndex 超出最大值，curIndex: " + curIndex + ", CollUtil.size(questionIdList.size()): " + CollUtil.size(questionIdList));
                    // 使用 invokeLater 确保在 EDT 线程中执行 UI 更新
                    ApplicationManager.getApplication().invokeLater(() -> {
                        Messages.showWarningDialog("已经是当前题库的最后一题啦，换个题库继续刷吧！", "没有下一题");
                    });
                    return;
                }
                Long nextQuestionId = questionIdList.get(curIndex);

                KeyFMap map = file.get().plus(KeyConstant.QUESTION_ID_KEY, nextQuestionId);
                file.set(map);

                String theme = ThemeUtil.getTheme();
                // 循环 问题、答案、评论 三个文件编辑器，更新题目
                for (FileEditor fileEditor : fileEditors) {
                    BrowserFileEditor browserFileEditor = (BrowserFileEditor) ((OuterBrowserFileEditorPreview) fileEditor).getNewEditor();
                    if (browserFileEditor == null) {
                        logger.warn("browserFileEditor 为空");
                        return;
                    }
                    String webType = browserFileEditor.getWebTypeEnum().getValue();
                    String url = String.format(CommonConstant.PLUGIN_QD, nextQuestionId, webType, theme);
                    // 使用 invokeLater 确保在 EDT 线程中执行 UI 更新
                    ApplicationManager.getApplication().invokeLater(() -> {
                        browserFileEditor.getJbCefBrowser().loadURL(url);
                    });
                }
            });

            nextQuestionButton.addActionListener(nextQuestionListener);
        }
        return myComponent;
    }

    /**
     * 获取刷题信息
     */
    public BaseResponse<DoQuestionInfoVO> getQuestionInfo(long questionBankId, long questionId) {
        Response<BaseResponse<DoQuestionInfoVO>> response = null;
        try {
            response = ApiConfig.mianShiYaApi.getDoQuestionInfo(questionBankId, questionId)
                    .execute();
        } catch (IOException e) {
            logger.error("获取题目信息失败: {}", e.getMessage());
        }
        if (response != null && response.isSuccessful()) {
            return response.body();
        }
        return null;
    }

    @Override
    public @Nullable JComponent getPreferredFocusedComponent() {
        return fileEditors[0].getPreferredFocusedComponent();
    }

    @Override
    public @NotNull String getName() {
        return ViewConstant.CONVERGE_PREVIEW;
    }

    @Override
    public void setState(@NotNull FileEditorState state) {
        if (state instanceof TabSelectFileEditorState) {
            if (jbEditorTabs != null) {
                String name = ((TabSelectFileEditorState) state).getName();
                for (int i = 0; i < names.length; i++) {
                    if (name.equals(names[i])) {
                        fileEditors[i].setState(state);
                        jbEditorTabs.select(tabInfos[i], true);
                    }
                }
            }
        }
    }

    @Override
    public boolean isModified() {
        return false;
    }

    @Override
    public boolean isValid() {
        return true;
    }

    @Override
    public void addPropertyChangeListener(@NotNull PropertyChangeListener listener) {

    }

    @Override
    public void removePropertyChangeListener(@NotNull PropertyChangeListener listener) {

    }

    @Override
    public @Nullable FileEditorLocation getCurrentLocation() {
        return fileEditors[0].getCurrentLocation();
    }

    @Override
    public void dispose() {
        for (FileEditor fileEditor : fileEditors) {
            Disposer.dispose(fileEditor);
        }
        if (myEditor != null) {
            EditorFactory.getInstance().releaseEditor(myEditor);
        }
    }


    @Override
    public @Nullable VirtualFile getFile() {
        return file;
    }

    @Override
    public Editor getEditor() {
        logger.warn("mianshiya log myEditor " + myEditor);
        return myEditor;
    }

    @Override
    public boolean canNavigateTo(@NotNull Navigatable navigatable) {
        return false;
    }

    @Override
    public void navigateTo(@NotNull Navigatable navigatable) {

    }

    @Data
    @AllArgsConstructor
    public static class TabFileEditorState implements FileEditorState {

        private boolean load;

        @Override
        public boolean canBeMergedWith(@NotNull FileEditorState otherState, @NotNull FileEditorStateLevel level) {
            return false;
        }

        public static TabFileEditorState TabFileEditorLoadState = new TabFileEditorState(true);

    }

    @Getter
    public static class TabSelectFileEditorState implements FileEditorState {

        private String name;

        private String childrenState;

        @Override
        public boolean canBeMergedWith(@NotNull FileEditorState otherState, @NotNull FileEditorStateLevel level) {
            return false;
        }

    }

}

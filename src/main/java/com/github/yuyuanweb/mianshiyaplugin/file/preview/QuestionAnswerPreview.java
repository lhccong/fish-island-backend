package com.github.yuyuanweb.mianshiyaplugin.file.preview;

import com.github.yuyuanweb.mianshiyaplugin.model.enums.ErrorCode;
import com.github.yuyuanweb.mianshiyaplugin.constant.KeyConstant;
import com.github.yuyuanweb.mianshiyaplugin.constant.TextConstant;
import com.github.yuyuanweb.mianshiyaplugin.constant.ViewConstant;
import com.github.yuyuanweb.mianshiyaplugin.model.common.BaseResponse;
import com.github.yuyuanweb.mianshiyaplugin.model.common.Page;
import com.github.yuyuanweb.mianshiyaplugin.model.dto.QuestionAnswerQueryRequest;
import com.github.yuyuanweb.mianshiyaplugin.model.response.QuestionAnswer;
import com.github.yuyuanweb.mianshiyaplugin.model.response.User;
import com.github.yuyuanweb.mianshiyaplugin.utils.FileUtils;
import com.github.yuyuanweb.mianshiyaplugin.utils.PanelUtil;
import com.github.yuyuanweb.mianshiyaplugin.file.provider.QuestionProvider;
import com.intellij.icons.AllIcons;
import com.intellij.openapi.actionSystem.*;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.fileEditor.*;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.Disposer;
import com.intellij.openapi.util.UserDataHolderBase;
import com.intellij.openapi.vfs.LocalFileSystem;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.ui.JBSplitter;
import com.intellij.ui.components.JBLabel;
import com.intellij.ui.components.JBPanel;
import com.intellij.ui.components.JBScrollPane;
import com.intellij.ui.table.JBTable;
import com.intellij.util.keyFMap.KeyFMap;
import com.intellij.util.ui.JBUI;
import com.intellij.util.ui.components.BorderLayoutPanel;
import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.jetbrains.annotations.Nls;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;
import javax.swing.table.AbstractTableModel;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.beans.PropertyChangeListener;
import java.io.File;
import java.io.IOException;
import java.util.List;

import static com.github.yuyuanweb.mianshiyaplugin.config.ApiConfig.mianShiYaApi;
import static com.intellij.openapi.actionSystem.ActionPlaces.TEXT_EDITOR_WITH_PREVIEW;

/**
 * 回答 文件编辑器
 * 已废弃，不再使用
 *
 * @author pine
 */
public class QuestionAnswerPreview extends UserDataHolderBase implements FileEditor {

    private static final String MY_PROPORTION_KEY = ViewConstant.QUESTION_ANSWER_PREVIEW + ViewConstant.SPLITTER;
    private final Project project;
    private final VirtualFile file;

    private BorderLayoutPanel myComponent;
    private FileEditor fileEditor;

    private boolean isLoad = false;

    private List<QuestionAnswer> questionAnswerList;
    private JBTable table;

    private JBSplitter mySplitter;
    private SplitFileEditor.SplitEditorLayout myLayout = SplitFileEditor.SplitEditorLayout.FIRST;

    public QuestionAnswerPreview(Project project, VirtualFile file) {
        this.project = project;
        this.file = file;
    }

    @Override
    public @NotNull JComponent getComponent() {
        if (myComponent == null) {
            mySplitter = new JBSplitter(false, 0.5f);
            mySplitter.setSplitterProportionKey(MY_PROPORTION_KEY);
            mySplitter.setDividerWidth(3);
            myComponent = JBUI.Panels.simplePanel();
            myComponent.addToCenter(mySplitter);
            if (isLoad) {
                initComponent(null);
            }
        }
        return myComponent;
    }

    private void initComponent(String defaultSlug) {
        isLoad = true;
        ApplicationManager.getApplication().executeOnPooledThread(() -> {
            KeyFMap keyFMap = file.get();
            Long questionId = keyFMap.get(KeyConstant.QUESTION_ID_KEY);

            QuestionAnswerQueryRequest questionAnswerQueryRequest = new QuestionAnswerQueryRequest();
            questionAnswerQueryRequest.setPageSize(10);
            questionAnswerQueryRequest.setQuestionId(questionId);
            BaseResponse<Page<QuestionAnswer>> baseResponse = null;
            try {
                baseResponse = mianShiYaApi.listQuestionAnswerByQuestionId(questionAnswerQueryRequest).execute().body();
            } catch (IOException ignored) {
            }

            BaseResponse<Page<QuestionAnswer>> finalBaseResponse = baseResponse;
            ApplicationManager.getApplication().invokeLater(() -> {
                JBLabel loadingLabel = new JBLabel("Loading......");
                mySplitter.setFirstComponent(loadingLabel);
                try {
                    if (finalBaseResponse.getCode() == ErrorCode.NO_VIP_AUTH_ERROR.getCode()) {
                        JBPanel<?> needVipPanel = PanelUtil.getNeedVipPanel();
                        myComponent.addToTop(needVipPanel);
                        return;
                    }
                    questionAnswerList = finalBaseResponse.getData().getRecords();
                    if (CollectionUtils.isEmpty(questionAnswerList)) {
                        myComponent.addToTop(new JBLabel(TextConstant.NO_SOLUTION));
                    } else {
                        table = new JBTable(new TableModel(questionAnswerList));
                        table.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
                        table.getTableHeader().setReorderingAllowed(false);
                        table.setRowSelectionAllowed(true);

                        table.addMouseListener(new MouseAdapter() {
                            @Override
                            public void mouseClicked(MouseEvent e) {
                                if (e.getButton() == MouseEvent.BUTTON1 && e.getClickCount() == 2) {
                                    int row = table.getSelectedRow();
                                    openSelectedQuestion(questionAnswerList, row);
                                }
                            }
                        });
                        table.addKeyListener(new KeyAdapter() {
                            @Override
                            public void keyTyped(KeyEvent e) {
                                if (e.getKeyChar() == KeyEvent.VK_ENTER) {
                                    int row = table.getSelectedRow();
                                    openSelectedQuestion(questionAnswerList, row);
                                }
                            }
                        });
                        JBScrollPane jbScrollPane = new JBScrollPane(table, JBScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED, JBScrollPane.HORIZONTAL_SCROLLBAR_NEVER);
                        // 确保表格充满视口
                        mySplitter.setFirstComponent(jbScrollPane);

                        if (StringUtils.isNotBlank(defaultSlug)) {
                            for (int i = 0; i < questionAnswerList.size(); i++) {
                                if (questionAnswerList.get(i).getContent().equals(defaultSlug)) {
                                    openSelectedQuestion(questionAnswerList, i);
                                    table.setRowSelectionInterval(i, i);
                                    table.scrollRectToVisible(table.getCellRect(i, 0, true));
                                }
                            }
                        }
                    }
                } catch (Exception e) {
                    myLayout = SplitFileEditor.SplitEditorLayout.FIRST;
                    adjustEditorsVisibility();
                    mySplitter.setFirstComponent(new JBLabel(e.getMessage()));
                } finally {
                    mySplitter.remove(loadingLabel);
                }
            });
        });
    }

    private void openSelectedQuestion(List<QuestionAnswer> solutionList, int row) {
        if (row < 0 || solutionList == null || row >= solutionList.size()) {
            return;
        }
        QuestionAnswer solution = solutionList.get(row);
        try {
            myLayout = SplitFileEditor.SplitEditorLayout.SPLIT;
            adjustEditorsVisibility();
            openArticle(solution);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void openArticle(QuestionAnswer solution) {
        // ApplicationManager.getApplication().executeOnPooledThread(() -> {
        ApplicationManager.getApplication().invokeLater(() -> {
            File file = FileUtils.openArticle(project, false);
            if (!file.exists()) {
                mySplitter.setSecondComponent(new JBLabel("No solution"));
            } else {
                VirtualFile vf = LocalFileSystem.getInstance().refreshAndFindFileByIoFile(file);
                QuestionProvider contentProvider = new QuestionProvider();
                FileEditor newEditor = contentProvider.createEditor(project, vf);
                if (fileEditor != null) {
                    mySplitter.setSecondComponent(new JBLabel("Loading......"));
                    FileEditor temp = fileEditor;
                    Disposer.dispose(temp);
                }
                fileEditor = newEditor;
                Disposer.register(this, fileEditor);
                BorderLayoutPanel secondComponent = JBUI.Panels.simplePanel(newEditor.getComponent());
                secondComponent.addToTop(createToolbarWrapper(newEditor.getComponent()));
                mySplitter.setSecondComponent(secondComponent);
            }
            // });
        });
    }

    private SplitEditorToolbar createToolbarWrapper(JComponent targetComponentForActions) {
        DefaultActionGroup actionGroup = new DefaultActionGroup(new AnAction("Close", "Close", AllIcons.Actions.Close) {
            @Override
            public void actionPerformed(@NotNull AnActionEvent e) {
                myLayout = SplitFileEditor.SplitEditorLayout.FIRST;
                adjustEditorsVisibility();
            }
        });
        ActionToolbar actionToolbar = ActionManager.getInstance().createActionToolbar("Solution" + TEXT_EDITOR_WITH_PREVIEW, actionGroup, true);
        actionToolbar.setTargetComponent(targetComponentForActions);
        return new SplitEditorToolbar(null, actionToolbar);
    }

    private void adjustEditorsVisibility() {
        if (mySplitter.getFirstComponent() != null) {
            mySplitter.getFirstComponent().setVisible(myLayout == SplitFileEditor.SplitEditorLayout.FIRST || myLayout == SplitFileEditor.SplitEditorLayout.SPLIT);
        }

        if (mySplitter.getSecondComponent() != null) {
            mySplitter.getSecondComponent().setVisible(myLayout == SplitFileEditor.SplitEditorLayout.SECOND || myLayout == SplitFileEditor.SplitEditorLayout.SPLIT);
        }
    }

    @Override
    public @Nullable JComponent getPreferredFocusedComponent() {
        return myComponent;
    }

    @Override
    public @Nls(capitalization = Nls.Capitalization.Title) @NotNull String getName() {
        return ViewConstant.QUESTION_ANSWER_PREVIEW;
    }

    @Override
    public void setState(@NotNull FileEditorState state) {
        if (state instanceof ConvergePreview.TabFileEditorState) {
            if (!isLoad && ((ConvergePreview.TabFileEditorState) state).isLoad()) {
                initComponent(null);
            }
        } else if (state instanceof ConvergePreview.TabSelectFileEditorState) {
            String slug = ((ConvergePreview.TabSelectFileEditorState) state).getChildrenState();
            if (!isLoad) {
                initComponent(slug);
            } else if (CollectionUtils.isNotEmpty(questionAnswerList)) {
                for (int i = 0; i < questionAnswerList.size(); i++) {
                    if (questionAnswerList.get(i).getContent().equals(slug)) {
                        openSelectedQuestion(questionAnswerList, i);
                        table.setRowSelectionInterval(i, i);
                        break;
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


    private static class TableModel extends AbstractTableModel {

        String[] columnNames = {"用户名", "回答"};

        String[][] data;

        public TableModel(List<QuestionAnswer> questionAnswers) {
            data = new String[questionAnswers.size()][columnNames.length];
            for (int i = 0, j = questionAnswers.size(); i < j; i++) {
                QuestionAnswer s = questionAnswers.get(i);
                data[i][1] = s.getContent();
                User user = s.getUser();
                if (user != null) {
                    data[i][0] = user.getUserName();
                }
            }
        }

        @Override
        public int getRowCount() {
            return data.length;
        }

        @Override
        public int getColumnCount() {
            return columnNames.length;
        }

        @Override
        public Object getValueAt(int rowIndex, int columnIndex) {
            return data[rowIndex][columnIndex];
        }

        @Override
        public String getColumnName(int column) {
            return columnNames[column];
        }
    }

    @Override
    public @Nullable FileEditorLocation getCurrentLocation() {
        return null;
    }

}

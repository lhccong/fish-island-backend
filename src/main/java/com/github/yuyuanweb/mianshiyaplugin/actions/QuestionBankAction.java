package com.github.yuyuanweb.mianshiyaplugin.actions;

import com.github.yuyuanweb.mianshiyaplugin.config.ApiConfig;
import com.github.yuyuanweb.mianshiyaplugin.constant.PageConstant;
import com.github.yuyuanweb.mianshiyaplugin.constant.SearchConstant;
import com.github.yuyuanweb.mianshiyaplugin.constant.TextConstant;
import com.github.yuyuanweb.mianshiyaplugin.model.common.Page;
import com.github.yuyuanweb.mianshiyaplugin.model.common.PageRequest;
import com.github.yuyuanweb.mianshiyaplugin.model.dto.QuestionBankCategoryBankQueryRequest;
import com.github.yuyuanweb.mianshiyaplugin.model.response.QuestionBank;
import com.github.yuyuanweb.mianshiyaplugin.model.response.QuestionBankCategory;
import com.github.yuyuanweb.mianshiyaplugin.utils.ContentUtil;
import com.github.yuyuanweb.mianshiyaplugin.utils.PanelUtil;
import com.github.yuyuanweb.mianshiyaplugin.view.MTabModel;
import com.github.yuyuanweb.mianshiyaplugin.view.QuestionListManager;
import com.intellij.openapi.actionSystem.AnAction;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.project.Project;
import com.intellij.ui.Gray;
import com.intellij.ui.JBColor;
import com.intellij.ui.components.*;
import com.intellij.ui.table.JBTable;
import com.intellij.util.ui.WrapLayout;
import org.jetbrains.annotations.NotNull;

import java.util.List;

import javax.swing.*;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.io.IOException;

/**
 * @author pine
 */
public class QuestionBankAction extends AnAction {

    private JBPanel<?> paginationPanel;
    private JBPanel<?> labelPanel;
    private MTabModel tableModel;
    private JBPanel<?> mainPanel;

    private final int[] currentPage = new int[]{1};
    private final QuestionBankCategoryBankQueryRequest queryRequest = new QuestionBankCategoryBankQueryRequest();

    public QuestionBankAction() {
    }

    public QuestionBankAction(String text, Icon icon) {
        super(text, text, icon);
    }

    public QuestionBankAction(JBPanel<?> mainPanel) {
        this.mainPanel = mainPanel;
    }

    @Override
    public void actionPerformed(@NotNull AnActionEvent anActionEvent) {
        // 创建新的 tab 页
        JBPanel<?> tabPanel = new JBPanel<>(new BorderLayout());

        Project project = anActionEvent.getProject();
        if (mainPanel != null) {
            mainPanel.add(tabPanel, BorderLayout.CENTER);
        } else {
            ContentUtil.createContent(tabPanel, TextConstant.QUESTION_BANK, false, project);
        }

        // 标签栏
        this.loadLabelPanel();
        tabPanel.add(labelPanel, BorderLayout.NORTH);

        // 数据表格
        this.loadDataTable(tabPanel, project);

        // 分页条
        paginationPanel = new JBPanel<>(new GridBagLayout());
        tabPanel.add(paginationPanel, BorderLayout.SOUTH);
    }

    private Page<QuestionBank> fetchDataFromApi(QuestionBankCategoryBankQueryRequest queryRequest) {
        if (queryRequest == null) {
            throw new IllegalArgumentException("queryRequest cannot be null");
        }
        try {
            if (SearchConstant.DEFAULT_QUESTION_BANK_CATEGORY_ID.equals(queryRequest.getQuestionBankCategoryId())) {
                return ApiConfig.mianShiYaApi.listQuestionBankVoByPage(queryRequest).execute().body().getData();
            } else {
                return ApiConfig.mianShiYaApi.getQuestionBankList(queryRequest).execute().body().getData();
            }
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * 加载顶部标签数据
     */
    private void loadLabelPanel() {
        labelPanel = new JBPanel<>(new WrapLayout(FlowLayout.LEFT, 5, 5));
        // 维护当前选中的标签
        // 通过数组的方式来持有可变的对象引用
        JBLabel[] selectedLabel = {null};
        ApplicationManager.getApplication().executeOnPooledThread(() -> {
            List<QuestionBankCategory> tagList;
            try {
                tagList = ApiConfig.mianShiYaApi.listQuestionBankCategory(new PageRequest()).execute().body().getData();
            } catch (IOException e) {
                return;
            }
            // 自定义淡化颜色
            // 比 LIGHT_GRAY 更淡的灰色
            JBColor customLightGray = new JBColor(Gray._220, Gray._80);
            // 比 GRAY 更淡的灰色
            JBColor customGray = new JBColor(Gray._180, Gray._40);

            ApplicationManager.getApplication().invokeLater(() -> {
                for (QuestionBankCategory tag : tagList) {
                    JBLabel label = new JBLabel(tag.getName());
                    label.setBorder(BorderFactory.createEmptyBorder(5, 10, 5, 10));
                    label.setOpaque(true);
                    // 使用自定义淡灰色
                    label.setBackground(customLightGray);
                    label.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));

                    // 点击事件
                    label.addMouseListener(new MouseAdapter() {
                        @Override
                        public void mouseClicked(MouseEvent e) {
                            if (selectedLabel[0] != null && selectedLabel[0] != label) {
                                // 取消上一个高亮的标签
                                selectedLabel[0].setBackground(customLightGray);
                            }

                            if (label == selectedLabel[0]) {
                                // 如果当前点击的是已经选中的标签，取消高亮
                                label.setBackground(customLightGray);
                                // 取消筛选条件
                                queryRequest.setQuestionBankCategoryId(SearchConstant.DEFAULT_QUESTION_BANK_CATEGORY_ID);
                                searchAndLoadData(queryRequest);
                                // 清空选中标签
                                selectedLabel[0] = null;
                            } else {
                                // 高亮当前标签
                                label.setBackground(customGray);
                                currentPage[0] = PageConstant.FIRST_PAGE;
                                queryRequest.setQuestionBankCategoryId(tag.getId());
                                queryRequest.setCurrent(PageConstant.FIRST_PAGE);
                                searchAndLoadData(queryRequest);
                                // 更新当前选中标签
                                selectedLabel[0] = label;
                            }
                        }

                        @Override
                        public void mouseEntered(MouseEvent e) {
                            if (label != selectedLabel[0]) {
                                // 鼠标悬浮效果，仅当标签未被选中时生效
                                label.setBackground(customGray);
                            }
                        }

                        @Override
                        public void mouseExited(MouseEvent e) {
                            if (label != selectedLabel[0]) {
                                // 鼠标离开时恢复原始背景色，仅当标签未被选中时生效
                                label.setBackground(customLightGray);
                            }
                        }
                    });

                    labelPanel.add(label);
                }
            });
        });
    }

    private void searchAndLoadData(QuestionBankCategoryBankQueryRequest queryRequest) {
        if (tableModel == null) {
            return;
        }
        // 清空现有表格数据
        ApplicationManager.getApplication().executeOnPooledThread(() -> {
            Page<QuestionBank> data = this.fetchDataFromApi(queryRequest);
            ApplicationManager.getApplication().invokeLater(() -> {
                tableModel.setRowCount(0);

                // 添加新数据到表格模型
                for (QuestionBank row : data.getRecords()) {
                    tableModel.addRow(new Object[]{row.getId().toString(), row.getTitle(), row.getTagList()});
                }

                // 重新渲染表格
                tableModel.fireTableDataChanged();
                PanelUtil.updatePaginationPanel(paginationPanel, data.getTotal(), currentPage, this::loadPage);
            });
        });
    }

    private void loadPage(int page, int pageSize) {
        // 实现分页逻辑，并更新表格数据
        queryRequest.setPageSize(pageSize);
        queryRequest.setCurrent(page);
        this.searchAndLoadData(queryRequest);
    }

    private void loadDataTable(JBPanel<?> tabPanel, Project project) {
        ApplicationManager.getApplication().executeOnPooledThread(() -> {
            Page<QuestionBank> data = this.fetchDataFromApi(queryRequest);
            // 创建表格数据模型
            ApplicationManager.getApplication().invokeLater(() -> {
                tableModel = new MTabModel();
                tableModel.addColumn("id");
                tableModel.addColumn("题库名称");
                tableModel.addColumn("所属分类");

                // 将数据添加到表格模型
                for (QuestionBank row : data.getRecords()) {
                    tableModel.addRow(new Object[]{row.getId().toString(), row.getTitle(), row.getTagList()});
                }

                JBTable table = PanelUtil.createTablePanel(tableModel, (tempTable, mouseEvent) -> {
                    int selectedRow = tempTable.getSelectedRow();
                    if (selectedRow != -1) {
                        // 获取选中行的数据
                        String id = (String) tempTable.getValueAt(selectedRow, 0);

                        // 打开包含该行数据的新选项卡
                        QuestionListManager questionListManager = new QuestionListManager();
                        questionListManager.addQuestionTab(Long.valueOf(id), project);
                    }
                }, 2);
                // 将表格添加到滚动面板
                JBScrollPane scrollPane = new JBScrollPane(table);
                // 确保表格充满视口
                scrollPane.setViewportView(table);
                tabPanel.add(scrollPane, BorderLayout.CENTER);

                // 更新分页条
                PanelUtil.updatePaginationPanel(paginationPanel, data.getTotal(), currentPage, this::loadPage);
            });
        });
    }

}

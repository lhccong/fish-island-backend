package com.github.yuyuanweb.mianshiyaplugin.view;

import com.github.yuyuanweb.mianshiyaplugin.model.HotNews;
import com.github.yuyuanweb.mianshiyaplugin.service.HotNewsService;
import com.intellij.openapi.Disposable;
import com.intellij.openapi.project.Project;
import com.intellij.ui.components.JBPanel;
import com.intellij.ui.components.JBScrollPane;
import com.intellij.ui.table.JBTable;
import com.intellij.util.ui.JBUI;
import com.intellij.openapi.ui.LoadingDecorator;

import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.TableColumnModel;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.*;
import java.util.List;

/**
 * 热榜新闻列表面板
 */
public class HotNewsListPanel extends JBPanel<HotNewsListPanel> implements Disposable {

    private final Project project;
    private final JBTable newsTable;
    private final DefaultTableModel newsTableModel;
    private final List<HotNews> currentNewsList = new ArrayList<>();
    private final Map<String, List<HotNews>> categoryNews = new LinkedHashMap<>();
    private final JSplitPane mainSplitPane;
    private final JSplitPane contentSplitPane;
    private final HotNewsPreviewPanel previewPanel;
    private final HotNewsService hotNewsService;
    private javax.swing.Timer refreshTimer;
    private final JList<String> platformList;
    private final DefaultListModel<String> platformListModel;
    private final LoadingDecorator loadingDecorator;
    private javax.swing.Timer loadingTimer;  // 添加加载动画定时器变量

    public HotNewsListPanel(Project project) {
        super(new BorderLayout());
        this.project = project;
        this.hotNewsService = new HotNewsService();
        this.setBorder(JBUI.Borders.empty(10));

        // 创建主分割面板（左侧平台列表和右侧内容）
        mainSplitPane = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT);
        mainSplitPane.setDividerLocation(150);
        mainSplitPane.setDividerSize(3);

        // 创建内容分割面板（新闻列表和预览）
        contentSplitPane = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT);
        contentSplitPane.setDividerLocation(400);
        contentSplitPane.setDividerSize(3);

        // 创建平台列表
        platformListModel = new DefaultListModel<>();
        platformList = new JList<>(platformListModel);
        platformList.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        platformList.addListSelectionListener(e -> {
            if (!e.getValueIsAdjusting()) {
                String selectedPlatform = platformList.getSelectedValue();
                if (selectedPlatform != null) {
                    updateNewsTable(selectedPlatform);
                }
            }
        });

        // 创建新闻列表表格
        String[] columnNames = {"序号", "标题", "关注数"};
        newsTableModel = new DefaultTableModel(columnNames, 0) {
            @Override
            public boolean isCellEditable(int row, int column) {
                return false;
            }
        };

        newsTable = new JBTable(newsTableModel);
        newsTable.setFillsViewportHeight(true);
        
        // 创建预览面板（包含加载动画）
        previewPanel = new HotNewsPreviewPanel(project);
        loadingDecorator = new LoadingDecorator(previewPanel, this, 0);
        
        // 添加表格选择监听器
        newsTable.getSelectionModel().addListSelectionListener(e -> {
            if (!e.getValueIsAdjusting()) {
                int row = newsTable.getSelectedRow();
                if (row >= 0 && row < currentNewsList.size()) {
                    HotNews news = currentNewsList.get(row);
                    if (!news.getUrl().isEmpty()) {  // 只在有URL时显示加载动画
                        // 停止之前的加载动画（如果存在）
                        if (loadingTimer != null && loadingTimer.isRunning()) {
                            loadingTimer.stop();
                            loadingTimer = null;
                        }
                        loadingDecorator.startLoading(false);
                        previewPanel.loadUrl(news.getUrl());
                        // 延迟3秒后隐藏加载动画
                        loadingTimer = new javax.swing.Timer(3000, evt -> {
                            loadingDecorator.stopLoading();
                            ((javax.swing.Timer) evt.getSource()).stop();
                            loadingTimer = null;
                        });
                        loadingTimer.setRepeats(false);
                        loadingTimer.start();
                    }
                }
            }
        });
        
        // 设置列宽
        TableColumnModel columnModel = newsTable.getColumnModel();
        columnModel.getColumn(0).setPreferredWidth(50);  // 序号列
        columnModel.getColumn(1).setPreferredWidth(300); // 标题列
        columnModel.getColumn(2).setPreferredWidth(100); // 关注数列
        
        // 设置序号列居中对齐
        columnModel.getColumn(0).setCellRenderer((table, value, isSelected, hasFocus, row, column) -> {
            JLabel label = new JLabel(value.toString());
            label.setHorizontalAlignment(SwingConstants.CENTER);
            if (isSelected) {
                label.setBackground(table.getSelectionBackground());
                label.setForeground(table.getSelectionForeground());
                label.setOpaque(true);
            }
            return label;
        });

        // 创建工具栏
        JPanel toolbarPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
        JButton refreshButton = new JButton("刷新");
        refreshButton.addActionListener(e -> refreshData());
        toolbarPanel.add(refreshButton);

        // 创建左侧面板（平台列表）
        JPanel leftPanel = new JPanel(new BorderLayout());
        leftPanel.add(new JLabel("平台列表"), BorderLayout.NORTH);
        leftPanel.add(new JBScrollPane(platformList), BorderLayout.CENTER);

        // 创建中间面板（新闻列表）
        JPanel newsListPanel = new JPanel(new BorderLayout());
        newsListPanel.add(toolbarPanel, BorderLayout.NORTH);
        newsListPanel.add(new JBScrollPane(newsTable), BorderLayout.CENTER);

        // 组装面板
        contentSplitPane.setLeftComponent(newsListPanel);
        contentSplitPane.setRightComponent(loadingDecorator.getComponent());

        mainSplitPane.setLeftComponent(leftPanel);
        mainSplitPane.setRightComponent(contentSplitPane);

        // 添加主分割面板到主面板
        this.add(mainSplitPane, BorderLayout.CENTER);

        // 启动定时刷新
        startAutoRefresh();
        
        // 加载初始数据
        refreshData();
    }

    private void startAutoRefresh() {
        if (refreshTimer != null) {
            refreshTimer.stop();
        }
        // 创建定时器，每5分钟刷新一次
        refreshTimer = new javax.swing.Timer(5 * 60 * 1000, e -> refreshData());
        refreshTimer.start();
    }

    @Override
    public void dispose() {
        if (refreshTimer != null) {
            refreshTimer.stop();
            refreshTimer = null;
        }
        if (previewPanel instanceof Disposable) {
            ((Disposable) previewPanel).dispose();
        }
    }

    private void updateNewsTable(String platform) {
        // 停止当前的加载动画（如果存在）
        if (loadingTimer != null && loadingTimer.isRunning()) {
            loadingTimer.stop();
            loadingTimer = null;
        }
        loadingDecorator.stopLoading();  // 确保加载动画停止

        currentNewsList.clear();
        List<HotNews> news = categoryNews.get(platform);
        if (news != null) {
            currentNewsList.addAll(news);
        }

        // 清空并添加新数据
        while (newsTableModel.getRowCount() > 0) {
            newsTableModel.removeRow(0);
        }

        int index = 1;
        for (HotNews item : currentNewsList) {
            newsTableModel.addRow(new Object[]{
                index++,
                item.getTitle(),
                String.format("%,d", item.getFollowerCount())
            });
        }
        
        // 清空预览面板
        previewPanel.loadUrl("");
    }

    private void refreshData() {
        SwingWorker<Map<String, List<HotNews>>, Void> worker = new SwingWorker<>() {
            @Override
            protected Map<String, List<HotNews>> doInBackground() {
                return hotNewsService.fetchHotNewsByCategory();
            }

            @Override
            protected void done() {
                try {
                    Map<String, List<HotNews>> fetchedNews = get();
                    updateData(fetchedNews);
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        };
        worker.execute();
    }

    private void updateData(Map<String, List<HotNews>> fetchedNews) {
        // 更新分类数据
        categoryNews.clear();
        categoryNews.putAll(fetchedNews);

        // 更新平台列表
        platformListModel.clear();
        fetchedNews.keySet().forEach(platformListModel::addElement);

        // 如果没有选中的平台，选择第一个
        if (platformList.getSelectedValue() == null && platformListModel.size() > 0) {
            platformList.setSelectedIndex(0);
        } else if (platformList.getSelectedValue() != null) {
            // 刷新当前选中的平台数据
            updateNewsTable(platformList.getSelectedValue());
        }
    }
} 
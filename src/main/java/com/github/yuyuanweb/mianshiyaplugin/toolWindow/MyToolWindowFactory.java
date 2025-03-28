package com.github.yuyuanweb.mianshiyaplugin.toolWindow;

import com.github.yuyuanweb.mianshiyaplugin.constant.KeyConstant;
import com.github.yuyuanweb.mianshiyaplugin.view.HotNewsListPanel;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.wm.ToolWindow;
import com.intellij.openapi.wm.ToolWindowFactory;
import com.intellij.ui.components.JBPanel;
import com.intellij.ui.content.Content;
import com.intellij.ui.content.ContentFactory;
import com.intellij.util.ui.JBUI;
import org.jetbrains.annotations.NotNull;
import com.intellij.openapi.application.ApplicationManager;

import javax.swing.*;
import java.awt.*;

/**
 * 主工具窗口工厂
 */
public class MyToolWindowFactory implements ToolWindowFactory {

    @Override
    public void createToolWindowContent(@NotNull Project project, @NotNull ToolWindow toolWindow) {
        // 创建主面板
        JBPanel<?> mainPanel = new JBPanel<>(new BorderLayout());
        mainPanel.setBorder(JBUI.Borders.empty());

        // 创建分割面板
        JSplitPane splitPane = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT);
        splitPane.setDividerLocation(150);
        splitPane.setDividerSize(3);


        // 添加热榜列表面板
        HotNewsListPanel listPanel = new HotNewsListPanel(project);
        splitPane.setRightComponent(listPanel);

        // 添加分割面板到主面板
        mainPanel.add(splitPane, BorderLayout.CENTER);

        // 创建内容
        ContentFactory contentFactory = ApplicationManager.getApplication().getService(ContentFactory.class);
        Content content = contentFactory.createContent(mainPanel, "热榜", false);
        content.setCloseable(false);

        // 添加内容到工具窗口
        toolWindow.getContentManager().addContent(content);
    }
}

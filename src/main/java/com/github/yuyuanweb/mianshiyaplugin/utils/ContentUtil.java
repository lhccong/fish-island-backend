package com.github.yuyuanweb.mianshiyaplugin.utils;

import com.github.yuyuanweb.mianshiyaplugin.constant.KeyConstant;
import com.intellij.openapi.actionSystem.ActionManager;
import com.intellij.openapi.actionSystem.ActionToolbar;
import com.intellij.openapi.actionSystem.DefaultActionGroup;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.wm.ToolWindowManager;
import com.intellij.ui.components.JBPanel;
import com.intellij.ui.content.Content;
import com.intellij.ui.content.ContentFactory;
import com.intellij.ui.content.ContentManager;
import com.intellij.util.ui.JBUI;

import javax.swing.*;
import java.awt.*;

/**
 * content 页 工具
 * @author pine
 */
public class ContentUtil {

    public static void createContent(JComponent component, String title, boolean isLockable, Project project) {
        if (component == null) {
            throw new IllegalArgumentException("component is null");
        }
        ActionManager actionManager = ActionManager.getInstance();
        DefaultActionGroup actionGroup = (DefaultActionGroup) actionManager.getAction(KeyConstant.ACTION_BAR);
        ActionToolbar actionToolbar = actionManager.createActionToolbar(KeyConstant.ACTION_BAR, actionGroup, true);
        // 创建主面板
        JPanel mainPanel = new JBPanel<>(new BorderLayout());
        mainPanel.setBorder(JBUI.Borders.empty());
        actionToolbar.setTargetComponent(mainPanel);
        mainPanel.add(actionToolbar.getComponent(), BorderLayout.NORTH);
        mainPanel.add(component, BorderLayout.CENTER);

        // ContentFactory contentFactory = ContentFactory.getInstance();
        ContentFactory contentFactory = ApplicationManager.getApplication().getService(ContentFactory.class);
        Content content = contentFactory.createContent(mainPanel, title, isLockable);
        ContentManager contentManager = ToolWindowManager.getInstance(project).getToolWindow(KeyConstant.PLUGIN_NAME).getContentManager();
        contentManager.addContent(content);
        contentManager.setSelectedContent(content);
    }

}

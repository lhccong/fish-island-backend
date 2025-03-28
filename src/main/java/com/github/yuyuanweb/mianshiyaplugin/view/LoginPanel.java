package com.github.yuyuanweb.mianshiyaplugin.view;

import com.github.yuyuanweb.mianshiyaplugin.constant.CommonConstant;
import com.github.yuyuanweb.mianshiyaplugin.manager.CookieManager;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.DialogWrapper;
import com.intellij.openapi.util.Disposer;
import com.intellij.ui.components.JBScrollPane;
import com.intellij.ui.jcef.JCEFHtmlPanel;
import com.intellij.util.ui.JBUI;
import com.intellij.util.ui.components.BorderLayoutPanel;
import org.cef.browser.CefBrowser;
import org.cef.handler.CefLoadHandlerAdapter;
import org.cef.network.CefCookieManager;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;
import java.awt.*;

/**
 * @author pine
 */
public class LoginPanel extends DialogWrapper {

    private final BorderLayoutPanel panel = JBUI.Panels.simplePanel();

    private final Action okAction;

    public LoginPanel(@Nullable Project project) {
        super(project, null, false, IdeModalityType.IDE, false);
        okAction = new OkAction() {
        };
        JcefPanel jcefPanel;
        try {
            jcefPanel = new JcefPanel();
        } catch (IllegalArgumentException e) {
            jcefPanel = new JcefPanel(true);
        }
        Disposer.register(getDisposable(), jcefPanel);
        jcefPanel.getComponent().setMinimumSize(new Dimension(1000, 500));
        jcefPanel.getComponent().setPreferredSize(new Dimension(1500, 800));
        panel.addToTop(new JBScrollPane(jcefPanel.getComponent(), JBScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED, JBScrollPane.HORIZONTAL_SCROLLBAR_NEVER));

        setModal(true);
        init();
        setTitle("Login");

        // 调整对话框大小
        pack();
    }

    @Override
    protected @Nullable JComponent createCenterPanel() {
        return panel;
    }

    @NotNull
    @Override
    protected Action getOKAction() {
        return okAction;
    }

    @Override
    protected void doOKAction() {
        super.doOKAction();
    }

    private class JcefPanel extends JCEFHtmlPanel {


        private CefLoadHandlerAdapter cefLoadHandler;

        public JcefPanel(boolean old) {
            super(null);
            init();
        }

        public JcefPanel() {
            super(null, null);
            init();
        }

        private void init() {
            getJBCefClient().addLoadHandler(cefLoadHandler = new CefLoadHandlerAdapter() {

                @Override
                public void onLoadingStateChange(CefBrowser browser, boolean isLoading, boolean canGoBack, boolean canGoForward) {
                    CefCookieManager cefCookieManager = getJBCefCookieManager().getCefCookieManager();
                    CookieManager.handleCookie(cefCookieManager, LoginPanel.this::doOKAction);
                }
            }, getCefBrowser());
            loadURL(CommonConstant.WEB_HOST + "user/login");
        }

        @Override
        public void dispose() {
            SwingUtilities.invokeLater(() -> {
                getJBCefClient().removeLoadHandler(cefLoadHandler, getCefBrowser());
                super.dispose();
            });
        }
    }
}

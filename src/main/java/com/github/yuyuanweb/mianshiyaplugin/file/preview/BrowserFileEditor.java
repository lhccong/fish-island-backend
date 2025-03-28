package com.github.yuyuanweb.mianshiyaplugin.file.preview;

import com.github.yuyuanweb.mianshiyaplugin.config.GlobalState;
import com.github.yuyuanweb.mianshiyaplugin.constant.CommonConstant;
import com.github.yuyuanweb.mianshiyaplugin.constant.KeyConstant;
import com.github.yuyuanweb.mianshiyaplugin.manager.CookieManager;
import com.github.yuyuanweb.mianshiyaplugin.model.enums.WebTypeEnum;
import com.github.yuyuanweb.mianshiyaplugin.utils.ThemeUtil;
import com.intellij.openapi.fileEditor.FileEditor;
import com.intellij.openapi.fileEditor.FileEditorLocation;
import com.intellij.openapi.fileEditor.FileEditorState;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.Key;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.ui.jcef.JBCefBrowser;
import com.intellij.ui.jcef.JBCefCookie;
import com.intellij.ui.jcef.JBCefCookieManager;
import lombok.Getter;
import org.cef.browser.CefBrowser;
import org.cef.handler.CefLoadHandlerAdapter;
import org.cef.network.CefCookieManager;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;
import java.awt.*;
import java.beans.PropertyChangeListener;


/**
 * 内嵌浏览器 文件编辑器
 *
 * @author pine
 */
public class BrowserFileEditor implements FileEditor {

    @Getter
    private final JBCefBrowser jbCefBrowser;

    private final JPanel panel;

    @Getter
    private final WebTypeEnum webTypeEnum;

    public BrowserFileEditor(@NotNull Project project, @NotNull VirtualFile file) {
        this.jbCefBrowser = new JBCefBrowser();
        this.panel = new JPanel(new BorderLayout());
        this.panel.add(jbCefBrowser.getComponent(), BorderLayout.CENTER);
        this.panel.setPreferredSize(new Dimension());

        // 加载文件内容到浏览器
        jbCefBrowser.getJBCefClient().addLoadHandler(new CefLoadHandlerAdapter() {
            @Override
            public void onLoadingStateChange(CefBrowser browser, boolean isLoading, boolean canGoBack, boolean canGoForward) {
                JBCefCookieManager cookieManager = jbCefBrowser.getJBCefCookieManager();
                String cookie = GlobalState.getInstance().getSavedCookie();
                JBCefCookie jbCefCookie = new JBCefCookie("SESSION", cookie.replace("SESSION=", ""), ".mianshiya.com", "/api", true, true);
                cookieManager.setCookie(CommonConstant.WEB_HOST, jbCefCookie, false);
                jbCefBrowser.setJBCefCookieManager(cookieManager);
            }
        }, jbCefBrowser.getCefBrowser());

        jbCefBrowser.getJBCefClient().addLoadHandler(new CefLoadHandlerAdapter() {
            @Override
            public void onLoadingStateChange(CefBrowser browser, boolean isLoading, boolean canGoBack, boolean canGoForward) {
                CefCookieManager cefCookieManager = jbCefBrowser.getJBCefCookieManager().getCefCookieManager();
                CookieManager.handleCookie(cefCookieManager, () -> {});
            }
        }, jbCefBrowser.getCefBrowser());

        Long questionId = file.get().get(KeyConstant.QUESTION_ID_KEY);
        webTypeEnum = file.get().get(KeyConstant.WEB_TYPE_KEY);
        if (questionId != null && webTypeEnum != null) {
            String theme = ThemeUtil.getTheme();
            String url = String.format(CommonConstant.PLUGIN_QD, questionId, webTypeEnum.getValue(), theme);
            jbCefBrowser.loadURL(url);
        }
    }

    @Override
    public @NotNull JComponent getComponent() {
        return panel;
    }

    @Override
    public @Nullable JComponent getPreferredFocusedComponent() {
        return jbCefBrowser.getComponent();
    }

    @Override
    public @NotNull String getName() {
        return "Browser Editor";
    }

    @Override
    public void setState(@NotNull FileEditorState state) {

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
        jbCefBrowser.dispose();
    }

    @Override
    public <T> @Nullable T getUserData(@NotNull Key<T> key) {
        return null;
    }

    @Override
    public <T> void putUserData(@NotNull Key<T> key, @Nullable T value) {

    }

    @Override
    public @Nullable FileEditorLocation getCurrentLocation() {
        return null;
    }

}

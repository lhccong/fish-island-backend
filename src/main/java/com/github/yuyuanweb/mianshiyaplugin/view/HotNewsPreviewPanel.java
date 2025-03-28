package com.github.yuyuanweb.mianshiyaplugin.view;

import com.intellij.openapi.project.Project;
import com.intellij.ui.jcef.JBCefBrowser;
import com.intellij.ui.components.JBPanel;
import com.intellij.util.ui.JBUI;
import com.intellij.openapi.Disposable;
import org.cef.browser.CefBrowser;
import org.cef.handler.CefLoadHandlerAdapter;

import java.awt.*;

/**
 * 热榜新闻预览面板
 */
public class HotNewsPreviewPanel extends JBPanel<HotNewsPreviewPanel> implements Disposable {

    private final Project project;
    private final JBCefBrowser browser;

    public HotNewsPreviewPanel(Project project) {
        super(new BorderLayout());
        this.project = project;
        this.setBorder(JBUI.Borders.empty(10));

        // 创建浏览器组件
        browser = new JBCefBrowser();
        this.add(browser.getComponent(), BorderLayout.CENTER);
        
        // 初始化默认内容
        clearContent();
    }

    /**
     * 加载URL
     */
    public void loadUrl(String url) {
        if (url != null && !url.isEmpty()) {
            browser.loadURL(url);
        }
    }
    
    /**
     * 加载URL并在加载完成后执行回调
     */
    public void loadUrl(String url, Runnable onLoadFinished) {
        if (url != null && !url.isEmpty()) {
            browser.getJBCefClient().addLoadHandler(new CefLoadHandlerAdapter() {
                @Override
                public void onLoadingStateChange(CefBrowser browser, boolean isLoading, boolean canGoBack, boolean canGoForward) {
                    if (!isLoading) {
                        onLoadFinished.run();
                        browser.getClient().removeLoadHandler();
                    }
                }
            }, browser.getCefBrowser());
            browser.loadURL(url);
        }
    }
    
    /**
     * 清空内容
     */
    public void clearContent() {
        browser.loadHTML("<html><body style='margin:0;padding:20px;font-family:Arial,sans-serif;color:#666;'><div style='text-align:center;padding-top:100px;'><h2>请选择一条新闻查看详情</h2></div></body></html>");
    }
    
    @Override
    public void dispose() {
        browser.dispose();
    }
} 
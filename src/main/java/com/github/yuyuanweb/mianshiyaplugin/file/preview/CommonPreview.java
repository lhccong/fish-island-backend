package com.github.yuyuanweb.mianshiyaplugin.file.preview;

import com.github.yuyuanweb.mianshiyaplugin.constant.ViewConstant;
import com.intellij.openapi.editor.Document;
import com.intellij.openapi.fileEditor.FileDocumentManager;
import com.intellij.openapi.fileEditor.FileEditor;
import com.intellij.openapi.fileEditor.FileEditorLocation;
import com.intellij.openapi.fileEditor.FileEditorState;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.UserDataHolderBase;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.ui.components.JBLabel;
import com.intellij.ui.components.JBPanel;
import com.intellij.util.ui.JBUI;
import com.intellij.util.ui.components.BorderLayoutPanel;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jsoup.Jsoup;

import javax.swing.*;
import java.awt.*;
import java.beans.PropertyChangeListener;

/**
 * 普通 文件编辑器
 *
 * @author pine
 */
public class CommonPreview extends UserDataHolderBase implements FileEditor {

    private final VirtualFile myFile;
    private final Document myDocument;

    private BorderLayoutPanel myHtmlPanelWrapper;

    public CommonPreview(@NotNull Project project, @NotNull VirtualFile file) {
        myFile = file;
        myDocument = FileDocumentManager.getInstance().getDocument(myFile);
    }

    @Override
    public @NotNull JComponent getComponent() {
        if (myHtmlPanelWrapper == null) {
            myHtmlPanelWrapper = JBUI.Panels.simplePanel();

            JBLabel loadingLabel = new JBLabel("Loading......");
            myHtmlPanelWrapper.addToTop(loadingLabel);
            try {
                JBPanel<?> tempPanel = new JBPanel<>(new BorderLayout());

                JBLabel jbLabel = new JBLabel(Jsoup.parse(myDocument.getText()).html());
                tempPanel.add(jbLabel);
                myHtmlPanelWrapper.addToTop(tempPanel.getComponent(0));

            } catch (Throwable e) {
                myHtmlPanelWrapper.addToTop(new JBLabel("<html><body>Your environment does not support JCEF.<br>Check the Registry 'ide.browser.jcef.enabled'.<br>" + e.getMessage() + "<body></html>"));
            } finally {
                myHtmlPanelWrapper.remove(loadingLabel);
                myHtmlPanelWrapper.repaint();
            }
        }

        return myHtmlPanelWrapper;
    }

    @Override
    public @Nullable JComponent getPreferredFocusedComponent() {
        return null;
    }

    @Override
    public @NotNull String getName() {
        return ViewConstant.COMMON_PREVIEW;
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
        return true;
    }

    @Override
    public void addPropertyChangeListener(@NotNull PropertyChangeListener listener) {

    }

    @Override
    public void removePropertyChangeListener(@NotNull PropertyChangeListener listener) {

    }

    @Override
    public void dispose() {
    }

    @Override
    public @Nullable VirtualFile getFile() {
        return myFile;
    }

    @Override
    public @Nullable FileEditorLocation getCurrentLocation() {
        return null;
    }

}

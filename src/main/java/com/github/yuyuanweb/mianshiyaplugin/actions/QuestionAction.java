package com.github.yuyuanweb.mianshiyaplugin.actions;

import com.github.yuyuanweb.mianshiyaplugin.view.QuestionListManager;
import com.intellij.openapi.actionSystem.AnAction;
import com.intellij.openapi.actionSystem.AnActionEvent;
import org.jetbrains.annotations.NotNull;

import javax.swing.*;

/**
 * @author pine
 */
public class QuestionAction extends AnAction {

    public QuestionAction() {
    }

    public QuestionAction(String text, Icon icon) {
        super(text, text, icon);
    }

    @Override
    public void actionPerformed(@NotNull AnActionEvent anActionEvent) {
        QuestionListManager questionListManager = new QuestionListManager();
        questionListManager.addQuestionTab(null, anActionEvent.getProject());
    }

}

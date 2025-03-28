package com.github.yuyuanweb.mianshiyaplugin.utils;

import cn.hutool.core.io.FileUtil;
import cn.hutool.core.text.StrPool;
import cn.hutool.core.util.RandomUtil;
import cn.hutool.core.util.StrUtil;
import com.github.yuyuanweb.mianshiyaplugin.constant.KeyConstant;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.fileEditor.FileEditorManager;
import com.intellij.openapi.fileEditor.OpenFileDescriptor;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.vfs.LocalFileSystem;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.openapi.vfs.newvfs.RefreshQueue;
import com.intellij.util.keyFMap.KeyFMap;

import java.io.*;
import java.nio.charset.StandardCharsets;

/**
 * @author pine
 */
public class FileUtils {

    public static int FILE_NAME_LENGTH = 7;

    public static String getTempDir() {
        return FileUtil.getTmpDirPath() + "mianshiya/";
    }

    public static File openArticle(Project project, Boolean isOpenEditor) {
        String filePath = FileUtils.getTempDir() + RandomUtil.randomString(10) + StrPool.DOT + KeyConstant.EDITOR_FILE_POSTFIX;

        File file = FileUtil.touch(filePath);
        if (!file.exists()) {
            FileUtil.writeString("content", filePath, StandardCharsets.UTF_8);
        }
        if (isOpenEditor) {
            FileUtils.openFileEditor(file, project);
        }
        return file;
    }

    public static void openNewEditorTab(Project project, Long questionId, Long questionBankId, Long questionNum, String questionTitle) {

        // 创建一个临时文件并写入内容
        if (StrUtil.isBlank(questionTitle)) {
            questionTitle = RandomUtil.randomString(FILE_NAME_LENGTH);
        }
        String fileName;
        if (questionTitle.length() > FILE_NAME_LENGTH) {
            fileName = questionTitle.substring(0, FILE_NAME_LENGTH) + "…";
        } else {
            fileName = questionTitle;
        }
        String filePath = FileUtils.getTempDir() + questionNum + ". " + fileName + StrPool.DOT + KeyConstant.EDITOR_FILE_POSTFIX_CONTENT;
        File tempFile = FileUtil.touch(filePath);

        if (!tempFile.exists()) {
            FileUtil.writeString(questionTitle, filePath, StandardCharsets.UTF_8);
        }
        FileUtils.openFileEditorAndSaveState(tempFile, project, questionId, questionBankId);
    }

    public static void openFileEditorAndSaveState(File file, Project project, Long questionId, Long questionBankId) {
        ApplicationManager.getApplication().executeOnPooledThread(() -> {
            VirtualFile vf = LocalFileSystem.getInstance().refreshAndFindFileByIoFile(file);
            KeyFMap map = KeyFMap.EMPTY_MAP.plus(KeyConstant.QUESTION_ID_KEY, questionId);
            map = map.plus(KeyConstant.QUESTION_BANK_ID_KEY, questionBankId);
            assert vf != null;
            vf.set(map);
            ApplicationManager.getApplication().invokeLater(() -> {
                if (ApplicationManager.getApplication().isDisposed()) {
                    return;
                }
                OpenFileDescriptor descriptor = new OpenFileDescriptor(project, vf);
                FileEditorManager.getInstance(project).openTextEditor(descriptor, false);
            });
        });
    }

    public static void openFileEditor(File file, Project project) {
        ApplicationManager.getApplication().invokeLater(() -> {
            VirtualFile vf = LocalFileSystem.getInstance().refreshAndFindFileByIoFile(file);
            OpenFileDescriptor descriptor = new OpenFileDescriptor(project, vf);
            FileEditorManager.getInstance(project).openTextEditor(descriptor, false);
            RefreshQueue.getInstance().refresh(false, false, null, vf);
        });
    }

}

package com.github.yuyuanweb.mianshiyaplugin.file.provider;

import cn.hutool.core.io.FileUtil;
import com.github.yuyuanweb.mianshiyaplugin.utils.FileUtils;
import com.intellij.openapi.fileEditor.FileEditor;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.vfs.LocalFileSystem;
import com.intellij.openapi.vfs.VirtualFile;
import org.jetbrains.annotations.NotNull;

/**
 * 问题页面
 * 已废弃
 *
 * @author pine
 */
public class QuestionProvider extends CommonProvider {

    @Override
    public @NotNull FileEditor createEditor(@NotNull Project project, @NotNull VirtualFile file) {
        VirtualFile contentVf = LocalFileSystem.getInstance().refreshAndFindFileByIoFile(FileUtil.touch(FileUtils.getTempDir() + file.getName()));

        return super.createEditor(project, contentVf);
    }
}

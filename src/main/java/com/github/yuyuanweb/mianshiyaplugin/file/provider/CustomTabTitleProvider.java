package com.github.yuyuanweb.mianshiyaplugin.file.provider;

import com.github.yuyuanweb.mianshiyaplugin.constant.KeyConstant;
import com.intellij.openapi.fileEditor.impl.EditorTabTitleProvider;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.vfs.VirtualFile;
import org.jetbrains.annotations.Nullable;

/**
 * 自定义 tab 栏的文件名
 *
 * @author pine
 */
public class CustomTabTitleProvider implements EditorTabTitleProvider {
    @Nullable
    @Override
    public String getEditorTabTitle(Project project, VirtualFile file) {
        if (KeyConstant.EDITOR_FILE_POSTFIX_CONTENT.equals(file.getExtension())) {
            // 返回自定义名称，不显示后缀
            return file.getNameWithoutExtension();
        }
        // 返回 null 保持默认行为
        return null;
    }
}

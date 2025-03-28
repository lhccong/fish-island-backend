package com.github.yuyuanweb.mianshiyaplugin.listeners;

import cn.hutool.core.io.FileUtil;
import com.github.yuyuanweb.mianshiyaplugin.utils.FileUtils;
import com.intellij.ide.AppLifecycleListener;
import com.intellij.openapi.diagnostic.Logger;

/**
 * @author pine
 */
public class ApplicationLifecycleListener implements AppLifecycleListener {

    private static final Logger logger = Logger.getInstance(ApplicationLifecycleListener.class);

    @Override
    public void appWillBeClosed(boolean isRestart) {
        // 删除插件产生的临时文件
        AppLifecycleListener.super.appWillBeClosed(isRestart);
        boolean del = FileUtil.del(FileUtils.getTempDir());
        if (!del) {
            logger.warn("缓存文件删除失败");
        }
    }
}

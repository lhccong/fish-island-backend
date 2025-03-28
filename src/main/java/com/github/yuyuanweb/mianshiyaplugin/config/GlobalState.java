package com.github.yuyuanweb.mianshiyaplugin.config;

import com.github.yuyuanweb.mianshiyaplugin.constant.PageConstant;
import com.github.yuyuanweb.mianshiyaplugin.model.response.User;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.components.*;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * 全局配置
 *
 * @author pine
 */
@State(name = "GlobalState", storages = {@Storage("mainshiya.xml")})
public class GlobalState implements PersistentStateComponent<GlobalState.State> {

    public static class State {
        public String cookie = "";
        public int pageSize = PageConstant.PAGE_SIZE;
        public User user = null;
    }

    private State state = new State();

    @Nullable
    @Override
    public State getState() {
        return state;
    }

    @Override
    public void loadState(@NotNull State state) {
        this.state = state;
    }

    public void saveCookie(String cookie) {
        state.cookie = cookie;
    }

    public void saveUser(User user) {
        state.user = user;
    }

    public String getSavedCookie() {
        return state.cookie;
    }

    public User getSavedUser() {
        return state.user;
    }

    public void removeSavedCookie() {
        state.cookie = "";
    }

    public void removeSavedUser() {
        state.user = null;
    }

    public static GlobalState getInstance() {
        return ApplicationManager.getApplication().getService(GlobalState.class);
    }
}

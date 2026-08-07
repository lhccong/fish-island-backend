package com.cong.fishisland.game.model;

import com.cong.fishisland.game.enums.PlayerRoomStatusEnum;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 游戏会话
 * 管理玩家的游戏会话状态
 *
 * @author cong
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class GameSession {

    /**
     * 用户ID
     */
    private Long userId;

    /**
     * 当前房间ID
     */
    private String roomId;

    /**
     * 用户名
     */
    private String userName;

    /**
     * 头像
     */
    private String avatar;

    /**
     * 是否在线
     */
    private boolean online;

    /**
     * 最后心跳时间
     */
    private long lastHeartbeat;

    /**
     * 断线时间（用于计算重连窗口）
     */
    private long disconnectedAt;

    /**
     * 离线持续时间（毫秒）
     */
    private long offlineDuration;

    /**
     * 玩家房间状态
     */
    @Builder.Default
    private PlayerRoomStatusEnum roomStatus = PlayerRoomStatusEnum.NONE;

    /**
     * 临时离开的房间ID（用于中途退出后追踪）
     */
    private String tempLeaveRoomId;

    /**
     * 断线重连窗口（毫秒），默认60秒
     */
    private static final long RECONNECT_WINDOW_MS = 60000L;

    /**
     * 检查是否在重连窗口内
     */
    public boolean isWithinReconnectWindow() {
        if (online) {
            return true;
        }
        if (disconnectedAt <= 0) {
            return false;
        }
        return System.currentTimeMillis() - disconnectedAt < RECONNECT_WINDOW_MS;
    }

    /**
     * 标记为离线
     */
    public void markOffline() {
        this.online = false;
        this.disconnectedAt = System.currentTimeMillis();
    }

    /**
     * 标记为在线
     */
    public void markOnline() {
        this.online = true;
        this.offlineDuration = 0;
        this.disconnectedAt = 0;
    }

    /**
     * 获取离线时长
     */
    public long getOfflineDuration() {
        if (disconnectedAt <= 0) {
            return 0;
        }
        return System.currentTimeMillis() - disconnectedAt;
    }

    /**
     * 是否可以重连
     */
    public boolean canReconnect() {
        return !online && isWithinReconnectWindow();
    }

    /**
     * 设置临时离开状态
     * @param roomId 离开的房间ID
     */
    public void setTempLeave(String roomId) {
        this.roomStatus = PlayerRoomStatusEnum.TEMP_LEAVE;
        this.tempLeaveRoomId = roomId;
        this.roomId = null; // 清空当前房间
    }

    /**
     * 清除临时离开状态
     */
    public void clearTempLeave() {
        this.roomStatus = PlayerRoomStatusEnum.NONE;
        this.tempLeaveRoomId = null;
    }

    /**
     * 检查是否有临时离开的房间
     */
    public boolean hasTempLeave() {
        return this.roomStatus == PlayerRoomStatusEnum.TEMP_LEAVE && this.tempLeaveRoomId != null;
    }
}

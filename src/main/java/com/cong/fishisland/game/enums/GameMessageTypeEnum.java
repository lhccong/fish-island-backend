package com.cong.fishisland.game.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * 游戏消息类型枚举
 *
 * @author cong
 */
@Getter
@AllArgsConstructor
public enum GameMessageTypeEnum {

    // 基础消息
    LOGIN("login", "登录"),
    LOGOUT("logout", "登出"),

    // 房间消息
    CREATE_ROOM("gameCreateRoom", "创建房间"),
    JOIN_ROOM("gameJoinRoom", "加入房间"),
    LEAVE_ROOM("gameLeaveRoom", "离开房间"),
    ROOM_LIST("gameRoomList", "房间列表"),
    ROOM_ADDED("gameRoomAdded", "房间新增"),
    ROOM_REMOVED("gameRoomRemoved", "房间删除"),
    READY("gameReady", "准备"),
    START_GAME("gameStart", "开始游戏"),

    // 游戏消息
    DEAL_CARDS("gameDealCards", "发牌"),
    ROB_LANDLORD("gameRobLandlord", "叫地主"),
    PLAY_CARDS("gamePlayCards", "出牌"),
    PASS("gamePass", "不出"),
    GAME_OVER("gameOver", "游戏结束"),

    // 统一流程消息
    TURN_NOTIFY("gameTurnNotify", "回合通知"),
    ACTION_RESULT("gameActionResult", "操作结果"),

    // 其他
    CHAT("gameChat", "聊天"),
    STATE_UPDATE("gameStateUpdate", "状态更新"),
    ERROR("error", "错误"),

    // AI托管
    CANCEL_ROBOT("gameCancelRobot", "取消AI托管"),
    SET_ROBOT("gameSetRobot", "设置AI托管"),
    ;

    private final String type;
    private final String desc;

    public static GameMessageTypeEnum of(String type) {
        if (type == null) {
            return null;
        }
        for (GameMessageTypeEnum value : values()) {
            if (value.type.equals(type)) {
                return value;
            }
        }
        return null;
    }
}

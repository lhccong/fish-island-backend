package com.cong.fishisland.model.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;

import java.util.Arrays;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * Description: 消息状态
 *
 * @author cong
 * @date 2024/02/18
 */
@AllArgsConstructor
@Getter
public enum MessageTypeEnum {
    ERROR("error", "错误提示"),
    INFO("info", "系统消息"),
    COUNTDOWN("countdown", "倒计时"),
    CREATE_CHESS_ROOM("createChessRoom", "创建五子棋房间"),
    JOIN_ROOM("joinRoom", "加入房间"),
    MOVE_CHESS("moveChess", "对方落子"),
    JOIN_SUCCESS("joinSuccess", "成功加入房间"),
    CHAT("chat", "群聊天消息"),
    UNDERCOVER("undercover", "谁是卧底消息"),
    DRAW("draw", "你画我猜消息"),
    CLEAR_DRAW("clearDraw", "清空画板"),
    GAME_START("gameStart", "开始游戏"),
    REFRESH_ROOM("refreshRoom", "刷新房间信息"),
    REFRESH_DRAW("refreshDraw", "刷新绘画房间信息"),
    USER_ONLINE("userOnline", "用户上线"),
    USER_OFFLINE("userOffline", "用户下线"),
    USER_MESSAGE_REVOKE("userMessageRevoke", "用户撤回消息"),
    CREATE_DRAW_ROOM("createDrawRoom", "创建绘画房间"),
    ROOM_DRAW_CREATED("roomDrawCreated", "房间绘画创建成功"),
    ROOM_DRAW_USER_LIST("roomDrawUserList", "发送你画我猜在线用户列表"),

    // ==================== 斗地主游戏消息 ====================
    GAME_ROOM_LIST("gameRoomList", "游戏房间列表"),
    GAME_CREATE_ROOM("gameCreateRoom", "创建游戏房间"),
    GAME_JOIN_ROOM("gameJoinRoom", "加入游戏房间"),
    GAME_LEAVE_ROOM("gameLeaveRoom", "离开游戏房间"),
    GAME_READY("gameReady", "玩家准备"),
    GAME_DEAL_CARDS("gameDealCards", "发牌"),
    GAME_ROB_LANDLORD("gameRobLandlord", "叫地主"),
    GAME_PLAY_CARDS("gamePlayCards", "出牌"),
    GAME_PASS("gamePass", "不出"),
    GAME_OVER("gameOver", "游戏结束"),
    GAME_STATE_UPDATE("gameStateUpdate", "游戏状态更新"),
    GAME_CHAT("gameChat", "游戏聊天"),
    GAME_TURN_NOTIFY("gameTurnNotify", "回合通知"),
    GAME_ACTION_RESULT("gameActionResult", "操作结果"),
    GAME_SET_ROBOT("gameSetRobot", "设置AI托管"),
    GAME_CANCEL_ROBOT("gameCancelRobot", "取消AI托管"),
    ;

    private final String type;
    private final String desc;

    private static final Map<String, MessageTypeEnum> CACHE;

    static {
        CACHE = Arrays.stream(values()).collect(Collectors.toMap(MessageTypeEnum::getType, Function.identity()));
    }

    public static MessageTypeEnum of(String type) {
        return CACHE.get(type);
    }
}

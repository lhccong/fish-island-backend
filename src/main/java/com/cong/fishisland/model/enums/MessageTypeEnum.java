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
    CREATE_CHESS_ROOM("createChessRoom", "创建五子棋房间"),
    JOIN_ROOM("joinRoom", "加入房间"),
    MOVE_CHESS("moveChess", "对方落子"),
    JOIN_SUCCESS("joinSuccess", "成功加入房间"),
    CHAT("chat", "群聊天消息"),
    UNDERCOVER("undercover", "谁是卧底消息"),
    REFRESH_ROOM("refreshRoom", "刷新房间信息"),
    USER_ONLINE("userOnline", "用户上线"),
    USER_OFFLINE("userOffline", "用户下线"),
    USER_MESSAGE_REVOKE("userMessageRevoke", "用户撤回消息"),
    CREATE_DRAW_ROOM("createDrawRoom", "创建绘画房间"),
    ROOM_DRAW_CREATED("roomDrawCreated", "房间绘画创建成功"),
    ROOM_DRAW_USER_LIST("roomDrawUserList", "发送你画我猜在线用户列表"),
    ;

    private final String type;
    private final String desc;

    private static final Map<String, MessageTypeEnum> CACHE;

    static {
        CACHE = Arrays.stream(MessageTypeEnum.values()).collect(Collectors.toMap(MessageTypeEnum::getType, Function.identity()));
    }

    public static MessageTypeEnum of(String type) {
        return CACHE.get(type);
    }
}

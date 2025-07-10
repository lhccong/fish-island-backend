package com.cong.fishisland.constant;

/**
 * 你画我猜游戏Redis键常量
 *
 * @author cong
 */
public class DrawGameRedisKey {
    /**
     * 基础键前缀
     */
    public static final String BASE_KEY = "fish:draw:";

    /**
     * 房间信息键 - fish:draw:roomInfo:{roomId}
     */
    public static final String ROOM_INFO = "roomInfo";

    /**
     * 房间绘画数据键 - fish:draw:drawData:{roomId}
     */
    public static final String DRAW_DATA = "drawData";

    /**
     * 玩家所在房间键 - fish:draw:playerRoom:{userId}
     */
    public static final String PLAYER_ROOM = "playerRoom";

    /**
     * 玩家猜词记录键 - fish:draw:playerGuess:{roomId}:{userId}
     */
    public static final String PLAYER_GUESS = "playerGuess";

    /**
     * 房间猜词记录键 - fish:draw:roomGuesses:{roomId}
     */
    public static final String ROOM_GUESSES = "roomGuesses";

    /**
     * 构建完整的Redis键
     *
     * @param keyPrefix 键前缀
     * @param params    参数列表
     * @return 完整的Redis键
     */
    public static String getKey(String keyPrefix, Object... params) {
        StringBuilder key = new StringBuilder(BASE_KEY);
        key.append(keyPrefix);
        for (Object param : params) {
            key.append(":").append(param);
        }
        return key.toString();
    }
} 
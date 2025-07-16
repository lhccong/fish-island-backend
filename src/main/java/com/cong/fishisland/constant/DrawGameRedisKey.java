package com.cong.fishisland.constant;

/**
 * 你画我猜游戏Redis键
 *
 * @author cong
 */
public interface DrawGameRedisKey {

    /**
     * 基础键前缀
     */
    String BASE_KEY = "fish:draw:";

    /**
     * 房间信息
     */
    String ROOM_INFO = "roomInfo";

    /**
     * 玩家所在房间
     */
    String PLAYER_ROOM = "playerRoom";

    /**
     * 绘画数据
     */
    String DRAW_DATA = "drawData";

    /**
     * 房间猜词记录
     */
    String ROOM_GUESSES = "roomGuesses";

    /**
     * 玩家积分
     */
    String PLAYER_SCORE = "playerScore";
    
    /**
     * 当天已使用的词语
     */
    String USED_WORDS = "usedWords";
    
    /**
     * 房间列表（轻量级，不包含绘画数据）
     */
    String ROOM_LIST = "roomList";

    /**
     * 获取完整的Redis键
     *
     * @param keys 键的各个部分
     * @return 完整的Redis键
     */
    static String getKey(String... keys) {
        StringBuilder sb = new StringBuilder(BASE_KEY);
        for (String key : keys) {
            sb.append(key).append(":");
        }
        // 移除最后一个冒号
        if (sb.charAt(sb.length() - 1) == ':') {
            sb.deleteCharAt(sb.length() - 1);
        }
        return sb.toString();
    }
}
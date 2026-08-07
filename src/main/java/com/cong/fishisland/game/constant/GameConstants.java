package com.cong.fishisland.game.constant;

/**
 * 游戏常量
 *
 * @author cong
 */
public class GameConstants {

    private GameConstants() {
    }

    // ==================== 游戏相关 ====================
    
    /** 最小玩家数 */
    public static final int MIN_PLAYERS = 2;
    
    /** 最大玩家数 */
    public static final int MAX_PLAYERS = 6;
    
    /** 斗地主玩家数 */
    public static final int LANDLORDS_PLAYERS = 3;
    
    /** 跑得快玩家数 */
    public static final int RUNFAST_PLAYERS = 3;
    
    /** 底牌数量 */
    public static final int BOTTOM_CARD_COUNT = 3;
    
    /** 每人初始手牌数 */
    public static final int INITIAL_HAND_CARDS = 17;
    
    // ==================== 超时时间(毫秒) ====================
    
    /** 叫地主超时时间 */
    public static final long ROB_TIMEOUT = 20000L;
    
    /** 出牌超时时间 */
    public static final long PLAY_TIMEOUT = 40000L;
    
    /** 麻将出牌超时时间 */
    public static final long MAHJONG_TIMEOUT = 30000L;
    
    /** 德州扑克押注超时时间 */
    public static final long BET_TIMEOUT = 60000L;
    
    /** 准备超时时间 */
    public static final long READY_TIMEOUT = 60000L;
    
    // ==================== WebSocket 消息类型 ====================
    
    /** 心跳 */
    public static final int MSG_HEARTBEAT = 0;
    
    /** 登录 */
    public static final int MSG_LOGIN = 1;
    
    /** 登出 */
    public static final int MSG_LOGOUT = 2;
    
    /** 创建房间 */
    public static final int MSG_CREATE_ROOM = 10;
    
    /** 加入房间 */
    public static final int MSG_JOIN_ROOM = 11;
    
    /** 离开房间 */
    public static final int MSG_LEAVE_ROOM = 12;
    
    /** 房间列表 */
    public static final int MSG_ROOM_LIST = 13;
    
    /** 玩家准备 */
    public static final int MSG_READY = 20;
    
    /** 开始游戏 */
    public static final int MSG_START_GAME = 21;
    
    /** 发牌 */
    public static final int MSG_DEAL_CARDS = 30;
    
    /** 叫地主 */
    public static final int MSG_ROB_LANDLORD = 31;
    
    /** 出牌 */
    public static final int MSG_PLAY_CARDS = 32;
    
    /** 不出 */
    public static final int MSG_PASS = 33;
    
    /** 游戏结束 */
    public static final int MSG_GAME_OVER = 40;
    
    /** 聊天 */
    public static final int MSG_CHAT = 50;
    
    /** 踢人 */
    public static final int MSG_KICK = 60;
    
    /** 房间设置 */
    public static final int MSG_ROOM_CONFIG = 61;
    
    /** 游戏状态更新 */
    public static final int MSG_GAME_STATE_UPDATE = 100;

    /** 错误消息 */
    public static final int MSG_ERROR = 999;
    
    // ==================== 扑克牌相关 ====================
    
    /** 扑克牌数量 */
    public static final int POKER_COUNT = 54;
    
    /** 花色数量 */
    public static final int POKER_TYPE_COUNT = 4;
    
    /** 每种面值牌数 */
    public static final int POKER_VALUE_COUNT = 4;
}

package com.cong.fishisland.model.fishbattle.battle;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 统一玩家输入消息。
 * 由 Socket.IO 事件线程创建并入队到 {@link com.cong.fishisland.socketio.battle.InputQueue}，
 * 由 tick 线程在每帧开头统一排空并应用到权威游戏状态。
 * <p>
 * 设计要点：
 * 1. 不可变：创建后不修改，线程间安全传递。
 * 2. 轻量：仅携带必要信息，不含 SocketIOClient 引用。
 * 3. 通用：move / stop / castSpell / basicAttack 等类型复用同一结构。
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PlayerInput {

    /**
     * 输入类型枚举。
     */
    public enum Type {
        /** 移动指令 */
        MOVE,
        /** 停止指令 */
        STOP,
        /** 施法指令（正式技能） */
        CAST_SPELL,
        /** 普攻指令 */
        BASIC_ATTACK
    }

    /** 输入类型 */
    private Type type;

    /** 发起输入的英雄 ID */
    private String championId;

    /** 输入所属战斗房间 ID */
    private String roomId;

    /** 客户端输入序列号（用于 prediction reconciliation） */
    private Long clientSeq;

    /** 客户端时间戳（毫秒） */
    private Long clientTimestamp;

    /** 服务端收到该输入的时间戳（毫秒），由 Socket.IO 线程在入队前设置 */
    private Long serverReceiveTime;

    /** 玩家会话 ID（Socket.IO sessionId），用于安全校验 */
    private String sessionId;

    // ==================== Move / Stop 载荷 ====================

    /** 移动目标点 X */
    private Double targetX;

    /** 移动目标点 Z */
    private Double targetZ;

    /** 输入模式（mouse / keyboard） */
    private String inputMode;

    // ==================== CastSpell / BasicAttack 载荷 ====================

    /** 施法请求 ID（客户端生成，用于追踪施法结果） */
    private String requestId;

    /** 技能槽位（Q/W/E/R/basicAttack） */
    private String slot;

    /** 技能定义 ID */
    private String skillId;

    /** 目标实体 ID（单体技能） */
    private String targetEntityId;

    /** 目标点 X（方向/范围技能） */
    private Double targetPointX;

    /** 目标点 Y（方向/范围技能） */
    private Double targetPointY;

    /** 目标点 Z（方向/范围技能） */
    private Double targetPointZ;

    /** 目标方向 X（方向技能） */
    private Double targetDirectionX;

    /** 目标方向 Y（方向技能） */
    private Double targetDirectionY;

    /** 目标方向 Z（方向技能） */
    private Double targetDirectionZ;

    /** 原始 JSON 载荷（用于兼容复杂技能参数，避免遗漏字段） */
    private com.fasterxml.jackson.databind.JsonNode rawPayload;

    // ==================== 客户端位置修正（STOP/CAST 专用） ====================

    /** 客户端报告的停止位置 X（消除网络延迟造成的位置偏差） */
    private Double clientPositionX;

    /** 客户端报告的停止位置 Z */
    private Double clientPositionZ;
}

package com.cong.fishisland.model.fishbattle.battle;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Map;

/**
 * 服务端权威英雄运行时状态。
 * 当前阶段先承载移动、朝向、冷却与基础施法骨架，后续逐步扩展到 Buff、投射物与复杂技能状态。
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class BattleChampionState {

    /** 英雄实例唯一 ID，格式如 champion_blue_0。 */
    private String id;

    /** 英雄定义 ID（对应 fish_battle_hero 表主键）。 */
    private String heroId;

    /** 当前使用的皮肤标识（为空则使用默认皮肤）。 */
    private String skin;

    /** 英雄 3D 模型 CDN 地址（后端下发，覆盖前端默认配置）。 */
    private String modelUrl;

    /** 玩家显示名称。 */
    private String playerName;

    /** 所属队伍：blue / red。 */
    private String team;

    /** 当前世界坐标。 */
    private BattleVector3 position;

    /** 当前朝向角度（弧度），基于 atan2(dirX, dirZ)。 */
    private Double rotation;

    /** 移动速度，单位：游戏坐标/秒。 */
    private Double moveSpeed;

    /** 基础移动速度，单位：游戏坐标/秒。 */
    private Double baseMoveSpeed;

    /** 移动目标点，为 null 表示静止。 */
    private BattleVector3 moveTarget;

    /** 当前动画状态：idle / run / attack / death / cast 等。 */
    private String animationState;

    /** 输入模式：mouse（鼠标右键点击）/ keyboard（键盘 S 键停止）/ idle（静止）。 */
    private String inputMode;

    /** 是否已死亡（内部逻辑用）。 */
    private Boolean dead;

    /** 是否已死亡（快照输出字段，与 dead 同义）。 */
    private Boolean isDead;

    /** 当前生命值。 */
    private Double hp;

    /** 最大生命值。 */
    private Double maxHp;

    /** 当前法力值。 */
    private Double mp;

    /** 最大法力值。 */
    private Double maxMp;

    /** 当前护盾值（吸收伤害后优先扣减护盾）。 */
    private Double shield;

    /** 英雄被动/特殊机制资源值（如怒气、能量等）。 */
    private Double flowValue;

    /** 基础攻击力。 */
    private Double baseAd;

    /** 基础法术强度。 */
    private Double baseAp;

    /** 基础护甲（物理减伤）。 */
    private Double baseArmor;

    /** 基础魔抗（魔法减伤）。 */
    private Double baseMr;

    /** 普攻射程。 */
    private Double attackRange;

    /** 攻击速度（每秒攻击次数）。 */
    private Double attackSpeed;

    /** 各技能槽位的冷却/等级运行时状态，key 为槽位名（Q/W/E/R/passive）。 */
    private Map<String, Map<String, Object>> skillStates;

    /** 当前正在执行的施法实例 ID（施法期间不为 null）。 */
    private String activeCastInstanceId;

    /** 当前施法阶段：windup（前摇）/ cast（释放）/ recovery（后摇）/ null（空闲）。 */
    private String activeCastPhase;

    /** 移动锁定截止时间戳（施法前摇/后摇期间禁止移动），单位毫秒。 */
    private Long movementLockedUntil;

    /** 进入 idle 动画状态的时间戳，用于触发 standby（待机）动画。 */
    private Long idleStartedAt;

    /** 本英雄最近一次对敌方英雄造成伤害的时间戳（ms），用于防御塔仇恨 P1 判定。 */
    private Long lastAttackedEnemyChampionAt;

    /** 最后处理的移动指令序列号（用于去重与排序）。 */
    private Long lastProcessedMoveSequence;

    /** 最后一条移动指令的客户端时间戳。 */
    private Long lastMoveCommandClientTime;

    /** 最后一条移动指令的服务端接收时间戳。 */
    private Long lastMoveCommandServerTime;

    /**
     * 该英雄最后一次被 tick 引擎处理的客户端输入序列号。
     * 随快照广播给客户端，客户端据此做 reconciliation：
     * 丢弃 seq ≤ lastProcessedInputSeq 的本地预测输入，
     * 对 seq > lastProcessedInputSeq 的输入进行重放。
     */
    private Long lastProcessedInputSeq;
}

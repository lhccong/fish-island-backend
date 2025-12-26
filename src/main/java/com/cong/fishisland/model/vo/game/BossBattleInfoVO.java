package com.cong.fishisland.model.vo.game;

import lombok.Data;

import java.io.Serializable;

/**
 * Boss对战信息视图对象
 * 包含当前用户的宠物信息和Boss信息
 *
 * @author cong
 */
@Data
public class BossBattleInfoVO implements Serializable {

    /**
     * 宠物信息
     */
    private PetInfo petInfo;

    /**
     * Boss信息
     */
    private BossInfo bossInfo;

    private static final long serialVersionUID = 1L;

    /**
     * 宠物信息内部类
     */
    @Data
    public static class PetInfo implements Serializable {
        /**
         * 宠物ID
         */
        private Long petId;

        /**
         * 宠物名称
         */
        private String name;

        /**
         * 宠物头像
         */
        private String avatar;

        /**
         * 宠物等级
         */
        private Integer level;

        /**
         * 宠物攻击力
         */
        private Integer attack;

        /**
         * 宠物血量
         */
        private Integer health;

        private static final long serialVersionUID = 1L;
    }

    /**
     * Boss信息内部类
     */
    @Data
    public static class BossInfo implements Serializable {
        /**
         * Boss ID
         */
        private Long id;

        /**
         * Boss名称
         */
        private String name;

        /**
         * Boss头像
         */
        private String avatar;

        /**
         * Boss攻击力
         */
        private Integer attack;

        /**
         * Boss当前血量
         */
        private Integer currentHealth;

        /**
         * Boss最大血量
         */
        private Integer maxHealth;

        /**
         * 击杀Boss的奖励积分
         */
        private Integer rewardPoints;

        private static final long serialVersionUID = 1L;
    }
}




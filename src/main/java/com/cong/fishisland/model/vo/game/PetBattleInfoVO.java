package com.cong.fishisland.model.vo.game;

import com.cong.fishisland.model.vo.pet.ItemInstanceVO;
import lombok.Data;

import java.io.Serializable;
import java.util.Map;

/**
 * 宠物对战信息视图对象
 * 包含双方宠物的详细信息
 *
 * @author cong
 */
@Data
public class PetBattleInfoVO implements Serializable {

    /**
     * 我的宠物信息
     */
    private PetInfo myPet;

    /**
     * 对手宠物信息
     */
    private PetInfo opponentPet;

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
         * 用户ID
         */
        private Long userId;

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
         * 宠物攻击力（等级 + 装备攻击）
         */
        private Integer attack;

        /**
         * 宠物血量（等级 * 100 + 装备生命值）
         */
        private Integer health;

        /**
         * 宠物已穿戴的装备列表（槽位 -> 装备VO）
         */
        private Map<String, ItemInstanceVO> equippedItems;

        private static final long serialVersionUID = 1L;
    }
}

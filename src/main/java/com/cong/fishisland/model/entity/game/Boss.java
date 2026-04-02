package com.cong.fishisland.model.entity.game;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.io.Serializable;
import java.util.Date;

/**
 * Boss实体
 *
 * @author cong
 */
@TableName(value = "boss")
@Data
public class Boss implements Serializable {

    /**
     * Boss ID
     */
    @TableId(value = "id", type = IdType.AUTO)
    private Long id;

    /**
     * Boss名称
     */
    private String name;

    /**
     * Boss头像URL
     */
    private String avatar;

    /**
     * Boss血量
     */
    private Integer health;

    /**
     * Boss攻击力
     */
    private Integer attack;

    /**
     * 击败奖励积分
     */
    private Integer rewardPoints;

    /**
     * 主动属性 - 暴击率
     */
    private Double critRate;

    /**
     * 主动属性 - 连击率
     */
    private Double comboRate;

    /**
     * 主动属性 - 闪避率
     */
    private Double dodgeRate;

    /**
     * 主动属性 - 格挡率
     */
    private Double blockRate;

    /**
     * 主动属性 - 吸血率
     */
    private Double lifesteal;

    /**
     * 抗性属性 - 抗暴击率
     */
    private Double critResistance;

    /**
     * 抗性属性 - 抗连击率
     */
    private Double comboResistance;

    /**
     * 抗性属性 - 抗闪避率
     */
    private Double dodgeResistance;

    /**
     * 抗性属性 - 抗格挡率
     */
    private Double blockResistance;

    /**
     * 抗性属性 - 抗吸血率
     */
    private Double lifestealResistance;

    /**
     * 排序
     */
    private Integer sort;

    /**
     * 状态：0-禁用，1-启用
     */
    private Integer status;

    /**
     * 创建时间
     */
    private Date createTime;

    /**
     * 更新时间
     */
    private Date updateTime;

    /**
     * 是否删除
     */
    @TableLogic
    private Integer isDelete;

    private static final long serialVersionUID = 1L;
}

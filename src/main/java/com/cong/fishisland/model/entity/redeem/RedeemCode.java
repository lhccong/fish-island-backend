package com.cong.fishisland.model.entity.redeem;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.io.Serializable;
import java.util.Date;

/**
 * 兑换码表
 *
 * @author cong
 */
@TableName(value = "redeem_code")
@Data
public class RedeemCode implements Serializable {

    /**
     * 兑换码ID
     */
    @TableId(type = IdType.AUTO)
    private Long id;

    /**
     * 兑换码（唯一）
     */
    private String code;

    /**
     * 类型：1-通用码（每人限领一次）2-专属码（一次性，仅限指定用户或先到先得）
     */
    private Integer type;

    /**
     * 专属码绑定的目标用户ID，NULL表示不限定用户（先到先得）
     */
    private Long targetUserId;

    /**
     * 奖励类型：1-积分 2-会员天数 3-道具 4-称号 5-头像框
     */
    private Integer rewardType;

    /**
     * 奖励值：积分数量/会员天数/道具ID/称号ID/头像框ID
     */
    private Long rewardValue;

    /**
     * 奖励数量（道具类有效）
     */
    private Integer rewardCount;

    /**
     * 兑换码描述/备注
     */
    private String description;

    /**
     * 过期时间，NULL表示永不过期
     */
    private Date expireTime;

    /**
     * 状态：0-已禁用 1-正常 2-已用完
     */
    private Integer status;

    /**
     * 已使用次数
     */
    private Integer usedCount;

    /**
     * 最大使用次数：-1不限（通用码），专属码固定为1
     */
    private Integer maxUseCount;

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

    @TableField(exist = false)
    private static final long serialVersionUID = 1L;
}

package com.cong.fishisland.model.entity.turntable;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.TableName;
import java.io.Serializable;
import java.util.Date;
import lombok.Data;

/**
 * 转盘表
 * @author cong
 */
@TableName(value = "turntable")
@Data
public class Turntable implements Serializable {
    /**
     * 转盘ID
     */
    @TableId(type = IdType.AUTO)
    private Long id;

    /**
     * 转盘类型 1-宠物装备转盘 2-称号转盘
     */
    private Integer type;

    /**
     * 转盘名称
     */
    private String name;

    /**
     * 每次抽奖消耗积分
     */
    private Integer costPoints;

    /**
     * 保底触发次数，0 表示无保底
     */
    private Integer guaranteeCount;

    /**
     * 状态 1启用 0禁用
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

    @TableField(exist = false)
    private static final long serialVersionUID = 1L;
}

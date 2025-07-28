package com.cong.fishisland.model.entity.props;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.io.Serializable;
import java.util.Date;

/**
 * 道具表
 *
 * @TableName props
 */
@TableName(value = "props")
@Data
public class Props implements Serializable {

    /**
     * 道具 ID
     */
    @TableId(type = IdType.AUTO)
    private Long frameId;

    /**
     * 道具图片地址
     */
    private String imgUrl;

    /**
     * 道具类型 1-摸鱼会员月卡 2-摸鱼称号
     */
    private String type;

    /**
     * 道具描述
     */
    private String description;

    /**
     * 道具名称
     */
    private String name;

    /**
     * 道具所需兑换积分
     */
    private Integer points;

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
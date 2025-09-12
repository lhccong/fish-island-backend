package com.cong.fishisland.model.entity.user;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import java.util.Date;
import lombok.Data;

/**
 * 用户称号
 * @author cong
 * @TableName user_title
 */
@TableName(value ="user_title")
@Data
public class UserTitle {
    /**
     * 称号 ID
     */
    @TableId(value = "titleId", type = IdType.AUTO)
    private Long titleId;

    /**
     * 称号名称
     */
    @TableField(value = "name")
    private String name;

    /**
     * 称号图标
     */
    @TableField(value = "titleImg")
    private String titleImg;

    /**
     * 创建时间
     */
    @TableField(value = "createTime")
    private Date createTime;

    /**
     * 更新时间
     */
    @TableField(value = "updateTime")
    private Date updateTime;

    /**
     * 是否删除
     */
    @TableField(value = "isDelete")
    private Integer isDelete;
}
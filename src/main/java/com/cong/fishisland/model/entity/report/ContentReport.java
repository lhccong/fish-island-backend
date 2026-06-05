package com.cong.fishisland.model.entity.report;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.util.Date;

/**
 * 内容举报表
 *
 * @author cong
 */
@TableName(value = "content_report")
@Data
public class ContentReport {

    @TableId(value = "id", type = IdType.AUTO)
    private Long id;

    @TableField(value = "reporterId")
    private Long reporterId;

    @TableField(value = "reportType")
    private Integer reportType;

    @TableField(value = "targetId")
    private Long targetId;

    @TableField(value = "targetUserId")
    private Long targetUserId;

    @TableField(value = "reasonType")
    private Integer reasonType;

    @TableField(value = "description")
    private String description;

    @TableField(value = "status")
    private Integer status;

    @TableField(value = "handlerId")
    private Long handlerId;

    @TableField(value = "handleRemark")
    private String handleRemark;

    @TableField(value = "handleTime")
    private Date handleTime;

    @TableField(value = "createTime")
    private Date createTime;

    @TableField(value = "updateTime")
    private Date updateTime;

    @TableField(value = "isDelete")
    private Integer isDelete;
}

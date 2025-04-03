package com.cong.fishisland.model.entity.mockInterview;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import java.io.Serializable;
import java.util.Date;
import lombok.Data;

/**
 * 模拟面试
 * @TableName mock_interview
 */
@TableName(value ="mock_interview")
@Data
public class MockInterview implements Serializable {
    /**
     * id
     */
    @TableId(value = "id", type = IdType.AUTO)
    private Long id;

    /**
     * 工作年限
     */
    @TableField(value = "workExperience")
    private String workExperience;

    /**
     * 工作岗位
     */
    @TableField(value = "jobPosition")
    private String jobPosition;

    /**
     * 面试难度
     */
    @TableField(value = "difficulty")
    private String difficulty;

    /**
     * 消息列表（JSON 对象数组字段，同时包括了总结）
     */
    @TableField(value = "messages")
    private String messages;

    /**
     * 状态（0-待开始、1-进行中、2-已结束）
     */
    @TableField(value = "status")
    private Integer status;

    /**
     * 创建人（用户 id）
     */
    @TableField(value = "userId")
    private Long userId;

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
     * 是否删除（逻辑删除）
     */
    @TableField(value = "isDelete")
    private Integer isDelete;

    @TableField(exist = false)
    private static final long serialVersionUID = 1L;
}
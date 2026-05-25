package com.cong.fishisland.model.entity.aiavatar;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import java.io.Serializable;
import java.util.Date;
import lombok.Data;

/**
 * 用户 AI 分身表
 */
@TableName(value = "user_ai_avatar")
@Data
public class UserAiAvatar implements Serializable {

    @TableId(value = "id", type = IdType.AUTO)
    private Long id;

    @TableField(value = "userId")
    private Long userId;

    @TableField(value = "avatarName")
    private String avatarName;

    @TableField(value = "systemPrompt")
    private String systemPrompt;

    /**
     * 是否启用分身：0-关闭，1-开启
     */
    @TableField(value = "enabled")
    private Integer enabled;

    @TableField(value = "createTime")
    private Date createTime;

    @TableField(value = "updateTime")
    private Date updateTime;

    @TableField(value = "isDelete")
    private Integer isDelete;

    @TableField(exist = false)
    private static final long serialVersionUID = 1L;
}

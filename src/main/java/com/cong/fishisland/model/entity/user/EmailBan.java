package com.cong.fishisland.model.entity.user;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import java.io.Serializable;
import java.util.Date;
import lombok.Data;

/**
 * 邮箱封禁表
 * @TableName email_ban
 */
@TableName(value ="email_ban")
@Data
public class EmailBan implements Serializable {
    /**
     * 主键ID
     */
    @TableId(type = IdType.AUTO)
    private Long id;

    /**
     * 被封禁的完整邮箱地址
     */
    private String email;

    /**
     * 邮箱后缀（如 .com、.net）
     */
    private String emailSuffix;

    /**
     * 封禁理由
     */
    private String reason;

    /**
     * 创建时间
     */
    private Date createTime;

    /**
     * 更新时间
     */
    private Date updateTime;

    private String bannedIp;

    @TableField(exist = false)
    private static final long serialVersionUID = 1L;
}
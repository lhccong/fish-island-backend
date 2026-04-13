package com.cong.fishisland.model.entity.chat;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.util.Date;

/**
 * 聊天记录备份表
 *
 * @TableName room_message_backup
 */
@TableName(value = "room_message_backup")
@Data
public class RoomMessageBackup {

    /**
     * 原始消息 id
     */
    @TableId(value = "id")
    private Long id;

    @TableField(value = "userId")
    private Long userId;

    @TableField(value = "roomId")
    private Long roomId;

    @TableField(value = "messageJson")
    private String messageJson;

    @TableField(value = "messageId")
    private String messageId;

    @TableField(value = "createTime")
    private Date createTime;

    @TableField(value = "updateTime")
    private Date updateTime;

    @TableField(value = "isDelete")
    private Integer isDelete;

    /**
     * 备份时间
     */
    @TableField(value = "backupTime")
    private Date backupTime;
}

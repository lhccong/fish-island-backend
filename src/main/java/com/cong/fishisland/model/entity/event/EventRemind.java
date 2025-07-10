package com.cong.fishisland.model.entity.event;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import java.io.Serializable;
import java.util.Date;
import lombok.Data;

/**
 * 事件提醒表
 * @TableName event_remind
 */
@TableName(value ="event_remind")
@Data
public class EventRemind implements Serializable {
    /**
     * id
     */
    @TableId(type = IdType.ASSIGN_ID)
    private Long id;

    /**
     * 动作类型：like-点赞、at-@提及、reply-回复、comment-评论、follow-关注、share-分享
     */
    private String action;

    /**
     * 事件源 ID，如帖子ID、评论ID 等
     */
    private Long sourceId;

    /**
     * 事件源类型：1- 帖子、2- 评论等
     */
    private Integer sourceType;

    /**
     * 事件源的内容，比如回复的内容，回复的评论等等
     */
    private String sourceContent;

    /**
     * 事件所发生的地点链接 url
     */
    private String url;

    /**
     * 是否已读(0-未读、1-已读)
     */
    private Integer state;

    /**
     * 操作者的 ID，即谁关注了你，谁艾特了你
     */
    private Long senderId;

    /**
     * 接受通知的用户的 ID
     */
    private Long recipientId;

    /**
     * 提醒的时间
     */
    private Date remindTime;

    /**
     * 创建时间
     */
    private Date createTime;

    /**
     * 更新时间
     */
    private Date updateTime;

    /**
     * 是否删除（逻辑删除）
     */
    @TableField(exist = false)
    private Integer isDelete;

    @TableField(exist = false)
    private static final long serialVersionUID = 1L;
}
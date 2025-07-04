package com.cong.fishisland.model.entity.comment;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import java.io.Serializable;
import java.util.Date;
import lombok.Data;

/**
 * 评论点赞表
 * @TableName comment_thumb
 */
@TableName(value ="comment_thumb")
@Data
public class CommentThumb implements Serializable {
    /**
     * 评论点赞id
     */
    @TableId(type = IdType.ASSIGN_ID)
    private Long id;

    /**
     * 评论id
     */
    private Long commentId;

    /**
     * 创建用户id
     */
    private Long userId;

    /**
     * 创建时间
     */
    private Date createTime;

    /**
     * 更新时间
     */
    private Date updateTime;

    @TableField(exist = false)
    private static final long serialVersionUID = 1L;
}
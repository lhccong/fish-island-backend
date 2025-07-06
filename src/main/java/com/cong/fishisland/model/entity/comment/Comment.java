package com.cong.fishisland.model.entity.comment;

import com.baomidou.mybatisplus.annotation.*;

import java.io.Serializable;
import java.util.Date;
import lombok.Data;

/**
 * 评论表
 * @TableName comment
 */
@TableName(value ="comment")
@Data
public class Comment implements Serializable {
    /**
     * 评论id
     */
    @TableId(type = IdType.ASSIGN_ID)
    private Long id;

    /**
     * 所属帖子id
     */
    private Long postId;

    /**
     * 评论者用户id
     */
    private Long userId;

    /**
     * 根评论id
     */
    private Long rootId;

    /**
     * 父评论id（为NULL则是顶级评论）
     */
    private Long parentId;

    /**
     * 评论内容
     */
    private String content;

    /**
     * 点赞数
     */
    private Integer thumbNum;

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
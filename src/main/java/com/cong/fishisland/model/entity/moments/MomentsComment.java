package com.cong.fishisland.model.entity.moments;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.io.Serializable;
import java.util.Date;

/**
 * 朋友圈评论实体
 *
 * @author cong
 */
@Data
@TableName("moments_comment")
public class MomentsComment implements Serializable {

    @TableId(value = "id", type = IdType.AUTO)
    private Long id;

    private Long momentId;

    private Long userId;

    /** 被回复的用户ID，NULL 表示直接评论动态 */
    private Long replyUserId;

    /** 父评论ID，NULL 表示顶级评论 */
    private Long parentId;

    private String content;

    private Date createTime;

    private Date updateTime;

    @TableLogic
    private Integer isDelete;

    private static final long serialVersionUID = 1L;
}

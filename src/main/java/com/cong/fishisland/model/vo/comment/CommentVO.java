package com.cong.fishisland.model.vo.comment;

import com.cong.fishisland.model.vo.user.UserVO;
import lombok.Data;

import java.io.Serializable;
import java.util.Date;

/**
 * 评论视图对象
 * @author 许林涛
 * @date 2025年07月04日 11:19
 */
@Data
public class CommentVO implements Serializable {
    /**
     * id
     */
    private Long id;
    /**
     * 帖子id
     */
    private Long postId;
    /**
     * 用户id
     */
    private Long userId;
    /**
     * 父评论id
     */
    private Long parentId;
    /**
     * 内容
     */
    private String content;
    /**
     * 点赞数
     */
    private Integer thumbNum;
    /**
     * 是否已点赞
     */
    private Boolean hasThumb;
    /**
     * 创建时间
     */
    private Date createTime;
    /**
     * 创建人信息
     */
    private UserVO user;
    private static final long serialVersionUID = 1L;
}

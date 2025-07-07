package com.cong.fishisland.model.vo.comment;

import com.cong.fishisland.model.vo.user.UserVO;
import lombok.Data;

import java.util.Date;
import java.util.List;

/**
 * 评论节点
 * @author 许林涛
 * @date 2025年07月04日 11:19
 */
@Data
public class CommentNodeVO {
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
     * 创建时间
     */
    private Date createTime;
    /**
     * 创建人信息
     */
    private UserVO user;
    /**
     * 是否已点赞
     */
    private Boolean hasThumb;
    /**
     * 二级评论总数
     */
    private Integer childCount;
    /**
     *  部分二级评论（默认加载前3条）
     */
    private List<CommentVO> previewChildren;
}

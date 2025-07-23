package com.cong.fishisland.model.vo.post;

import cn.hutool.json.JSONUtil;
import com.cong.fishisland.model.entity.post.Post;
import java.io.Serializable;
import java.util.Date;
import java.util.List;

import com.cong.fishisland.model.vo.comment.CommentVO;
import com.cong.fishisland.model.vo.user.UserVO;
import lombok.Data;
import org.springframework.beans.BeanUtils;

/**
 * 帖子视图
 * # @author <a href="https://github.com/lhccong">程序员聪</a>
 */
@Data
public class PostVO implements Serializable {

    /**
     * id
     */
    private Long id;

    /**
     * 标题
     */
    private String title;

    /**
     * 内容
     */
    private String content;

    /**
     * 封面图片
     */
    private String coverImage;

    /**
     * 点赞数
     */
    private Integer thumbNum;

    /**
     * 收藏数
     */
    private Integer favourNum;

    /**
     * 浏览数
     */
    private Integer viewNum;

    /**
     * 创建用户 id
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

    /**
     * 标签列表
     */
    private List<String> tagList;

    /**
     * 创建人信息
     */
    private UserVO user;

    /**
     * 是否已点赞
     */
    private Boolean hasThumb;

    /**
     * 是否已收藏
     */
    private Boolean hasFavour;

    /**
     * 是否加精（0-普通，1-加精）
     */
    private Integer isFeatured;

    /**
     * 摘要
     */
    private String summary;

    /**
     * 评论数
     */
    private Integer commentNum;

    /**
     * 点赞最高一条评论
     */
    private CommentVO thumbComment;

    /**
     * 包装类转对象
     *
     * @param postVO
     * @return
     */
    public static Post voToObj(PostVO postVO) {
        if (postVO == null) {
            return null;
        }
        Post post = new Post();
        BeanUtils.copyProperties(postVO, post);
        List<String> tagList = postVO.getTagList();
        post.setTags(JSONUtil.toJsonStr(tagList));
        return post;
    }

    /**
     * 对象转包装类
     *
     * @param post
     * @return
     */
    public static PostVO objToVo(Post post) {
        if (post == null) {
            return null;
        }
        PostVO postVO = new PostVO();
        BeanUtils.copyProperties(post, postVO);
        postVO.setTagList(JSONUtil.toList(post.getTags(), String.class));
        return postVO;
    }
}

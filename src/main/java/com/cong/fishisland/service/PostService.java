package com.cong.fishisland.service;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.IService;
import com.cong.fishisland.model.dto.post.PostFeaturedRequest;
import com.cong.fishisland.model.dto.post.PostQueryRequest;
import com.cong.fishisland.model.entity.post.Post;
import com.cong.fishisland.model.vo.post.PostVO;

/**
 * 帖子服务
 *
 * # @author <a href="https://github.com/lhccong">程序员聪</a>
 */
public interface PostService extends IService<Post> {

    /**
     * 校验
     *
     * @param post 发布
     * @param add  加
     */
    void validPost(Post post, boolean add);

    /**
     * 获取查询条件
     *
     * @param postQueryRequest 发布查询请求
     * @return {@link QueryWrapper}<{@link Post}>
     */
    QueryWrapper<Post> getQueryWrapper(PostQueryRequest postQueryRequest);

    /**
     * 从 ES 查询
     *
     * @param postQueryRequest 发布查询请求
     * @return {@link Page}<{@link Post}>
     */
    Page<Post> searchFromEs(PostQueryRequest postQueryRequest);

    /**
     * 获取帖子封装
     *
     * @param post    发布
     * @return {@link PostVO}
     */
    PostVO getPostVO(Post post);

    /**
     * 分页获取帖子封装
     *
     * @param postPage 帖子页面
     * @return {@link Page}<{@link PostVO}>
     */
    Page<PostVO> getPostVOPage(Page<Post> postPage);

    /**
     * 增加帖子浏览数
     * @param postId 帖子id
     */
    void incrementViewCountAsync(Long postId);

    /**
     * 设置帖子加精状态
     * @param request 请求
     * @return 布尔值
     */
    Boolean setFeaturedStatus(PostFeaturedRequest request);
}

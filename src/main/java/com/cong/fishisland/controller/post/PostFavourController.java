package com.cong.fishisland.controller.post;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.cong.fishisland.common.BaseResponse;
import com.cong.fishisland.common.ErrorCode;
import com.cong.fishisland.common.ResultUtils;
import com.cong.fishisland.common.exception.BusinessException;
import com.cong.fishisland.common.exception.ThrowUtils;
import com.cong.fishisland.model.dto.post.PostQueryRequest;
import com.cong.fishisland.model.dto.postfavour.PostFavourAddRequest;
import com.cong.fishisland.model.dto.postfavour.PostFavourQueryRequest;
import com.cong.fishisland.model.entity.post.Post;
import com.cong.fishisland.model.entity.user.User;
import com.cong.fishisland.model.vo.post.PostVO;
import com.cong.fishisland.service.PostFavourService;
import com.cong.fishisland.service.PostService;
import com.cong.fishisland.service.UserService;

import javax.annotation.Resource;

import io.swagger.annotations.ApiOperation;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 帖子收藏接口
 * # @author <a href="https://github.com/lhccong">程序员聪</a>
 */
@RestController
@RequestMapping("/post_favour")
@Slf4j
//@Api(tags = "帖子收藏")
public class PostFavourController {

    @Resource
    private PostFavourService postFavourService;

    @Resource
    private PostService postService;

    @Resource
    private UserService userService;

    /**
     * 收藏 / 取消收藏
     *
     * @param postFavourAddRequest 发布收藏添加请求
     * @return resultNum 收藏变化数
     */
    @PostMapping("/")
    @ApiOperation(value = "收藏 / 取消收藏")
    public BaseResponse<Integer> doPostFavour(@RequestBody PostFavourAddRequest postFavourAddRequest) {
        if (postFavourAddRequest == null || postFavourAddRequest.getPostId() <= 0) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }
        // 登录才能操作
        final User loginUser = userService.getLoginUser();
        long postId = postFavourAddRequest.getPostId();
        int result = postFavourService.doPostFavour(postId, loginUser);
        return ResultUtils.success(result);
    }


    /**
     * 获取我收藏的帖子列表
     *
     * @param postQueryRequest 发布查询请求
     * @return {@link BaseResponse}<{@link Page}<{@link PostVO}>>
     */
    @PostMapping("/my/list/page")
    @ApiOperation(value = "获取我收藏的帖子列表")
    public BaseResponse<Page<PostVO>> listMyFavourPostByPage(@RequestBody PostQueryRequest postQueryRequest) {
        if (postQueryRequest == null) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }
        User loginUser = userService.getLoginUser();
        long current = postQueryRequest.getCurrent();
        long size = postQueryRequest.getPageSize();
        // 限制爬虫
        ThrowUtils.throwIf(size > 20, ErrorCode.PARAMS_ERROR);
        Page<Post> postPage = postFavourService.listFavourPostByPage(new Page<>(current, size),
                postService.getQueryWrapper(postQueryRequest), loginUser.getId());
        return ResultUtils.success(postService.getPostVOPage(postPage));
    }

    /**
     * 获取用户收藏的帖子列表
     *
     * @param postFavourQueryRequest 发布优惠查询请求
     * @return {@link BaseResponse}<{@link Page}<{@link PostVO}>>
     */
    @PostMapping("/list/page")
    @ApiOperation(value = "获取用户收藏的帖子列表")
    public BaseResponse<Page<PostVO>> listFavourPostByPage(@RequestBody PostFavourQueryRequest postFavourQueryRequest) {
        if (postFavourQueryRequest == null) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }
        long current = postFavourQueryRequest.getCurrent();
        long size = postFavourQueryRequest.getPageSize();
        Long userId = postFavourQueryRequest.getUserId();
        // 限制爬虫
        ThrowUtils.throwIf(size > 20 || userId == null, ErrorCode.PARAMS_ERROR);
        Page<Post> postPage = postFavourService.listFavourPostByPage(new Page<>(current, size),
                postService.getQueryWrapper(postFavourQueryRequest.getPostQueryRequest()), userId);
        return ResultUtils.success(postService.getPostVOPage(postPage));
    }
}

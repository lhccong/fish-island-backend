package com.cong.fishisland.controller.moments;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.cong.fishisland.common.BaseResponse;
import com.cong.fishisland.common.DeleteRequest;
import com.cong.fishisland.common.ResultUtils;import com.cong.fishisland.model.dto.moments.MomentsAddRequest;
import com.cong.fishisland.model.dto.moments.MomentsCommentAddRequest;
import com.cong.fishisland.model.dto.moments.MomentsCommentQueryRequest;
import com.cong.fishisland.model.dto.moments.MomentsLikeRequest;
import com.cong.fishisland.model.dto.moments.MomentsQueryRequest;
import com.cong.fishisland.model.dto.moments.MomentsRewardRequest;
import com.cong.fishisland.model.dto.moments.MomentsUpdateRequest;
import com.cong.fishisland.model.vo.moments.MomentsCommentVO;
import com.cong.fishisland.model.vo.moments.MomentsVO;
import com.cong.fishisland.service.moments.MomentsService;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

/**
 * 朋友圈接口
 *
 * @author cong
 */
@RestController
@RequestMapping("/moments")
@Slf4j
@RequiredArgsConstructor
//@Api(tags = "朋友圈接口")
public class MomentsController {

    private final MomentsService momentsService;

    /**
     * 发布动态
     */
    @PostMapping("/publish")
    @ApiOperation("发布朋友圈动态")
    public BaseResponse<Long> publishMoment(@RequestBody MomentsAddRequest request) {
        return ResultUtils.success(momentsService.publishMoment(request));
    }

    /**
     * 修改动态（本人或管理员）
     */
    @PostMapping("/update")
    @ApiOperation("修改朋友圈动态（本人或管理员）")
    public BaseResponse<Boolean> updateMoment(@RequestBody MomentsUpdateRequest request) {
        momentsService.updateMoment(request);
        return ResultUtils.success(true);
    }

    /**
     * 删除动态（本人或管理员）
     */
    @PostMapping("/delete")
    @ApiOperation("删除朋友圈动态（本人或管理员）")
    public BaseResponse<Boolean> deleteMoment(@RequestBody DeleteRequest deleteRequest) {
        momentsService.deleteMoment(Long.parseLong(deleteRequest.getId()));
        return ResultUtils.success(true);
    }

    /**
     * 分页查询动态列表
     */
    @PostMapping("/list")
    @ApiOperation("分页查询朋友圈动态")
    public BaseResponse<Page<MomentsVO>> listMoments(@RequestBody MomentsQueryRequest request) {
        return ResultUtils.success(momentsService.listMoments(request));
    }

    /**
     * 打赏动态
     */
    @PostMapping("/reward")
    @ApiOperation("打赏朋友圈动态（消耗 usedPoints）")
    public BaseResponse<Boolean> rewardMoment(@RequestBody MomentsRewardRequest request) {
        momentsService.rewardMoment(request);
        return ResultUtils.success(true);
    }

    /**
     * 点赞 / 取消点赞
     */
    @PostMapping("/like")
    @ApiOperation("点赞或取消点赞")
    public BaseResponse<Boolean> toggleLike(@RequestBody MomentsLikeRequest request) {
        return ResultUtils.success(momentsService.toggleLike(request.getMomentId()));
    }

    /**
     * 发表评论
     */
    @PostMapping("/comment/add")
    @ApiOperation("发表评论")
    public BaseResponse<Long> addComment(@RequestBody MomentsCommentAddRequest request) {
        return ResultUtils.success(momentsService.addComment(request));
    }

    /**
     * 删除评论
     */
    @PostMapping("/comment/delete")
    @ApiOperation("删除评论")
    public BaseResponse<Boolean> deleteComment(@RequestBody DeleteRequest deleteRequest) {
        momentsService.deleteComment(Long.parseLong(deleteRequest.getId()));
        return ResultUtils.success(true);
    }

    /**
     * 获取动态评论列表
     */
    @PostMapping("/comment/list")
    @ApiOperation("获取动态评论列表（顶级评论分页，子评论全量挂载）")
    public BaseResponse<Page<MomentsCommentVO>> listComments(@RequestBody MomentsCommentQueryRequest request) {
        return ResultUtils.success(momentsService.listComments(request));
    }
}

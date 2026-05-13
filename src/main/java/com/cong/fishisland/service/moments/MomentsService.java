package com.cong.fishisland.service.moments;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.IService;
import com.cong.fishisland.model.dto.moments.MomentsAddRequest;
import com.cong.fishisland.model.dto.moments.MomentsCommentAddRequest;
import com.cong.fishisland.model.dto.moments.MomentsCommentQueryRequest;
import com.cong.fishisland.model.dto.moments.MomentsCommentTopRequest;
import com.cong.fishisland.model.dto.moments.MomentsLotteryRequest;
import com.cong.fishisland.model.dto.moments.MomentsQueryRequest;
import com.cong.fishisland.model.dto.moments.MomentsRewardRequest;
import com.cong.fishisland.model.dto.moments.MomentsTopRequest;
import com.cong.fishisland.model.dto.moments.MomentsUpdateRequest;
import com.cong.fishisland.model.entity.moments.Moments;
import com.cong.fishisland.model.vo.moments.MomentsCommentVO;
import com.cong.fishisland.model.vo.moments.MomentsLotteryVO;
import com.cong.fishisland.model.vo.moments.MomentsVO;

/**
 * 朋友圈服务
 *
 * @author cong
 */
public interface MomentsService extends IService<Moments> {

    /**
     * 发布动态
     */
    Long publishMoment(MomentsAddRequest request);

    /**
     * 修改动态（本人或管理员）
     */
    void updateMoment(MomentsUpdateRequest request);

    /**
     * 删除动态（本人或管理员）
     */
    void deleteMoment(Long momentId);

    /**
     * 分页查询动态列表
     */
    Page<MomentsVO> listMoments(MomentsQueryRequest request);

    /**
     * 打赏动态（消耗打赏者的 usedPoints，增加作者的 usedPoints）
     */
    void rewardMoment(MomentsRewardRequest request);

    /**
     * 点赞 / 取消点赞
     *
     * @return true=点赞成功，false=取消点赞
     */
    boolean toggleLike(Long momentId);

    /**
     * 发表评论
     */
    Long addComment(MomentsCommentAddRequest request);

    /**
     * 删除评论（仅本人）
     */
    void deleteComment(Long commentId);

    /**
     * 获取动态评论列表（树形结构，仅顶级评论分页，子评论全量挂载）
     */
    Page<MomentsCommentVO> listComments(MomentsCommentQueryRequest request);

    /**
     * 查询朋友圈动态详情
     */
    MomentsVO getMomentDetail(Long id);

    /**
     * 朋友圈抽奖：从点赞用户中随机抽取指定数量的中奖者，并自动在评论区发布结果
     */
    MomentsLotteryVO startLottery(MomentsLotteryRequest request);

    /**
     * 置顶/取消置顶动态（仅管理员）
     */
    void topMoment(MomentsTopRequest request);

    /**
     * 置顶/取消置顶评论（动态发布者或管理员）
     */
    void topComment(MomentsCommentTopRequest request);
}

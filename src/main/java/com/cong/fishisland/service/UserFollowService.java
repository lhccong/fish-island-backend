package com.cong.fishisland.service;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.IService;
import com.cong.fishisland.model.entity.user.UserFollow;
import com.cong.fishisland.model.vo.user.UserFollowVO;

import java.util.List;
import java.util.Map;

/**
 * 用户关注 Service
 *
 * @author <a href="https://github.com/lhccong">程序员聪</a>
 */
public interface UserFollowService extends IService<UserFollow> {

    /**
     * 关注 / 取消关注
     *
     * @param followUserId 被关注用户 ID（字符串，防止前端精度丢失）
     * @return true=关注成功，false=取消关注成功
     */
    boolean toggleFollow(String followUserId);

    /**
     * 查询我的关注列表（分页）
     *
     * @param current  当前页
     * @param pageSize 每页大小
     * @return 关注用户分页列表
     */
    Page<UserFollowVO> listMyFollowing(int current, int pageSize);

    /**
     * 查询我的粉丝列表（分页）
     *
     * @param current  当前页
     * @param pageSize 每页大小
     * @return 粉丝用户分页列表
     */
    Page<UserFollowVO> listMyFollowers(int current, int pageSize);

    /**
     * 判断当前登录用户是否已关注指定用户
     *
     * @param followUserId 目标用户 ID（字符串，防止前端精度丢失）
     * @return true=已关注
     */
    boolean isFollowing(String followUserId);

    /**
     * 批量统计多个用户各自的关注数
     *
     * @param userIds 用户 ID 列表
     * @return Map&lt;userId, 关注数&gt;
     */
    Map<Long, Long> batchCountFollowing(List<Long> userIds);

    /**
     * 批量统计多个用户各自的粉丝数
     *
     * @param userIds 用户 ID 列表
     * @return Map&lt;userId, 粉丝数&gt;
     */
    Map<Long, Long> batchCountFollowers(List<Long> userIds);

    /**
     * 统计指定用户的关注数（TA 关注了多少人）
     *
     * @param userId 用户 ID
     * @return 关注数
     */
    long countFollowing(Long userId);

    /**
     * 统计指定用户的粉丝数（多少人关注了 TA）
     *
     * @param userId 用户 ID
     * @return 粉丝数
     */
    long countFollowers(Long userId);
}

package com.cong.fishisland.service.impl.user;

import cn.dev33.satoken.stp.StpUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.cong.fishisland.common.ErrorCode;
import com.cong.fishisland.common.exception.BusinessException;
import com.cong.fishisland.common.exception.ThrowUtils;
import com.cong.fishisland.mapper.user.UserFollowMapper;
import com.cong.fishisland.mapper.user.UserMapper;
import com.cong.fishisland.model.entity.user.User;
import com.cong.fishisland.model.entity.user.UserFollow;
import com.cong.fishisland.model.vo.user.UserFollowVO;
import com.cong.fishisland.service.UserFollowService;
import com.cong.fishisland.service.event.EventRemindHandler;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.annotation.Resource;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * 用户关注 Service 实现
 *
 * @author <a href="https://github.com/lhccong">程序员聪</a>
 */
@Service
@Slf4j
public class UserFollowServiceImpl extends ServiceImpl<UserFollowMapper, UserFollow>
        implements UserFollowService {

    @Resource
    private UserMapper userMapper;

    @Resource
    private EventRemindHandler eventRemindHandler;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean toggleFollow(String followUserIdStr) {
        ThrowUtils.throwIf(followUserIdStr == null, ErrorCode.PARAMS_ERROR, "被关注用户ID不合法");
        Long followUserId;
        try {
            followUserId = Long.parseLong(followUserIdStr);
        } catch (NumberFormatException e) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "被关注用户ID不合法");
        }
        ThrowUtils.throwIf(followUserId <= 0, ErrorCode.PARAMS_ERROR, "被关注用户ID不合法");

        Long loginUserId = Long.parseLong(StpUtil.getLoginId().toString());
        ThrowUtils.throwIf(loginUserId.equals(followUserId), ErrorCode.PARAMS_ERROR, "不能关注自己");

        // 被关注用户必须存在
        User targetUser = userMapper.selectById(followUserId);
        ThrowUtils.throwIf(targetUser == null, ErrorCode.NOT_FOUND_ERROR, "用户不存在");

        // 查询是否已关注
        UserFollow existing = this.baseMapper.selectOne(
                new LambdaQueryWrapper<UserFollow>()
                        .eq(UserFollow::getUserId, loginUserId)
                        .eq(UserFollow::getFollowUserId, followUserId)
        );

        if (existing != null) {
            // 已关注 → 取消关注（物理删除，避免唯一索引冲突）
            this.baseMapper.deleteById(existing.getId());
            log.info("[UserFollow] 取消关注，userId={}, followUserId={}", loginUserId, followUserId);
            return false;
        } else {
            // 未关注 → 新增关注
            UserFollow follow = new UserFollow();
            follow.setUserId(loginUserId);
            follow.setFollowUserId(followUserId);
            this.save(follow);
            log.info("[UserFollow] 新增关注，userId={}, followUserId={}", loginUserId, followUserId);
            // 异步发送关注事件提醒
            eventRemindHandler.handleFollow(loginUserId, followUserId);
            return true;
        }
    }

    @Override
    public Page<UserFollowVO> listMyFollowing(int current, int pageSize) {
        Long loginUserId = Long.parseLong(StpUtil.getLoginId().toString());

        Page<UserFollow> followPage = this.page(
                new Page<>(current, pageSize),
                new LambdaQueryWrapper<UserFollow>()
                        .eq(UserFollow::getUserId, loginUserId)
                        .orderByDesc(UserFollow::getCreateTime)
        );

        return convertToUserFollowVOPage(followPage, loginUserId, true);
    }

    @Override
    public Page<UserFollowVO> listMyFollowers(int current, int pageSize) {
        Long loginUserId = Long.parseLong(StpUtil.getLoginId().toString());

        Page<UserFollow> followPage = this.page(
                new Page<>(current, pageSize),
                new LambdaQueryWrapper<UserFollow>()
                        .eq(UserFollow::getFollowUserId, loginUserId)
                        .orderByDesc(UserFollow::getCreateTime)
        );

        return convertToUserFollowVOPage(followPage, loginUserId, false);
    }

    @Override
    public boolean isFollowing(String followUserIdStr) {
        if (!StpUtil.isLogin()) {
            return false;
        }
        Long followUserId;
        try {
            followUserId = Long.parseLong(followUserIdStr);
        } catch (NumberFormatException e) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "被关注用户ID不合法");
        }
        Long loginUserId = Long.parseLong(StpUtil.getLoginId().toString());
        return this.count(
                new LambdaQueryWrapper<UserFollow>()
                        .eq(UserFollow::getUserId, loginUserId)
                        .eq(UserFollow::getFollowUserId, followUserId)
        ) > 0;
    }

    @Override
    public long countFollowing(Long userId) {
        return this.baseMapper.countFollowing(userId);
    }

    @Override
    public long countFollowers(Long userId) {
        return this.baseMapper.countFollowers(userId);
    }

    @Override
    public Map<Long, Long> batchCountFollowing(List<Long> userIds) {
        if (userIds == null || userIds.isEmpty()) {
            return new HashMap<>();
        }
        return toCountMap(this.baseMapper.batchCountFollowing(userIds), "userId");
    }

    @Override
    public Map<Long, Long> batchCountFollowers(List<Long> userIds) {
        if (userIds == null || userIds.isEmpty()) {
            return new HashMap<>();
        }
        return toCountMap(this.baseMapper.batchCountFollowers(userIds), "followUserId");
    }

    // ==================== 私有方法 ====================

    /**
     * 将关注记录分页转换为 VO 分页
     *
     * @param followPage  关注记录分页
     * @param loginUserId 当前登录用户 ID
     * @param isFollowing true=查关注列表（取 followUserId），false=查粉丝列表（取 userId）
     */
    private Page<UserFollowVO> convertToUserFollowVOPage(
            Page<UserFollow> followPage, Long loginUserId, boolean isFollowing) {

        Page<UserFollowVO> voPage = new Page<>(followPage.getCurrent(), followPage.getSize(), followPage.getTotal());

        List<UserFollow> records = followPage.getRecords();
        if (records.isEmpty()) {
            voPage.setRecords(new ArrayList<>());
            return voPage;
        }

        // 收集需要查询的用户 ID
        Set<Long> userIds = records.stream()
                .map(f -> isFollowing ? f.getFollowUserId() : f.getUserId())
                .collect(Collectors.toSet());

        // 批量查询用户信息（直接走 mapper，避免循环依赖）
        List<User> users = userMapper.selectBatchIds(userIds);
        Map<Long, User> userMap = users.stream().collect(Collectors.toMap(User::getId, u -> u));

        // 判断互粉：两个场景方向不同
        // 关注列表：userIds 是我关注的人，互粉 = 对方也关注了我（对方 userId in userIds，followUserId = loginUserId）
        // 粉丝列表：userIds 是关注我的粉丝，互粉 = 我也关注了对方（我 userId = loginUserId，followUserId in userIds）
        Set<Long> mutualIds;
        if (isFollowing) {
            // 查"我关注的人"中谁也关注了我
            mutualIds = this.list(
                    new LambdaQueryWrapper<UserFollow>()
                            .eq(UserFollow::getFollowUserId, loginUserId)
                            .in(UserFollow::getUserId, userIds)
            ).stream().map(UserFollow::getUserId).collect(Collectors.toSet());
        } else {
            // 查"我的粉丝"中我有没有回关
            mutualIds = this.list(
                    new LambdaQueryWrapper<UserFollow>()
                            .eq(UserFollow::getUserId, loginUserId)
                            .in(UserFollow::getFollowUserId, userIds)
            ).stream().map(UserFollow::getFollowUserId).collect(Collectors.toSet());
        }

        // 组装 VO
        List<UserFollowVO> voList = records.stream()
                .map(f -> {
                    Long targetUserId = isFollowing ? f.getFollowUserId() : f.getUserId();
                    User user = userMap.get(targetUserId);
                    if (user == null) {
                        return null;
                    }
                    UserFollowVO vo = new UserFollowVO();
                    vo.setUserId(String.valueOf(user.getId()));
                    vo.setUserName(user.getUserName());
                    vo.setUserAvatar(user.getUserAvatar());
                    vo.setUserProfile(user.getUserProfile());
                    vo.setAvatarFramerUrl(user.getAvatarFramerUrl());
                    vo.setFollowTime(f.getCreateTime());
                    vo.setIsMutual(mutualIds.contains(targetUserId));
                    return vo;
                })
                .filter(vo -> vo != null)
                .collect(Collectors.toList());

        voPage.setRecords(voList);
        return voPage;
    }

    /**
     * 将 mapper 返回的 [{key: x, cnt: n}] 转为 Map<Long, Long>
     */
    private Map<Long, Long> toCountMap(List<Map<String, Object>> rows, String keyField) {
        return rows.stream().collect(Collectors.toMap(
                m -> ((Number) m.get(keyField)).longValue(),
                m -> ((Number) m.get("cnt")).longValue()
        ));
    }
}

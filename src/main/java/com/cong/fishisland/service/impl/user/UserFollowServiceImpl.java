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
import com.cong.fishisland.model.vo.user.UserFollowRelationshipVO;
import com.cong.fishisland.service.UserFollowService;
import com.cong.fishisland.service.event.EventRemindHandler;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DuplicateKeyException;
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
        long followUserId = parseTargetId(followUserIdStr);
        long loginUserId = StpUtil.getLoginIdAsLong();
        validateTarget(loginUserId, followUserId);

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
        }
        insertFollow(loginUserId, followUserId);
        return true;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void addFollow(String followUserIdStr) {
        long followUserId = parseTargetId(followUserIdStr);
        long loginUserId = StpUtil.getLoginIdAsLong();
        validateTarget(loginUserId, followUserId);
        if (!isFollowing(loginUserId, followUserId)) {
            insertFollow(loginUserId, followUserId);
        }
    }

    private void validateTarget(long loginUserId, long followUserId) {
        ThrowUtils.throwIf(loginUserId == followUserId, ErrorCode.PARAMS_ERROR, "不能关注自己");
        User targetUser = userMapper.selectById(followUserId);
        ThrowUtils.throwIf(targetUser == null, ErrorCode.NOT_FOUND_ERROR, "用户不存在");
    }

    private void insertFollow(long loginUserId, long followUserId) {
        UserFollow follow = new UserFollow();
        follow.setUserId(loginUserId);
        follow.setFollowUserId(followUserId);
        try {
            this.baseMapper.insert(follow);
        } catch (DuplicateKeyException alreadyFollowing) {
            return;
        }
        log.info("[UserFollow] 新增关注，userId={}, followUserId={}", loginUserId, followUserId);
        eventRemindHandler.handleFollow(loginUserId, followUserId);
    }

    @Override
    public Page<UserFollowVO> listMyFollowing(int current, int pageSize) {
        Long loginUserId = Long.parseLong(StpUtil.getLoginId().toString());
        return listFollows(loginUserId, true, current, pageSize, true);
    }

    @Override
    public Page<UserFollowVO> listMyFollowers(int current, int pageSize) {
        Long loginUserId = Long.parseLong(StpUtil.getLoginId().toString());
        return listFollows(loginUserId, false, current, pageSize, true);
    }

    @Override
    public Page<UserFollowVO> listUserFollows(long ownerId, boolean following, int current, int pageSize) {
        long viewerId = Long.parseLong(StpUtil.getLoginId().toString());
        ThrowUtils.throwIf(ownerId <= 0 || current < 1 || pageSize < 1 || pageSize > 50,
                ErrorCode.PARAMS_ERROR, "用户或分页参数不合法");
        if (ownerId != viewerId) {
            long ownerCount = userMapper.selectCount(new LambdaQueryWrapper<User>().eq(User::getId, ownerId));
            ThrowUtils.throwIf(ownerCount == 0, ErrorCode.NOT_FOUND_ERROR, "用户不存在");
        }
        return listFollows(ownerId, following, current, pageSize, ownerId == viewerId);
    }

    private Page<UserFollowVO> listFollows(long ownerId, boolean following, int current,
                                           int pageSize, boolean includeFollowTime) {
        Page<UserFollow> followPage = this.page(
                new Page<>(current, pageSize),
                new LambdaQueryWrapper<UserFollow>()
                        .eq(following ? UserFollow::getUserId : UserFollow::getFollowUserId, ownerId)
                        .inSql(following ? UserFollow::getFollowUserId : UserFollow::getUserId,
                                "SELECT id FROM `user` WHERE isDelete = 0")
                        .orderByDesc(UserFollow::getCreateTime, UserFollow::getId)
        );
        return convertToUserFollowVOPage(followPage, ownerId, following, includeFollowTime);
    }

    @Override
    public boolean isFollowing(String followUserIdStr) {
        if (!StpUtil.isLogin()) {
            return false;
        }
        return isFollowing(StpUtil.getLoginIdAsLong(), parseTargetId(followUserIdStr));
    }

    @Override
    public UserFollowRelationshipVO followRelationship(String targetId) {
        long viewer = StpUtil.getLoginIdAsLong();
        long target = parseTargetId(targetId);
        return new UserFollowRelationshipVO(isFollowing(viewer, target), isFollowing(target, viewer));
    }

    private static long parseTargetId(String targetId) {
        try {
            long id = Long.parseLong(targetId);
            ThrowUtils.throwIf(id <= 0, ErrorCode.PARAMS_ERROR, "被关注用户ID不合法");
            return id;
        } catch (NumberFormatException e) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "被关注用户ID不合法");
        }
    }

    private boolean isFollowing(long userId, long followUserId) {
        return this.count(
                new LambdaQueryWrapper<UserFollow>()
                        .eq(UserFollow::getUserId, userId)
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

    @Override
    public boolean isMutualFollow(Long userId, Long targetUserId) {
        if (userId == null || targetUserId == null || userId.equals(targetUserId)) {
            return false;
        }
        boolean followsTarget = this.count(
                new LambdaQueryWrapper<UserFollow>()
                        .eq(UserFollow::getUserId, userId)
                        .eq(UserFollow::getFollowUserId, targetUserId)
        ) > 0;
        if (!followsTarget) {
            return false;
        }
        return this.count(
                new LambdaQueryWrapper<UserFollow>()
                        .eq(UserFollow::getUserId, targetUserId)
                        .eq(UserFollow::getFollowUserId, userId)
        ) > 0;
    }

    @Override
    public List<Long> listMutualFollowUserIds(Long userId) {
        if (userId == null) {
            return new ArrayList<>();
        }
        List<Long> mutualIds = this.baseMapper.listMutualFollowUserIds(userId);
        return mutualIds != null ? mutualIds : new ArrayList<>();
    }

    @Override
    public long countMutualFollows(Long userId) {
        if (userId == null) {
            return 0;
        }
        return this.baseMapper.countMutualFollows(userId);
    }

    // ==================== 私有方法 ====================

    /**
     * 将关注记录分页转换为 VO 分页
     *
     * @param followPage  关注记录分页
     * @param ownerId 名单所属用户 ID
     * @param isFollowing true=查关注列表（取 followUserId），false=查粉丝列表（取 userId）
     * @param includeFollowTime 是否为本人名单
     */
    private Page<UserFollowVO> convertToUserFollowVOPage(
            Page<UserFollow> followPage, long ownerId, boolean isFollowing, boolean includeFollowTime) {

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
        // 关注列表：查询哪些用户也关注了名单所属用户。
        // 粉丝列表：查询名单所属用户已经回关了哪些粉丝。
        Set<Long> mutualIds;
        if (isFollowing) {
            mutualIds = this.list(
                    new LambdaQueryWrapper<UserFollow>()
                            .eq(UserFollow::getFollowUserId, ownerId)
                            .in(UserFollow::getUserId, userIds)
            ).stream().map(UserFollow::getUserId).collect(Collectors.toSet());
        } else {
            mutualIds = this.list(
                    new LambdaQueryWrapper<UserFollow>()
                            .eq(UserFollow::getUserId, ownerId)
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
                    if (includeFollowTime) {
                        vo.setFollowTime(f.getCreateTime());
                    }
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

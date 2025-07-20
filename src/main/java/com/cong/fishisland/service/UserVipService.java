package com.cong.fishisland.service;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.IService;
import com.cong.fishisland.model.dto.user.UserVipAddRequest;
import com.cong.fishisland.model.dto.user.UserVipQueryRequest;
import com.cong.fishisland.model.dto.user.UserVipUpdateRequest;
import com.cong.fishisland.model.entity.user.UserVip;
import com.cong.fishisland.model.vo.user.UserVipVO;

/**
 * @author cong
 * @description 针对表【user_vip(用户会员表)】的数据库操作Service
 * @createDate 2025-05-01 10:00:00
 */
public interface UserVipService extends IService<UserVip> {

    /**
     * 获取查询条件
     *
     * @param userVipQueryRequest 会员查询请求
     * @return {@link QueryWrapper}<{@link UserVip}>
     */
    QueryWrapper<UserVip> getQueryWrapper(UserVipQueryRequest userVipQueryRequest);

    /**
     * 创建会员
     *
     * @param userVipAddRequest 会员添加请求
     * @return 会员ID
     */
    Long createVip(UserVipAddRequest userVipAddRequest);

    /**
     * 更新会员
     *
     * @param userVipUpdateRequest 会员更新请求
     * @return 是否成功
     */
    boolean updateVip(UserVipUpdateRequest userVipUpdateRequest);

    /**
     * 获取会员视图对象
     *
     * @param userVip 会员
     * @return {@link UserVipVO}
     */
    UserVipVO getVipVO(UserVip userVip);

    /**
     * 分页获取会员视图对象
     *
     * @param userVipPage 会员分页
     * @return {@link Page}<{@link UserVipVO}>
     */
    Page<UserVipVO> getVipVOPage(Page<UserVip> userVipPage);

    /**
     * 检查用户是否是会员
     *
     * @param userId 用户ID
     * @return 是否是会员
     */
    boolean isUserVip(Long userId);

    /**
     * 检查用户是否是永久会员
     *
     * @param userId 用户ID
     * @return 是否是永久会员
     */
    boolean isPermanentVip(Long userId);

    /**
     * 检查用户会员是否过期
     *
     * @param userId 用户ID
     * @return 是否过期
     */
    boolean isVipExpired(Long userId);
} 
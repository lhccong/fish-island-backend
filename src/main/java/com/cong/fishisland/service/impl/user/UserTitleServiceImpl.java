package com.cong.fishisland.service.impl.user;

import com.alibaba.fastjson.JSON;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.cong.fishisland.common.ErrorCode;
import com.cong.fishisland.common.exception.BusinessException;
import com.cong.fishisland.common.exception.ThrowUtils;
import com.cong.fishisland.constant.CommonConstant;
import com.cong.fishisland.constant.UserConstant;
import com.cong.fishisland.model.dto.user.UserTitleQueryRequest;
import com.cong.fishisland.model.entity.user.User;
import com.cong.fishisland.model.entity.user.UserTitle;
import com.cong.fishisland.service.UserService;
import com.cong.fishisland.service.UserTitleService;
import com.cong.fishisland.mapper.user.UserTitleMapper;
import com.cong.fishisland.utils.SqlUtils;
import lombok.RequiredArgsConstructor;
import org.apache.commons.lang3.StringUtils;
import org.jetbrains.annotations.NotNull;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;

import javax.annotation.Resource;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * @author cong
 * @description 针对表【user_title(用户称号)】的数据库操作Service实现
 * @createDate 2025-04-30 10:07:06
 */
@Service
@RequiredArgsConstructor
public class UserTitleServiceImpl extends ServiceImpl<UserTitleMapper, UserTitle>
        implements UserTitleService {
    @Resource
    private UserService userService;

    @Resource
    private UserTitleMapper userTitleMapper;

    @Override
    public List<UserTitle> listAvailableTitles() {
        // 1. 获取当前登录用户ID
        User loginUser = userService.getLoginUser();
        //2. 查询用户已拥有的称号
        List<String> titleIds = Optional.ofNullable(JSON.parseArray(loginUser.getTitleIdList(), String.class))
                .orElse(new ArrayList<>());

        if (CollectionUtils.isEmpty(titleIds)) {
            return new ArrayList<>();
        }
        // 3. 返回称号列表
        return this.listByIds(titleIds);
    }

    @Override
    public Boolean setCurrentTitle(Long titleId) {
        User loginUser = userService.getLoginUser();

        Boolean result = checkSpecialDeal(titleId, loginUser);
        if (Boolean.TRUE.equals(result)) {
            return true;
        }

        // 1. 检查称号是否存在
        UserTitle userTitle = this.getById(titleId);
        if (userTitle == null) {
            throw new BusinessException(ErrorCode.NOT_FOUND_ERROR, "称号不存在");
        }

        // 2. 检查用户是否拥有该称号
        List<String> titleIds = JSON.parseArray(loginUser.getTitleIdList(), String.class);
        if (!titleIds.contains(titleId.toString())) {
            throw new BusinessException(ErrorCode.OPERATION_ERROR, "用户未拥有该称号");
        }
        // 3. 更新用户当前使用的称号
        loginUser.setTitleId(userTitle.getTitleId());
        userService.updateById(loginUser);
        // 4. 返回成功
        return true;
    }

    @Override
    public List<UserTitle> listUserTitlesByUserId(Long userId) {
        // 1. 获取用户信息
        User user = userService.getById(userId);
        if (user == null) {
            throw new BusinessException(ErrorCode.NOT_FOUND_ERROR, "用户不存在");
        }

        // 2. 查询用户已拥有的称号
        List<String> titleIds = Optional.ofNullable(JSON.parseArray(user.getTitleIdList(), String.class))
                .orElse(new ArrayList<>());

        if (CollectionUtils.isEmpty(titleIds)) {
            return new ArrayList<>();
        }

        // 3. 返回称号列表
        return this.listByIds(titleIds);
    }


    @NotNull
    private Boolean checkSpecialDeal(Long titleId, User loginUser) {
        if (titleId == 0) {
            // 3. 更新用户当前使用的称号
            loginUser.setTitleId(titleId);
            userService.updateById(loginUser);
            return true;
        }
        if (titleId == -1L) {
            //检查是否是管理员
            if (!UserConstant.ADMIN_ROLE.equals(loginUser.getUserRole())) {
                throw new BusinessException(ErrorCode.OPERATION_ERROR, "非管理员不能使用该称号");
            }
            // 3. 更新用户当前使用的称号
            loginUser.setTitleId(titleId);
            userService.updateById(loginUser);
            return true;
        }
        return false;
    }

    /**
     * 给用户添加称号
     *
     * @param userId 用户ID
     * @param titleId 称号ID
     * @return 是否添加成功
     */
    @Override
    public boolean addTitleToUser(Long userId, Long titleId) {
        // 1. 参数校验
        if (userId == null || userId <= 0 || titleId == null) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "参数错误");
        }

        // 2. 检查用户是否存在
        User user = userService.getById(userId);
        if (user == null) {
            throw new BusinessException(ErrorCode.NOT_FOUND_ERROR, "用户不存在");
        }

        // 3. 检查称号是否存在
        UserTitle userTitle = this.getById(titleId);
        if (userTitle == null) {
            throw new BusinessException(ErrorCode.NOT_FOUND_ERROR, "称号不存在");
        }

        // 4. 获取用户当前拥有的称号列表
        List<String> titleIds = new ArrayList<>();
        if (StringUtils.isNotBlank(user.getTitleIdList())) {
            titleIds = JSON.parseArray(user.getTitleIdList(), String.class);
        }

        // 5. 检查是否已经拥有该称号
        String titleIdStr = titleId.toString();
        if (titleIds.contains(titleIdStr)) {
            throw new BusinessException(ErrorCode.OPERATION_ERROR, "用户已拥有该称号");
        }

        // 6. 添加新称号到列表
        titleIds.add(titleIdStr);

        // 7. 更新用户称号列表
        user.setTitleIdList(JSON.toJSONString(titleIds));
        return userService.updateById(user);
    }

    /**
     * 删除用户称号
     *
     * @param userId 用户ID
     * @param titleId 称号ID
     * @return 是否删除成功
     */
    @Override
    public boolean removeTitleFromUser(Long userId, Long titleId) {
        // 1. 参数校验
        if (userId == null || userId <= 0 || titleId == null) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "参数错误");
        }

        // 2. 检查用户是否存在
        User user = userService.getById(userId);
        if (user == null) {
            throw new BusinessException(ErrorCode.NOT_FOUND_ERROR, "用户不存在");
        }

        // 3. 获取用户当前拥有的称号列表
        List<String> titleIds = new ArrayList<>();
        if (StringUtils.isNotBlank(user.getTitleIdList())) {
            titleIds = JSON.parseArray(user.getTitleIdList(), String.class);
        }

        // 4. 检查是否拥有该称号
        String titleIdStr = titleId.toString();
        if (!titleIds.contains(titleIdStr)) {
            throw new BusinessException(ErrorCode.OPERATION_ERROR, "用户未拥有该称号");
        }

        // 5. 不能删除用户当前正在使用的称号
        if (user.getTitleId() != null && user.getTitleId().equals(titleId)) {
            throw new BusinessException(ErrorCode.OPERATION_ERROR, "不能删除用户当前正在使用的称号，请先更换称号再删除");
        }

        // 6. 从列表中移除称号
        titleIds.remove(titleIdStr);

        // 7. 更新用户称号列表
        user.setTitleIdList(JSON.toJSONString(titleIds));
        return userService.updateById(user);
    }


    @Override
    public QueryWrapper<UserTitle> getQueryWrapper(UserTitleQueryRequest userTitleQueryRequest) {
        ThrowUtils.throwIf(userTitleQueryRequest == null, ErrorCode.PARAMS_ERROR, "请求参数为空");

        Long titleId = userTitleQueryRequest.getTitleId();
        String name = userTitleQueryRequest.getName();
        String sortField = userTitleQueryRequest.getSortField();
        String sortOrder = userTitleQueryRequest.getSortOrder();

        QueryWrapper<UserTitle> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq(titleId != null, "titleId", titleId);
        queryWrapper.like(StringUtils.isNotBlank(name), "name", name);
        queryWrapper.orderBy(SqlUtils.validSortField(sortField),
                sortOrder.equals(CommonConstant.SORT_ORDER_ASC), sortField);
        return queryWrapper;
    }

    @Override
    public Boolean existTitle(String name, Long titleId) {
        ThrowUtils.throwIf(name == null, ErrorCode.PARAMS_ERROR, "称号名为空");
        QueryWrapper<UserTitle> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq("name", name);
        queryWrapper.ne(titleId != null, "titleId", titleId);
        return userTitleMapper.exists(queryWrapper);
    }
}





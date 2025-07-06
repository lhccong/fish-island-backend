package com.cong.fishisland.controller.emoticon;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.cong.fishisland.annotation.NoRepeatSubmit;
import com.cong.fishisland.common.*;
import com.cong.fishisland.common.exception.BusinessException;
import com.cong.fishisland.common.exception.ThrowUtils;
import com.cong.fishisland.model.entity.emoticon.EmoticonFavour;
import com.cong.fishisland.model.entity.user.User;
import com.cong.fishisland.service.EmoticonFavourService;
import com.cong.fishisland.service.UserService;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.annotation.Resource;

/**
 * 收藏表情包接口
 * @author 许林涛
 * @date 2025年04月02日 16:08
 */
@RestController
@RequestMapping("/emoticon_favour")
@Slf4j
@Api(value = "收藏表情包接口")
public class EmoticonFavourController{
    @Resource
    private EmoticonFavourService emoticonFavourService;
    @Resource
    private UserService userService;

    /**
     * 新增收藏表情包
     * @param emoticonSrc 收藏表情包地址
     * @return 是否收藏成功
     */
    @NoRepeatSubmit
    @PostMapping("/add")
    @ApiOperation(value = "新增收藏表情包")
    public BaseResponse<Boolean> addEmoticonFavour(@RequestBody String emoticonSrc) {
        // 登录才能操作
        final User loginUser = userService.getLoginUser();
        return ResultUtils.success(emoticonFavourService.addEmoticonFavour(emoticonSrc, loginUser));
    }

    /**
     * 删除收藏表情包
     * @param deleteRequest 删除请求
     * @return 是否删除成功
     */
    @PostMapping("/delete")
    @ApiOperation(value = "删除收藏表情包")
    public BaseResponse<Boolean> deleteEmoticonFavour(@RequestBody DeleteRequest deleteRequest) {
        // 登录才能操作
        final User loginUser = userService.getLoginUser();
        long id = Long.parseLong(deleteRequest.getId());
        // 判断是否存在
        EmoticonFavour oldEmoticonFavour = emoticonFavourService.getById(id);
        ThrowUtils.throwIf(oldEmoticonFavour == null, ErrorCode.NOT_FOUND_ERROR, "收藏表情包不存在");
        // 仅本人或管理员可删除
        if (!oldEmoticonFavour.getUserId().equals(loginUser.getId()) && !userService.isAdmin()) {
            throw new BusinessException(ErrorCode.NO_AUTH_ERROR);
        }
        boolean b = emoticonFavourService.removeById(id);
        return ResultUtils.success(b);
    }

    /**
     * 根据用户id分页查询收藏表情包
     * @param request 分页请求
     * @return 表情包列表
     */
    @PostMapping("/list/page")
    @ApiOperation(value = "根据用户id分页查询收藏表情包")
    public BaseResponse<Page<EmoticonFavour>> listEmoticonFavourByPage(@RequestBody PageRequest request) {
        // 登录才能操作
        final User loginUser = userService.getLoginUser();
        Long userId = loginUser.getId();
        ThrowUtils.throwIf(userId == null, ErrorCode.NOT_LOGIN_ERROR);
        long current = request.getCurrent();
        long size = request.getPageSize();
        QueryWrapper<EmoticonFavour> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq("userId", userId);
        // 按照创建时间倒序排列
        queryWrapper.orderByDesc("createTime");
        return ResultUtils.success(emoticonFavourService.page(new Page<>(current, size), queryWrapper));
    }
}

package com.cong.fishisland.controller.tags;

import cn.dev33.satoken.annotation.SaCheckRole;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.cong.fishisland.common.BaseResponse;
import com.cong.fishisland.common.DeleteRequest;
import com.cong.fishisland.common.ErrorCode;
import com.cong.fishisland.common.ResultUtils;
import com.cong.fishisland.common.exception.BusinessException;
import com.cong.fishisland.common.exception.ThrowUtils;
import com.cong.fishisland.constant.UserConstant;
import com.cong.fishisland.model.dto.tags.TagsAddRequest;
import com.cong.fishisland.model.dto.tags.TagsQueryRequest;
import com.cong.fishisland.model.dto.tags.TagsUpdateRequest;
import com.cong.fishisland.model.entity.tags.Tags;
import com.cong.fishisland.model.entity.user.User;
import com.cong.fishisland.model.vo.tags.TagsVO;
import com.cong.fishisland.service.TagsService;
import com.cong.fishisland.service.UserService;
import io.swagger.annotations.ApiOperation;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanUtils;
import org.springframework.web.bind.annotation.*;

import javax.annotation.Resource;

/**
 * 标签接口
 *
 * @author <a href="https://github.com/lhccong">聪</a>

 */
@RestController
@RequestMapping("/tags")
@Slf4j
public class TagsController {

    @Resource
    private TagsService tagsService;

    @Resource
    private UserService userService;

    // region 增删改查

    /**
     * 创建标签（仅管理员可用）
     *
     * @param tagsAddRequest 创建标签请求
     * @return {@link BaseResponse }<{@link Long }>
     */
    @PostMapping("/add")
    @ApiOperation(value = "创建标签（仅管理员可用）")
    @SaCheckRole(UserConstant.ADMIN_ROLE)
    public BaseResponse<Long> addTags(@RequestBody TagsAddRequest tagsAddRequest) {
        ThrowUtils.throwIf(tagsAddRequest == null, ErrorCode.PARAMS_ERROR);
        // todo 在此处将实体类和 DTO 进行转换
        Tags tags = new Tags();
        BeanUtils.copyProperties(tagsAddRequest, tags);
        // 数据校验
        tagsService.validTags(tags, true);
        // 写入数据库
        boolean result = tagsService.save(tags);
        ThrowUtils.throwIf(!result, ErrorCode.OPERATION_ERROR);
        // 返回新写入的数据 id
        long newTagsId = tags.getId();
        return ResultUtils.success(newTagsId);
    }

    /**
     * 删除标签（仅管理员可用）
     *
     * @param deleteRequest 删除标签请求
     * @return {@link BaseResponse }<{@link Boolean }>
     */
    @PostMapping("/delete")
    @ApiOperation(value = "删除标签（仅管理员可用）")
    @SaCheckRole(UserConstant.ADMIN_ROLE)
    public BaseResponse<Boolean> deleteTags(@RequestBody DeleteRequest deleteRequest) {
        if (deleteRequest == null || deleteRequest.getId() <= 0) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }
        User user = userService.getLoginUser();
        long id = deleteRequest.getId();
        // 判断是否存在
        Tags oldTags = tagsService.getById(id);
        ThrowUtils.throwIf(oldTags == null, ErrorCode.NOT_FOUND_ERROR);
        // 仅本人或管理员可删除
        if (!oldTags.getId().equals(user.getId()) && !userService.isAdmin()) {
            throw new BusinessException(ErrorCode.NO_AUTH_ERROR);
        }
        // 操作数据库
        boolean result = tagsService.removeById(id);
        ThrowUtils.throwIf(!result, ErrorCode.OPERATION_ERROR);
        return ResultUtils.success(true);
    }

    /**
     * 更新标签（仅管理员可用）
     *
     * @param tagsUpdateRequest 更新标签请求
     * @return {@link BaseResponse }<{@link Boolean }>
     */
    @PostMapping("/update")
    @ApiOperation(value = "更新标签（仅管理员可用）")
    @SaCheckRole(UserConstant.ADMIN_ROLE)
    public BaseResponse<Boolean> updateTags(@RequestBody TagsUpdateRequest tagsUpdateRequest) {
        if (tagsUpdateRequest == null || tagsUpdateRequest.getId() <= 0) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }
        // todo 在此处将实体类和 DTO 进行转换
        Tags tags = new Tags();
        BeanUtils.copyProperties(tagsUpdateRequest, tags);
        // 数据校验
        tagsService.validTags(tags, false);
        // 判断是否存在
        long id = tagsUpdateRequest.getId();
        Tags oldTags = tagsService.getById(id);
        ThrowUtils.throwIf(oldTags == null, ErrorCode.NOT_FOUND_ERROR);
        // 操作数据库
        boolean result = tagsService.updateById(tags);
        ThrowUtils.throwIf(!result, ErrorCode.OPERATION_ERROR);
        return ResultUtils.success(true);
    }

    /**
     * 根据 id 获取标签（封装类）
     *
     * @param id 标签 id
     * @return {@link BaseResponse }<{@link TagsVO }>
     */
    @GetMapping("/get/vo")
    @ApiOperation(value = "根据 id 获取标签（封装类）")
    public BaseResponse<TagsVO> getTagsVOById(long id) {
        ThrowUtils.throwIf(id <= 0, ErrorCode.PARAMS_ERROR);
        // 查询数据库
        Tags tags = tagsService.getById(id);
        ThrowUtils.throwIf(tags == null, ErrorCode.NOT_FOUND_ERROR);
        // 获取封装类
        return ResultUtils.success(tagsService.getTagsVO(tags));
    }

    /**
     * 分页获取标签列表（仅管理员可用）
     *
     * @param tagsQueryRequest 分页获取标签列表请求
     * @return {@link BaseResponse }<{@link Page }<{@link Tags }>>
     */
    @PostMapping("/list/page")
    @SaCheckRole(UserConstant.ADMIN_ROLE)
    @ApiOperation(value = "分页获取标签列表（仅管理员可用）")
    public BaseResponse<Page<Tags>> listTagsByPage(@RequestBody TagsQueryRequest tagsQueryRequest) {
        long current = tagsQueryRequest.getCurrent();
        long size = tagsQueryRequest.getPageSize();
        // 查询数据库
        Page<Tags> tagsPage = tagsService.page(new Page<>(current, size),
                tagsService.getQueryWrapper(tagsQueryRequest));
        return ResultUtils.success(tagsPage);
    }

    /**
     * 分页获取标签列表（封装类）
     *
     * @param tagsQueryRequest 分页获取标签列表请求
     * @return {@link BaseResponse }<{@link Page }<{@link TagsVO }>>
     */
    @PostMapping("/list/page/vo")
    @ApiOperation(value = "分页获取标签列表（封装类）")
    public BaseResponse<Page<TagsVO>> listTagsVOByPage(@RequestBody TagsQueryRequest tagsQueryRequest) {
        long current = tagsQueryRequest.getCurrent();
        long size = tagsQueryRequest.getPageSize();
        // 限制爬虫
        ThrowUtils.throwIf(size > 20, ErrorCode.PARAMS_ERROR);
        // 查询数据库
        Page<Tags> tagsPage = tagsService.page(new Page<>(current, size),
                tagsService.getQueryWrapper(tagsQueryRequest));
        // 获取封装类
        return ResultUtils.success(tagsService.getTagsVOPage(tagsPage));
    }

    // endregion
}

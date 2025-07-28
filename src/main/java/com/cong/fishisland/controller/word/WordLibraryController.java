package com.cong.fishisland.controller.word;

import cn.dev33.satoken.annotation.SaCheckRole;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.cong.fishisland.common.BaseResponse;
import com.cong.fishisland.common.DeleteRequest;
import com.cong.fishisland.common.ErrorCode;
import com.cong.fishisland.common.ResultUtils;
import com.cong.fishisland.common.exception.BusinessException;
import com.cong.fishisland.common.exception.ThrowUtils;
import com.cong.fishisland.constant.UserConstant;
import com.cong.fishisland.model.dto.word.WordLibraryAddRequest;
import com.cong.fishisland.model.dto.word.WordLibraryQueryRequest;
import com.cong.fishisland.model.dto.word.WordLibraryUpdateRequest;
import com.cong.fishisland.model.entity.word.WordLibrary;
import com.cong.fishisland.service.WordLibraryService;
import io.swagger.annotations.ApiOperation;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.BeanUtils;
import org.springframework.web.bind.annotation.*;

import javax.annotation.Resource;

/**
 * 词库接口
 *
 * @author 许林涛
 * @date 2025年07月28日 10:30
 */
@RestController
@RequestMapping("/word/library")
@Slf4j
public class WordLibraryController {
    @Resource
    private WordLibraryService wordLibraryService;

    /**
     * 创建词库项（仅管理员）
     *
     * @param wordLibraryAddRequest 词库添加请求
     * @return 词库ID
     */
    @PostMapping("/add")
    @SaCheckRole(UserConstant.ADMIN_ROLE)
    @ApiOperation(value = "创建词库项（仅管理员）")
    public BaseResponse<Long> addWordLibrary(@RequestBody WordLibraryAddRequest wordLibraryAddRequest) {
        if (wordLibraryAddRequest == null) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }

        String word = wordLibraryAddRequest.getWord();
        String category = wordLibraryAddRequest.getCategory();
        String wordType = wordLibraryAddRequest.getWordType();

        if (StringUtils.isAnyBlank(word, category)) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "词语和分类不能为空");
        }
        ThrowUtils.throwIf(wordLibraryService.existWordLibrary(word, category, null), ErrorCode.OPERATION_ERROR, word+"已存在");

        WordLibrary wordLibrary = new WordLibrary();
        BeanUtils.copyProperties(wordLibraryAddRequest, wordLibrary);

        boolean result = wordLibraryService.save(wordLibrary);
        ThrowUtils.throwIf(!result, ErrorCode.OPERATION_ERROR);

        return ResultUtils.success(wordLibrary.getId());
    }

    /**
     * 删除词库项（仅管理员）
     *
     * @param deleteRequest 删除请求
     * @return 是否删除成功
     */
    @PostMapping("/delete")
    @SaCheckRole(UserConstant.ADMIN_ROLE)
    @ApiOperation(value = "删除词库项（仅管理员）")
    public BaseResponse<Boolean> deleteWordLibrary(@RequestBody DeleteRequest deleteRequest) {
        if (deleteRequest == null || StringUtils.isBlank(deleteRequest.getId())) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }

        boolean result = wordLibraryService.removeById(Long.parseLong(deleteRequest.getId()));
        return ResultUtils.success(result);
    }

    /**
     * 更新词库项（仅管理员）
     *
     * @param wordLibraryUpdateRequest 词库更新请求
     * @return 是否更新成功
     */
    @PostMapping("/update")
    @SaCheckRole(UserConstant.ADMIN_ROLE)
    @ApiOperation(value = "更新词库项（仅管理员）")
    public BaseResponse<Boolean> updateWordLibrary(@RequestBody WordLibraryUpdateRequest wordLibraryUpdateRequest) {
        if (wordLibraryUpdateRequest == null || wordLibraryUpdateRequest.getId() == null) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }
        String word = wordLibraryUpdateRequest.getWord();
        ThrowUtils.throwIf(wordLibraryService.existWordLibrary(word, wordLibraryUpdateRequest.getCategory(), wordLibraryUpdateRequest.getId()), ErrorCode.OPERATION_ERROR, word+"已存在");
        WordLibrary wordLibrary = new WordLibrary();
        BeanUtils.copyProperties(wordLibraryUpdateRequest, wordLibrary);

        boolean result = wordLibraryService.updateById(wordLibrary);
        ThrowUtils.throwIf(!result, ErrorCode.OPERATION_ERROR);

        return ResultUtils.success(true);
    }

    /**
     * 根据 ID 获取词库项（仅管理员）
     *
     * @param id 词库ID
     * @return 词库信息
     */
    @GetMapping("/get")
    @SaCheckRole(UserConstant.ADMIN_ROLE)
    @ApiOperation(value = "根据 ID 获取词库项（仅管理员）")
    public BaseResponse<WordLibrary> getWordLibraryById(long id) {
        if (id <= 0) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }

        WordLibrary wordLibrary = wordLibraryService.getById(id);
        ThrowUtils.throwIf(wordLibrary == null, ErrorCode.NOT_FOUND_ERROR);

        return ResultUtils.success(wordLibrary);
    }


    /**
     * 分页获取词库列表（仅管理员）
     *
     * @param wordLibraryQueryRequest 词库查询请求
     * @return 词库分页列表
     */
    @PostMapping("/list/page")
    @SaCheckRole(UserConstant.ADMIN_ROLE)
    @ApiOperation(value = "分页获取词库列表（仅管理员）")
    public BaseResponse<Page<WordLibrary>> listWordLibraryByPage(@RequestBody WordLibraryQueryRequest wordLibraryQueryRequest) {
        long current = wordLibraryQueryRequest.getCurrent();
        long size = wordLibraryQueryRequest.getPageSize();

        // 限制爬虫
        ThrowUtils.throwIf(size > 20, ErrorCode.PARAMS_ERROR, "请求页大小不能超过20");

        Page<WordLibrary> wordLibraryPage = wordLibraryService.page(
                new Page<>(current, size),
                wordLibraryService.getQueryWrapper(wordLibraryQueryRequest)
        );

        return ResultUtils.success(wordLibraryPage);
    }

}

package com.cong.fishisland.controller.datasource;

import cn.dev33.satoken.annotation.SaCheckRole;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.cong.fishisland.common.BaseResponse;
import com.cong.fishisland.common.DeleteRequest;
import com.cong.fishisland.common.ErrorCode;
import com.cong.fishisland.common.ResultUtils;
import com.cong.fishisland.common.exception.BusinessException;
import com.cong.fishisland.common.exception.ThrowUtils;
import com.cong.fishisland.constant.DataSourceCookieStatusConstant;
import com.cong.fishisland.constant.UserConstant;
import com.cong.fishisland.model.dto.datasource.DataSourceCookieAddRequest;
import com.cong.fishisland.model.dto.datasource.DataSourceCookieQueryRequest;
import com.cong.fishisland.model.dto.datasource.DataSourceCookieUpdateRequest;
import com.cong.fishisland.model.entity.datasource.DataSourceCookie;
import com.cong.fishisland.model.enums.HotDataKeyEnum;
import com.cong.fishisland.model.vo.datasource.DataSourceCookieVO;
import com.cong.fishisland.model.vo.datasource.DataSourceKeyOptionVO;
import com.cong.fishisland.service.datasource.DataSourceCookieService;
import io.swagger.annotations.ApiOperation;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.BeanUtils;
import org.springframework.web.bind.annotation.*;

import javax.annotation.Resource;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 数据源 Cookie 管理
 */
@RestController
@RequestMapping("/datasource/cookie")
@Slf4j
public class DataSourceCookieController {

    @Resource
    private DataSourceCookieService dataSourceCookieService;

    @PostMapping("/add")
    @SaCheckRole(UserConstant.ADMIN_ROLE)
    @ApiOperation("新增数据源 Cookie（管理员）")
    public BaseResponse<Long> add(@RequestBody DataSourceCookieAddRequest request) {
        validateAddRequest(request);
        HotDataKeyEnum.getEnumByValue(request.getDataSourceKey());
        ThrowUtils.throwIf(dataSourceCookieService.existsByDataSourceKey(request.getDataSourceKey(), null),
                ErrorCode.OPERATION_ERROR, "该数据源已存在 Cookie 配置");

        DataSourceCookie record = new DataSourceCookie();
        BeanUtils.copyProperties(request, record);
        if (record.getStatus() == null) {
            record.setStatus(DataSourceCookieStatusConstant.ENABLED);
        }
        boolean saved = dataSourceCookieService.save(record);
        ThrowUtils.throwIf(!saved, ErrorCode.OPERATION_ERROR);
        dataSourceCookieService.evictCache(record.getDataSourceKey());
        return ResultUtils.success(record.getId());
    }

    @PostMapping("/update")
    @SaCheckRole(UserConstant.ADMIN_ROLE)
    @ApiOperation("更新数据源 Cookie（管理员）")
    public BaseResponse<Boolean> update(@RequestBody DataSourceCookieUpdateRequest request) {
        if (request == null || request.getId() == null) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }
        DataSourceCookie old = dataSourceCookieService.getById(request.getId());
        ThrowUtils.throwIf(old == null, ErrorCode.NOT_FOUND_ERROR);

        if (StringUtils.isNotBlank(request.getDataSourceKey())) {
            HotDataKeyEnum.getEnumByValue(request.getDataSourceKey());
            ThrowUtils.throwIf(dataSourceCookieService.existsByDataSourceKey(request.getDataSourceKey(), request.getId()),
                    ErrorCode.OPERATION_ERROR, "该数据源已存在 Cookie 配置");
        }

        DataSourceCookie record = new DataSourceCookie();
        BeanUtils.copyProperties(request, record);
        boolean updated = dataSourceCookieService.updateById(record);
        ThrowUtils.throwIf(!updated, ErrorCode.OPERATION_ERROR);

        dataSourceCookieService.evictCache(old.getDataSourceKey());
        if (StringUtils.isNotBlank(request.getDataSourceKey())
                && !request.getDataSourceKey().equals(old.getDataSourceKey())) {
            dataSourceCookieService.evictCache(request.getDataSourceKey());
        }
        return ResultUtils.success(true);
    }

    @PostMapping("/delete")
    @SaCheckRole(UserConstant.ADMIN_ROLE)
    @ApiOperation("删除数据源 Cookie（管理员）")
    public BaseResponse<Boolean> delete(@RequestBody DeleteRequest deleteRequest) {
        if (deleteRequest == null || StringUtils.isBlank(deleteRequest.getId())) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }
        long id = Long.parseLong(deleteRequest.getId());
        DataSourceCookie old = dataSourceCookieService.getById(id);
        ThrowUtils.throwIf(old == null, ErrorCode.NOT_FOUND_ERROR);
        boolean removed = dataSourceCookieService.removeById(id);
        dataSourceCookieService.evictCache(old.getDataSourceKey());
        return ResultUtils.success(removed);
    }

    @GetMapping("/get")
    @SaCheckRole(UserConstant.ADMIN_ROLE)
    @ApiOperation("根据 ID 获取数据源 Cookie（管理员）")
    public BaseResponse<DataSourceCookieVO> getById(long id) {
        if (id <= 0) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }
        return ResultUtils.success(dataSourceCookieService.getVoById(id));
    }

    @PostMapping("/list/page")
    @SaCheckRole(UserConstant.ADMIN_ROLE)
    @ApiOperation("分页查询数据源 Cookie（管理员）")
    public BaseResponse<Page<DataSourceCookieVO>> listPage(@RequestBody DataSourceCookieQueryRequest request) {
        if (request == null) {
            request = new DataSourceCookieQueryRequest();
        }
        ThrowUtils.throwIf(request.getPageSize() > 20, ErrorCode.PARAMS_ERROR, "请求页大小不能超过20");
        return ResultUtils.success(dataSourceCookieService.listPage(request));
    }

    @GetMapping("/keys")
    @SaCheckRole(UserConstant.ADMIN_ROLE)
    @ApiOperation("获取可选数据源 Key 列表（管理员）")
    public BaseResponse<List<DataSourceKeyOptionVO>> listKeys() {
        return ResultUtils.success(dataSourceCookieService.listDataSourceKeyOptions());
    }

    @GetMapping("/resolve")
    @SaCheckRole(UserConstant.ADMIN_ROLE)
    @ApiOperation("解析生效 Cookie（数据库优先，配置兜底，管理员）")
    public BaseResponse<Map<String, String>> resolve(String dataSourceKey) {
        if (StringUtils.isBlank(dataSourceKey)) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "数据源标识不能为空");
        }
        HotDataKeyEnum.getEnumByValue(dataSourceKey);
        String cookie = dataSourceCookieService.getEnabledCookie(dataSourceKey);
        Map<String, String> result = new HashMap<>(2);
        result.put("dataSourceKey", dataSourceKey);
        result.put("cookie", cookie);
        return ResultUtils.success(result);
    }

    private void validateAddRequest(DataSourceCookieAddRequest request) {
        if (request == null) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }
        if (StringUtils.isAnyBlank(request.getDataSourceKey(), request.getCookieValue())) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "数据源标识与 Cookie 不能为空");
        }
    }
}

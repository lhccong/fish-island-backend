package com.cong.fishisland.service.datasource.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.cong.fishisland.common.ErrorCode;
import com.cong.fishisland.common.exception.ThrowUtils;
import com.cong.fishisland.constant.CommonConstant;
import com.cong.fishisland.constant.DataSourceCookieStatusConstant;
import com.cong.fishisland.constant.RedisKey;
import com.cong.fishisland.mapper.datasource.DataSourceCookieMapper;
import com.cong.fishisland.model.dto.datasource.DataSourceCookieQueryRequest;
import com.cong.fishisland.model.entity.datasource.DataSourceCookie;
import com.cong.fishisland.model.enums.HotDataKeyEnum;
import com.cong.fishisland.model.vo.datasource.DataSourceCookieVO;
import com.cong.fishisland.model.vo.datasource.DataSourceKeyOptionVO;
import com.cong.fishisland.service.datasource.DataSourceCookieService;
import com.cong.fishisland.utils.SqlUtils;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

/**
 * 数据源 Cookie 服务实现
 * @author cong
 */
@Service
public class DataSourceCookieServiceImpl extends ServiceImpl<DataSourceCookieMapper, DataSourceCookie>
        implements DataSourceCookieService {

    private static final long CACHE_HOURS = 1;

    @Resource
    private StringRedisTemplate stringRedisTemplate;

    @Value("${fishisland.datasource.nga.cookie:}")
    private String ngaCookieFallback;

    @Value("${fishisland.datasource.zhihu.cookie:}")
    private String zhiHuCookieFallback;

    @Value("${fishisland.koishi.token:}")
    private String koishiTokenFallback;

    @Override
    public String getEnabledCookie(String dataSourceKey) {
        if (StringUtils.isBlank(dataSourceKey)) {
            return "";
        }
        String cacheKey = RedisKey.getKey(RedisKey.DATASOURCE_COOKIE_CACHE_KEY, dataSourceKey);
        String cached = stringRedisTemplate.opsForValue().get(cacheKey);
        if (cached != null) {
            return cached;
        }

        String cookie = loadEnabledCookieFromDb(dataSourceKey);
        if (StringUtils.isBlank(cookie)
                && HotDataKeyEnum.NGA_QING_FENG.getValue().equals(dataSourceKey)) {
            cookie = loadEnabledCookieFromDb(HotDataKeyEnum.NGA.getValue());
        }
        if (StringUtils.isBlank(cookie)) {
            cookie = loadFallbackFromConfig(dataSourceKey);
        }
        if (cookie == null) {
            cookie = "";
        }

        stringRedisTemplate.opsForValue().set(cacheKey, cookie, CACHE_HOURS, TimeUnit.HOURS);
        return cookie;
    }

    private String loadEnabledCookieFromDb(String dataSourceKey) {
        DataSourceCookie record = this.getOne(new LambdaQueryWrapper<DataSourceCookie>()
                .eq(DataSourceCookie::getDataSourceKey, dataSourceKey)
                .eq(DataSourceCookie::getStatus, DataSourceCookieStatusConstant.ENABLED)
                .orderByDesc(DataSourceCookie::getUpdateTime)
                .last("LIMIT 1"));
        if (record == null || StringUtils.isBlank(record.getCookieValue())) {
            return "";
        }
        return record.getCookieValue();
    }

    private String loadFallbackFromConfig(String dataSourceKey) {
        if (HotDataKeyEnum.NGA.getValue().equals(dataSourceKey)
                || HotDataKeyEnum.NGA_QING_FENG.getValue().equals(dataSourceKey)) {
            return ngaCookieFallback;
        }
        if (HotDataKeyEnum.ZHI_HU.getValue().equals(dataSourceKey)) {
            return zhiHuCookieFallback;
        }
        if (HotDataKeyEnum.KOISHI.getValue().equals(dataSourceKey)) {
            return koishiTokenFallback;
        }
        return "";
    }

    @Override
    public boolean existsByDataSourceKey(String dataSourceKey, Long excludeId) {
        ThrowUtils.throwIf(StringUtils.isBlank(dataSourceKey), ErrorCode.PARAMS_ERROR, "数据源标识不能为空");
        return baseMapper.exists(new LambdaQueryWrapper<DataSourceCookie>()
                .eq(DataSourceCookie::getDataSourceKey, dataSourceKey)
                .ne(excludeId != null, DataSourceCookie::getId, excludeId));
    }

    @Override
    public LambdaQueryWrapper<DataSourceCookie> getLambdaQueryWrapper(DataSourceCookieQueryRequest request) {
        ThrowUtils.throwIf(request == null, ErrorCode.PARAMS_ERROR, "请求参数为空");
        Long id = request.getId();
        String dataSourceKey = request.getDataSourceKey();
        Integer status = request.getStatus();
        String sortField = request.getSortField();
        String sortOrder = request.getSortOrder();
        boolean asc = CommonConstant.SORT_ORDER_ASC.equals(sortOrder);

        LambdaQueryWrapper<DataSourceCookie> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(id != null, DataSourceCookie::getId, id);
        queryWrapper.eq(StringUtils.isNotBlank(dataSourceKey), DataSourceCookie::getDataSourceKey, dataSourceKey);
        queryWrapper.eq(status != null, DataSourceCookie::getStatus, status);

        if (SqlUtils.validSortField(sortField)) {
            switch (sortField) {
                case "id":
                    queryWrapper.orderBy(true, asc, DataSourceCookie::getId);
                    break;
                case "dataSourceKey":
                    queryWrapper.orderBy(true, asc, DataSourceCookie::getDataSourceKey);
                    break;
                case "status":
                    queryWrapper.orderBy(true, asc, DataSourceCookie::getStatus);
                    break;
                case "createTime":
                    queryWrapper.orderBy(true, asc, DataSourceCookie::getCreateTime);
                    break;
                case "updateTime":
                    queryWrapper.orderBy(true, asc, DataSourceCookie::getUpdateTime);
                    break;
                default:
                    queryWrapper.orderByDesc(DataSourceCookie::getUpdateTime);
                    break;
            }
        } else {
            queryWrapper.orderByDesc(DataSourceCookie::getUpdateTime);
        }
        return queryWrapper;
    }

    @Override
    public Page<DataSourceCookieVO> listPage(DataSourceCookieQueryRequest request) {
        long current = request.getCurrent();
        long size = request.getPageSize();
        Page<DataSourceCookie> page = this.page(new Page<>(current, size), getLambdaQueryWrapper(request));
        Page<DataSourceCookieVO> voPage = new Page<>(page.getCurrent(), page.getSize(), page.getTotal());
        voPage.setRecords(page.getRecords().stream().map(this::toVo).collect(Collectors.toList()));
        return voPage;
    }

    @Override
    public DataSourceCookieVO getVoById(long id) {
        DataSourceCookie record = this.getById(id);
        ThrowUtils.throwIf(record == null, ErrorCode.NOT_FOUND_ERROR);
        return toVo(record);
    }

    @Override
    public void evictCache(String dataSourceKey) {
        if (StringUtils.isNotBlank(dataSourceKey)) {
            stringRedisTemplate.delete(RedisKey.getKey(RedisKey.DATASOURCE_COOKIE_CACHE_KEY, dataSourceKey));
        }
    }

    @Override
    public List<DataSourceKeyOptionVO> listDataSourceKeyOptions() {
        return Arrays.stream(HotDataKeyEnum.values())
                .map(item -> new DataSourceKeyOptionVO(item.getValue(), item.getText()))
                .collect(Collectors.toList());
    }

    private DataSourceCookieVO toVo(DataSourceCookie record) {
        DataSourceCookieVO vo = new DataSourceCookieVO();
        BeanUtils.copyProperties(record, vo);
        try {
            vo.setDataSourceName(HotDataKeyEnum.getEnumByValue(record.getDataSourceKey()).getText());
        } catch (Exception ignored) {
            vo.setDataSourceName(record.getDataSourceKey());
        }
        return vo;
    }
}

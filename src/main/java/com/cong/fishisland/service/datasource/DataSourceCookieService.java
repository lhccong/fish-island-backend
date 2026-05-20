package com.cong.fishisland.service.datasource;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.IService;
import com.cong.fishisland.model.dto.datasource.DataSourceCookieQueryRequest;
import com.cong.fishisland.model.entity.datasource.DataSourceCookie;
import com.cong.fishisland.model.vo.datasource.DataSourceCookieVO;
import com.cong.fishisland.model.vo.datasource.DataSourceKeyOptionVO;

import java.util.List;

/**
 * 数据源 Cookie 服务
 */
public interface DataSourceCookieService extends IService<DataSourceCookie> {

    /**
     * 获取启用的 Cookie（数据库优先，其次配置文件兜底）
     *
     * @param dataSourceKey 数据源标识
     * @return Cookie，可能为空字符串
     */
    String getEnabledCookie(String dataSourceKey);

    /**
     * 是否存在相同数据源 Key 的配置
     */
    boolean existsByDataSourceKey(String dataSourceKey, Long excludeId);

    LambdaQueryWrapper<DataSourceCookie> getLambdaQueryWrapper(DataSourceCookieQueryRequest request);

    Page<DataSourceCookieVO> listPage(DataSourceCookieQueryRequest request);

    DataSourceCookieVO getVoById(long id);

    void evictCache(String dataSourceKey);

    List<DataSourceKeyOptionVO> listDataSourceKeyOptions();
}

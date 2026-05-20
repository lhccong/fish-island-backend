package com.cong.fishisland.datasource.hostpost;

import com.alibaba.fastjson.JSON;
import com.cong.fishisland.model.entity.hot.HotPost;
import com.cong.fishisland.model.enums.CategoryTypeEnum;
import com.cong.fishisland.model.enums.HotDataKeyEnum;
import com.cong.fishisland.model.enums.UpdateIntervalEnum;
import com.cong.fishisland.model.vo.hot.HotPostDataVO;
import com.cong.fishisland.service.datasource.DataSourceCookieService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * NGA 晴风村（fid=-7955747）帖子列表，逻辑与杂谈共用 {@link NgaForumFetcher}。
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class NgaQingFengDataSource implements DataSource {

    /** 晴风村版面 fid */
    private static final int FID_QING_FENG = -7955747;
    private static final int TOP_N = 20;

    private final NgaForumFetcher ngaForumFetcher;
    private final DataSourceCookieService dataSourceCookieService;

    @Override
    public HotPost getHotPost() {
        String ngaCookie = dataSourceCookieService.getEnabledCookie(HotDataKeyEnum.NGA_QING_FENG.getValue());
        List<HotPostDataVO> sortedTop = ngaForumFetcher.fetchTopByReplies(FID_QING_FENG, ngaCookie, TOP_N);
        if (sortedTop.isEmpty()) {
            log.warn("NGA 晴风村解析结果为空");
            return HotPost.builder().build();
        }

        return HotPost.builder()
                .sort(CategoryTypeEnum.GENERAL_DISCUSSION.getValue())
                .name("NGA晴风")
                .category(CategoryTypeEnum.GENERAL_DISCUSSION.getValue())
                .updateInterval(UpdateIntervalEnum.HALF_HOUR.getValue())
                .iconUrl("https://bbs.nga.cn/favicon.ico")
                .hostJson(JSON.toJSONString(sortedTop))
                .typeName("NGA晴风")
                .build();
    }
}

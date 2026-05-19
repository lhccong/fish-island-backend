package com.cong.fishisland.datasource.hostpost;

import com.alibaba.fastjson.JSON;
import com.cong.fishisland.model.entity.hot.HotPost;
import com.cong.fishisland.model.enums.CategoryTypeEnum;
import com.cong.fishisland.model.enums.UpdateIntervalEnum;
import com.cong.fishisland.model.vo.hot.HotPostDataVO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * NGA 网事杂谈（fid=-7）帖子列表，与晴风村等版面共用 {@link NgaForumFetcher}。
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class NgaDataSource implements DataSource {

    /** 网事杂谈版面 fid */
    private static final int FID_ZA_TAN = -7;
    private static final int TOP_N = 20;

    private final NgaForumFetcher ngaForumFetcher;

    @Value("${fishisland.datasource.nga.cookie:}")
    private String ngaCookie;

    @Override
    public HotPost getHotPost() {
        List<HotPostDataVO> sortedTop = ngaForumFetcher.fetchTopByReplies(FID_ZA_TAN, ngaCookie, TOP_N);
        if (sortedTop.isEmpty()) {
            log.error("无法获取或解析 NGA 网事杂谈版面列表");
            return HotPost.builder().build();
        }

        return HotPost.builder()
                .sort(CategoryTypeEnum.GENERAL_DISCUSSION.getValue())
                .name("NGA杂谈")
                .category(CategoryTypeEnum.GENERAL_DISCUSSION.getValue())
                .updateInterval(UpdateIntervalEnum.HALF_HOUR.getValue())
                .iconUrl("https://bbs.nga.cn/favicon.ico")
                .hostJson(JSON.toJSONString(sortedTop))
                .typeName("NGA杂谈")
                .build();
    }
}

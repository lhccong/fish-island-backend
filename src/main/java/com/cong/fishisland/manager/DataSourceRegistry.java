package com.cong.fishisland.manager;

import com.cong.fishisland.datasource.hostpost.*;
import com.cong.fishisland.model.enums.HotDataKeyEnum;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;
import java.util.HashMap;
import java.util.Map;

/**
 * 数据源注册表
 *
 * @author 86188
 * @date 2023/03/20
 */
@Component
@RequiredArgsConstructor
public class DataSourceRegistry {


    private Map<String, DataSource> typeDataSourceMap;

    private final ZhiHuDataSource zhiHuDataSource;
    private final WeiBoDataSource weiBoDataSource;
    private final CodeFatherDataSource codeFatherDataSource;
    private final BiliBiliDataSource bilibiliDataSource;
    private final HuPuStreetDataSource huPuStreetDataSource;
    private final DouYinDataSource douYinDataSource;
    private final WYCloudDataSource wyCloudDataSource;
    private final CsdnDataSource csdnDataSource;
    private final JueJinDataSource jueJinDataSource;
    private final SmzdmDataSource smzdmDataSource;
    private final ZhiBo8DataSource zhiBo8DataSource;
    private final TieBaDataSource tieBaDataSource;
    private final QQMusicDataSource qqMusicDataSource;


    @PostConstruct
    public void doInit() {
        typeDataSourceMap = new HashMap<String, DataSource>() {{
            put(HotDataKeyEnum.ZHI_HU.getValue(), zhiHuDataSource);
            put(HotDataKeyEnum.WEI_BO.getValue(), weiBoDataSource);
            put(HotDataKeyEnum.CODE_FATHER.getValue(), codeFatherDataSource);
            put(HotDataKeyEnum.BILI_BILI.getValue(), bilibiliDataSource);
            put(HotDataKeyEnum.HU_PU_STREET.getValue(), huPuStreetDataSource);
            put(HotDataKeyEnum.WY_CLOUD_MUSIC.getValue(), wyCloudDataSource);
            put(HotDataKeyEnum.DOU_YIN.getValue(), douYinDataSource);
            put(HotDataKeyEnum.CS_DN.getValue(), csdnDataSource);
            put(HotDataKeyEnum.JUE_JIN.getValue(), jueJinDataSource);
            put(HotDataKeyEnum.SM_ZDM.getValue(), smzdmDataSource);
            put(HotDataKeyEnum.ZHI_BO_8.getValue(), zhiBo8DataSource);
            put(HotDataKeyEnum.TIE_BA.getValue(), tieBaDataSource);
            put(HotDataKeyEnum.QQ_MUSIC.getValue(), qqMusicDataSource);
        }};
    }


    public DataSource getDataSourceByType(String type) {
        if (typeDataSourceMap == null) {
            return null;
        }
        return typeDataSourceMap.get(type);
    }
}
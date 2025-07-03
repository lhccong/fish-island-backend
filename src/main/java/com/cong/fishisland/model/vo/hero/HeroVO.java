package com.cong.fishisland.model.vo.hero;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Data;

import java.io.Serializable;
import java.util.Date;

/**
 * 英雄VO
 * @author 许林涛
 * @date 2025年05月03日 15:52
 */
@Data
public class HeroVO implements Serializable {
    /**
     * 主键ID
     */
    private String id;

    /**
     * 英雄英文标识(如177)
     */
    private String ename;

    /**
     * 中文名(如苍)
     */
    private String cname;

    /**
     * 称号(如苍狼末裔)
     */
    private String title;

    /**
     * 上线时间
     */
    @JsonFormat(pattern = "yyyy-MM-dd", timezone = "GMT+8")
    private Date releaseDate;

    /**
     * 新英雄标识(0常规/1新英雄)
     */
    private Integer newType;

    /**
     * 主定位(1战士/2法师/3坦克/4刺客/5射手/6辅助)
     */
    private Integer primaryType;

    /**
     * 副定位(1战士/2法师/3坦克/4刺客/5射手/6辅助)
     */
    private Integer secondaryType;

    /**
     * 皮肤列表(用|分隔，如苍狼末裔|维京掠夺者|苍林狼骑)
     */
    private String skins;

    /**
     * 官网详情页链接
     */
    private String officialLink;

    /**
     * 内部ID
     */
    private Long mossId;

    /**
     * 种族[yxzz_b8]
     */
    private String race;

    /**
     * 势力[yxsl_54]
     */
    private String faction;

    /**
     * 身份[yxsf_48]
     */
    private String identity;

    /**
     * 区域[qym_e7]
     */
    private String region;

    /**
     * 能量[nl_96]
     */
    private String ability;

    /**
     * 身高[sg_30]
     */
    private String height;

    /**
     * 经典台词[rsy_49]
     */
    private String quote;

    /**
     * 皮肤数量
     */
    private Integer skinsNum;

    private static final long serialVersionUID = 1L;
}

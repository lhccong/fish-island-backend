package com.cong.fishisland.model.vo.user;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.math.BigDecimal;
import java.util.Date;

/**
 * 用户年度报告数据快照
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UserAnnualReportVO implements Serializable {

    /**
     * 年份
     */
    private Integer year;

    /**
     * 用户基础信息
     */
    private BaseInfo baseInfo;

    /**
     * 帖子/内容表现
     */
    private ContentStats contentStats;

    /**
     * 收藏/表情包数据
     */
    private CollectionStats collectionStats;

    /**
     * 宠物相关数据
     */
    private PetStats petStats;

    /**
     * 赞助/打赏数据
     */
    private DonationStats donationStats;

    /**
     * 成长数据（等级 / 积分）
     */
    private GrowthStats growthStats;

    private static final long serialVersionUID = 1L;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class BaseInfo implements Serializable {
        private Long userId;
        private String userName;
        private String userAvatar;
        private Date registerTime;
        /**
         * 注册天数（统计到指定年度结束或当前时间）
         */
        private Long registerDays;
        /**
         * 本年度活跃天数（有互动/发帖/养宠/赞助）
         */
        private Integer activeDays;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ContentStats implements Serializable {
        /**
         * 累计发帖数
         */
        private Long totalPosts;
        /**
         * 当年发帖数
         */
        private Long postsThisYear;
        /**
         * 当年帖子浏览
         */
        private Long postViewsThisYear;
        /**
         * 当年帖子收藏
         */
        private Long postFavoursThisYear;
        /**
         * 累计获赞（基于帖子 thumbNum 汇总）
         */
        private Long totalPostThumbs;
        /**
         * 当年获赞
         */
        private Long postThumbsThisYear;
        /**
         * 当年表现最佳的帖子
         */
        private PostBrief topPost;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class PostBrief implements Serializable {
        private Long postId;
        private String title;
        private Integer thumbNum;
        private Integer viewNum;
        private Date createTime;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class CollectionStats implements Serializable {
        /**
         * 累计收藏表情包数量
         */
        private Long emoticonFavourTotal;
        /**
         * 当年收藏表情包数量
         */
        private Long emoticonFavourThisYear;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class PetStats implements Serializable {
        private Long petTotal;
        private String topPetName;
        private Integer topPetLevel;
        private Date firstPetCreateTime;
        /**
         * 最高等级宠物当前使用的皮肤/形象地址
         */
        private String topPetSkinUrl;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class DonationStats implements Serializable {
        /**
         * 赞助总额（目前表结构为累计金额）
         */
        private BigDecimal donationTotal;
        private Date lastDonateTime;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class GrowthStats implements Serializable {
        private Integer level;
        private Integer points;
        private Integer usedPoints;
        private Date lastSignInDate;
    }
}


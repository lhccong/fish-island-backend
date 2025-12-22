package com.cong.fishisland.service.annual;

import com.cong.fishisland.model.vo.user.UserAnnualReportVO;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Optional;

/**
 * 用户年度报告 AI 文案服务
 * <p>
 * 说明：
 * 当前版本仅生成基于统计数据的兜底文案，预留 AI 接入能力。
 * 后续可在此处接入大模型，根据 {@link UserAnnualReportVO} 自动生成更个性化的年度总结。
 */
@Service
public class AnnualReportAiService {

    /**
     * 生成用户年度总结文案
     * TODO 后续接入 AI 大模型，根据 reportData 生成更高级的文案内容
     *
     * @param reportData 年度报告数据
     * @return 年度总结文案
     */
    public String generateSummary(UserAnnualReportVO reportData) {
        if (reportData == null) {
            return "这一年你与摸鱼岛一起度过了许多时光，期待来年继续一起摸鱼与创作。";
        }

        UserAnnualReportVO.BaseInfo baseInfo = reportData.getBaseInfo();
        UserAnnualReportVO.ContentStats contentStats = reportData.getContentStats();
        UserAnnualReportVO.DonationStats donationStats = reportData.getDonationStats();

        String userName = baseInfo == null ? "用户" : Optional.ofNullable(baseInfo.getUserName()).orElse("用户");
        long postCount = contentStats == null ? 0L : Optional.ofNullable(contentStats.getPostsThisYear()).orElse(0L);
        long thumbs = contentStats == null ? 0L : Optional.ofNullable(contentStats.getPostThumbsThisYear()).orElse(0L);
        long favours = contentStats == null ? 0L : Optional.ofNullable(contentStats.getPostFavoursThisYear()).orElse(0L);
        int activeDays = baseInfo == null || baseInfo.getActiveDays() == null ? 0 : baseInfo.getActiveDays();
        BigDecimal donationAmount = donationStats == null || donationStats.getDonationTotal() == null
                ? BigDecimal.ZERO
                : donationStats.getDonationTotal();
        String donationText = donationAmount.setScale(2, RoundingMode.HALF_UP).toPlainString();

        return String.format("%s在%d年发布了%d篇帖子，收获%d次点赞和%d次收藏，活跃%d天，感谢你贡献的%s元赞助，期待来年继续摸鱼与创作！",
                userName,
                reportData.getYear(),
                postCount,
                thumbs,
                favours,
                activeDays,
                donationText);
    }
}









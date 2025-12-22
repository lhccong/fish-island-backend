package com.cong.fishisland.service.annual;

import com.cong.fishisland.constant.RedisKey;
import com.cong.fishisland.datasource.ai.AIChatDataSource;
import com.cong.fishisland.model.vo.ai.AiResponse;
import com.cong.fishisland.model.vo.ai.SiliconFlowRequest;
import com.cong.fishisland.model.vo.user.UserAnnualReportVO;
import com.cong.fishisland.utils.RedisUtils;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Duration;
import java.util.Optional;

/**
 * 用户年度报告 AI 文案服务
 * <p>
 * 说明：
 * 当前版本仅生成基于统计数据的兜底文案，预留 AI 接入能力。
 * 后续可在此处接入大模型，根据 {@link UserAnnualReportVO} 自动生成更个性化的年度总结。
 */
@Service
@RequiredArgsConstructor
public class AnnualReportAiService {

    @Qualifier("siliconFlowDataSource")
    private final AIChatDataSource siliconFlowDataSource;

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

        // 赞助金额为 0 时，不展示“感谢你贡献的 X 元赞助”文案
        String donationPart = donationAmount.compareTo(BigDecimal.ZERO) > 0
                ? String.format("感谢你贡献的%s米米赞助，", donationText)
                : "";

        return String.format("%s在%d年发布了%d篇帖子，收获%d次点赞和%d次收藏，%s期待来年继续摸鱼与创作！与你在摸鱼岛一起分享生活的点滴感悟，一起成长🔥。",
                userName,
                reportData.getYear(),
                postCount,
                thumbs,
                favours,
                donationPart);
    }

    /**
     * 生成内容发布统计的 AI 总结文案
     * 用于模板中内容发布统计卡片的展示
     *
     * @param reportData 年度报告数据
     * @param postCount  发帖数量
     * @param totalWords 总字数
     * @return AI 生成的内容发布总结文案
     */
    public String generateContentSummary(UserAnnualReportVO reportData, long postCount, long totalWords) {
        if (reportData == null) {
            return "这一年，您在摸鱼岛留下了珍贵的足迹。";
        }

        UserAnnualReportVO.BaseInfo baseInfo = reportData.getBaseInfo();
        Long userId = baseInfo != null ? baseInfo.getUserId() : null;
        int year = reportData.getYear() != null ? reportData.getYear() : java.util.Calendar.getInstance().get(java.util.Calendar.YEAR);

        // 如果有 userId，尝试从缓存中获取
        if (userId != null) {
            String cacheKey = RedisKey.getKey(RedisKey.ANNUAL_REPORT_CONTENT_SUMMARY, userId, year);
            String cachedResult = RedisUtils.get(cacheKey);
            if (cachedResult != null && !cachedResult.isEmpty()) {
                return cachedResult;
            }
        }

        String userName = baseInfo == null ? "您" : Optional.ofNullable(baseInfo.getUserName()).orElse("您");

        // 构建提示词
        StringBuilder content = new StringBuilder();
        content.append(String.format("用户%s在%d年发布了%d篇内容", userName, year, postCount));
        if (totalWords > 0) {
            if (totalWords >= 10000) {
                content.append(String.format("，累计%.1f万字", totalWords / 10000.0));
            } else {
                content.append(String.format("，累计%d个字", totalWords));
            }
        }
        ;
        content.append(generateSummary(reportData));
        content.append("。请用一段温馨、简洁的中文总结这段创作经历，要求：");
        content.append("1. 语言自然流畅，不低于100字也不要高于 120 字，可以搭配emoji；");
        content.append("2. 可以包含数字，但要自然融入文案中；");
        content.append("3. 语气要温馨、鼓励，符合摸鱼岛的社区氛围；");
        content.append("4. 直接返回文案内容，不要加引号或其他格式。");

        try {
            // 对齐帖子摘要的调用方式，使用消息列表形式调用 SiliconFlow
            SiliconFlowRequest.Message systemMessage = new SiliconFlowRequest.Message();
            systemMessage.setRole("system");
            systemMessage.setContent("你是一名友好的年度创作总结助手，用温暖的语气为用户生成简短中文文案。");

            SiliconFlowRequest.Message userMessage = new SiliconFlowRequest.Message();
            userMessage.setRole("user");
            userMessage.setContent(content.toString());

            AiResponse aiResponse = siliconFlowDataSource.getAiResponse(
                    java.util.Arrays.asList(systemMessage, userMessage),
                    "Qwen/Qwen2.5-14B-Instruct"
            );
            String aiResult = aiResponse.getAnswer();
            // 清理可能的多余格式和空白
            if (aiResult != null) {
                aiResult = aiResult.trim()
                        .replaceAll("^[\"']+|[\"']+$", "") // 移除首尾引号
                        .replaceAll("\\s+", " ") // 多个空格合并为一个
                        .trim();
            }
            // 如果 AI 返回为空或异常，返回兜底文案
            if (aiResult == null || aiResult.isEmpty()) {
                return generateFallbackContentSummary(postCount, totalWords);
            }
            
            // 将 AI 生成的结果存入 Redis，缓存时间为 3 个月（约90天）
            if (userId != null) {
                String cacheKey = RedisKey.getKey(RedisKey.ANNUAL_REPORT_CONTENT_SUMMARY, userId, year);
                RedisUtils.set(cacheKey, aiResult, Duration.ofDays(90));
            }
            
            return aiResult;
        } catch (Exception e) {
            // AI 调用失败时返回兜底文案
            return generateFallbackContentSummary(postCount, totalWords);
        }
    }

    /**
     * 生成兜底的内容发布统计文案（当 AI 调用失败时使用）
     */
    private String generateFallbackContentSummary(long postCount, long totalWords) {
        StringBuilder result = new StringBuilder();
        result.append("这一年，您共发布了 ").append(postCount).append(" 篇内容");
        if (totalWords > 0) {
            if (totalWords >= 10000) {
                result.append("，累计 ").append(String.format("%.1f", totalWords / 10000.0)).append(" 万字");
            } else {
                result.append("，累计 ").append(totalWords).append(" 个字");
            }
        }
        result.append("。");
        return result.toString();
    }
}









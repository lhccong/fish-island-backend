package com.cong.fishisland.service.annual;

import cn.hutool.core.date.DateUtil;
import cn.hutool.core.text.CharSequenceUtil;
import com.cong.fishisland.model.vo.user.UserAnnualReportVO;
import freemarker.template.Configuration;
import freemarker.template.Template;
import freemarker.template.TemplateException;
import org.springframework.stereotype.Service;
import org.springframework.web.util.HtmlUtils;

import javax.annotation.Resource;
import java.io.IOException;
import java.io.StringWriter;
import java.math.RoundingMode;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

/**
 * 用户年度报告模板渲染服务
 * 使用 FreeMarker 模板引擎渲染年度报告 HTML 内容
 */
@Service
public class AnnualReportTemplateService {

    @Resource
    private Configuration freeMarkerConfiguration;

    @Resource
    private AnnualReportAiService annualReportAiService;

    /**
     * 渲染年度报告 HTML 内容
     *
     * @param reportData 年度报告数据
     * @param summary    年度总结文案
     * @return HTML 字符串
     */
    public String render(UserAnnualReportVO reportData, String summary) {
        if (reportData == null) {
            return "";
        }

        UserAnnualReportVO.BaseInfo baseInfo = reportData.getBaseInfo();
        UserAnnualReportVO.ContentStats contentStats = reportData.getContentStats();
        UserAnnualReportVO.CollectionStats collectionStats = reportData.getCollectionStats();
        UserAnnualReportVO.PetStats petStats = reportData.getPetStats();
        UserAnnualReportVO.DonationStats donationStats = reportData.getDonationStats();

        // 衍生展示字段计算
        String displayName = HtmlUtils.htmlEscape(CharSequenceUtil.blankToDefault(
                baseInfo == null ? "用户" : baseInfo.getUserName(),
                "用户"));
        // 用户头像，为空时传递空字符串，模板中会显示首字母占位
        String avatar = baseInfo != null && CharSequenceUtil.isNotBlank(baseInfo.getUserAvatar())
                ?  baseInfo.getUserAvatar()
                : "";
        // 处理总结文案，移除换行符，直接作为纯文本
        String summaryText = CharSequenceUtil.blankToDefault(summary, "这一年你与摸鱼岛一起探索，感谢陪伴。")
                .replace("\r\n", " ").replace("\n", " ").replace("\r", " ").trim();

        long postCount = Optional.ofNullable(contentStats == null ? null : contentStats.getPostsThisYear()).orElse(0L);
        long postViews = Optional.ofNullable(contentStats == null ? null : contentStats.getPostViewsThisYear()).orElse(0L);
        long postThumbs = Optional.ofNullable(contentStats == null ? null : contentStats.getPostThumbsThisYear()).orElse(0L);
        long postFavours = Optional.ofNullable(contentStats == null ? null : contentStats.getPostFavoursThisYear()).orElse(0L);
        long emoticonFavours = Optional.ofNullable(collectionStats == null ? null : collectionStats.getEmoticonFavourThisYear()).orElse(0L);
        long petCount = Optional.ofNullable(petStats == null ? null : petStats.getPetTotal()).orElse(0L);
        String topPetName = petStats == null ? null : petStats.getTopPetName();
        Integer topPetLevel = petStats == null ? null : petStats.getTopPetLevel();
        String petSkinUrl = petStats != null && CharSequenceUtil.isNotBlank(petStats.getTopPetSkinUrl())
                ? petStats.getTopPetSkinUrl()
                : "https://oss.cqbo.com/moyu/moyu.png";

        UserAnnualReportVO.PostBrief topPost = contentStats == null ? null : contentStats.getTopPost();
        String bestPostTitle = topPost == null ? null
                : HtmlUtils.htmlEscape(CharSequenceUtil.blankToDefault(topPost.getTitle(), null));
        int bestPostThumb = topPost == null ? 0 : Optional.ofNullable(topPost.getThumbNum()).orElse(0);
        String bestPostDate = topPost == null || topPost.getCreateTime() == null ? null
                : DateUtil.format(topPost.getCreateTime(), "M 月 d 日");

        String donationText = donationStats == null || donationStats.getDonationTotal() == null
                ? "0.00"
                : donationStats.getDonationTotal().setScale(2, RoundingMode.HALF_UP).toPlainString();

        // 时间区间与陪伴天数
        String period = "";
        long accompanyDays = 0;
        int activeDays = 0;
        if (reportData.getYear() != null) {
            String startStr = reportData.getYear() + "-01-01 00:00:00";
            String endStr = reportData.getYear() + "-12-31 23:59:59";
            period = DateUtil.formatDate(DateUtil.parse(startStr)) + " - " + DateUtil.formatDate(DateUtil.parse(endStr));
        }
        if (baseInfo != null) {
            accompanyDays = Optional.ofNullable(baseInfo.getRegisterDays()).orElse(0L);
            activeDays = Optional.ofNullable(baseInfo.getActiveDays()).orElse(0);
        }

        // 计算年度关键字（根据数据特征）
        String annualKeyword = calculateAnnualKeyword(postCount, postThumbs, postFavours, activeDays, accompanyDays);
        
        // 计算总字数（从当年帖子内容统计）
        // 注意：由于性能考虑，这里不查询完整内容，如果需要精确统计，可以在 DataAssembler 中计算
        long totalWords = 0L; // 暂时设为0，如果需要显示，需要在 DataAssembler 中查询 content 字段并统计

        // 生成内容发布统计的 AI 总结
        String contentSummary = annualReportAiService.generateContentSummary(reportData, postCount, totalWords);

        // 构造模板数据模型
        Map<String, Object> model = new HashMap<>();
        model.put("year", reportData.getYear());
        model.put("displayName", displayName);
        model.put("avatar", avatar);
        model.put("summaryText", summaryText);
        model.put("annualKeyword", annualKeyword);

        model.put("postCount", postCount);
        model.put("postViews", postViews);
        model.put("postThumbs", postThumbs);
        model.put("postFavours", postFavours);
        model.put("emoticonFavours", emoticonFavours);
        model.put("petCount", petCount);
        model.put("topPetName", topPetName);
        model.put("topPetLevel", topPetLevel);
        model.put("petSkinUrl", petSkinUrl);
        model.put("donationText", donationText);

        model.put("bestPostTitle", bestPostTitle);
        model.put("bestPostThumb", bestPostThumb);
        model.put("bestPostDate", bestPostDate);

        model.put("period", period);
        model.put("accompanyDays", accompanyDays);
        model.put("activeDays", activeDays);
        model.put("registerTime", baseInfo == null ? null : baseInfo.getRegisterTime());
        
        model.put("totalWords", totalWords);
        // 教程数量（暂时设为0，后续如果有教程数据可以统计）
        model.put("tutorialCount", 0);
        // AI 生成的内容发布统计总结
        model.put("contentSummary", contentSummary);

        try {
            Template template = freeMarkerConfiguration.getTemplate("UserAnnualReportTemplate.ftl", "UTF-8");
            StringWriter writer = new StringWriter();
            template.process(model, writer);
            String html = writer.toString();
            // 清理多余的换行符和空白字符
            html = html.replaceAll("(\r\n|\r|\n)+", " ")  // 将所有换行符替换为单个空格
                    .replaceAll("\\s+", " ")  // 将多个连续空白字符替换为单个空格
                    .replaceAll(">\\s+<", "><")  // 移除标签之间的空白
                    .trim();
            return html;
        } catch (IOException | TemplateException e) {
            // 渲染失败时返回空字符串，避免影响主流程
            return "";
        }
    }

    /**
     * 根据用户数据计算年度关键字
     *
     * @param postCount  发帖数
     * @param postThumbs   获赞数
     * @param postFavours  收藏数
     * @param activeDays   活跃天数
     * @param accompanyDays 注册天数
     * @return 年度关键字
     */
    private String calculateAnnualKeyword(long postCount, long postThumbs, long postFavours, int activeDays, long accompanyDays) {
        // 根据数据特征判断用户类型
        if (accompanyDays > 180) {
            return "摸鱼元老💕";
        } else if (postCount >= 5) {
            return "摸鱼内容创作者摸🐟";
        } else if (postThumbs >= 10) {
            return "摸鱼帖子点赞达人❤";
        } else if (postFavours >= 15) {
            return "摸鱼收藏达人⭐";
        } else if (activeDays >= 20) {
            return "活跃用户🔥";
        } else {
            return "摸鱼新手";
        }
    }
}





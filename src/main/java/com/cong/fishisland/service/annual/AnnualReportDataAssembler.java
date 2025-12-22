package com.cong.fishisland.service.annual;

import cn.hutool.core.date.DateUnit;
import cn.hutool.core.date.DateUtil;
import cn.hutool.core.text.CharSequenceUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.cong.fishisland.mapper.donation.DonationRecordsMapper;
import com.cong.fishisland.mapper.emoticon.EmoticonFavourMapper;
import com.cong.fishisland.mapper.pet.FishPetMapper;
import com.cong.fishisland.mapper.post.PostMapper;
import com.cong.fishisland.model.entity.donation.DonationRecords;
import com.cong.fishisland.model.entity.emoticon.EmoticonFavour;
import com.cong.fishisland.model.entity.pet.FishPet;
import com.cong.fishisland.model.entity.post.Post;
import com.cong.fishisland.model.entity.user.User;
import com.cong.fishisland.model.entity.user.UserPoints;
import com.cong.fishisland.model.enums.DeleteStatusEnum;
import com.cong.fishisland.model.vo.user.UserAnnualReportVO;
import com.cong.fishisland.service.UserPointsService;
import org.springframework.stereotype.Component;

import javax.annotation.Resource;
import java.math.BigDecimal;
import java.util.*;

/**
 * 用户年度报告数据聚合组件
 * 负责从数据库中聚合用户年度相关的统计数据，构建 {@link UserAnnualReportVO}
 */
@Component
public class AnnualReportDataAssembler {

    @Resource
    private PostMapper postMapper;

    @Resource
    private EmoticonFavourMapper emoticonFavourMapper;

    @Resource
    private FishPetMapper fishPetMapper;

    @Resource
    private DonationRecordsMapper donationRecordsMapper;

    @Resource
    private UserPointsService userPointsService;

    /**
     * 构建用户年度报告视图对象
     *
     * @param user 用户实体
     * @param year 年度
     * @return 用户年度报告视图对象
     */
    public UserAnnualReportVO assemble(User user, Integer year) {
        Date yearStart = DateUtil.beginOfYear(DateUtil.parse(year + "-01-01 00:00:00"));
        Date yearEnd = DateUtil.endOfYear(yearStart);
        Long userId = user.getId();

        // 帖子相关数据
        List<Post> postsThisYear = postMapper.selectList(Wrappers.<Post>lambdaQuery()
                .eq(Post::getUserId, userId)
                .eq(Post::getIsDelete, DeleteStatusEnum.NOT_DELETED.getValue())
                .between(Post::getCreateTime, yearStart, yearEnd));
        long postViewsThisYear = postsThisYear.stream().map(Post::getViewNum).filter(Objects::nonNull).mapToLong(Integer::longValue).sum();
        long postThumbsThisYear = postsThisYear.stream().map(Post::getThumbNum).filter(Objects::nonNull).mapToLong(Integer::longValue).sum();
        long postFavoursThisYear = postsThisYear.stream().map(Post::getFavourNum).filter(Objects::nonNull).mapToLong(Integer::longValue).sum();
        Post bestPost = postsThisYear.stream()
                .max(Comparator.comparingInt(post -> Optional.ofNullable(post.getThumbNum()).orElse(0)))
                .orElse(null);
        
        // 计算总字数（统计当年帖子内容长度）
        long totalWords = postsThisYear.stream()
                .map(Post::getContent)
                .filter(Objects::nonNull)
                .mapToLong(content -> content.length())
                .sum();

        long totalPosts = postMapper.selectCount(Wrappers.<Post>lambdaQuery()
                .eq(Post::getUserId, userId)
                .eq(Post::getIsDelete, DeleteStatusEnum.NOT_DELETED.getValue()));
        List<Post> allPostLight = postMapper.selectList(Wrappers.<Post>lambdaQuery()
                .select(Post::getId, Post::getThumbNum, Post::getViewNum)
                .eq(Post::getUserId, userId)
                .eq(Post::getIsDelete, DeleteStatusEnum.NOT_DELETED.getValue()));
        long totalPostThumbs = allPostLight.stream().map(Post::getThumbNum).filter(Objects::nonNull).mapToLong(Integer::longValue).sum();

        // 表情包相关数据
        List<EmoticonFavour> emoticonYearList = emoticonFavourMapper.selectList(Wrappers.<EmoticonFavour>lambdaQuery()
                .eq(EmoticonFavour::getUserId, userId)
                .between(EmoticonFavour::getCreateTime, yearStart, yearEnd));
        long emoticonFavourThisYear = emoticonYearList.size();
        long emoticonFavourTotal = emoticonFavourMapper.selectCount(Wrappers.<EmoticonFavour>lambdaQuery()
                .eq(EmoticonFavour::getUserId, userId));

        // 宠物相关数据
        List<FishPet> petYearList = fishPetMapper.selectList(Wrappers.<FishPet>lambdaQuery()
                .eq(FishPet::getUserId, userId)
                .eq(FishPet::getIsDelete, DeleteStatusEnum.NOT_DELETED.getValue())
                .between(FishPet::getCreateTime, yearStart, yearEnd));
        List<FishPet> petAllList = fishPetMapper.selectList(Wrappers.<FishPet>lambdaQuery()
                .eq(FishPet::getUserId, userId)
                .eq(FishPet::getIsDelete, DeleteStatusEnum.NOT_DELETED.getValue()));
        long petTotal = petAllList.size();
        FishPet topPet = petAllList.stream()
                .max(Comparator.comparingInt(pet -> Optional.ofNullable(pet.getLevel()).orElse(0)))
                .orElse(null);
        Date firstPetCreateTime = petAllList.stream()
                .map(FishPet::getCreateTime)
                .filter(Objects::nonNull)
                .min(Date::compareTo)
                .orElse(null);

        // 赞助相关数据
        List<DonationRecords> donationYearList = donationRecordsMapper.selectList(Wrappers.<DonationRecords>lambdaQuery()
                .eq(DonationRecords::getUserId, userId)
                .eq(DonationRecords::getIsDelete, DeleteStatusEnum.NOT_DELETED.getValue())
                .between(DonationRecords::getCreateTime, yearStart, yearEnd));
        BigDecimal donationAmount = donationYearList.stream()
                .map(record -> record.getAmount() == null ? BigDecimal.ZERO : record.getAmount())
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        DonationRecords lastDonation = donationRecordsMapper.selectOne(Wrappers.<DonationRecords>lambdaQuery()
                .eq(DonationRecords::getUserId, userId)
                .eq(DonationRecords::getIsDelete, DeleteStatusEnum.NOT_DELETED.getValue())
                .orderByDesc(DonationRecords::getUpdateTime)
                .last("limit 1"));

        // 成长 / 活跃相关数据
        UserPoints userPoints = userPointsService.getOne(new LambdaQueryWrapper<UserPoints>().eq(UserPoints::getUserId, userId));
        int activeDays = calculateActiveDays(postsThisYear, emoticonYearList, petYearList, donationYearList);

        Date endForRegister = new Date();
        if (endForRegister.after(yearEnd)) {
            endForRegister = yearEnd;
        }
        long registerDays = DateUtil.between(user.getCreateTime(), endForRegister, DateUnit.DAY, true) + 1;

        UserAnnualReportVO.PostBrief topPostBrief = null;
        if (bestPost != null) {
            topPostBrief = UserAnnualReportVO.PostBrief.builder()
                    .postId(bestPost.getId())
                    .title(bestPost.getTitle())
                    .thumbNum(bestPost.getThumbNum())
                    .viewNum(bestPost.getViewNum())
                    .createTime(bestPost.getCreateTime())
                    .build();
        }

        // 将总字数存储到 ContentStats 中（通过扩展字段或直接使用现有字段）
        // 注意：由于 UserAnnualReportVO 结构限制，我们将在 TemplateService 中计算总字数
        
        return UserAnnualReportVO.builder()
                .year(year)
                .baseInfo(UserAnnualReportVO.BaseInfo.builder()
                        .userId(userId)
                        .userName(CharSequenceUtil.blankToDefault(user.getUserName(), user.getUserAccount()))
                        .userAvatar(user.getUserAvatar())
                        .registerTime(user.getCreateTime())
                        .registerDays(registerDays)
                        .activeDays(activeDays)
                        .build())
                .contentStats(UserAnnualReportVO.ContentStats.builder()
                        .totalPosts(totalPosts)
                        .postsThisYear(postsThisYear.size() * 1L)
                        .postViewsThisYear(postViewsThisYear)
                        .postFavoursThisYear(postFavoursThisYear)
                        .totalPostThumbs(totalPostThumbs)
                        .postThumbsThisYear(postThumbsThisYear)
                        .topPost(topPostBrief)
                        .build())
                .collectionStats(UserAnnualReportVO.CollectionStats.builder()
                        .emoticonFavourTotal(emoticonFavourTotal)
                        .emoticonFavourThisYear(emoticonFavourThisYear)
                        .build())
                .petStats(UserAnnualReportVO.PetStats.builder()
                        .petTotal(petTotal)
                        .topPetName(topPet == null ? null : topPet.getName())
                        .topPetLevel(topPet == null ? null : topPet.getLevel())
                        .firstPetCreateTime(firstPetCreateTime)
                        .build())
                .donationStats(UserAnnualReportVO.DonationStats.builder()
                        .donationTotal(donationAmount)
                        .lastDonateTime(lastDonation == null ? null : lastDonation.getUpdateTime())
                        .build())
                .growthStats(UserAnnualReportVO.GrowthStats.builder()
                        .level(userPoints == null ? null : userPoints.getLevel())
                        .points(userPoints == null ? null : userPoints.getPoints())
                        .usedPoints(userPoints == null ? null : userPoints.getUsedPoints())
                        .lastSignInDate(userPoints == null ? null : userPoints.getLastSignInDate())
                        .build())
                .build();
    }

    /**
     * 计算用户在年度内的活跃天数
     *
     * @param postList      帖子列表
     * @param favourList    表情包收藏列表
     * @param petList       宠物列表
     * @param donationList  赞助记录列表
     * @return 活跃天数
     */
    private int calculateActiveDays(List<Post> postList,
                                   List<EmoticonFavour> favourList,
                                   List<FishPet> petList,
                                   List<DonationRecords> donationList) {
        Set<String> daySet = new HashSet<>();
        postList.forEach(post -> addActiveDay(daySet, post.getCreateTime()));
        favourList.forEach(favour -> addActiveDay(daySet, favour.getCreateTime()));
        petList.forEach(pet -> addActiveDay(daySet, pet.getCreateTime()));
        donationList.forEach(record -> addActiveDay(daySet, record.getCreateTime()));
        return daySet.size();
    }

    /**
     * 将指定日期归档为某一天的活跃记录
     *
     * @param daySet 活跃日期集合
     * @param date   日期
     */
    private void addActiveDay(Set<String> daySet, Date date) {
        if (date != null) {
            daySet.add(DateUtil.formatDate(date));
        }
    }
}
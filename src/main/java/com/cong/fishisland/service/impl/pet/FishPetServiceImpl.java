package com.cong.fishisland.service.impl.pet;

import cn.dev33.satoken.stp.StpUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.cong.fishisland.common.ErrorCode;
import com.cong.fishisland.common.exception.BusinessException;
import com.cong.fishisland.constant.PetRedisKey;
import com.cong.fishisland.constant.TitleConstant;
import com.cong.fishisland.mapper.pet.FishPetMapper;
import com.cong.fishisland.model.dto.pet.CreatePetRequest;
import com.cong.fishisland.model.dto.pet.UpdatePetNameRequest;
import com.cong.fishisland.model.entity.pet.FishPet;
import com.cong.fishisland.model.vo.pet.OtherUserPetVO;
import com.cong.fishisland.model.vo.pet.PetRankVO;
import com.cong.fishisland.model.vo.pet.PetSkinVO;
import com.cong.fishisland.model.vo.pet.PetVO;
import com.cong.fishisland.model.entity.user.User;
import com.cong.fishisland.service.FishPetService;
import com.cong.fishisland.service.PetSkinService;
import com.cong.fishisland.service.UserPointsService;
import com.cong.fishisland.service.UserTitleService;
import com.cong.fishisland.service.UserService;
import com.cong.fishisland.service.event.EventRemindHandler;
import com.cong.fishisland.utils.RedisUtils;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import toolgood.words.StringSearch;

import java.time.Duration;
import java.util.*;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;

/**
 * 宠物服务实现类
 *
 * @author cong
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class FishPetServiceImpl extends ServiceImpl<FishPetMapper, FishPet> implements FishPetService {

    private final StringSearch wordsUtil;
    private final UserPointsService userPointsService;
    private final PetSkinService petSkinService;
    private final UserTitleService userTitleService;
    private final UserService userService;
    private final EventRemindHandler eventRemindHandler;


    // 每次喂食增加的饥饿度
    private static final int FEED_HUNGER_INCREASE = 20;
    // 每次喂食增加的心情值
    private static final int FEED_MOOD_INCREASE = 5;
    // 每次抚摸增加的心情值
    private static final int PAT_MOOD_INCREASE = 15;

    // 喂食和抚摸消耗的积分
    private static final int FEED_POINT_COST = 5;
    private static final int PAT_POINT_COST = 3;
    // 修改宠物名字消耗的积分
    private static final int RENAME_POINT_COST = 100;

    // 宠物排行榜缓存时间（24小时）
    private static final Duration PET_RANK_CACHE_DURATION = Duration.ofHours(24);
    // 默认排行榜数量
    private static final int DEFAULT_RANK_LIMIT = 10;

    @Override
    public Long createPet(CreatePetRequest createPetRequest) {

        Long userId = StpUtil.getLoginIdAsLong();

        if (createPetRequest == null) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }

        // 校验名称
        String name = createPetRequest.getName();
        if (name == null || name.isEmpty()) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "宠物名称不能为空");
        }

        if (wordsUtil.Replace(name).contains("*")) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "宠物名称不能包含，敏感词");
        }

        // 检查用户是否已经有宠物
        QueryWrapper<FishPet> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq("userId", userId);
        long count = this.count(queryWrapper);
        if (count > 0) {
            throw new BusinessException(ErrorCode.OPERATION_ERROR, "您已经拥有宠物了");
        }

        // 创建宠物
        FishPet fishPet = new FishPet();
        BeanUtils.copyProperties(createPetRequest, fishPet);
        fishPet.setUserId(userId);
        fishPet.setLevel(1);
        fishPet.setExp(0);
        fishPet.setMood(100);
        fishPet.setHunger(0);

        // 如果没有提供宠物图片，设置默认图片
        if (fishPet.getPetUrl() == null || fishPet.getPetUrl().isEmpty()) {
            fishPet.setPetUrl("https://api.oss.cqbo.com/moyu/pet/超级玛丽马里奥 (73)_爱给网_aigei_com.png");
        }

        boolean result = this.save(fishPet);
        if (!result) {
            throw new BusinessException(ErrorCode.OPERATION_ERROR);
        }

        return fishPet.getPetId();
    }

    @Override
    public PetVO getPetDetail() {

        if (!StpUtil.isLogin()) {
            return null;
        }

        Long userId = StpUtil.getLoginIdAsLong();


        // 查询宠物
        FishPet fishPet;

        // 按用户ID查询
        QueryWrapper<FishPet> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq("userId", userId);
        fishPet = this.getOne(queryWrapper);

        if (fishPet == null) {
            return null;
        }

        // 转换为VO
        PetVO petVO = new PetVO();
        BeanUtils.copyProperties(fishPet, petVO);

        // 获取宠物拥有的皮肤列表
        List<PetSkinVO> petSkins = this.getPetSkins(fishPet.getPetId());
        petVO.setSkins(petSkins);

        return petVO;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean updatePetName(UpdatePetNameRequest updatePetNameRequest, Long userId) {
        if (updatePetNameRequest == null || updatePetNameRequest.getPetId() == null) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }

        // 校验名称
        String name = updatePetNameRequest.getName();
        if (name == null || name.isEmpty()) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "宠物名称不能为空");
        }

        if (wordsUtil.Replace(name).contains("*")) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "宠物名称不能包含，敏感词");
        }

        // 查询宠物是否存在且属于该用户
        Long petId = updatePetNameRequest.getPetId();
        FishPet fishPet = this.getById(petId);

        if (fishPet == null) {
            throw new BusinessException(ErrorCode.NOT_FOUND_ERROR, "宠物不存在");
        }

        // 检查宠物是否属于该用户
        if (!Objects.equals(fishPet.getUserId(), userId)) {
            throw new BusinessException(ErrorCode.NO_AUTH_ERROR, "无权修改该宠物");
        }

        // 扣除用户积分
        userPointsService.deductPoints(userId, RENAME_POINT_COST);

        // 修改名称
        fishPet.setName(name);

        return this.updateById(fishPet);
    }

    @Override
    public OtherUserPetVO getOtherUserPet(Long otherUserId) {
        if (otherUserId == null) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }

        // 查询其他用户的宠物
        QueryWrapper<FishPet> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq("userId", otherUserId);
        FishPet fishPet = this.getOne(queryWrapper);

        if (fishPet == null) {
            throw new BusinessException(ErrorCode.NOT_FOUND_ERROR, "该用户没有宠物");
        }

        // 转换为其他用户宠物VO（不包含扩展数据）
        OtherUserPetVO otherUserPetVO = new OtherUserPetVO();
        BeanUtils.copyProperties(fishPet, otherUserPetVO);

        // 获取宠物拥有的皮肤列表
        List<PetSkinVO> petSkins = this.getPetSkins(fishPet.getPetId());
        otherUserPetVO.setSkins(petSkins);

        return otherUserPetVO;
    }

    @Override
    public PetVO feedPet(Long petId) {
        Long userId = StpUtil.getLoginIdAsLong();

        // 检查宠物是否存在且属于当前用户
        FishPet fishPet = checkPetOwnership(petId, userId);

        // 检查饥饿度是否已满
        if (fishPet.getHunger() >= 100) {
            throw new BusinessException(ErrorCode.OPERATION_ERROR, "宠物已经吃饱了，不需要再喂食");
        }

        // 扣除用户积分
        userPointsService.deductPoints(userId, FEED_POINT_COST);

        // 更新宠物饥饿度和心情值
        int newHunger = Math.min(100, fishPet.getHunger() + FEED_HUNGER_INCREASE);
        int newMood = Math.min(100, fishPet.getMood() + FEED_MOOD_INCREASE);


        fishPet.setHunger(newHunger);
        fishPet.setMood(newMood);

        boolean result = this.updateById(fishPet);
        if (!result) {
            throw new BusinessException(ErrorCode.OPERATION_ERROR, "喂食失败");
        }

        List<String> userIds = new ArrayList<>();
        userIds.add(userId.toString());
        batchUpdateOnlineUserPetExp(userIds);

        // 返回更新后的宠物信息
        PetVO petVO = new PetVO();
        BeanUtils.copyProperties(fishPet, petVO);
        petVO.setExp(petVO.getExp() + 1);

        return petVO;
    }

    @Override
    public PetVO patPet(Long petId) {
        Long userId = StpUtil.getLoginIdAsLong();

        // 检查宠物是否存在且属于当前用户
        FishPet fishPet = checkPetOwnership(petId, userId);

        // 检查心情值是否已满
        if (fishPet.getMood() >= 100) {
            throw new BusinessException(ErrorCode.OPERATION_ERROR, "宠物心情已经很好了，不需要再抚摸");
        }

        // 扣除用户积分
        userPointsService.deductPoints(userId, PAT_POINT_COST);

        // 更新宠物心情值
        int newMood = Math.min(100, fishPet.getMood() + PAT_MOOD_INCREASE);
        fishPet.setMood(newMood);


        boolean result = this.updateById(fishPet);

        List<String> userIds = new ArrayList<>();
        userIds.add(userId.toString());
        batchUpdateOnlineUserPetExp(userIds);

        if (!result) {
            throw new BusinessException(ErrorCode.OPERATION_ERROR, "抚摸失败");
        }


        // 返回更新后的宠物信息
        PetVO petVO = new PetVO();
        BeanUtils.copyProperties(fishPet, petVO);
        petVO.setExp(petVO.getExp() + 1);


        return petVO;
    }

    @Override
    public int batchUpdatePetStatus(int hungerDecrement, int moodDecrement) {
        return baseMapper.batchUpdatePetStatus(hungerDecrement, moodDecrement);
    }

    @Override
    public int batchUpdateOnlineUserPetExp(List<String> userIds) {
        if (userIds == null || userIds.isEmpty()) {
            return 0;
        }

        // 注意：在SQL实现中，只有当宠物的饥饿度(hunger)或心情值(mood)任意一个大于0时，
        // 宠物才会获得经验并可能升级。这确保了宠物得到基本照顾就能成长。
        return baseMapper.batchUpdateOnlineUserPetExp(userIds);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public int generateDailyPetPoints(int maxPoints) {
        // 获取所有符合条件的宠物（饥饿度或心情值任意一个大于0）
        List<Map<String, Object>> eligiblePets = baseMapper.getPetsForDailyPoints();

        if (eligiblePets == null || eligiblePets.isEmpty()) {
            log.info("没有符合条件的宠物产出积分");
            return 0;
        }

        int count = 0;

        // 为每个符合条件的宠物产出积分
        for (Map<String, Object> pet : eligiblePets) {
            try {
                Long userId = ((Number) pet.get("userId")).longValue();
                int level = ((Number) pet.get("level")).intValue();

                // 产出积分 = 宠物等级（最高不超过maxPoints）
                int pointsToAdd = Math.min(level, maxPoints);

                // 为用户增加积分（非签到积分）
                userPointsService.updateUsedPoints(userId, -pointsToAdd);

                log.info("宠物产出积分：用户ID={}, 宠物等级={}, 产出积分={}", userId, level, pointsToAdd);
                count++;
            } catch (Exception e) {
                log.error("宠物产出积分异常", e);
                // 继续处理下一个宠物，不影响整体流程
            }
        }

        return count;
    }

    @Override
    public List<PetSkinVO> getPetSkins(Long petId) {
        if (petId == null) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }

        // 查询宠物
        FishPet fishPet = this.getById(petId);
        if (fishPet == null) {
            throw new BusinessException(ErrorCode.NOT_FOUND_ERROR, "宠物不存在");
        }

        // 从扩展数据中获取皮肤ID列表
        List<Long> skinIds = new ArrayList<>();
        if (fishPet.getExtendData() != null && !fishPet.getExtendData().isEmpty()) {
            JSONObject extendData = JSON.parseObject(fishPet.getExtendData());
            if (extendData.containsKey("skinIds")) {
                skinIds = JSON.parseArray(extendData.getString("skinIds"), Long.class);
            }
        }

        // 如果没有皮肤，返回空列表
        if (skinIds.isEmpty()) {
            return new ArrayList<>();
        }

        // 查询皮肤信息
        return petSkinService.getPetSkinsByIds(skinIds);
    }

    @Override
    public int generatePetRankList() {
        log.info("开始生成宠物排行榜");

        try {
            // 从数据库获取排行榜数据
            List<PetRankVO> petRankList = baseMapper.getPetRankList(DEFAULT_RANK_LIMIT);

            if (petRankList == null || petRankList.isEmpty()) {
                log.info("没有宠物数据，不生成排行榜");
                return 0;
            }

            // 设置排名
            for (int i = 0; i < petRankList.size(); i++) {
                petRankList.get(i).setRank(i + 1);
            }

            // 将排行榜数据缓存到Redis
            String petRankKey = PetRedisKey.getKey(PetRedisKey.PET_RANK);

            // 先删除旧的排行榜数据
            RedisUtils.delete(petRankKey);

            // 将新的排行榜数据存入Redis
            String petRankJson = JSON.toJSONString(petRankList);
            RedisUtils.set(petRankKey, petRankJson, PET_RANK_CACHE_DURATION);

            log.info("宠物排行榜生成成功，共{}条数据", petRankList.size());
            return petRankList.size();
        } catch (Exception e) {
            log.error("生成宠物排行榜异常", e);
            return 0;
        }
    }

    @Override
    public List<PetRankVO> getPetRankList(int limit) {
        // 限制获取数量
        if (limit <= 0) {
            limit = 10;
        }
        limit = Math.min(limit, DEFAULT_RANK_LIMIT);

        // 从Redis获取排行榜数据
        String petRankKey = PetRedisKey.getKey(PetRedisKey.PET_RANK);
        String petRankJson = RedisUtils.get(petRankKey);

        List<PetRankVO> petRankList;

        if (petRankJson != null && !petRankJson.isEmpty()) {
            // 如果Redis中有数据，直接返回
            petRankList = JSON.parseArray(petRankJson, PetRankVO.class);

            // 如果需要的数量小于缓存的数量，截取前limit个
            if (petRankList.size() > limit) {
                petRankList = petRankList.subList(0, limit);
            }
        } else {
            // 如果Redis中没有数据，从数据库获取并生成排行榜
            petRankList = baseMapper.getPetRankList(limit);

            // 设置排名
            for (int i = 0; i < petRankList.size(); i++) {
                petRankList.get(i).setRank(i + 1);
            }

            // 将排行榜数据缓存到Redis
            if (!petRankList.isEmpty()) {
                String newPetRankJson = JSON.toJSONString(petRankList);
                RedisUtils.set(petRankKey, newPetRankJson, PET_RANK_CACHE_DURATION);
            }
        }

        return petRankList;
    }

    /**
     * 检查宠物是否存在且属于指定用户
     *
     * @param petId  宠物ID
     * @param userId 用户ID
     * @return 宠物实体
     */
    private FishPet checkPetOwnership(Long petId, Long userId) {
        if (petId == null) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }

        FishPet fishPet = this.getById(petId);
        if (fishPet == null) {
            throw new BusinessException(ErrorCode.NOT_FOUND_ERROR, "宠物不存在");
        }

        if (!Objects.equals(fishPet.getUserId(), userId)) {
            throw new BusinessException(ErrorCode.NO_AUTH_ERROR, "无权操作该宠物");
        }

        return fishPet;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public int updatePetRankTitles() {
        log.info("开始执行宠物排行榜称号更新任务");

        try {
            // 1. 获取所有拥有宠物称号的用户ID列表（包括前三名额外称号）
            Set<Long> usersWithPetTitle = new HashSet<>();
            List<User> allUsers = userService.list(new LambdaQueryWrapper<User>().like(User::getTitleIdList, TitleConstant.PET_RANK_TITLE_ID.toString()));
            for (User user : allUsers) {
                if (user.getTitleIdList() != null && !user.getTitleIdList().isEmpty()) {
                    List<String> titleIds = JSON.parseArray(user.getTitleIdList(), String.class);
                    if (titleIds.contains(TitleConstant.PET_RANK_TITLE_ID.toString()) ||
                        titleIds.contains(TitleConstant.PET_RANK_FIRST_TITLE_ID.toString()) ||
                        titleIds.contains(TitleConstant.PET_RANK_SECOND_TITLE_ID.toString()) ||
                        titleIds.contains(TitleConstant.PET_RANK_THIRD_TITLE_ID.toString())) {
                        usersWithPetTitle.add(user.getId());
                    }
                }
            }

            // 2. 获取今天排行榜的用户ID列表
            List<PetRankVO> todayRankList = baseMapper.getPetRankList(DEFAULT_RANK_LIMIT);
            Set<Long> todayUserIds = new HashSet<>();
            if (todayRankList != null && !todayRankList.isEmpty()) {
                for (PetRankVO rankVO : todayRankList) {
                    todayUserIds.add(rankVO.getUserId());
                }
            }

            int updatedCount = 0;

            // 3. 移除所有拥有宠物称号但不在今天排行榜中的用户
            for (Long userId : usersWithPetTitle) {
                if (!todayUserIds.contains(userId)) {
                    try {
                        // 检查用户当前是否正在使用宠物称号
                        User user = userService.getById(userId);
                        if (user != null && (TitleConstant.PET_RANK_TITLE_ID.equals(user.getTitleId()) ||
                            TitleConstant.PET_RANK_FIRST_TITLE_ID.equals(user.getTitleId()) ||
                            TitleConstant.PET_RANK_SECOND_TITLE_ID.equals(user.getTitleId()) ||
                            TitleConstant.PET_RANK_THIRD_TITLE_ID.equals(user.getTitleId()))) {
                            // 如果用户当前正在使用宠物称号，将其设置为默认称号
                            user.setTitleId(TitleConstant.DEFAULT_TITLE_ID);
                            userService.updateById(user);
                            log.info("用户{}当前正在使用宠物称号，已设置为默认称号", userId);
                        }

                        // 从用户的称号列表中移除所有宠物相关称号
                        boolean removed = false;
                        removed |= userTitleService.removeTitleFromUser(userId, TitleConstant.PET_RANK_TITLE_ID);
                        removed |= userTitleService.removeTitleFromUser(userId, TitleConstant.PET_RANK_FIRST_TITLE_ID);
                        removed |= userTitleService.removeTitleFromUser(userId, TitleConstant.PET_RANK_SECOND_TITLE_ID);
                        removed |= userTitleService.removeTitleFromUser(userId, TitleConstant.PET_RANK_THIRD_TITLE_ID);
                        
                        if (removed) {
                            updatedCount++;
                            log.info("成功移除用户{}的宠物称号", userId);
                        }
                    } catch (Exception e) {
                        log.error("移除用户{}的宠物称号时发生异常", userId, e);
                    }
                }
            }

            // 4. 给今天排行榜用户添加宠物称号
            for (Long userId : todayUserIds) {
                try {
                    // 检查用户是否已经拥有宠物称号
                    User user = userService.getById(userId);
                    if (user != null) {
                        List<String> titleIds = new ArrayList<>();
                        if (user.getTitleIdList() != null && !user.getTitleIdList().isEmpty()) {
                            titleIds = JSON.parseArray(user.getTitleIdList(), String.class);
                        }

                        // 如果用户还没有宠物称号，则添加
                        if (!titleIds.contains(TitleConstant.PET_RANK_TITLE_ID.toString())) {
                            boolean added = userTitleService.addTitleToUser(userId, TitleConstant.PET_RANK_TITLE_ID);
                            if (added) {
                                updatedCount++;
                                log.info("成功给用户{}添加宠物称号", userId);
                                eventRemindHandler.handleSystemMessage(userId, "恭喜获得宠物排行榜称号！");
                            }
                        } else {
                            log.info("用户{}已经拥有宠物称号，无需重复添加", userId);
                        }
                    }
                } catch (Exception e) {
                    log.error("给用户{}添加宠物称号时发生异常", userId, e);
                }
            }

            // 5. 给前三名用户添加额外称号
            if (todayRankList != null && !todayRankList.isEmpty()) {
                // 第一名额外称号
                if (todayRankList.size() >= 1) {
                    Long firstUserId = todayRankList.get(0).getUserId();
                    try {
                        User user = userService.getById(firstUserId);
                        if (user != null) {
                            List<String> titleIds = new ArrayList<>();
                            if (user.getTitleIdList() != null && !user.getTitleIdList().isEmpty()) {
                                titleIds = JSON.parseArray(user.getTitleIdList(), String.class);
                            }

                            // 如果用户还没有第一名额外称号，则添加
                            if (!titleIds.contains(TitleConstant.PET_RANK_FIRST_TITLE_ID.toString())) {
                                boolean added = userTitleService.addTitleToUser(firstUserId, TitleConstant.PET_RANK_FIRST_TITLE_ID);
                                if (added) {
                                    updatedCount++;
                                    log.info("成功给第一名用户{}添加额外称号", firstUserId);
                                    eventRemindHandler.handleSystemMessage(firstUserId, "恭喜获得宠物排行榜第一名额外称号！");
                                }
                            } else {
                                log.info("第一名用户{}已经拥有额外称号，无需重复添加", firstUserId);
                            }
                        }
                    } catch (Exception e) {
                        log.error("给第一名用户{}添加额外称号时发生异常", firstUserId, e);
                    }
                }

                // 第二名额外称号
                if (todayRankList.size() >= 2) {
                    Long secondUserId = todayRankList.get(1).getUserId();
                    try {
                        User user = userService.getById(secondUserId);
                        if (user != null) {
                            List<String> titleIds = new ArrayList<>();
                            if (user.getTitleIdList() != null && !user.getTitleIdList().isEmpty()) {
                                titleIds = JSON.parseArray(user.getTitleIdList(), String.class);
                            }

                            // 如果用户还没有第二名额外称号，则添加
                            if (!titleIds.contains(TitleConstant.PET_RANK_SECOND_TITLE_ID.toString())) {
                                boolean added = userTitleService.addTitleToUser(secondUserId, TitleConstant.PET_RANK_SECOND_TITLE_ID);
                                if (added) {
                                    updatedCount++;
                                    log.info("成功给第二名用户{}添加额外称号", secondUserId);
                                    eventRemindHandler.handleSystemMessage(secondUserId, "恭喜获得宠物排行榜第二名额外称号！");
                                }
                            } else {
                                log.info("第二名用户{}已经拥有额外称号，无需重复添加", secondUserId);
                            }
                        }
                    } catch (Exception e) {
                        log.error("给第二名用户{}添加额外称号时发生异常", secondUserId, e);
                    }
                }

                // 第三名额外称号
                if (todayRankList.size() >= 3) {
                    Long thirdUserId = todayRankList.get(2).getUserId();
                    try {
                        User user = userService.getById(thirdUserId);
                        if (user != null) {
                            List<String> titleIds = new ArrayList<>();
                            if (user.getTitleIdList() != null && !user.getTitleIdList().isEmpty()) {
                                titleIds = JSON.parseArray(user.getTitleIdList(), String.class);
                            }

                            // 如果用户还没有第三名额外称号，则添加
                            if (!titleIds.contains(TitleConstant.PET_RANK_THIRD_TITLE_ID.toString())) {
                                boolean added = userTitleService.addTitleToUser(thirdUserId, TitleConstant.PET_RANK_THIRD_TITLE_ID);
                                if (added) {
                                    updatedCount++;
                                    log.info("成功给第三名用户{}添加额外称号", thirdUserId);
                                    eventRemindHandler.handleSystemMessage(thirdUserId, "恭喜获得宠物排行榜第三名额外称号！");
                                }
                            } else {
                                log.info("第三名用户{}已经拥有额外称号，无需重复添加", thirdUserId);
                            }
                        }
                    } catch (Exception e) {
                        log.error("给第三名用户{}添加额外称号时发生异常", thirdUserId, e);
                    }
                }
            }

            log.info("宠物排行榜称号更新任务执行完成，共更新{}个用户", updatedCount);
            return updatedCount;
        } catch (Exception e) {
            log.error("宠物排行榜称号更新任务执行异常", e);
            return 0;
        }
    }
} 
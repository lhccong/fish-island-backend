package com.cong.fishisland.service.impl;

import cn.dev33.satoken.stp.StpUtil;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.cong.fishisland.common.ErrorCode;
import com.cong.fishisland.common.exception.BusinessException;
import com.cong.fishisland.mapper.pet.FishPetMapper;
import com.cong.fishisland.model.dto.pet.CreatePetRequest;
import com.cong.fishisland.model.dto.pet.UpdatePetNameRequest;
import com.cong.fishisland.model.entity.pet.FishPet;
import com.cong.fishisland.model.vo.pet.OtherUserPetVO;
import com.cong.fishisland.model.vo.pet.PetSkinVO;
import com.cong.fishisland.model.vo.pet.PetVO;
import com.cong.fishisland.service.FishPetService;
import com.cong.fishisland.service.PetSkinService;
import com.cong.fishisland.service.UserPointsService;
import com.cong.fishisland.utils.RedisUtils;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import toolgood.words.StringSearch;

import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;

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

        // 增加1点经验值
        int newExp = fishPet.getExp() + 1;

        fishPet.setHunger(newHunger);
        fishPet.setMood(newMood);
        fishPet.setExp(newExp);

        boolean result = this.updateById(fishPet);
        if (!result) {
            throw new BusinessException(ErrorCode.OPERATION_ERROR, "喂食失败");
        }


        // 返回更新后的宠物信息
        PetVO petVO = new PetVO();
        BeanUtils.copyProperties(fishPet, petVO);

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

        // 增加1点经验值
        int newExp = fishPet.getExp() + 1;
        fishPet.setExp(newExp);

        boolean result = this.updateById(fishPet);
        if (!result) {
            throw new BusinessException(ErrorCode.OPERATION_ERROR, "抚摸失败");
        }


        // 返回更新后的宠物信息
        PetVO petVO = new PetVO();
        BeanUtils.copyProperties(fishPet, petVO);

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

        // 注意：在SQL实现中，只有当宠物的饥饿度(hunger)和心情值(mood)都大于0时，
        // 宠物才会获得经验并可能升级。这确保了宠物需要得到适当的照顾才能成长。
        return baseMapper.batchUpdateOnlineUserPetExp(userIds);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public int generateDailyPetPoints(int maxPoints) {
        // 获取所有符合条件的宠物（饥饿度和心情值都大于0）
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
                userPointsService.updatePoints(userId, pointsToAdd, false);

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
} 
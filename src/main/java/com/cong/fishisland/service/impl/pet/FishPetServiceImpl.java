package com.cong.fishisland.service.impl.pet;

import cn.dev33.satoken.stp.StpUtil;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.cong.fishisland.common.ErrorCode;
import com.cong.fishisland.common.exception.BusinessException;
import com.cong.fishisland.constant.PetForgeConstant;
import com.cong.fishisland.constant.PetRedisKey;
import com.cong.fishisland.constant.TitleConstant;
import com.cong.fishisland.mapper.pet.FishPetMapper;
import com.cong.fishisland.mapper.pet.PetEquipForgeMapper;
import com.cong.fishisland.model.dto.pet.CreatePetRequest;
import com.cong.fishisland.model.dto.pet.UpdatePetNameRequest;
import com.cong.fishisland.model.entity.pet.EquipEntry;
import com.cong.fishisland.model.entity.pet.FishPet;
import com.cong.fishisland.model.entity.pet.PetEquipForge;
import com.cong.fishisland.model.vo.pet.*;
import com.cong.fishisland.model.entity.user.User;
import com.cong.fishisland.service.*;

import static com.cong.fishisland.model.enums.user.PointsRecordSourceEnum.*;

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
    private final ItemInstancesService itemInstancesService;
    private final PetEquipForgeMapper petEquipForgeMapper;


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
    // 宠物等级上限
    private static final int PET_LEVEL_MAX = 60;

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
            fishPet.setPetUrl("https://oss.cqbo.com/moyu/pet/超级玛丽马里奥 (73)_爱给网_aigei_com.png");
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

        // 获取已穿戴的装备列表
        Map<String, ItemInstanceVO> equippedItems = getEquippedItems(fishPet);
        petVO.setEquippedItems(equippedItems);

        return petVO;
    }

    /**
     * 从宠物扩展数据中获取已穿戴的装备列表
     *
     * @param fishPet 宠物实体
     * @return 槽位 -> 装备VO 的映射
     */
    @Override
    public Map<String, ItemInstanceVO> getEquippedItems(FishPet fishPet) {
        Map<String, ItemInstanceVO> result = new HashMap<>();

        if (fishPet.getExtendData() == null || fishPet.getExtendData().isEmpty()) {
            return result;
        }

        JSONObject extendData = JSON.parseObject(fishPet.getExtendData());
        if (!extendData.containsKey("equippedItems")) {
            return result;
        }

        JSONObject equippedItemsJson = extendData.getJSONObject("equippedItems");
        if (equippedItemsJson == null || equippedItemsJson.isEmpty()) {
            return result;
        }

        // 预加载该宠物所有锻造记录，避免循环内多次查库
        Map<Integer, Integer> forgeEnhanceLevelMap = loadForgeEnhanceLevels(fishPet.getPetId());

        // 遍历所有槽位，获取装备详情
        for (String slot : equippedItemsJson.keySet()) {
            Long itemInstanceId = equippedItemsJson.getLong(slot);
            if (itemInstanceId != null) {
                ItemInstanceVO itemInstanceVO = itemInstancesService.getItemInstanceById(itemInstanceId);
                if (itemInstanceVO != null) {
                    // 解析装备属性
                    SingleEquipStatsVO equipStats = parseEquipStats(itemInstanceVO);
                    itemInstanceVO.setEquipStats(equipStats);
                    // 填充锻造强化等级
                    Integer forgeSlot = EQUIP_SLOT_NAME_MAP.get(slot);
                    if (forgeSlot != null) {
                        itemInstanceVO.setEnhanceLevel(forgeEnhanceLevelMap.getOrDefault(forgeSlot, 0));
                    }
                    result.put(slot, itemInstanceVO);
                }
            }
        }

        return result;
    }

    /**
     * 装备槽位名称（item_templates.equip_slot）到 pet_equip_forge.equipSlot 整数值的映射
     */
    private static final Map<String, Integer> EQUIP_SLOT_NAME_MAP;

    static {
        EQUIP_SLOT_NAME_MAP = new HashMap<>();
        EQUIP_SLOT_NAME_MAP.put("weapon", 1);
        EQUIP_SLOT_NAME_MAP.put("hand", 2);
        EQUIP_SLOT_NAME_MAP.put("foot", 3);
        EQUIP_SLOT_NAME_MAP.put("head", 4);
        EQUIP_SLOT_NAME_MAP.put("necklace", 5);
        EQUIP_SLOT_NAME_MAP.put("wing", 6);
    }

    /**
     * 查询宠物所有锻造装备的强化等级，返回 equipSlot -> equipLevel 映射
     */
    private Map<Integer, Integer> loadForgeEnhanceLevels(Long petId) {
        Map<Integer, Integer> map = new HashMap<>();
        if (petId == null) {
            return map;
        }
        try {
            List<PetEquipForge> forgeList =
                    petEquipForgeMapper.selectList(
                            new LambdaQueryWrapper<PetEquipForge>()
                                    .eq(PetEquipForge::getPetId, petId));
            for (PetEquipForge forge : forgeList) {
                if (forge.getEquipSlot() != null) {
                    map.put(forge.getEquipSlot(),
                            forge.getEquipLevel() == null ? 0 : forge.getEquipLevel());
                }
            }
        } catch (Exception e) {
            log.error("加载锻造强化等级失败，petId={}", petId, e);
        }
        return map;
    }

    /**
     * 解析单个装备的属性
     *
     * @param itemInstanceVO 装备VO
     * @return 装备属性VO
     */
    private SingleEquipStatsVO parseEquipStats(ItemInstanceVO itemInstanceVO) {
        SingleEquipStatsVO statsVO = new SingleEquipStatsVO();

        // 初始化默认值
        statsVO.setBaseAttack(0);
        statsVO.setBaseDefense(0);
        statsVO.setBaseHp(0);
        statsVO.setCritRate(0.0);
        statsVO.setComboRate(0.0);
        statsVO.setDodgeRate(0.0);
        statsVO.setBlockRate(0.0);
        statsVO.setLifesteal(0.0);
        statsVO.setCritResistance(0.0);
        statsVO.setComboResistance(0.0);
        statsVO.setDodgeResistance(0.0);
        statsVO.setBlockResistance(0.0);
        statsVO.setLifestealResistance(0.0);

        if (itemInstanceVO == null || itemInstanceVO.getTemplate() == null) {
            return statsVO;
        }

        ItemTemplateVO template = itemInstanceVO.getTemplate();

        // 基础属性
        if (template.getBaseAttack() != null) {
            statsVO.setBaseAttack(template.getBaseAttack());
        }
        if (template.getBaseDefense() != null) {
            statsVO.setBaseDefense(template.getBaseDefense());
        }
        if (template.getBaseHp() != null) {
            statsVO.setBaseHp(template.getBaseHp());
        }

        // 解析 mainAttr 属性
        if (template.getMainAttr() != null) {
            parseMainAttrForSingleEquip(template.getMainAttr(), statsVO);
        }

        return statsVO;
    }

    /**
     * 解析单个装备的 mainAttr 属性
     * mainAttr 格式: {"critRate":0.1, ...} 或 "{\"critRate\":0.1}"
     *
     * @param mainAttr 属性JSON
     * @param statsVO  装备属性VO
     */
    private void parseMainAttrForSingleEquip(Object mainAttr, SingleEquipStatsVO statsVO) {
        try {
            if (mainAttr == null) {
                return;
            }

            String mainAttrStr;
            if (mainAttr instanceof String) {
                mainAttrStr = (String) mainAttr;
            } else {
                mainAttrStr = JSON.toJSONString(mainAttr);
            }

            if (mainAttrStr == null || mainAttrStr.isEmpty() || "null".equals(mainAttrStr)) {
                return;
            }

            // 处理双重转义的 JSON 字符串: "{\"critRate\":0.1}"
            if (mainAttrStr.startsWith("\"") && mainAttrStr.endsWith("\"")) {
                // 先解析一次得到原始 JSON 字符串
                mainAttrStr = JSON.parseObject(mainAttrStr, String.class);
                if (mainAttrStr == null || mainAttrStr.isEmpty()) {
                    return;
                }
            }

            // 解析 JSON 对象格式 {"critRate":0.1}
            Map<String, Object> attrMap = JSON.parseObject(mainAttrStr, Map.class);
            if (attrMap == null) {
                return;
            }

            for (Map.Entry<String, Object> entry : attrMap.entrySet()) {
                String key = entry.getKey();
                Object valueObj = entry.getValue();

                if (key == null || valueObj == null) {
                    continue;
                }

                double value;
                if (valueObj instanceof Number) {
                    value = ((Number) valueObj).doubleValue();
                } else {
                    try {
                        value = Double.parseDouble(valueObj.toString());
                    } catch (NumberFormatException e) {
                        continue;
                    }
                }

                // 设置对应属性
                switch (key) {
                    case "critRate":
                        statsVO.setCritRate(value);
                        break;
                    case "comboRate":
                        statsVO.setComboRate(value);
                        break;
                    case "dodgeRate":
                        statsVO.setDodgeRate(value);
                        break;
                    case "blockRate":
                        statsVO.setBlockRate(value);
                        break;
                    case "lifesteal":
                        statsVO.setLifesteal(value);
                        break;
                    case "critResistance":
                        statsVO.setCritResistance(value);
                        break;
                    case "comboResistance":
                        statsVO.setComboResistance(value);
                        break;
                    case "dodgeResistance":
                        statsVO.setDodgeResistance(value);
                        break;
                    case "blockResistance":
                        statsVO.setBlockResistance(value);
                        break;
                    case "lifestealResistance":
                        statsVO.setLifestealResistance(value);
                        break;
                    default:
                        // 未知属性，忽略
                        break;
                }
            }
        } catch (Exception e) {
            log.error("解析装备 mainAttr 属性失败: {}", mainAttr, e);
        }
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
        userPointsService.deductPoints(userId, RENAME_POINT_COST, PET_RENAME.getValue(), null, "宠物改名");

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

        // 获取已穿戴的装备列表
        Map<String, ItemInstanceVO> equippedItems = getEquippedItems(fishPet);
        otherUserPetVO.setEquippedItems(equippedItems);

        // 获取宠物装备属性统计
        PetEquipStatsVO equipStats = this.getPetEquipStatsByUserId(otherUserId);
        otherUserPetVO.setEquipStats(equipStats);

        return otherUserPetVO;
    }

    @Override
    public PetVO feedPet(Long petId) {
        Long userId = StpUtil.getLoginIdAsLong();

        // 检查宠物是否存在且属于当前用户
        FishPet fishPet = checkPetOwnership(petId, userId);

        if (fishPet.getLevel() >= PET_LEVEL_MAX) {
            throw new BusinessException(ErrorCode.OPERATION_ERROR, "宠物已经达到60级，已会自己补充饥饿度");
        }

        // 检查饥饿度是否已满
        if (fishPet.getHunger() >= 100) {
            throw new BusinessException(ErrorCode.OPERATION_ERROR, "宠物已经吃饱了，不需要再喂食");
        }

        // 扣除用户积分
        userPointsService.deductPoints(userId, FEED_POINT_COST, PET_FEED.getValue(), null, "宠物喂食");

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

        if (fishPet.getLevel() >= PET_LEVEL_MAX) {
            throw new BusinessException(ErrorCode.OPERATION_ERROR, "宠物已经达到60级，已会自己补充心情值");
        }
        // 检查心情值是否已满
        if (fishPet.getMood() >= 100) {
            throw new BusinessException(ErrorCode.OPERATION_ERROR, "宠物心情已经很好了，不需要再抚摸");
        }

        // 扣除用户积分
        userPointsService.deductPoints(userId, PAT_POINT_COST, PET_PAT.getValue(), null, "宠物抚摸");

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
        // 批量更新宠物状态：30级宠物保持满状态，其他宠物扣除相应数值
        return baseMapper.batchUpdatePetStatus(hungerDecrement, moodDecrement);
    }

    @Override
    public int batchUpdateOnlineUserPetExp(List<String> userIds) {
        if (userIds == null || userIds.isEmpty()) {
            return 0;
        }

        // 在更新之前，查询哪些宠物会升到60级
        List<FishPet> petsToUpgrade = baseMapper.selectList(
                new LambdaQueryWrapper<FishPet>()
                        .in(FishPet::getUserId, userIds)
                        .eq(FishPet::getLevel, 59)
                        .ge(FishPet::getExp, 99) // 经验值为99，+1后会达到100
//                .and(wrapper -> wrapper.gt(FishPet::getHunger, 0).or().gt(FishPet::getMood, 0))
                        .eq(FishPet::getIsDelete, 0)
        );

        // 注意：在SQL实现中，只有当宠物的饥饿度(hunger)或心情值(mood)任意一个大于0时，
        // 宠物才会获得经验并可能升级。这确保了宠物得到基本照顾就能成长。
        // 当宠物升级到60级时，经验值会设为100，饥饿度设为0，心情值设为100。
        int updatedCount = baseMapper.batchUpdateOnlineUserPetExp(userIds);

        // 为升到60级的宠物记录时间到Redis
        if (!petsToUpgrade.isEmpty()) {
            String currentTime = String.valueOf(System.currentTimeMillis());
            for (FishPet pet : petsToUpgrade) {
                String petLevel60TimeKey = PetRedisKey.getKey(PetRedisKey.PET_LEVEL_60_TIME, pet.getUserId().toString());
                // 只有当Redis中还没有记录时才设置（避免重复设置）
                if (!RedisUtils.hasKey(petLevel60TimeKey)) {
                    // 永久存储，用于排行榜排序
                    RedisUtils.set(petLevel60TimeKey, currentTime, Duration.ofDays(365 * 10)); // 10年过期时间，基本等于永久
                    log.info("用户{}的宠物达到60级，记录时间：{}", pet.getUserId(), currentTime);
                }
            }
        }

        return updatedCount;
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
                userPointsService.updateUsedPoints(userId, -pointsToAdd, PET_DAILY.getValue(), null, "宠物每日产出积分");

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

            // 对宠物进行排序（包括30级宠物的时间排序）
            sortPetRankList(petRankList);

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

            // 对宠物进行排序（包括30级宠物的时间排序）
            sortPetRankList(petRankList);

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
     * 对宠物排行榜进行排序
     * 优先按等级排序，30级宠物按到达30级时间排序
     *
     * @param petRankList 宠物排行榜列表
     */
    private void sortPetRankList(List<PetRankVO> petRankList) {
        petRankList.sort((pet1, pet2) -> {
            // 优先按等级排序（降序）
            int levelCompare = Integer.compare(pet2.getLevel(), pet1.getLevel());
            if (levelCompare != 0) {
                return levelCompare;
            }

            // 如果等级相同且都是60级，按到达60级的时间排序
            if (pet1.getLevel() == 60 && pet2.getLevel() == 60) {
                String pet1TimeKey = PetRedisKey.getKey(PetRedisKey.PET_LEVEL_60_TIME, pet1.getUserId().toString());
                String pet2TimeKey = PetRedisKey.getKey(PetRedisKey.PET_LEVEL_60_TIME, pet2.getUserId().toString());

                String pet1Time = RedisUtils.get(pet1TimeKey);
                String pet2Time = RedisUtils.get(pet2TimeKey);

                // 如果都有时间记录，按时间升序排序（早的在前）
                if (pet1Time != null && pet2Time != null) {
                    try {
                        long time1 = Long.parseLong(pet1Time);
                        long time2 = Long.parseLong(pet2Time);
                        return Long.compare(time1, time2);
                    } catch (NumberFormatException e) {
                        log.warn("解析宠物60级时间失败，userId1: {}, userId2: {}", pet1.getUserId(), pet2.getUserId());
                    }
                }

                // 如果只有一个有时间记录，有记录的排前面
                if (pet1Time != null && pet2Time == null) {
                    return -1;
                }
                if (pet1Time == null && pet2Time != null) {
                    return 1;
                }

                // 如果都没有时间记录，按经验值排序
                return Integer.compare(pet2.getExp(), pet1.getExp());
            }

            // 如果不是60级，按经验值排序（降序）
            return Integer.compare(pet2.getExp(), pet1.getExp());
        });
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

    @Override
    public PetEquipStatsVO getPetEquipStats() {
        if (!StpUtil.isLogin()) {
            return null;
        }

        Long userId = StpUtil.getLoginIdAsLong();
        return getPetEquipStatsByUserId(userId);
    }

    @Override
    public PetEquipStatsVO getPetEquipStatsByPet(FishPet fishPet) {
        if (fishPet == null) {
            return null;
        }
        // 获取已穿戴的装备列表
        Map<String, ItemInstanceVO> equippedItems = getEquippedItems(fishPet);

        // 初始化属性统计
        PetEquipStatsVO statsVO = new PetEquipStatsVO();
        statsVO.setTotalBaseAttack(0);
        statsVO.setTotalBaseDefense(0);
        statsVO.setTotalBaseHp(0);
        statsVO.setSpeed(0);
        statsVO.setCritRate(0.0);
        statsVO.setComboRate(0.0);
        statsVO.setDodgeRate(0.0);
        statsVO.setBlockRate(0.0);
        statsVO.setLifesteal(0.0);
        statsVO.setCritResistance(0.0);
        statsVO.setComboResistance(0.0);
        statsVO.setDodgeResistance(0.0);
        statsVO.setBlockResistance(0.0);
        statsVO.setLifestealResistance(0.0);

        if (equippedItems != null && !equippedItems.isEmpty()) {
            for (ItemInstanceVO item : equippedItems.values()) {
                if (item == null || item.getTemplate() == null) {
                    continue;
                }
                ItemTemplateVO template = item.getTemplate();
                if (template.getBaseAttack() != null) {
                    statsVO.setTotalBaseAttack(statsVO.getTotalBaseAttack() + template.getBaseAttack());
                }
                if (template.getBaseDefense() != null) {
                    statsVO.setTotalBaseDefense(statsVO.getTotalBaseDefense() + template.getBaseDefense());
                }
                if (template.getBaseHp() != null) {
                    statsVO.setTotalBaseHp(statsVO.getTotalBaseHp() + template.getBaseHp());
                }
                if (template.getBaseSpeed() != null) {
                    statsVO.setSpeed(statsVO.getSpeed() == null ? template.getBaseSpeed() : statsVO.getSpeed() + template.getBaseSpeed());
                }
                if (template.getMainAttr() != null) {
                    parseMainAttr(template.getMainAttr(), statsVO);
                }
            }
        }

        // 叠加锻造词条属性和装备等级加成
        mergeForgeStats(fishPet.getPetId(), statsVO);

        return statsVO;
    }

    @Override
    public PetEquipStatsVO getPetEquipStatsByUserId(Long userId) {
        if (userId == null) {
            return null;
        }

        // 查询宠物
        QueryWrapper<FishPet> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq("userId", userId);
        FishPet fishPet = this.getOne(queryWrapper);

        if (fishPet == null) {
            return null;
        }

        // 获取已穿戴的装备列表
        Map<String, ItemInstanceVO> equippedItems = getEquippedItems(fishPet);

        // 初始化属性统计
        PetEquipStatsVO statsVO = new PetEquipStatsVO();
        statsVO.setTotalBaseAttack(0);
        statsVO.setTotalBaseDefense(0);
        statsVO.setTotalBaseHp(0);
        statsVO.setSpeed(0);
        statsVO.setCritRate(0.0);
        statsVO.setComboRate(0.0);
        statsVO.setDodgeRate(0.0);
        statsVO.setBlockRate(0.0);
        statsVO.setLifesteal(0.0);
        statsVO.setCritResistance(0.0);
        statsVO.setComboResistance(0.0);
        statsVO.setDodgeResistance(0.0);
        statsVO.setBlockResistance(0.0);
        statsVO.setLifestealResistance(0.0);

        // 遍历所有装备，累加属性
        if (equippedItems != null && !equippedItems.isEmpty()) {
            for (ItemInstanceVO item : equippedItems.values()) {
                if (item == null || item.getTemplate() == null) {
                    continue;
                }

                ItemTemplateVO template = item.getTemplate();

                // 累加基础属性
                if (template.getBaseAttack() != null) {
                    statsVO.setTotalBaseAttack(statsVO.getTotalBaseAttack() + template.getBaseAttack());
                }
                if (template.getBaseDefense() != null) {
                    statsVO.setTotalBaseDefense(statsVO.getTotalBaseDefense() + template.getBaseDefense());
                }
                if (template.getBaseHp() != null) {
                    statsVO.setTotalBaseHp(statsVO.getTotalBaseHp() + template.getBaseHp());
                }
                if (template.getBaseSpeed() != null) {
                    statsVO.setSpeed(statsVO.getSpeed() == null ? template.getBaseSpeed() : statsVO.getSpeed() + template.getBaseSpeed());
                }

                // 解析 mainAttr 属性
                if (template.getMainAttr() != null) {
                    parseMainAttr(template.getMainAttr(), statsVO);
                }
            }
        }

        // 叠加锻造词条属性和装备等级加成
        mergeForgeStats(fishPet.getPetId(), statsVO);

        return statsVO;
    }

    /**
     * 将宠物锻造装备的词条属性和装备等级加成叠加到已有的统计VO中
     *
     * @param petId   宠物ID
     * @param statsVO 已初始化的装备属性统计VO（会被直接修改）
     */
    private void mergeForgeStats(Long petId, PetEquipStatsVO statsVO) {
        if (petId == null) {
            return;
        }
        try {
            List<PetEquipForge> forgeList =
                    petEquipForgeMapper.selectList(
                            new LambdaQueryWrapper<PetEquipForge>()
                                    .eq(PetEquipForge::getPetId, petId));
            if (forgeList == null || forgeList.isEmpty()) {
                return;
            }
            // 复用 PetEquipForgeServiceImpl 的逻辑：按词条和等级累加
            for (PetEquipForge forge : forgeList) {
                // 装备等级加成（按槽位差异化，非线性成长）
                int level = forge.getEquipLevel() == null ? 0 : forge.getEquipLevel();
                int slot  = forge.getEquipSlot()  == null ? 0 : forge.getEquipSlot();
                PetEquipForgeServiceImpl.applyLevelBonusBySlot(slot, level, statsVO);

                applyForgeEntry(forge.getEntry1(), statsVO);
                applyForgeEntry(forge.getEntry2(), statsVO);
                applyForgeEntry(forge.getEntry3(), statsVO);
                applyForgeEntry(forge.getEntry4(), statsVO);
            }
        } catch (Exception e) {
            log.error("合并锻造属性失败，petId={}", petId, e);
        }
    }

    /**
     * 将单条锻造词条累加到统计VO
     */
    private void applyForgeEntry(EquipEntry entry, PetEquipStatsVO stats) {
        if (entry == null || entry.getAttr() == null || entry.getValue() == null) {
            return;
        }
        double value = entry.getValue();
        switch (entry.getAttr()) {
            case "attack":        stats.setTotalBaseAttack(stats.getTotalBaseAttack() + (int) value); break;
            case "maxHp":         stats.setTotalBaseHp(stats.getTotalBaseHp() + (int) value); break;
            case "defense":       stats.setTotalBaseDefense(stats.getTotalBaseDefense() + (int) value); break;
            case "speed":         stats.setSpeed(stats.getSpeed() == null ? (int) value : stats.getSpeed() + (int) value); break;
            case "critRate":      stats.setCritRate(stats.getCritRate() + value); break;
            case "comboRate":     stats.setComboRate(stats.getComboRate() + value); break;
            case "dodgeRate":     stats.setDodgeRate(stats.getDodgeRate() + value); break;
            case "blockRate":     stats.setBlockRate(stats.getBlockRate() + value); break;
            case "lifesteal":     stats.setLifesteal(stats.getLifesteal() + value); break;
            case "antiCrit":      stats.setCritResistance(stats.getCritResistance() + value); break;
            case "antiCombo":     stats.setComboResistance(stats.getComboResistance() + value); break;
            case "antiDodge":     stats.setDodgeResistance(stats.getDodgeResistance() + value); break;
            case "antiBlock":     stats.setBlockResistance(stats.getBlockResistance() + value); break;
            case "antiLifesteal": stats.setLifestealResistance(stats.getLifestealResistance() + value); break;
            default: break;
        }
    }

    /**
     * 解析装备的 mainAttr 属性，累加到统计VO中
     * mainAttr 格式: {"critRate":0.1, ...} 或 "{\"critRate\":0.1}"
     *
     * @param mainAttr 属性JSON
     * @param statsVO  统计VO
     */
    private void parseMainAttr(Object mainAttr, PetEquipStatsVO statsVO) {
        try {
            if (mainAttr == null) {
                return;
            }

            String mainAttrStr;
            if (mainAttr instanceof String) {
                mainAttrStr = (String) mainAttr;
            } else {
                mainAttrStr = JSON.toJSONString(mainAttr);
            }

            if (mainAttrStr == null || mainAttrStr.isEmpty() || "null".equals(mainAttrStr)) {
                return;
            }

            // 处理双重转义的 JSON 字符串: "{\"critRate\":0.1}"
            if (mainAttrStr.startsWith("\"") && mainAttrStr.endsWith("\"")) {
                // 先解析一次得到原始 JSON 字符串
                mainAttrStr = JSON.parseObject(mainAttrStr, String.class);
                if (mainAttrStr == null || mainAttrStr.isEmpty()) {
                    return;
                }
            }

            // 解析 JSON 对象格式 {"critRate":0.1}
            Map<String, Object> attrMap = JSON.parseObject(mainAttrStr, Map.class);
            if (attrMap == null) {
                return;
            }

            for (Map.Entry<String, Object> entry : attrMap.entrySet()) {
                if (entry == null) {
                    continue;
                }
                String key = entry.getKey();
                Object valueObj = entry.getValue();

                if (key == null || valueObj == null) {
                    continue;
                }

                double value;
                if (valueObj instanceof Number) {
                    value = ((Number) valueObj).doubleValue();
                } else {
                    try {
                        value = Double.parseDouble(valueObj.toString());
                    } catch (NumberFormatException e) {
                        continue;
                    }
                }

                // 累加对应属性
                switch (key) {
                    case "critRate":
                        statsVO.setCritRate(statsVO.getCritRate() + value);
                        break;
                    case "comboRate":
                        statsVO.setComboRate(statsVO.getComboRate() + value);
                        break;
                    case "dodgeRate":
                        statsVO.setDodgeRate(statsVO.getDodgeRate() + value);
                        break;
                    case "blockRate":
                        statsVO.setBlockRate(statsVO.getBlockRate() + value);
                        break;
                    case "lifesteal":
                        statsVO.setLifesteal(statsVO.getLifesteal() + value);
                        break;
                    case "critResistance":
                        statsVO.setCritResistance(statsVO.getCritResistance() + value);
                        break;
                    case "comboResistance":
                        statsVO.setComboResistance(statsVO.getComboResistance() + value);
                        break;
                    case "dodgeResistance":
                        statsVO.setDodgeResistance(statsVO.getDodgeResistance() + value);
                        break;
                    case "blockResistance":
                        statsVO.setBlockResistance(statsVO.getBlockResistance() + value);
                        break;
                    case "lifestealResistance":
                        statsVO.setLifestealResistance(statsVO.getLifestealResistance() + value);
                        break;
                    default:
                        // 未知属性，忽略
                        break;
                }
            }
        } catch (Exception e) {
            log.error("解析装备 mainAttr 属性失败: {}", mainAttr, e);
        }
    }
} 
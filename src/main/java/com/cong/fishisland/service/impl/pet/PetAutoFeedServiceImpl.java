package com.cong.fishisland.service.impl.pet;

import cn.dev33.satoken.stp.StpUtil;
import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.cong.fishisland.common.ErrorCode;
import com.cong.fishisland.common.exception.BusinessException;
import com.cong.fishisland.mapper.pet.PetAutoFeedConfigMapper;
import com.cong.fishisland.model.dto.pet.PetAutoFeedConfigRequest;
import com.cong.fishisland.model.entity.pet.FishPet;
import com.cong.fishisland.model.entity.pet.ItemInstances;
import com.cong.fishisland.model.entity.pet.ItemTemplates;
import com.cong.fishisland.model.entity.pet.PetAutoFeedConfig;
import com.cong.fishisland.model.vo.pet.PetAutoFeedConfigVO;
import com.cong.fishisland.service.FishPetService;
import com.cong.fishisland.service.ItemInstancesService;
import com.cong.fishisland.service.ItemTemplatesService;
import com.cong.fishisland.service.PetAutoFeedService;
import com.cong.fishisland.service.UserPointsService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

import static com.cong.fishisland.model.enums.user.PointsRecordSourceEnum.PET_AUTO_FEED;

/**
 * 宠物自动喂食服务实现
 *
 * @author cong
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class PetAutoFeedServiceImpl extends ServiceImpl<PetAutoFeedConfigMapper, PetAutoFeedConfig>
        implements PetAutoFeedService {

    private final FishPetService fishPetService;
    private final ItemInstancesService itemInstancesService;
    private final ItemTemplatesService itemTemplatesService;
    private final UserPointsService userPointsService;

    /**
     * 自注入，用于在同类方法间保证 @Transactional 生效（通过 Spring 代理）
     */
    @Autowired
    @Lazy
    private PetAutoFeedService self;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public PetAutoFeedConfigVO saveOrUpdateConfig(PetAutoFeedConfigRequest request) {
        Long userId = StpUtil.getLoginIdAsLong();

        if (request == null || request.getPetId() == null) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "宠物ID不能为空");
        }
        if (request.getEnabled() == null || (request.getEnabled() != 0 && request.getEnabled() != 1)) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "启用状态只能为0或1");
        }
        if (StringUtils.isBlank(request.getFoodCode())) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "食物类型不能为空");
        }
        if (request.getTriggerThreshold() == null || request.getTriggerThreshold() < 1 || request.getTriggerThreshold() > 100) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "触发阈值范围为1-100");
        }

        // 校验宠物归属
        FishPet fishPet = fishPetService.getOne(
                new LambdaQueryWrapper<FishPet>()
                        .eq(FishPet::getPetId, request.getPetId())
                        .eq(FishPet::getUserId, userId)
        );
        if (fishPet == null) {
            throw new BusinessException(ErrorCode.NOT_FOUND_ERROR, "宠物不存在或不属于当前用户");
        }

        // 校验食物模板是否存在
        ItemTemplates foodTemplate = itemTemplatesService.getOne(
                new LambdaQueryWrapper<ItemTemplates>()
                        .eq(ItemTemplates::getCode, request.getFoodCode())
                        .eq(ItemTemplates::getCategory, "consumable")
                        .eq(ItemTemplates::getSubType, "food")
        );
        if (foodTemplate == null) {
            throw new BusinessException(ErrorCode.NOT_FOUND_ERROR, "食物模板不存在，请检查食物类型");
        }

        // 查询是否已有配置（同一用户同一宠物只有一条）
        PetAutoFeedConfig config = this.getOne(
                new LambdaQueryWrapper<PetAutoFeedConfig>()
                        .eq(PetAutoFeedConfig::getUserId, userId)
                        .eq(PetAutoFeedConfig::getPetId, request.getPetId())
        );

        if (config == null) {
            config = new PetAutoFeedConfig();
            config.setUserId(userId);
            config.setPetId(request.getPetId());
        }

        config.setEnabled(request.getEnabled());
        config.setFoodCode(request.getFoodCode());
        config.setTriggerThreshold(request.getTriggerThreshold());

        this.saveOrUpdate(config);

        return buildVO(config, foodTemplate, userId);
    }

    @Override
    public PetAutoFeedConfigVO getConfig(Long petId) {
        Long userId = StpUtil.getLoginIdAsLong();

        PetAutoFeedConfig config = this.getOne(
                new LambdaQueryWrapper<PetAutoFeedConfig>()
                        .eq(PetAutoFeedConfig::getUserId, userId)
                        .eq(PetAutoFeedConfig::getPetId, petId)
        );

        if (config == null) {
            return null;
        }

        ItemTemplates foodTemplate = itemTemplatesService.getOne(
                new LambdaQueryWrapper<ItemTemplates>()
                        .eq(ItemTemplates::getCode, config.getFoodCode())
        );

        return buildVO(config, foodTemplate, userId);
    }

    @Override
    public int executeAutoFeed() {
        // 查询所有启用的自动喂食配置
        List<PetAutoFeedConfig> enabledConfigs = this.list(
                new LambdaQueryWrapper<PetAutoFeedConfig>()
                        .eq(PetAutoFeedConfig::getEnabled, 1)
        );

        if (enabledConfigs == null || enabledConfigs.isEmpty()) {
            log.info("没有启用的自动喂食配置");
            return 0;
        }

        int successCount = 0;

        for (PetAutoFeedConfig config : enabledConfigs) {
            try {
                // 通过 self（Spring 代理）调用，确保 @Transactional 生效
                boolean fed = self.doAutoFeedSingle(config);
                if (fed) {
                    successCount++;
                }
            } catch (Exception e) {
                log.error("自动喂食异常，userId={}, petId={}", config.getUserId(), config.getPetId(), e);
            }
        }

        return successCount;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean doAutoFeedSingle(PetAutoFeedConfig config) {
        Long userId = config.getUserId();
        Long petId = config.getPetId();

        // 1. 查询宠物当前状态
        FishPet fishPet = fishPetService.getOne(
                new LambdaQueryWrapper<FishPet>()
                        .eq(FishPet::getPetId, petId)
                        .eq(FishPet::getUserId, userId)
        );
        if (fishPet == null) {
            log.warn("自动喂食：宠物不存在，userId={}, petId={}", userId, petId);
            return false;
        }

        // 2. 检查饱食度是否低于阈值（hunger 越高越饱，低于阈值说明宠物饿了需要喂食）
        int currentHunger = fishPet.getHunger() == null ? 0 : fishPet.getHunger();
        // 饱食度已满（100），不需要喂食
        if (currentHunger >= 100) {
            return false;
        }
        // 饱食度高于触发阈值，说明还不够饿，不触发喂食
        if (currentHunger >= config.getTriggerThreshold()) {
            return false;
        }

        // 3. 查询食物模板
        ItemTemplates foodTemplate = itemTemplatesService.getOne(
                new LambdaQueryWrapper<ItemTemplates>()
                        .eq(ItemTemplates::getCode, config.getFoodCode())
                        .eq(ItemTemplates::getCategory, "consumable")
                        .eq(ItemTemplates::getSubType, "food")
        );
        if (foodTemplate == null) {
            log.warn("自动喂食：食物模板不存在，foodCode={}", config.getFoodCode());
            return false;
        }

        // 4. 查询用户背包中是否有该食物
        ItemInstances foodInstance = itemInstancesService.getOne(
                new QueryWrapper<ItemInstances>()
                        .eq("ownerUserId", userId)
                        .eq("templateId", foodTemplate.getId())
                        .last("LIMIT 1")
        );
        if (foodInstance == null || foodInstance.getQuantity() == null || foodInstance.getQuantity() <= 0) {
            log.info("自动喂食：用户背包中没有食物，userId={}, foodCode={}", userId, config.getFoodCode());
            return false;
        }

        // 5. 解析食物效果（从 mainAttr 读取）
        int hungerRestore = 20;
        int moodBonus = 0;
        int expBonus = 0;
        if (StringUtils.isNotBlank(foodTemplate.getMainAttr())) {
            try {
                JSONObject attr = JSON.parseObject(foodTemplate.getMainAttr());
                if (attr.containsKey("hungerRestore")) {
                    hungerRestore = attr.getIntValue("hungerRestore");
                }
                if (attr.containsKey("moodBonus")) {
                    moodBonus = attr.getIntValue("moodBonus");
                }
                if (attr.containsKey("expBonus")) {
                    expBonus = attr.getIntValue("expBonus");
                }
            } catch (Exception e) {
                log.warn("解析食物属性失败，使用默认值，foodCode={}", config.getFoodCode(), e);
            }
        }

        // 6. 消耗一个食物，并记录自动喂食积分日志（食物本身已花积分购买，此处仅做行为记录，不再扣分）
        itemInstancesService.consumeItem(foodInstance.getId(), 1);
        userPointsService.updateUsedPoints(userId, 0, PET_AUTO_FEED.getValue(),
                petId.toString(), "宠物自动喂食消耗食物：" + foodTemplate.getName());

        // 7. 更新宠物饱食度、心情值和经验值
        // hunger 越高越饱，喂食后增加饱食度，上限 100
        int newHunger = Math.min(100, currentHunger + hungerRestore);
        int currentMood = fishPet.getMood() == null ? 0 : fishPet.getMood();
        int newMood = Math.min(100, currentMood + moodBonus);

        fishPet.setHunger(newHunger);
        fishPet.setMood(newMood);

        // 经验加成：60 级封顶，升级逻辑与 batchUpdateOnlineUserPetExp 保持一致
        if (expBonus > 0) {
            int currentLevel = fishPet.getLevel() == null ? 1 : fishPet.getLevel();
            if (currentLevel < 60) {
                int currentExp = fishPet.getExp() == null ? 0 : fishPet.getExp();
                int newExp = currentExp + expBonus;
                // 循环处理连续升级（expBonus 较大时可能跨多级）
                while (newExp >= 100 && currentLevel < 60) {
                    newExp -= 100;
                    currentLevel++;
                }
                // 60 级时经验固定为 100，饥饿度和心情值回满（与 XML 升级逻辑一致）
                if (currentLevel >= 60) {
                    currentLevel = 60;
                    newExp = 100;
                    fishPet.setHunger(100);
                    fishPet.setMood(100);
                }
                fishPet.setLevel(currentLevel);
                fishPet.setExp(newExp);
            }
        }

        fishPetService.updateById(fishPet);

        log.info("自动喂食成功：userId={}, petId={}, 食物={}, 饥饿度 {} -> {}, 心情 {} -> {}, 经验+{}",
                userId, petId, config.getFoodCode(), currentHunger, newHunger, currentMood, newMood, expBonus);

        return true;
    }

    /**
     * 构建 VO
     */
    private PetAutoFeedConfigVO buildVO(PetAutoFeedConfig config, ItemTemplates foodTemplate, Long userId) {        PetAutoFeedConfigVO vo = new PetAutoFeedConfigVO();
        BeanUtils.copyProperties(config, vo);

        if (foodTemplate != null) {
            vo.setFoodName(foodTemplate.getName());
            vo.setFoodIcon(foodTemplate.getIcon());

            // 查询背包中该食物的剩余数量
            ItemInstances foodInstance = itemInstancesService.getOne(
                    new QueryWrapper<ItemInstances>()
                            .eq("ownerUserId", userId)
                            .eq("templateId", foodTemplate.getId())
                            .last("LIMIT 1")
            );
            vo.setRemainingQuantity(foodInstance != null && foodInstance.getQuantity() != null
                    ? foodInstance.getQuantity() : 0);
        } else {
            vo.setRemainingQuantity(0);
        }

        return vo;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void buyFood(String foodCode, int quantity) {
        if (StringUtils.isBlank(foodCode)) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "食物类型不能为空");
        }
        if (quantity <= 0) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "购买数量必须大于0");
        }

        // 通过 code 查出 templateId，委托给通用购买逻辑
        ItemTemplates foodTemplate = itemTemplatesService.getOne(
                new LambdaQueryWrapper<ItemTemplates>()
                        .eq(ItemTemplates::getCode, foodCode)
                        .eq(ItemTemplates::getCategory, "consumable")
                        .eq(ItemTemplates::getSubType, "food")
        );
        if (foodTemplate == null) {
            throw new BusinessException(ErrorCode.NOT_FOUND_ERROR, "食物不存在");
        }

        itemTemplatesService.purchaseItem(foodTemplate.getId(), quantity);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public PetAutoFeedConfigVO toggleAutoFeed(Long petId, int enabled) {
        if (petId == null || petId <= 0) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "宠物ID不能为空");
        }
        if (enabled != 0 && enabled != 1) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "状态值只能为0或1");
        }

        Long userId = StpUtil.getLoginIdAsLong();

        // 查询配置，不存在则报错（必须先通过 saveOrUpdateConfig 创建配置才能开关）
        PetAutoFeedConfig config = this.getOne(
                new LambdaQueryWrapper<PetAutoFeedConfig>()
                        .eq(PetAutoFeedConfig::getUserId, userId)
                        .eq(PetAutoFeedConfig::getPetId, petId)
        );
        if (config == null) {
            throw new BusinessException(ErrorCode.NOT_FOUND_ERROR, "尚未配置自动喂食，请先保存配置");
        }

        config.setEnabled(enabled);
        this.updateById(config);

        ItemTemplates foodTemplate = itemTemplatesService.getOne(
                new LambdaQueryWrapper<ItemTemplates>()
                        .eq(ItemTemplates::getCode, config.getFoodCode())
        );

        log.info("{}自动喂食：userId={}, petId={}", enabled == 1 ? "开启" : "关闭", userId, petId);
        return buildVO(config, foodTemplate, userId);
    }
}

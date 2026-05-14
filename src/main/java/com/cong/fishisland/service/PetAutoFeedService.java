package com.cong.fishisland.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.cong.fishisland.model.dto.pet.PetAutoFeedConfigRequest;
import com.cong.fishisland.model.entity.pet.PetAutoFeedConfig;
import com.cong.fishisland.model.vo.pet.PetAutoFeedConfigVO;

/**
 * 宠物自动喂食服务接口
 *
 * @author cong
 */
public interface PetAutoFeedService extends IService<PetAutoFeedConfig> {

    /**
     * 保存或更新自动喂食配置（同一用户同一宠物只有一条配置）
     *
     * @param request 配置请求
     * @return 配置VO
     */
    PetAutoFeedConfigVO saveOrUpdateConfig(PetAutoFeedConfigRequest request);

    /**
     * 获取当前用户的自动喂食配置
     *
     * @param petId 宠物ID
     * @return 配置VO，不存在则返回 null
     */
    PetAutoFeedConfigVO getConfig(Long petId);

    /**
     * 执行自动喂食（定时任务调用）
     * 遍历所有启用的配置，对饥饿度低于阈值的宠物自动消耗食物喂食
     *
     * @return 成功喂食的宠物数量
     */
    int executeAutoFeed();

    /**
     * 对单条配置执行自动喂食（事务方法，由 executeAutoFeed 通过代理调用）
     *
     * @param config 自动喂食配置
     * @return 是否执行了喂食
     */
    boolean doAutoFeedSingle(PetAutoFeedConfig config);

    /**
     * 购买食物（扣除积分，食物入背包）
     *
     * @param foodCode 食物模板 code
     * @param quantity 购买数量
     */
    void buyFood(String foodCode, int quantity);

    /**
     * 开启或关闭自动喂食
     *
     * @param petId   宠物ID
     * @param enabled 1-开启，0-关闭
     * @return 更新后的配置VO
     */
    PetAutoFeedConfigVO toggleAutoFeed(Long petId, int enabled);
}

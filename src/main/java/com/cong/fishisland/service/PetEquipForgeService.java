package com.cong.fishisland.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.cong.fishisland.model.dto.pet.ForgeRefreshRequest;
import com.cong.fishisland.model.dto.pet.ForgeUpgradeRequest;
import com.cong.fishisland.model.entity.pet.PetEquipForge;
import com.cong.fishisland.model.vo.pet.PetEquipForgeVO;

import java.util.List;

/**
 * 宠物装备锻造服务
 *
 * @author cong
 */
public interface PetEquipForgeService extends IService<PetEquipForge> {

    /**
     * 获取宠物所有装备锻造信息
     *
     * @param petId 宠物ID
     * @return 装备列表
     */
    List<PetEquipForgeVO> listByPetId(Long petId);

    /**
     * 刷新装备词条（四条一起刷新，锁定的词条保留）
     * 基础消耗 100 积分，每锁定一条额外 +50 积分
     *
     * @param request 刷新请求
     * @return 刷新后的装备信息
     */
    PetEquipForgeVO refreshEntries(ForgeRefreshRequest request);

    /**
     * 装备升级（武器不支持）
     * 消耗积分随等级提升，成功概率随等级降低
     *
     * @param request 升级请求
     * @return 升级结果（true=成功，false=失败）
     */
    boolean upgradeEquip(ForgeUpgradeRequest request);
}

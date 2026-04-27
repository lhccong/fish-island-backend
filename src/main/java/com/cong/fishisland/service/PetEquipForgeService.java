package com.cong.fishisland.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.cong.fishisland.model.dto.pet.ForgeLockRequest;
import com.cong.fishisland.model.dto.pet.ForgeRefreshRequest;
import com.cong.fishisland.model.dto.pet.ForgeUpgradeRequest;
import com.cong.fishisland.model.entity.pet.PetEquipForge;
import com.cong.fishisland.model.vo.pet.PetEquipForgeDetailVO;
import com.cong.fishisland.model.vo.pet.PetEquipForgeVO;
import com.cong.fishisland.model.vo.pet.PetEquipStatsVO;

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

    /**
     * 查询单件装备锻造详情（含本次升级消耗积分和成功概率）
     *
     * @param petId     宠物ID
     * @param equipSlot 装备位置 1-武器 2-手套 3-鞋子 4-头盔 5-项链 6-翅膀
     * @return 装备锻造详情
     */
    PetEquipForgeDetailVO getForgeDetail(Long petId, Integer equipSlot);

    /**
     * 锁定/解锁词条
     * 指定需要锁定的词条序号，未在列表中的词条将被解锁，传空列表表示解锁全部
     *
     * @param request 锁定请求
     * @return 更新后的装备信息
     */
    PetEquipForgeVO lockEntries(ForgeLockRequest request);

    /**
     * 汇总宠物所有锻造装备的词条属性和装备等级加成
     * <p>
     * 词条属性直接累加到对应字段；装备等级加成：每级 +2 攻击/防御/生命，+0.1% 概率属性。
     *
     * @param petId 宠物ID
     * @return 锻造属性统计VO（不含装备模板基础属性，仅锻造部分）
     */
    PetEquipStatsVO getForgeStatsByPetId(Long petId);
}

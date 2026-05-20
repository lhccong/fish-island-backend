package com.cong.fishisland.service;

import com.cong.fishisland.model.dto.farm.LandDTO;
import com.cong.fishisland.model.entity.farm.FarmLand;

import java.util.List;

/**
 * 农场地块服务
 */
public interface FarmLandService {

    /**
     * 按农场用户 ID 查询地块列表（按地块序号升序）。
     * 若该用户尚无地块，会自动初始化后再返回。
     *
     * @param userId 系统用户 ID
     * @return 地块列表，不为 null
     */
    List<FarmLand> getLandsByUserId(Long userId);

    /**
     * 为指定农场用户初始化地块（最多 9 块，前 3 块默认解锁）。
     *
     * @param userId 系统用户 ID
     */
    void initLands(Long userId);

    /**
     * 在指定地块种植作物，扣减种子积分并创建种植记录。
     *
     * @param landId 地块 ID
     * @param cropId 作物 ID
     * @return 更新后的地块；地块不存在、非空闲或作物不存在时返回 null
     */
    FarmLand plant(Long landId, Long cropId);

    /**
     * 收获指定地块上的成熟作物，发放积分并更新图鉴与农场用户统计。
     *
     * @param userId 系统用户 ID
     * @param landId 地块 ID
     * @return 清空种植状态后的地块；地块不存在或未成熟时返回 null
     */
    FarmLand harvest(Long userId, Long landId);

    /**
     * 将地块实体转换为 DTO。
     *
     * @param land 地块实体
     * @return 地块 DTO；入参为 null 时返回 null
     */
    LandDTO toDTO(FarmLand land);

    /**
     * 批量将地块实体转换为 DTO。
     *
     * @param lands 地块列表
     * @return 地块 DTO 列表
     */
    List<LandDTO> toDTOList(List<FarmLand> lands);
}

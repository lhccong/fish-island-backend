package com.cong.fishisland.service;

import com.cong.fishisland.model.dto.farm.LandDTO;
import com.cong.fishisland.model.dto.farm.PlantItem;
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
     * 为指定农场用户初始化地块（最多 24 块，前 8 块默认解锁）。
     *
     * @param userId 系统用户 ID
     */
    void initLands(Long userId);

    /**
     * 批量在指定地块种植作物，扣减种子积分并创建种植记录。
     * 任一块地校验失败时整批回滚。
     *
     * @param items 种植项列表（每项含地块 ID、作物 ID）
     * @return 更新后的地块列表（顺序与入参一致）
     */
    List<FarmLand> plantBatch(List<PlantItem> items);

    /**
     * 批量收获指定地块上的成熟作物，发放积分并更新图鉴与农场用户统计。
     * 任一块地校验失败时整批回滚。
     *
     * @param landIds 地块 ID 列表
     * @return 清空种植状态后的地块列表（顺序与入参一致）
     * @throws com.cong.fishisland.common.exception.BusinessException 地块不存在、无权操作、未种植或未成熟时
     */
    List<FarmLand> harvestBatch(List<Long> landIds);

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

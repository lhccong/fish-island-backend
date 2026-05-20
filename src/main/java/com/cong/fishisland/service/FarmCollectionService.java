package com.cong.fishisland.service;

import com.cong.fishisland.model.dto.farm.CollectionDTO;
import com.cong.fishisland.model.dto.farm.CollectionStatsVO;
import com.cong.fishisland.model.entity.farm.FarmCollection;

import java.util.List;

/**
 * 农场作物图鉴服务
 */
public interface FarmCollectionService {

    /**
     * 查询指定农场用户的图鉴记录。
     *
     * @param userId 农场用户 ID（{@code farm_user.id}）
     * @return 图鉴列表
     */
    List<FarmCollection> getUserCollections(Long userId);

    /**
     * 收获作物后更新图鉴（首次获得标记已获得，获得次数累加）。
     *
     * @param userId 农场用户 ID（{@code farm_user.id}）
     * @param cropId 作物 ID
     */
    void updateCollection(Long userId, Long cropId);

    /**
     * 统计指定用户已获得的图鉴数量。
     *
     * @param userId 农场用户 ID（{@code farm_user.id}）
     * @return 已获得图鉴数
     */
    long getObtainedCount(Long userId);

    /**
     * 为指定用户初始化未获得的图鉴占位记录。
     *
     * @param userId 农场用户 ID（{@code farm_user.id}）
     */
    void initCollections(Long userId);

    /**
     * 将图鉴实体转换为 DTO。
     *
     * @param collection 图鉴实体
     * @return 图鉴 DTO；入参为 null 时返回 null
     */
    CollectionDTO toDTO(FarmCollection collection);

    /**
     * 批量将图鉴实体转换为 DTO。
     *
     * @param collections 图鉴列表
     * @return 图鉴 DTO 列表
     */
    List<CollectionDTO> toDTOList(List<FarmCollection> collections);

    /**
     * 获取指定用户的收集册统计信息。
     *
     * @param userId 农场用户 ID（{@code farm_user.id}）
     * @return 收集册统计 VO
     */
    CollectionStatsVO getCollectionStats(Long userId);
}

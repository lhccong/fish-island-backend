package com.cong.fishisland.service;

import com.cong.fishisland.model.dto.farm.CropCategoryVO;
import com.cong.fishisland.model.dto.farm.CropDTO;
import com.cong.fishisland.model.entity.farm.FarmCrop;

import java.util.List;

/**
 * 农场作物配置服务
 */
public interface FarmCropService {

    /**
     * 查询全部作物。
     *
     * @return 作物列表
     */
    List<FarmCrop> getAllCrops();

    /**
     * 按分类查询作物。
     *
     * @param category 分类（如 grain、vegetable、fruit、flower）
     * @return 作物列表
     */
    List<FarmCrop> getCropsByCategory(String category);

    /**
     * 按 ID 查询作物。
     *
     * @param cropId 作物 ID
     * @return 作物，不存在时返回 null
     */
    FarmCrop getCropById(Long cropId);

    /**
     * 创建作物配置。
     *
     * @param crop 作物实体
     * @return 保存后的作物
     */
    FarmCrop createCrop(FarmCrop crop);

    /**
     * 获取支持的作物分类列表。
     *
     * @return 分类选项列表（枚举 value + label）
     */
    List<CropCategoryVO> getCategories();

    /**
     * 将作物实体转换为 DTO，并根据农场等级计算是否未解锁。
     *
     * @param crop      作物实体
     * @param farmLevel 当前农场等级
     * @return 作物 DTO；入参为 null 时返回 null
     */
    CropDTO toDTO(FarmCrop crop, Integer farmLevel);

    /**
     * 批量将作物实体转换为 DTO。
     *
     * @param crops     作物列表
     * @param farmLevel 当前农场等级
     * @return 作物 DTO 列表
     */
    List<CropDTO> toDTOList(List<FarmCrop> crops, Integer farmLevel);

    /**
     * 判断作物是否已对指定农场等级解锁。
     *
     * @param crop      作物
     * @param farmLevel 农场等级
     * @return 已解锁返回 true
     */
    boolean isUnlocked(FarmCrop crop, Integer farmLevel);
}

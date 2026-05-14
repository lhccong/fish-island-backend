package com.cong.fishisland.mapper.pet;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.cong.fishisland.model.entity.pet.PetAutoFeedConfig;
import org.apache.ibatis.annotations.Mapper;

/**
 * 宠物自动喂食配置 Mapper
 *
 * @author cong
 */
@Mapper
public interface PetAutoFeedConfigMapper extends BaseMapper<PetAutoFeedConfig> {
}

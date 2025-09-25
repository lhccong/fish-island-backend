package com.cong.fishisland.mapper.pet;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.cong.fishisland.model.entity.pet.FishPet;
import com.cong.fishisland.model.vo.pet.PetRankVO;
import org.apache.ibatis.annotations.MapKey;
import org.apache.ibatis.annotations.Param;

import java.util.List;
import java.util.Map;

/**
 * 宠物数据库操作
 *
 * @author cong
 */
public interface FishPetMapper extends BaseMapper<FishPet> {
    
    /**
     * 批量更新宠物状态
     * 30级宠物会保持饥饿度为100和心情值为100（满心情）
     * 
     * @param hungerDecrement 饥饿度减少值
     * @param moodDecrement 心情值减少值
     * @return 更新的记录数
     */
    int batchUpdatePetStatus(@Param("hungerDecrement") int hungerDecrement, @Param("moodDecrement") int moodDecrement);
    
    /**
     * 批量更新在线用户宠物经验
     * 经验值满100时，等级加1，经验值清零
     * 当宠物升级到30级时，经验值、饥饿度和心情值会自动回满
     *
     * @param userIds 在线用户ID列表
     * @return 更新的记录数
     */
    int batchUpdateOnlineUserPetExp(@Param("userIds") List<String> userIds);
    
    /**
     * 获取所有符合条件的宠物及其用户ID和等级
     * 条件：饥饿度和心情值都大于0
     *
     * @return 宠物列表，包含用户ID和宠物等级
     */
    @MapKey("petId")
    List<Map<String, Object>> getPetsForDailyPoints();
    
    /**
     * 获取宠物排行榜数据
     * 按照宠物等级和经验值排序
     *
     * @param limit 获取数量
     * @return 宠物排行榜数据
     */
    List<PetRankVO> getPetRankList(@Param("limit") int limit);
} 
package com.cong.fishisland.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.cong.fishisland.model.dto.pet.CreatePetRequest;
import com.cong.fishisland.model.dto.pet.UpdatePetNameRequest;
import com.cong.fishisland.model.entity.pet.FishPet;
import com.cong.fishisland.model.vo.pet.OtherUserPetVO;
import com.cong.fishisland.model.vo.pet.PetRankVO;
import com.cong.fishisland.model.vo.pet.PetSkinVO;
import com.cong.fishisland.model.vo.pet.PetVO;

import java.util.List;

/**
 * 宠物服务接口
 *
 * @author cong
 */
public interface FishPetService extends IService<FishPet> {

    /**
     * 创建宠物
     *
     * @param createPetRequest 创建宠物请求
     * @return 宠物ID
     */
    Long createPet(CreatePetRequest createPetRequest);

    /**
     * 获取宠物详情
     *
     * @return 宠物详情
     */
    PetVO getPetDetail();

    /**
     * 修改宠物名称
     *
     * @param updatePetNameRequest 修改宠物名称请求
     * @param userId 用户ID
     * @return 是否成功
     */
    boolean updatePetName(UpdatePetNameRequest updatePetNameRequest, Long userId);
    
    /**
     * 查看其他用户的宠物（不返回扩展数据）
     *
     * @param otherUserId 其他用户ID
     * @return 宠物详情（不包含扩展数据）
     */
    OtherUserPetVO getOtherUserPet(Long otherUserId);
    
    /**
     * 喂食宠物
     * 
     * @param petId 宠物ID
     * @return 更新后的宠物详情
     */
    PetVO feedPet(Long petId);
    
    /**
     * 抚摸宠物
     * 
     * @param petId 宠物ID
     * @return 更新后的宠物详情
     */
    PetVO patPet(Long petId);
    
    /**
     * 批量更新宠物状态
     * 
     * @param hungerDecrement 饥饿度减少值
     * @param moodDecrement 心情值减少值
     * @return 更新的记录数
     */
    int batchUpdatePetStatus(int hungerDecrement, int moodDecrement);
    
    /**
     * 批量更新在线用户宠物经验
     * 经验值满100时，等级加1，经验值清零
     * 注意：只有当宠物的饥饿度和心情值都大于0时，才会更新经验
     *
     * @param userIds 在线用户ID列表
     * @return 更新的记录数
     */
    int batchUpdateOnlineUserPetExp(List<String> userIds);
    
    /**
     * 宠物每日产出积分
     * 产出积分 = 宠物等级（最高不超过maxPoints）
     * 注意：只有当宠物的饥饿度和心情值都大于0时，才会产出积分
     *
     * @param maxPoints 最大积分限制
     * @return 产出积分的宠物数量
     */
    int generateDailyPetPoints(int maxPoints);
    
    /**
     * 获取宠物拥有的皮肤列表
     *
     * @param petId 宠物ID
     * @return 皮肤列表
     */
    List<PetSkinVO> getPetSkins(Long petId);
    
    /**
     * 生成宠物排行榜并缓存到Redis
     * 
     * @return 生成的排行榜数量
     */
    int generatePetRankList();
    
    /**
     * 获取宠物排行榜
     * 
     * @param limit 获取数量，默认为前10名
     * @return 宠物排行榜列表
     */
    List<PetRankVO> getPetRankList(int limit);

    /**
     * 更新用户宠物称号
     * 移除昨天排行榜用户的宠物称号，给今天排行榜用户添加宠物称号
     * 
     * @return 更新的用户数量
     */
    int updatePetRankTitles();
} 
package com.cong.fishisland.service;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.IService;
import com.cong.fishisland.model.dto.pet.PetSkinExchangeRequest;
import com.cong.fishisland.model.dto.pet.PetSkinQueryRequest;
import com.cong.fishisland.model.dto.pet.PetSkinSetRequest;
import com.cong.fishisland.model.entity.pet.PetSkin;
import com.cong.fishisland.model.vo.pet.PetSkinVO;
import com.cong.fishisland.model.vo.pet.PetVO;

import java.util.List;

/**
 * 宠物皮肤服务接口
 *
 * @author cong
 */
public interface PetSkinService extends IService<PetSkin> {

    /**
     * 分页查询宠物皮肤
     *
     * @param petSkinQueryRequest 查询请求
     * @param userId 当前用户ID
     * @return 宠物皮肤分页结果
     */
    Page<PetSkinVO> queryPetSkinByPage(PetSkinQueryRequest petSkinQueryRequest, Long userId);
    
    /**
     * 兑换宠物皮肤
     *
     * @param petSkinExchangeRequest 兑换请求
     * @param userId 用户ID
     * @return 是否兑换成功
     */
    boolean exchangePetSkin(PetSkinExchangeRequest petSkinExchangeRequest, Long userId);
    
    /**
     * 设置宠物皮肤
     *
     * @param petSkinSetRequest 设置请求
     * @param userId 用户ID
     * @return 更新后的宠物信息
     */
    PetVO setPetSkin(PetSkinSetRequest petSkinSetRequest, Long userId);
    
    /**
     * 根据皮肤ID列表获取皮肤信息
     *
     * @param skinIds 皮肤ID列表
     * @return 皮肤信息列表
     */
    List<PetSkinVO> getPetSkinsByIds(List<Long> skinIds);
} 
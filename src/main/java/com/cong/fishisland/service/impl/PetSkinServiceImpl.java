package com.cong.fishisland.service.impl;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.cong.fishisland.common.ErrorCode;
import com.cong.fishisland.common.exception.BusinessException;
import com.cong.fishisland.mapper.pet.FishPetMapper;
import com.cong.fishisland.mapper.pet.PetSkinMapper;
import com.cong.fishisland.model.dto.pet.PetSkinExchangeRequest;
import com.cong.fishisland.model.dto.pet.PetSkinQueryRequest;
import com.cong.fishisland.model.dto.pet.PetSkinSetRequest;
import com.cong.fishisland.model.entity.pet.FishPet;
import com.cong.fishisland.model.entity.pet.PetSkin;
import com.cong.fishisland.model.entity.user.UserPoints;
import com.cong.fishisland.model.vo.pet.PetSkinVO;
import com.cong.fishisland.model.vo.pet.PetVO;
import com.cong.fishisland.service.FishPetService;
import com.cong.fishisland.service.PetSkinService;
import com.cong.fishisland.service.UserPointsService;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.annotation.Resource;
import java.util.*;
import java.util.stream.Collectors;

/**
 * 宠物皮肤服务实现类
 *
 * @author cong
 */
@Service
public class PetSkinServiceImpl extends ServiceImpl<PetSkinMapper, PetSkin> implements PetSkinService {

    @Resource
    private FishPetMapper fishPetMapper;

    @Resource
    private UserPointsService userPointsService;
    
    @Override
    public Page<PetSkinVO> queryPetSkinByPage(PetSkinQueryRequest petSkinQueryRequest, Long userId) {
        // 查询条件
        LambdaQueryWrapper<PetSkin> queryWrapper = new LambdaQueryWrapper<>();
        if (StringUtils.isNotBlank(petSkinQueryRequest.getName())) {
            queryWrapper.like(PetSkin::getName, petSkinQueryRequest.getName());
        }
        queryWrapper.orderByAsc(PetSkin::getPoints);
        
        // 分页查询
        Page<PetSkin> page = new Page<>(petSkinQueryRequest.getCurrent(), petSkinQueryRequest.getPageSize());
        Page<PetSkin> petSkinPage = this.page(page, queryWrapper);
        
        // 获取用户宠物信息，查看已拥有的皮肤
        FishPet fishPet = fishPetMapper.selectOne(new LambdaQueryWrapper<FishPet>()
                .eq(FishPet::getUserId, userId));
        
        Set<Long> ownedSkinIds = new HashSet<>();
        if (fishPet != null && StringUtils.isNotBlank(fishPet.getExtendData())) {
            JSONObject extendData = JSON.parseObject(fishPet.getExtendData());
            if (extendData.containsKey("skinIds")) {
                List<Long> skinIds = JSON.parseArray(extendData.getString("skinIds"), Long.class);
                ownedSkinIds.addAll(skinIds);
            }
        }
        
        // 转换为VO
        Page<PetSkinVO> petSkinVOPage = new Page<>(petSkinPage.getCurrent(), petSkinPage.getSize(), petSkinPage.getTotal());
        List<PetSkinVO> petSkinVOList = petSkinPage.getRecords().stream().map(petSkin -> {
            PetSkinVO petSkinVO = new PetSkinVO();
            BeanUtils.copyProperties(petSkin, petSkinVO);
            // 设置是否已拥有
            petSkinVO.setOwned(ownedSkinIds.contains(petSkin.getSkinId()));
            return petSkinVO;
        }).collect(Collectors.toList());
        
        petSkinVOPage.setRecords(petSkinVOList);
        return petSkinVOPage;
    }
    
    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean exchangePetSkin(PetSkinExchangeRequest petSkinExchangeRequest, Long userId) {
        Long skinId = petSkinExchangeRequest.getSkinId();
        
        // 查询皮肤信息
        PetSkin petSkin = this.getById(skinId);
        if (petSkin == null) {
            throw new BusinessException(ErrorCode.NOT_FOUND_ERROR, "皮肤不存在");
        }
        
        // 查询用户宠物
        FishPet fishPet = fishPetMapper.selectOne(new LambdaQueryWrapper<FishPet>()
                .eq(FishPet::getUserId, userId));
        if (fishPet == null) {
            throw new BusinessException(ErrorCode.NOT_FOUND_ERROR, "请先创建宠物");
        }
        
        // 检查是否已拥有该皮肤
        Set<Long> ownedSkinIds = new HashSet<>();
        JSONObject extendData = new JSONObject();
        if (StringUtils.isNotBlank(fishPet.getExtendData())) {
            extendData = JSON.parseObject(fishPet.getExtendData());
            if (extendData.containsKey("skinIds")) {
                List<Long> skinIds = JSON.parseArray(extendData.getString("skinIds"), Long.class);
                ownedSkinIds.addAll(skinIds);
                if (ownedSkinIds.contains(skinId)) {
                    throw new BusinessException(ErrorCode.PARAMS_ERROR, "已拥有该皮肤");
                }
            }
        }
        
        // 扣除积分
        userPointsService.deductPoints(userId, petSkin.getPoints());
        
        // 更新宠物扩展数据，添加皮肤ID
        ownedSkinIds.add(skinId);
        extendData.put("skinIds", ownedSkinIds);
        fishPet.setExtendData(extendData.toJSONString());
        fishPetMapper.updateById(fishPet);
        
        return true;
    }
    
    @Override
    public PetVO setPetSkin(PetSkinSetRequest petSkinSetRequest, Long userId) {
        Long skinId = petSkinSetRequest.getSkinId();
        
        // 查询用户宠物
        FishPet fishPet = fishPetMapper.selectOne(new LambdaQueryWrapper<FishPet>()
                .eq(FishPet::getUserId, userId));
        if (fishPet == null) {
            throw new BusinessException(ErrorCode.NOT_FOUND_ERROR, "请先创建宠物");
        }
        
        // 如果skinId为-1，则恢复原皮
        if (skinId != null && skinId == -1L) {
            // 默认原皮的图片地址，可以根据实际情况设置
            String defaultPetUrl = "https://api.oss.cqbo.com/moyu/pet/超级玛丽马里奥 (73)_爱给网_aigei_com.png";
            fishPet.setPetUrl(defaultPetUrl);
            fishPetMapper.updateById(fishPet);
            
            // 返回更新后的宠物信息
            PetVO petVO = new PetVO();
            BeanUtils.copyProperties(fishPet, petVO);
            return petVO;
        }
        
        // 查询皮肤信息
        PetSkin petSkin = this.getById(skinId);
        if (petSkin == null) {
            throw new BusinessException(ErrorCode.NOT_FOUND_ERROR, "皮肤不存在");
        }
        
        // 检查是否已拥有该皮肤
        boolean hasSkin = false;
        if (StringUtils.isNotBlank(fishPet.getExtendData())) {
            JSONObject extendData = JSON.parseObject(fishPet.getExtendData());
            if (extendData.containsKey("skinIds")) {
                List<Long> skinIds = JSON.parseArray(extendData.getString("skinIds"), Long.class);
                hasSkin = skinIds.contains(skinId);
            }
        }
        
        if (!hasSkin) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "未拥有该皮肤，请先兑换");
        }
        
        // 更新宠物图片地址
        fishPet.setPetUrl(petSkin.getUrl());
        fishPetMapper.updateById(fishPet);
        
        // 返回更新后的宠物信息
        PetVO petVO = new PetVO();
        BeanUtils.copyProperties(fishPet, petVO);
        return petVO;
    }
    
    @Override
    public List<PetSkinVO> getPetSkinsByIds(List<Long> skinIds) {
        if (skinIds == null || skinIds.isEmpty()) {
            return new ArrayList<>();
        }
        
        // 查询皮肤信息
        List<PetSkin> petSkins = this.list(new LambdaQueryWrapper<PetSkin>()
                .in(PetSkin::getSkinId, skinIds));
        
        // 转换为VO
        return petSkins.stream().map(petSkin -> {
            PetSkinVO petSkinVO = new PetSkinVO();
            BeanUtils.copyProperties(petSkin, petSkinVO);
            petSkinVO.setOwned(true); // 这里是根据ID查询的，所以都是已拥有的
            return petSkinVO;
        }).collect(Collectors.toList());
    }
} 
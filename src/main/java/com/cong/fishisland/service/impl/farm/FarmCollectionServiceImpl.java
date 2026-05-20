package com.cong.fishisland.service.impl.farm;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.cong.fishisland.mapper.farm.FarmCollectionMapper;
import com.cong.fishisland.model.dto.farm.CollectionDTO;
import com.cong.fishisland.model.dto.farm.CollectionStatsVO;
import com.cong.fishisland.model.entity.farm.FarmCollection;
import com.cong.fishisland.model.entity.farm.FarmCrop;
import com.cong.fishisland.model.enums.farm.FarmYesNoEnum;
import com.cong.fishisland.service.FarmCollectionService;
import com.cong.fishisland.service.FarmCropService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class FarmCollectionServiceImpl extends ServiceImpl<FarmCollectionMapper, FarmCollection> implements FarmCollectionService {

    @Autowired
    private FarmCropService cropService;

    @Override
    public List<FarmCollection> getUserCollections(Long userId) {
        return list(new LambdaQueryWrapper<FarmCollection>()
                .eq(FarmCollection::getUserId, userId));
    }

    @Override
    public void updateCollection(Long userId, Long cropId) {
        FarmCollection collection = getOne(new LambdaQueryWrapper<FarmCollection>()
                .eq(FarmCollection::getUserId, userId)
                .eq(FarmCollection::getCropId, cropId)
                .last("LIMIT 1"));

        if (collection == null) {
            collection = new FarmCollection();
            collection.setUserId(userId);
            collection.setCropId(cropId);
            collection.setObtained(FarmYesNoEnum.YES.getValue());
            collection.setObtainedTime(LocalDateTime.now());
            collection.setCount(1);
            collection.setCreateTime(LocalDateTime.now());
            collection.setUpdateTime(LocalDateTime.now());
            save(collection);
        } else {
            if (FarmYesNoEnum.isNo(collection.getObtained())) {
                collection.setObtained(FarmYesNoEnum.YES.getValue());
                collection.setObtainedTime(LocalDateTime.now());
            }
            collection.setCount(collection.getCount() + 1);
            collection.setUpdateTime(LocalDateTime.now());
            updateById(collection);
        }
    }

    @Override
    public long getObtainedCount(Long userId) {
        return count(new LambdaQueryWrapper<FarmCollection>()
                .eq(FarmCollection::getUserId, userId)
                .eq(FarmCollection::getObtained, FarmYesNoEnum.YES.getValue()));
    }

    @Override
    public void initCollections(Long userId) {
        if (count(new LambdaQueryWrapper<FarmCollection>()
                .eq(FarmCollection::getUserId, userId)
                .eq(FarmCollection::getObtained, FarmYesNoEnum.NO.getValue())) == 0) {
            List<FarmCollection> all = list();
            LocalDateTime now = LocalDateTime.now();
            List<FarmCollection> toInsert = new ArrayList<>();
            for (FarmCollection c : all) {
                if (!c.getUserId().equals(userId)) {
                    FarmCollection newCol = new FarmCollection();
                    newCol.setUserId(userId);
                    newCol.setCropId(c.getCropId());
                    newCol.setObtained(FarmYesNoEnum.NO.getValue());
                    newCol.setCreateTime(now);
                    newCol.setUpdateTime(now);
                    toInsert.add(newCol);
                }
            }
            if (!toInsert.isEmpty()) {
                this.saveBatch(toInsert);
            }
        }
    }

    @Override
    public CollectionDTO toDTO(FarmCollection collection) {
        if (collection == null) {
            return null;
        }
        CollectionDTO dto = new CollectionDTO();
        dto.setId(collection.getId());
        dto.setCropId(collection.getCropId());
        dto.setObtained(collection.getObtained());
        dto.setCount(collection.getCount());
        dto.setObtainedTime(collection.getObtainedTime());

        FarmCrop crop = cropService.getCropById(collection.getCropId());
        if (crop != null) {
            dto.setCropName(crop.getName());
            dto.setCategory(crop.getCategory());
        }
        return dto;
    }

    @Override
    public List<CollectionDTO> toDTOList(List<FarmCollection> collections) {
        if (collections == null || collections.isEmpty()) {
            return Collections.emptyList();
        }
        return collections.stream().map(this::toDTO).collect(Collectors.toList());
    }

    @Override
    public CollectionStatsVO getCollectionStats(Long userId) {
        long obtained = getObtainedCount(userId);
        long total = cropService.getAllCrops().size();
        CollectionStatsVO stats = new CollectionStatsVO();
        stats.setObtained(obtained);
        stats.setTotal(total);
        stats.setProgress(total > 0 ? (obtained * 100 / total) : 0);
        return stats;
    }
}

package com.cong.fishisland.service.impl.farm;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.cong.fishisland.mapper.farm.FarmCollectionMapper;
import com.cong.fishisland.model.entity.farm.FarmCollection;
import com.cong.fishisland.service.FarmCollectionService;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Service
public class FarmCollectionServiceImpl extends ServiceImpl<FarmCollectionMapper, FarmCollection> implements FarmCollectionService {

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
            collection.setObtained(1);
            collection.setObtainedTime(LocalDateTime.now());
            collection.setCount(1);
            collection.setCreatedAt(LocalDateTime.now());
            collection.setUpdatedAt(LocalDateTime.now());
            save(collection);
        } else {
            if (collection.getObtained() == 0) {
                collection.setObtained(1);
                collection.setObtainedTime(LocalDateTime.now());
            }
            collection.setCount(collection.getCount() + 1);
            collection.setUpdatedAt(LocalDateTime.now());
            updateById(collection);
        }
    }

    @Override
    public long getObtainedCount(Long userId) {
        return count(new LambdaQueryWrapper<FarmCollection>()
                .eq(FarmCollection::getUserId, userId)
                .eq(FarmCollection::getObtained, 1));
    }

    @Override
    public void initCollections(Long userId) {
        if (count(new LambdaQueryWrapper<FarmCollection>()
                .eq(FarmCollection::getUserId, userId)
                .eq(FarmCollection::getObtained, 0)) == 0) {
            List<FarmCollection> all = list();
            LocalDateTime now = LocalDateTime.now();
            List<FarmCollection> toInsert = new ArrayList<>();
            for (FarmCollection c : all) {
                if (!c.getUserId().equals(userId)) {
                    FarmCollection newCol = new FarmCollection();
                    newCol.setUserId(userId);
                    newCol.setCropId(c.getCropId());
                    newCol.setObtained(0);
                    newCol.setCreatedAt(now);
                    newCol.setUpdatedAt(now);
                    toInsert.add(newCol);
                }
            }
            if (!toInsert.isEmpty()) {
                this.saveBatch(toInsert);
            }
        }
    }
}

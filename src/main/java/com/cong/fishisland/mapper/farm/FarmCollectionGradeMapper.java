package com.cong.fishisland.mapper.farm;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.cong.fishisland.model.entity.farm.FarmCollectionGrade;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

@Mapper
public interface FarmCollectionGradeMapper extends BaseMapper<FarmCollectionGrade> {

    /**
     * 图鉴 upsert：同用户同作物同品级仅一条，仅当新重量更大时刷新最大重量。
     * 首次解锁时间不随重量刷新变化。
     *
     * @return 影响行数：1-新增或刷新，2-重复键且未刷新（MySQL upsert 为 2 时表示仅冲突未变更），0-异常
     */
    @Insert("INSERT INTO farm_collection_grade (userId, cropId, grade, maxWeight, firstObtainedTime, createTime) "
            + "VALUES (#{userId}, #{cropId}, #{grade}, #{maxWeight}, NOW(), NOW()) "
            + "ON DUPLICATE KEY UPDATE maxWeight = IF(VALUES(maxWeight) > maxWeight, VALUES(maxWeight), maxWeight)")
    int upsertOnHeavier(@Param("userId") Long userId,
                        @Param("cropId") Long cropId,
                        @Param("grade") Integer grade,
                        @Param("maxWeight") Integer maxWeight);
}

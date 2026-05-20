package com.cong.fishisland.mapper.farm;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.cong.fishisland.model.dto.farm.FarmStealRecordVO;
import com.cong.fishisland.model.entity.farm.FarmStealRecord;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

@Mapper
public interface FarmStealRecordMapper extends BaseMapper<FarmStealRecord> {

    List<FarmStealRecordVO> selectStealRecordsWithStealerInfo(@Param("ownerId") Long ownerId);
}

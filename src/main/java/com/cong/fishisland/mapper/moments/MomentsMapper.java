package com.cong.fishisland.mapper.moments;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.cong.fishisland.model.entity.moments.Moments;
import org.apache.ibatis.annotations.Mapper;

/**
 * 朋友圈动态 Mapper
 *
 * @author cong
 */
@Mapper
public interface MomentsMapper extends BaseMapper<Moments> {
}

package com.cong.fishisland.mapper.moments;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.cong.fishisland.model.entity.moments.MomentsLike;
import org.apache.ibatis.annotations.Mapper;

/**
 * 朋友圈点赞 Mapper
 *
 * @author cong
 */
@Mapper
public interface MomentsLikeMapper extends BaseMapper<MomentsLike> {
}

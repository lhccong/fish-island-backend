package com.cong.fishisland.mapper.moments;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.cong.fishisland.model.entity.moments.MomentsComment;
import org.apache.ibatis.annotations.Mapper;

/**
 * 朋友圈评论 Mapper
 *
 * @author cong
 */
@Mapper
public interface MomentsCommentMapper extends BaseMapper<MomentsComment> {
}

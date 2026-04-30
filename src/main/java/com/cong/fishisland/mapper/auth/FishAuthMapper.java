package com.cong.fishisland.mapper.auth;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.cong.fishisland.model.entity.auth.FishAuth;
import org.apache.ibatis.annotations.Mapper;

/**
 * 第三方应用 Mapper
 *
 * @author cong
 */
@Mapper
public interface FishAuthMapper extends BaseMapper<FishAuth> {
}

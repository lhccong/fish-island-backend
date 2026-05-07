package com.cong.fishisland.mapper.auth;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.cong.fishisland.model.entity.auth.FishAuthCode;
import org.apache.ibatis.annotations.Mapper;

/**
 * OAuth2 授权码 Mapper
 *
 * @author cong
 */
@Mapper
public interface FishAuthCodeMapper extends BaseMapper<FishAuthCode> {
}

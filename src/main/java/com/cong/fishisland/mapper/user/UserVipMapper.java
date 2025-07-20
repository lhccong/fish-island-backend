package com.cong.fishisland.mapper.user;

import com.cong.fishisland.model.entity.user.UserVip;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

/**
 * @author cong
 * @description 针对表【user_vip(用户会员表)】的数据库操作Mapper
 * @createDate 2025-05-01 10:00:00
 * @Entity com.cong.fishisland.model.entity.user.UserVip
 */
public interface UserVipMapper extends BaseMapper<UserVip> {
    
    /**
     * 根据用户ID查询会员信息，并加锁（悲观锁）
     */
    @Select("SELECT * FROM user_vip WHERE userId = #{userId} AND isDelete = 0 FOR UPDATE")
    UserVip selectByUserIdForUpdate(@Param("userId") Long userId);
    
    /**
     * 查询即将过期的会员（7天内过期）
     */
    @Select("SELECT * FROM user_vip WHERE validDays BETWEEN NOW() AND DATE_ADD(NOW(), INTERVAL 7 DAY) AND isDelete = 0")
    UserVip[] selectExpiringVips();
} 
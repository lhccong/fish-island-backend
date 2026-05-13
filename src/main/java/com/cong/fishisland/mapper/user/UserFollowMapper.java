package com.cong.fishisland.mapper.user;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.cong.fishisland.model.entity.user.UserFollow;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.Collection;
import java.util.List;
import java.util.Map;

/**
 * 用户关注 Mapper
 *
 * @author <a href="https://github.com/lhccong">程序员聪</a>
 */
public interface UserFollowMapper extends BaseMapper<UserFollow> {

    /**
     * 统计单个用户的关注数
     */
    @Select("SELECT COUNT(*) FROM user_follow WHERE userId = #{userId}")
    long countFollowing(@Param("userId") Long userId);

    /**
     * 统计单个用户的粉丝数
     */
    @Select("SELECT COUNT(*) FROM user_follow WHERE followUserId = #{userId}")
    long countFollowers(@Param("userId") Long userId);

    /**
     * 批量统计多个用户各自的关注数
     *
     * @return List of map with keys: userId, cnt
     */
    @Select("<script>" +
            "SELECT userId, COUNT(*) AS cnt FROM user_follow " +
            "WHERE userId IN " +
            "<foreach item='id' collection='userIds' open='(' separator=',' close=')'> #{id} </foreach>" +
            " GROUP BY userId" +
            "</script>")
    List<Map<String, Object>> batchCountFollowing(@Param("userIds") Collection<Long> userIds);

    /**
     * 批量统计多个用户各自的粉丝数
     *
     * @return List of map with keys: followUserId, cnt
     */
    @Select("<script>" +
            "SELECT followUserId, COUNT(*) AS cnt FROM user_follow " +
            "WHERE followUserId IN " +
            "<foreach item='id' collection='userIds' open='(' separator=',' close=')'> #{id} </foreach>" +
            " GROUP BY followUserId" +
            "</script>")
    List<Map<String, Object>> batchCountFollowers(@Param("userIds") Collection<Long> userIds);
}

package com.cong.fishisland.mapper.moments;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.cong.fishisland.model.entity.moments.MomentsComment;
import org.apache.ibatis.annotations.Delete;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.Collection;
import java.util.List;

/**
 * 朋友圈评论 Mapper
 *
 * @author cong
 */
@Mapper
public interface MomentsCommentMapper extends BaseMapper<MomentsComment> {

    @Select({"<script>",
            "SELECT commentId FROM moments_comment_like WHERE userId = #{userId} AND commentId IN",
            "<foreach collection='ids' item='id' open='(' separator=',' close=')'>#{id}</foreach>",
            "</script>"})
    List<Long> selectLikedCommentIds(@Param("userId") long userId, @Param("ids") Collection<Long> ids);

    @Insert("INSERT INTO moments_comment_like (commentId, userId) VALUES (#{commentId}, #{userId})")
    int insertLike(@Param("commentId") long commentId, @Param("userId") long userId);

    @Delete("DELETE FROM moments_comment_like WHERE commentId = #{commentId} AND userId = #{userId}")
    int deleteLike(@Param("commentId") long commentId, @Param("userId") long userId);
}

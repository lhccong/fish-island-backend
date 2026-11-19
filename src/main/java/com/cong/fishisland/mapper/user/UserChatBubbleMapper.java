package com.cong.fishisland.mapper.user;

import com.cong.fishisland.model.entity.user.UserChatBubble;
import com.cong.fishisland.model.entity.user.ChatBubbleProduct;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

import java.util.List;

public interface UserChatBubbleMapper {
    @Select("SELECT bubble_id AS bubbleId, expires_at AS expiresAt, equipped FROM user_chat_bubble WHERE user_id = #{userId}")
    List<UserChatBubble> findAll(@Param("userId") long userId);

    @Select("SELECT expires_at FROM user_chat_bubble WHERE user_id = #{userId} AND bubble_id = #{bubbleId}")
    Long findExpiry(@Param("userId") long userId, @Param("bubbleId") String bubbleId);

    @Select("SELECT " + ChatBubbleProductMapper.COLUMNS + " FROM user_chat_bubble grant_row "
            + "JOIN chat_bubble_product product ON product.id = grant_row.bubble_id "
            + "WHERE grant_row.user_id = #{userId} AND grant_row.equipped = 1 AND grant_row.expires_at > #{now}")
    ChatBubbleProduct findActive(@Param("userId") long userId, @Param("now") long now);

    @Insert("INSERT INTO user_chat_bubble (user_id, bubble_id, expires_at) VALUES (#{userId}, #{bubbleId}, #{expiresAt}) "
            + "ON DUPLICATE KEY UPDATE expires_at = #{expiresAt}, equipped = NULL")
    int grant(@Param("userId") long userId, @Param("bubbleId") String bubbleId, @Param("expiresAt") long expiresAt);

    @Update("UPDATE user_chat_bubble SET equipped = NULL WHERE user_id = #{userId} AND equipped = 1")
    int clearSelection(@Param("userId") long userId);

    @Update("UPDATE user_chat_bubble SET equipped = 1 WHERE user_id = #{userId} AND bubble_id = #{bubbleId} AND expires_at > #{now}")
    int select(@Param("userId") long userId, @Param("bubbleId") String bubbleId, @Param("now") long now);
}

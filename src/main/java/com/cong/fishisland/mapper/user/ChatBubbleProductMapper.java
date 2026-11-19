package com.cong.fishisland.mapper.user;

import com.cong.fishisland.model.entity.user.ChatBubbleProduct;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

import java.util.List;

public interface ChatBubbleProductMapper {
    String COLUMNS = "id, name, points, image_url AS imageUrl, dark_image_url AS darkImageUrl, text_tone AS textTone";

    @Select("SELECT " + COLUMNS + " FROM chat_bubble_product ORDER BY sort_order, id")
    List<ChatBubbleProduct> list();

    @Select("SELECT " + COLUMNS + " FROM chat_bubble_product WHERE id = #{id}")
    ChatBubbleProduct find(@Param("id") String id);

    @Select("SELECT " + COLUMNS + " FROM chat_bubble_product WHERE id = #{id} LOCK IN SHARE MODE")
    ChatBubbleProduct findForExchange(@Param("id") String id);

    @Insert("INSERT INTO chat_bubble_product (id, name, points, image_url, dark_image_url, text_tone) "
            + "VALUES (#{id}, #{name}, #{points}, #{imageUrl}, #{darkImageUrl}, #{textTone})")
    int create(ChatBubbleProduct product);

    @Update("UPDATE chat_bubble_product SET points = #{points} WHERE id = #{id}")
    int setPrice(@Param("id") String id, @Param("points") int points);
}

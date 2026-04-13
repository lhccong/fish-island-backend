package com.cong.fishisland.mapper.chat;

import com.cong.fishisland.model.entity.chat.RoomMessage;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Delete;
import org.apache.ibatis.annotations.Param;

/**
* @author cong
* @description 针对表【room_message(房间消息表)】的数据库操作Mapper
* @createDate 2025-03-09 11:14:07
* @Entity com.cong.fishisland.model.entity.chat.RoomMessage
*/
public interface RoomMessageMapper extends BaseMapper<RoomMessage> {

    /**
     * 按 id 物理删除（绕过逻辑删除）
     */
    @Delete("DELETE FROM room_message WHERE id = #{id}")
    int physicalDeleteById(@Param("id") Long id);
}





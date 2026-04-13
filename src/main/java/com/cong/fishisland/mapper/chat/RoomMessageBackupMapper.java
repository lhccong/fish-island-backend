package com.cong.fishisland.mapper.chat;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.cong.fishisland.model.entity.chat.RoomMessage;
import com.cong.fishisland.model.entity.chat.RoomMessageBackup;
import org.apache.ibatis.annotations.Delete;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.Date;
import java.util.List;

/**
 * 聊天记录备份表 Mapper
 *
 * @author cong
 */
public interface RoomMessageBackupMapper extends BaseMapper<RoomMessageBackup> {

    /**
     * 查询一周前的所有消息（绕过逻辑删除）
     */
    @Select("SELECT id, userId, roomId, messageJson, messageId, createTime, updateTime, isDelete " +
            "FROM room_message WHERE createTime < #{oneWeekAgo}")
    List<RoomMessage> selectOldMessages(@Param("oneWeekAgo") Date oneWeekAgo);

    /**
     * 物理删除一周前的消息
     */
    @Delete("DELETE FROM room_message WHERE createTime < #{oneWeekAgo}")
    int deleteOldMessages(@Param("oneWeekAgo") Date oneWeekAgo);
}

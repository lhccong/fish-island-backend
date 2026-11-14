package com.cong.fishisland.service.impl.chat;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.cong.fishisland.model.dto.chat.MessageQueryRequest;
import com.cong.fishisland.model.entity.chat.RoomMessage;
import com.cong.fishisland.model.vo.chat.RoomMessageVo;
import com.cong.fishisland.service.RoomMessageService;
import com.cong.fishisland.mapper.chat.RoomMessageMapper;
import com.cong.fishisland.common.ErrorCode;
import com.cong.fishisland.common.exception.BusinessException;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;

/**
 * @author cong
 * @description 针对表【room_message(房间消息表)】的数据库操作Service实现
 * @createDate 2025-03-09 11:14:07
 */
@Service
public class RoomMessageServiceImpl extends ServiceImpl<RoomMessageMapper, RoomMessage>
        implements RoomMessageService {

    @Override
    public Page<RoomMessageVo> listMessageVoByPage(MessageQueryRequest messageQueryRequest) {
        Long roomId = messageQueryRequest.getRoomId();
        int size = messageQueryRequest.getPageSize();
        if (roomId == null) {
            Page<RoomMessageVo> messageVoPage = new Page<>(0, size, 0);
            messageVoPage.setRecords(null);
            return messageVoPage;
        }

        String cursorMessageId = messageQueryRequest.getMessageId();
        if (cursorMessageId != null) {
            return listMessageVoByCursor(roomId, cursorMessageId, size);
        }

        int current = messageQueryRequest.getCurrent();
        Page<RoomMessage> messagePage = this.page(new Page<>(current, size),
                new LambdaQueryWrapper<RoomMessage>()
                        .eq(RoomMessage::getRoomId, roomId)
                        .orderByDesc(RoomMessage::getCreateTime, RoomMessage::getId));
        return buildMessageVoPage(messagePage.getRecords(), current, size, messagePage.getTotal());
    }

    /**
     * 游标分页：获取指定消息 ID 之前的历史消息
     */
    private Page<RoomMessageVo> listMessageVoByCursor(Long roomId, String cursorMessageId, int size) {
        if (cursorMessageId.trim().isEmpty() || cursorMessageId.length() > 128) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "历史消息游标无效，请刷新后重试");
        }
        RoomMessage anchor = this.getOne(new LambdaQueryWrapper<RoomMessage>()
                .eq(RoomMessage::getRoomId, roomId)
                .eq(RoomMessage::getMessageId, cursorMessageId)
                .orderByDesc(RoomMessage::getId)
                .last("LIMIT 1"));
        if (anchor == null || anchor.getCreateTime() == null) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "历史消息游标已失效，请刷新后重试");
        }
        Page<RoomMessage> messagePage = this.page(new Page<>(1, size, false),
                new LambdaQueryWrapper<RoomMessage>()
                        .eq(RoomMessage::getRoomId, roomId)
                        .and(older -> older.lt(RoomMessage::getCreateTime, anchor.getCreateTime())
                                .or(tie -> tie.eq(RoomMessage::getCreateTime, anchor.getCreateTime())
                                        .lt(RoomMessage::getId, anchor.getId())))
                        .orderByDesc(RoomMessage::getCreateTime, RoomMessage::getId));
        return buildMessageVoPage(messagePage.getRecords(), 1, size, 0);
    }

    private Page<RoomMessageVo> buildMessageVoPage(List<RoomMessage> messages, int current, int size, long total) {
        List<RoomMessageVo> chatMessageRespList = messages.stream()
                .map(item -> new RoomMessageVo().getVoByEntity(item))
                .collect(Collectors.toList());
        Page<RoomMessageVo> messageVoPage = new Page<>(current, size, total);
        messageVoPage.setRecords(chatMessageRespList);
        return messageVoPage;
    }
}





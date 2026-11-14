package com.cong.fishisland.service;

import com.baomidou.mybatisplus.core.MybatisConfiguration;
import com.baomidou.mybatisplus.core.conditions.Wrapper;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.TableInfoHelper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.cong.fishisland.common.exception.BusinessException;
import com.cong.fishisland.model.dto.chat.MessageQueryRequest;
import com.cong.fishisland.model.entity.chat.RoomMessage;
import com.cong.fishisland.service.impl.chat.RoomMessageServiceImpl;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.ibatis.builder.MapperBuilderAssistant;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import java.util.Collections;
import java.util.Date;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

class RoomMessageCursorTest {
    RoomMessageServiceImpl service;
    MessageQueryRequest input;

    @BeforeEach void setup() {
        TableInfoHelper.initTableInfo(new MapperBuilderAssistant(new MybatisConfiguration(), "cursor-test"), RoomMessage.class);
        service = spy(new RoomMessageServiceImpl());
        input = new MessageQueryRequest();
        input.setRoomId(-1L);
        input.setPageSize(50);
    }

    @Test @SuppressWarnings("unchecked") void locatesUuidAndLongNumericIdsThenPagesByTimeAndRowId() {
        for (String id : new String[]{"a01b23456789cdef0123456789abcdef", "9007199254740993"}) {
            RoomMessage anchor = new RoomMessage();
            anchor.setId(73L);
            anchor.setCreateTime(new Date(1000));
            doAnswer(call -> {
                LambdaQueryWrapper<RoomMessage> query = call.getArgument(0);
                assertTrue(query.getSqlSegment().contains("roomId"));
                assertTrue(query.getSqlSegment().contains("messageId ="));
                assertTrue(query.getParamNameValuePairs().containsValue(id));
                return anchor;
            }).when(service).getOne(any(Wrapper.class));
            doAnswer(call -> {
                LambdaQueryWrapper<RoomMessage> query = call.getArgument(1);
                String sql = query.getSqlSegment();
                assertTrue(sql.contains("createTime <"), sql);
                assertTrue(sql.contains("OR"), sql);
                assertTrue(sql.contains("createTime ="), sql);
                assertTrue(sql.contains("id <"), sql);
                assertTrue(sql.contains("ORDER BY createTime DESC,id DESC"), sql);
                assertFalse(sql.contains("messageId"), sql);
                assertTrue(query.getParamNameValuePairs().containsValue(73L));
                assertTrue(query.getParamNameValuePairs().containsValue(anchor.getCreateTime()));
                return new Page<RoomMessage>(1, 50).setRecords(Collections.emptyList());
            }).when(service).page(any(Page.class), any(Wrapper.class));
            input.setMessageId(id);
            assertTrue(service.listMessageVoByPage(input).getRecords().isEmpty());
        }
    }

    @Test @SuppressWarnings("unchecked") void initialPageUsesSameStableOrder() {
        doAnswer(call -> {
            LambdaQueryWrapper<RoomMessage> query = call.getArgument(1);
            assertTrue(query.getSqlSegment().contains("ORDER BY createTime DESC,id DESC"));
            return new Page<RoomMessage>(1, 50).setRecords(Collections.emptyList());
        }).when(service).page(any(Page.class), any(Wrapper.class));
        service.listMessageVoByPage(input);
        verify(service, never()).getOne(any(Wrapper.class));
    }

    @Test void invalidOrMissingCursorDoesNotPretendHistoryEnded() {
        input.setMessageId(" ");
        assertThrows(BusinessException.class, () -> service.listMessageVoByPage(input));
        doReturn(null).when(service).getOne(any(Wrapper.class));
        input.setMessageId("missing-system-message");
        assertThrows(BusinessException.class, () -> service.listMessageVoByPage(input));
        verify(service, never()).page(any(Page.class), any(Wrapper.class));
    }

    @Test void acceptsLegacyNumericAndSystemUuidRequestBodiesWithoutPrecisionLoss() throws Exception {
        ObjectMapper json = new ObjectMapper();
        assertEquals("9007199254740993", json.readValue("{\"messageId\":9007199254740993}", MessageQueryRequest.class).getMessageId());
        assertEquals("a01b23456789cdef0123456789abcdef", json.readValue("{\"messageId\":\"a01b23456789cdef0123456789abcdef\"}", MessageQueryRequest.class).getMessageId());
    }
}

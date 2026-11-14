package com.cong.fishisland.service;

import com.alibaba.fastjson.JSON;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.MybatisConfiguration;
import com.baomidou.mybatisplus.core.metadata.TableInfoHelper;
import com.cong.fishisland.common.exception.BusinessException;
import com.cong.fishisland.model.dto.chat.SendMessageRequest;
import com.cong.fishisland.model.entity.chat.RoomMessage;
import com.cong.fishisland.model.entity.user.User;
import com.cong.fishisland.model.entity.user.UserPoints;
import com.cong.fishisland.model.vo.IpLocationVO;
import com.cong.fishisland.model.vo.user.UserMuteVO;
import com.cong.fishisland.model.ws.request.Message;
import com.cong.fishisland.model.ws.request.MessageWrapper;
import com.cong.fishisland.model.ws.response.WSBaseResp;
import com.cong.fishisland.service.impl.chat.HttpChatMessageService;
import com.cong.fishisland.websocket.service.WebSocketService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.apache.ibatis.builder.MapperBuilderAssistant;
import org.mockito.ArgumentCaptor;
import org.redisson.api.*;
import org.springframework.context.ApplicationEventPublisher;
import toolgood.words.StringSearch;
import java.util.HashMap;
import java.util.Map;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;
import static org.mockito.ArgumentMatchers.*;

class HttpChatMessageServiceTest {
    UserService users;
    UserPointsService points;
    UserVipService vips;
    RoomMessageService messages;
    UserMuteService mutes;
    IpLocationService ips;
    WebSocketService socket;
    HttpChatMessageService service;
    User user;
    UserMuteVO mute;
    Map<String, String> cache;
    RoomMessage stored;

    @BeforeEach
    @SuppressWarnings("unchecked")
    void setup() {
        TableInfoHelper.initTableInfo(new MapperBuilderAssistant(new MybatisConfiguration(), "chat-send-test"), UserPoints.class);
        users=mock(UserService.class); messages=mock(RoomMessageService.class);
        mutes=mock(UserMuteService.class); ips=mock(IpLocationService.class);
        socket=mock(WebSocketService.class);
        vips=mock(UserVipService.class); points=mock(UserPointsService.class);
        RedissonClient redis=mock(RedissonClient.class);
        RLock lock=mock(RLock.class); when(redis.getLock(anyString())).thenReturn(lock);
        cache=new HashMap<>();
        when(redis.getBucket(anyString())).thenAnswer(call -> {
            String key=call.getArgument(0);
            RBucket<Object> bucket=mock(RBucket.class);
            when(bucket.get()).thenAnswer(x->cache.get(key));
            when(bucket.isExists()).thenAnswer(x->cache.containsKey(key));
            doAnswer(x->{ cache.put(key,(String)x.getArgument(0));return null; }).when(bucket).set(any(),anyLong(),any());
            return bucket;
        });
        RAtomicLong seq=mock(RAtomicLong.class);
        when(seq.compareAndSet(anyLong(),anyLong())).thenReturn(true);
        when(redis.getAtomicLong(anyString())).thenReturn(seq);
        StringSearch words=mock(StringSearch.class); when(words.Replace(anyString())).thenAnswer(c->c.getArgument(0));
        user=new User();user.setId(123L);user.setUserName("合成昵称");user.setUserRole("user");user.setTitleId(8L);user.setTitleIdList("[8]");user.setUserAvatar("https://synthetic.test/avatar.png");
        when(users.getLoginUser()).thenReturn(user);
        UserPoints profile=new UserPoints();profile.setLevel(6);profile.setPoints(321);
        when(points.getOne(any(LambdaQueryWrapper.class))).thenReturn(profile);when(vips.isUserVip(123L)).thenReturn(true);
        mute=new UserMuteVO();mute.setIsMuted(false);when(mutes.getUserMuteInfo(123L)).thenReturn(mute);
        IpLocationVO location=new IpLocationVO();location.setCountry("合成国家");location.setRegionName("合成地区");when(ips.getLocationByIp("192.0.2.1")).thenReturn(location);
        when(messages.save(any(RoomMessage.class))).thenAnswer(c->{stored=c.getArgument(0);return true;});
        service=new HttpChatMessageService(users,points,messages,mutes,vips,ips,socket,mock(ApplicationEventPublisher.class),words,redis);
    }
    SendMessageRequest input() {
        SendMessageRequest input=new SendMessageRequest(); input.setContent("合成正文");input.setClientMessageId("synthetic-client-id-123");return input;
    }
    @Test void canonicalIdentityPersistedBeforeBroadcastIncludingSelfAndRetry() {
        MessageWrapper result=service.send(input(),"192.0.2.1");
        assertEquals("123",result.getMessage().getSender().getId());
        assertEquals(6,result.getMessage().getSender().getLevel());
        assertEquals(321,result.getMessage().getSender().getPoints());
        verify(users,never()).getLoginUserVO(any());
        verify(points).getOne(any(LambdaQueryWrapper.class));
        verify(vips).isUserVip(123L);
        assertEquals(Long.valueOf(8),result.getMessage().getSender().getTitleId());
        assertEquals("合成地区",result.getMessage().getSender().getRegion());
        assertTrue(result.getMessage().getSender().isVip());
        assertNotNull(stored);
        ArgumentCaptor<WSBaseResp> capture=ArgumentCaptor.forClass(WSBaseResp.class);
        verify(socket).sendToAllOnline(capture.capture());
        assertSame(result,capture.getValue().getData());
        verify(socket,never()).sendToAllOnline(any(),any());
        org.mockito.InOrder order=inOrder(messages,socket);order.verify(messages).save(any());order.verify(socket).sendToAllOnline(any());
        when(messages.getOne(any(LambdaQueryWrapper.class))).thenReturn(stored);
        assertEquals(result.getMessage().getId(),service.send(input(),"192.0.2.1").getMessage().getId());
        verify(messages,times(1)).save(any());
        verify(points,times(1)).getOne(any(LambdaQueryWrapper.class));
        verify(vips,times(1)).isUserVip(123L);
    }
    @Test void missingPointsRetainDefaultsAndVipIsIndependent() {
        when(points.getOne(any(LambdaQueryWrapper.class))).thenReturn(null);
        MessageWrapper result=service.send(input(),"");
        assertEquals(1,result.getMessage().getSender().getLevel());
        assertEquals(0,result.getMessage().getSender().getPoints());
        assertTrue(result.getMessage().getSender().isVip());
        verify(users,never()).getLoginUserVO(any());
        verify(vips).isUserVip(123L);
    }
    @Test void nullPointFieldsRetainDefaultsAndNonVipIsPreserved() {
        when(points.getOne(any(LambdaQueryWrapper.class))).thenReturn(new UserPoints());
        when(vips.isUserVip(123L)).thenReturn(false);
        MessageWrapper result=service.send(input(),"");
        assertEquals(1,result.getMessage().getSender().getLevel());
        assertEquals(0,result.getMessage().getSender().getPoints());
        assertFalse(result.getMessage().getSender().isVip());
    }
    @Test @SuppressWarnings("unchecked") void pointsQueryOnlyReadsSenderFieldsForCurrentUser() {
        service.send(input(),"");
        ArgumentCaptor<LambdaQueryWrapper<UserPoints>> query=ArgumentCaptor.forClass(LambdaQueryWrapper.class);
        verify(points).getOne(query.capture());
        assertEquals("points,level",query.getValue().getSqlSelect());
        assertTrue(query.getValue().getSqlSegment().contains("userId ="));
        assertEquals(java.util.Collections.singletonList(123L),new java.util.ArrayList<>(query.getValue().getParamNameValuePairs().values()));
        verify(users,never()).getLoginUserVO(any());
    }
    @Test void validatesMuteEmptySizeBanAndLogin() {
        SendMessageRequest empty=input();empty.setContent("  ");assertThrows(BusinessException.class,()->service.send(empty,""));
        SendMessageRequest large=input();large.setContent(new String(new char[5001]).replace('\0','x'));assertThrows(BusinessException.class,()->service.send(large,""));
        mute.setIsMuted(true);assertThrows(BusinessException.class,()->service.send(input(),""));
        user.setUserRole("ban");assertThrows(BusinessException.class,()->service.send(input(),""));
        when(users.getLoginUser()).thenThrow(new BusinessException(com.cong.fishisland.common.ErrorCode.NOT_LOGIN_ERROR));
        assertThrows(BusinessException.class,()->service.send(input(),""));
        verify(messages,never()).save(any());verifyNoInteractions(socket);
    }
    @Test void quoteIsLoadedServerSideAndMissingQuoteRejected() {
        Message original=new Message();original.setId("42");original.setContent("合成引用");original.setQuotedMessage(new Message());
        MessageWrapper wrapper=new MessageWrapper();wrapper.setMessage(original);
        RoomMessage record=new RoomMessage();record.setMessageJson(JSON.toJSONString(wrapper));
        when(messages.getOne(any(LambdaQueryWrapper.class))).thenReturn(record);
        SendMessageRequest input=input();input.setQuotedMessageId("42");
        MessageWrapper sent=service.send(input,"");
        assertEquals("合成引用",sent.getMessage().getQuotedMessage().getContent());assertNull(sent.getMessage().getQuotedMessage().getQuotedMessage());
        assertEquals("未知地区",sent.getMessage().getSender().getRegion());
        when(messages.getOne(any(LambdaQueryWrapper.class))).thenReturn(null);
        input.setClientMessageId("synthetic-client-id-456");assertThrows(BusinessException.class,()->service.send(input,""));
    }
    @Test void failureDoesNotBroadcastAndDeletedReceiptCannotResurrect() {
        when(messages.save(any(RoomMessage.class))).thenReturn(false);
        assertThrows(BusinessException.class,()->service.send(input(),""));verifyNoInteractions(socket);
        when(messages.save(any(RoomMessage.class))).thenReturn(true);
        service.send(input(),"");
        assertThrows(BusinessException.class,()->service.send(input(),""));
        verify(socket,times(1)).sendToAllOnline(any());
    }
}

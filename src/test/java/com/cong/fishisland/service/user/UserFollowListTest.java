package com.cong.fishisland.service.user;

import cn.dev33.satoken.exception.NotLoginException;
import cn.dev33.satoken.stp.StpLogic;
import cn.dev33.satoken.stp.StpUtil;
import com.baomidou.mybatisplus.core.MybatisConfiguration;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.TableInfoHelper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.cong.fishisland.common.exception.BusinessException;
import com.cong.fishisland.controller.user.UserFollowController;
import com.cong.fishisland.mapper.user.UserFollowMapper;
import com.cong.fishisland.mapper.user.UserMapper;
import com.cong.fishisland.model.entity.user.User;
import com.cong.fishisland.model.entity.user.UserFollow;
import com.cong.fishisland.model.vo.user.UserFollowVO;
import com.cong.fishisland.model.vo.user.UserFollowRelationshipVO;
import com.cong.fishisland.service.UserFollowService;
import com.cong.fishisland.service.event.EventRemindHandler;
import com.cong.fishisland.service.impl.user.UserFollowServiceImpl;
import org.apache.ibatis.builder.MapperBuilderAssistant;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.util.Collections;
import java.util.Date;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;

class UserFollowListTest {
    private static final long OWNER = 90071992547409921L;
    private final StpLogic previous = StpUtil.getStpLogic();
    private final StpLogic login = mock(StpLogic.class);
    private final UserMapper users = mock(UserMapper.class);
    private final UserFollowMapper follows = mock(UserFollowMapper.class);
    private final EventRemindHandler reminders = mock(EventRemindHandler.class);
    private final UserFollowServiceImpl service = spy(new UserFollowServiceImpl());

    @BeforeEach
    void setup() {
        when(login.getLoginId()).thenReturn("123");
        when(login.getLoginIdAsLong()).thenReturn(123L);
        StpUtil.setStpLogic(login);
        ReflectionTestUtils.setField(service, "userMapper", users);
        ReflectionTestUtils.setField(service, "baseMapper", follows);
        ReflectionTestUtils.setField(service, "eventRemindHandler", reminders);
        TableInfoHelper.initTableInfo(new MapperBuilderAssistant(new MybatisConfiguration(), "test"), UserFollow.class);
    }

    @AfterEach
    void restoreLogin() {
        StpUtil.setStpLogic(previous);
    }

    @Test
    void routesPassTheExactOwnerAndDirection() throws Exception {
        UserFollowService mockService = mock(UserFollowService.class);
        UserFollowController endpoint = new UserFollowController();
        ReflectionTestUtils.setField(endpoint, "userFollowService", mockService);
        when(mockService.listUserFollows(eq(OWNER), anyBoolean(), eq(2), eq(20)))
                .thenReturn(new Page<>(2, 20, 0));
        for (String kind : new String[]{"following", "followers"}) {
            MockMvcBuilders.standaloneSetup(endpoint).build()
                    .perform(get("/api/follow/user/" + kind).contextPath("/api")
                            .param("userId", String.valueOf(OWNER)).param("current", "2").param("pageSize", "20"))
                    .andExpect(jsonPath("$.code").value(0));
        }
        verify(mockService).listUserFollows(OWNER, true, 2, 20);
        verify(mockService).listUserFollows(OWNER, false, 2, 20);
    }

    @Test
    void preservesTheBooleanRouteAndExposesBothDirectionsOnRequest() throws Exception {
        UserFollowService mockService = mock(UserFollowService.class);
        UserFollowController endpoint = new UserFollowController();
        ReflectionTestUtils.setField(endpoint, "userFollowService", mockService);
        String target = String.valueOf(OWNER);
        when(mockService.isFollowing(target)).thenReturn(false);
        when(mockService.followRelationship(target)).thenReturn(new UserFollowRelationshipVO(false, true));
        org.springframework.test.web.servlet.MockMvc mvc = MockMvcBuilders.standaloneSetup(endpoint).build();
        mvc.perform(get("/follow/is-following").param("followUserId", target))
                .andExpect(jsonPath("$.data").value(false));
        mvc.perform(get("/follow/is-following").param("followUserId", target).param("includeIncoming", "true"))
                .andExpect(jsonPath("$.data.following").value(false))
                .andExpect(jsonPath("$.data.followedBy").value(true));
        mvc.perform(post("/follow/add").param("followUserId", target))
                .andExpect(jsonPath("$.data").value(true));
        verify(mockService).addFollow(target);
    }

    @Test
    void retriesAndConcurrentInsertsNeverRemoveAnExistingFollow() {
        when(users.selectById(OWNER)).thenReturn(new User());
        when(follows.selectCount(any())).thenReturn(0L, 1L, 0L);
        when(follows.insert(any(UserFollow.class))).thenReturn(1)
                .thenThrow(new DuplicateKeyException("follow already exists"));
        for (int attempt = 0; attempt < 3; attempt++) {
            service.addFollow(String.valueOf(OWNER));
        }
        verify(follows, times(2)).insert(argThat(row ->
                row.getUserId() == 123L && row.getFollowUserId() == OWNER));
        verify(reminders).handleFollow(123L, OWNER);
        verify(follows, never()).deleteById(any(java.io.Serializable.class));
    }

    @Test
    void paginatesAndComputesMutualRelativeToTheCardOwner() {
        when(users.selectCount(any())).thenReturn(1L);
        User member = new User();
        member.setId(456L);
        when(users.selectBatchIds(Collections.singleton(456L))).thenReturn(Collections.singletonList(member));

        for (boolean following : new boolean[]{true, false}) {
            Page<UserFollow> page = new Page<>(2, 20, 21);
            page.setRecords(Collections.singletonList(relationship(following ? OWNER : 456L, following ? 456L : OWNER)));
            doAnswer(call -> {
                LambdaQueryWrapper<?> query = call.getArgument(1);
                assertTrue(query.getSqlSegment().contains(following ? "userId =" : "followUserId ="));
                assertTrue(query.getSqlSegment().contains("SELECT id FROM `user` WHERE isDelete = 0"));
                assertTrue(query.getParamNameValuePairs().containsValue(OWNER));
                return page;
            }).when(service).page(any(Page.class), any());
            doAnswer(call -> {
                LambdaQueryWrapper<?> query = call.getArgument(0);
                assertTrue(query.getSqlSegment().contains(following ? "followUserId =" : "userId ="));
                assertTrue(query.getParamNameValuePairs().containsValue(OWNER));
                return Collections.singletonList(relationship(following ? 456L : OWNER, following ? OWNER : 456L));
            }).when(service).list(any());

            Page<UserFollowVO> result = service.listUserFollows(OWNER, following, 2, 20);
            assertEquals(21, result.getTotal());
            assertEquals("456", result.getRecords().get(0).getUserId());
            assertTrue(result.getRecords().get(0).getIsMutual());
            assertNull(result.getRecords().get(0).getFollowTime());
        }
    }

    @Test
    void rejectsGuestsAndUnavailableTargetsBeforePaging() {
        when(users.selectCount(any())).thenReturn(0L);
        assertThrows(BusinessException.class, () -> service.listUserFollows(OWNER, true, 1, 20));
        assertThrows(BusinessException.class, () -> service.listUserFollows(OWNER, false, 1, 51));
        when(login.getLoginId()).thenThrow(NotLoginException.class);
        assertThrows(NotLoginException.class, () -> service.listUserFollows(OWNER, false, 1, 20));
        verify(service, never()).page(any(Page.class), any());
    }

    private static UserFollow relationship(long follower, long followed) {
        UserFollow row = new UserFollow();
        row.setUserId(follower);
        row.setFollowUserId(followed);
        row.setCreateTime(new Date());
        return row;
    }
}

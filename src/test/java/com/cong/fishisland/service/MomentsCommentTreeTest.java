package com.cong.fishisland.service;

import cn.dev33.satoken.stp.StpLogic;
import cn.dev33.satoken.stp.StpUtil;
import com.baomidou.mybatisplus.core.MybatisConfiguration;
import com.baomidou.mybatisplus.core.conditions.Wrapper;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.core.metadata.TableInfoHelper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.cong.fishisland.common.exception.BusinessException;
import com.cong.fishisland.mapper.moments.*;
import com.cong.fishisland.model.dto.moments.*;
import com.cong.fishisland.model.entity.moments.*;
import com.cong.fishisland.model.vo.moments.MomentsCommentVO;
import com.cong.fishisland.service.event.EventRemindHandler;
import com.cong.fishisland.service.impl.moments.MomentsServiceImpl;
import org.apache.ibatis.builder.MapperBuilderAssistant;
import org.junit.jupiter.api.*;
import org.springframework.test.util.ReflectionTestUtils;
import java.util.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;
import static org.mockito.ArgumentMatchers.*;

@SuppressWarnings("unchecked")
class MomentsCommentTreeTest {
    MomentsCommentMapper comments;
    MomentsMapper moments;
    MomentsServiceImpl service;
    EventRemindHandler events;
    StpLogic previous;

    @BeforeEach void setup() {
        MybatisConfiguration configuration = new MybatisConfiguration();
        configuration.setMapUnderscoreToCamelCase(false);
        MapperBuilderAssistant assistant = new MapperBuilderAssistant(configuration, "moments-tree");
        TableInfoHelper.initTableInfo(assistant, MomentsComment.class);
        TableInfoHelper.initTableInfo(assistant, Moments.class);
        comments = mock(MomentsCommentMapper.class);
        moments = mock(MomentsMapper.class);
        UserService users = mock(UserService.class);
        when(users.listByIds(anyCollection())).thenReturn(Collections.emptyList());
        events = mock(EventRemindHandler.class);
        service = spy(new MomentsServiceImpl(null, comments, null, users, events, null, null, null));
        ReflectionTestUtils.setField(service, "baseMapper", moments);
        Moments moment = new Moments(); moment.setId(10L); moment.setUserId(9L);
        doReturn(moment).when(service).getById(10L);
        previous = StpUtil.getStpLogic();
        StpLogic login = mock(StpLogic.class);
        when(login.getLoginType()).thenReturn("login");
        when(login.isLogin()).thenReturn(true);
        when(login.getLoginIdAsLong()).thenReturn(123L);
        StpUtil.setStpLogic(login);
    }
    @AfterEach void restore() { StpUtil.setStpLogic(previous); }

    MomentsComment node(long id, Long parent) {
        MomentsComment c = new MomentsComment(); c.setId(id); c.setMomentId(10L);
        c.setUserId(123L); c.setParentId(parent); c.setContent("synthetic");
        c.setLikeNum(0);
        return c;
    }
    MomentsCommentQueryRequest query() {
        MomentsCommentQueryRequest q = new MomentsCommentQueryRequest(); q.setMomentId(10L); return q;
    }
    @Test void readsEveryLevelOnceAndBuildsDirectChildren() {
        when(comments.selectPage(any(Page.class), any(Wrapper.class))).thenAnswer(call -> {
            Page<MomentsComment> p = call.getArgument(0); p.setTotal(1);
            p.setRecords(Collections.singletonList(node(1, null))); return p;
        });
        when(comments.selectList(any(Wrapper.class))).thenReturn(
                Arrays.asList(node(2, 1L), node(3, 1L)),
                Collections.singletonList(node(4, 2L)),
                Collections.singletonList(node(5, 4L)),
                Collections.emptyList());
        Page<MomentsCommentVO> result = service.listComments(query());
        assertEquals(1, result.getTotal());
        MomentsCommentVO root = result.getRecords().get(0);
        assertEquals(2, root.getChildren().size());
        assertEquals(Long.valueOf(5), root.getChildren().get(0).getChildren().get(0).getChildren().get(0).getId());
        assertTrue(root.getChildren().get(1).getChildren().isEmpty());
        verify(comments, times(4)).selectList(any(Wrapper.class));
    }
    @Test void repeatedRowsStopTraversalAndDoNotCreateCycles() {
        when(comments.selectPage(any(Page.class), any(Wrapper.class))).thenAnswer(call -> {
            Page<MomentsComment> p = call.getArgument(0); p.setTotal(1);
            p.setRecords(Collections.singletonList(node(1, null))); return p;
        });
        when(comments.selectList(any(Wrapper.class))).thenReturn(Collections.singletonList(node(2, 1L)));
        assertEquals(1, service.listComments(query()).getRecords().get(0).getChildren().size());
        verify(comments, times(2)).selectList(any(Wrapper.class));
    }
    @Test void emptyLaterPageKeepsTotal() {
        when(comments.selectPage(any(Page.class), any(Wrapper.class))).thenAnswer(call -> {
            Page<MomentsComment> p = call.getArgument(0); p.setTotal(17); return p;
        });
        assertEquals(17, service.listComments(query()).getTotal());
        verify(comments, never()).selectList(any(Wrapper.class));
    }
    @Test void ranksBeforePaginationAndProjectsViewerLikesForRootsAndReplies() {
        MomentsComment root = node(1, null);
        MomentsComment child = node(2, 1L);
        child.setLikeNum(4);
        when(comments.selectPage(any(Page.class), any(Wrapper.class))).thenAnswer(call -> {
            Page<MomentsComment> page = call.getArgument(0);
            LambdaQueryWrapper<MomentsComment> query = call.getArgument(1);
            String sql = query.getSqlSegment();
            assertTrue(sql.contains("ORDER BY isTop DESC,likeNum DESC,createTime DESC,id DESC"), sql);
            page.setRecords(Collections.singletonList(root));
            return page;
        });
        when(comments.selectList(any(Wrapper.class))).thenAnswer(call -> {
            LambdaQueryWrapper<MomentsComment> query = call.getArgument(0);
            String sql = query.getSqlSegment();
            assertTrue(sql.contains("ORDER BY likeNum DESC,createTime DESC,id DESC"), sql);
            return query.getParamNameValuePairs().containsValue(1L)
                    ? Collections.singletonList(child) : Collections.emptyList();
        });
        when(comments.selectLikedCommentIds(123L, Arrays.asList(1L, 2L)))
                .thenReturn(Collections.singletonList(2L));

        MomentsCommentVO result = service.listComments(query()).getRecords().get(0);
        assertFalse(result.isLiked());
        assertTrue(result.getChildren().get(0).isLiked());
        assertEquals(Integer.valueOf(4), result.getChildren().get(0).getLikeNum());
    }

    @Test void setsAndCancelsLikeOncePerTargetState() {
        MomentsComment comment = node(1, null);
        comment.setLikeNum(4);
        when(comments.selectOne(any(Wrapper.class))).thenAnswer(call -> {
            LambdaQueryWrapper<MomentsComment> query = call.getArgument(0);
            assertTrue(query.getSqlSegment().endsWith("FOR UPDATE"));
            return comment;
        });
        when(comments.selectLikedCommentIds(123L, Collections.singletonList(1L))).thenReturn(
                Collections.emptyList(), Collections.singletonList(1L),
                Collections.singletonList(1L), Collections.emptyList());
        when(comments.insertLike(1L, 123L)).thenReturn(1);
        when(comments.deleteLike(1L, 123L)).thenReturn(1);
        List<Integer> counts = new ArrayList<>();
        when(comments.update(isNull(), any(Wrapper.class))).thenAnswer(call -> {
            LambdaUpdateWrapper<MomentsComment> update = call.getArgument(1);
            String sql = update.getSqlSet();
            assertTrue(sql.contains("likeNum="), sql);
            int count = (int) update.getParamNameValuePairs().get("MPGENVAL1");
            counts.add(count);
            comment.setLikeNum(count);
            return 1;
        });
        service.setCommentLiked(1L, true);
        service.setCommentLiked(1L, true);
        service.setCommentLiked(1L, false);
        service.setCommentLiked(1L, false);
        assertEquals(Arrays.asList(5, 4), counts);
        verify(comments).insertLike(1L, 123L);
        verify(comments).deleteLike(1L, 123L);
    }

    @Test void rejectsLikesOnMissingCommentsAndPrivateMoments() {
        assertThrows(BusinessException.class, () -> service.setCommentLiked(1L, true));
        when(comments.selectOne(any(Wrapper.class))).thenReturn(node(1, null));
        service.getById(10L).setVisibility(1);
        assertThrows(BusinessException.class, () -> service.setCommentLiked(1L, true));
        verify(comments, never()).insertLike(anyLong(), anyLong());
    }
    @Test void rejectsMissingAndCrossMomentParentsBeforeInsert() {
        MomentsCommentAddRequest input = new MomentsCommentAddRequest();
        input.setMomentId(10L); input.setParentId(2L); input.setContent("synthetic reply");
        assertThrows(BusinessException.class, () -> service.addComment(input));
        MomentsComment other = node(2, null); other.setMomentId(11L);
        when(comments.selectById(2L)).thenReturn(other);
        assertThrows(BusinessException.class, () -> service.addComment(input));
        verify(comments, never()).insert(any(MomentsComment.class));
    }
    @Test void savesDirectParentAndNotifiesItsCanonicalAuthor() {
        MomentsComment parent = node(4, 2L); parent.setUserId(456L);
        when(comments.selectById(4L)).thenReturn(parent);
        when(comments.insert(any(MomentsComment.class))).thenAnswer(call -> {
            MomentsComment saved = call.getArgument(0);
            assertEquals(Long.valueOf(4), saved.getParentId());
            assertEquals(Long.valueOf(456), saved.getReplyUserId());
            saved.setId(5L); return 1;
        });
        MomentsCommentAddRequest input = new MomentsCommentAddRequest();
        input.setMomentId(10L); input.setParentId(4L); input.setReplyUserId(999L); input.setContent("synthetic reply");
        assertEquals(Long.valueOf(5), service.addComment(input));
        verify(events).handleMomentsComment(5L, 10L, 123L, 456L, "synthetic reply", true);
    }
    @Test void deletesSubtreeAndUsesActualDeletedCount() {
        when(comments.selectById(2L)).thenReturn(node(2, 1L));
        when(comments.selectList(any(Wrapper.class))).thenReturn(
                Collections.singletonList(node(4, 2L)), Collections.singletonList(node(5, 4L)), Collections.emptyList());
        when(comments.deleteBatchIds(anyCollection())).thenAnswer(call -> {
            Collection<Long> ids = call.getArgument(0);
            assertEquals(new HashSet<>(Arrays.asList(2L, 4L, 5L)), new HashSet<>(ids)); return 3;
        });
        when(moments.update(isNull(), any(Wrapper.class))).thenAnswer(call -> {
            com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper<Moments> update = call.getArgument(1);
            assertTrue(update.getSqlSet().contains("commentNum - 3")); return 1;
        });
        service.deleteComment(2L);
        verify(moments).update(isNull(), any(Wrapper.class));
    }
}

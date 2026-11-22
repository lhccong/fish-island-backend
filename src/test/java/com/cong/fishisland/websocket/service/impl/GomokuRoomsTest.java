package com.cong.fishisland.websocket.service.impl;

import cn.hutool.json.JSONUtil;
import com.alibaba.fastjson.JSONObject;
import com.cong.fishisland.model.entity.user.User;
import com.cong.fishisland.model.vo.ws.ChatMessageVo;
import com.cong.fishisland.model.ws.request.WSBaseReq;
import com.cong.fishisland.model.ws.response.WSBaseResp;
import io.netty.channel.Channel;
import io.netty.channel.embedded.EmbeddedChannel;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class GomokuRoomsTest {
    private final EmbeddedChannel black = new EmbeddedChannel();
    private final EmbeddedChannel white = new EmbeddedChannel();
    private final EmbeddedChannel viewer = new EmbeddedChannel();
    private final Map<Channel, List<WSBaseResp<?>>> inboxes = new HashMap<>();
    private final GomokuRooms rooms =
            new GomokuRooms((channel, frame) -> inboxes.computeIfAbsent(channel, key -> new ArrayList<>()).add(frame));

    @AfterEach
    void closeChannels() {
        black.finishAndReleaseAll();
        white.finishAndReleaseAll();
        viewer.finishAndReleaseAll();
    }

    @Test
    void sendsMidgameSnapshotBeforeMovesAndKeepsTheFinishedScoreUntilAPlayerLeaves() {
        String room = create();
        rooms.join(white, 2L, user("白方岛友"), room, null);
        assertEquals("black", last(black, "joinSuccess").getString("yourColor"));
        assertEquals("white", last(white, "joinSuccess").getString("yourColor"));

        move(black, 1L, room, "black", 7, 0);
        move(white, 2L, room, "white", 8, 0);
        rooms.watch(viewer, 3L, watchCommand(room, "1"));
        move(black, 1L, room, "black", 7, 1);
        viewer.runPendingTasks();
        List<WSBaseResp<?>> frames = inboxes.get(viewer);
        assertEquals("chessWatched", frames.get(0).getType());
        JSONObject snapshot = (JSONObject) frames.get(0).getData();
        assertEquals("1", snapshot.getString("requestId"));
        assertEquals("白方岛友", snapshot.getJSONObject("white").getString("name"));
        assertEquals(2, snapshot.getJSONArray("moves").size());
        assertEquals("chessWatchMove", frames.get(1).getType());
        assertEquals(3, ((JSONObject) frames.get(1).getData()).getIntValue("number"));
        assertEquals("black", last(white, "moveChess").getString("player"));

        for (int col = 1; col < 5; col++) {
            move(white, 2L, room, "white", 8, col);
            if (col < 4) move(black, 1L, room, "black", 7, col + 1);
        }
        move(white, 2L, room, "white", 9, 0);
        last(white, "chessMoveRejected");
        rooms.watch(viewer, 3L, watchCommand(room, "2"));
        assertEquals(9, last(viewer, "chessWatched").getJSONArray("moves").size());
        rooms.disconnected(white);
        assertEquals(room, last(viewer, "chessRoomClosed").getString("roomId"));
        assertEquals(room, last(black, "chessRoomClosed").getString("roomId"));
    }

    @Test
    void checksRoomMembershipColorAndTurnAndReleasesOnlyTheViewerOnExit() {
        String room = create();
        rooms.watch(viewer, 3L, watchCommand(room, "1"));
        assertTrue(last(viewer, "chessWatchError").getString("message").contains("尚未开始"));
        rooms.join(white, 1L, user("黑方岛友"), room, null);
        assertEquals("error", lastFrame(white).getType());
        rooms.join(white, 2L, user("白方岛友"), room, null);
        rooms.watch(viewer, 3L, watchCommand(room, "2"));
        assertTrue(last(viewer, "chessWatched").getJSONArray("moves").isEmpty());

        move(white, 2L, room, "white", 0, 0);
        last(white, "chessMoveRejected");
        move(viewer, 3L, room, "black", 0, 0);
        move(viewer, 1L, room, "black", 0, 0);
        rooms.leave(viewer, room);
        move(black, 1L, room, "black", 7, 7);
        move(white, 2L, room, "white", 7, 7);
        last(white, "chessMoveRejected");
        last(viewer, "chessWatched");

        rooms.watch(viewer, 3L, watchCommand(room, "3"));
        JSONObject snapshot = last(viewer, "chessWatched");
        assertEquals(1, snapshot.getJSONArray("moves").size());
        assertEquals(7, snapshot.getJSONArray("moves").getJSONObject(0).getIntValue("row"));
        move(white, 2L, room, "white", 8, 7);
        assertEquals(2, last(viewer, "chessWatchMove").getIntValue("number"));
    }

    @Test
    void opensTheSameInvitationAsPlayerThenSpectatorAndReturnsScopedErrors() {
        String room = create();
        rooms.join(white, 2L, user("白方岛友"), room, "first");
        assertEquals("first", last(white, "joinSuccess").getString("requestId"));
        assertNull(last(black, "joinSuccess").getString("requestId"));
        rooms.join(viewer, 3L, user("观战岛友"), room, null);
        assertEquals("error", lastFrame(viewer).getType());
        move(black, 1L, room, "black", 7, 7);
        rooms.join(viewer, 3L, user("观战岛友"), room, "later");
        JSONObject snapshot = last(viewer, "chessWatched");
        assertEquals("later", snapshot.getString("requestId"));
        assertEquals(1, snapshot.getJSONArray("moves").size());
        move(white, 2L, room, "white", 8, 7);
        assertEquals(2, last(viewer, "chessWatchMove").getIntValue("number"));
        rooms.leave(black, room);
        rooms.join(viewer, 3L, user("观战岛友"), room, "closed");
        JSONObject error = last(viewer, "chessWatchError");
        assertEquals("closed", error.getString("requestId"));
        assertTrue(error.getString("message").contains("已关闭"));
    }

    @Test
    void decodesTheBrowserEnvelopeWithTheExistingWebSocketRequestBeans() {
        WSBaseReq frame = JSONUtil.toBean(
                "{\"type\":2,\"userId\":-1,\"data\":{\"type\":\"watchChess\","
                        + "\"content\":{\"roomId\":\"g-synthetic\",\"requestId\":\"1\"}}}", WSBaseReq.class);
        ChatMessageVo message = JSONUtil.toBean(frame.getData(), ChatMessageVo.class);
        assertEquals("watchChess", message.getType());
        assertEquals(watchCommand("g-synthetic", "1"), GomokuRooms.parse(message.getContent()));

        WSBaseReq invitation = JSONUtil.toBean(
                "{\"type\":2,\"userId\":-1,\"data\":{\"type\":\"joinRoom\","
                        + "\"content\":\"g-synthetic\",\"watchRequestId\":\"2\"}}", WSBaseReq.class);
        ChatMessageVo entry = JSONUtil.toBean(invitation.getData(), ChatMessageVo.class);
        assertEquals("g-synthetic", entry.getContent());
        assertEquals("2", entry.getWatchRequestId());
    }

    private String create() {
        rooms.create(black, 1L, user("黑方岛友"));
        return last(black, "createChessRoom").getString("roomId");
    }

    private static User user(String name) {
        User user = new User();
        user.setUserName(name);
        return user;
    }

    private static JSONObject watchCommand(String room, String request) {
        JSONObject command = new JSONObject();
        command.put("roomId", room);
        command.put("requestId", request);
        return command;
    }

    private void move(Channel channel, long id, String room, String color, int row, int col) {
        JSONObject move = new JSONObject();
        move.put("roomId", room);
        move.put("player", color);
        JSONObject position = new JSONObject();
        position.put("row", row);
        position.put("col", col);
        move.put("position", position);
        rooms.move(channel, id, move);
    }

    private WSBaseResp<?> lastFrame(EmbeddedChannel channel) {
        channel.runPendingTasks();
        List<WSBaseResp<?>> frames = inboxes.get(channel);
        return frames.get(frames.size() - 1);
    }

    private JSONObject last(EmbeddedChannel channel, String type) {
        WSBaseResp<?> frame = lastFrame(channel);
        assertEquals(type, frame.getType());
        return (JSONObject) frame.getData();
    }
}

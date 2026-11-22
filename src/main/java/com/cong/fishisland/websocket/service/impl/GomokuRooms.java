package com.cong.fishisland.websocket.service.impl;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONException;
import com.alibaba.fastjson.JSONObject;
import com.cong.fishisland.model.entity.user.User;
import com.cong.fishisland.model.enums.MessageTypeEnum;
import com.cong.fishisland.model.ws.response.WSBaseResp;
import io.netty.channel.Channel;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;
import java.util.function.BiConsumer;

final class GomokuRooms {
    private static final String PREFIX = "g-";
    private static final int SIZE = 15;
    private static final int MAX_SPECTATORS = 64;
    private static final int[][] DIRECTIONS = {{1, 0}, {0, 1}, {1, 1}, {1, -1}};

    private final Map<String, Room> rooms = new HashMap<>();
    private final Map<Channel, Room> memberships = new HashMap<>();
    private final BiConsumer<Channel, WSBaseResp<?>> sender;

    GomokuRooms(BiConsumer<Channel, WSBaseResp<?>> sender) {
        this.sender = sender;
    }

    static boolean manages(String roomId) {
        return roomId != null && roomId.startsWith(PREFIX);
    }

    static JSONObject parse(String content) {
        try {
            return JSON.parseObject(content);
        } catch (JSONException e) {
            return null;
        }
    }

    synchronized void create(Channel channel, long userId, User user) {
        if (channel == null || !channel.isActive()) return;
        detach(channel);
        String id = PREFIX + UUID.randomUUID().toString().replace("-", "").substring(0, 12);
        while (rooms.containsKey(id)) {
            id = PREFIX + UUID.randomUUID().toString().replace("-", "").substring(0, 12);
        }
        Room room = new Room(id, new Player(channel, userId, user));
        rooms.put(id, room);
        memberships.put(channel, room);
        send(channel, MessageTypeEnum.CREATE_CHESS_ROOM, data(room));
    }

    synchronized void join(Channel channel, long userId, User user, String roomId, String watchRequestId) {
        if (channel == null || !channel.isActive()) return;
        String requestId = validIdentifier(watchRequestId) && validIdentifier(roomId) ? watchRequestId : null;
        Room room = rooms.get(roomId);
        if (room == null || room.white != null || !room.black.channel.isActive()) {
            if (requestId != null) {
                watchRoom(channel, userId, roomId, requestId);
            } else {
                send(channel, MessageTypeEnum.ERROR, "房间不存在或已开始");
            }
            return;
        }
        if (room.black.userId == userId) {
            String message = "不能和自己的账号对局";
            if (requestId != null) {
                JSONObject response = watchResponse(roomId, requestId);
                response.put("message", message);
                send(channel, MessageTypeEnum.CHESS_WATCH_ERROR, response);
            } else {
                send(channel, MessageTypeEnum.ERROR, message);
            }
            return;
        }
        detach(channel);
        room.white = new Player(channel, userId, user);
        memberships.put(channel, room);
        joined(room, room.black, room.white, "black", "white", null);
        joined(room, room.white, room.black, "white", "black", requestId);
    }

    synchronized void watch(Channel channel, long userId, JSONObject command) {
        if (channel == null || !channel.isActive() || command == null) return;
        String roomId = command.getString("roomId");
        String requestId = command.getString("requestId");
        if (!validIdentifier(roomId) || !validIdentifier(requestId)) return;
        watchRoom(channel, userId, roomId, requestId);
    }

    private void watchRoom(Channel channel, long userId, String roomId, String requestId) {
        JSONObject response = watchResponse(roomId, requestId);
        Room room = rooms.get(roomId);
        String error = null;
        if (!manages(roomId)) {
            error = "该房间使用旧版协议，请棋手更新后重新创建房间。";
        } else if (room == null || !room.black.channel.isActive()
                || (room.white != null && !room.white.channel.isActive())) {
            error = "房间已关闭或不存在，请核对房间号。";
        } else if (room.white == null) {
            error = "对局尚未开始，请稍后重试。";
        } else if (room.black.userId == userId || room.white.userId == userId) {
            error = "你是本局棋手，请返回对局。";
        } else if (room.spectators.size() >= MAX_SPECTATORS && !room.spectators.contains(channel)) {
            error = "观战席已满，请稍后重试。";
        }
        if (error != null) {
            response.put("message", error);
            send(channel, MessageTypeEnum.CHESS_WATCH_ERROR, response);
            return;
        }
        if (memberships.get(channel) != room) {
            detach(channel);
            memberships.put(channel, room);
            room.spectators.add(channel);
        }
        response.put("black", room.black.profile);
        response.put("white", room.white.profile);
        response.put("moves", new ArrayList<>(room.moves));
        send(channel, MessageTypeEnum.CHESS_WATCHED, response);
    }

    synchronized void move(Channel channel, long userId, JSONObject message) {
        if (channel == null || !channel.isActive()) return;
        String roomId = message.getString("roomId");
        Room room = rooms.get(roomId);
        String color = room == null || room.white == null ? null
                : room.black.matches(channel, userId) ? "black"
                : room.white.matches(channel, userId) ? "white" : null;
        if (color == null) return;
        JSONObject position = message.get("position") instanceof JSONObject
                ? (JSONObject) message.get("position") : null;
        Integer row = coordinate(position, "row");
        Integer col = coordinate(position, "col");
        String turn = room.moves.size() % 2 == 0 ? "black" : "white";
        if (room.finished || !turn.equals(color) || !color.equals(message.get("player"))
                || row == null || col == null || room.board[row][col] != null) {
            JSONObject response = data(room);
            response.put("message", "服务端未接受这一步，请退出后重新建房。");
            send(channel, MessageTypeEnum.CHESS_MOVE_REJECTED, response);
            return;
        }
        room.board[row][col] = color;
        JSONObject move = new JSONObject();
        move.put("row", row);
        move.put("col", col);
        move.put("player", color);
        room.moves.add(move);
        room.finished = room.moves.size() == SIZE * SIZE || won(room.board, row, col, color);

        JSONObject opponentMove = data(room);
        opponentMove.put("position", position);
        opponentMove.put("player", color);
        send(color.equals("black") ? room.white.channel : room.black.channel,
                MessageTypeEnum.MOVE_CHESS, opponentMove);
        if (!room.spectators.isEmpty()) {
            JSONObject update = data(room);
            update.put("number", room.moves.size());
            update.put("move", move);
            for (Channel spectator : room.spectators) {
                send(spectator, MessageTypeEnum.CHESS_WATCH_MOVE, update);
            }
        }
    }

    synchronized void leave(Channel channel, String roomId) {
        Room room = memberships.get(channel);
        if (room != null && room.id.equals(roomId)) detach(channel);
    }

    synchronized void disconnected(Channel channel) {
        detach(channel);
    }

    private void detach(Channel channel) {
        Room room = memberships.remove(channel);
        if (room == null || room.spectators.remove(channel)) return;
        rooms.remove(room.id);
        JSONObject response = data(room);
        for (Channel spectator : room.spectators) {
            memberships.remove(spectator);
            send(spectator, MessageTypeEnum.CHESS_ROOM_CLOSED, response);
        }
        if (room.white != null) {
            Channel opponent = channel.equals(room.black.channel) ? room.white.channel : room.black.channel;
            memberships.remove(opponent);
            send(opponent, MessageTypeEnum.CHESS_ROOM_CLOSED, response);
        }
    }

    private void joined(Room room, Player player, Player opponent, String color, String opponentColor,
                        String requestId) {
        JSONObject response = data(room);
        response.put("playerId", String.valueOf(opponent.userId));
        response.put("playerInfo", opponent.profile);
        response.put("yourColor", color);
        response.put("opponentColor", opponentColor);
        if (requestId != null) response.put("requestId", requestId);
        send(player.channel, MessageTypeEnum.JOIN_SUCCESS, response);
    }

    private static JSONObject watchResponse(String roomId, String requestId) {
        JSONObject response = new JSONObject();
        response.put("roomId", roomId);
        response.put("requestId", requestId);
        return response;
    }

    private static boolean validIdentifier(String value) {
        return value != null && value.matches("[a-zA-Z0-9_-]{1,64}");
    }

    private void send(Channel channel, MessageTypeEnum type, Object data) {
        // Enqueue every frame while holding the room lock, also when called from this event loop.
        WSBaseResp<?> frame = WSBaseResp.builder().type(type.getType()).data(data).build();
        channel.eventLoop().execute(() -> sender.accept(channel, frame));
    }

    private static JSONObject data(Room room) {
        JSONObject data = new JSONObject();
        data.put("roomId", room.id);
        data.put("gameType", "normal");
        data.put("spectatable", true);
        return data;
    }

    private static Integer coordinate(JSONObject position, String key) {
        Object value = position == null ? null : position.get(key);
        return value instanceof Integer && (Integer) value >= 0 && (Integer) value < SIZE
                ? (Integer) value : null;
    }

    private static boolean won(String[][] board, int row, int col, String color) {
        for (int[] direction : DIRECTIONS) {
            int count = 1;
            for (int sign : new int[]{-1, 1}) {
                int r = row + direction[0] * sign;
                int c = col + direction[1] * sign;
                while (r >= 0 && r < SIZE && c >= 0 && c < SIZE && color.equals(board[r][c])) {
                    count++;
                    r += direction[0] * sign;
                    c += direction[1] * sign;
                }
            }
            if (count >= 5) return true;
        }
        return false;
    }

    private static final class Room {
        final String id;
        final Player black;
        final Set<Channel> spectators = new HashSet<>();
        final List<JSONObject> moves = new ArrayList<>();
        final String[][] board = new String[SIZE][SIZE];
        Player white;
        boolean finished;

        Room(String id, Player black) {
            this.id = id;
            this.black = black;
        }
    }

    private static final class Player {
        final Channel channel;
        final long userId;
        final JSONObject profile = new JSONObject();

        Player(Channel channel, long userId, User user) {
            this.channel = channel;
            this.userId = userId;
            profile.put("name", user.getUserName());
            profile.put("avatar", user.getUserAvatar());
        }

        boolean matches(Channel channel, long userId) {
            return Objects.equals(this.channel, channel) && this.userId == userId;
        }
    }
}

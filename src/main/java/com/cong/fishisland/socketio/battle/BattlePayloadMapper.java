package com.cong.fishisland.socketio.battle;

import com.cong.fishisland.model.fishbattle.battle.BattleVector3;
import com.cong.fishisland.model.fishbattle.battle.SpellCastRequest;
import com.fasterxml.jackson.databind.JsonNode;
import org.springframework.stereotype.Component;

import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * 客户端消息载荷到服务端模型的映射器。
 */
@Component
public class BattlePayloadMapper {

    public SpellCastRequest toSpellCastRequest(JsonNode payload) {
        return SpellCastRequest.builder()
                .requestId(payload.path("requestId").asText(null))
                .roomId(payload.path("roomId").asText(null))
                .casterId(payload.path("casterId").asText(null))
                .slot(payload.path("slot").asText(null))
                .skillId(payload.path("skillId").asText(null))
                .targetEntityId(payload.path("targetEntityId").asText(null))
                .targetPoint(readVector3(payload.path("targetPoint")))
                .targetDirection(readVector3(payload.path("targetDirection")))
                .clientTimestamp(payload.path("clientTimestamp").asLong(0L))
                .extraContext(readExtraContext(payload.path("extraContext")))
                .build();
    }

    public BattleVector3 readVector3(JsonNode node) {
        if (node == null || node.isMissingNode() || node.isNull()) {
            return null;
        }
        return BattleVector3.builder()
                .x(node.path("x").asDouble())
                .y(node.path("y").asDouble())
                .z(node.path("z").asDouble())
                .build();
    }

    private Map<String, Object> readExtraContext(JsonNode node) {
        if (node == null || node.isMissingNode() || node.isNull() || !node.isObject()) {
            return null;
        }
        Map<String, Object> map = new LinkedHashMap<String, Object>();
        Iterator<Map.Entry<String, JsonNode>> iterator = node.fields();
        while (iterator.hasNext()) {
            Map.Entry<String, JsonNode> entry = iterator.next();
            map.put(entry.getKey(), entry.getValue());
        }
        return map;
    }
}

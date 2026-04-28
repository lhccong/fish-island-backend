package com.cong.fishisland.socketio.battle;

import com.cong.fishisland.model.fishbattle.battle.ActiveSpellInstance;
import com.cong.fishisland.model.fishbattle.battle.BattleChampionState;
import com.cong.fishisland.model.fishbattle.battle.BattleRoom;
import com.cong.fishisland.model.fishbattle.battle.CastValidationResult;
import com.cong.fishisland.model.fishbattle.battle.PlayerInput;
import com.cong.fishisland.model.fishbattle.battle.PlayerSession;
import com.cong.fishisland.model.fishbattle.battle.SpellCastRequest;
import com.fasterxml.jackson.databind.JsonNode;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * 施法处理器：封装施法验证、创建、广播逻辑。
 */
@Component
@RequiredArgsConstructor
public class SpellCastHandler {
    private final SpellLifecycleService spellLifecycleService;
    private final BattlePayloadMapper battlePayloadMapper;
    private final Battle3dBroadcastService battleBroadcastService;
    private final HeroSkillDefinitionService heroSkillDefinitionService;

    public void handleCastFromQueue(BattleRoom room, BattleChampionState champion, PlayerInput input) {
        if (input.getRawPayload() == null) {
            return;
        }
        SpellCastRequest spellCastRequest = battlePayloadMapper.toSpellCastRequest(input.getRawPayload());
        if (input.getType() == PlayerInput.Type.BASIC_ATTACK && spellCastRequest.getSlot() == null) {
            spellCastRequest.setSlot("basicAttack");
        }
        if (spellCastRequest.getCasterId() == null) {
            spellCastRequest.setCasterId(champion.getId());
        }
        if (spellCastRequest.getRoomId() == null) {
            spellCastRequest.setRoomId(room.getRoomId());
        }

        String requestType = input.getType() == PlayerInput.Type.BASIC_ATTACK ? "basicAttack" : "castSpell";
        CastValidationResult validationResult = spellLifecycleService.validate(room, spellCastRequest);
        if (!validationResult.isPassed()) {
            Map<String, Object> rejectedPayload = new LinkedHashMap<String, Object>();
            rejectedPayload.put("eventId", requestType + "-rejected-" + room.getSequence().incrementAndGet());
            rejectedPayload.put("sequence", room.getSequence().get());
            rejectedPayload.put("roomId", room.getRoomId());
            rejectedPayload.put("serverTime", System.currentTimeMillis());
            rejectedPayload.put("requestId", spellCastRequest.getRequestId());
            rejectedPayload.put("casterId", spellCastRequest.getCasterId());
            rejectedPayload.put("skillId", spellCastRequest.getSkillId());
            rejectedPayload.put("slot", spellCastRequest.getSlot());
            rejectedPayload.put("reasonCode", validationResult.getReasonCode());
            rejectedPayload.put("reasonMessage", validationResult.getReasonMessage());
            PlayerSession requester = findSessionById(room, input.getSessionId());
            if (requester != null) {
                battleBroadcastService.sendToPlayer(requester, "spellCastRejected", rejectedPayload);
            }
            return;
        }

        ActiveSpellInstance activeSpellInstance = spellLifecycleService.create(room, spellCastRequest);

        Map<String, Object> acceptedPayload = new LinkedHashMap<String, Object>();
        acceptedPayload.put("eventId", requestType + "-accepted-" + room.getSequence().incrementAndGet());
        acceptedPayload.put("sequence", room.getSequence().get());
        acceptedPayload.put("roomId", room.getRoomId());
        acceptedPayload.put("serverTime", System.currentTimeMillis());
        acceptedPayload.put("requestId", spellCastRequest.getRequestId());
        acceptedPayload.put("castInstanceId", activeSpellInstance.getCastInstanceId());
        acceptedPayload.put("casterId", activeSpellInstance.getCasterId());
        acceptedPayload.put("skillId", activeSpellInstance.getSkillId());
        acceptedPayload.put("slot", activeSpellInstance.getSlot());
        acceptedPayload.put("targetEntityId", activeSpellInstance.getTargetEntityId());
        acceptedPayload.put("targetPoint", activeSpellInstance.getTargetPoint());
        battleBroadcastService.broadcast(room, "spellCastAccepted", acceptedPayload);

        Map<String, Object> startedPayload = new LinkedHashMap<String, Object>();
        startedPayload.put("eventId", "spell-started-" + room.getSequence().incrementAndGet());
        startedPayload.put("sequence", room.getSequence().get());
        startedPayload.put("roomId", room.getRoomId());
        startedPayload.put("serverTime", System.currentTimeMillis());
        startedPayload.put("castInstanceId", activeSpellInstance.getCastInstanceId());
        startedPayload.put("requestId", activeSpellInstance.getRequestId());
        startedPayload.put("casterId", activeSpellInstance.getCasterId());
        startedPayload.put("skillId", activeSpellInstance.getSkillId());
        startedPayload.put("slot", activeSpellInstance.getSlot());
        startedPayload.put("stage", activeSpellInstance.getStage());
        startedPayload.put("targetEntityId", activeSpellInstance.getTargetEntityId());
        startedPayload.put("targetPoint", activeSpellInstance.getTargetPoint());
        startedPayload.put("rotation", champion.getRotation());

        JsonNode skillDef = heroSkillDefinitionService.findSkillBySlot(champion.getHeroId(), activeSpellInstance.getSlot());
        if (skillDef == null && activeSpellInstance.getSkillId() != null) {
            skillDef = heroSkillDefinitionService.findSkillById(champion.getHeroId(), activeSpellInstance.getSkillId(), activeSpellInstance.getSlot());
        }
        JsonNode cast = skillDef != null ? skillDef.path("cast") : null;
        boolean lockMovement = resolveLockMovement(cast);
        long movementLockDurationMs = lockMovement ? resolveCastTimeMs(cast) + resolveBackswingMs(cast) : 0L;
        startedPayload.put("lockMovement", lockMovement);
        startedPayload.put("movementLockDurationMs", movementLockDurationMs);
        battleBroadcastService.broadcast(room, "spellCastStarted", startedPayload);
    }

    private long resolveCastTimeMs(JsonNode cast) {
        if (cast == null || cast.isMissingNode()) {
            return 0L;
        }
        return Math.max(0L, cast.path("castTimeMs").asLong(cast.path("windupMs").asLong(0L)));
    }

    private long resolveBackswingMs(JsonNode cast) {
        if (cast == null || cast.isMissingNode()) {
            return 0L;
        }
        return Math.max(0L, cast.path("backswingMs").asLong(0L));
    }

    private boolean resolveLockMovement(JsonNode cast) {
        if (cast == null || cast.isMissingNode()) {
            return false;
        }
        if (cast.has("lockMovement")) {
            return cast.path("lockMovement").asBoolean(false);
        }
        if (cast.has("allowMoveDuringCast")) {
            return !cast.path("allowMoveDuringCast").asBoolean(true);
        }
        return false;
    }

    private PlayerSession findSessionById(BattleRoom room, String sessionId) {
        if (sessionId == null || room.getPlayers() == null) {
            return null;
        }
        return room.getPlayers().stream()
                .filter(ps -> sessionId.equals(ps.getSessionId()))
                .findFirst()
                .orElse(null);
    }
}

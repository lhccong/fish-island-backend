package com.cong.fishisland.socketio.battle;

import com.cong.fishisland.model.entity.fishbattle.FishBattleSummonerSpell;
import com.cong.fishisland.service.fishbattle.FishBattleSummonerSpellService;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import com.fasterxml.jackson.databind.node.ObjectNode;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Locale;

/**
 * 技能定义服务（main 项目最小版）。
 * 当前仅保留：普攻模板 + 数据库驱动的召唤师技能定义。
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class HeroSkillDefinitionService {
    private final FishBattleSummonerSpellService fishBattleSummonerSpellService;
    private final ObjectMapper objectMapper;

    public JsonNode getHeroSkillDefinition(String heroId) {
        ObjectNode root = JsonNodeFactory.instance.objectNode();
        root.put("heroId", heroId != null ? heroId : "template_basic_hero");
        root.set("passives", JsonNodeFactory.instance.arrayNode());
        ArrayNode skills = JsonNodeFactory.instance.arrayNode();
        skills.add(createBasicAttackDefinition());
        root.set("skills", skills);
        return root;
    }

    public JsonNode getAllSkillsByHeroId(String heroId) {
        return getHeroSkillDefinition(heroId);
    }

    public JsonNode findSkillById(String heroId, String skillId) {
        return findSkillById(heroId, skillId, null);
    }

    public JsonNode findSkillById(String heroId, String skillId, String slot) {
        if (skillId == null || skillId.trim().isEmpty()) {
            return null;
        }
        if ("template_basic_attack".equals(skillId) || "basicAttack".equals(skillId)) {
            return createBasicAttackDefinition();
        }
        return getSummonerSpellDefinition(skillId, slot);
    }

    public JsonNode findSkillBySlot(String heroId, String slot) {
        if (slot == null || slot.trim().isEmpty()) {
            return null;
        }
        if ("basicAttack".equals(slot)) {
            return createBasicAttackDefinition();
        }
        return null;
    }

    public JsonNode getSummonerSpellDefinition(String spellId, String slot) {
        if (spellId == null || spellId.trim().isEmpty()) {
            return null;
        }
        FishBattleSummonerSpell spell = findEnabledSpellById(spellId);
        if (spell == null) {
            return createFallbackSummonerDefinition(spellId, slot);
        }
        return buildSummonerDefinition(spell, slot);
    }

    private FishBattleSummonerSpell findEnabledSpellById(String spellId) {
        List<FishBattleSummonerSpell> spells = fishBattleSummonerSpellService.listEnabledSpells();
        if (spells == null || spells.isEmpty()) {
            return null;
        }
        for (FishBattleSummonerSpell spell : spells) {
            if (spell != null && spell.getSpellId() != null && spell.getSpellId().equalsIgnoreCase(spellId)) {
                return spell;
            }
        }
        return null;
    }

    private JsonNode buildSummonerDefinition(FishBattleSummonerSpell spell, String slot) {
        ObjectNode root = null;
        if (spell.getAssetConfig() != null && !spell.getAssetConfig().trim().isEmpty()) {
            try {
                JsonNode parsed = objectMapper.readTree(spell.getAssetConfig());
                if (parsed != null && parsed.isObject()) {
                    root = (ObjectNode) parsed.deepCopy();
                }
            } catch (Exception e) {
                log.warn("解析召唤师技能 asset_config 失败，spellId={}", spell.getSpellId(), e);
            }
        }
        if (root == null) {
            JsonNode fallback = createFallbackSummonerDefinition(spell.getSpellId(), slot);
            root = fallback != null && fallback.isObject()
                    ? (ObjectNode) fallback
                    : createBaseDefinition(spell.getSpellId(), slot, spell.getName(),
                    spell.getCooldown() != null ? spell.getCooldown() * 1000L : 0L,
                    "self_cast", 0D);
        }
        normalizeSummonerDefinition(root, spell, slot);
        return root;
    }

    private void normalizeSummonerDefinition(ObjectNode root, FishBattleSummonerSpell spell, String slot) {
        root.put("skillId", spell.getSpellId());
        if (slot != null && !slot.trim().isEmpty()) {
            root.put("slot", slot);
        } else if (!root.hasNonNull("slot")) {
            root.put("slot", "summonerD");
        }
        root.put("name", spell.getName() != null ? spell.getName() : spell.getSpellId());
        if (!root.has("initialLevel")) {
            root.put("initialLevel", 1);
        }

        ObjectNode cooldown = root.with("cooldown");
        if (!cooldown.has("baseMs")) {
            long baseMs = spell.getCooldown() != null ? spell.getCooldown() * 1000L : 0L;
            cooldown.put("baseMs", baseMs);
        }

        ObjectNode cast = root.with("cast");
        if (!cast.has("type")) {
            cast.put("type", "self_cast");
        }
        if (!cast.has("range")) {
            cast.put("range", 0D);
        }
        if (!cast.has("castTimeMs")) {
            cast.put("castTimeMs", 0L);
        }
        if (!cast.has("backswingMs")) {
            cast.put("backswingMs", 0L);
        }
        if (!cast.has("lockMovement")) {
            cast.put("lockMovement", false);
        }

        ObjectNode cost = root.with("cost");
        if (!cost.has("resourceType")) {
            cost.put("resourceType", "none");
        }
        if (!cost.has("amount")) {
            cost.put("amount", 0D);
        }

        ObjectNode effects = root.with("effects");
        if (!effects.has("onActivate")) {
            effects.set("onActivate", JsonNodeFactory.instance.arrayNode());
        }
        if (!effects.has("onImpact")) {
            effects.set("onImpact", JsonNodeFactory.instance.arrayNode());
        }
        if (!effects.has("onSuccessCast")) {
            effects.set("onSuccessCast", JsonNodeFactory.instance.arrayNode());
        }
    }

    private JsonNode createFallbackSummonerDefinition(String spellId, String slot) {
        String normalized = spellId == null ? "" : spellId.trim().toLowerCase(Locale.ROOT);
        switch (normalized) {
            case "flash":
                return createFlashDefinition(slot);
            case "ghost":
                return createGhostDefinition(slot);
            case "heal":
                return createHealDefinition(slot);
            default:
                return null;
        }
    }

    private ObjectNode createFlashDefinition(String slot) {
        ObjectNode skill = createBaseDefinition("flash", slot, "闪现", 300000L, "target_point", 10D);
        ArrayNode onActivate = JsonNodeFactory.instance.arrayNode();
        ObjectNode effect = JsonNodeFactory.instance.objectNode();
        effect.put("type", "Teleport");
        onActivate.add(effect);
        skill.with("effects").set("onActivate", onActivate);
        return skill;
    }

    private ObjectNode createGhostDefinition(String slot) {
        ObjectNode skill = createBaseDefinition("ghost", slot, "疾跑", 180000L, "self_cast", 0D);
        ArrayNode onActivate = JsonNodeFactory.instance.arrayNode();
        ObjectNode effect = JsonNodeFactory.instance.objectNode();
        effect.put("type", "ApplyStatus");
        effect.put("statusId", "summoner_ghost_speed_up");
        effect.put("durationMs", 10000L);
        effect.put("targetMode", "self");
        effect.put("stacks", 1);
        onActivate.add(effect);
        skill.with("effects").set("onActivate", onActivate);
        return skill;
    }

    private ObjectNode createHealDefinition(String slot) {
        ObjectNode skill = createBaseDefinition("heal", slot, "治疗", 240000L, "self_cast", 0D);
        ArrayNode onActivate = JsonNodeFactory.instance.arrayNode();

        ObjectNode healSelf = JsonNodeFactory.instance.objectNode();
        healSelf.put("type", "Heal");
        healSelf.put("amount", 120D);
        healSelf.put("targetMode", "self");
        onActivate.add(healSelf);

        ObjectNode healAlly = JsonNodeFactory.instance.objectNode();
        healAlly.put("type", "Heal");
        healAlly.put("amount", 90D);
        healAlly.put("targetMode", "radius");
        healAlly.put("radius", 5D);
        ObjectNode targetRules = healAlly.putObject("targetRules");
        targetRules.put("allyOnly", true);
        targetRules.put("allowSelf", false);
        onActivate.add(healAlly);

        ObjectNode speedUp = JsonNodeFactory.instance.objectNode();
        speedUp.put("type", "ApplyStatus");
        speedUp.put("statusId", "generic_move_speed_up");
        speedUp.put("durationMs", 1000L);
        speedUp.put("targetMode", "self");
        speedUp.put("stacks", 1);
        onActivate.add(speedUp);

        skill.with("effects").set("onActivate", onActivate);
        return skill;
    }

    private ObjectNode createBasicAttackDefinition() {
        ObjectNode skill = createBaseDefinition("template_basic_attack", "basicAttack", "普攻", 0L, "target_unit", 0D);
        skill.with("cast").put("castTimeMs", 100L);
        skill.with("cast").put("backswingMs", 120L);
        skill.with("cast").put("lockMovement", true);
        ObjectNode rules = skill.with("cast").putObject("targetRules");
        rules.put("enemyOnly", true);
        rules.put("allowSelf", false);

        ArrayNode onImpact = JsonNodeFactory.instance.arrayNode();
        ObjectNode damage = JsonNodeFactory.instance.objectNode();
        damage.put("type", "Damage");
        damage.put("damageType", "physical");
        damage.put("amount", 0D);
        damage.put("targetMode", "single");
        onImpact.add(damage);
        skill.with("effects").set("onImpact", onImpact);
        return skill;
    }

    private ObjectNode createBaseDefinition(String skillId, String slot, String name, long cooldownMs, String castType, double range) {
        ObjectNode skill = JsonNodeFactory.instance.objectNode();
        skill.put("skillId", skillId);
        skill.put("slot", slot != null && !slot.trim().isEmpty() ? slot : "summonerD");
        skill.put("name", name);
        skill.put("initialLevel", 1);
        ObjectNode cooldown = skill.putObject("cooldown");
        cooldown.put("baseMs", cooldownMs);
        ObjectNode cast = skill.putObject("cast");
        cast.put("type", castType);
        cast.put("range", range);
        cast.put("castTimeMs", 0L);
        cast.put("backswingMs", 0L);
        cast.put("lockMovement", false);
        ObjectNode cost = skill.putObject("cost");
        cost.put("resourceType", "none");
        cost.put("amount", 0D);
        ObjectNode effects = skill.putObject("effects");
        effects.set("onActivate", JsonNodeFactory.instance.arrayNode());
        effects.set("onImpact", JsonNodeFactory.instance.arrayNode());
        effects.set("onSuccessCast", JsonNodeFactory.instance.arrayNode());
        return skill;
    }
}

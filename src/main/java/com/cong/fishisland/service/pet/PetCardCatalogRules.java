package com.cong.fishisland.service.pet;

import com.cong.fishisland.common.ErrorCode;
import com.cong.fishisland.common.exception.BusinessException;
import com.cong.fishisland.model.dto.pet.PetCardCatalog.Card;
import com.cong.fishisland.model.dto.pet.PetCardCatalog.Pool;
import com.fasterxml.jackson.databind.JsonNode;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;

final class PetCardCatalogRules {
    private static final Set<String> GRADES = keys("common", "fine", "epic", "legendary", "shiny");
    private static final Set<String> STATS = keys("attack", "defense", "health", "speed");
    private static final Set<String> BOOST_STATS = keys("attack", "defense", "speed");
    private static final Set<String> TRIGGERS = keys("turnStart", "firstAttack", "firstIncomingAttack",
            "firstHit", "firstCriticalHit", "firstIncomingCriticalHit", "afterFirstIncomingCriticalHit",
            "healthBelowAtTurnStart");

    private PetCardCatalogRules() {}

    static void validatePool(Pool pool) {
        require(pool != null, "缺少卡池数据");
        validateBase(pool.getId(), pool.getName(), pool.getSortOrder(), pool.getVersion());
        require(pool.getWeights() != null && pool.getWeights().keySet().equals(GRADES), "请填写全部品质权重");
        require(pool.getWeights().values().stream().allMatch(n -> n != null && n >= 0 && n <= 10000)
                && pool.getWeights().values().stream().anyMatch(n -> n > 0), "品质权重须在 0–10000 之间且至少一项大于 0");
        if (pool.getCoverId() != null) validateId(pool.getCoverId());
    }

    static void validateCard(Card card) {
        require(card != null, "缺少卡牌数据");
        validateBase(card.getId(), card.getName(), card.getSortOrder(), card.getVersion());
        require(GRADES.contains(card.getGrade()), "未知卡牌品质");
        require(PetCardAtlasStore.isAtlasKey(card.getArtKey()) && card.getArtTile() != null
                && card.getArtTile() >= 0 && card.getArtTile() < 10, "请指定图集及第 1–10 格");
        require(card.getPoolIds() != null && card.getPoolIds().size() <= 100
                && new HashSet<>(card.getPoolIds()).size() == card.getPoolIds().size(), "卡池列表无效");
        card.getPoolIds().forEach(PetCardCatalogRules::validateId);
        JsonNode stats = card.getStats();
        require(stats != null && stats.isObject() && stats.has("health"), "请填写基础生命");
        Iterator<String> fields = stats.fieldNames();
        while (fields.hasNext()) {
            String stat = fields.next();
            require(STATS.contains(stat) && bounded(stats.path(stat), "health".equals(stat) ? 1 : 0, 1000000),
                    "基础属性超出范围");
        }
        validateBattle(card.getGrade(), card.getBattle());
    }

    private static void validateBase(String id, String name, int sort, Integer version) {
        validateId(id);
        require(name != null && !name.trim().isEmpty() && name.length() <= 80, "名称须为 1–80 字");
        require(sort >= 0 && sort <= 1000000 && (version == null || version > 0), "排序或版本无效");
    }

    static void validateId(String id) {
        require(id != null && id.matches("[A-Za-z][A-Za-z0-9_-]{0,47}"), "编号须以英文字母开头，最多 48 位");
    }

    private static void validateBattle(String grade, JsonNode battle) {
        require(battle != null && battle.isObject(), "缺少对战能力");
        if ("common".equals(grade)) {
            require(only(battle, "kind", "phase", "action") && "action".equals(battle.path("kind").asText())
                    && "afterReveal".equals(battle.path("phase").asText())
                    && keys("attack", "dodge").contains(battle.path("action").asText()), "普通卡仅支持亮牌后攻击或闪避");
            return;
        }
        require(only(battle, "kind", "name", "turnOffset", "trigger", "outcome")
                && "effect".equals(battle.path("kind").asText())
                && bounded(battle.path("turnOffset"), 1, 1)
                && battle.path("name").isTextual() && !battle.path("name").asText().trim().isEmpty()
                && battle.path("name").asText().length() <= 80, "技能名称或生效回合无效");
        JsonNode trigger = battle.path("trigger");
        String timing = trigger.path("kind").asText();
        require(TRIGGERS.contains(timing) && ("healthBelowAtTurnStart".equals(timing)
                ? only(trigger, "kind", "thresholdPercent") && bounded(trigger.path("thresholdPercent"), 1, 100)
                : only(trigger, "kind")), "技能触发条件无效");
        JsonNode outcome = battle.path("outcome");
        String kind = outcome.path("kind").asText();
        if ("fine".equals(grade)) {
            require("turnStart".equals(timing) && "heal".equals(kind), "精良卡须在下一回合开始时回血");
        }
        if ("guaranteedCriticalHit".equals(kind)) {
            require("firstHit".equals(timing), "必定暴击须在首次命中时触发");
        }
        require(validOutcome(kind, outcome), "技能效果或数值无效");
    }

    private static boolean validOutcome(String kind, JsonNode outcome) {
        switch (kind) {
            case "statBoost":
                JsonNode stats = outcome.path("stats");
                Set<String> seen = new HashSet<>();
                if (!stats.isArray()) return false;
                for (JsonNode stat : stats) {
                    if (!BOOST_STATS.contains(stat.asText()) || !seen.add(stat.asText())) return false;
                }
                return !seen.isEmpty() && percent(outcome, "percent", "stats");
            case "criticalChanceBoost":
            case "accuracyBoost":
                return percent(outcome, "percentagePoints");
            case "outgoingDamageBoost":
            case "incomingDamageReduction":
                return percent(outcome, "percent");
            case "shield":
            case "heal":
                return percent(outcome, "maxHealthPercent");
            case "guaranteedCriticalHit":
                return only(outcome, "kind");
            default:
                return false;
        }
    }

    private static boolean percent(JsonNode node, String field, String... additional) {
        Set<String> fields = keys("kind", field);
        fields.addAll(Arrays.asList(additional));
        return node.isObject() && fields.size() == node.size()
                && fields.stream().allMatch(node::has) && bounded(node.path(field), 1, 100);
    }

    private static boolean only(JsonNode node, String... fields) {
        return node.isObject() && node.size() == fields.length && Arrays.stream(fields).allMatch(node::has);
    }

    private static boolean bounded(JsonNode node, int min, int max) {
        return node.isIntegralNumber() && node.canConvertToInt() && node.intValue() >= min && node.intValue() <= max;
    }

    private static Set<String> keys(String... values) {
        return new HashSet<>(Arrays.asList(values));
    }

    static void require(boolean condition, String message) {
        if (!condition) throw new BusinessException(ErrorCode.PARAMS_ERROR, message);
    }
}

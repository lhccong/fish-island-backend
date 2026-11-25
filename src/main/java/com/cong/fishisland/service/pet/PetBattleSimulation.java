package com.cong.fishisland.service.pet;

import com.cong.fishisland.constant.BattleConstant;
import com.cong.fishisland.model.dto.pet.PetCardPassive;
import com.cong.fishisland.model.vo.game.AttackResultVO;
import com.cong.fishisland.model.vo.game.BattleResultVO;
import com.cong.fishisland.model.vo.game.BattleStatsVO;
import com.cong.fishisland.model.vo.game.PetBattleResultVO;
import com.cong.fishisland.model.vo.game.PetCardActivationVO;
import com.fasterxml.jackson.databind.JsonNode;
import lombok.Value;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Random;

/** Mutable state for a single battle. Each result frame still represents one attack. */
public final class PetBattleSimulation {
    private static final double CARD_CHANCE = 0.20;
    private static final int RATE_SCALE = 10000;

    private final Fighter pet;
    private final Fighter opponent;
    private final Random random;
    private final boolean petUsuallyFirst;
    private Fighter attacker;
    private boolean lastSlot;
    private int roundNumber;

    public PetBattleSimulation(BattleStatsVO pet, BattleStatsVO opponent, int opponentHealth,
                               boolean opponentHealsFromLifesteal, Random random) {
        this.pet = new Fighter(pet, pet.getHealth(), "self", true);
        this.opponent = new Fighter(opponent, opponentHealth, "opponent", opponentHealsFromLifesteal);
        this.random = random;
        this.petUsuallyFirst = BattleStatsVO.aGoesFirst(pet, opponent, random);
    }

    public boolean hasNext() {
        return pet.health > 0 && opponent.health > 0;
    }

    public Action next() {
        List<PetCardActivationVO> activations = new ArrayList<>(2);
        if (attacker == null) {
            startRound(activations);
        } else {
            pet.appliedThisAction = false;
            opponent.appliedThisAction = false;
        }
        Fighter defender = attacker == pet ? opponent : pet;
        AttackResultVO hit = attack(attacker, defender, activations);
        Action action = new Action(roundNumber, attacker == pet, pet.health, opponent.health, hit, activations);
        if (!hit.isCombo()) {
            attacker = lastSlot ? null : defender;
            lastSlot = true;
        }
        return action;
    }

    private void startRound(List<PetCardActivationVO> activations) {
        roundNumber++;
        pet.startRound(draw(pet), activations);
        opponent.startRound(draw(opponent), activations);
        double petSpeed = pet.stat("speed", pet.stats.getSpeed());
        double opponentSpeed = opponent.stat("speed", opponent.stats.getSpeed());
        attacker = (petSpeed == opponentSpeed ? petUsuallyFirst : petSpeed > opponentSpeed) ? pet : opponent;
        lastSlot = false;
    }

    private PetCardPassive draw(Fighter fighter) {
        List<PetCardPassive> cards = fighter.stats.getCardPassives();
        if (cards.isEmpty() || random.nextDouble() >= CARD_CHANCE) return null;
        return cards.get(random.nextInt(cards.size()));
    }

    private AttackResultVO attack(Fighter from, Fighter to, List<PetCardActivationVO> activations) {
        AttackResultVO result = new AttackResultVO();
        from.trigger("firstAttack", activations);
        to.trigger("firstIncomingAttack", activations);
        double dodge = to.stats.getDodgeRate() - from.stats.getDodgeResistance()
                - from.bonus("accuracyBoost", "percentagePoints");
        if (random.nextDouble() < Math.max(0, dodge)) {
            result.setDodge(true);
            return result;
        }

        from.trigger("firstHit", activations);
        boolean block = random.nextDouble() < Math.max(0, to.stats.getBlockRate() - from.stats.getBlockResistance());
        double critChance = from.stats.getCritRate() - to.stats.getCritResistance()
                + from.bonus("criticalChanceBoost", "percentagePoints");
        boolean critical = random.nextDouble() < Math.max(0, critChance) || from.guaranteesCritical();
        boolean combo = random.nextDouble() < Math.max(0, from.stats.getComboRate() - to.stats.getComboResistance());
        if (critical) {
            from.trigger("firstCriticalHit", activations);
            to.trigger("firstIncomingCriticalHit", activations);
        }
        result.setBlock(block);
        result.setCritical(critical);
        result.setCombo(combo);

        double multiplier = 1.0;
        if (critical) multiplier *= BattleConstant.CRITICAL_DAMAGE_MULTIPLIER;
        if (combo) multiplier *= BattleConstant.COMBO_DAMAGE_MULTIPLIER;
        if (block) multiplier *= BattleConstant.BLOCK_DAMAGE_REDUCTION;
        int base = (int) (from.stat("attack", from.stats.getAttack()) * multiplier);
        int afterDefense = Math.max(1, base - (int) to.stat("defense", to.stats.getDefense()));
        double variation = 0.9 + random.nextDouble() * 0.2;
        int outgoingPercent = 100 + from.damagePercent("outgoingDamageBoost");
        int incomingPercent = 100 - to.damagePercent("incomingDamageReduction");
        int impact = incomingPercent == 0 ? 0 : Math.max(1,
                (int) (afterDefense * variation * outgoingPercent * incomingPercent / 10000));
        int damage = Math.min(to.health, to.absorb(impact));
        to.health -= damage;
        result.setDamage(damage);

        if (from.healsFromLifesteal) {
            double lifesteal = Math.max(0, from.stats.getLifesteal() - to.stats.getLifestealResistance());
            long lifestealUnits = Math.round(lifesteal * RATE_SCALE);
            int heal = (int) Math.min((double) damage * lifestealUnits / RATE_SCALE, from.missingHealth());
            from.health += heal;
            result.setLifestealHeal(heal);
        }
        if (critical) to.trigger("afterFirstIncomingCriticalHit", activations);
        return result;
    }

    private static final class Fighter {
        private final BattleStatsVO stats;
        private final String side;
        private final boolean healsFromLifesteal;
        private int health;
        private int shield;
        private PetCardPassive card;
        private JsonNode outcome;
        private boolean triggered;
        private boolean appliedThisAction;
        private boolean appliedAtRoundStart;

        private Fighter(BattleStatsVO stats, int health, String side, boolean healsFromLifesteal) {
            this.stats = stats;
            this.health = health;
            this.side = side;
            this.healsFromLifesteal = healsFromLifesteal;
        }

        private void startRound(PetCardPassive card, List<PetCardActivationVO> activations) {
            this.card = card;
            outcome = null;
            triggered = false;
            appliedThisAction = false;
            shield = 0;
            appliedAtRoundStart = false;
            trigger("turnStart", activations);
            if (card != null && health * 100L < stats.getHealth() * (long)
                    card.getEffect().path("trigger").path("thresholdPercent").asInt()) {
                trigger("healthBelowAtTurnStart", activations);
            }
            appliedAtRoundStart = appliedThisAction;
        }

        private void trigger(String phase, List<PetCardActivationVO> activations) {
            if (card == null || triggered || health == 0 || !phase.equals(card.getEffect().path("trigger").path("kind").asText())) {
                return;
            }
            triggered = true;
            outcome = card.getEffect().path("outcome");
            if (hasOutcome("heal")) {
                int healed = Math.min(missingHealth(), maxHealthAmount());
                if (healed == 0) return;
                health += healed;
            }
            if (hasOutcome("shield")) shield = maxHealthAmount();
            appliedThisAction = true;
            activations.add(new PetCardActivationVO(side, card.getCard(), card.getEffect().path("name").asText()));
        }

        private int maxHealthAmount() {
            return Math.max(1, (int) (stats.getHealth() * (long) outcome.path("maxHealthPercent").asInt() / 100));
        }

        private int missingHealth() {
            return Math.max(0, stats.getHealth() - health);
        }

        private int absorb(int damage) {
            int absorbed = Math.min(shield, damage);
            shield -= absorbed;
            return damage - absorbed;
        }

        private boolean guaranteesCritical() {
            return appliedThisAction && hasOutcome("guaranteedCriticalHit");
        }

        private double stat(String stat, int base) {
            if (!hasOutcome("statBoost")) return base;
            for (JsonNode boosted : outcome.path("stats")) {
                if (stat.equals(boosted.asText())) return base * (100L + outcome.path("percent").asInt()) / 100.0;
            }
            return base;
        }

        private int damagePercent(String kind) {
            return (appliedThisAction || appliedAtRoundStart) && hasOutcome(kind)
                    ? outcome.path("percent").asInt() : 0;
        }

        private boolean hasOutcome(String kind) {
            return outcome != null && kind.equals(outcome.path("kind").asText());
        }

        private double bonus(String kind, String field) {
            return hasOutcome(kind) ? ratio(field) : 0;
        }

        private double ratio(String field) {
            return outcome.path(field).asInt() / 100.0;
        }
    }

    @Value
    public static class Action {
        int roundNumber;
        boolean petAttacks;
        int petHealth;
        int opponentHealth;
        AttackResultVO hit;
        List<PetCardActivationVO> activations;

        public BattleResultVO toChallengeRound(boolean normalIncludesBlock) {
            BattleResultVO result = new BattleResultVO();
            result.setAttackerType(petAttacks ? "PET" : "BOSS");
            result.setRoundNumber(roundNumber);
            result.setDamage(hit.getDamage());
            result.setIsDodge(hit.isDodge());
            result.setIsCritical(hit.isCritical());
            result.setIsCombo(hit.isCombo());
            result.setIsBlock(hit.isBlock());
            result.setLifestealHeal(hit.getLifestealHeal());
            result.setIsNormalAttack(normalAttack(normalIncludesBlock));
            result.setPetRemainingHealth(petHealth);
            result.setBossRemainingHealth(opponentHealth);
            result.setCardActivations(Collections.unmodifiableList(activations));
            return result;
        }

        public PetBattleResultVO toDuelRound() {
            PetBattleResultVO result = new PetBattleResultVO();
            result.setAttackerType(petAttacks ? "MY_PET" : "OPPONENT_PET");
            result.setRoundNumber(roundNumber);
            result.setDamage(hit.getDamage());
            result.setIsDodge(hit.isDodge());
            result.setIsCritical(hit.isCritical());
            result.setIsCombo(hit.isCombo());
            result.setIsBlock(hit.isBlock());
            result.setLifestealHeal(hit.getLifestealHeal());
            result.setIsNormalAttack(normalAttack(false));
            result.setMyPetRemainingHealth(petHealth);
            result.setOpponentPetRemainingHealth(opponentHealth);
            result.setCardActivations(Collections.unmodifiableList(activations));
            return result;
        }

        private boolean normalAttack(boolean normalIncludesBlock) {
            return !hit.isDodge() && !hit.isCritical() && !hit.isCombo() && (normalIncludesBlock || !hit.isBlock());
        }
    }
}

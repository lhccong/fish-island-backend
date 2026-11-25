package com.cong.fishisland.service.pet;

import com.cong.fishisland.model.dto.pet.PetCardPassive;
import com.cong.fishisland.model.dto.pet.PetCardPlay.CollectedCard;
import com.cong.fishisland.model.vo.game.BattleStatsVO;
import com.cong.fishisland.model.vo.game.PetCardActivationVO;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.Collections;
import java.util.Random;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class PetBattleSimulationTest {
    private final ObjectMapper json = new ObjectMapper();

    @Test
    void drawsOneCardAndExpiresItsUnusedShieldWhenNextDrawMisses() throws Exception {
        BattleStatsVO pet = fighter(1);
        BattleStatsVO boss = fighter(0);
        pet.setCardPassives(Arrays.asList(
                card("命中", "turnStart", "{\"kind\":\"accuracyBoost\",\"percentagePoints\":20}"),
                card("屏障", "turnStart", "{\"kind\":\"shield\",\"maxHealthPercent\":25}")));
        Random random = mock(Random.class);
        when(random.nextInt(2)).thenReturn(1);
        when(random.nextDouble()).thenReturn(0.19, 0.5, 0.5, 0.5, 0.5, 0.5, 0.5, 0.5, 0.5, 0.5, 0.5, 0.20, 0.5);
        PetBattleSimulation battle = new PetBattleSimulation(pet, boss, 100, false, random);

        PetBattleSimulation.Action first = battle.next();
        assertEquals(1, first.getRoundNumber());
        assertEquals("屏障", first.getActivations().get(0).getEffectName());
        assertEquals(1, first.getActivations().size());
        assertEquals("self", first.getActivations().get(0).getSide());
        PetBattleSimulation.Action reply = battle.next();
        assertEquals(0, reply.getHit().getDamage());
        assertEquals(1, reply.getRoundNumber());
        PetBattleSimulation.Action secondRound = battle.next();
        assertEquals(2, secondRound.getRoundNumber());
        assertTrue(secondRound.getActivations().isEmpty());
        PetBattleSimulation.Action unshielded = battle.next();
        assertEquals(10, unshielded.getHit().getDamage());
        assertEquals(90, unshielded.getPetHealth());
        verify(random, times(1)).nextInt(2);
    }

    @Test
    void bothPetsCanActivateAndSpeedOnlyChangesTheDrawnRound() throws Exception {
        BattleStatsVO pet = fighter(10);
        BattleStatsVO opponent = fighter(9);
        pet.setCardPassives(Collections.singletonList(
                card("磨爪", "turnStart", "{\"kind\":\"statBoost\",\"stats\":[\"attack\"],\"percent\":20}")));
        opponent.setCardPassives(Collections.singletonList(
                card("抢先", "turnStart", "{\"kind\":\"statBoost\",\"stats\":[\"speed\"],\"percent\":20}")));
        Random random = mock(Random.class);
        when(random.nextDouble()).thenReturn(0.1, 0.1, 0.5);
        PetBattleSimulation battle = new PetBattleSimulation(pet, opponent, 100, true, random);

        PetBattleSimulation.Action first = battle.next();
        assertFalse(first.isPetAttacks());
        assertEquals(2, first.getActivations().size());
        assertEquals("opponent", first.getActivations().get(1).getSide());
        assertEquals(12, battle.next().getHit().getDamage());
        PetBattleSimulation.Action nextRound = battle.next();
        assertTrue(nextRound.isPetAttacks());
        assertEquals(10, nextRound.getHit().getDamage());
        assertTrue(nextRound.getActivations().isEmpty());
    }

    @Test
    void guaranteedCriticalOnlyAppliesToFirstHitEvenDuringACombo() throws Exception {
        BattleStatsVO pet = fighter(1);
        pet.setComboRate(1);
        pet.setCardPassives(Collections.singletonList(
                card("锐爪", "firstHit", "{\"kind\":\"guaranteedCriticalHit\"}")));
        Random random = mock(Random.class);
        when(random.nextDouble()).thenReturn(0.1, 0.5);
        PetBattleSimulation battle = new PetBattleSimulation(pet, fighter(0), 100, true, random);

        PetBattleSimulation.Action first = battle.next();
        assertTrue(first.getHit().isCritical());
        assertTrue(first.getHit().isCombo());
        assertEquals("锐爪", first.getActivations().get(0).getEffectName());
        PetBattleSimulation.Action combo = battle.next();
        assertEquals(first.getRoundNumber(), combo.getRoundNumber());
        assertTrue(combo.isPetAttacks());
        assertFalse(combo.getHit().isCritical());
        assertTrue(combo.getActivations().isEmpty());
        verify(random, times(1)).nextInt(1);
    }

    @Test
    void postCriticalHealIsAppliedAfterDamageAndCannotRevive() throws Exception {
        BattleStatsVO opponent = fighter(1);
        opponent.setCritRate(1);
        BattleStatsVO pet = fighter(0);
        pet.setCardPassives(Collections.singletonList(
                card("援护", "afterFirstIncomingCriticalHit", "{\"kind\":\"heal\",\"maxHealthPercent\":20}")));
        Random random = mock(Random.class);
        when(random.nextDouble()).thenReturn(0.1, 0.5);
        PetBattleSimulation battle = new PetBattleSimulation(pet, opponent, 100, true, random);
        PetBattleSimulation.Action healed = battle.next();
        assertTrue(healed.getHit().isCritical());
        assertEquals(100, healed.getPetHealth());
        assertEquals("援护", healed.getActivations().get(0).getEffectName());

        opponent.setAttack(100);
        when(random.nextDouble()).thenReturn(0.1, 0.5);
        PetBattleSimulation lethal = new PetBattleSimulation(pet, opponent, 100, true, random);
        assertTrue(lethal.next().getActivations().isEmpty());
        assertFalse(lethal.hasNext());
    }

    @Test
    void fullHealthDoesNotDisplayAnUnusedHeal() throws Exception {
        BattleStatsVO pet = fighter(1);
        pet.setCardPassives(Collections.singletonList(
                card("回血", "turnStart", "{\"kind\":\"heal\",\"maxHealthPercent\":6}")));
        Random random = mock(Random.class);
        when(random.nextDouble()).thenReturn(0.1, 0.5);
        PetBattleSimulation battle = new PetBattleSimulation(pet, fighter(0), 100, true, random);
        assertTrue(battle.next().getActivations().isEmpty());
        assertTrue(battle.next().getActivations().isEmpty());
    }

    @Test
    void lowDamageStillHitsForOneUnlessFullyReduced() throws Exception {
        BattleStatsVO pet = fighter(1);
        BattleStatsVO opponent = fighter(0);
        opponent.setDefense(100);
        Random random = mock(Random.class);
        when(random.nextDouble()).thenReturn(0.0);

        PetBattleSimulation.Action regular = new PetBattleSimulation(pet, opponent, 100, true, random).next();
        assertEquals(1, regular.getHit().getDamage());
        assertEquals(99, regular.getOpponentHealth());

        for (int reduction : new int[]{35, 100}) {
            opponent.setCardPassives(Collections.singletonList(card("减伤", "turnStart",
                    "{\"kind\":\"incomingDamageReduction\",\"percent\":" + reduction + "}")));
            PetBattleSimulation.Action mitigated = new PetBattleSimulation(pet, opponent, 100, true, random).next();
            int expected = reduction == 100 ? 0 : 1;
            assertEquals(expected, mitigated.getHit().getDamage());
            assertEquals(100 - expected, mitigated.getOpponentHealth());
            assertEquals(1, mitigated.getActivations().size());
        }
    }

    @Test
    void lifestealRecordsOnlyTheHealthActuallyRestored() {
        BattleStatsVO opponent = fighter(1);
        opponent.setLifesteal(0.5);
        BattleStatsVO pet = fighter(0);
        Random random = mock(Random.class);
        when(random.nextDouble()).thenReturn(0.5);

        PetBattleSimulation.Action sharedBoss = new PetBattleSimulation(pet, opponent, 55, false, random).next();
        assertEquals(10, sharedBoss.getHit().getDamage());
        assertEquals(0, sharedBoss.toChallengeRound(false).getLifestealHeal());
        assertEquals(55, sharedBoss.getOpponentHealth());

        PetBattleSimulation.Action regular = new PetBattleSimulation(pet, opponent, 98, true, random).next();
        assertEquals(2, regular.getHit().getLifestealHeal());
        assertEquals(100, regular.getOpponentHealth());
    }

    @Test
    void lifestealUsesExactConfiguredPrecisionAfterResistance() {
        BattleStatsVO attacker = fighter(1);
        attacker.setAttack(100);
        attacker.setLifesteal(0.30);
        BattleStatsVO defender = fighter(0);
        defender.setLifestealResistance(0.01);
        Random random = mock(Random.class);
        when(random.nextDouble()).thenReturn(0.5);

        PetBattleSimulation.Action hit = new PetBattleSimulation(defender, attacker, 50, true, random).next();
        assertEquals(100, hit.getHit().getDamage());
        assertEquals(29, hit.getHit().getLifestealHeal());
        assertEquals(79, hit.getOpponentHealth());
    }

    @Test
    void damageAbsorbedByShieldDoesNotGrantLifesteal() throws Exception {
        BattleStatsVO pet = fighter(0);
        pet.setLifesteal(1);
        BattleStatsVO opponent = fighter(1);
        opponent.setCardPassives(Collections.singletonList(
                card("屏障", "turnStart", "{\"kind\":\"shield\",\"maxHealthPercent\":25}")));
        Random random = mock(Random.class);
        when(random.nextDouble()).thenReturn(0.1, 0.5);
        PetBattleSimulation battle = new PetBattleSimulation(pet, opponent, 100, true, random);

        assertEquals(90, battle.next().getPetHealth());
        PetBattleSimulation.Action reply = battle.next();
        assertEquals(0, reply.getHit().getDamage());
        assertEquals(0, reply.getHit().getLifestealHeal());
        assertEquals(90, reply.getPetHealth());
        assertEquals(100, reply.getOpponentHealth());
    }

    @Test
    void lowHealthHealRequiresStrictlyLessThanTheThreshold() throws Exception {
        BattleStatsVO pet = fighter(1);
        BattleStatsVO opponent = fighter(0);
        opponent.setCardPassives(Collections.singletonList(new PetCardPassive(
                new CollectedCard("heal", "回血", "epic", 1, "atlas", 0, false),
                json.readTree("{\"kind\":\"effect\",\"name\":\"回血\","
                        + "\"trigger\":{\"kind\":\"healthBelowAtTurnStart\",\"thresholdPercent\":30},"
                        + "\"outcome\":{\"kind\":\"heal\",\"maxHealthPercent\":20}}"))));
        Random random = mock(Random.class);
        when(random.nextDouble()).thenReturn(0.1, 0.5);
        PetBattleSimulation.Action atThreshold = new PetBattleSimulation(pet, opponent, 30, true, random).next();
        assertTrue(atThreshold.getActivations().isEmpty());
        assertEquals(20, atThreshold.getOpponentHealth());

        when(random.nextDouble()).thenReturn(0.1, 0.5);
        PetBattleSimulation.Action belowThreshold = new PetBattleSimulation(pet, opponent, 29, true, random).next();
        assertEquals(1, belowThreshold.getActivations().size());
        assertEquals(39, belowThreshold.getOpponentHealth());
    }

    private BattleStatsVO fighter(int speed) {
        BattleStatsVO fighter = new BattleStatsVO();
        fighter.setAttack(10);
        fighter.setHealth(100);
        fighter.setSpeed(speed);
        return fighter;
    }

    private PetCardPassive card(String name, String trigger, String outcome) throws JsonProcessingException {
        return new PetCardPassive(new CollectedCard(name, name, "epic", 1, "atlas", 0, false),
                json.readTree(String.format("{\"kind\":\"effect\",\"name\":\"%s\",\"trigger\":{\"kind\":\"%s\"},\"outcome\":%s}",
                        name, trigger, outcome)));
    }
}

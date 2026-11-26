package com.cong.fishisland.service.pet;

import com.cong.fishisland.constant.BattleConstant;
import com.cong.fishisland.mapper.PetCardPlayMapper;
import com.cong.fishisland.mapper.PetCardPlayMapper.ShowcaseStatsRow;
import com.cong.fishisland.model.dto.pet.PetCardPlay.CollectedCard;
import com.cong.fishisland.model.entity.pet.FishPet;
import com.cong.fishisland.model.entity.user.User;
import com.cong.fishisland.model.vo.game.BattleStatsVO;
import com.cong.fishisland.model.vo.game.PetBattleInfoVO;
import com.cong.fishisland.model.vo.game.PetBattleResultVO;
import com.cong.fishisland.model.vo.pet.ItemInstanceVO;
import com.cong.fishisland.model.vo.pet.ItemTemplateVO;
import com.cong.fishisland.service.UserService;
import com.cong.fishisland.service.impl.game.PetBattleServiceImpl;
import com.cong.fishisland.service.impl.pet.FishPetServiceImpl;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Random;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class PetCardShowcaseBattleTest {
    @Test
    void bothPetsGetTheirOwnShowcaseBonusesInPreviewAndBattle() {
        PetCardPlayMapper cards = mock(PetCardPlayMapper.class);
        when(cards.showcaseStats(1L)).thenReturn(Arrays.asList(
                card(5, "{\"attack\":9,\"defense\":1,\"health\":10,\"speed\":3}"),
                card(2, "{\"attack\":0,\"health\":8,\"speed\":1}"),
                card(1, "{\"health\":7}"),
                card(1, "{\"health\":6}"),
                card(1, "{\"health\":5}"),
                card(1, "{\"health\":4}")));
        when(cards.showcaseStats(2L)).thenReturn(Collections.singletonList(
                card(1, "{\"attack\":5,\"defense\":2,\"health\":20,\"speed\":4}")));
        FishPet me = new FishPet();
        me.setUserId(1L);
        me.setLevel(1);
        FishPet opponent = new FishPet();
        opponent.setUserId(2L);
        opponent.setLevel(1);
        ItemTemplateVO template = new ItemTemplateVO();
        template.setBaseAttack(10);
        template.setBaseDefense(2);
        template.setBaseHp(10);
        template.setBaseSpeed(0);
        ItemInstanceVO item = new ItemInstanceVO();
        item.setTemplate(template);
        FishPetServiceImpl pets = spy(new FishPetServiceImpl(null, null, null, null, null, null, null, null,
                null, null, new PetCardShowcaseStats(cards, new ObjectMapper())));
        doReturn(me, opponent, me, opponent).when(pets).getOne(any());
        doReturn(Collections.singletonMap("weapon", item)).when(pets).getEquippedItems(me);

        UserService users = mock(UserService.class);
        User user = new User();
        user.setId(1L);
        when(users.getLoginUser()).thenReturn(user);
        PetBattleServiceImpl battle = new PetBattleServiceImpl(users, pets);
        PetBattleInfoVO preview = battle.getPetBattleInfo(2L);
        int baseAttack = (int) (BattleConstant.BASE_ATK * (1 + BattleConstant.GROWTH_RATE));
        assertEquals(baseAttack + 23, preview.getMyPet().getAttack());
        assertEquals(159, preview.getMyPet().getHealth());
        assertEquals(baseAttack + 5, preview.getOpponentPet().getAttack());
        assertEquals(120, preview.getOpponentPet().getHealth());
        assertEquals(6, preview.getMyPet().getShowcaseCards().size());
        assertEquals(1, preview.getOpponentPet().getShowcaseCards().size());
        assertEquals(Integer.valueOf(18), preview.getMyPet().getShowcaseCards().get(0).getStats().get("health"));
        assertNull(preview.getMyPet().getShowcaseCards().get(0).getEffect());
        verify(cards).showcaseStats(1L);
        verify(cards).showcaseStats(2L);
        assertEquals(7, pets.getPetEquipStatsByPet(me).getTotalBaseDefense());
        assertEquals(9, pets.getPetEquipStatsByPet(me).getSpeed());

        PetBattleResultVO first = battle.battle(2L).get(0);
        assertEquals("MY_PET", first.getAttackerType());
        assertTrue(first.getDamage() > 0);
        assertEquals(159, first.getMyPetRemainingHealth());
        assertEquals(120 - first.getDamage(), first.getOpponentPetRemainingHealth());
        assertTrue(first.getCardActivations().isEmpty());

        when(cards.showcaseStats(2L)).thenReturn(Collections.singletonList(new ShowcaseStatsRow(
                new CollectedCard("shield", "潮月守卫", "epic", 1, "atlas", 0, false),
                "{\"health\":20,\"speed\":4}",
                "{\"kind\":\"effect\",\"name\":\"潮月护体\",\"turnOffset\":1,\"trigger\":{\"kind\":\"turnStart\"},"
                        + "\"outcome\":{\"kind\":\"shield\",\"maxHealthPercent\":100}}")));
        doReturn(me, opponent, me, opponent).when(pets).getOne(any());
        PetBattleInfoVO effectPreview = battle.getPetBattleInfo(2L);
        assertEquals("潮月护体",
                effectPreview.getOpponentPet().getShowcaseCards().get(0).getEffect().path("name").asText());
        Random draw = mock(Random.class);
        when(draw.nextDouble()).thenReturn(0.1, 0.5);
        ReflectionTestUtils.setField(battle, "random", draw);

        PetBattleResultVO shielded = battle.battle(2L).get(0);
        assertEquals("MY_PET", shielded.getAttackerType());
        assertEquals(0, shielded.getDamage());
        assertEquals(120, shielded.getOpponentPetRemainingHealth());
        assertEquals(1, shielded.getCardActivations().size());
        assertEquals("opponent", shielded.getCardActivations().get(0).getSide());
        assertEquals("shield", shielded.getCardActivations().get(0).getCard().getId());
    }

    @Test
    void bondsRequireDistinctShowcasedCardsAndStackAcrossTiersAndGroups() {
        PetCardPlayMapper cards = mock(PetCardPlayMapper.class);
        when(cards.showcaseStats(1L)).thenReturn(
                lineup("jianYong", "jianYong", "feiYi", "radish", "radish", "penguin"),
                lineup("jianYong", "feiYi", "maLiang"),
                lineup("jianYong", "feiYi", "maLiang", "weiYan", "jiangWan", "liaoHua"),
                lineup("jianYong", "feiYi", "maLiang", "liDian", "yuJin", "yueJin"),
                lineup("feiYi", "maLiang", "liDian", "yuJin", "yueJin"));
        when(cards.showcaseStats(2L)).thenReturn(Collections.emptyList());
        FishPetServiceImpl pets = withShowcase(cards);

        assertEquals(0.0, fighter(pets, 1L).getComboRate());
        BattleStatsVO three = fighter(pets, 1L);
        assertEquals(0.05, three.getComboRate(), 0.00001);
        assertEquals(0.04, three.getCritRate(), 0.00001);
        assertEquals(0.0, three.getComboResistance());
        BattleStatsVO six = fighter(pets, 1L);
        assertEquals(three.getComboRate(), six.getComboRate());
        assertEquals(three.getCritRate(), six.getCritRate());
        assertEquals(0.06, six.getComboResistance(), 0.00001);
        assertEquals(0.06, six.getCritResistance(), 0.00001);
        assertEquals(three.getAttack(), six.getAttack());
        assertEquals(three.getHealth(), six.getHealth());
        assertEquals(0.0, fighter(pets, 2L).getComboRate());
        BattleStatsVO mixed = fighter(pets, 1L);
        assertEquals(0.05, mixed.getComboRate(), 0.00001);
        assertEquals(0.05, mixed.getLifesteal(), 0.00001);
        assertEquals(0.08, mixed.getCritRate(), 0.00001);
        BattleStatsVO removed = fighter(pets, 1L);
        assertEquals(0.0, removed.getComboRate());
        assertEquals(0.05, removed.getLifesteal(), 0.00001);
    }

    @Test
    void islandBondCountsDifferentIslandSeriesAndAllowsSameFactionReplacement() {
        PetCardPlayMapper cards = mock(PetCardPlayMapper.class);
        when(cards.showcaseStats(1L)).thenReturn(
                lineup("radish", "seahorse", "frog", "penguin", "cloudbird", "manta"),
                lineup("radish", "shellTurtle", "frog", "penguin", "cloudbird", "manta"),
                lineup("radish", "shellTurtle", "frog", "penguin", "cloudbird", "luBu"));
        FishPetServiceImpl pets = withShowcase(cards);

        BattleStatsVO six = fighter(pets, 1L);
        assertEquals(0.04, six.getLifesteal(), 0.00001);
        assertEquals(0.04, six.getDodgeRate(), 0.00001);
        assertEquals(0.05, six.getLifestealResistance(), 0.00001);
        assertEquals(0.05, six.getDodgeResistance(), 0.00001);
        BattleStatsVO replaced = fighter(pets, 1L);
        assertEquals(six.getDodgeRate(), replaced.getDodgeRate());
        assertEquals(six.getDodgeResistance(), replaced.getDodgeResistance());
        BattleStatsVO mixed = fighter(pets, 1L);
        assertEquals(six.getDodgeRate(), mixed.getDodgeRate());
        assertEquals(0.0, mixed.getDodgeResistance());
        assertEquals(0.0, mixed.getCritRate());
    }

    @Test
    void showcaseBonusesReachTheSharedCombatEngine() {
        PetCardPlayMapper cards = mock(PetCardPlayMapper.class);
        when(cards.showcaseStats(1L)).thenReturn(lineup("jianYong", "feiYi", "maLiang"));
        when(cards.showcaseStats(2L)).thenReturn(
                lineup("ganNing", "luMeng", "xuSheng", "sunQuan", "zhouYu", "luXun"));
        when(cards.showcaseStats(3L)).thenReturn(
                lineup("zhangLiao", "xiahouDun", "caoHong", "caoCao", "xuChu", "dianWei"));
        when(cards.showcaseStats(4L)).thenReturn(Collections.emptyList());
        FishPetServiceImpl pets = withShowcase(cards);
        BattleStatsVO shu = fighter(pets, 1L);
        BattleStatsVO wu = fighter(pets, 2L);
        BattleStatsVO wei = fighter(pets, 3L);
        BattleStatsVO plain = fighter(pets, 4L);

        Random draw = mock(Random.class);
        when(draw.nextBoolean()).thenReturn(true);
        when(draw.nextDouble()).thenReturn(0.03);
        PetBattleSimulation.Action attack = new PetBattleSimulation(shu, plain, 100, true, draw).next();
        assertTrue(attack.getHit().isCritical());
        assertTrue(attack.getHit().isCombo());
        assertTrue(new PetBattleSimulation(plain, wu, 100, true, draw).next().getHit().isDodge());
        assertFalse(new PetBattleSimulation(wu, wu, 100, true, draw).next().getHit().isDodge());
        assertFalse(new PetBattleSimulation(shu, wei, 100, true, draw).next().getHit().isCritical());

        wei.setSpeed(1);
        wei.setAttack(100);
        when(draw.nextDouble()).thenReturn(0.5);
        assertEquals(5, new PetBattleSimulation(plain, wei, 50, true, draw).next().getHit().getLifestealHeal());
        BattleStatsVO defender = fighter(pets, 3L);
        assertEquals(0, new PetBattleSimulation(defender, wei, 50, true, draw).next().getHit().getLifestealHeal());
    }

    private static FishPetServiceImpl withShowcase(PetCardPlayMapper cards) {
        return new FishPetServiceImpl(null, null, null, null, null, null, null, null,
                null, null, new PetCardShowcaseStats(cards, new ObjectMapper()));
    }

    private static BattleStatsVO fighter(FishPetServiceImpl pets, long userId) {
        FishPet pet = new FishPet();
        pet.setUserId(userId);
        pet.setLevel(1);
        return BattleStatsVO.fromPet(1, pets.getPetEquipStatsByPet(pet), BattleConstant.BASE_ATK,
                BattleConstant.GROWTH_RATE);
    }

    private static List<ShowcaseStatsRow> lineup(String... ids) {
        return Arrays.stream(ids).map(id -> card(id, 1, "{}")).collect(Collectors.toList());
    }

    private static ShowcaseStatsRow card(int stars, String stats) {
        return card("card", stars, stats);
    }

    private static ShowcaseStatsRow card(String id, int stars, String stats) {
        return new ShowcaseStatsRow(new CollectedCard(id, "卡片", "common", stars, "atlas", 0, false),
                stats, "{\"kind\":\"action\",\"phase\":\"afterReveal\",\"action\":\"attack\"}");
    }
}

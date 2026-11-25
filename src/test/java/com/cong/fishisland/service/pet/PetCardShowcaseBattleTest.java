package com.cong.fishisland.service.pet;

import com.cong.fishisland.constant.BattleConstant;
import com.cong.fishisland.mapper.PetCardPlayMapper;
import com.cong.fishisland.mapper.PetCardPlayMapper.ShowcaseStatsRow;
import com.cong.fishisland.model.dto.pet.PetCardPlay.CollectedCard;
import com.cong.fishisland.model.entity.pet.FishPet;
import com.cong.fishisland.model.entity.user.User;
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
import java.util.Random;

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
        doReturn(me, opponent).when(pets).getOne(any());
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

    private static ShowcaseStatsRow card(int stars, String stats) {
        return new ShowcaseStatsRow(new CollectedCard("card", "卡片", "common", stars, "atlas", 0, false),
                stats, "{\"kind\":\"action\",\"phase\":\"afterReveal\",\"action\":\"attack\"}");
    }
}

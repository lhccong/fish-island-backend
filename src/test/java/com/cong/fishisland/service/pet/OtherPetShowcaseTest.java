package com.cong.fishisland.service.pet;

import com.cong.fishisland.mapper.PetCardPlayMapper;
import com.cong.fishisland.mapper.PetCardPlayMapper.ShowcaseStatsRow;
import com.cong.fishisland.mapper.pet.PetEquipForgeMapper;
import com.cong.fishisland.model.dto.pet.PetCardPlay.CollectedCard;
import com.cong.fishisland.model.entity.pet.FishPet;
import com.cong.fishisland.service.impl.pet.FishPetServiceImpl;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;

import java.util.Collections;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class OtherPetShowcaseTest {
    @Test
    void profileSerializesCardDetailsFromTheSameSnapshotAsTheStats() {
        ObjectMapper json = new ObjectMapper();
        PetCardPlayMapper cards = mock(PetCardPlayMapper.class);
        CollectedCard card = new CollectedCard("shield", "潮月守卫", "epic", 3, "atlas", 5, false);
        when(cards.showcaseStats(2L)).thenReturn(Collections.singletonList(new ShowcaseStatsRow(card,
                "{\"health\":25}",
                "{\"kind\":\"effect\",\"name\":\"潮月护体\",\"turnOffset\":1,\"trigger\":{\"kind\":\"turnStart\"},"
                        + "\"outcome\":{\"kind\":\"shield\",\"maxHealthPercent\":20}}")));
        FishPet pet = new FishPet();
        pet.setPetId(17L);
        pet.setUserId(2L);
        PetEquipForgeMapper forge = mock(PetEquipForgeMapper.class);
        FishPetServiceImpl pets = spy(new FishPetServiceImpl(null, null, null, null, null, null, null,
                forge, null, null, new PetCardShowcaseStats(cards, json)));
        doReturn(pet).when(pets).getOne(any());
        doReturn(Collections.emptyList()).when(pets).getPetSkins(17L);

        JsonNode profile = json.valueToTree(pets.getOtherUserPet(2L));
        assertEquals(1, profile.path("showcaseDetails").size());
        JsonNode detail = profile.path("showcaseDetails").get(0);
        assertEquals(3, detail.path("card").path("stars").asInt());
        assertEquals("潮月护体", detail.path("effect").path("name").asText());
        assertEquals(35, detail.path("stats").path("health").asInt());
        assertEquals(35, profile.path("equipStats").path("totalBaseHp").asInt());
        assertFalse(profile.path("equipStats").has("cardPassives"));
        verify(cards).showcaseStats(2L);

        when(cards.showcaseStats(2L)).thenReturn(Collections.emptyList());
        JsonNode empty = json.valueToTree(pets.getOtherUserPet(2L));
        assertTrue(empty.path("showcaseDetails").isArray());
        assertEquals(0, empty.path("showcaseDetails").size());
    }
}

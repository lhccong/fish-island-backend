package com.cong.fishisland.service.pet;

import cn.dev33.satoken.annotation.SaCheckRole;
import com.cong.fishisland.common.exception.BusinessException;
import com.cong.fishisland.constant.UserConstant;
import com.cong.fishisland.controller.admin.PetCardAdminController;
import com.cong.fishisland.model.dto.pet.PetCardCatalog.Card;
import com.cong.fishisland.model.dto.pet.PetCardCatalog.Pool;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.core.io.ClassPathResource;

import java.io.InputStream;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

class PetCardCatalogTest {
    private final ObjectMapper json = new ObjectMapper();

    @Test
    void sourceCatalogContainsAllOriginalCardsAndAtlasSlots() throws Exception {
        JsonNode seed;
        try (InputStream input = new ClassPathResource("pet-cards/original-catalog.json").getInputStream()) {
            seed = json.readTree(input);
        }
        Set<String> ids = new HashSet<>();
        Map<String, Set<Integer>> slots = new HashMap<>();
        Map<String, Integer> membership = new HashMap<>();
        for (JsonNode entry : seed.path("cards")) {
            Card card = json.treeToValue(entry.path("card"), Card.class);
            card.setArtKey(String.format("%064d.jpg", 0));
            PetCardCatalogRules.validateCard(card);
            assertTrue(ids.add(card.getId()));
            assertTrue(slots.computeIfAbsent(entry.path("atlas").asText(), ignored -> new HashSet<>()).add(card.getArtTile()));
            card.getPoolIds().forEach(pool -> membership.merge(pool, 1, Integer::sum));
            if ("fox".equals(card.getId())) {
                assertEquals("guaranteedCriticalHit", card.getBattle().path("outcome").path("kind").asText());
            }
        }
        assertEquals(100, ids.size());
        assertEquals(10, slots.size());
        assertTrue(slots.values().stream().allMatch(group -> group.size() == 10));
        assertEquals(10, seed.path("atlases").size());
        assertEquals(2, seed.path("pools").size());
        for (JsonNode entry : seed.path("pools")) {
            Pool pool = json.treeToValue(entry, Pool.class);
            PetCardCatalogRules.validatePool(pool);
            assertEquals(50, membership.get(pool.getId()).intValue());
            assertTrue(ids.contains(pool.getCoverId()));
        }
    }

    @Test
    void adminControllerIsRoleRestrictedAndImportRejectsInvalidAtlasKeys() {
        SaCheckRole role = PetCardAdminController.class.getAnnotation(SaCheckRole.class);
        assertNotNull(role);
        assertTrue(Arrays.asList(role.value()).contains(UserConstant.ADMIN_ROLE));
        assertFalse(PetCardAtlasStore.isAtlasKey("../private.jpg"));
        assertThrows(BusinessException.class, () -> PetCardCatalogRules.validateId("invalid/code"));
    }
}

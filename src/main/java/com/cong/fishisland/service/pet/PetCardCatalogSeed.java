package com.cong.fishisland.service.pet;

import com.cong.fishisland.model.dto.pet.PetCardCatalog.Atlas;
import com.cong.fishisland.model.dto.pet.PetCardCatalog.Card;
import com.cong.fishisland.model.dto.pet.PetCardCatalog.Pool;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Component
@RequiredArgsConstructor
@ConditionalOnProperty(name = "fish.pet-card.seed-enabled", havingValue = "true")
public class PetCardCatalogSeed implements ApplicationRunner {
    private final PetCardCatalogStore store;
    private final PetCardAtlasStore images;
    private final ObjectMapper json;
    @Value("${fish.pet-card.seed-dir:}")
    private String directory;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void run(ApplicationArguments args) throws Exception {
        if (!store.startSeed("original-two-packs-v1")) return;
        PetCardCatalogRules.require(!directory.trim().isEmpty(), "请设置卡牌图集种子目录");
        Path root = Paths.get(directory).toAbsolutePath().normalize();
        JsonNode seed;
        try (InputStream input = new ClassPathResource("pet-cards/original-catalog.json").getInputStream()) {
            seed = json.readTree(input);
        }
        Map<String, Atlas> atlases = importAtlases(seed.path("atlases"), root);
        List<Card> cards = new ArrayList<>();
        for (JsonNode entry : seed.path("cards")) {
            Card card = json.treeToValue(entry.path("card"), Card.class);
            card.setArtKey(atlases.get(entry.path("atlas").asText()).getId());
            PetCardCatalogRules.validateCard(card);
            store.saveCard(card);
            cards.add(card);
        }
        for (JsonNode entry : seed.path("pools")) {
            Pool pool = json.treeToValue(entry, Pool.class);
            PetCardCatalogRules.validatePool(pool);
            store.savePool(pool);
        }
        cards.forEach(card -> store.setMemberships(card, Collections.emptyList()));
    }

    private Map<String, Atlas> importAtlases(JsonNode data, Path root) throws Exception {
        Map<String, Atlas> atlases = new HashMap<>();
        for (JsonNode entry : data) {
            String file = entry.path("file").asText();
            boolean portrait = entry.path("portrait").asBoolean();
            Path path = root.resolve(file).normalize();
            PetCardCatalogRules.require(root.equals(path.getParent()) && Files.size(path) <= PetCardAtlasStore.MAX_BYTES,
                    "种子图集路径或大小无效");
            String id = images.write(Files.readAllBytes(path), portrait, true);
            atlases.put(file, store.registerAtlas(new Atlas(id, entry.path("name").asText(), portrait)));
        }
        return atlases;
    }
}

package com.cong.fishisland.service.pet;

import com.cong.fishisland.common.exception.BusinessException;
import com.cong.fishisland.config.MinioConfig;
import com.cong.fishisland.controller.pet.PetCardCatalogController;
import com.cong.fishisland.manager.MinioManager;
import com.cong.fishisland.model.dto.pet.PetCardCatalog.Atlas;
import com.cong.fishisland.model.dto.pet.PetCardCatalog.Card;
import com.cong.fishisland.model.dto.pet.PetCardCatalog.Pool;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.minio.MakeBucketArgs;
import io.minio.MinioClient;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import org.springframework.core.io.FileSystemResource;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.jdbc.datasource.DataSourceTransactionManager;
import org.springframework.jdbc.datasource.DriverManagerDataSource;
import org.springframework.jdbc.datasource.init.ResourceDatabasePopulator;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.transaction.support.TransactionTemplate;
import org.springframework.util.StreamUtils;
import org.springframework.util.ReflectionUtils;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.lang.reflect.Field;
import java.util.Arrays;
import java.util.Collections;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

@EnabledIfEnvironmentVariable(named = "PET_CARD_VERIFY_JDBC", matches = "jdbc:mysql://127\\.0\\.0\\.1:[0-9]+/pet_catalog.*")
class PetCardCatalogDatabaseTest {
    @Test
    void importsAndReadsCatalogAndSavesVersionedConfiguration() throws Exception {
        DriverManagerDataSource source = new DriverManagerDataSource(System.getenv("PET_CARD_VERIFY_JDBC"), "root", "");
        new ResourceDatabasePopulator(new FileSystemResource("sql/20261122_pet_card_catalog.sql"),
                new FileSystemResource("sql/20261123_pet_card_play.sql")).execute(source);
        JdbcTemplate jdbc = new JdbcTemplate(source);
        ObjectMapper json = new ObjectMapper();
        PetCardCatalogStore store = new PetCardCatalogStore(jdbc, new NamedParameterJdbcTemplate(source), json);
        TransactionTemplate transactions = new TransactionTemplate(new DataSourceTransactionManager(source));
        PetCardAtlasStore images = new PetCardAtlasStore(objectStorage());
        PetCardCatalogSeed seed = new PetCardCatalogSeed(store, images, json);
        ReflectionTestUtils.setField(seed, "directory", System.getenv("PET_CARD_VERIFY_ASSETS"));
        transactions.executeWithoutResult(status -> applySeed(seed));
        transactions.executeWithoutResult(status -> applySeed(seed));
        PetCardCatalogService service = new PetCardCatalogService(store, images);
        PetCardCatalogController publicApi = new PetCardCatalogController(service, images);
        assertEquals(100, publicApi.catalog().getData().getCards().size());
        assertEquals(2, publicApi.catalog().getData().getPools().size());
        assertEquals(10, service.atlases().size());
        assertEquals(100, jdbc.queryForObject("SELECT COUNT(*) FROM pet_card_pool_entry", Integer.class).intValue());
        assertEquals(50, service.cards("island", "", 1).getTotal());
        assertEquals(20, service.cards("kingdoms", "", 2).getItems().size());
        for (Atlas atlas : service.atlases()) {
            try (InputStream input = publicApi.art(atlas.getId()).getBody().getInputStream()) {
                assertNotNull(input);
                assertTrue(StreamUtils.copyToByteArray(input).length > 10000);
            }
        }
        ByteArrayOutputStream png = new ByteArrayOutputStream();
        ImageIO.write(new BufferedImage(500, 200, BufferedImage.TYPE_INT_RGB), "png", png);
        Atlas uploaded = service.uploadAtlas("验证十卡图集", false, png.toByteArray());
        assertThrows(BusinessException.class, () -> service.uploadAtlas("比例错误", true, png.toByteArray()));
        assertEquals(uploaded.getId(), service.uploadAtlas("重复上传", false, png.toByteArray()).getId());
        try (InputStream input = images.read(uploaded.getId())) {
            assertArrayEquals(png.toByteArray(), StreamUtils.copyToByteArray(input));
        }
        Pool pool = service.pools().get(0);
        pool.setName("海岛验证池");
        transactions.executeWithoutResult(status -> service.savePool(pool));
        assertThrows(BusinessException.class, () -> transactions.executeWithoutResult(status -> service.savePool(pool)));
        Card card = service.cards("island", "拾贝萝卜", 1).getItems().get(0);
        card.setArtKey(uploaded.getId());
        card.setArtTile(9);
        card.setPoolIds(Arrays.asList("island", "kingdoms"));
        transactions.executeWithoutResult(status -> service.saveCard(card));
        assertEquals(51, service.cards("kingdoms", "", 1).getTotal());
        Card saved = service.cards("kingdoms", "radish", 1).getItems().get(0);
        assertEquals(9, saved.getArtTile().intValue());
        assertEquals(uploaded.getId(), saved.getArtKey());
        assertThrows(BusinessException.class, () -> transactions.executeWithoutResult(status -> service.saveCard(card)));

        Pool expansion = json.convertValue(pool, Pool.class);
        expansion.setId("verificationExpansion");
        expansion.setCoverId(null);
        expansion.setVersion(null);
        transactions.executeWithoutResult(status -> service.savePool(expansion));
        Card newCard = json.convertValue(saved, Card.class);
        newCard.setId("verificationCard");
        newCard.setVersion(null);
        newCard.setPoolIds(Collections.singletonList(expansion.getId()));
        transactions.executeWithoutResult(status -> service.saveCard(newCard));
        assertEquals(1, service.cards(expansion.getId(), "", 1).getTotal());
        Card onlyMember = service.cards(expansion.getId(), "", 1).getItems().get(0);
        onlyMember.setPoolIds(Collections.emptyList());
        assertThrows(BusinessException.class, () -> transactions.executeWithoutResult(status -> service.saveCard(onlyMember)));
        assertEquals(1, service.cards(expansion.getId(), "", 1).getTotal());
    }

    private static void applySeed(PetCardCatalogSeed seed) {
        try {
            seed.run(null);
        } catch (Exception e) {
            throw new IllegalStateException(e);
        }
    }

    private static MinioManager objectStorage() throws Exception {
        String endpoint = System.getenv("PET_CARD_VERIFY_MINIO");
        assertNotNull(endpoint);
        assertTrue(endpoint.startsWith("http://127.0.0.1:"));
        MinioConfig config = new MinioConfig();
        config.setEndpoint(endpoint);
        config.setAccesskey(System.getenv("PET_CARD_VERIFY_MINIO_USER"));
        config.setSecretKey(System.getenv("PET_CARD_VERIFY_MINIO_PASSWORD"));
        config.setBucketName("pet-cards-" + UUID.randomUUID().toString().substring(0, 8));
        config.setRegion("us-east-1");
        MinioClient client = config.minioClient();
        client.makeBucket(MakeBucketArgs.builder().bucket(config.getBucketName()).build());
        MinioManager manager = new MinioManager();
        setField(manager, "minioClient", client);
        setField(manager, "minioConfig", config);
        return manager;
    }

    private static void setField(MinioManager manager, String name, Object value) {
        Field field = ReflectionUtils.findField(MinioManager.class, name);
        assertNotNull(field);
        ReflectionUtils.makeAccessible(field);
        ReflectionUtils.setField(field, manager, value);
    }
}

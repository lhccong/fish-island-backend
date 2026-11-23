package com.cong.fishisland.service.pet;

import com.cong.fishisland.common.exception.BusinessException;
import com.cong.fishisland.model.dto.pet.PetCardCatalog.Card;
import com.cong.fishisland.model.dto.pet.PetCardPlay.Command;
import com.cong.fishisland.model.dto.pet.PetCardPlay.Opening;
import com.cong.fishisland.model.dto.pet.PetCardPlay.State;
import com.cong.fishisland.model.entity.user.UserPoints;
import com.cong.fishisland.service.UserPointsService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import org.springframework.core.io.FileSystemResource;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.jdbc.datasource.DataSourceTransactionManager;
import org.springframework.jdbc.datasource.DriverManagerDataSource;
import org.springframework.jdbc.datasource.init.ResourceDatabasePopulator;
import org.springframework.transaction.support.TransactionTemplate;

import java.time.Instant;
import java.util.Collections;
import java.util.UUID;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@EnabledIfEnvironmentVariable(named = "PET_CARD_PLAY_VERIFY_JDBC", matches = "jdbc:mysql://127\\.0\\.0\\.1:[0-9]+/pet_catalog_play.*")
class PetCardPlayDatabaseTest {
    private static JdbcTemplate jdbc;
    private static PetCardCatalogStore catalog;
    private static PetCardPlayService service;
    private static DataSourceTransactionManager transactions;

    @BeforeAll
    static void prepare() {
        DriverManagerDataSource dataSource = new DriverManagerDataSource(
                System.getenv("PET_CARD_PLAY_VERIFY_JDBC"), "root", "");
        new ResourceDatabasePopulator(new FileSystemResource("sql/20261122_pet_card_catalog.sql")).execute(dataSource);
        jdbc = new JdbcTemplate(dataSource);
        jdbc.update("INSERT INTO pet_card_pool (id,name,weights) VALUES ('island','海岛初遇',JSON_OBJECT('common',1)),"
                + "('kingdoms','三国群英',JSON_OBJECT('common',1))");
        new ResourceDatabasePopulator(new FileSystemResource("sql/20261123_pet_card_play.sql")).execute(dataSource);
        String atlas = String.format("%064d.jpg", 0);
        jdbc.update("INSERT INTO pet_card_atlas (id,name,portrait) VALUES (?,'图集',0)", atlas);
        jdbc.update("INSERT INTO pet_card_definition (id,name,grade,stats,battle,art_key,art_tile) VALUES "
                + "('commonCard','普通卡','common',JSON_OBJECT('health',5),JSON_OBJECT(),?,0),"
                + "('shinyCard','闪耀卡','shiny',JSON_OBJECT('health',10),?,?,1)", atlas,
                "{\"kind\":\"effect\",\"name\":\"暴击\",\"turnOffset\":1,\"trigger\":{\"kind\":\"firstHit\"},"
                        + "\"outcome\":{\"kind\":\"guaranteedCriticalHit\"}}", atlas);
        jdbc.update("INSERT INTO pet_card_pool_entry VALUES ('island','commonCard'),('island','shinyCard'),"
                + "('kingdoms','commonCard'),('kingdoms','shinyCard')");
        jdbc.execute("CREATE TABLE test_card_wallet (user_id BIGINT PRIMARY KEY, spent INT NOT NULL) ENGINE=InnoDB");
        jdbc.update("INSERT INTO test_card_wallet VALUES (8,0),(9,0)");
        UserPointsService points = mock(UserPointsService.class);
        when(points.getById(anyLong())).thenAnswer(call -> {
            UserPoints wallet = new UserPoints();
            wallet.setPoints(1000);
            wallet.setUsedPoints(jdbc.queryForObject("SELECT spent FROM test_card_wallet WHERE user_id=?",
                    Integer.class, call.getArgument(0, Long.class)));
            return wallet;
        });
        doAnswer(call -> {
            call.getArgument(1, Runnable.class).run();
            return null;
        }).when(points).runWithUserPointsLocks(any(Long[].class), any(Runnable.class));
        doAnswer(call -> {
            jdbc.update("UPDATE test_card_wallet SET spent=spent+? WHERE user_id=?",
                    call.getArgument(1, Integer.class), call.getArgument(0, Long.class));
            if (call.getArgument(0, Long.class) == 9) throw new IllegalStateException("simulated points ledger failure");
            return null;
        }).when(points).updateUsedPoints(anyLong(), anyInt(), eq("pet_card_pack"), anyString(), anyString());
        ObjectMapper json = new ObjectMapper();
        catalog = new PetCardCatalogStore(jdbc, new NamedParameterJdbcTemplate(dataSource), json);
        transactions = new DataSourceTransactionManager(dataSource);
        service = new PetCardPlayService(new PetCardPlayStore(jdbc, json), catalog, points, transactions, json);
    }

    @Test
    void claimsStocksOpensLevelsUpAndKeepsHistoryAndMaterials() throws Exception {
        assertEquals(2, service.state(8).getFreeRemaining());
        Command free = action("claim", "island");
        service.submit(8, free);
        assertEquals(1, service.submit(8, free).getFreeRemaining());
        ExecutorService threads = Executors.newFixedThreadPool(2);
        try {
            Future<Boolean> first = threads.submit(() -> claim(8));
            Future<Boolean> second = threads.submit(() -> claim(8));
            assertNotEquals(first.get(5, TimeUnit.SECONDS), second.get(5, TimeUnit.SECONDS));
        } finally {
            threads.shutdownNow();
        }
        State before = service.state(8);
        assertEquals(0, before.getFreeRemaining());
        assertEquals(2, before.getStock().get(0).getQuantity());
        assertThrows(BusinessException.class, () -> service.submit(8, action("claim", "kingdoms")));
        assertThrows(BusinessException.class, () -> service.submit(8, buy("kingdoms", 100, 1)));
        assertThrows(BusinessException.class, () -> service.submit(8, buy("island", 49, 1)));

        Command buyTen = buy("island", 50, 10);
        assertEquals(550, service.submit(8, buyTen).getBalance());
        assertEquals(12, service.submit(8, buyTen).getStock().get(0).getQuantity());
        assertEquals(500, service.submit(8, buy("island", 50, 1)).getBalance());

        Command open = action("open", "island");
        State opened = service.submit(8, open);
        Opening pending = opened.getPending();
        assertEquals(5, pending.getCards().size());
        assertEquals("shinyCard", pending.getCards().get(4).getCardId());
        assertEquals(3, opened.getCollection().get(0).getDuplicates());
        assertTrue(Instant.parse(pending.getOpenedAt()).isBefore(Instant.now().plusSeconds(1)));
        assertEquals(pending.getId(), service.submit(8, open).getPending().getId());
        assertEquals(pending.getId(), service.state(8).getPending().getId());
        assertThrows(BusinessException.class, () -> service.submit(8, action("open", "island")));
        finish(8, pending);

        for (int stars = 1; stars <= 2; stars++) {
            Command upgrade = action("upgrade", null);
            upgrade.setCardId("commonCard");
            upgrade.setStars(stars);
            assertEquals(stars + 1, service.submit(8, upgrade).getCollection().get(0).getStars());
        }
        assertEquals(0, service.state(8).getCollection().get(0).getDuplicates());
        assertEquals(4, service.state(8).getCollection().get(0).getUpgradeCost().intValue());
        jdbc.update("UPDATE pet_card_player SET claim_day='2020-01-01' WHERE user_id=8");
        assertEquals(2, service.state(8).getFreeRemaining());
        assertEquals(12, service.state(8).getStock().get(0).getQuantity());

        jdbc.update("UPDATE pet_card_player SET opened=39 WHERE user_id=8");
        assertEquals(9, service.state(8).getLevel());
        assertEquals(1, service.state(8).getXpToNext());
        assertThrows(BusinessException.class, () -> service.submit(8, buy("kingdoms", 100, 1)));
        finish(8, service.submit(8, action("open", "island")).getPending());
        assertEquals(10, service.state(8).getLevel());
        assertEquals(6, service.state(8).getXpToNext());
        assertEquals(400, service.submit(8, buy("kingdoms", 100, 1)).getBalance());
        for (int i = 0; i < 20; i++) {
            jdbc.update("INSERT INTO pet_card_opening (user_id, pool_id, pool_name, opened_at, cards) "
                    + "SELECT user_id, pool_id, pool_name, opened_at, cards FROM pet_card_opening WHERE id=?", pending.getId());
        }
        String cursor = service.history(8, null).getNextCursor();
        assertNotNull(cursor);
        assertEquals(20, service.history(8, null).getItems().size());
        assertEquals(2, service.history(8, cursor).getItems().size());
        assertTrue(service.history(9, null).getItems().isEmpty());
        assertTrue(service.state(9).getCollection().isEmpty());

        Opening kingdoms = service.submit(8, action("open", "kingdoms")).getPending();
        assertTrue(kingdoms.getCards().stream().allMatch(card -> "commonCard".equals(card.getCardId())));
        finish(8, kingdoms);

        jdbc.update("UPDATE pet_card_player SET opened=2306 WHERE user_id=8");
        assertEquals(59, service.state(8).getLevel());
        assertEquals(1, service.state(8).getXpToNext());
        State capped = service.submit(8, action("open", "island"));
        assertEquals(60, capped.getLevel());
        assertEquals(0, capped.getXpToNext());
        finish(8, capped.getPending());
        State afterCap = service.submit(8, action("open", "island"));
        assertEquals(2308, afterCap.getOpened());
        assertEquals(60, afterCap.getLevel());
        assertEquals(0, afterCap.getXpToNext());
    }

    @Test
    void keepsTheLastShinyCardInTheIslandPool() {
        Card shiny = catalog.poolCards("island").stream().filter(card -> "shiny".equals(card.getGrade())).findFirst().get();
        shiny.setPoolIds(Collections.singletonList("kingdoms"));
        PetCardCatalogService administrator = new PetCardCatalogService(catalog, null);
        BusinessException error = assertThrows(BusinessException.class, () -> new TransactionTemplate(transactions)
                .executeWithoutResult(status -> administrator.saveCard(shiny)));
        assertTrue(error.getMessage().contains("首包保底"));
        assertTrue(catalog.hasShinyCard("island"));
    }

    @Test
    void purchaseRollsBackWithPointsLedgerFailure() {
        assertThrows(IllegalStateException.class, () -> service.submit(9, buy("island", 50, 10)));
        assertEquals(1000, service.state(9).getBalance());
        assertTrue(service.state(9).getStock().isEmpty());
    }

    private static boolean claim(long userId) {
        try {
            service.submit(userId, action("claim", "island"));
            return true;
        } catch (BusinessException e) {
            return false;
        }
    }

    private static Command action(String type, String pool) {
        Command command = new Command();
        command.setType(type);
        command.setPoolId(pool);
        command.setRequestId(UUID.randomUUID().toString());
        return command;
    }

    private static Command buy(String pool, int price, int quantity) {
        Command command = action("buy", pool);
        command.setUnitPrice(price);
        command.setQuantity(quantity);
        return command;
    }

    private static void finish(long userId, Opening pending) {
        Command command = action("finish", null);
        command.setOpeningId(pending.getId());
        assertNull(service.submit(userId, command).getPending());
    }
}

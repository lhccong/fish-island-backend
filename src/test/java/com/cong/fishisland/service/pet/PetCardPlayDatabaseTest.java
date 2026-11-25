package com.cong.fishisland.service.pet;

import com.cong.fishisland.common.exception.BusinessException;
import com.cong.fishisland.mapper.PetCardCatalogMapper;
import com.cong.fishisland.mapper.PetCardPlayMapper;
import com.cong.fishisland.model.dto.pet.PetCardCatalog.Atlas;
import com.cong.fishisland.model.dto.pet.PetCardCatalog.Card;
import com.cong.fishisland.model.dto.pet.PetCardCatalog.Pool;
import com.cong.fishisland.model.dto.pet.PetCardPlay.CollectedCard;
import com.cong.fishisland.model.dto.pet.PetCardPlay.CollectionPage;
import com.cong.fishisland.model.dto.pet.PetCardPlay.Command;
import com.cong.fishisland.model.dto.pet.PetCardPlay.LevelProgress;
import com.cong.fishisland.model.dto.pet.PetCardPlay.Opening;
import com.cong.fishisland.model.dto.pet.PetCardPlay.Owned;
import com.cong.fishisland.model.dto.pet.PetCardPlay.State;
import com.cong.fishisland.model.entity.user.UserPoints;
import com.cong.fishisland.model.vo.pet.PetEquipStatsVO;
import com.cong.fishisland.service.UserPointsService;
import com.cong.fishisland.service.broadcast.AchievementBroadcastService;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

import java.time.Instant;
import java.time.LocalDate;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.UUID;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@EnabledIfEnvironmentVariable(named = "PET_CARD_PLAY_VERIFY_JDBC", matches = "jdbc:mysql://127\\.0\\.0\\.1:[0-9]+/pet_catalog_play.*")
class PetCardPlayDatabaseTest {
    private static AnnotationConfigApplicationContext database;
    private static PetCardDatabaseFixtures fixtures;
    private static PetCardCatalogStore catalog;
    private static PetCardPlayService service;
    private static PlatformTransactionManager transactions;

    @BeforeAll
    static void prepare() {
        database = PetCardDatabaseContext.open(System.getenv("PET_CARD_PLAY_VERIFY_JDBC"),
                "file:sql/20261122_pet_card_catalog.sql", "file:sql/20261123_pet_card_play.sql");
        fixtures = PetCardDatabaseFixtures.from(database);
        fixtures.createWallets();
        fixtures.seedWallets();
        UserPointsService points = mock(UserPointsService.class);
        when(points.getById(anyLong())).thenAnswer(call -> {
            UserPoints wallet = new UserPoints();
            wallet.setPoints(1000);
            wallet.setUsedPoints(fixtures.spent(call.getArgument(0, Long.class)));
            return wallet;
        });
        doAnswer(call -> {
            call.getArgument(1, Runnable.class).run();
            return null;
        }).when(points).runWithUserPointsLocks(any(Long[].class), any(Runnable.class));
        doAnswer(call -> {
            fixtures.spend(call.getArgument(0, Long.class), call.getArgument(1, Integer.class));
            if (call.getArgument(0, Long.class) == 9) throw new IllegalStateException("simulated points ledger failure");
            return null;
        }).when(points).updateUsedPoints(anyLong(), anyInt(), eq("pet_card_pack"), anyString(), anyString());
        ObjectMapper json = new ObjectMapper();
        catalog = new PetCardCatalogStore(database.getBean(PetCardCatalogMapper.class), json);
        transactions = database.getBean(PlatformTransactionManager.class);
        new TransactionTemplate(transactions).executeWithoutResult(status -> seedCatalog(json));
        service = new PetCardPlayService(new PetCardPlayStore(database.getBean(PetCardPlayMapper.class), json),
                catalog, points, transactions, json, mock(AchievementBroadcastService.class));
    }

    private static void seedCatalog(ObjectMapper json) {
        Pool island = new Pool();
        island.setId("island");
        island.setName("海岛初遇");
        island.setWeights(Collections.singletonMap("common", 1));
        catalog.savePool(island);

        Pool kingdoms = new Pool();
        kingdoms.setId("kingdoms");
        kingdoms.setName("三国群英");
        kingdoms.setWeights(island.getWeights());
        catalog.savePool(kingdoms);

        String atlas = String.format("%064d.jpg", 0);
        catalog.registerAtlas(new Atlas(atlas, "图集", false));
        Card common = new Card();
        common.setId("commonCard");
        common.setName("普通卡");
        common.setGrade("common");
        common.setStats(json.createObjectNode().put("health", 5));
        common.setBattle(json.createObjectNode());
        common.setArtKey(atlas);
        common.setArtTile(0);
        common.setPoolIds(Arrays.asList("island", "kingdoms"));
        catalog.saveCard(common);
        catalog.setMemberships(common, Collections.emptyList());

        Card shiny = json.convertValue(common, Card.class);
        shiny.setId("shinyCard");
        shiny.setName("闪耀卡");
        shiny.setGrade("shiny");
        shiny.setStats(json.createObjectNode().put("health", 10));
        ObjectNode battle = json.createObjectNode().put("kind", "effect").put("name", "暴击").put("turnOffset", 1);
        battle.putObject("trigger").put("kind", "firstHit");
        battle.putObject("outcome").put("kind", "guaranteedCriticalHit");
        shiny.setBattle(battle);
        shiny.setArtTile(1);
        catalog.saveCard(shiny);
        catalog.setMemberships(shiny, Collections.emptyList());
    }

    @AfterAll
    static void closeDatabase() {
        if (database != null) database.close();
    }

    @Test
    void collectionSortsByGradeBeforePagingAndKeepsIdOrderWithinGrade() {
        long userId = 12;
        ObjectMapper json = new ObjectMapper();
        PetCardPlayMapper play = database.getBean(PetCardPlayMapper.class);
        List<String> grades = Arrays.asList("fine", "epic", "legendary", "shiny");
        new TransactionTemplate(transactions).executeWithoutResult(status -> {
            for (int index = 24; index >= 1; index--) {
                Card card = new Card();
                card.setId(String.format(Locale.ROOT, "collection-%02d", index));
                card.setName(card.getId());
                card.setGrade(index <= 20 ? "common" : grades.get(index - 21));
                card.setStats(json.createObjectNode().put("health", 5));
                card.setBattle(json.createObjectNode());
                card.setArtKey(String.format(Locale.ROOT, "%064d.jpg", 0));
                card.setArtTile(0);
                catalog.saveCard(card);
                play.insertOwned(userId, card.getId());
            }
        });

        CollectionPage first = service.collection(userId, 1);
        assertEquals(24, first.getTotal());
        assertEquals(20, first.getItems().size());
        assertEquals(Arrays.asList("shiny", "legendary", "epic", "fine"),
                first.getItems().subList(0, 4).stream().map(CollectedCard::getGrade).collect(Collectors.toList()));
        assertEquals("collection-01", first.getItems().get(4).getId());
        assertEquals("collection-16", first.getItems().get(19).getId());
        assertEquals(Arrays.asList("collection-17", "collection-18", "collection-19", "collection-20"),
                service.collection(userId, 2).getItems().stream().map(CollectedCard::getId).collect(Collectors.toList()));
    }

    @Test
    void sixShowcasedCardsContributeAndASeventhCanReplaceOne() {
        long userId = 13;
        long otherUserId = 14;
        ObjectMapper json = new ObjectMapper();
        PetCardPlayMapper play = database.getBean(PetCardPlayMapper.class);
        new TransactionTemplate(transactions).executeWithoutResult(status -> {
            for (int index = 1; index <= 7; index++) {
                Card card = new Card();
                card.setId("showcase-" + index);
                card.setName(card.getId());
                card.setGrade("common");
                card.setStats(json.createObjectNode().put("health", index * 10).put("attack", index));
                card.setBattle(json.createObjectNode());
                card.setArtKey(String.format(Locale.ROOT, "%064d.jpg", 0));
                card.setArtTile(0);
                catalog.saveCard(card);
                play.insertOwned(userId, card.getId());
            }
            play.insertOwned(otherUserId, "showcase-7");
            play.display(otherUserId, "showcase-7", true);
            fixtures.setDuplicates(userId, "showcase-1", 1);
        });
        for (int index = 1; index <= 6; index++) {
            service.submit(userId, cardAction("display", "showcase-" + index));
        }
        BusinessException full = assertThrows(BusinessException.class,
                () -> service.submit(userId, cardAction("display", "showcase-7")));
        assertTrue(full.getMessage().contains("展示柜已满"));
        List<CollectedCard> cards = assertShowcaseStats(play, json, userId, 210, 21);
        assertEquals(6, cards.size());
        assertEquals("showcase-1", cards.get(0).getName());
        assertEquals(String.format(Locale.ROOT, "%064d.jpg", 0), cards.get(0).getArtKey());
        assertEquals("showcase-7", assertShowcaseStats(play, json, otherUserId, 70, 7).get(0).getId());

        Command upgrade = cardAction("upgrade", "showcase-1");
        upgrade.setStars(1);
        service.submit(userId, upgrade);
        assertEquals(2, assertShowcaseStats(play, json, userId, 212, 22).get(0).getStars());
        service.submit(userId, cardAction("remove", "showcase-1"));
        service.submit(userId, cardAction("display", "showcase-7"));
        assertShowcaseStats(play, json, userId, 270, 27);

        Command removeAll = action("removeAll", null);
        List<Owned> kept = service.submit(userId, removeAll).getCollection();
        assertEquals(7, kept.size());
        assertTrue(kept.stream().noneMatch(Owned::isShowcased));
        assertEquals(2, kept.get(0).getStars());
        assertTrue(assertShowcaseStats(play, json, userId, 0, 0).isEmpty());
        assertEquals(1, assertShowcaseStats(play, json, otherUserId, 70, 7).size());
        service.submit(userId, cardAction("display", "showcase-1"));
        assertTrue(service.submit(userId, removeAll).getCollection().get(0).isShowcased());
    }

    private static List<CollectedCard> assertShowcaseStats(PetCardPlayMapper play, ObjectMapper json, long userId,
                                                          int health, int attack) {
        PetEquipStatsVO stats = new PetEquipStatsVO();
        stats.setTotalBaseHp(0);
        stats.setTotalBaseAttack(0);
        stats.setTotalBaseDefense(0);
        List<CollectedCard> cards = new PetCardShowcaseStats(play, json).addTo(userId, stats);
        assertEquals(health, stats.getTotalBaseHp());
        assertEquals(attack, stats.getTotalBaseAttack());
        return cards;
    }

    private static Command cardAction(String type, String cardId) {
        Command command = action(type, null);
        command.setCardId(cardId);
        return command;
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
        assertEquals("shiny", pending.getCards().get(4).getGrade());
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
        CollectionPage collection = service.collection(8, 1);
        assertEquals(2, collection.getTotal());
        assertEquals(new LevelProgress(1, 1, 3), collection.getProgress());
        assertEquals("shinyCard", collection.getItems().get(0).getId());
        assertEquals("commonCard", collection.getItems().get(1).getId());
        assertEquals("common", collection.getItems().get(1).getGrade());
        assertEquals(3, collection.getItems().get(1).getStars());
        assertEquals(64 + ".jpg".length(), collection.getItems().get(1).getArtKey().length());
        CollectionPage next = service.collection(8, 2);
        assertTrue(next.getItems().isEmpty());
        assertNull(next.getProgress());
        CollectionPage empty = service.collection(9, 1);
        assertEquals(0, empty.getTotal());
        assertEquals(new LevelProgress(1, 0, 3), empty.getProgress());
        assertThrows(BusinessException.class, () -> service.collection(8, 0));
        assertEquals(4, service.state(8).getCollection().get(0).getUpgradeCost().intValue());
        fixtures.setClaimDay(8, LocalDate.of(2020, 1, 1));
        assertEquals(2, service.state(8).getFreeRemaining());
        assertEquals(12, service.state(8).getStock().get(0).getQuantity());

        fixtures.setOpened(8, 39);
        assertEquals(9, service.state(8).getLevel());
        assertEquals(1, service.state(8).getXpToNext());
        assertThrows(BusinessException.class, () -> service.submit(8, buy("kingdoms", 100, 1)));
        finish(8, service.submit(8, action("open", "island")).getPending());
        assertEquals(10, service.state(8).getLevel());
        assertEquals(6, service.state(8).getXpToNext());
        assertEquals(400, service.submit(8, buy("kingdoms", 100, 1)).getBalance());
        for (int i = 0; i < 20; i++) {
            fixtures.copyOpening(pending.getId());
        }
        String cursor = service.history(8, null).getNextCursor();
        assertNotNull(cursor);
        assertEquals(20, service.history(8, null).getItems().size());
        assertEquals(2, service.history(8, cursor).getItems().size());
        assertEquals("shiny", service.history(8, cursor).getItems().get(1).getCards().get(4).getGrade());
        assertTrue(service.history(9, null).getItems().isEmpty());
        assertTrue(service.state(9).getCollection().isEmpty());

        Opening kingdoms = service.submit(8, action("open", "kingdoms")).getPending();
        assertTrue(kingdoms.getCards().stream().allMatch(card -> "commonCard".equals(card.getCardId())));
        finish(8, kingdoms);

        fixtures.setOpened(8, 2306);
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
    void opensBatchesAndUpgradesEveryEligibleCardWithIdempotentSummaries() {
        long user = 10L;
        Command batch = action("openBatch", "island");
        batch.setQuantity(2);
        assertThrows(BusinessException.class, () -> service.submit(user, batch));
        service.submit(user, action("claim", "island"));
        service.submit(user, action("claim", "island"));
        State opened = service.submit(user, batch);
        assertNull(opened.getPending());
        assertEquals(2, opened.getOpened());
        assertEquals(0, opened.getStock().get(0).getQuantity());
        assertEquals(2, opened.getBatch().getOpenedPacks());
        assertEquals(2, opened.getBatch().getNewCards());
        assertEquals(8, opened.getBatch().getDuplicateCards());
        assertEquals(opened.getBatch(), service.submit(user, batch).getBatch());
        assertEquals(2, service.history(user, null).getItems().size());
        assertEquals("shinyCard", service.history(user, null).getItems().get(1).getCards().get(4).getCardId());
        assertTrue(service.history(user, null).getItems().get(0).getCards().stream().allMatch(card -> card.isDuplicate()));

        service.submit(user, buy("island", 50, 10));
        Opening pending = service.submit(user, action("open", "island")).getPending();
        Command remaining = action("openBatch", "island");
        remaining.setQuantity(9);
        assertThrows(BusinessException.class, () -> service.submit(user, remaining));
        finish(user, pending);
        assertEquals(12, service.submit(user, remaining).getOpened());
        assertEquals(12, service.history(user, null).getItems().size());
        assertThrows(BusinessException.class, () -> {
            remaining.setQuantity(8);
            service.submit(user, remaining);
        });

        fixtures.setDuplicates(user, "shinyCard", 4);
        Command upgrade = action("upgradeAll", null);
        State upgraded = service.submit(user, upgrade);
        assertEquals(2, upgraded.getBatch().getUpgradedCards());
        assertEquals(6, upgraded.getBatch().getAddedStars());
        assertEquals(18, upgraded.getBatch().getSpentDuplicates());
        assertEquals(5, upgraded.getCollection().get(0).getStars());
        assertEquals(43, upgraded.getCollection().get(0).getDuplicates());
        assertEquals(3, upgraded.getCollection().get(1).getStars());
        assertEquals(1, upgraded.getCollection().get(1).getDuplicates());
        State retried = service.submit(user, upgrade);
        assertEquals(upgraded.getBatch(), retried.getBatch());
        assertEquals(upgraded.getCollection(), retried.getCollection());
        assertThrows(BusinessException.class, () -> service.submit(user, action("upgradeAll", null)));
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

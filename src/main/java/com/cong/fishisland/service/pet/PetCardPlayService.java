package com.cong.fishisland.service.pet;

import com.cong.fishisland.common.ErrorCode;
import com.cong.fishisland.common.exception.BusinessException;
import com.cong.fishisland.mapper.PetCardPlayMapper.Player;
import com.cong.fishisland.model.dto.pet.PetCardCatalog.Card;
import com.cong.fishisland.model.dto.pet.PetCardCatalog.Pool;
import com.cong.fishisland.model.dto.pet.PetCardPlay.*;
import com.cong.fishisland.model.entity.user.UserPoints;
import com.cong.fishisland.model.enums.user.PointsRecordSourceEnum;
import com.cong.fishisland.service.UserPointsService;
import com.cong.fishisland.service.broadcast.AchievementBroadcastService;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.springframework.stereotype.Service;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionTemplate;

import java.security.SecureRandom;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.atomic.AtomicReference;
import java.util.stream.Collectors;

import static com.cong.fishisland.service.pet.PetCardCatalogRules.require;
import static com.cong.fishisland.service.pet.PetCardCatalogRules.validateId;
import static com.cong.fishisland.service.pet.PetCardPlayRules.*;

@Service
public class PetCardPlayService {
    private static final ZoneId BEIJING = ZoneId.of("Asia/Shanghai");
    private static final SecureRandom RANDOM = new SecureRandom();
    private final PetCardPlayStore store;
    private final PetCardCatalogStore catalog;
    private final UserPointsService points;
    private final TransactionTemplate transactions;
    private final ObjectMapper json;
    private final AchievementBroadcastService broadcasts;

    public PetCardPlayService(PetCardPlayStore store, PetCardCatalogStore catalog, UserPointsService points,
                              PlatformTransactionManager manager, ObjectMapper json,
                              AchievementBroadcastService broadcasts) {
        this.store = store;
        this.catalog = catalog;
        this.points = points;
        this.transactions = new TransactionTemplate(manager);
        this.json = json;
        this.broadcasts = broadcasts;
    }

    @Transactional(readOnly = true)
    public State state(long userId) {
        return readState(userId, LocalDate.now(BEIJING), null);
    }

    @Transactional(readOnly = true)
    public History history(long userId, String before) {
        List<Opening> rows = store.history(userId, before == null ? null : parseId(before));
        boolean more = rows.size() > 20;
        return new History(more ? new ArrayList<>(rows.subList(0, 20)) : rows,
                more ? rows.get(19).getId() : null);
    }

    @Transactional(readOnly = true)
    public CollectionPage collection(long userId, int page) {
        require(userId > 0 && page >= 1 && page <= 100000, "查询参数无效");
        return store.collectionPage(userId, (page - 1) * 20);
    }

    public State submit(long userId, Command command) {
        require(command != null && command.getType() != null && command.getRequestId() != null
                && command.getRequestId().matches("[0-9a-f]{8}(-[0-9a-f]{4}){3}-[0-9a-f]{12}"), "操作编号无效");
        if (!"buy".equals(command.getType())) {
            return transactions.execute(status -> apply(userId, command));
        }
        AtomicReference<State> result = new AtomicReference<>();
        points.runWithUserPointsLocks(new Long[]{userId},
                () -> result.set(transactions.execute(status -> apply(userId, command))));
        return result.get();
    }

    private State apply(long userId, Command command) {
        store.lock(userId);
        Instant now = Instant.now();
        LocalDate day = now.atZone(BEIJING).toLocalDate();
        JsonNode submitted = json.valueToTree(command);
        JsonNode receipt = store.receipt(userId, command.getRequestId());
        if (receipt != null) {
            boolean hasBatch = receipt.has("batch");
            require((hasBatch ? receipt.get("request") : receipt).equals(submitted), "操作编号已经使用");
            return readState(userId, day, hasBatch ? json.convertValue(receipt.get("batch"), BatchResult.class) : null);
        }
        Player player = store.player(userId);
        BatchResult batch = null;
        switch (command.getType()) {
            case "claim":
                claim(userId, command.getPoolId(), player, day);
                break;
            case "buy":
                buy(userId, command, player);
                break;
            case "open":
                open(userId, command.getPoolId(), player, now, 1);
                break;
            case "openBatch":
                Integer quantity = command.getQuantity();
                require(quantity != null && quantity >= 2 && quantity <= 10, "一次可批量拆开 2–10 包");
                batch = open(userId, command.getPoolId(), player, now, quantity);
                break;
            case "finish":
                long id = parseId(command.getOpeningId());
                require(Objects.equals(player.getPending(), id), "请查看当前开包进度");
                store.finish(userId);
                break;
            case "upgrade":
                upgrade(userId, command);
                break;
            case "upgradeAll":
                batch = upgradeAll(userId);
                break;
            case "display":
            case "remove":
                display(userId, command.getCardId(), "display".equals(command.getType()));
                break;
            case "removeAll":
                store.removeAllFromShowcase(userId);
                break;
            default:
                throw new BusinessException(ErrorCode.PARAMS_ERROR, "不支持的卡包操作");
        }
        if (batch != null) {
            ObjectNode completed = json.createObjectNode();
            completed.set("request", submitted);
            completed.set("batch", json.valueToTree(batch));
            submitted = completed;
        }
        store.receipt(userId, command.getRequestId(), submitted);
        return readState(userId, day, batch);
    }

    private void claim(long userId, String id, Player player, LocalDate day) {
        require(ISLAND_POOL.equals(id), "只有海岛卡包可免费领取");
        int claims = day.equals(player.getClaimDay()) ? player.getClaims() : 0;
        require(claims < DAILY_FREE, "今日的免费卡包已领完");
        availablePool(id, player);
        store.claim(userId, day, claims + 1);
        store.addStock(userId, id, 1);
    }

    private void buy(long userId, Command command, Player player) {
        Pool pool = availablePool(command.getPoolId(), player);
        require(Objects.equals(command.getUnitPrice(), pool.getUnitPrice()), "卡包价格已更新，请重新打开卡包页面确认");
        require(command.getQuantity() != null, "请选择购买数量");
        int price = purchaseCost(command.getQuantity(), pool.getUnitPrice());
        points.checkAvailablePoints(userId, price);
        points.updateUsedPoints(userId, price, PointsRecordSourceEnum.PET_CARD_PACK.getValue(),
                command.getRequestId(), "购买" + pool.getName() + " × " + command.getQuantity());
        store.addStock(userId, pool.getId(), command.getQuantity());
    }

    private BatchResult open(long userId, String id, Player player, Instant now, int quantity) {
        require(player.getPending() == null, "请先完成当前卡包");
        Pool pool = availablePool(id, player);
        Stock stock = store.stock(userId, id);
        require(stock.getQuantity() >= quantity, quantity == 1 ? "没有待拆卡包" : "待拆库存不足，请确认拆包数量");
        List<Card> cards = catalog.poolCards(id);
        Set<String> owned = store.collection(userId).stream().map(Owned::getCardId).collect(Collectors.toCollection(HashSet::new));
        int previousSize = owned.size();
        LocalDateTime openedAt = LocalDateTime.ofInstant(now, ZoneOffset.UTC);
        List<String> shinyCards = new ArrayList<>();
        long openingId = 0;
        for (int pack = 0; pack < quantity; pack++) {
            List<Card> drawn = draw(pool, cards, stock.getOpened() == 0 && pack == 0, RANDOM::nextInt);
            List<Pull> pulls = new ArrayList<>(PACK_SIZE);
            for (Card card : drawn) {
                boolean duplicate = !owned.add(card.getId());
                store.collect(userId, card.getId(), duplicate);
                pulls.add(new Pull(card.getId(), card.getName(), duplicate, card.getGrade()));
                if ("shiny".equals(card.getGrade())) {
                    shinyCards.add(card.getName());
                }
            }
            openingId = store.recordOpening(userId, id, pool.getName(), openedAt, pulls);
        }
        store.consumeStock(userId, id, quantity, quantity == 1 ? openingId : null);
        broadcasts.shinyCardsDrawn(userId, shinyCards);
        int newCards = owned.size() - previousSize;
        return quantity == 1 ? null : new BatchResult(quantity, newCards, quantity * PACK_SIZE - newCards, 0, 0, 0);
    }

    private Pool availablePool(String id, Player player) {
        validateId(id);
        int minimum = requiredLevel(id);
        require(level(player.getOpened()) >= minimum, "抽卡等级达到 " + minimum + " 级后解锁");
        Pool pool = catalog.pool(id);
        require(pool != null && catalog.drawable(pool), "这个卡池暂未开放");
        if (ISLAND_POOL.equals(id)) {
            require(catalog.hasShinyCard(id), "海岛初遇的闪耀卡尚未配置，请联系管理员");
        }
        return pool;
    }

    private void upgrade(long userId, Command command) {
        Owned card = owned(userId, command.getCardId());
        require(Objects.equals(command.getStars(), card.getStars()), "星级已更新，请确认当前星级");
        Integer cost = card.getUpgradeCost();
        require(cost != null, "卡片已达最高星级");
        require(card.getDuplicates() >= cost, "同名重复卡数量不足");
        store.upgrade(userId, card.getCardId(), card.getStars() + 1, cost);
    }

    private BatchResult upgradeAll(long userId) {
        int upgraded = 0;
        int addedStars = 0;
        long spentDuplicates = 0;
        for (Owned card : store.collection(userId)) {
            int target = card.getStars();
            int spent = 0;
            Integer cost = card.getUpgradeCost();
            while (cost != null && card.getDuplicates() - spent >= cost) {
                spent += cost;
                cost = upgradeCost(++target);
            }
            if (target > card.getStars()) {
                store.upgrade(userId, card.getCardId(), target, spent);
                upgraded++;
                addedStars += target - card.getStars();
                spentDuplicates += spent;
            }
        }
        require(upgraded > 0, "当前没有可升星的卡片");
        return new BatchResult(0, 0, 0, upgraded, addedStars, spentDuplicates);
    }

    private void display(long userId, String id, boolean displayed) {
        Owned card = owned(userId, id);
        if (displayed && !card.isShowcased()) {
            require(store.collection(userId).stream().filter(Owned::isShowcased).count() < SHOWCASE_LIMIT, "展示柜已满");
        }
        store.display(userId, id, displayed);
    }

    private Owned owned(long userId, String id) {
        validateId(id);
        Owned card = store.owned(userId, id);
        require(card != null, "尚未收藏这张卡");
        return card;
    }

    private State readState(long userId, LocalDate day, BatchResult batch) {
        Player player = store.player(userId);
        int remaining = day.equals(player.getClaimDay()) ? DAILY_FREE - player.getClaims() : DAILY_FREE;
        UserPoints wallet = points.getById(userId);
        long balance = wallet == null ? 0 : (long) zero(wallet.getPoints()) - zero(wallet.getUsedPoints());
        int level = level(player.getOpened());
        return new State(day.toString(), remaining, player.getOpened(), level,
                openingsToNextLevel(player.getOpened(), level), balance,
                store.stock(userId), store.collection(userId),
                player.getPending() == null ? null : store.opening(userId, player.getPending()), batch);
    }

    private static int zero(Integer amount) {
        return amount == null ? 0 : amount;
    }

    private static long parseId(String id) {
        try {
            long parsed = Long.parseLong(id);
            require(parsed > 0, "记录编号无效");
            return parsed;
        } catch (NumberFormatException e) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "记录编号无效");
        }
    }
}

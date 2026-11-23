package com.cong.fishisland.service.pet;

import com.cong.fishisland.common.ErrorCode;
import com.cong.fishisland.common.exception.BusinessException;
import com.cong.fishisland.model.dto.pet.PetCardCatalog.Card;
import com.cong.fishisland.model.dto.pet.PetCardCatalog.Pool;
import com.cong.fishisland.model.dto.pet.PetCardPlay.*;
import com.cong.fishisland.model.entity.user.UserPoints;
import com.cong.fishisland.model.enums.user.PointsRecordSourceEnum;
import com.cong.fishisland.service.UserPointsService;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
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

    public PetCardPlayService(PetCardPlayStore store, PetCardCatalogStore catalog, UserPointsService points,
                              PlatformTransactionManager manager, ObjectMapper json) {
        this.store = store;
        this.catalog = catalog;
        this.points = points;
        this.transactions = new TransactionTemplate(manager);
        this.json = json;
    }

    @Transactional(readOnly = true)
    public State state(long userId) {
        return readState(userId, LocalDate.now(BEIJING));
    }

    @Transactional(readOnly = true)
    public History history(long userId, String before) {
        List<Opening> rows = store.history(userId, before == null ? null : parseId(before));
        boolean more = rows.size() > 20;
        return new History(more ? new ArrayList<>(rows.subList(0, 20)) : rows,
                more ? rows.get(19).getId() : null);
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
            require(receipt.equals(submitted), "操作编号已经使用");
            return readState(userId, day);
        }
        PetCardPlayStore.Player player = store.player(userId);
        switch (command.getType()) {
            case "claim":
                claim(userId, command.getPoolId(), player, day);
                break;
            case "buy":
                buy(userId, command, player);
                break;
            case "open":
                open(userId, command.getPoolId(), player, now);
                break;
            case "finish":
                long id = parseId(command.getOpeningId());
                require(Objects.equals(player.getPending(), id), "请查看当前开包进度");
                store.finish(userId);
                break;
            case "upgrade":
                upgrade(userId, command);
                break;
            case "display":
            case "remove":
                display(userId, command.getCardId(), "display".equals(command.getType()));
                break;
            default:
                throw new BusinessException(ErrorCode.PARAMS_ERROR, "不支持的卡包操作");
        }
        store.receipt(userId, command.getRequestId(), submitted);
        return readState(userId, day);
    }

    private void claim(long userId, String id, PetCardPlayStore.Player player, LocalDate day) {
        require(ISLAND_POOL.equals(id), "只有海岛卡包可免费领取");
        int claims = day.equals(player.getClaimDay()) ? player.getClaims() : 0;
        require(claims < DAILY_FREE, "今日的免费卡包已领完");
        availablePool(id, player);
        store.claim(userId, day, claims + 1);
        store.addStock(userId, id, 1);
    }

    private void buy(long userId, Command command, PetCardPlayStore.Player player) {
        Pool pool = availablePool(command.getPoolId(), player);
        require(Objects.equals(command.getUnitPrice(), pool.getUnitPrice()), "卡包价格已更新，请重新打开卡包页面确认");
        require(command.getQuantity() != null, "请选择购买数量");
        int price = purchaseCost(command.getQuantity(), pool.getUnitPrice());
        points.checkAvailablePoints(userId, price);
        points.updateUsedPoints(userId, price, PointsRecordSourceEnum.PET_CARD_PACK.getValue(),
                command.getRequestId(), "购买" + pool.getName() + " × " + command.getQuantity());
        store.addStock(userId, pool.getId(), command.getQuantity());
    }

    private void open(long userId, String id, PetCardPlayStore.Player player, Instant now) {
        require(player.getPending() == null, "请先完成当前卡包");
        Pool pool = availablePool(id, player);
        Stock stock = store.stock(userId, id);
        require(stock.getQuantity() > 0, "没有待拆卡包");
        List<Card> drawn = draw(pool, catalog.poolCards(id), stock.getOpened() == 0, RANDOM::nextInt);
        Set<String> owned = store.collection(userId).stream().map(Owned::getCardId).collect(Collectors.toCollection(HashSet::new));
        List<Pull> pulls = new ArrayList<>();
        for (Card card : drawn) {
            boolean duplicate = !owned.add(card.getId());
            store.collect(userId, card.getId(), duplicate);
            pulls.add(new Pull(card.getId(), card.getName(), duplicate));
        }
        store.open(userId, id, pool.getName(), LocalDateTime.ofInstant(now, ZoneOffset.UTC), pulls);
    }

    private Pool availablePool(String id, PetCardPlayStore.Player player) {
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
        store.upgrade(userId, card.getCardId(), cost);
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

    private State readState(long userId, LocalDate day) {
        PetCardPlayStore.Player player = store.player(userId);
        int remaining = day.equals(player.getClaimDay()) ? DAILY_FREE - player.getClaims() : DAILY_FREE;
        UserPoints wallet = points.getById(userId);
        long balance = wallet == null ? 0 : (long) zero(wallet.getPoints()) - zero(wallet.getUsedPoints());
        int level = level(player.getOpened());
        return new State(day.toString(), remaining, player.getOpened(), level,
                openingsToNextLevel(player.getOpened(), level), balance,
                store.stock(userId), store.collection(userId),
                player.getPending() == null ? null : store.opening(userId, player.getPending()));
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

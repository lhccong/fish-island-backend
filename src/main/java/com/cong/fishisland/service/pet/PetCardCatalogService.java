package com.cong.fishisland.service.pet;

import com.cong.fishisland.model.dto.pet.PetCardCatalog.Atlas;
import com.cong.fishisland.model.dto.pet.PetCardCatalog.Card;
import com.cong.fishisland.model.dto.pet.PetCardCatalog.CardPage;
import com.cong.fishisland.model.dto.pet.PetCardCatalog.Catalog;
import com.cong.fishisland.model.dto.pet.PetCardCatalog.Pool;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

import static com.cong.fishisland.service.pet.PetCardCatalogRules.*;
import static com.cong.fishisland.service.pet.PetCardPlayRules.ISLAND_POOL;

@Service
@RequiredArgsConstructor
public class PetCardCatalogService {
    private final PetCardCatalogStore store;
    private final PetCardAtlasStore images;

    @Transactional(readOnly = true)
    public Catalog catalog() {
        return new Catalog(store.pools(false), store.cards());
    }

    public List<Pool> pools() {
        return store.pools(false);
    }

    public List<Atlas> atlases() {
        return store.atlases();
    }

    @Transactional(readOnly = true)
    public CardPage cards(String poolId, String query, int page) {
        if (poolId != null) validateId(poolId);
        require(page >= 1 && page <= 100000 && (query == null || query.length() <= 80), "筛选参数无效");
        return store.cards(poolId, query == null ? "" : query.trim(), page);
    }

    @Transactional
    public void savePool(Pool pool) {
        validatePool(pool);
        Map<String, Pool> pools = lockPools();
        Pool previous = pools.get(pool.getId());
        matchVersion(previous == null ? null : previous.getVersion(), pool.getVersion());
        if (pool.getCoverId() != null) {
            require(store.containsCard(pool.getId(), pool.getCoverId()), "封面卡须属于该卡池");
        }
        if (previous != null && store.drawable(previous)) {
            require(store.drawable(pool), "卡池至少保留一种权重大于 0 的入池品质");
        }
        pool.setName(pool.getName().trim());
        store.savePool(pool);
    }

    @Transactional
    public void saveCard(Card card) {
        validateCard(card);
        Map<String, Pool> pools = lockPools();
        matchVersion(store.cardVersion(card.getId()), card.getVersion());
        require(store.hasAtlas(card.getArtKey()), "图集不存在");
        List<String> before = store.memberships(card.getId());
        require(pools.keySet().containsAll(card.getPoolIds()), "卡池不存在");
        Set<String> affected = new HashSet<>(before);
        affected.addAll(card.getPoolIds());
        boolean islandHadShiny = affected.contains(ISLAND_POOL) && store.hasShinyCard(ISLAND_POOL);
        card.setName(card.getName().trim());
        store.saveCard(card);
        store.setMemberships(card, before);
        if (islandHadShiny) {
            require(store.hasShinyCard(ISLAND_POOL), "海岛初遇至少需保留一张闪耀卡用于首包保底");
        }
        for (String id : affected) {
            require(store.drawable(pools.get(id)), "「" + pools.get(id).getName() + "」至少需保留一张可抽取的卡");
        }
    }

    public Atlas uploadAtlas(String name, boolean portrait, byte[] content) {
        require(name != null && !name.trim().isEmpty() && name.length() <= 80, "图集名称须为 1–80 字");
        String id = images.write(content, portrait, false);
        Atlas atlas = store.registerAtlas(new Atlas(id, name.trim(), portrait));
        require(atlas.isPortrait() == portrait, "相同图集已按另一种格位比例登记");
        return atlas;
    }

    private Map<String, Pool> lockPools() {
        // All membership and pool changes take locks in the pool's stable read order.
        return store.pools(true).stream().collect(Collectors.toMap(Pool::getId, Function.identity()));
    }

    private static void matchVersion(Integer existing, Integer submitted) {
        require(Objects.equals(existing, submitted), "配置已存在或已更新，请刷新后重新编辑");
    }
}

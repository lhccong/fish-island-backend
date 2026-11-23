package com.cong.fishisland.service.pet;

import com.cong.fishisland.model.dto.pet.PetCardCatalog.Atlas;
import com.cong.fishisland.model.dto.pet.PetCardCatalog.Card;
import com.cong.fishisland.model.dto.pet.PetCardCatalog.CardPage;
import com.cong.fishisland.model.dto.pet.PetCardCatalog.Pool;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Repository;

import java.io.IOException;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Repository
@RequiredArgsConstructor
public class PetCardCatalogStore {
    private static final String CARDS = "SELECT c.*, a.portrait FROM pet_card_definition c "
            + "JOIN pet_card_atlas a ON a.id = c.art_key ";
    private static final TypeReference<Map<String, Integer>> WEIGHTS = new TypeReference<Map<String, Integer>>() {};
    private final JdbcTemplate jdbc;
    private final NamedParameterJdbcTemplate named;
    private final ObjectMapper json;

    List<Pool> pools(boolean lock) {
        return jdbc.query("SELECT * FROM pet_card_pool ORDER BY sort_order, id" + (lock ? " FOR UPDATE" : ""),
                this::readPool);
    }

    Pool pool(String id) {
        return jdbc.query("SELECT * FROM pet_card_pool WHERE id=?", this::readPool, id)
                .stream().findFirst().orElse(null);
    }

    List<Card> poolCards(String id) {
        return jdbc.query(CARDS + "JOIN pet_card_pool_entry e ON e.card_id=c.id WHERE e.pool_id=? ORDER BY c.id",
                this::readCard, id);
    }

    private Pool readPool(ResultSet rs, int row) throws SQLException {
        Pool pool = new Pool();
        pool.setId(rs.getString("id"));
        pool.setName(rs.getString("name"));
        pool.setSortOrder(rs.getInt("sort_order"));
        pool.setCoverId(rs.getString("cover_id"));
        pool.setWeights(json.convertValue(readJson(rs.getString("weights")), WEIGHTS));
        pool.setVersion(rs.getInt("version"));
        pool.setUnitPrice(rs.getInt("unit_price"));
        return pool;
    }

    List<Card> cards() {
        List<Card> cards = jdbc.query(CARDS + "ORDER BY c.sort_order, c.id", this::readCard);
        return loadMemberships(cards);
    }

    CardPage cards(String poolId, String query, int page) {
        String where = " WHERE (:pool IS NULL OR EXISTS (SELECT 1 FROM pet_card_pool_entry e "
                + "WHERE e.card_id = c.id AND e.pool_id = :pool))"
                + " AND (INSTR(c.name, :query) > 0 OR INSTR(CONVERT(c.id USING utf8mb4), :query) > 0 OR :query = '')";
        MapSqlParameterSource args = new MapSqlParameterSource("pool", poolId)
                .addValue("query", query).addValue("offset", (page - 1L) * 20);
        Long total = named.queryForObject("SELECT COUNT(*) FROM pet_card_definition c" + where, args, Long.class);
        List<Card> cards = named.query(CARDS + where + " ORDER BY c.sort_order, c.id LIMIT 20 OFFSET :offset",
                args, this::readCard);
        return new CardPage(loadMemberships(cards), total == null ? 0 : total);
    }

    private List<Card> loadMemberships(List<Card> cards) {
        if (cards.isEmpty()) return cards;
        List<String> ids = cards.stream().map(Card::getId).collect(Collectors.toList());
        Map<String, List<String>> groups = new HashMap<>();
        named.query("SELECT card_id, pool_id FROM pet_card_pool_entry WHERE card_id IN (:ids) ORDER BY pool_id",
                new MapSqlParameterSource("ids", ids), rs -> {
                    groups.computeIfAbsent(rs.getString("card_id"), id -> new ArrayList<>()).add(rs.getString("pool_id"));
                });
        cards.forEach(card -> card.setPoolIds(groups.getOrDefault(card.getId(), Collections.emptyList())));
        return cards;
    }

    Integer cardVersion(String id) {
        List<Integer> rows = jdbc.query("SELECT version FROM pet_card_definition WHERE id = ? FOR UPDATE",
                (rs, row) -> rs.getInt(1), id);
        return rows.isEmpty() ? null : rows.get(0);
    }

    List<String> memberships(String id) {
        return jdbc.queryForList("SELECT pool_id FROM pet_card_pool_entry WHERE card_id = ?", String.class, id);
    }

    boolean containsCard(String pool, String card) {
        return count("SELECT COUNT(*) FROM pet_card_pool_entry WHERE pool_id = ? AND card_id = ?", pool, card) > 0;
    }

    boolean drawable(Pool pool) {
        return jdbc.queryForList("SELECT DISTINCT c.grade FROM pet_card_pool_entry e "
                        + "JOIN pet_card_definition c ON c.id = e.card_id WHERE e.pool_id = ?", String.class, pool.getId())
                .stream().anyMatch(grade -> pool.getWeights().getOrDefault(grade, 0) > 0);
    }

    void saveCard(Card card) {
        if (card.getVersion() == null) {
            jdbc.update("INSERT INTO pet_card_definition (id, name, grade, stats, battle, art_key, art_tile, sort_order)"
                            + " VALUES (?, ?, ?, ?, ?, ?, ?, ?)",
                    card.getId(), card.getName(), card.getGrade(), card.getStats().toString(),
                    card.getBattle().toString(), card.getArtKey(), card.getArtTile(), card.getSortOrder());
        } else {
            jdbc.update("UPDATE pet_card_definition SET name=?, grade=?, stats=?, battle=?, art_key=?, art_tile=?,"
                            + " sort_order=?, version=version+1 WHERE id=? AND version=?",
                    card.getName(), card.getGrade(), card.getStats().toString(), card.getBattle().toString(),
                    card.getArtKey(), card.getArtTile(), card.getSortOrder(), card.getId(), card.getVersion());
        }
    }

    void setMemberships(Card card, List<String> before) {
        for (String id : before) {
            if (!card.getPoolIds().contains(id)) {
                jdbc.update("DELETE FROM pet_card_pool_entry WHERE pool_id=? AND card_id=?", id, card.getId());
                jdbc.update("UPDATE pet_card_pool SET cover_id=NULL, version=version+1 WHERE id=? AND cover_id=?", id, card.getId());
            }
        }
        for (String id : card.getPoolIds()) {
            if (!before.contains(id)) jdbc.update("INSERT INTO pet_card_pool_entry VALUES (?, ?)", id, card.getId());
        }
    }

    void savePool(Pool pool) {
        String weights = json.valueToTree(pool.getWeights()).toString();
        if (pool.getVersion() == null) {
            int price = pool.getUnitPrice() == null ? ("island".equals(pool.getId()) ? 50 : 100) : pool.getUnitPrice();
            jdbc.update("INSERT INTO pet_card_pool (id, name, weights, cover_id, sort_order, unit_price) VALUES (?, ?, ?, ?, ?, ?)",
                    pool.getId(), pool.getName(), weights, pool.getCoverId(), pool.getSortOrder(), price);
        } else {
            jdbc.update("UPDATE pet_card_pool SET name=?, weights=?, cover_id=?, sort_order=?,"
                            + " unit_price=COALESCE(?, unit_price), version=version+1 WHERE id=? AND version=?",
                    pool.getName(), weights, pool.getCoverId(), pool.getSortOrder(), pool.getUnitPrice(), pool.getId(), pool.getVersion());
        }
    }

    List<Atlas> atlases() {
        return jdbc.query("SELECT id, name, portrait FROM pet_card_atlas ORDER BY name, id",
                (rs, row) -> new Atlas(rs.getString(1), rs.getString(2), rs.getBoolean(3)));
    }

    boolean hasAtlas(String id) {
        return count("SELECT COUNT(*) FROM pet_card_atlas WHERE id=?", id) > 0;
    }

    Atlas registerAtlas(Atlas atlas) {
        jdbc.update("INSERT INTO pet_card_atlas (id, name, portrait) VALUES (?, ?, ?) "
                        + "ON DUPLICATE KEY UPDATE id = id",
                atlas.getId(), atlas.getName(), atlas.isPortrait());
        return jdbc.queryForObject("SELECT id, name, portrait FROM pet_card_atlas WHERE id=?",
                (rs, row) -> new Atlas(rs.getString(1), rs.getString(2), rs.getBoolean(3)), atlas.getId());
    }

    boolean startSeed(String id) {
        return jdbc.update("INSERT IGNORE INTO pet_card_catalog_migration (id) VALUES (?)", id) == 1;
    }

    private long count(String sql, Object... args) {
        Long count = jdbc.queryForObject(sql, Long.class, args);
        return count == null ? 0 : count;
    }

    private Card readCard(ResultSet rs, int row) throws SQLException {
        Card card = new Card();
        card.setId(rs.getString("id"));
        card.setName(rs.getString("name"));
        card.setGrade(rs.getString("grade"));
        card.setStats(readJson(rs.getString("stats")));
        card.setBattle(readJson(rs.getString("battle")));
        card.setArtKey(rs.getString("art_key"));
        card.setArtTile(rs.getInt("art_tile"));
        card.setArtPortrait(rs.getBoolean("portrait"));
        card.setSortOrder(rs.getInt("sort_order"));
        card.setVersion(rs.getInt("version"));
        return card;
    }

    private JsonNode readJson(String value) {
        try {
            return json.readTree(value);
        } catch (IOException e) {
            throw new IllegalStateException("Invalid persisted card configuration", e);
        }
    }
}

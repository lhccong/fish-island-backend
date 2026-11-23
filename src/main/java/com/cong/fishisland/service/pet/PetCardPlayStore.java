package com.cong.fishisland.service.pet;

import com.cong.fishisland.model.dto.pet.PetCardPlay.*;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.Value;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.stereotype.Repository;

import java.io.IOException;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Objects;

@Repository
@RequiredArgsConstructor
public class PetCardPlayStore {
    private static final String HISTORY = "SELECT id, pool_id, pool_name, opened_at, cards FROM pet_card_opening";
    private static final String COLLECTION = "SELECT card_id, stars, duplicates, showcased FROM pet_card_collection";
    private static final TypeReference<List<Pull>> PULLS = new TypeReference<List<Pull>>() {};
    private final JdbcTemplate jdbc;
    private final ObjectMapper json;

    @Value
    static class Player {
        LocalDate claimDay;
        int claims;
        long opened;
        Long pending;
    }

    void lock(long userId) {
        jdbc.update("INSERT INTO pet_card_player (user_id) VALUES (?) ON DUPLICATE KEY UPDATE user_id=user_id", userId);
    }

    Player player(long userId) {
        return jdbc.query("SELECT claim_day, claims, opened, pending_open FROM pet_card_player WHERE user_id=?",
                (rs, i) -> new Player(rs.getObject(1, LocalDate.class), rs.getInt(2), rs.getLong(3),
                        rs.getObject(4, Long.class)), userId)
                .stream().findFirst().orElse(new Player(null, 0, 0, null));
    }

    JsonNode receipt(long userId, String requestId) {
        return jdbc.query("SELECT command FROM pet_card_operation WHERE user_id=? AND request_id=?",
                (rs, i) -> readJson(rs.getString(1)), userId, requestId).stream().findFirst().orElse(null);
    }

    void receipt(long userId, String requestId, JsonNode command) {
        jdbc.update("INSERT INTO pet_card_operation (user_id, request_id, command) VALUES (?, ?, ?)",
                userId, requestId, command.toString());
    }

    List<Stock> stock(long userId) {
        return jdbc.query("SELECT pool_id, quantity, opened FROM pet_card_stock WHERE user_id=? ORDER BY pool_id",
                (rs, i) -> new Stock(rs.getString(1), rs.getInt(2), rs.getLong(3)), userId);
    }

    Stock stock(long userId, String pool) {
        return jdbc.query("SELECT quantity, opened FROM pet_card_stock WHERE user_id=? AND pool_id=?",
                (rs, i) -> new Stock(pool, rs.getInt(1), rs.getLong(2)), userId, pool)
                .stream().findFirst().orElse(new Stock(pool, 0, 0));
    }

    void addStock(long userId, String pool, int quantity) {
        jdbc.update("INSERT INTO pet_card_stock (user_id, pool_id, quantity) VALUES (?, ?, ?) "
                + "ON DUPLICATE KEY UPDATE quantity=quantity+?", userId, pool, quantity, quantity);
    }

    void claim(long userId, LocalDate day, int claims) {
        jdbc.update("UPDATE pet_card_player SET claim_day=?, claims=? WHERE user_id=?", day, claims, userId);
    }

    void open(long userId, String pool, String name, LocalDateTime time, List<Pull> cards) {
        GeneratedKeyHolder key = new GeneratedKeyHolder();
        jdbc.update(connection -> {
            String sql = "INSERT INTO pet_card_opening (user_id, pool_id, pool_name, opened_at, cards) VALUES (?, ?, ?, ?, ?)";
            PreparedStatement statement = connection.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS);
            statement.setLong(1, userId);
            statement.setString(2, pool);
            statement.setString(3, name);
            statement.setObject(4, time);
            statement.setString(5, json.valueToTree(cards).toString());
            return statement;
        }, key);
        long id = Objects.requireNonNull(key.getKey()).longValue();
        jdbc.update("UPDATE pet_card_stock SET quantity=quantity-1, opened=opened+1 WHERE user_id=? AND pool_id=?",
                userId, pool);
        jdbc.update("UPDATE pet_card_player SET pending_open=?, opened=opened+1 WHERE user_id=?", id, userId);
    }

    void finish(long userId) {
        jdbc.update("UPDATE pet_card_player SET pending_open=NULL WHERE user_id=?", userId);
    }

    List<Owned> collection(long userId) {
        return jdbc.query(COLLECTION + " WHERE user_id=? ORDER BY card_id", this::readOwned, userId);
    }

    Owned owned(long userId, String card) {
        return jdbc.query(COLLECTION + " WHERE user_id=? AND card_id=?",
                this::readOwned, userId, card).stream().findFirst().orElse(null);
    }

    void collect(long userId, String card, boolean duplicate) {
        if (duplicate) {
            jdbc.update("UPDATE pet_card_collection SET duplicates=duplicates+1 WHERE user_id=? AND card_id=?", userId, card);
        } else {
            jdbc.update("INSERT INTO pet_card_collection (user_id, card_id) VALUES (?, ?)", userId, card);
        }
    }

    void upgrade(long userId, String card, int cost) {
        jdbc.update("UPDATE pet_card_collection SET stars=stars+1, duplicates=duplicates-? WHERE user_id=? AND card_id=?",
                cost, userId, card);
    }

    void display(long userId, String card, boolean displayed) {
        jdbc.update("UPDATE pet_card_collection SET showcased=? WHERE user_id=? AND card_id=?", displayed, userId, card);
    }

    Opening opening(long userId, long id) {
        return jdbc.queryForObject(HISTORY + " WHERE user_id=? AND id=?", this::readOpening, userId, id);
    }

    List<Opening> history(long userId, Long before) {
        if (before == null) {
            return jdbc.query(HISTORY + " WHERE user_id=? ORDER BY id DESC LIMIT 21", this::readOpening, userId);
        }
        return jdbc.query(HISTORY + " WHERE user_id=? AND id<? ORDER BY id DESC LIMIT 21", this::readOpening, userId, before);
    }

    private Owned readOwned(ResultSet rs, int row) throws SQLException {
        int stars = rs.getInt(2);
        return new Owned(rs.getString(1), stars, rs.getLong(3), rs.getBoolean(4), PetCardPlayRules.upgradeCost(stars));
    }

    private Opening readOpening(ResultSet rs, int row) throws SQLException {
        try {
            return new Opening(rs.getString(1), rs.getString(2), rs.getString(3),
                    rs.getObject(4, LocalDateTime.class).toInstant(ZoneOffset.UTC).toString(),
                    json.readValue(rs.getString(5), PULLS));
        } catch (IOException e) {
            throw new IllegalStateException("Invalid persisted card opening", e);
        }
    }

    private JsonNode readJson(String value) {
        try {
            return json.readTree(value);
        } catch (IOException e) {
            throw new IllegalStateException("Invalid persisted card operation", e);
        }
    }
}

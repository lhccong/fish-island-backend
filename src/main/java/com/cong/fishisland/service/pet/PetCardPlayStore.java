package com.cong.fishisland.service.pet;

import com.cong.fishisland.mapper.PetCardPlayMapper;
import com.cong.fishisland.mapper.PetCardPlayMapper.OpeningRow;
import com.cong.fishisland.mapper.PetCardPlayMapper.OwnedRow;
import com.cong.fishisland.mapper.PetCardPlayMapper.Player;
import com.cong.fishisland.model.dto.pet.PetCardPlay.Opening;
import com.cong.fishisland.model.dto.pet.PetCardPlay.Owned;
import com.cong.fishisland.model.dto.pet.PetCardPlay.Pull;
import com.cong.fishisland.model.dto.pet.PetCardPlay.Stock;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.EmptyResultDataAccessException;
import org.springframework.stereotype.Repository;

import java.io.IOException;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

@Repository
@RequiredArgsConstructor
public class PetCardPlayStore {
    private static final TypeReference<List<Pull>> PULLS = new TypeReference<List<Pull>>() {};
    private final PetCardPlayMapper mapper;
    private final ObjectMapper json;

    void lock(long userId) {
        mapper.lockPlayer(userId);
    }

    Player player(long userId) {
        Player player = mapper.player(userId);
        return player == null ? new Player(null, 0, 0, null) : player;
    }

    JsonNode receipt(long userId, String requestId) {
        String receipt = mapper.receipt(userId, requestId);
        if (receipt == null) return null;
        try {
            return json.readTree(receipt);
        } catch (IOException e) {
            throw new IllegalStateException("Invalid persisted card operation", e);
        }
    }

    void receipt(long userId, String requestId, JsonNode command) {
        mapper.saveReceipt(userId, requestId, command.toString());
    }

    List<Stock> stock(long userId) {
        return mapper.stocks(userId);
    }

    Stock stock(long userId, String pool) {
        Stock stock = mapper.stock(userId, pool);
        return stock == null ? new Stock(pool, 0, 0) : stock;
    }

    void addStock(long userId, String pool, int quantity) {
        mapper.addStock(userId, pool, quantity);
    }

    void claim(long userId, LocalDate day, int claims) {
        mapper.claim(userId, day, claims);
    }

    long recordOpening(long userId, String pool, String name, LocalDateTime time, List<Pull> cards) {
        OpeningRow opening = new OpeningRow(null, pool, name, time, json.valueToTree(cards).toString());
        mapper.insertOpening(userId, opening);
        return Objects.requireNonNull(opening.getId());
    }

    void consumeStock(long userId, String pool, int quantity, Long pending) {
        mapper.consumeStock(userId, pool, quantity);
        mapper.recordProgress(userId, quantity, pending);
    }

    void finish(long userId) {
        mapper.finish(userId);
    }

    List<Owned> collection(long userId) {
        return mapper.collection(userId).stream().map(this::toOwned).collect(Collectors.toList());
    }

    Owned owned(long userId, String card) {
        OwnedRow row = mapper.owned(userId, card);
        return row == null ? null : toOwned(row);
    }

    void collect(long userId, String card, boolean duplicate) {
        if (duplicate) {
            mapper.incrementDuplicates(userId, card);
        } else {
            mapper.insertOwned(userId, card);
        }
    }

    void upgrade(long userId, String card, int stars, int cost) {
        mapper.upgrade(userId, card, stars, cost);
    }

    void display(long userId, String card, boolean displayed) {
        mapper.display(userId, card, displayed);
    }

    Opening opening(long userId, long id) {
        OpeningRow row = mapper.opening(userId, id);
        if (row == null) throw new EmptyResultDataAccessException(1);
        return toOpening(row);
    }

    List<Opening> history(long userId, Long before) {
        return mapper.history(userId, before).stream().map(this::toOpening).collect(Collectors.toList());
    }

    private Owned toOwned(OwnedRow row) {
        return new Owned(row.getCardId(), row.getStars(), row.getDuplicates(), row.isShowcased(),
                PetCardPlayRules.upgradeCost(row.getStars()));
    }

    private Opening toOpening(OpeningRow row) {
        try {
            return new Opening(row.getId().toString(), row.getPoolId(), row.getPoolName(),
                    row.getOpenedAt().toInstant(ZoneOffset.UTC).toString(),
                    json.readValue(row.getCards(), PULLS));
        } catch (IOException e) {
            throw new IllegalStateException("Invalid persisted card opening", e);
        }
    }
}

package com.cong.fishisland.mapper;

import com.cong.fishisland.model.dto.pet.PetCardPlay.Stock;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.Value;
import org.apache.ibatis.annotations.Param;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

public interface PetCardPlayMapper {
    void lockPlayer(@Param("userId") long userId);

    Player player(@Param("userId") long userId);

    String receipt(@Param("userId") long userId, @Param("requestId") String requestId);

    void saveReceipt(@Param("userId") long userId, @Param("requestId") String requestId, @Param("command") String command);

    List<Stock> stocks(@Param("userId") long userId);

    Stock stock(@Param("userId") long userId, @Param("pool") String pool);

    void addStock(@Param("userId") long userId, @Param("pool") String pool, @Param("quantity") int quantity);

    void claim(@Param("userId") long userId, @Param("day") LocalDate day, @Param("claims") int claims);

    void insertOpening(@Param("userId") long userId, @Param("opening") OpeningRow opening);

    void consumeStock(@Param("userId") long userId, @Param("pool") String pool, @Param("quantity") int quantity);

    void recordProgress(@Param("userId") long userId, @Param("quantity") int quantity, @Param("pending") Long pending);

    void finish(@Param("userId") long userId);

    List<OwnedRow> collection(@Param("userId") long userId);

    OwnedRow owned(@Param("userId") long userId, @Param("card") String card);

    void incrementDuplicates(@Param("userId") long userId, @Param("card") String card);

    void insertOwned(@Param("userId") long userId, @Param("card") String card);

    void upgrade(@Param("userId") long userId, @Param("card") String card,
                 @Param("stars") int stars, @Param("cost") int cost);

    void display(@Param("userId") long userId, @Param("card") String card, @Param("displayed") boolean displayed);

    OpeningRow opening(@Param("userId") long userId, @Param("id") long id);

    List<OpeningRow> history(@Param("userId") long userId, @Param("before") Long before);

    @Value
    class Player {
        LocalDate claimDay;
        int claims;
        long opened;
        Long pending;
    }

    @Value
    class OwnedRow {
        String cardId;
        int stars;
        long duplicates;
        boolean showcased;
    }

    @Data
    @AllArgsConstructor
    class OpeningRow {
        private Long id;
        private String poolId;
        private String poolName;
        private LocalDateTime openedAt;
        private String cards;
    }
}

package com.cong.fishisland.mapper;

import com.cong.fishisland.model.dto.pet.PetCardCatalog.Atlas;
import com.cong.fishisland.model.dto.pet.PetCardCatalog.Card;
import com.cong.fishisland.model.dto.pet.PetCardCatalog.Pool;
import lombok.Value;
import org.apache.ibatis.annotations.Param;

import java.util.List;

public interface PetCardCatalogMapper {
    List<Pool> pools(@Param("lock") boolean lock);

    Pool pool(@Param("id") String id);

    List<Card> poolCards(@Param("id") String id);

    List<Card> cards();

    long countCards(@Param("pool") String pool, @Param("query") String query);

    List<Card> cardPage(@Param("pool") String pool, @Param("query") String query, @Param("offset") long offset);

    List<Membership> cardMemberships(@Param("ids") List<String> ids);

    Integer lockCardVersion(@Param("id") String id);

    List<String> memberships(@Param("id") String id);

    boolean containsCard(@Param("pool") String pool, @Param("card") String card);

    boolean hasShinyCard(@Param("pool") String pool);

    List<String> grades(@Param("pool") String pool);

    void insertCard(@Param("card") Card card, @Param("stats") String stats, @Param("battle") String battle);

    void updateCard(@Param("card") Card card, @Param("stats") String stats, @Param("battle") String battle);

    void deleteMembership(@Param("pool") String pool, @Param("card") String card);

    void clearCover(@Param("pool") String pool, @Param("card") String card);

    void insertMembership(@Param("pool") String pool, @Param("card") String card);

    void insertPool(@Param("pool") Pool pool, @Param("weights") String weights, @Param("price") int price);

    void updatePool(@Param("pool") Pool pool, @Param("weights") String weights);

    List<Atlas> atlases();

    boolean hasAtlas(@Param("id") String id);

    void insertAtlas(Atlas atlas);

    Atlas atlas(@Param("id") String id);

    int insertSeed(@Param("id") String id);

    @Value
    class Membership {
        String cardId;
        String poolId;
    }
}

package com.cong.fishisland.service.pet;

import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;
import org.mybatis.spring.SqlSessionTemplate;
import org.springframework.context.ApplicationContext;

import java.time.LocalDate;

interface PetCardDatabaseFixtures {
    static PetCardDatabaseFixtures from(ApplicationContext context) {
        SqlSessionTemplate session = context.getBean(SqlSessionTemplate.class);
        session.getConfiguration().addMapper(PetCardDatabaseFixtures.class);
        return session.getMapper(PetCardDatabaseFixtures.class);
    }

    @Select("SELECT COUNT(*) FROM pet_card_pool_entry")
    int countMemberships();

    // A real transactional table lets the mocked points service prove that a ledger failure rolls back the debit.
    @Update("CREATE TABLE test_card_wallet (user_id BIGINT PRIMARY KEY, spent INT NOT NULL) ENGINE=InnoDB")
    void createWallets();

    @Insert("INSERT INTO test_card_wallet (user_id, spent) VALUES (8, 0), (9, 0), (10, 0), (13, 0)")
    void seedWallets();

    @Select("SELECT spent FROM test_card_wallet WHERE user_id = #{userId}")
    int spent(@Param("userId") long userId);

    @Update("UPDATE test_card_wallet SET spent = spent + #{amount} WHERE user_id = #{userId}")
    void spend(@Param("userId") long userId, @Param("amount") int amount);

    @Update("UPDATE pet_card_player SET claim_day = #{day} WHERE user_id = #{userId}")
    void setClaimDay(@Param("userId") long userId, @Param("day") LocalDate day);

    @Update("UPDATE pet_card_player SET opened = #{opened} WHERE user_id = #{userId}")
    void setOpened(@Param("userId") long userId, @Param("opened") long opened);

    @Insert("INSERT INTO pet_card_opening (user_id, pool_id, pool_name, opened_at, cards) "
            + "SELECT user_id, pool_id, pool_name, opened_at, cards FROM pet_card_opening WHERE id = #{id}")
    void copyOpening(@Param("id") String id);

    @Update("UPDATE pet_card_collection SET duplicates = #{duplicates} WHERE user_id = #{userId} AND card_id = #{card}")
    void setDuplicates(@Param("userId") long userId, @Param("card") String card, @Param("duplicates") long duplicates);
}

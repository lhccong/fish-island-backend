package com.cong.fishisland.service.pet;

import com.cong.fishisland.mapper.PetCardPlayMapper;
import com.cong.fishisland.mapper.PetCardPlayMapper.ShowcaseStatsRow;
import com.cong.fishisland.model.dto.pet.PetCardPassive;
import com.cong.fishisland.model.dto.pet.PetCardPlay.CollectedCard;
import com.cong.fishisland.model.dto.pet.PetCardPlay.ShowcaseCard;
import com.cong.fishisland.model.vo.pet.PetEquipStatsVO;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static com.cong.fishisland.service.pet.PetCardPlayRules.statAtStars;

@Service
@RequiredArgsConstructor
public class PetCardShowcaseStats {
    private final PetCardPlayMapper mapper;
    private final ObjectMapper json;

    public List<CollectedCard> addTo(long userId, PetEquipStatsVO totals) {
        List<ShowcaseStatsRow> rows = mapper.showcaseStats(userId);
        List<CollectedCard> showcase = new ArrayList<>(rows.size());
        List<ShowcaseCard> showcaseDetails = new ArrayList<>(rows.size());
        List<PetCardPassive> passives = new ArrayList<>(rows.size());
        Set<String> cardIds = new HashSet<>();
        for (ShowcaseStatsRow row : rows) {
            CollectedCard card = row.getCard();
            JsonNode stats = readJson(row.getStats());
            int stars = card.getStars();
            Map<String, Integer> bonuses = new HashMap<>(4);
            bonuses.put("attack", statAtStars(stats.path("attack").asInt(), stars));
            bonuses.put("defense", statAtStars(stats.path("defense").asInt(), stars));
            bonuses.put("health", statAtStars(stats.path("health").asInt(), stars));
            bonuses.put("speed", statAtStars(stats.path("speed").asInt(), stars));
            totals.setTotalBaseAttack(totals.getTotalBaseAttack() + bonuses.get("attack"));
            totals.setTotalBaseDefense(totals.getTotalBaseDefense() + bonuses.get("defense"));
            totals.setTotalBaseHp(totals.getTotalBaseHp() + bonuses.get("health"));
            totals.setSpeed(totals.getSpeed() + bonuses.get("speed"));
            showcase.add(card);
            cardIds.add(card.getId());
            JsonNode battle = readJson(row.getBattle());
            JsonNode effect = "effect".equals(battle.path("kind").asText()) ? battle : null;
            if (effect != null) {
                passives.add(new PetCardPassive(card, effect));
            }
            showcaseDetails.add(new ShowcaseCard(card, bonuses, effect));
        }
        totals.setCardPassives(passives);
        totals.setShowcaseCards(showcaseDetails);
        PetCardBonds.addTo(cardIds, totals);
        return showcase;
    }

    private JsonNode readJson(String value) {
        try {
            return json.readTree(value);
        } catch (JsonProcessingException e) {
            throw new IllegalStateException("Invalid persisted showcase card definition", e);
        }
    }
}

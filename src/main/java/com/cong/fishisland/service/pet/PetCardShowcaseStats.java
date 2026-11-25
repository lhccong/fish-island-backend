package com.cong.fishisland.service.pet;

import com.cong.fishisland.mapper.PetCardPlayMapper;
import com.cong.fishisland.mapper.PetCardPlayMapper.ShowcaseStatsRow;
import com.cong.fishisland.model.dto.pet.PetCardPassive;
import com.cong.fishisland.model.dto.pet.PetCardPlay.CollectedCard;
import com.cong.fishisland.model.vo.pet.PetEquipStatsVO;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

import static com.cong.fishisland.service.pet.PetCardPlayRules.statAtStars;

@Service
@RequiredArgsConstructor
public class PetCardShowcaseStats {
    private final PetCardPlayMapper mapper;
    private final ObjectMapper json;

    public List<CollectedCard> addTo(long userId, PetEquipStatsVO totals) {
        List<ShowcaseStatsRow> rows = mapper.showcaseStats(userId);
        List<CollectedCard> showcase = new ArrayList<>(rows.size());
        List<PetCardPassive> passives = new ArrayList<>(rows.size());
        for (ShowcaseStatsRow row : rows) {
            CollectedCard card = row.getCard();
            JsonNode stats = readJson(row.getStats());
            int stars = card.getStars();
            totals.setTotalBaseAttack(totals.getTotalBaseAttack() + statAtStars(stats.path("attack").asInt(), stars));
            totals.setTotalBaseDefense(totals.getTotalBaseDefense() + statAtStars(stats.path("defense").asInt(), stars));
            totals.setTotalBaseHp(totals.getTotalBaseHp() + statAtStars(stats.path("health").asInt(), stars));
            totals.setSpeed(totals.getSpeed() + statAtStars(stats.path("speed").asInt(), stars));
            showcase.add(card);
            JsonNode battle = readJson(row.getBattle());
            if ("effect".equals(battle.path("kind").asText())) {
                passives.add(new PetCardPassive(card, battle));
            }
        }
        totals.setCardPassives(passives);
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

package com.cong.fishisland.service.pet;

import com.cong.fishisland.common.ResultUtils;
import com.cong.fishisland.config.JsonConfig;
import com.cong.fishisland.model.vo.pet.PetTreasureVO;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.http.converter.json.Jackson2ObjectMapperBuilder;
import static org.junit.jupiter.api.Assertions.*;

class PetTreasureSerializationTest {
    @Test
    void realJsonConfigSerializesTreasureLongTimestampsAsStringsAndIntValuesAsNumbers() throws Exception {
        ObjectMapper mapper = new JsonConfig().jacksonObjectMapper(new Jackson2ObjectMapperBuilder());
        PetTreasureVO vo = new PetTreasureVO();
        vo.setServerNow(1794873600000L);
        vo.setResetAt(1794931200000L);
        vo.setNextAvailableAt(0L);
        JsonNode ready = mapper.readTree(mapper.writeValueAsString(ResultUtils.success(vo)));
        assertEquals(0, ready.get("code").intValue());
        JsonNode data = ready.get("data");
        assertTrue(data.get("serverNow").isTextual());
        assertEquals("1794873600000", data.get("serverNow").textValue());
        assertEquals("1794931200000", data.get("resetAt").textValue());
        assertEquals("0", data.get("nextAvailableAt").textValue());
        assertTrue(data.get("used").isIntegralNumber());

        PetTreasureVO.Active active = new PetTreasureVO.Active();
        active.setId("dig");
        active.setSiteId("grove");
        active.setReadyAt(1794873605000L);
        vo.setActive(active);
        vo.setUsed(1);
        vo.setNextAvailableAt(1794877205000L);
        JsonNode digging = mapper.readTree(mapper.writeValueAsString(vo));
        assertEquals("1794873605000", digging.get("active").get("readyAt").textValue());
        assertEquals("1794877205000", digging.get("nextAvailableAt").textValue());

        vo.setActive(null);
        PetTreasureVO.Reward reward = new PetTreasureVO.Reward();
        reward.setId("dig");
        reward.setSiteId("grove");
        reward.setPoints(7);
        vo.setLastReward(reward);
        JsonNode settled = mapper.readTree(mapper.writeValueAsString(vo));
        assertTrue(settled.get("lastReward").get("points").isIntegralNumber());
        assertEquals(7, settled.get("lastReward").get("points").intValue());
    }
}

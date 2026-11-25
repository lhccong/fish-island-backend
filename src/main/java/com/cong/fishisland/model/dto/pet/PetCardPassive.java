package com.cong.fishisland.model.dto.pet;

import com.cong.fishisland.model.dto.pet.PetCardPlay.CollectedCard;
import com.fasterxml.jackson.databind.JsonNode;
import lombok.Value;

@Value
public class PetCardPassive {
    CollectedCard card;
    JsonNode effect;
}

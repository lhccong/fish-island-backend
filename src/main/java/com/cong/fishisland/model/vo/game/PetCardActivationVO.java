package com.cong.fishisland.model.vo.game;

import com.cong.fishisland.model.dto.pet.PetCardPlay.CollectedCard;
import lombok.Value;

import java.io.Serializable;

@Value
public class PetCardActivationVO implements Serializable {
    String side;
    CollectedCard card;
    String effectName;
}

package com.cong.fishisland.model.fishbattle.battle;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PendingAttackState {
    private String sourceEntityId;
    private String sourceType;
    private String castInstanceId;
    private String slot;
    private String targetEntityId;
    private String targetType;
    private String skillId;
    private Double damage;
    private String damageType;
    private Long impactAt;
}

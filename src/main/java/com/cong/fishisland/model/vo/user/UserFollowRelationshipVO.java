package com.cong.fishisland.model.vo.user;

import lombok.Value;

@Value
public class UserFollowRelationshipVO {
    boolean following;
    boolean followedBy;
}

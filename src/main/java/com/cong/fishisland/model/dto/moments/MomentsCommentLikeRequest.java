package com.cong.fishisland.model.dto.moments;

import lombok.Data;

@Data
public class MomentsCommentLikeRequest {

    private Long commentId;

    private Boolean liked;
}

package com.cong.fishisland.model.vo.user;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class ChatBubbleArtwork {
    private String imageUrl;
    private String darkImageUrl;
    private String textTone;
}

package com.cong.fishisland.model.entity.user;

import com.cong.fishisland.model.vo.user.ChatBubbleArtwork;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class ChatBubbleProduct {
    private String id;
    private String name;
    private int points;
    private String imageUrl;
    private String darkImageUrl;
    private String textTone;

    public ChatBubbleArtwork artwork() {
        return imageUrl == null ? null : new ChatBubbleArtwork(imageUrl, darkImageUrl, textTone);
    }
}

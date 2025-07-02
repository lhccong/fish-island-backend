package com.cong.fishisland.model.dto.game;

import lombok.Data;

/**
 * 卧底猜平民词请求
 *
 * @author cong
 */
@Data
public class UndercoverGuessRequest {

    /**
     * 房间ID
     */
    private String roomId;

    /**
     * 猜测的词语
     */
    private String guessWord;
} 
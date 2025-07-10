package com.cong.fishisland.model.dto.game;

import com.cong.fishisland.model.ws.request.MessageWrapper;
import lombok.Data;

import java.io.Serializable;

/**
 * 你画我猜游戏猜词请求
 *
 * @author cong
 */
@Data
public class DrawGuessRequest implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * 房间ID
     */
    private String roomId;

    /**
     * 猜测的词语
     */
    private String guessWord;

    private MessageWrapper messageWrapper;
} 
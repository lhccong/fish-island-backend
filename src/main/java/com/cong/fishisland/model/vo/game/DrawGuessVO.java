package com.cong.fishisland.model.vo.game;

import lombok.Data;

import java.io.Serializable;
import java.util.Date;

/**
 * 你画我猜游戏猜词记录VO
 *
 * @author cong
 */
@Data
public class DrawGuessVO implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * 用户ID
     */
    private Long userId;

    /**
     * 用户名
     */
    private String userName;

    /**
     * 用户头像
     */
    private String userAvatar;

    /**
     * 猜测的词语
     */
    private String guessWord;

    /**
     * 是否猜中
     */
    private Boolean isCorrect;

    /**
     * 猜词时间
     */
    private Date guessTime;
} 
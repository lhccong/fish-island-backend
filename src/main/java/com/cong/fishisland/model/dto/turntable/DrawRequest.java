package com.cong.fishisland.model.dto.turntable;

import lombok.Data;

import java.io.Serializable;

/**
 * 抽奖请求
 * @author cong
 */
@Data
public class DrawRequest implements Serializable {
    /**
     * 转盘ID
     */
    private Long turntableId;

    /**
     * 抽奖次数（1单抽，10十连抽）
     */
    private Integer drawCount;

    private static final long serialVersionUID = 1L;
}

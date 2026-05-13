package com.cong.fishisland.model.dto.redeem;

import lombok.Data;

import java.io.Serializable;

/**
 * 兑换码使用请求
 *
 * @author cong
 */
@Data
public class RedeemCodeUseRequest implements Serializable {

    /**
     * 兑换码
     */
    private String code;

    private static final long serialVersionUID = 1L;
}

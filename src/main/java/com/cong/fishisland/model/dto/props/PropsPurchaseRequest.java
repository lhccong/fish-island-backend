package com.cong.fishisland.model.dto.props;

import lombok.Data;

import java.io.Serializable;

/**
 * 道具购买请求
 *
 * @author cong
 */
@Data
public class PropsPurchaseRequest implements Serializable {

    /**
     * 道具ID
     */
    private Long propsId;

    private static final long serialVersionUID = 1L;
} 
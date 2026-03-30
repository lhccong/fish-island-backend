package com.cong.fishisland.model.dto.turntable;

import com.cong.fishisland.common.PageRequest;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.io.Serializable;

/**
 * 转盘查询请求
 * @author cong
 */
@EqualsAndHashCode(callSuper = true)
@Data
public class TurntableQueryRequest extends PageRequest implements Serializable {
    /**
     * 转盘类型 1-宠物装备转盘 2-称号转盘
     */
    private Integer type;

    private static final long serialVersionUID = 1L;
}

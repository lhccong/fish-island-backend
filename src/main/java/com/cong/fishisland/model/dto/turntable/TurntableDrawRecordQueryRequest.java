package com.cong.fishisland.model.dto.turntable;

import com.cong.fishisland.common.PageRequest;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.io.Serializable;

/**
 * 抽奖记录查询请求
 * @author cong
 */
@EqualsAndHashCode(callSuper = true)
@Data
public class TurntableDrawRecordQueryRequest extends PageRequest implements Serializable {
    /**
     * 转盘ID
     */
    private Long turntableId;

    private static final long serialVersionUID = 1L;
}

package com.cong.fishisland.model.dto.donation;

import com.cong.fishisland.common.PageRequest;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.io.Serializable;

/**
 * 打赏明细记录查询请求
 *
 * @author <a href="https://github.com/lhccong">程序员聪</a>
 */
@EqualsAndHashCode(callSuper = true)
@Data
public class DonationDetailRecordsQueryRequest extends PageRequest implements Serializable {

    /**
     * 打赏用户ID（可选，按用户筛选）
     */
    private Long userId;

    private static final long serialVersionUID = 1L;
}

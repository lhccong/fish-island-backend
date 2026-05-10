package com.cong.fishisland.model.vo.donation;

import com.cong.fishisland.model.entity.donation.DonationDetailRecords;
import com.cong.fishisland.model.vo.user.LoginUserVO;
import lombok.Data;
import org.springframework.beans.BeanUtils;

import java.io.Serializable;
import java.math.BigDecimal;
import java.util.Date;

/**
 * 打赏明细记录 VO（每次打赏独立展示，不累加）
 *
 * @author <a href="https://github.com/lhccong">程序员聪</a>
 */
@Data
public class DonationDetailRecordsVO implements Serializable {

    /**
     * 明细记录ID
     */
    private Long id;

    /**
     * 打赏用户ID
     */
    private Long userId;

    /**
     * 本次打赏金额（元）
     */
    private BigDecimal amount;

    /**
     * 打赏留言/备注
     */
    private String remark;

    /**
     * 打赏用户信息
     */
    private LoginUserVO donorUser;

    /**
     * 打赏时间
     */
    private Date createTime;

    private static final long serialVersionUID = 1L;

    /**
     * 实体转 VO
     */
    public static DonationDetailRecordsVO objToVo(DonationDetailRecords entity) {
        if (entity == null) {
            return null;
        }
        DonationDetailRecordsVO vo = new DonationDetailRecordsVO();
        BeanUtils.copyProperties(entity, vo);
        return vo;
    }
}

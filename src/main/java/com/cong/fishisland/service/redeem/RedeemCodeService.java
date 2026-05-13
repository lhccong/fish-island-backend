package com.cong.fishisland.service.redeem;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.IService;
import com.cong.fishisland.model.dto.redeem.RedeemCodeAddRequest;
import com.cong.fishisland.model.dto.redeem.RedeemCodeQueryRequest;
import com.cong.fishisland.model.dto.redeem.RedeemCodeUseRequest;
import com.cong.fishisland.model.entity.redeem.RedeemCode;
import com.cong.fishisland.model.vo.redeem.RedeemCodeUseResultVO;
import com.cong.fishisland.model.vo.redeem.RedeemCodeVO;

import java.util.List;

/**
 * 兑换码 Service
 *
 * @author cong
 */
public interface RedeemCodeService extends IService<RedeemCode> {

    /**
     * 创建兑换码（支持批量）
     *
     * @param request 创建请求
     * @return 生成的兑换码列表
     */
    List<String> addRedeemCode(RedeemCodeAddRequest request);

    /**
     * 使用兑换码
     *
     * @param request 使用请求
     * @return 兑换结果
     */
    RedeemCodeUseResultVO useRedeemCode(RedeemCodeUseRequest request);

    /**
     * 分页查询兑换码（管理员）
     *
     * @param request 查询请求
     * @return 分页结果
     */
    Page<RedeemCodeVO> listRedeemCodePage(RedeemCodeQueryRequest request);

    /**
     * 删除兑换码（管理员）
     *
     * @param id 兑换码ID
     * @return 是否成功
     */
    boolean deleteRedeemCode(Long id);
}

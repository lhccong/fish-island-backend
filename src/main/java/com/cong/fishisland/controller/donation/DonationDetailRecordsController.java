package com.cong.fishisland.controller.donation;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.cong.fishisland.common.BaseResponse;
import com.cong.fishisland.common.ErrorCode;
import com.cong.fishisland.common.ResultUtils;
import com.cong.fishisland.common.exception.ThrowUtils;
import com.cong.fishisland.model.dto.donation.DonationDetailRecordsQueryRequest;
import com.cong.fishisland.model.vo.donation.DonationDetailRecordsVO;
import com.cong.fishisland.service.DonationDetailRecordsService;
import io.swagger.annotations.ApiOperation;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.annotation.Resource;

/**
 * 打赏明细记录接口
 * <p>
 * 展示每次打赏的用户信息、金额、留言和打赏时间，相同用户不累加，每次独立一条。
 *
 * @author <a href="https://github.com/lhccong">程序员聪</a>
 */
@RestController
@RequestMapping("/donation/detail")
@Slf4j
public class DonationDetailRecordsController {

    @Resource
    private DonationDetailRecordsService donationDetailRecordsService;

    /**
     * 分页获取打赏明细列表（按打赏时间倒序）
     * <p>
     * 前端展示用，无需登录，每页最多50条。
     * 可传 userId 按用户筛选，不传则查全部。
     */
    @PostMapping("/list/page/vo")
    @ApiOperation(value = "分页获取打赏明细列表", notes = "按打赏时间倒序，每次打赏独立展示，不累加")
    public BaseResponse<Page<DonationDetailRecordsVO>> listDetailVoByPage(
            @RequestBody DonationDetailRecordsQueryRequest queryRequest) {
        ThrowUtils.throwIf(queryRequest == null, ErrorCode.PARAMS_ERROR);
        return ResultUtils.success(donationDetailRecordsService.listDetailVOByPage(queryRequest));
    }
}

package com.cong.fishisland.controller.donation;

import cn.dev33.satoken.annotation.SaCheckRole;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.cong.fishisland.common.BaseResponse;
import com.cong.fishisland.common.DeleteRequest;
import com.cong.fishisland.common.ErrorCode;
import com.cong.fishisland.common.ResultUtils;
import com.cong.fishisland.common.exception.BusinessException;
import com.cong.fishisland.common.exception.ThrowUtils;
import com.cong.fishisland.constant.UserConstant;
import com.cong.fishisland.model.dto.donation.DonationRecordsAddRequest;
import com.cong.fishisland.model.dto.donation.DonationRecordsQueryRequest;
import com.cong.fishisland.model.dto.donation.DonationRecordsUpdateRequest;
import com.cong.fishisland.model.entity.donation.DonationRecords;
import com.cong.fishisland.model.entity.user.User;
import com.cong.fishisland.model.vo.donation.DonationRecordsVO;
import com.cong.fishisland.service.DonationRecordsService;
import com.cong.fishisland.service.UserService;
import io.swagger.annotations.ApiOperation;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

import javax.annotation.Resource;

/**
 * 打赏记录接口
 *
 * @author Shing
 * date 22/4/2025
 */
@RestController
@RequestMapping("/donation")
@Slf4j
public class DonationRecordsController {

    @Resource
    private UserService userService;

    @Resource
    private DonationRecordsService donationRecordsService;

    // region 增删改查
    @PostMapping("/add")
    @SaCheckRole(UserConstant.ADMIN_ROLE)
    @ApiOperation(value = "添加打赏记录")
    public BaseResponse<Long> addDonationRecords(@RequestBody DonationRecordsAddRequest donationRecordsAddRequest) {
        ThrowUtils.throwIf(donationRecordsAddRequest == null, ErrorCode.PARAMS_ERROR);
        Long donationRecordsId = donationRecordsService.createRecord(donationRecordsAddRequest);
        return ResultUtils.success(donationRecordsId);
    }

    @PostMapping("/delete")
    @ApiOperation(value = "删除打赏记录")
    @SaCheckRole(UserConstant.ADMIN_ROLE)
    public BaseResponse<Boolean> deleteDonation(@RequestBody DeleteRequest deleteRequest) {
        if (deleteRequest == null || Long.parseLong(deleteRequest.getId()) <= 0) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }
        boolean b = donationRecordsService.removeById(Long.parseLong(deleteRequest.getId()));
        return ResultUtils.success(b);
    }

    /**
     * 更新打赏记录（仅管理员）
     *
     * @param donationRecordsUpdateRequest 打赏记录更新请求
     * @return {@link BaseResponse}<{@link Boolean}>
     */
    @PostMapping("/update")
    @SaCheckRole(UserConstant.ADMIN_ROLE)
    @ApiOperation(value = "更新打赏记录（仅管理员）")
    public BaseResponse<Boolean> updateDonationRecords(@RequestBody DonationRecordsUpdateRequest donationRecordsUpdateRequest) {
        if (donationRecordsUpdateRequest == null || donationRecordsUpdateRequest.getId() <= 0) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }
        long id = donationRecordsUpdateRequest.getId();
        // 判断是否存在
        DonationRecords oldDonationRecords = donationRecordsService.getById(id);
        ThrowUtils.throwIf(oldDonationRecords == null, ErrorCode.NOT_FOUND_ERROR);
        // 校验 userId 是否存在用户表中
        Long userId = donationRecordsUpdateRequest.getUserId();
        if (userId != null) {
            User user = userService.getById(userId);
            ThrowUtils.throwIf(user == null, ErrorCode.PARAMS_ERROR, "该打赏用户不存在");
        }
        // 构建更新对象
        DonationRecords donationRecords = new DonationRecords();
        donationRecords.setId(id);
        donationRecords.setAmount(donationRecordsUpdateRequest.getAmount());
        donationRecords.setRemark(donationRecordsUpdateRequest.getRemark());
        donationRecords.setUserId(userId);

        boolean result = donationRecordsService.updateById(donationRecords);
        return ResultUtils.success(result);
    }

    /**
     * 根据 id 获取包装类
     *
     * @param id 编号
     * @return {@link BaseResponse}<{@link DonationRecordsVO}>
     */
    @GetMapping("/get/vo")
    @ApiOperation(value = "根据 id 获取打赏记录包装类")
    public BaseResponse<DonationRecordsVO> getDonationRecordsVoById(long id) {
        if (id <= 0) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }
        DonationRecords donationRecords = donationRecordsService.getById(id);
        if (donationRecords == null) {
            throw new BusinessException(ErrorCode.NOT_FOUND_ERROR);
        }
       return ResultUtils.success(donationRecordsService.getRecordVO(donationRecords));
    }

    /**
     * 分页获取列表（仅管理员）
     *
     * @param donationRecordsQueryRequest 用户查询请求
     * @return {@link BaseResponse}<{@link Page}<{@link DonationRecords}>>
     */
    @PostMapping("/list/page")
    @SaCheckRole(UserConstant.ADMIN_ROLE)
    @ApiOperation(value = "分页获取打赏记录列表（仅管理员）")
    public BaseResponse<Page<DonationRecords>> listDonationByPage(@RequestBody DonationRecordsQueryRequest donationRecordsQueryRequest) {
        long current = donationRecordsQueryRequest.getCurrent();
        long size = donationRecordsQueryRequest.getPageSize();
        Page<DonationRecords> donationRecordsPage = donationRecordsService.page(new Page<>(current, size),
                donationRecordsService.getQueryWrapper(donationRecordsQueryRequest));
        return ResultUtils.success(donationRecordsPage);
    }

    /**
     * 分页获取列表（封装类）
     *
     * @param donationRecordsQueryRequest 用户查询请求
     * @return {@link BaseResponse}<{@link Page}<{@link DonationRecords}>>
     */
    @PostMapping("/list/page/vo")
    @ApiOperation(value = "分页获取打赏记录列表（封装类）")
    public BaseResponse<Page<DonationRecordsVO>> listDonationVoByPage(@RequestBody DonationRecordsQueryRequest donationRecordsQueryRequest) {
        long current= donationRecordsQueryRequest.getCurrent();
        long size = donationRecordsQueryRequest.getPageSize();
        // 限制爬虫
        ThrowUtils.throwIf(size > 20, ErrorCode.PARAMS_ERROR);
        Page<DonationRecords> donationRecordsVOPage = donationRecordsService.page(new Page<>(current, size),
                donationRecordsService.getQueryWrapper(donationRecordsQueryRequest));
        if (donationRecordsVOPage.getRecords().isEmpty()){
            return ResultUtils.success(new Page<>());
        }
        return ResultUtils.success(donationRecordsService.getRecordsVOPage(donationRecordsVOPage));
    }

    // endregion

}

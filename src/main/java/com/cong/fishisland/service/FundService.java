package com.cong.fishisland.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.cong.fishisland.model.dto.fund.AddFundRequest;
import com.cong.fishisland.model.dto.fund.DeleteFundRequest;
import com.cong.fishisland.model.dto.fund.EditFundRequest;
import com.cong.fishisland.model.dto.fund.UpdateFundRequest;
import com.cong.fishisland.model.entity.fund.Fund;
import com.cong.fishisland.model.vo.fund.FundListVO;

/**
 * @author shing
 * @description 针对表【fund(基金持仓表)】的数据库操作Service
 */
public interface FundService extends IService<Fund> {

    /**
     * 添加基金
     * 
     * @param addFundRequest 添加基金请求
     * @param userId 用户ID
     * @return 是否成功
     */
    Boolean addFund(AddFundRequest addFundRequest, Long userId);

    /**
     * 删除基金
     * 
     * @param deleteFundRequest 删除基金请求
     * @param userId 用户ID
     * @return 是否成功
     */
    Boolean deleteFund(DeleteFundRequest deleteFundRequest, Long userId);

    /**
     * 编辑基金
     * 
     * @param editFundRequest 编辑基金请求
     * @param userId 用户ID
     * @return 是否成功
     */
    Boolean editFund(EditFundRequest editFundRequest, Long userId);

    /**
     * 管理员更新基金（直接修改份额和成本）
     * 
     * @param updateFundRequest 更新基金请求
     * @return 是否成功
     */
    Boolean updateFund(UpdateFundRequest updateFundRequest);


    /**
     * 获取用户基金持仓列表（包含实时数据）
     * 
     * @param userId 用户ID
     * @return 基金持仓列表
     */
    FundListVO getFundList(Long userId);
}

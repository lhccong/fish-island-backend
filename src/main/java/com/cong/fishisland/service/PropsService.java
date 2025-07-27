package com.cong.fishisland.service;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.IService;
import com.cong.fishisland.model.dto.props.PropsQueryRequest;
import com.cong.fishisland.model.dto.props.PropsPurchaseRequest;
import com.cong.fishisland.model.entity.props.Props;
import com.cong.fishisland.model.vo.props.PropsVO;

/**
* @author cong
* @description 针对表【props(道具表)】的数据库操作Service
* @createDate 2025-05-10 16:00:00
*/
public interface PropsService extends IService<Props> {

    /**
     * 获取查询条件
     *
     * @param propsQueryRequest 道具查询请求
     * @return {@link QueryWrapper}<{@link Props}>
     */
    QueryWrapper<Props> getQueryWrapper(PropsQueryRequest propsQueryRequest);

    /**
     * 获取道具封装
     *
     * @param props 道具
     * @return {@link PropsVO}
     */
    PropsVO getPropsVO(Props props);

    /**
     * 分页获取道具封装
     *
     * @param propsPage 道具分页
     * @return {@link Page}<{@link PropsVO}>
     */
    Page<PropsVO> getPropsVOPage(Page<Props> propsPage);

    /**
     * 购买道具
     *
     * @param propsPurchaseRequest 道具购买请求
     * @return 是否成功
     */
    Boolean purchaseProps(PropsPurchaseRequest propsPurchaseRequest);
} 
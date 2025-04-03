package com.cong.fishisland.service;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.cong.fishisland.model.dto.mockInterview.MockInterviewAddRequest;
import com.cong.fishisland.model.dto.mockInterview.MockInterviewEventRequest;
import com.cong.fishisland.model.dto.mockInterview.MockInterviewQueryRequest;
import com.cong.fishisland.model.entity.mockInterview.MockInterview;
import com.baomidou.mybatisplus.extension.service.IService;

/**
* @author Shing
* @description 针对表【mock_interview(模拟面试)】的数据库操作Service
* @createDate 2025-04-02 18:21:40
*/
public interface MockInterviewService extends IService<MockInterview> {

    /**
     * 创建模拟面试
     * @param mockInterviewAddRequest
     * @return
     */
    Long createMockInterview(MockInterviewAddRequest mockInterviewAddRequest);

    /**
     * 构造查询条件
     *
     * @param mockInterviewQueryRequest
     * @return
     */
    QueryWrapper<MockInterview> getQueryWrapper(MockInterviewQueryRequest mockInterviewQueryRequest);

    /**
     * 处理模拟面试事件
     * @param mockInterviewEventRequest
     * @return AI 给出的回复
     */
    String handleMockInterviewEvent(MockInterviewEventRequest mockInterviewEventRequest);

}

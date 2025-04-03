package com.cong.fishisland.model.dto.mockInterview;


import com.cong.fishisland.common.PageRequest;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.io.Serializable;

/**
 * 查询模拟面试请求
 */
@EqualsAndHashCode(callSuper = true)
@Data
public class MockInterviewQueryRequest  extends PageRequest implements Serializable {

    /**
     * id
     */
    private Long id;

    /**
     * 工作年限
     */
    private String workExperience;

    /**
     * 工作岗位
     */
    private String jobPosition;

    /**
     * 面试难度
     */
    private String difficulty;

    /**
     * 状态（0-待开始、1-进行中、2-已结束）
     */
    private Integer status;

    /**
     * 创建用户 id
     */
    private Long userId;

    private static final long serialVersionUID = 1L;
}

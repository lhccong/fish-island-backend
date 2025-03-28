package com.github.yuyuanweb.mianshiyaplugin.model.response;

import lombok.Data;

import java.io.Serializable;
import java.util.Date;
import java.util.List;

/**
 * 题库视图
 *
 * @author pine
 */
@Data
public class QuestionBank implements Serializable {
    /**
     * id
     */
    private Long id;

    /**
     * 标题
     */
    private String title;

    /**
     * 描述
     */
    private String description;

    /**
     * 图片
     */
    private String picture;


    /**
     * 标签列表
     */
    private List<String> tagList;
    /**
     * 浏览数
     */
    private Integer viewNum;

    /**
     * 状态：0-待审核, 1-通过, 2-拒绝
     */
    private Integer reviewStatus;

    /**
     * 审核信息
     */
    private String reviewMessage;

    /**
     * 审核人 id
     */
    private Long reviewerId;

    /**
     * 审核时间
     */
    private Date reviewTime;

    /**
     * 优先级
     */
    private Integer priority;

    /**
     * 创建用户 id
     */
    private Long userId;

    /**
     * 编辑时间
     */
    private Date editTime;

    /**
     * 创建时间
     */
    private Date createTime;

    /**
     * 更新时间
     */
    private Date updateTime;

    /**
     * 会员题目数量
     */
    private Long vipCount;
    /**
     * 所有题目数量
     */
    private Long allCount;
    /**
     * 是否所有题目是会员
     */
    private Boolean isAllQuestionVip;

    private static final long serialVersionUID = 1L;
}
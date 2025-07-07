package com.cong.fishisland.model.dto.tags;

import lombok.Data;

import java.io.Serializable;

/**
 * 更新标签请求
 *
 * @author <a href="https://github.com/lhccong">聪</a>
 */
@Data
public class TagsUpdateRequest implements Serializable {

    /**
     *  id
     */
    private Long id;

    /**
     * 标签名
     */
    private String tagsName;

    /**
     * 类型
     */
    private Integer type;

    /**
     * 图标
     */
    private String icon;

    /**
     * 颜色
     */
    private String color;

    /**
     * 排序
     */
    private Integer sort;
    
    private static final long serialVersionUID = 1L;
}
package com.cong.fishisland.model.dto.tags;

import lombok.Data;

import java.io.Serializable;

/**
 * 编辑标签请求
 *
 * @author <a href="https://github.com/lhccong">聪</a>
 */
@Data
public class TagsEditRequest implements Serializable {

    private Long id;

    private Long userId;

    private String tagsName;

    private Integer type;
    
    private static final long serialVersionUID = 1L;
}
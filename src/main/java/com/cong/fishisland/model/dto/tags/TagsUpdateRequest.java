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

    private Long id;

    private String tagsName;

    private Integer type;
    
    private static final long serialVersionUID = 1L;
}
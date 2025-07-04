package com.cong.fishisland.model.dto.tags;

import lombok.Data;

import java.io.Serializable;

/**
 * 创建标签请求
 *
 * @author <a href="https://github.com/lhccong">聪</a>
 */
@Data
public class TagsAddRequest implements Serializable {

    private String tagsName;
    
    private static final long serialVersionUID = 1L;
}
package com.cong.fishisland.model.dto.tags;

import com.cong.fishisland.common.PageRequest;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.io.Serializable;

/**
 * 查询标签请求
 *
 * @author <a href="https://github.com/lhccong">聪</a>
 */
@EqualsAndHashCode(callSuper = true)
@Data
public class TagsQueryRequest extends PageRequest implements Serializable {

    private Long id;

    private String tagsName;

    private Integer type;

    private static final long serialVersionUID = 1L;
}
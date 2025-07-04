package com.cong.fishisland.model.dto.post;

import com.cong.fishisland.common.PageRequest;
import java.io.Serializable;
import java.util.List;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 查询请求
 * # @author <a href="https://github.com/lhccong">程序员聪</a>
 */
@EqualsAndHashCode(callSuper = true)
@Data
public class PostQueryRequest extends PageRequest implements Serializable {

    /**
     * 标题
     */
    private String title;

    /**
     * 内容
     */
    private String content;

    /**
     * 标签列表
     */
    private List<String> tags;

    /**
     * 创建用户 id
     */
    private Long userId;

    /**
     * 是否加精（0-普通，1-加精）
     */
    private Integer isFeatured;

    /**
     * 搜索词
     */
    private String searchText;

    private static final long serialVersionUID = 1L;
}
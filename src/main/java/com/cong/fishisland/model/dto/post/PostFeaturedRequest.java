package com.cong.fishisland.model.dto.post;

import lombok.Data;

import java.io.Serializable;

/**
 * 帖子加精请求
 * @author 许林涛
 * @date 2025年07月08日 9:03
 */
@Data
public class PostFeaturedRequest implements Serializable {

    /**
     * id
     */
    private Long id;

    /**
     * 是否加精
     */
    private Integer isFeatured;
    private static final long serialVersionUID = 1L;
}

package com.cong.fishisland.model.dto.post;

import lombok.Data;

import java.io.Serializable;

/**
 * 帖子随机点赞用户请求
 * # @author <a href="https://github.com/lhccong">程序员聪</a>
 */
@Data
public class PostRandomThumbRequest implements Serializable {

    /**
     * 帖子id
     */
    private Long postId;

    /**
     * 随机数（不能大于点赞列表数量）
     */
    private Integer randomIndex;

    private static final long serialVersionUID = 1L;
}




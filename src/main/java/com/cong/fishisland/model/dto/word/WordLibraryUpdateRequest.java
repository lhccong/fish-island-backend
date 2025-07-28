package com.cong.fishisland.model.dto.word;

import lombok.Data;

import java.io.Serializable;

/**
 * 词库更新请求
 * @author 许林涛
 * @date 2025年07月28日 10:43
 */
@Data
public class WordLibraryUpdateRequest implements Serializable {

    /**
     * 词库ID
     */
    private Long id;

    /**
     * 词语名称
     */
    private String word;

    /**
     * 词库分类
     */
    private String category;

    /**
     * 词语类型
     */
    private String wordType;

    private static final long serialVersionUID = 1L;
}

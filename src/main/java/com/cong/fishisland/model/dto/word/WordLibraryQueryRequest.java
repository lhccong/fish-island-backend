package com.cong.fishisland.model.dto.word;

import com.cong.fishisland.common.PageRequest;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.io.Serializable;

/**
 * 词库查询请求
 * @author 许林涛
 * @date 2025年07月28日 10:45
 */
@EqualsAndHashCode(callSuper = true)
@Data
public class WordLibraryQueryRequest extends PageRequest implements Serializable {

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

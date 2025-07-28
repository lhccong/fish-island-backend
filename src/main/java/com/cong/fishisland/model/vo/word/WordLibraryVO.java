package com.cong.fishisland.model.vo.word;

import lombok.Data;

import java.io.Serializable;
import java.util.Date;

/**
 * 词库VO
 * @author 许林涛
 * @date 2025年07月28日 10:46
 */
@Data
public class WordLibraryVO implements Serializable {

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

    /**
     * 创建时间
     */
    private Date createTime;

    /**
     * 更新时间
     */
    private Date updateTime;

    private static final long serialVersionUID = 1L;
}

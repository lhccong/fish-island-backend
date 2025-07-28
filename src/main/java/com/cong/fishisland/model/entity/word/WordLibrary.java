package com.cong.fishisland.model.entity.word;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import java.io.Serializable;
import java.util.Date;
import lombok.Data;

/**
 * 词库表
 * @TableName word_library
 */
@TableName(value ="word_library")
@Data
public class WordLibrary implements Serializable {
    /**
     * 词库ID
     */
    @TableId(type = IdType.ASSIGN_ID)
    private Long id;

    /**
     * 词语名称
     */
    private String word;

    /**
     * 词库分类: undercover-谁是卧底, draw-default-你画我猜默认, draw-hero-你画我猜王者荣耀, draw-idiom-你画我猜成语
     */
    private String category;

    /**
     * 词语类型（如：水果、动物、王者英雄、成语等）
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

    @TableField(exist = false)
    private static final long serialVersionUID = 1L;
}
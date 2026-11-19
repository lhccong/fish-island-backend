package com.cong.fishisland.model.entity.emoticon;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.fasterxml.jackson.annotation.JsonIgnore;
import java.io.Serializable;
import java.util.Date;
import lombok.Data;

/**
 * 收藏表情包表
 * @TableName emoticon_favour
 */
@TableName(value ="emoticon_favour")
@Data
public class EmoticonFavour implements Serializable {
    /**
     * id
     */
    @TableId(type = IdType.ASSIGN_ID)
    private Long id;

    /**
     * 用户 id
     */
    private Long userId;

    /**
     * 表情包地址
     */
    private String emoticonSrc;

    private String ocrText;

    @JsonIgnore
    private Integer ocrAttempts;

    @JsonIgnore
    private Date ocrNextAttemptAt;

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

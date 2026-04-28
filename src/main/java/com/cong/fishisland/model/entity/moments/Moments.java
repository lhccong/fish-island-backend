package com.cong.fishisland.model.entity.moments;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.TableName;
import com.baomidou.mybatisplus.extension.handlers.JacksonTypeHandler;
import lombok.Data;

import java.io.Serializable;
import java.util.Date;
import java.util.List;

/**
 * 朋友圈动态实体
 *
 * @author cong
 */
@Data
@TableName(value = "moments", autoResultMap = true)
public class Moments implements Serializable {

    @TableId(value = "id", type = IdType.AUTO)
    private Long id;

    private Long userId;

    private String content;

    /**
     * 媒体资源列表，格式：[{type:"image",url:"..."},{type:"video",url:"...",cover:"..."}]
     */
    @TableField(typeHandler = JacksonTypeHandler.class)
    private List<MediaItem> mediaJson;

    private String location;

    /**
     * 可见范围：0-所有朋友，1-仅自己，2-部分可见，3-不给谁看
     */
    private Integer visibility;

    @TableField(typeHandler = JacksonTypeHandler.class)
    private List<Long> allowList;

    @TableField(typeHandler = JacksonTypeHandler.class)
    private List<Long> blockList;

    private Integer likeNum;

    private Integer commentNum;

    private Date createTime;

    private Date updateTime;

    @TableLogic
    private Integer isDelete;

    private static final long serialVersionUID = 1L;
}

package com.cong.fishisland.model.entity.moments;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.io.Serializable;
import java.util.Date;

/**
 * 朋友圈点赞实体
 *
 * @author cong
 */
@Data
@TableName("moments_like")
public class MomentsLike implements Serializable {

    @TableId(value = "id", type = IdType.AUTO)
    private Long id;

    private Long momentId;

    private Long userId;

    private Date createTime;

    private static final long serialVersionUID = 1L;
}

package com.cong.fishisland.model.entity.turntable;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.TableName;
import java.io.Serializable;
import java.util.Date;
import lombok.Data;

/**
 * 用户转盘进度表
 * @author cong
 */
@TableName(value = "turntable_user_progress")
@Data
public class TurntableUserProgress implements Serializable {
    /**
     * 用户转盘进度ID
     */
    @TableId(type = IdType.AUTO)
    private Long id;

    /**
     * 用户ID
     */
    private Long userId;

    /**
     * 转盘ID
     */
    private Long turntableId;

    /**
     * 小保底失败次数 10次小保底命中清零
     */
    private Integer smallFailCount;

    /**
     * 累计抽奖次数 大保底命中重置或减300
     */
    private Integer totalDrawCount;

    /**
     * 保底阈值（冗余快照）
     */
    private Integer guaranteeCount;

    /**
     * 上次抽奖时间
     */
    private Date lastDrawTime;

    /**
     * 创建时间
     */
    private Date createTime;

    /**
     * 更新时间
     */
    private Date updateTime;

    /**
     * 是否删除
     */
    @TableLogic
    private Integer isDelete;

    @TableField(exist = false)
    private static final long serialVersionUID = 1L;
}

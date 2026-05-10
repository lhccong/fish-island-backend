package com.cong.fishisland.model.entity.user;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.io.Serializable;
import java.util.Date;

/**
 * 用户关注
 *
 * @TableName user_follow
 * @author <a href="https://github.com/lhccong">程序员聪</a>
 */
@TableName(value = "user_follow")
@Data
public class UserFollow implements Serializable {

    /**
     * id
     */
    @TableId(value = "id", type = IdType.AUTO)
    private Long id;

    /**
     * 关注者用户ID（我）
     */
    @TableField(value = "userId")
    private Long userId;

    /**
     * 被关注者用户ID（TA）
     */
    @TableField(value = "followUserId")
    private Long followUserId;

    /**
     * 创建时间
     */
    @TableField(value = "createTime")
    private Date createTime;

    /**
     * 更新时间
     */
    @TableField(value = "updateTime")
    private Date updateTime;


    @TableField(exist = false)
    private static final long serialVersionUID = 1L;
}

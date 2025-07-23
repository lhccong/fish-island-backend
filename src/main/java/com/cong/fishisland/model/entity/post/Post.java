package com.cong.fishisland.model.entity.post;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.TableName;
import java.io.Serializable;
import java.util.Date;
import lombok.Data;

/**
 * 帖子
 * # @author <a href="https://github.com/lhccong">程序员聪</a>
 */
@TableName(value = "post")
@Data
public class Post implements Serializable {

    /**
     * id
     */
    @TableId(type = IdType.ASSIGN_ID)
    private Long id;

    /**
     * 标题
     */
    private String title;

    /**
     * 内容
     */
    private String content;

    /**
     * 标签列表 json
     */
    private String tags;

    /**
     * 封面图片
     */
    private String coverImage;

    /**
     * 点赞数
     */
    private Integer thumbNum;

    /**
     * 收藏数
     */
    private Integer favourNum;

    /**
     * 浏览量
     */
    private Integer viewNum;

    /**
     * 创建用户 id
     */
    private Long userId;

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

    /**
     * 是否加精（0-普通，1-加精）
     */
    private Integer isFeatured;

    /**
     * 总结
     */
    private String summary;

    @TableField(exist = false)
    private static final long serialVersionUID = 1L;
}
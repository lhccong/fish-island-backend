package com.cong.fishisland.model.entity.datasource;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.io.Serializable;
import java.util.Date;

/**
 * 数据源 Cookie 配置
 *
 * @TableName datasource_cookie
 */
@TableName(value = "datasource_cookie")
@Data
public class DataSourceCookie implements Serializable {

    @TableId(type = IdType.AUTO)
    private Long id;

    /**
     * 数据源标识，对应 HotDataKeyEnum.value
     */
    private String dataSourceKey;

    /**
     * Cookie 字符串
     */
    private String cookieValue;

    /**
     * 备注
     */
    private String remark;

    /**
     * 状态：0-禁用 1-启用
     */
    private Integer status;

    private Date createTime;

    private Date updateTime;

    @TableLogic
    private Integer isDelete;

    @TableField(exist = false)
    private static final long serialVersionUID = 1L;
}

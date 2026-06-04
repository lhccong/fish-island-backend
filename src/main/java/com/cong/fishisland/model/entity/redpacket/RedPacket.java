package com.cong.fishisland.model.entity.redpacket;

import lombok.Data;

import java.io.Serializable;
import java.util.Date;

/**
 * 红包实体类
 * 注意：这个类只用于在内存中表示红包，不存储到数据库
 * @author cong
 */
@Data
public class RedPacket implements Serializable {
    
    /**
     * 红包ID
     */
    private String id;
    
    /**
     * 红包创建者ID
     */
    private Long creatorId;

    /**
     * 红包名称
     */
    private String name;
    
    /**
     * 红包总金额（积分）
     */
    private Integer totalAmount;
    
    /**
     * 红包个数
     */
    private Integer count;
    
    /**
     * 红包类型：1-随机红包，2-平均红包，3-答题红包
     */
    private Integer type;

    /**
     * 正确答案（答题红包，仅存 Redis；详情接口返回前会清空）
     */
    private String answer;
    
    /**
     * 每个红包的金额（平均红包时使用）
     */
    private Integer amountPerPacket;
    
    /**
     * 剩余金额
     */
    private Integer remainingAmount;
    
    /**
     * 剩余个数
     */
    private Integer remainingCount;
    
    /**
     * 创建时间
     */
    private Date createTime;
    
    /**
     * 过期时间（24小时后）
     */
    private Date expireTime;
    
    /**
     * 红包状态：0-进行中，1-已抢完，2-已过期
     */
    private Integer status;
    
    /**
     * 已抢红包数量
     */
    private Integer grabCount;
    
    /**
     * 创建者用户名
     */
    private String creatorName;
    
    /**
     * 创建者头像
     */
    private String creatorAvatar;
    
    private static final long serialVersionUID = 1L;
} 
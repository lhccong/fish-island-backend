package com.cong.fishisland.model.dto.admin;

import lombok.Data;
import lombok.Builder;

import java.util.Date;

/**
 * 管理员撤回消息记录DTO
 * 
 * @author cong
 */
@Data
@Builder
public class AdminRevokeRecordDTO {
    
    /**
     * 管理员ID
     */
    private Long adminId;
    
    /**
     * 管理员名称
     */
    private String adminName;
    
    /**
     * 被撤回消息的用户ID
     */
    private Long revokedUserId;
    
    /**
     * 被撤回消息的用户名称
     */
    private String revokedUserName;
    
    /**
     * 消息ID
     */
    private String messageId;
    
    /**
     * 撤回时间
     */
    private Date revokeTime;
    
    /**
     * 消息内容（可选，用于记录被撤回的内容）
     */
    private String messageContent;
}


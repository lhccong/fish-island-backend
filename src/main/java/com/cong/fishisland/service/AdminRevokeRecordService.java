package com.cong.fishisland.service;

import com.cong.fishisland.model.dto.admin.AdminRevokeRecordDTO;

import java.util.List;

/**
 * 管理员撤回记录服务接口
 * 
 * @author cong
 */
public interface AdminRevokeRecordService {
    
    /**
     * 保存管理员撤回记录到Redis
     * 
     * @param record 撤回记录
     */
    void saveRevokeRecord(AdminRevokeRecordDTO record);
    
    /**
     * 获取管理员撤回记录列表
     * 
     * @param limit 限制数量，默认获取最近100条
     * @return 撤回记录列表
     */
    List<AdminRevokeRecordDTO> getRevokeRecords(int limit);
    
    /**
     * 获取指定管理员的撤回记录
     * 
     * @param adminId 管理员ID
     * @param limit 限制数量
     * @return 撤回记录列表
     */
    List<AdminRevokeRecordDTO> getRevokeRecordsByAdmin(Long adminId, int limit);
}


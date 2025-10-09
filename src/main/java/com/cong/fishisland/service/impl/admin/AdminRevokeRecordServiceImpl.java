package com.cong.fishisland.service.impl.admin;

import cn.hutool.json.JSONUtil;
import com.cong.fishisland.constant.RedisKey;
import com.cong.fishisland.model.dto.admin.AdminRevokeRecordDTO;
import com.cong.fishisland.service.AdminRevokeRecordService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

/**
 * 管理员撤回记录服务实现类
 * 
 * @author cong
 */
@Service
@Slf4j
@RequiredArgsConstructor
public class AdminRevokeRecordServiceImpl implements AdminRevokeRecordService {
    
    private final RedisTemplate<String, Object> redisTemplate;
    
    @Override
    public void saveRevokeRecord(AdminRevokeRecordDTO record) {
        try {
            String key = RedisKey.getKey(RedisKey.ADMIN_REVOKE_RECORDS);
            String recordJson = JSONUtil.toJsonStr(record);
            
            // 使用当前时间戳作为score，保证按时间排序
            double score = System.currentTimeMillis();
            redisTemplate.opsForZSet().add(key, recordJson, score);
            
            // 保留最近1000条记录，删除更早的记录
            Long count = redisTemplate.opsForZSet().zCard(key);
            if (count != null && count > 1000) {
                redisTemplate.opsForZSet().removeRange(key, 0, count - 1000 - 1);
            }
            
            log.info("保存管理员撤回记录成功: adminId={}, revokedUserId={}, messageId={}", 
                    record.getAdminId(), record.getRevokedUserId(), record.getMessageId());
        } catch (Exception e) {
            log.error("保存管理员撤回记录失败", e);
        }
    }
    
    @Override
    public List<AdminRevokeRecordDTO> getRevokeRecords(int limit) {
        try {
            String key = RedisKey.getKey(RedisKey.ADMIN_REVOKE_RECORDS);
            
            // 获取最新的记录，按score降序排列
            List<Object> records = Collections.singletonList(redisTemplate.opsForZSet().reverseRange(key, 0, limit - 1));

            return records.stream()
                    .map(record -> JSONUtil.toBean(record.toString(), AdminRevokeRecordDTO.class))
                    .collect(Collectors.toList());
        } catch (Exception e) {
            log.error("获取管理员撤回记录失败", e);
            return new ArrayList<>();
        }
    }
    
    @Override
    public List<AdminRevokeRecordDTO> getRevokeRecordsByAdmin(Long adminId, int limit) {
        try {
            // 先获取所有记录，然后过滤指定管理员的记录
            List<AdminRevokeRecordDTO> allRecords = getRevokeRecords(1000);
            
            return allRecords.stream()
                    .filter(record -> record.getAdminId().equals(adminId))
                    .limit(limit)
                    .collect(Collectors.toList());
        } catch (Exception e) {
            log.error("获取指定管理员撤回记录失败: adminId={}", adminId, e);
            return new ArrayList<>();
        }
    }
}


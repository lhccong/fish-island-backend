package com.cong.fishisland.job.cycle;

import com.cong.fishisland.model.ws.response.UserChatResponse;
import com.cong.fishisland.service.FishPetService;
import com.cong.fishisland.service.UserPointsService;
import com.cong.fishisland.websocket.service.WebSocketService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.stream.Collectors;

/**
 * 宠物状态定时更新任务
 *
 * @author cong
 */
@Component
@Slf4j
@RequiredArgsConstructor
public class PetStatusUpdateJob {

    private final FishPetService fishPetService;
    private final WebSocketService webSocketService;
    private final UserPointsService userPointsService;

    // 每小时饥饿度减少值
    private static final int HUNGER_DECREMENT = 5;
    // 每小时心情值减少值
    private static final int MOOD_DECREMENT = 3;
    // 宠物产出积分的最大值
    private static final int MAX_PET_POINTS = 10;


    /**
     * 每小时更新宠物经验
     * 注意：只有当饥饿度和心情值都大于0时，宠物才会获得经验
     */
    @Scheduled(fixedRate = 3600000) // 3600000毫秒 = 1小时
    public void updatePetLevel() {
        List<UserChatResponse> onlineUserList = webSocketService.getOnlineUserList();
        if (onlineUserList.isEmpty()) {
            log.info("当前没有在线用户，不执行宠物等级更新任务");
        } else {
            log.info("开始执行宠物等级更新任务，只有饥饿度和心情值都大于0的宠物才会获得经验");
            List<String> userIds = onlineUserList.stream().map(UserChatResponse::getId).collect(Collectors.toList());
            
            try {
                int updatedCount = fishPetService.batchUpdateOnlineUserPetExp(userIds);
                
                if (updatedCount > 0) {
                    log.info("在线用户宠物经验更新成功，共更新{}个宠物（饥饿度和心情值都大于0）", updatedCount);
                } else {
                    log.info("没有符合条件的宠物需要更新经验（可能是饥饿度或心情值为0）");
                }
                
                log.info("宠物等级更新任务执行完成");
            } catch (Exception e) {
                log.error("宠物等级更新任务执行异常", e);
            }
        }
    }

    /**
     * 每小时更新宠物状态
     * 每小时扣除5点饥饿度、3点心情值
     * 注意：饥饿度和心情值为0的宠物无法获得经验
     */
    @Scheduled(fixedRate = 3600000) // 3600000毫秒 = 1小时
    public void updatePetStatus() {
        log.info("开始执行宠物状态更新任务");

        try {
            // 使用批量更新方法一次性更新所有宠物状态
            int updatedCount = fishPetService.batchUpdatePetStatus(HUNGER_DECREMENT, MOOD_DECREMENT);

            if (updatedCount > 0) {
                log.info("宠物状态批量更新成功，共更新{}个宠物", updatedCount);
                log.info("提醒：饥饿度和心情值为0的宠物将无法获得经验和升级");
            } else {
                log.info("没有宠物需要更新状态");
            }

            log.info("宠物状态更新任务执行完成");
        } catch (Exception e) {
            log.error("宠物状态更新任务执行异常", e);
        }
    }
    
    /**
     * 每天凌晨0点执行宠物积分产出
     * 产出积分 = 宠物等级（最高10积分）
     * 注意：只有当饥饿度和心情值都大于0时，宠物才会产出积分
     */
    @Scheduled(cron = "0 0 0 * * ?") // 每天0点执行
    public void dailyPetPointsGeneration() {
        log.info("开始执行宠物每日积分产出任务");
        
        try {
            int updatedCount = fishPetService.generateDailyPetPoints(MAX_PET_POINTS);
            
            if (updatedCount > 0) {
                log.info("宠物每日积分产出成功，共有{}个宠物产出积分（饥饿度和心情值都大于0）", updatedCount);
            } else {
                log.info("没有符合条件的宠物产出积分（可能是饥饿度或心情值为0）");
            }
            
            log.info("宠物每日积分产出任务执行完成");
        } catch (Exception e) {
            log.error("宠物每日积分产出任务执行异常", e);
        }
    }
} 
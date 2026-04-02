package com.cong.fishisland.service.turntable;

import com.cong.fishisland.common.TestBaseByLogin;
import com.cong.fishisland.model.dto.turntable.DrawRequest;
import com.cong.fishisland.model.dto.turntable.TurntableDrawRecordQueryRequest;
import com.cong.fishisland.model.dto.turntable.TurntableQueryRequest;
import com.cong.fishisland.model.vo.turntable.DrawRecordVO;
import com.cong.fishisland.model.vo.turntable.DrawResultVO;
import com.cong.fishisland.model.vo.turntable.TurntableVO;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

import javax.annotation.Resource;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * 转盘服务测试
 * @author cong
 */
@Slf4j
@SpringBootTest
public class TurntableServiceTest extends TestBaseByLogin {

    @Resource
    private TurntableService turntableService;

    /**
     * 测试获取激活的转盘列表
     */
    @Test
    public void testListActiveTurntables() {
        // 创建查询请求
        TurntableQueryRequest request = new TurntableQueryRequest();
        request.setType(1); // 宠物装备转盘
        
        // 调用服务
        List<TurntableVO> result = turntableService.listActiveTurntables(request);
        
        // 验证结果
        assertNotNull(result, "转盘列表不应为null");
        log.info("获取到的转盘数量: {}", result.size());
        
        // 打印转盘信息
        result.forEach(turntable -> {
            log.info("转盘ID: {}, 名称: {}, 类型: {}, 消耗积分: {}", 
                    turntable.getId(), 
                    turntable.getName(), 
                    turntable.getType(),
                    turntable.getCostPoints());
            if (turntable.getPrizeList() != null) {
                log.info("奖品数量: {}", turntable.getPrizeList().size());
            }
        });
    }

    /**
     * 测试获取转盘详情
     */
    @Test
    public void testGetTurntableDetail() {
        Long turntableId = 1L; // 假设转盘ID为1
        
        // 调用服务
        TurntableVO result = turntableService.getTurntableDetail(turntableId);
        
        // 验证结果
        if (result != null) {
            assertNotNull(result.getId(), "转盘ID不应为null");
            assertEquals(turntableId, result.getId(), "转盘ID应匹配");
            
            log.info("转盘详情 - ID: {}, 名称: {}, 类型: {}", 
                    result.getId(), result.getName(), result.getType());
            log.info("消耗积分: {}, 保底次数: {}", 
                    result.getCostPoints(), result.getGuaranteeCount());
            
            if (result.getPrizeList() != null) {
                log.info("奖品列表数量: {}", result.getPrizeList().size());
                result.getPrizeList().forEach(prize -> {
                    log.info("奖品: {}, 品质: {}, 概率: {}", 
                            prize.getName(), prize.getQuality(), prize.getProbability());
                });
            }
            
            if (result.getUserProgress() != null) {
                log.info("用户进度 - 累计抽奖次数: {}, 小保底失败次数: {}, 保底阈值: {}", 
                        result.getUserProgress().getTotalDrawCount(),
                        result.getUserProgress().getSmallFailCount(),
                        result.getUserProgress().getGuaranteeCount());
            }
        } else {
            log.warn("转盘不存在, ID: {}", turntableId);
        }
    }

    /**
     * 测试执行抽奖 - 单抽
     */
    @Test
    public void testDraw_Single() {
        DrawRequest request = new DrawRequest();
        request.setTurntableId(1L); // 转盘ID
        request.setDrawCount(1); // 单抽
        
        // 调用服务
        DrawResultVO result = turntableService.draw(request);
        
        // 验证结果
        assertNotNull(result, "抽奖结果不应为null");
        assertNotNull(result.getPrizeList(), "奖品列表不应为null");
        assertEquals(1, result.getPrizeList().size(), "单抽应获得1个奖品");
        
        log.info("单抽结果 - 奖品数量: {}, 消耗积分: {}", 
                result.getPrizeList().size(), result.getCostPoints());
        log.info("是否触发保底: {}, 保底类型: {}", 
                result.getIsGuarantee(), result.getGuaranteeType());
        
        result.getPrizeList().forEach(prize -> {
            log.info("获得奖品: {}, 品质: {}", prize.getName(), prize.getQuality());
        });
    }

    /**
     * 测试执行抽奖 - 十连抽
     */
    @Test
    public void testDraw_TenTimes() {
        DrawRequest request = new DrawRequest();
        request.setTurntableId(1L); // 转盘ID
        request.setDrawCount(10); // 十连抽
        
        // 调用服务
        DrawResultVO result = turntableService.draw(request);
        
        // 验证结果
        assertNotNull(result, "抽奖结果不应为null");
        assertNotNull(result.getPrizeList(), "奖品列表不应为null");
        assertEquals(10, result.getPrizeList().size(), "十连抽应获得10个奖品");
        
        log.info("十连抽结果 - 奖品数量: {}, 消耗积分: {}", 
                result.getPrizeList().size(), result.getCostPoints());
        log.info("是否触发保底: {}, 保底类型: {}", 
                result.getIsGuarantee(), result.getGuaranteeType());
        
        // 统计各品质奖品数量
        long epicCount = result.getPrizeList().stream()
                .filter(p -> p.getQuality() >= 3).count();
        log.info("史诗及以上品质数量: {}", epicCount);
        
        result.getPrizeList().forEach(prize -> {
            log.info("获得奖品: {}, 品质: {}", prize.getName(), prize.getQuality());
        });
    }


    /**
     * 测试完整的抽奖流程
     */
    @Test
    public void testCompleteDrawFlow() {
        log.info("========== 开始完整抽奖流程测试 ==========");
        
        // 1. 获取转盘列表
        TurntableQueryRequest queryRequest = new TurntableQueryRequest();
        queryRequest.setType(1);
        List<TurntableVO> turntables = turntableService.listActiveTurntables(queryRequest);
        log.info("步骤1: 获取转盘列表, 数量: {}", turntables.size());
        
        if (turntables.isEmpty()) {
            log.warn("没有可用的转盘, 测试终止");
            return;
        }
        
        // 2. 获取第一个转盘详情
        Long turntableId = turntables.get(0).getId();
        TurntableVO detail = turntableService.getTurntableDetail(turntableId);
        log.info("步骤2: 获取转盘详情, ID: {}, 名称: {}", turntableId, detail.getName());
        
        // 3. 执行单抽
        DrawRequest drawRequest = new DrawRequest();
        drawRequest.setTurntableId(turntableId);
        drawRequest.setDrawCount(1);
        DrawResultVO singleResult = turntableService.draw(drawRequest);
        log.info("步骤3: 执行单抽, 获得奖品: {}", 
                singleResult.getPrizeList().get(0).getName());
        
        // 4. 执行十连抽
        drawRequest.setDrawCount(10);
        DrawResultVO tenResult = turntableService.draw(drawRequest);
        log.info("步骤4: 执行十连抽, 获得{}个奖品", tenResult.getPrizeList().size());

        
        log.info("========== 完整抽奖流程测试结束 ==========");
    }
}

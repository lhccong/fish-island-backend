package com.cong.fishisland.service.turntable;

import com.baomidou.mybatisplus.extension.service.IService;
import com.cong.fishisland.model.dto.turntable.DrawRequest;
import com.cong.fishisland.model.dto.turntable.TurntableDrawRecordQueryRequest;
import com.cong.fishisland.model.dto.turntable.TurntableQueryRequest;
import com.cong.fishisland.model.entity.turntable.Turntable;
import com.cong.fishisland.model.vo.turntable.DrawRecordVO;
import com.cong.fishisland.model.vo.turntable.DrawResultVO;
import com.cong.fishisland.model.vo.turntable.TurntableVO;

import java.util.List;

/**
 * 转盘服务接口
 * @author cong
 */
public interface TurntableService extends IService<Turntable> {

    /**
     * 获取当前激活的转盘列表
     * @param turntableQueryRequest 查询请求
     * @return 转盘列表
     */
    List<TurntableVO> listActiveTurntables(TurntableQueryRequest turntableQueryRequest);

    /**
     * 获取转盘详情（包含用户进度和奖品列表）
     * @param turntableId 转盘ID
     * @return 转盘详情
     */
    TurntableVO getTurntableDetail(Long turntableId);

    /**
     * 执行抽奖
     * @param drawRequest 抽奖请求
     * @return 抽奖结果
     */
    DrawResultVO draw(DrawRequest drawRequest);

    /**
     * 查询抽奖记录
     * @param queryRequest 查询请求
     * @return 抽奖记录列表
     */
    List<DrawRecordVO> listDrawRecords(TurntableDrawRecordQueryRequest queryRequest);

    /**
     * 获取查询条件
     * @param turntableQueryRequest 查询请求
     * @return 查询条件
     */
    com.baomidou.mybatisplus.core.conditions.query.QueryWrapper<Turntable> getQueryWrapper(TurntableQueryRequest turntableQueryRequest);
}

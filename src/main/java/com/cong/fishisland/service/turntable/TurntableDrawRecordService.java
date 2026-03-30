package com.cong.fishisland.service.turntable;

import com.baomidou.mybatisplus.extension.service.IService;
import com.cong.fishisland.model.entity.turntable.TurntableDrawRecord;

import java.util.List;

/**
 * 转盘抽奖记录服务接口
 * @author cong
 */
public interface TurntableDrawRecordService extends IService<TurntableDrawRecord> {

    /**
     * 批量保存抽奖记录
     * @param records 记录列表
     */
    void saveBatchRecords(List<TurntableDrawRecord> records);

    /**
     * 根据用户ID和转盘ID查询记录
     * @param userId 用户ID
     * @param turntableId 转盘ID
     * @return 记录列表
     */
    List<TurntableDrawRecord> listByUserIdAndTurntableId(Long userId, Long turntableId);
}

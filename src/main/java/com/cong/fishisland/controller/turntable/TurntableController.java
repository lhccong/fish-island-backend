package com.cong.fishisland.controller.turntable;

import com.cong.fishisland.common.BaseResponse;
import com.cong.fishisland.common.ResultUtils;
import com.cong.fishisland.model.dto.turntable.DrawRequest;
import com.cong.fishisland.model.dto.turntable.TurntableDrawRecordQueryRequest;
import com.cong.fishisland.model.dto.turntable.TurntableQueryRequest;
import com.cong.fishisland.model.vo.turntable.DrawRecordVO;
import com.cong.fishisland.model.vo.turntable.DrawResultVO;
import com.cong.fishisland.model.vo.turntable.TurntableVO;
import com.cong.fishisland.service.turntable.TurntableService;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

import javax.annotation.Resource;
import java.util.List;

/**
 * 转盘控制器
 * @author cong
 */
@Slf4j
@RestController
@RequestMapping("/turntable")
@Api(tags = "转盘抽奖接口")
public class TurntableController {

    @Resource
    private TurntableService turntableService;

    /**
     * 获取当前激活的转盘列表
     */
    @GetMapping("/list")
    @ApiOperation("获取激活的转盘列表")
    public BaseResponse<List<TurntableVO>> listTurntables(TurntableQueryRequest request) {
        List<TurntableVO> result = turntableService.listActiveTurntables(request);
        return ResultUtils.success(result);
    }

    /**
     * 获取转盘详情
     */
    @GetMapping("/detail/{id}")
    @ApiOperation("获取转盘详情")
    public BaseResponse<TurntableVO> getTurntableDetail(@PathVariable Long id) {
        TurntableVO result = turntableService.getTurntableDetail(id);
        return ResultUtils.success(result);
    }

    /**
     * 执行抽奖
     */
    @PostMapping("/draw")
    @ApiOperation("执行抽奖")
    public BaseResponse<DrawResultVO> draw(@RequestBody DrawRequest request) {
        DrawResultVO result = turntableService.draw(request);
        return ResultUtils.success(result);
    }

    /**
     * 查询抽奖记录
     */
    @GetMapping("/records")
    @ApiOperation("查询抽奖记录")
    public BaseResponse<List<DrawRecordVO>> listDrawRecords(TurntableDrawRecordQueryRequest request) {
        List<DrawRecordVO> result = turntableService.listDrawRecords(request);
        return ResultUtils.success(result);
    }
}

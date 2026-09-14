package com.cong.fishisland.controller.farm;

import cn.dev33.satoken.stp.StpUtil;
import com.cong.fishisland.common.BaseResponse;
import com.cong.fishisland.common.ResultUtils;
import com.cong.fishisland.model.dto.farm.CollectionGradeGroupVO;
import com.cong.fishisland.service.FarmCollectionGradeService;
import io.swagger.annotations.ApiOperation;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.annotation.Resource;
import java.util.List;

@RestController
@RequestMapping("/collection")
public class FarmCollectionGradeController {

    @Resource
    private FarmCollectionGradeService farmCollectionGradeService;

    @GetMapping("/my")
    @ApiOperation(value = "我的作物图鉴", notes = "按作物分组，每作物每品级仅保留最大重量记录；可与 GET /crop/all 合并渲染未解锁剪影")
    public BaseResponse<List<CollectionGradeGroupVO>> getMyCollection() {
        Long userId = StpUtil.getLoginIdAsLong();
        return ResultUtils.success(farmCollectionGradeService.getMyCollection(userId));
    }
}

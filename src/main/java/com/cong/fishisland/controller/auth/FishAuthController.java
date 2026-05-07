package com.cong.fishisland.controller.auth;

import com.cong.fishisland.common.BaseResponse;
import com.cong.fishisland.common.DeleteRequest;
import com.cong.fishisland.common.ResultUtils;
import com.cong.fishisland.model.dto.auth.FishAuthAddRequest;
import com.cong.fishisland.model.dto.auth.FishAuthUpdateRequest;
import com.cong.fishisland.model.vo.auth.FishAuthDetailVO;
import com.cong.fishisland.model.vo.auth.FishAuthVO;
import com.cong.fishisland.service.auth.FishAuthService;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 第三方应用管理接口
 *
 * @author cong
 */
@RestController
@RequestMapping("/auth/app")
@RequiredArgsConstructor
//@Api(tags = "第三方应用管理")
public class FishAuthController {

    private final FishAuthService fishAuthService;

    @PostMapping("/create")
    @ApiOperation("创建应用（自动生成 clientId / clientSecret）")
    public BaseResponse<FishAuthDetailVO> createApp(@RequestBody FishAuthAddRequest request) {
        return ResultUtils.success(fishAuthService.createApp(request));
    }

    @PostMapping("/update")
    @ApiOperation("更新应用信息")
    public BaseResponse<Boolean> updateApp(@RequestBody FishAuthUpdateRequest request) {
        fishAuthService.updateApp(request);
        return ResultUtils.success(true);
    }

    @PostMapping("/delete")
    @ApiOperation("删除应用")
    public BaseResponse<Boolean> deleteApp(@RequestBody DeleteRequest deleteRequest) {
        fishAuthService.deleteApp(Long.parseLong(deleteRequest.getId()));
        return ResultUtils.success(true);
    }

    @PostMapping("/reset-secret")
    @ApiOperation("重置 clientSecret")
    public BaseResponse<String> resetSecret(@RequestParam Long id) {
        return ResultUtils.success(fishAuthService.resetSecret(id));
    }

    @GetMapping("/list/my")
    @ApiOperation("查询我的应用列表")
    public BaseResponse<List<FishAuthVO>> listMyApps() {
        return ResultUtils.success(fishAuthService.listMyApps());
    }

    @GetMapping("/detail")
    @ApiOperation("获取应用详情（含 clientSecret）")
    public BaseResponse<FishAuthDetailVO> getAppDetail(@RequestParam Long id) {
        return ResultUtils.success(fishAuthService.getAppDetail(id));
    }

    @GetMapping("/info")
    @ApiOperation("根据 clientId 查询应用公开信息（任何人可访问）")
    public BaseResponse<FishAuthVO> getAppByClientId(@RequestParam String clientId) {
        return ResultUtils.success(fishAuthService.getAppByClientId(clientId));
    }
}

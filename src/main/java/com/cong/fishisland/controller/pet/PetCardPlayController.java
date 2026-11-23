package com.cong.fishisland.controller.pet;

import cn.dev33.satoken.stp.StpUtil;
import com.cong.fishisland.common.BaseResponse;
import com.cong.fishisland.common.ResultUtils;
import com.cong.fishisland.model.dto.pet.PetCardPlay.Command;
import com.cong.fishisland.model.dto.pet.PetCardPlay.History;
import com.cong.fishisland.model.dto.pet.PetCardPlay.State;
import com.cong.fishisland.service.pet.PetCardPlayService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/pet/cards")
@RequiredArgsConstructor
public class PetCardPlayController {
    private final PetCardPlayService service;

    @GetMapping("/state")
    public BaseResponse<State> state() {
        return ResultUtils.success(service.state(StpUtil.getLoginIdAsLong()));
    }

    @GetMapping("/history")
    public BaseResponse<History> history(@RequestParam(required = false) String before) {
        return ResultUtils.success(service.history(StpUtil.getLoginIdAsLong(), before));
    }

    @PostMapping("/actions")
    public BaseResponse<State> submit(@RequestBody Command command) {
        return ResultUtils.success(service.submit(StpUtil.getLoginIdAsLong(), command));
    }
}

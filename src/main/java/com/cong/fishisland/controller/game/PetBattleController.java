package com.cong.fishisland.controller.game;

import com.cong.fishisland.common.BaseResponse;
import com.cong.fishisland.common.ResultUtils;
import com.cong.fishisland.model.vo.game.PetBattleInfoVO;
import com.cong.fishisland.model.vo.game.PetBattleResultVO;
import com.cong.fishisland.service.PetBattleService;
import io.swagger.annotations.ApiOperation;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import javax.annotation.Resource;
import java.util.List;

/**
 * 宠物对战接口
 *
 * @author cong
 */
@RestController
@RequestMapping("/pet/battle")
@Slf4j
public class PetBattleController {

    @Resource
    private PetBattleService petBattleService;

    /**
     * 获取宠物对战信息（双方宠物详情及剩余挑战次数）
     *
     * @param opponentUserId 对手用户ID
     * @return 对战信息
     */
    @GetMapping("/info")
    @ApiOperation(value = "获取宠物对战信息（双方宠物详情及剩余挑战次数）")
    public BaseResponse<PetBattleInfoVO> getPetBattleInfo(@RequestParam Long opponentUserId) {
        return ResultUtils.success(petBattleService.getPetBattleInfo(opponentUserId));
    }

    /**
     * 宠物对战（不限次数）
     *
     * @param opponentUserId 对手用户ID
     * @return 对战回合结果列表
     */
    @GetMapping("/start")
    @ApiOperation(value = "宠物对战（不限次数）")
    public BaseResponse<List<PetBattleResultVO>> battle(@RequestParam Long opponentUserId) {
        return ResultUtils.success(petBattleService.battle(opponentUserId));
    }
}

package com.cong.fishisland.model.vo.moments;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

import java.io.Serializable;
import java.util.List;

/**
 * 朋友圈抽奖结果 VO
 *
 * @author cong
 */
@Data
@ApiModel(value = "MomentsLotteryVO", description = "朋友圈抽奖结果")
public class MomentsLotteryVO implements Serializable {

    @ApiModelProperty(value = "动态ID")
    private Long momentId;

    @ApiModelProperty(value = "中奖用户列表")
    private List<LotteryWinnerVO> winners;

    @ApiModelProperty(value = "评论ID（系统自动发布的抽奖结果评论）")
    private Long commentId;

    @Data
    public static class LotteryWinnerVO implements Serializable {

        @ApiModelProperty(value = "用户ID")
        private Long userId;

        @ApiModelProperty(value = "用户昵称")
        private String userName;

        @ApiModelProperty(value = "用户头像")
        private String userAvatar;

        private static final long serialVersionUID = 1L;
    }

    private static final long serialVersionUID = 1L;
}

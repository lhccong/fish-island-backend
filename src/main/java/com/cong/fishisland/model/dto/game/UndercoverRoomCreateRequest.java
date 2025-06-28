package com.cong.fishisland.model.dto.game;

import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

/**
 * 创建谁是卧底游戏房间请求
 *
 * @author cong
 */
@Data
public class UndercoverRoomCreateRequest {

    /**
     * 平民词语
     */
    @ApiModelProperty("平民词")
    private String civilianWord;

    /**
     * 卧底词语
     */
    @ApiModelProperty("卧底词")
    private String undercoverWord;

    /**
     * 游戏持续时间（秒）
     */
    @ApiModelProperty("持续时间秒")
    private Integer duration;
} 
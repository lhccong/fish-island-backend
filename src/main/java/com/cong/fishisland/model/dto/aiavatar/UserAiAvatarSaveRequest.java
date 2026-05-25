package com.cong.fishisland.model.dto.aiavatar;

import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

/**
 * 保存用户 AI 分身请求
 */
@Data
public class UserAiAvatarSaveRequest {

    @ApiModelProperty(value = "分身名称", required = true)
    private String avatarName;

    @ApiModelProperty(value = "分身系统提示词")
    private String systemPrompt;

    @ApiModelProperty(value = "是否启用分身：0-关闭，1-开启", required = true)
    private Integer enabled;
}

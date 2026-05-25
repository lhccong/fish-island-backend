package com.cong.fishisland.model.dto.aiavatar;

import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

/**
 * 生成 AI 分身系统提示词请求
 */
@Data
public class UserAiAvatarGenerateRequest {

    @ApiModelProperty(value = "分身名称", required = true)
    private String avatarName;

    @ApiModelProperty(value = "个人描述素材（性格、说话风格、兴趣等）", required = true)
    private String sourceContent;
}

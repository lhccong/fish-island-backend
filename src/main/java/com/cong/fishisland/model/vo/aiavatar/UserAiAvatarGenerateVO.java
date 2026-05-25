package com.cong.fishisland.model.vo.aiavatar;

import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

/**
 * AI 分身提示词生成结果
 */
@Data
public class UserAiAvatarGenerateVO {

    @ApiModelProperty(value = "生成的系统提示词")
    private String systemPrompt;
}

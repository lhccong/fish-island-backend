package com.cong.fishisland.model.vo.user;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class UserCardVO extends UserVO {

    private static final long serialVersionUID = 1L;

    /** 当前佩戴的称号 ID，0 表示等级称号。 */
    private Long titleId;

    /** 已获称号 ID 的 JSON 列表，与聊天快照使用同一格式。 */
    private String titleIdList;
}

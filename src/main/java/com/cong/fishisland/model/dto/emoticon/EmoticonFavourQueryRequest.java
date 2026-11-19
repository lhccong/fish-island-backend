package com.cong.fishisland.model.dto.emoticon;

import com.cong.fishisland.common.PageRequest;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
public class EmoticonFavourQueryRequest extends PageRequest {
    private String keyword;
}

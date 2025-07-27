package com.cong.fishisland.model.dto.pet;

import com.cong.fishisland.common.PageRequest;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.io.Serializable;

/**
 * 宠物皮肤查询请求
 *
 * @author cong
 */
@EqualsAndHashCode(callSuper = true)
@Data
public class PetSkinQueryRequest extends PageRequest implements Serializable {
    
    /**
     * 皮肤名称（可选，模糊搜索）
     */
    private String name;

    private static final long serialVersionUID = 1L;
} 
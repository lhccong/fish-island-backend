package com.cong.fishisland.model.dto.pet;

import lombok.Data;

import java.io.Serializable;

/**
 * 宠物皮肤设置请求
 *
 * @author cong
 */
@Data
public class PetSkinSetRequest implements Serializable {
    
    /**
     * 皮肤ID
     */
    private Long skinId;

    private static final long serialVersionUID = 1L;
} 
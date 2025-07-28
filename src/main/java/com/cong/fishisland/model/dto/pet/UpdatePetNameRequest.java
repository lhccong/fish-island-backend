package com.cong.fishisland.model.dto.pet;

import lombok.Data;
import java.io.Serializable;

/**
 * 修改宠物名称请求
 * 
 * @author cong
 */
@Data
public class UpdatePetNameRequest implements Serializable {

    /**
     * 宠物ID
     */
    private Long petId;
    
    /**
     * 新的宠物名称
     */
    private String name;

    private static final long serialVersionUID = 1L;
} 
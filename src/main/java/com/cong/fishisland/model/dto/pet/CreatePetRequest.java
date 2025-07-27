package com.cong.fishisland.model.dto.pet;

import lombok.Data;
import java.io.Serializable;

/**
 * 创建宠物请求
 * 
 * @author cong
 */
@Data
public class CreatePetRequest implements Serializable {

    /**
     * 宠物名称
     */
    private String name;
    

    private static final long serialVersionUID = 1L;
} 
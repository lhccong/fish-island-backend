package com.cong.fishisland.model.vo.game;

import com.cong.fishisland.model.vo.pet.PetEquipStatsVO;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

import java.io.Serializable;

/**
 * 武道大会排行榜条目
 *
 * @author cong
 */
@Data
public class TournamentRankVO implements Serializable {

    @ApiModelProperty("名次")
    private Integer rank;

    @ApiModelProperty("用户ID")
    private Long userId;

    @ApiModelProperty("用户昵称")
    private String userName;

    @ApiModelProperty("用户头像")
    private String userAvatar;

    @ApiModelProperty("宠物名称")
    private String petName;

    @ApiModelProperty("宠物等级")
    private Integer petLevel;

    @ApiModelProperty("宠物图片")
    private String petUrl;


    private static final long serialVersionUID = 1L;
}

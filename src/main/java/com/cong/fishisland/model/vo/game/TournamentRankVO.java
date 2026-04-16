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

    @ApiModelProperty("实际攻击力（含装备）")
    private Integer attack;

    @ApiModelProperty("实际生命值（含装备）")
    private Integer health;

    @ApiModelProperty("宠物装备属性统计")
    private PetEquipStatsVO equipStats;

    private static final long serialVersionUID = 1L;
}

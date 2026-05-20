package com.cong.fishisland.model.dto.farm;

import com.cong.fishisland.model.entity.farm.FarmUser;
import com.cong.fishisland.model.entity.user.User;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@ApiModel(description = "农场用户信息VO")
public class FarmUserVO {

    @ApiModelProperty(value = "农场用户ID")
    private Long id;

    @ApiModelProperty(value = "关联的系统用户ID")
    private Long userId;

    @ApiModelProperty(value = "用户昵称（来自用户表）")
    private String userName;

    @ApiModelProperty(value = "用户头像（来自用户表）")
    private String userAvatar;

    @ApiModelProperty(value = "农场等级")
    private Integer level;

    @ApiModelProperty(value = "经验值")
    private Integer experience;

    @ApiModelProperty(value = "总收获次数")
    private Integer totalHarvest;

    @ApiModelProperty(value = "总偷菜次数")
    private Integer totalSteal;

    @ApiModelProperty(value = "总防御次数")
    private Integer totalDefense;

    @ApiModelProperty(value = "好友数量")
    private Integer friendCount;

    @ApiModelProperty(value = "被访问次数")
    private Integer visitedCount;

    @ApiModelProperty(value = "状态（0-禁用，1-正常）")
    private Integer status;

    @ApiModelProperty(value = "创建时间")
    private LocalDateTime createTime;

    @ApiModelProperty(value = "更新时间")
    private LocalDateTime updateTime;

    public static FarmUserVO from(FarmUser farmUser, User user) {
        FarmUserVO vo = new FarmUserVO();
        vo.setId(farmUser.getId());
        vo.setUserId(farmUser.getUserId());
        vo.setLevel(farmUser.getLevel());
        vo.setExperience(farmUser.getExperience());
        vo.setTotalHarvest(farmUser.getTotalHarvest());
        vo.setTotalSteal(farmUser.getTotalSteal());
        vo.setTotalDefense(farmUser.getTotalDefense());
        vo.setFriendCount(farmUser.getFriendCount());
        vo.setVisitedCount(farmUser.getVisitedCount());
        vo.setStatus(farmUser.getStatus());
        vo.setCreateTime(farmUser.getCreateTime());
        vo.setUpdateTime(farmUser.getUpdateTime());
        if (user != null) {
            vo.setUserName(user.getUserName());
            vo.setUserAvatar(user.getUserAvatar());
        }
        return vo;
    }
}

package com.cong.fishisland.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.cong.fishisland.model.dto.farm.FarmUserVO;
import com.cong.fishisland.model.entity.farm.FarmUser;

import java.util.List;

/**
 * 农场用户服务
 */
public interface FarmUserService extends IService<FarmUser> {

    /**
     * 按系统用户 ID 查询农场用户。
     *
     * @param userId 系统用户 ID
     * @return 农场用户，不存在时返回 null
     */
    FarmUser getFarmUserByUserId(Long userId);

    /**
     * 为系统用户创建农场用户（已存在则直接返回）。
     *
     * @param userId 系统用户 ID
     * @return 农场用户
     */
    FarmUser createFarmUser(Long userId);

    /**
     * 获取或创建当前登录用户的农场用户。
     *
     * @return 农场用户
     */
    FarmUser getFarmUser();

    /**
     * 获取或创建指定系统用户的农场用户。
     *
     * @param userId 系统用户 ID
     * @return 农场用户
     */
    FarmUser getOrCreateFarmUser(Long userId);

    FarmUser getFarmUser(Long systemUserId);

    /**
     * 获取或创建农场用户，并转换为 VO（含系统用户昵称、头像）。
     *
     * @param systemUserId 系统用户 ID
     * @return 农场用户 VO
     */
    FarmUserVO getFarmUserVO(Long systemUserId);

    /**
     * 将农场用户实体转换为 VO。
     *
     * @param farmUser 农场用户
     * @return 农场用户 VO；入参为 null 时返回 null
     */
    FarmUserVO toVO(FarmUser farmUser);

    /**
     * 批量将农场用户实体转换为 VO。
     *
     * @param farmUsers 农场用户列表
     * @return 农场用户 VO 列表
     */
    List<FarmUserVO> toVOList(List<FarmUser> farmUsers);

    /**
     * 为农场用户增加经验，并按经验值自动升级。
     *
     * @param userId 系统用户 ID
     * @param exp    增加的经验值
     * @return 是否更新成功
     */
    boolean addExperience(Long userId, Integer exp);

    /**
     * 根据累计经验计算等级（递增门槛，见 {@link com.cong.fishisland.model.enums.farm.FarmConstants}）。
     *
     * @param experience 累计经验
     * @return 等级
     */
    int calculateLevel(Integer experience);

    /**
     * 累计收获次数 +1。
     *
     * @param userId 系统用户 ID
     */
    void incrementTotalHarvest(Long userId);

    /**
     * 累计偷菜次数 +1。
     *
     * @param userId 系统用户 ID
     */
    void incrementTotalSteal(Long userId);

    /**
     * 累计被防御（被偷）次数 +1。
     *
     * @param userId 系统用户 ID
     */
    void incrementTotalDefense(Long userId);

    /**
     * 被访问次数 +1。
     *
     * @param userId 系统用户 ID
     * @return 是否更新成功
     */
    boolean incrementVisitedCount(Long userId);

    /**
     * 按系统用户 ID 列表批量查询。
     *
     * @param userIds 系统用户 ID 列表
     * @return 农场用户列表
     */
    List<FarmUser> getFarmUsersByUserIds(List<Long> userIds);
}

package com.cong.fishisland.service;

import com.baomidou.mybatisplus.core.conditions.Wrapper;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.cong.fishisland.model.dto.user.UserTitleQueryRequest;
import com.cong.fishisland.model.entity.user.UserTitle;
import com.baomidou.mybatisplus.extension.service.IService;

import java.util.List;

/**
* @author cong
* @description 针对表【user_title(用户称号)】的数据库操作Service
* @createDate 2025-04-30 10:07:06
*/
public interface UserTitleService extends IService<UserTitle> {
    /**
     * 获取用户可用的称号列表
     * @return 称号列表
     */
    List<UserTitle> listAvailableTitles();

    /**
     * 设置当前使用的称号
     * @param titleId 称号ID
     * @return 是否设置成功
     */
    Boolean setCurrentTitle(Long titleId);

    /**
     * 根据用户ID获取用户拥有的称号列表
     *
     * @param userId 用户ID
     * @return 称号列表
     */
    List<UserTitle> listUserTitlesByUserId(Long userId);

    /**
     * 给用户添加称号
     *
     * @param userId 用户ID
     * @param titleId 称号ID
     * @return 是否添加成功
     */
    boolean addTitleToUser(Long userId, Long titleId);

    /**
     * 删除用户称号
     *
     * @param userId 用户ID
     * @param titleId 称号ID
     * @return 是否删除成功
     */
    boolean removeTitleFromUser(Long userId, Long titleId);

    /**
     * 获取查询包装类
     *
     * @param userTitleQueryRequest 称号查询请求
     * @return 查询包装类
     */
    QueryWrapper<UserTitle> getQueryWrapper(UserTitleQueryRequest userTitleQueryRequest);

    /**
     * 判断是否存在称号
     * @param name 称号名称
     * @param titleId 称号ID
     * @return 是否存在
     */
    Boolean existTitle(String name, Long titleId);
}

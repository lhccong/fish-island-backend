package com.cong.fishisland.service;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.service.IService;
import com.cong.fishisland.model.dto.user.NewUserDataWebRequest;
import com.cong.fishisland.model.dto.user.UserQueryRequest;
import com.cong.fishisland.model.entity.user.User;
import com.cong.fishisland.model.vo.user.*;

import java.util.List;

import me.zhyd.oauth.model.AuthCallback;

import javax.servlet.http.HttpServletRequest;

/**
 * 用户服务
 * # @author <a href="https://github.com/lhccong">程序员聪</a>
 */
public interface UserService extends IService<User> {

    /**
     * 用户注册
     *
     * @param userAccount   用户账户
     * @param userPassword  用户密码
     * @param checkPassword 校验密码
     * @return 新用户 id
     */
    long userRegister(String userAccount, String userPassword, String checkPassword);

    /**
     * 用户邮箱注册
     *
     * @param email         邮箱
     * @param userPassword  用户密码
     * @param checkPassword 校验密码
     * @return 新用户 id
     */
    long userEmilRegister(String userAccount,String email, String userPassword, String checkPassword, String code);

    /**
     * 用户邮箱发验证码
     *
     * @param email 邮箱
     * @return 验证码
     */
    boolean userEmailSend(String email, HttpServletRequest request);

    /**
     * 用户登录
     *
     * @param userAccount  用户账户
     * @param userPassword 用户密码
     * @return 脱敏后的用户信息
     */
    LoginUserVO userLogin(String userAccount, String userPassword);

    /**
     * 用户邮箱登录
     *
     * @param email        邮箱
     * @param userPassword 用户密码
     * @return 脱敏后的用户信息
     */
    LoginUserVO userEmailLogin(String email, String userPassword);

    /**
     * 用户邮箱找回密码
     *
     * @param email 邮箱
     * @return 脱敏后的用户信息
     */
    boolean userEmailResetPassword(String email, String userPassword, String checkPassword, String code);

    /**
     * 用户邮箱绑定账号
     *
     * @param email 邮箱
     * @return 脱敏后的用户信息
     */
    boolean userEmailBindToAccount(String email, String code);

    /**
     * 获取当前登录用户根据token
     *
     * @param token 令 牌
     * @return {@link User}
     */
    User getLoginUser(String token);

    /**
     * 获取当前登录用户
     *
     * @return {@link User}
     */
    User getLoginUser();

    /**
     * 获取当前登录用户（允许未登录）
     *
     * @return {@link User}
     */
    User getLoginUserPermitNull();

    /**
     * 是否为管理员
     *
     * @return boolean
     */
    boolean isAdmin();

    /**
     * 是否为管理员
     *
     * @param user 用户
     * @return boolean
     */
    boolean isAdmin(User user);

    /**
     * 用户注销
     *
     * @return boolean
     */
    boolean userLogout();

    /**
     * 获取脱敏的已登录用户信息
     *
     * @param user 用户
     * @return {@link LoginUserVO}
     */
    LoginUserVO getLoginUserVO(User user);

    /**
     * 获取脱敏的用户信息
     *
     * @param user 用户
     * @return {@link UserVO}
     */
    UserVO getUserVO(User user);

    /**
     * 获取脱敏的用户信息
     *
     * @param userList 用户列表
     * @return {@link List}<{@link UserVO}>
     */
    List<UserVO> getUserVO(List<User> userList);

    /**
     * 获取查询条件
     *
     * @param userQueryRequest 用户查询请求
     * @return {@link QueryWrapper}<{@link User}>
     */
    QueryWrapper<User> getQueryWrapper(UserQueryRequest userQueryRequest);

    /**
     * 用户通过 GitHub 登录
     *
     * @param callback 回调
     * @return {@link TokenLoginUserVo }
     */
    TokenLoginUserVo userLoginByGithub(AuthCallback callback);

    /**
     * 用户通过 Linux Do 登录
     *
     * @param code  授权码
     * @param state 状态参数
     * @return {@link TokenLoginUserVo}
     */
    TokenLoginUserVo userLoginByLinuxDo(String code, String state);

    /**
     * 用户数据
     *
     * @return {@link UserDataWebVO}
     */
    UserDataWebVO getUserDataWebVO();

    /**
     * 新用户数据
     *
     * @return {@link NewUserDataWebVO}
     */
    List<NewUserDataWebVO> getNewUserDataWebVO(NewUserDataWebRequest request);

    /**
     * 生成用户年度总结报告
     *
     * @return 年度总结 HTML 内容
     */
    String generateUserAnnualReport();

}

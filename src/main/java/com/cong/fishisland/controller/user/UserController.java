package com.cong.fishisland.controller.user;

import cn.dev33.satoken.annotation.SaCheckRole;
import com.anji.captcha.model.common.ResponseModel;
import com.anji.captcha.model.vo.CaptchaVO;
import com.anji.captcha.service.CaptchaService;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.cong.fishisland.annotation.NoRepeatSubmit;
import com.cong.fishisland.common.BaseResponse;
import com.cong.fishisland.common.DeleteRequest;
import com.cong.fishisland.common.ErrorCode;
import com.cong.fishisland.common.ResultUtils;
import com.cong.fishisland.common.exception.BusinessException;
import com.cong.fishisland.common.exception.ThrowUtils;
import com.cong.fishisland.constant.PointConstant;
import com.cong.fishisland.constant.RedisKey;
import com.cong.fishisland.constant.UserConstant;
import com.cong.fishisland.model.dto.user.*;
import com.cong.fishisland.model.entity.user.User;
import com.cong.fishisland.model.vo.user.*;
import com.cong.fishisland.service.LinuxDoOAuth2Service;
import com.cong.fishisland.service.UserPointsService;
import com.cong.fishisland.service.UserService;
import com.cong.fishisland.utils.RedisUtils;
import io.swagger.annotations.ApiOperation;
import lombok.extern.slf4j.Slf4j;
import me.zhyd.oauth.model.AuthCallback;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.BeanUtils;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import org.springframework.util.DigestUtils;
import org.springframework.web.bind.annotation.*;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletRequest;
import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.temporal.TemporalAdjusters;
import java.util.List;
import java.util.UUID;

import static com.cong.fishisland.constant.SystemConstants.SALT;

/**
 * 用户接口
 * # @author <a href="https://github.com/lhccong">程序员聪</a>
 */
@RestController
@RequestMapping("/user")
@Slf4j
//@Api(tags = "用户相关")
public class UserController {

    @Resource
    private UserService userService;
    @Resource
    private CaptchaService captchaService;
    @Resource
    private UserPointsService userPointsService;
    @Resource
    private LinuxDoOAuth2Service linuxDoOAuth2Service;

    // region 登录相关

    /**
     * 用户注册
     *
     * @param userRegisterRequest 用户注册请求
     * @return {@link BaseResponse}<{@link Long}>
     */
    @PostMapping("/register")
    @ApiOperation(value = "用户注册")
//    @SaCheckRole(UserConstant.ADMIN_ROLE)
    public BaseResponse<Long> userRegister(@RequestBody UserRegisterRequest userRegisterRequest) {
        if (userRegisterRequest == null) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }
        String userAccount = userRegisterRequest.getUserAccount();
        String userPassword = userRegisterRequest.getUserPassword();
        String checkPassword = userRegisterRequest.getCheckPassword();
        if (StringUtils.isAnyBlank(userAccount, userPassword, checkPassword)) {
            return null;
        }
        CaptchaVO captchaVO = new CaptchaVO();
        captchaVO.setCaptchaVerification(userRegisterRequest.getCaptchaVerification());
        ResponseModel response = captchaService.verification(captchaVO);
        if (!response.isSuccess()) {
            //验证码校验失败，返回信息告诉前端
            //repCode  0000  无异常，代表成功
            //repCode  9999  服务器内部异常
            //repCode  0011  参数不能为空
            //repCode  6110  验证码已失效，请重新获取
            //repCode  6111  验证失败
            //repCode  6112  获取验证码失败,请联系管理员
            throw new BusinessException(ErrorCode.FORBIDDEN_ERROR, "验证码错误请重试");

        }
        if (StringUtils.isAnyBlank(userAccount, userPassword, checkPassword)) {
            return null;
        }
        long result = userService.userRegister(userAccount, userPassword, checkPassword);
        return ResultUtils.success(result);
    }

    /**
     * 用户邮箱注册
     *
     * @param userRegisterRequest 用户注册请求
     * @return {@link BaseResponse}<{@link Long}>
     */
    @PostMapping("/email/register")
    @ApiOperation(value = "用户邮箱注册")
    public BaseResponse<Long> userEmailRegister(@RequestBody UserRegisterRequest userRegisterRequest) {
        if (userRegisterRequest == null) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }
        String userAccount = userRegisterRequest.getUserAccount();
        String email = userRegisterRequest.getEmail();
        String userPassword = userRegisterRequest.getUserPassword();
        String checkPassword = userRegisterRequest.getCheckPassword();
        String code = userRegisterRequest.getCode();
        if (StringUtils.isAnyBlank(email, userPassword, checkPassword)) {
            return null;
        }
        long result = userService.userEmilRegister(userAccount, email, userPassword, checkPassword, code);
        return ResultUtils.success(result);
    }

    /**
     * 用户登录
     *
     * @param userLoginRequest 用户登录请求
     * @return {@link BaseResponse}<{@link LoginUserVO}>
     */
    @PostMapping("/login")
    @ApiOperation(value = "用户登录")
    public BaseResponse<LoginUserVO> userLogin(@RequestBody UserLoginRequest userLoginRequest) {
        if (userLoginRequest == null) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }
        String userAccount = userLoginRequest.getUserAccount();
        String userPassword = userLoginRequest.getUserPassword();
        if (StringUtils.isAnyBlank(userAccount, userPassword)) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }
        LoginUserVO loginUserVO = userService.userLogin(userAccount, userPassword);
        return ResultUtils.success(loginUserVO);
    }

    /**
     * 用户登录
     *
     * @param userLoginRequest 用户登录请求
     * @return {@link BaseResponse}<{@link LoginUserVO}>
     */
    @PostMapping("/email/login")
    @ApiOperation(value = "用户邮箱登录")
    public BaseResponse<LoginUserVO> userEmailLogin(@RequestBody UserLoginRequest userLoginRequest) {
        if (userLoginRequest == null) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }
        String email = userLoginRequest.getEmail();
        String userPassword = userLoginRequest.getUserPassword();
        if (StringUtils.isAnyBlank(email, userPassword)) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }
        LoginUserVO loginUserVO = userService.userEmailLogin(email, userPassword);
        return ResultUtils.success(loginUserVO);
    }

    /**
     * 用户通过 GitHub 登录
     *
     * @param callback 回调
     * @return {@link BaseResponse}<{@link TokenLoginUserVo}>
     */
    @PostMapping("/login/github")
    @ApiOperation(value = "用户GitHub登录")
    public BaseResponse<TokenLoginUserVo> userLoginByGithub(AuthCallback callback) {
        if (callback.getCode() == null) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "Github 登录失败，code 为空");
        }
        TokenLoginUserVo tokenLoginUserVo = userService.userLoginByGithub(callback);
        return ResultUtils.success(tokenLoginUserVo);

    }

    /**
     * 用户通过 Linux Do 登录 - 获取授权链接
     * 4. Linux Do 自动重定向到回调地址（/login/linuxdo/callback），并带上 code 和 state 参数
     *
     * @return {@link BaseResponse}<{@link String}> 返回授权 URL
     */
    @GetMapping("/login/linuxdo")
    @ApiOperation(value = "获取 Linux Do 授权链接")
    public BaseResponse<String> getLinuxDoAuthUrl() {
        // 生成随机 state 参数，用于防止 CSRF 攻击
        String state = UUID.randomUUID().toString().replace("-", "");
        // 生成授权链接，包含 client_id, redirect_uri, response_type=code, state 等参数
        String authUrl = linuxDoOAuth2Service.getAuthorizationUrl(state);
        return ResultUtils.success(authUrl);
    }

    /**
     * Linux Do 授权回调接口
     *
     * @param code  授权码（Linux Do 自动带上）
     * @param state 状态参数（防止 CSRF 攻击，Linux Do 自动带上）
     * @return {@link BaseResponse}<{@link TokenLoginUserVo}> 返回登录用户信息和 Token
     */
    @GetMapping("/login/linuxdo/callback")
    @ApiOperation(value = "Linux Do 授权回调")
    public BaseResponse<TokenLoginUserVo> linuxDoCallback(String code, String state) {
        if (StringUtils.isBlank(code)) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "Linux Do 登录失败，code 为空");
        }
        TokenLoginUserVo tokenLoginUserVo = userService.userLoginByLinuxDo(code, state);
        return ResultUtils.success(tokenLoginUserVo);
    }

    /**
     * 用户注销
     *
     * @return {@link BaseResponse}<{@link Boolean}>
     */
    @PostMapping("/logout")
    @ApiOperation(value = "用户注销")
    public BaseResponse<Boolean> userLogout() {

        boolean result = userService.userLogout();
        return ResultUtils.success(result);
    }

    /**
     * 获取当前登录用户
     *
     * @return {@link BaseResponse}<{@link LoginUserVO}>
     */
    @GetMapping("/get/login")
    @ApiOperation(value = "获取当前登录用户")
    public BaseResponse<LoginUserVO> getLoginUser() {
        User user = userService.getLoginUser();
        return ResultUtils.success(userService.getLoginUserVO(user));
    }

    // endregion

    // region 邮箱相关

    /**
     * 用户邮箱验证码
     *
     * @param userEmailSendRequest 用户注册请求
     * @return {@link BaseResponse}<{@link Long}>
     */
    @PostMapping("/email/send")
    @ApiOperation(value = "用户邮箱验证码")
    public BaseResponse<Boolean> userEmailSend(@RequestBody UserEmailSendRequest userEmailSendRequest, HttpServletRequest request) {
        String email = userEmailSendRequest.getEmail();
        if (email == null) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }
        boolean result = userService.userEmailSend(email, request);
        return ResultUtils.success(result);
    }

    /**
     * 用户找回密码（邮箱）
     *
     * @return {@link BaseResponse}<{@link LoginUserVO}>
     */
    @PostMapping("/email/resetPassword")
    @ApiOperation(value = "用户邮箱找回密码")
    public BaseResponse<Boolean> userEmailResetPassword(@RequestBody UserEmailResetPasswordRequest userEmailRestPasswordRequest) {
        if (userEmailRestPasswordRequest == null) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "重置密码失败，参数为空");
        }
        boolean result = userService.userEmailResetPassword(userEmailRestPasswordRequest.getEmail(), userEmailRestPasswordRequest.getUserPassword(), userEmailRestPasswordRequest.getCheckPassword(), userEmailRestPasswordRequest.getCode());
        return ResultUtils.success(result);
    }

    /**
     * 用户邮箱绑定账号
     *
     * @return {@link BaseResponse}<{@link LoginUserVO}>
     */
    @PostMapping("/email/bindToAccount")
    @ApiOperation(value = "用户邮箱绑定账号")
    public BaseResponse<Boolean> userEmailBindToAccount(@RequestBody UserBindEmailRequest userBindEmailRequest) {
        if (userBindEmailRequest == null) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "邮箱绑定失败，参数为空");
        }
        boolean result = userService.userEmailBindToAccount(userBindEmailRequest.getEmail(), userBindEmailRequest.getCode()
        );
        return ResultUtils.success(result);
    }

    // endregion

    // region 增删改查

    /**
     * 创建用户
     *
     * @param userAddRequest 用户添加请求
     * @return {@link BaseResponse}<{@link Long}>
     */
    @PostMapping("/add")
    @SaCheckRole(UserConstant.ADMIN_ROLE)
    @ApiOperation(value = "创建用户")
    public BaseResponse<Long> addUser(@RequestBody UserAddRequest userAddRequest) {
        if (userAddRequest == null) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }
        User user = new User();
        BeanUtils.copyProperties(userAddRequest, user);
        // 默认密码 12345678
        String defaultPassword = "12345678";
        String encryptPassword = DigestUtils.md5DigestAsHex((SALT + defaultPassword).getBytes());
        user.setUserPassword(encryptPassword);
        boolean result = userService.save(user);
        ThrowUtils.throwIf(!result, ErrorCode.OPERATION_ERROR);
        return ResultUtils.success(user.getId());
    }

    /**
     * 删除用户
     *
     * @param deleteRequest 删除请求
     * @return {@link BaseResponse}<{@link Boolean}>
     */
    @PostMapping("/delete")
    @SaCheckRole(UserConstant.ADMIN_ROLE)
    @ApiOperation(value = "删除用户")
    public BaseResponse<Boolean> deleteUser(@RequestBody DeleteRequest deleteRequest) {
        if (deleteRequest == null || Long.parseLong(deleteRequest.getId()) <= 0) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }
        boolean b = userService.removeById(Long.parseLong(deleteRequest.getId()));
        return ResultUtils.success(b);
    }

    /**
     * 更新用户
     *
     * @param userUpdateRequest 用户更新请求
     * @return {@link BaseResponse}<{@link Boolean}>
     */
    @PostMapping("/update")
    @SaCheckRole(UserConstant.ADMIN_ROLE)
    @ApiOperation(value = "更新用户")
    public BaseResponse<Boolean> updateUser(@RequestBody UserUpdateRequest userUpdateRequest) {
        if (userUpdateRequest == null || userUpdateRequest.getId() == null) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }
        User user = new User();
        BeanUtils.copyProperties(userUpdateRequest, user);
        boolean result = userService.updateById(user);
        ThrowUtils.throwIf(!result, ErrorCode.OPERATION_ERROR);
        return ResultUtils.success(true);
    }

    /**
     * 根据 id 获取用户（仅管理员）
     *
     * @param id 编号
     * @return {@link BaseResponse}<{@link User}>
     */
    @GetMapping("/get")
    @SaCheckRole(UserConstant.ADMIN_ROLE)
    @ApiOperation(value = "根据 id 获取用户（仅管理员）")
    public BaseResponse<User> getUserById(long id) {
        if (id <= 0) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }
        User user = userService.getById(id);
        ThrowUtils.throwIf(user == null, ErrorCode.NOT_FOUND_ERROR);
        return ResultUtils.success(user);
    }

    /**
     * 根据 id 获取包装类
     *
     * @param id 编号
     * @return {@link BaseResponse}<{@link UserVO}>
     */
    @GetMapping("/get/vo")
    @ApiOperation(value = "根据 id 获取包装类")
    public BaseResponse<UserVO> getUserVoById(long id) {
        BaseResponse<User> response = getUserById(id);
        User user = response.getData();
        return ResultUtils.success(userService.getUserVO(user));
    }

    /**
     * 分页获取用户列表（仅管理员）
     *
     * @param userQueryRequest 用户查询请求
     * @return {@link BaseResponse}<{@link Page}<{@link User}>>
     */
    @PostMapping("/list/page")
    @SaCheckRole(UserConstant.ADMIN_ROLE)
    @ApiOperation(value = "分页获取用户列表（仅管理员）")
    public BaseResponse<Page<User>> listUserByPage(@RequestBody UserQueryRequest userQueryRequest) {
        long current = userQueryRequest.getCurrent();
        long size = userQueryRequest.getPageSize();
        Page<User> userPage = userService.page(new Page<>(current, size),
                userService.getQueryWrapper(userQueryRequest));
        return ResultUtils.success(userPage);
    }

    /**
     * 分页获取用户封装列表
     *
     * @param userQueryRequest 用户查询请求
     * @return {@link BaseResponse}<{@link Page}<{@link UserVO}>>
     */
    @PostMapping("/list/page/vo")
    @ApiOperation(value = "分页获取用户封装列表")
    public BaseResponse<Page<UserVO>> listUserVoByPage(@RequestBody UserQueryRequest userQueryRequest) {
        if (userQueryRequest == null) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }
        long current = userQueryRequest.getCurrent();
        long size = userQueryRequest.getPageSize();
        // 限制爬虫
        ThrowUtils.throwIf(size > 20, ErrorCode.PARAMS_ERROR);
        Page<User> userPage = userService.page(new Page<>(current, size),
                userService.getQueryWrapper(userQueryRequest));
        Page<UserVO> userVoPage = new Page<>(current, size, userPage.getTotal());
        List<UserVO> userVO = userService.getUserVO(userPage.getRecords());
        userVoPage.setRecords(userVO);
        return ResultUtils.success(userVoPage);
    }

    // endregion

    /**
     * 更新个人信息
     *
     * @param userUpdateMyRequest 用户更新我请求
     * @return {@link BaseResponse}<{@link Boolean}>
     */
    @NoRepeatSubmit
    @PostMapping("/update/my")
    @ApiOperation(value = "更新个人信息")
    @Transactional(rollbackFor = Exception.class) // 添加事务注解
    public BaseResponse<Boolean> updateMyUser(@RequestBody UserUpdateMyRequest userUpdateMyRequest) {
        ThrowUtils.throwIf(userUpdateMyRequest == null, ErrorCode.PARAMS_ERROR);
        String userName = userUpdateMyRequest.getUserName();
        ThrowUtils.throwIf(StringUtils.isBlank(userName), ErrorCode.PARAMS_ERROR, "请输入用户名");
        ThrowUtils.throwIf(userName.length() > 10, ErrorCode.PARAMS_ERROR, "用户名不能超过10个字符");
        String userProfile = userUpdateMyRequest.getUserProfile();
        ThrowUtils.throwIf(StringUtils.isNotBlank(userProfile) && userProfile.length() > 100, ErrorCode.PARAMS_ERROR, "个人简介不能超过100个字符");
        User loginUser = userService.getLoginUser();
        String loginUserUserName = loginUser.getUserName();
        String userRole = loginUser.getUserRole();
        // ========== 先执行更新操作，除了用户名 ==========
        User user = new User();
        //新用户名为空或者是管理员时，设置用户名
        if (StringUtils.isBlank(loginUserUserName) || UserConstant.ADMIN_ROLE.equals(userRole)) {
            user.setUserName(userName);
        }
        user.setUserAvatar(userUpdateMyRequest.getUserAvatar());
        user.setUserProfile(userProfile);
        user.setId(loginUser.getId());
        if (!StringUtils.isAllBlank(user.getUserName(), user.getUserAvatar(), userProfile)) {
            boolean result = userService.updateById(user);
            ThrowUtils.throwIf(!result, ErrorCode.OPERATION_ERROR);
        }
        // ========== 修改后逻辑：仅在更新成功后处理积分 ==========
        if (StringUtils.isNotBlank(loginUserUserName) && !userName.equals(loginUserUserName) && UserConstant.DEFAULT_ROLE.equals(userRole)) {
            String redisKey = RedisKey.getKey(
                    RedisKey.USER_RENAME_LIMIT,
                    loginUser.getId(),
                    LocalDate.now().format(DateTimeFormatter.ofPattern("yyyyMM"))
            );

            if (RedisUtils.hasKey(redisKey)) {
                throw new BusinessException(ErrorCode.OPERATION_ERROR, "每月仅可修改一次用户名");
            }
            // 修改用户名
            user.setUserName(userUpdateMyRequest.getUserName());
            user.setId(loginUser.getId());
            boolean updated = userService.updateById(user);
            // 扣除积分（在事务中执行）
            if (updated) {
                userPointsService.deductPoints(loginUser.getId(), PointConstant.RENAME_POINT);
            }
            // 设置限制（事务提交后执行）
            TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
                @Override
                public void afterCommit() {
                    LocalDateTime endOfMonth = LocalDate.now()
                            .with(TemporalAdjusters.lastDayOfMonth())
                            .atTime(23, 59, 59);
                    Duration duration = Duration.between(LocalDateTime.now(), endOfMonth);
                    RedisUtils.set(redisKey, "1", duration);
                }
            });
        }
        return ResultUtils.success(true);
    }


    /**
     * 签到
     *
     * @return {@link BaseResponse }<{@link Boolean }>
     */
    @PostMapping("/signIn")
    @ApiOperation(value = "签到")
    public BaseResponse<Boolean> signIn() {
        return ResultUtils.success(userPointsService.signIn());
    }


    /**
     * 用户数据（仅管理员）
     *
     * @return 用户数据
     */
    @ApiOperation(value = "用户数据（仅管理员）")
    @SaCheckRole(UserConstant.ADMIN_ROLE)
    @PostMapping("/get/UserDataWebVO")
    public BaseResponse<UserDataWebVO> getUserDataWebVO() {
        return ResultUtils.success(userService.getUserDataWebVO());
    }

    /**
     * 新增用户走势图（仅管理员）
     *
     * @param request 新增用户数据请求
     * @return 用户新增数据
     */
    @ApiOperation(value = "新增用户走势图（仅管理员）")
    @SaCheckRole(UserConstant.ADMIN_ROLE)
    @PostMapping("/get/NewUserDataWebVO")
    public BaseResponse<List<NewUserDataWebVO>> getNewUserDataWebVO(@RequestBody NewUserDataWebRequest request) {
        return ResultUtils.success(userService.getNewUserDataWebVO(request));
    }

    /**
     * 生成用户年度报告
     *
     * @return 用户年度报告
     */
    @ApiOperation(value = "生成用户年度报告")
    @GetMapping(value = "/get/annualReport", produces = "text/html;charset=UTF-8")
    public String generateAnnualReport() {
        return userService.generateUserAnnualReport();
    }

}

package com.cong.fishisland.service.impl.user;

import cn.dev33.satoken.stp.SaTokenInfo;
import cn.dev33.satoken.stp.StpUtil;
import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.text.CharSequenceUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.cong.fishisland.common.ErrorCode;
import com.cong.fishisland.common.exception.BusinessException;
import com.cong.fishisland.config.GitHubConfig;
import com.cong.fishisland.constant.CommonConstant;
import com.cong.fishisland.constant.SystemConstants;
import com.cong.fishisland.manager.EmailManager;
import com.cong.fishisland.mapper.user.UserMapper;
import com.cong.fishisland.mapper.user.UserThirdAuthMapper;
import com.cong.fishisland.model.dto.user.UserQueryRequest;
import com.cong.fishisland.model.entity.user.User;
import com.cong.fishisland.model.entity.user.UserPoints;
import com.cong.fishisland.model.entity.user.UserThirdAuth;
import com.cong.fishisland.model.enums.UserRoleEnum;
import com.cong.fishisland.model.vo.user.LoginUserVO;
import com.cong.fishisland.model.vo.user.PlatformBindVO;
import com.cong.fishisland.model.vo.user.TokenLoginUserVo;
import com.cong.fishisland.model.vo.user.UserVO;
import com.cong.fishisland.service.UserPointsService;
import com.cong.fishisland.service.UserService;
import com.cong.fishisland.service.UserThirdAuthService;
import com.cong.fishisland.utils.SqlUtils;
import lombok.extern.slf4j.Slf4j;
import me.zhyd.oauth.model.AuthCallback;
import me.zhyd.oauth.model.AuthResponse;
import me.zhyd.oauth.model.AuthUser;
import me.zhyd.oauth.request.AuthRequest;
import org.apache.commons.compress.utils.Lists;
import org.apache.commons.lang3.StringUtils;
import org.redisson.api.RRateLimiter;
import org.redisson.api.RateIntervalUnit;
import org.redisson.api.RateType;
import org.redisson.api.RedissonClient;
import org.springframework.beans.BeanUtils;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.util.DigestUtils;

import javax.annotation.Resource;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

import static com.cong.fishisland.constant.SystemConstants.SALT;

/**
 * 用户服务实现
 * # @author <a href="https://github.com/lhccong">程序员聪</a>
 */
@Service
@Slf4j
public class UserServiceImpl extends ServiceImpl<UserMapper, User> implements UserService {
    @Resource
    private GitHubConfig gitHubConfig;
    @Resource
    private UserPointsService userPointsService;
    @Resource
    private EmailManager emailManager;

    @Resource
    StringRedisTemplate stringRedisTemplate;
    private static final String EMAIL_CODE_PREFIX = "email:code:";

    @Resource
    private RedissonClient redissonClient;

    @Resource
    private UserThirdAuthMapper userThirdAuthMapper;


    @Override
    public long userRegister(String userAccount, String userPassword, String checkPassword) {
        // 1. 校验
        if (StringUtils.isAnyBlank(userAccount, userPassword, checkPassword)) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "参数为空");
        }
        if (userAccount.length() < 4) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "用户账号过短");
        }
        if (userPassword.length() < 8 || checkPassword.length() < 8) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "用户密码过短");
        }
        // 密码和校验密码相同
        if (!userPassword.equals(checkPassword)) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "两次输入的密码不一致");
        }
        Map<String, Object> lockMap = new ConcurrentHashMap<>();

        Object lock = lockMap.computeIfAbsent(userAccount, key -> new Object());

        synchronized (lock) {
            try {
                // 账户不能重复
                QueryWrapper<User> queryWrapper = new QueryWrapper<>();
                queryWrapper.eq("userAccount", userAccount);
                long count = this.baseMapper.selectCount(queryWrapper);
                if (count > 0) {
                    throw new BusinessException(ErrorCode.PARAMS_ERROR, "账号重复");
                }
                // 2. 加密
                String encryptPassword = DigestUtils.md5DigestAsHex((SALT + userPassword).getBytes());
                // 3. 插入数据
                User user = new User();
                user.setUserAccount(userAccount);
                user.setUserPassword(encryptPassword);
                boolean saveResult = this.save(user);
                if (!saveResult) {
                    throw new BusinessException(ErrorCode.SYSTEM_ERROR, "注册失败，数据库错误");
                }
                // 保存积分
                savePoints(user);

                return user.getId();
            } finally {
                // 防止内存泄漏
                lockMap.remove(userAccount);
            }
        }
    }

    /**
     * 用户邮箱注册
     *
     * @param email         邮箱
     * @param userPassword  用户密码
     * @param checkPassword 校验密码
     * @return 用户id
     */
    @Override
    public long userEmilRegister(String userAccount, String email, String userPassword, String checkPassword, String code) {

        String EMAIL_REGISTER_LOCK = "email:register:lock:";

        String RATE_LIMITER_KEY = "email:register:rate_limiter";

        // 校验参数
        if (StringUtils.isAnyBlank(email, code, userPassword, checkPassword)) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "参数不能为空");
        }
        // 校验邮箱格式
        validateEmailFormat(email);

        if (!userPassword.equals(checkPassword)) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "两次密码输入不一致");
        }

        // 使用 Redisson 限流，一分钟最多 10 次
        RRateLimiter rateLimiter = redissonClient.getRateLimiter(RATE_LIMITER_KEY);
        rateLimiter.trySetRate(RateType.OVERALL, 10, 1, RateIntervalUnit.MINUTES);
        if (!rateLimiter.tryAcquire()) {
            throw new BusinessException(ErrorCode.SYSTEM_ERROR, "系统繁忙，请稍后再试");
        }
        // 获取分布式锁，防止并发注册
        String lockKey = EMAIL_REGISTER_LOCK + email;
        Boolean isLocked = stringRedisTemplate.opsForValue().setIfAbsent(lockKey, "1", 10, TimeUnit.SECONDS);
        if (Boolean.FALSE.equals(isLocked)) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "该邮箱正在注册中，请稍后重试");
        }
        try {
            //  校验邮箱是否已注册
            QueryWrapper<User> queryWrapper = new QueryWrapper<>();
            queryWrapper.eq("email", email);
            long count = this.baseMapper.selectCount(queryWrapper);
            if (count > 0) {
                throw new BusinessException(ErrorCode.PARAMS_ERROR, "该邮箱已注册");
            }
            // 校验验证码
            String correctCode = stringRedisTemplate.opsForValue().get(EMAIL_CODE_PREFIX + email);
            if (correctCode == null || !correctCode.equals(code)) {
                throw new BusinessException(ErrorCode.PARAMS_ERROR, "验证码错误或已过期");
            }

            // 验证码比对成功后删除 Redis 中的验证码，防止重复使用
            stringRedisTemplate.delete(EMAIL_CODE_PREFIX + email);

            //  加密
            String encryptPassword = DigestUtils.md5DigestAsHex((SALT + userPassword).getBytes());

            // 插入数据
            User user = new User();
            user.setEmail(email);
            user.setUserAccount(StringUtils.isNotBlank(userAccount) ? userAccount : "邮箱用户" + email);
            user.setUserPassword(encryptPassword);
            boolean saveResult = this.save(user);
            if (!saveResult) {
                throw new BusinessException(ErrorCode.SYSTEM_ERROR, "注册失败，数据库错误");
            }
            // 保存积分
            savePoints(user);
            return user.getId();
        } finally {
            // 释放 Redis 锁
            stringRedisTemplate.delete(lockKey);
        }
    }

    @Override
    public boolean userEmailSend(String email) {
        // 校验邮箱格式
        validateEmailFormat(email);

        // 邮箱不能重复
        boolean emailExists = this.baseMapper.selectCount(new QueryWrapper<User>().eq("email", email)) > 0;
        if (emailExists) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "该邮箱已注册，无法发送验证码");
        }
        // 检查 Redis 是否已有验证码，防止频繁发送
        String existingCode = stringRedisTemplate.opsForValue().get(EMAIL_CODE_PREFIX + email);
        if (StringUtils.isNotBlank(existingCode)) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "验证码已发送，请稍后再试");
        }
        try {
            // 发送验证码邮件
            emailManager.sendVerificationCode(email);

            return true;
        } catch (Exception e) {
            throw new BusinessException(ErrorCode.SYSTEM_ERROR, "邮件发送失败，请检查邮箱是否有效");
        }
    }

    private void savePoints(User user) {
        UserPoints userPoints = new UserPoints();
        userPoints.setUserId(user.getId());
        userPoints.setPoints(100);
        userPoints.setLevel(1);
        userPoints.setUsedPoints(0);
        userPointsService.save(userPoints);
    }

    @Override
    public TokenLoginUserVo userLogin(String userAccount, String userPassword) {
        // 1. 校验
        if (StringUtils.isAnyBlank(userAccount, userPassword)) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "参数为空");
        }
        if (userAccount.length() < 4) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "账号错误");
        }
        if (userPassword.length() < 8) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "密码错误");
        }
        // 2. 加密
        String encryptPassword = DigestUtils.md5DigestAsHex((SALT + userPassword).getBytes());
        // 查询用户是否存在
        QueryWrapper<User> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq("userAccount", userAccount);
        queryWrapper.eq("userPassword", encryptPassword);
        User user = this.baseMapper.selectOne(queryWrapper);
        // 用户不存在
        if (user == null) {
            log.info("user login failed, userAccount cannot match userPassword");
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "用户不存在或密码错误");
        }
        // 3. 记录用户的登录态
        StpUtil.login(user.getId());
        StpUtil.getTokenSession().set(SystemConstants.USER_LOGIN_STATE, user);
        return this.getTokenLoginUserVO(user);
    }

    /**
     * 用户通过邮箱登录
     *
     * @param email        邮箱
     * @param userPassword 用户密码
     */
    @Override
    public LoginUserVO userEmailLogin(String email, String userPassword) {
        // 1. 校验
        if (StringUtils.isAnyBlank(email, userPassword)) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "参数为空");
        }
        // 校验邮箱格式
        validateEmailFormat(email);
        if (userPassword.length() < 8) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "密码错误");
        }
        // 2. 加密
        String encryptPassword = DigestUtils.md5DigestAsHex((SALT + userPassword).getBytes());
        // 查询用户是否存在
        QueryWrapper<User> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq("email", email);
        queryWrapper.eq("userPassword", encryptPassword);
        User user = this.baseMapper.selectOne(queryWrapper);
        // 用户不存在
        if (user == null) {
            log.info("user login failed, email cannot match userPassword");
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "用户不存在或密码错误");
        }
        // 3. 记录用户的登录态
        StpUtil.login(user.getId());
        StpUtil.getTokenSession().set(SystemConstants.USER_LOGIN_STATE, user);
        return this.getTokenLoginUserVO(user);
    }

    @Override
    public User getLoginUser(String token) {
        if (CharSequenceUtil.isEmpty(token)) {
            throw new BusinessException(ErrorCode.NOT_LOGIN_ERROR);
        }

        // 先判断是否已登录
        Object userObj = StpUtil.getTokenSessionByToken(token).get(SystemConstants.USER_LOGIN_STATE);
        User currentUser = (User) userObj;
        // 从数据库查询（追求性能的话可以注释，直接走缓存）
        long userId = currentUser.getId();
        currentUser = this.getById(userId);
        if (currentUser == null) {
            throw new BusinessException(ErrorCode.NOT_LOGIN_ERROR);
        }
        return currentUser;
    }

    public TokenLoginUserVo getTokenLoginUserVO(User user) {
        if (user == null) {
            return null;
        }
        TokenLoginUserVo loginUserVO = new TokenLoginUserVo();
        BeanUtils.copyProperties(user, loginUserVO);
        // 获取 Token  相关参数
        SaTokenInfo tokenInfo = StpUtil.getTokenInfo();
        loginUserVO.setSaTokenInfo(tokenInfo);

        UserPoints userPoints = userPointsService.getOne(new LambdaQueryWrapper<UserPoints>().eq(UserPoints::getUserId, user.getId()));
        if (userPoints == null) {
            return loginUserVO;
        }

        loginUserVO.setPoints(userPoints.getPoints());
        loginUserVO.setLevel(userPoints.getLevel());
        loginUserVO.setUsedPoints(userPoints.getUsedPoints());
        loginUserVO.setLastSignInDate(userPoints.getLastSignInDate());
        return loginUserVO;
    }

    /**
     * 获取登录用户
     * 获取当前登录用户
     *
     * @return {@link User}
     */
    @Override
    public User getLoginUser() {
        if (!StpUtil.isLogin()) {
            throw new BusinessException(ErrorCode.NOT_LOGIN_ERROR);
        }
        // 先判断是否已登录
        Object userObj = StpUtil.getTokenSession().get(SystemConstants.USER_LOGIN_STATE);
        User currentUser = (User) userObj;
        // 从数据库查询（追求性能的话可以注释，直接走缓存）
        long userId = currentUser.getId();
        currentUser = this.getById(userId);
        if (currentUser == null) {
            throw new BusinessException(ErrorCode.NOT_LOGIN_ERROR);
        }
        return currentUser;
    }

    /**
     * 获取登录用户许可 null
     * 获取当前登录用户（允许未登录）
     *
     * @return {@link User}
     */
    @Override
    public User getLoginUserPermitNull() {
        // 先判断是否已登录
        Object userObj = StpUtil.getTokenSession().get(SystemConstants.USER_LOGIN_STATE);
        User currentUser = (User) userObj;
        if (currentUser == null || currentUser.getId() == null) {
            return null;
        }
        // 从数据库查询（追求性能的话可以注释，直接走缓存）
        long userId = currentUser.getId();
        return this.getById(userId);
    }

    /**
     * 是否为管理员
     *
     * @return boolean
     */
    @Override
    public boolean isAdmin() {
        // 仅管理员可查询
        Object userObj = StpUtil.getTokenSession().get(SystemConstants.USER_LOGIN_STATE);
        User user = (User) userObj;
        return isAdmin(user);
    }

    @Override
    public boolean isAdmin(User user) {
        return user != null && UserRoleEnum.ADMIN.getValue().equals(user.getUserRole());
    }

    /**
     * 用户注销
     *
     * @return boolean
     */
    @Override
    public boolean userLogout() {
        if (!StpUtil.isLogin() || StpUtil.getTokenSession().get(SystemConstants.USER_LOGIN_STATE) == null) {
            throw new BusinessException(ErrorCode.OPERATION_ERROR, "未登录");
        }
        // 移除登录态
        StpUtil.logout();
        return true;
    }

    @Override
    public LoginUserVO getLoginUserVO(User user) {
        if (user == null) {
            return null;
        }
        LoginUserVO loginUserVO = new LoginUserVO();
        BeanUtils.copyProperties(user, loginUserVO);
        UserPoints userPoints = userPointsService.getOne(new LambdaQueryWrapper<UserPoints>().eq(UserPoints::getUserId, user.getId()));
        if (userPoints == null) {
            return loginUserVO;
        }

        loginUserVO.setPoints(userPoints.getPoints());
        loginUserVO.setLevel(userPoints.getLevel());
        loginUserVO.setUsedPoints(userPoints.getUsedPoints());
        loginUserVO.setLastSignInDate(userPoints.getLastSignInDate());

        List<UserThirdAuth> userThirdAuths = userThirdAuthMapper.selectList(
                Wrappers.lambdaQuery(UserThirdAuth.class)
                        .eq(UserThirdAuth::getUserId, user.getId())
        );
        List<PlatformBindVO> bindPlatforms  = userThirdAuths.stream().map(
                userThirdAuth -> {
                    PlatformBindVO platformBindVO = new PlatformBindVO();
                    platformBindVO.setPlatform(userThirdAuth.getPlatform());
                    platformBindVO.setNickname(userThirdAuth.getNickname());
                    platformBindVO.setAvatar(userThirdAuth.getAvatar());
                    return platformBindVO;
                }
        ).collect(Collectors.toList());
        loginUserVO.setBindPlatforms(bindPlatforms);

        return loginUserVO;
    }

    @Override
    public UserVO getUserVO(User user) {
        if (user == null) {
            return null;
        }
        UserVO userVO = new UserVO();
        BeanUtils.copyProperties(user, userVO);
        return userVO;
    }

    @Override
    public List<UserVO> getUserVO(List<User> userList) {
        if (CollUtil.isEmpty(userList)) {
            return new ArrayList<>();
        }
        return userList.stream().map(this::getUserVO).collect(Collectors.toList());
    }

    @Override
    public QueryWrapper<User> getQueryWrapper(UserQueryRequest userQueryRequest) {
        if (userQueryRequest == null) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "请求参数为空");
        }
        Long id = userQueryRequest.getId();
        String userAccount = userQueryRequest.getUserAccount();
        String unionId = userQueryRequest.getUnionId();
        String mpOpenId = userQueryRequest.getMpOpenId();
        String userName = userQueryRequest.getUserName();
        String userProfile = userQueryRequest.getUserProfile();
        String userRole = userQueryRequest.getUserRole();
        String sortField = userQueryRequest.getSortField();
        String sortOrder = userQueryRequest.getSortOrder();
        QueryWrapper<User> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq(id != null, "id", id);
        queryWrapper.like(StringUtils.isNotBlank(userAccount), "userAccount", userAccount);
        queryWrapper.eq(StringUtils.isNotBlank(unionId), "unionId", unionId);
        queryWrapper.eq(StringUtils.isNotBlank(mpOpenId), "mpOpenId", mpOpenId);
        queryWrapper.eq(StringUtils.isNotBlank(userRole), "userRole", userRole);
        queryWrapper.like(StringUtils.isNotBlank(userProfile), "userProfile", userProfile);
        queryWrapper.like(StringUtils.isNotBlank(userName), "userName", userName);
        queryWrapper.orderBy(SqlUtils.validSortField(sortField), sortOrder.equals(CommonConstant.SORT_ORDER_ASC),
                sortField);
        return queryWrapper;
    }

    @Override
    public TokenLoginUserVo userLoginByGithub(AuthCallback callback) {
        AuthRequest authRequest = gitHubConfig.getAuthRequest();
        AuthResponse response = authRequest.login(callback);
        // 获取用户信息
        AuthUser authUser = (AuthUser) response.getData();
        if (authUser == null) {
            throw new BusinessException(ErrorCode.SYSTEM_ERROR, "Github 登录失败，获取用户信息失败");
        }
        // 判断用户是否存在
        String userAccount = authUser.getUsername();

        // 1、用户不存在，则注册
        User user = this.getOne(new LambdaQueryWrapper<User>().eq(User::getUserAccount, userAccount));
        if (user == null) {
            saveGithubUser(userAccount, authUser);
        }
        // 2、用户存在，则登录
        return this.userLogin(userAccount, authUser.getUuid() + authUser.getUsername());
    }

    private void saveGithubUser(String userAccount, AuthUser authUser) {
        User user;
        user = new User();
        String defaultPassword = authUser.getUuid() + authUser.getUsername();
        String encryptPassword = DigestUtils.md5DigestAsHex((SALT + defaultPassword).getBytes());
        user.setUserPassword(encryptPassword);
        user.setUserAccount(userAccount);
        user.setUserAvatar(authUser.getAvatar());
        user.setUserProfile(authUser.getRemark());
        user.setUserName(authUser.getNickname());
        user.setUserRole(UserRoleEnum.USER.getValue());
        this.save(user);
    }

    /**
     * 校验邮箱格式
     *
     * @param email 邮箱地址
     */
    private void validateEmailFormat(String email) {
        if (StringUtils.isBlank(email) || !email.matches("^[A-Za-z0-9+_.-]+@(.+)$")) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "邮箱格式不正确");
        }
    }
}

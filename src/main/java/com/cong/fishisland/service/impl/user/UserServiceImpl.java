package com.cong.fishisland.service.impl.user;

import cn.dev33.satoken.stp.SaTokenInfo;
import cn.dev33.satoken.stp.StpUtil;
import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.date.DateUnit;
import cn.hutool.core.date.DateUtil;
import cn.hutool.core.text.CharSequenceUtil;
import cn.hutool.extra.servlet.ServletUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.cong.fishisland.common.ErrorCode;
import com.cong.fishisland.common.exception.BusinessException;
import com.cong.fishisland.common.exception.ThrowUtils;
import com.cong.fishisland.config.GitHubConfig;
import com.cong.fishisland.constant.CommonConstant;
import com.cong.fishisland.constant.NewUserDataTypeWebConstant;
import com.cong.fishisland.constant.SystemConstants;
import com.cong.fishisland.constant.VipTypeConstant;
import com.cong.fishisland.manager.AiManager;
import com.cong.fishisland.manager.EmailManager;
import com.cong.fishisland.mapper.user.UserMapper;
import com.cong.fishisland.mapper.user.UserThirdAuthMapper;
import com.cong.fishisland.mapper.user.UserVipMapper;
import com.cong.fishisland.model.dto.oauth.LinuxDoTokenResponse;
import com.cong.fishisland.model.dto.oauth.LinuxDoUserInfo;
import com.cong.fishisland.model.dto.user.NewUserDataWebRequest;
import com.cong.fishisland.model.dto.user.UserQueryRequest;
import com.cong.fishisland.model.entity.user.*;
import com.cong.fishisland.model.enums.DeleteStatusEnum;
import com.cong.fishisland.model.enums.UserRoleEnum;
import com.cong.fishisland.model.vo.user.*;
import com.cong.fishisland.service.EmailBanService;
import com.cong.fishisland.service.LinuxDoOAuth2Service;
import com.cong.fishisland.service.UserPointsService;
import com.cong.fishisland.service.UserService;
import com.cong.fishisland.service.annual.AnnualReportAiService;
import com.cong.fishisland.service.annual.AnnualReportDataAssembler;
import com.cong.fishisland.service.annual.AnnualReportTemplateService;
import com.cong.fishisland.utils.SqlUtils;
import lombok.extern.slf4j.Slf4j;
import me.zhyd.oauth.model.AuthCallback;
import me.zhyd.oauth.model.AuthResponse;
import me.zhyd.oauth.model.AuthUser;
import me.zhyd.oauth.request.AuthRequest;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.validator.routines.EmailValidator;
import org.redisson.api.RRateLimiter;
import org.redisson.api.RateIntervalUnit;
import org.redisson.api.RateType;
import org.redisson.api.RedissonClient;
import org.springframework.beans.BeanUtils;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.util.DigestUtils;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletRequest;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.ReentrantLock;
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
    private UserMapper userMapper;

    @Resource
    private GitHubConfig gitHubConfig;

    @Resource
    private UserPointsService userPointsService;

    @Resource
    private EmailManager emailManager;

    @Resource
    private EmailBanService emailBanService;

    private static final EmailValidator EMAIL_VALIDATOR = EmailValidator.getInstance(true);

    @Resource
    StringRedisTemplate stringRedisTemplate;

    private static final String EMAIL_CODE_PREFIX = "email:code:";

    private static final String IP_COUNT_PREFIX = "email:ip:";

    // 限流阈值：同一 IP 10 分钟内最多 5 次
    private static final int IP_THRESHOLD = 5;
    private static final Duration IP_WINDOW = Duration.ofMinutes(10);

    @Resource
    private RedissonClient redissonClient;

    @Resource
    private UserThirdAuthMapper userThirdAuthMapper;

    @Resource
    private UserVipMapper userVipMapper;

    @Resource
    private LinuxDoOAuth2Service linuxDoOAuth2Service;

    @Resource
    private AiManager aiManager;

    @Resource
    private AnnualReportDataAssembler annualReportDataAssembler;

    @Resource
    private AnnualReportAiService annualReportAiService;

    @Resource
    private AnnualReportTemplateService annualReportTemplateService;

    private static final ConcurrentHashMap<String, ReentrantLock> LOCK_MAP = new ConcurrentHashMap<>();

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

        ReentrantLock lock = LOCK_MAP.computeIfAbsent(userAccount, k -> new ReentrantLock());
        lock.lock();

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
            lock.unlock();
            LOCK_MAP.remove(userAccount);
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

        // 注册时，邮箱不能重复
        boolean emailExists = this.baseMapper.selectCount(new QueryWrapper<User>().eq("email", email)) > 0;
        if (emailExists) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "该邮箱已注册，请直接登录");
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
            if (code == null) {
                throw new BusinessException(ErrorCode.PARAMS_ERROR, "验证码不能为空");
            }
            String correctCode = stringRedisTemplate.opsForValue().get(EMAIL_CODE_PREFIX + email);
            if (correctCode == null) {
                throw new BusinessException(ErrorCode.PARAMS_ERROR, "验证码已过期");
            }
            if (!correctCode.equals(code)) {
                throw new BusinessException(ErrorCode.PARAMS_ERROR, "验证码错误");
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
    public boolean userEmailSend(String email, HttpServletRequest request) {
        // 校验邮箱格式
        validateEmailFormat(email);
        // 获取客户端 IP
        String clientIp = ServletUtil.getClientIP(request);
        // IP 黑名单检查
        boolean ipBanned = emailBanService.lambdaQuery()
                .eq(EmailBan::getBannedIp, clientIp)
                .exists();
        if (ipBanned) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "您的 IP 已被封禁，暂时无法发送验证码");
        }
        // IP 限流 & 日志告警
        String ipKey = IP_COUNT_PREFIX + clientIp;
        Long ipCount = stringRedisTemplate.opsForValue().increment(ipKey);
        if (ipCount != null) {
            if (ipCount == 1) {
                // 第一次请求，设置过期时间
                stringRedisTemplate.expire(ipKey, IP_WINDOW);
            }
            if (ipCount > IP_THRESHOLD) {
                // 超出阈值，记录警告日志
                log.warn("频繁请求警告：来自 IP [{}] 在 {} 分钟内已请求 {} 次",
                        clientIp, IP_WINDOW.toMinutes(), ipCount);
            }
        }

        // 检查 Redis 是否已有验证码，防止频繁发送
        String redisKey = EMAIL_CODE_PREFIX + email;
        Boolean occupied = stringRedisTemplate.opsForValue()
                .setIfAbsent(redisKey, "SENT", Duration.ofMinutes(5));
        if (Boolean.FALSE.equals(occupied)) {
            // 如果已存在占位（意味着该邮箱已被处理过），直接拒绝
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

    /**
     * 用户邮箱找回密码
     *
     * @param email         邮箱
     * @param userPassword  新密码
     * @param checkPassword 确认密码
     * @param code          验证码
     * @return 脱敏后的用户信息
     */
    @Override
    public boolean userEmailResetPassword(String email, String userPassword, String checkPassword, String code) {

        // 使用常量定义Redis键模式
        String EMAIL_RESET_LOCK = "email:RESET:lock:";
        String RATE_LIMITER_KEY = "email:reset:rate_limiter";

        // 参数校验
        if (StringUtils.isAnyBlank(email, code, userPassword, checkPassword)) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "参数不能为空");
        }

        // 校验邮箱格式
        validateEmailFormat(email);

        // 输入密码与确认密码必须一致
        if (!userPassword.equals(checkPassword)) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "两次密码输入不一致,请重新输入");
        }

        // 获取当前登录用户
        User loginUser = this.getOne(new QueryWrapper<User>().eq("email", email));
        if (loginUser == null) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "该邮箱未注册");
        }

        // 使用 Redisson 限流，一分钟最多 10 次
        RRateLimiter rateLimiter = redissonClient.getRateLimiter(RATE_LIMITER_KEY);
        rateLimiter.trySetRate(RateType.OVERALL, 10, 1, RateIntervalUnit.MINUTES);
        if (!rateLimiter.tryAcquire()) {
            throw new BusinessException(ErrorCode.SYSTEM_ERROR, "操作过于频繁，请稍后再试");
        }
        // 获取分布式锁，防止并发注册
        String lockKey = EMAIL_RESET_LOCK + email;
        Boolean isLocked = stringRedisTemplate.opsForValue().setIfAbsent(lockKey, "1", 10, TimeUnit.SECONDS);
        if (Boolean.FALSE.equals(isLocked)) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "该邮箱正在重置密码中，请稍后重试");
        }
        try {
            // 校验验证码
            String correctCode = stringRedisTemplate.opsForValue().get(EMAIL_CODE_PREFIX + email);
            if (code == null) {
                throw new BusinessException(ErrorCode.PARAMS_ERROR, "验证码不能为空");
            }
            if (correctCode == null) {
                throw new BusinessException(ErrorCode.PARAMS_ERROR, "验证码已过期");
            }
            if (!correctCode.equals(code)) {
                throw new BusinessException(ErrorCode.PARAMS_ERROR, "验证码错误");
            }

            // 验证码比对成功后删除 Redis 中的验证码，防止重复使用
            stringRedisTemplate.delete(EMAIL_CODE_PREFIX + email);

            //  加密
            String encryptPassword = DigestUtils.md5DigestAsHex((SALT + userPassword).getBytes());

            // 新密码不能和原来的密码一致
            if (encryptPassword.equals(loginUser.getUserPassword())) {
                throw new BusinessException(ErrorCode.PARAMS_ERROR, "新密码不能和原来的密码一致");
            }

            // 更新数据
            loginUser.setUserPassword(encryptPassword);
            boolean updateResult = this.updateById(loginUser);
            if (!updateResult) {
                throw new BusinessException(ErrorCode.SYSTEM_ERROR, "重置密码，数据库错误");
            }
        } finally {
            // 释放 Redis 锁
            stringRedisTemplate.delete(lockKey);
        }
        return true;
    }

    /**
     * 用户通过邮箱绑定账号
     *
     * @param email 邮箱
     * @return 脱敏后的用户信息
     */
    @Override
    public boolean userEmailBindToAccount(String email, String code) {

        // 使用常量定义Redis键模式
        String EMAIL_BIND_LOCK = "email:bind:lock:";
        String RATE_LIMITER_KEY = "email:bind:rate_limiter";

        // 参数校验
        if (StringUtils.isAnyBlank(email, code)) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "参数不能为空");
        }

        // 校验邮箱格式
        validateEmailFormat(email);

        // 绑定邮箱不能重复
        boolean emailExists = this.baseMapper.selectCount(new QueryWrapper<User>().eq("email", email)) > 0;
        if (emailExists) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "该邮箱已被绑定，请重新输入");
        }

        // 获取当前登录用户
        User loginUser = this.getLoginUser();
        // 用户必须登录
        if (loginUser == null) {
            throw new BusinessException(ErrorCode.NOT_LOGIN_ERROR, "请先登录");
        }

        // 使用 Redisson 限流，一分钟最多 10 次
        RRateLimiter rateLimiter = redissonClient.getRateLimiter(RATE_LIMITER_KEY);
        rateLimiter.trySetRate(RateType.OVERALL, 10, 1, RateIntervalUnit.MINUTES);
        if (!rateLimiter.tryAcquire()) {
            throw new BusinessException(ErrorCode.SYSTEM_ERROR, "操作过于频繁，请稍后再试");
        }
        // 获取分布式锁，防止并发注册
        String lockKey = EMAIL_BIND_LOCK + email;
        Boolean isLocked = stringRedisTemplate.opsForValue().setIfAbsent(lockKey, "1", 10, TimeUnit.SECONDS);
        if (Boolean.FALSE.equals(isLocked)) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "该邮箱正在绑定中，请稍后重试");
        }
        try {
            //  校验邮箱是否已注册
            QueryWrapper<User> queryWrapper = new QueryWrapper<>();
            queryWrapper.eq("email", email);
            long count = this.baseMapper.selectCount(queryWrapper);
            if (count > 0) {
                throw new BusinessException(ErrorCode.PARAMS_ERROR, "该邮箱已绑定");
            }
            // 校验验证码
            String correctCode = stringRedisTemplate.opsForValue().get(EMAIL_CODE_PREFIX + email);
            if (code == null) {
                throw new BusinessException(ErrorCode.PARAMS_ERROR, "验证码不能为空");
            }
            if (correctCode == null) {
                throw new BusinessException(ErrorCode.PARAMS_ERROR, "验证码已过期");
            }
            if (!correctCode.equals(code)) {
                throw new BusinessException(ErrorCode.PARAMS_ERROR, "验证码错误");
            }

            // 验证码比对成功后删除 Redis 中的验证码，防止重复使用
            stringRedisTemplate.delete(EMAIL_CODE_PREFIX + email);

            // 插入数据
            loginUser.setEmail(email);
            boolean updateResult = this.updateById(loginUser);
            if (!updateResult) {
                throw new BusinessException(ErrorCode.SYSTEM_ERROR, "绑定失败，数据库错误");
            }
        } finally {
            // 释放 Redis 锁
            stringRedisTemplate.delete(lockKey);
        }
        return true;
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
        if (!StpUtil.isLogin()) {
            return null;
        }
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

        loginUserVO.setVip(isUserVip(user.getId()));

        loginUserVO.setPoints(userPoints.getPoints());
        loginUserVO.setLevel(userPoints.getLevel());
        loginUserVO.setUsedPoints(userPoints.getUsedPoints());
        loginUserVO.setLastSignInDate(userPoints.getLastSignInDate());

        List<UserThirdAuth> userThirdAuths = userThirdAuthMapper.selectList(
                Wrappers.lambdaQuery(UserThirdAuth.class)
                        .eq(UserThirdAuth::getUserId, user.getId())
        );
        List<PlatformBindVO> bindPlatforms = userThirdAuths.stream().map(
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

    public boolean isUserVip(Long userId) {
        if (userId == null) {
            return false;
        }

        // 查询用户会员信息
        QueryWrapper<UserVip> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq("userId", userId);
        queryWrapper.eq("isDelete", 0);
        UserVip userVip = userVipMapper.selectOne(queryWrapper);

        if (userVip == null) {
            return false;
        }

        // 如果是永久会员，直接返回true
        if (VipTypeConstant.PERMANENT.equals(userVip.getType())) {
            return true;
        }

        // 如果是月卡会员，检查是否过期
        Date now = new Date();
        return userVip.getValidDays() != null && now.before(userVip.getValidDays());
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
        ThrowUtils.throwIf(userQueryRequest == null, ErrorCode.PARAMS_ERROR, "请求参数为空");
        //创建时间开始不能小于创建时间结束
        String[] createTimeRange = userQueryRequest.getCreateTimeRange();
        ThrowUtils.throwIf(createTimeRange != null && createTimeRange.length == 2 && createTimeRange[0].compareTo(createTimeRange[1]) > 0, ErrorCode.PARAMS_ERROR, "创建时间开始不能小于创建时间结束");
        //更新时间开始不能小于更新时间结束
        String[] updateTimeRange = userQueryRequest.getUpdateTimeRange();
        ThrowUtils.throwIf(updateTimeRange != null && updateTimeRange.length == 2 && updateTimeRange[0].compareTo(updateTimeRange[1]) > 0, ErrorCode.PARAMS_ERROR, "更新时间开始不能小于更新时间结束");
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
        //范围查询
        if (createTimeRange != null && createTimeRange.length == 2) {
            queryWrapper.apply("DATE(createTime) BETWEEN {0} AND {1}", createTimeRange[0], createTimeRange[1]);
        }
        if (updateTimeRange != null && updateTimeRange.length == 2) {
            queryWrapper.apply("DATE(updateTime) BETWEEN {0} AND {1}", updateTimeRange[0], updateTimeRange[1]);
        }
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

    /**
     * 用户通过 Linux Do 登录（推荐方案：使用 user_third_auth 表）
     * <p>
     * 设计理念：
     * 1. 使用 user_third_auth 表关联第三方账号和本地用户
     * 2. 通过 platform='linuxdo' + openid=用户ID 唯一确定用户
     * 3. 支持一个用户绑定多个第三方平台
     * 4. 即使用户在第三方平台修改了用户名、昵称，也能准确识别
     * <p>
     * 使用标准 OAuth2 流程：
     * 第二步：使用授权码获取访问令牌
     * 第三步：使用访问令牌获取用户信息
     *
     * @param code  授权码
     * @param state 状态参数
     * @return {@link TokenLoginUserVo}
     */
    @Override
    public TokenLoginUserVo userLoginByLinuxDo(String code, String state) {

        // 第二步：使用授权码获取访问令牌
        LinuxDoTokenResponse tokenResponse = linuxDoOAuth2Service.getAccessToken(code);
        if (tokenResponse == null || StringUtils.isBlank(tokenResponse.getAccessToken())) {
            throw new BusinessException(ErrorCode.SYSTEM_ERROR, "Linux Do 登录失败，获取访问令牌失败");
        }

        // 第三步：使用访问令牌获取用户信息
        LinuxDoUserInfo userInfo = linuxDoOAuth2Service.getUserInfo(tokenResponse.getAccessToken());
        if (userInfo == null || userInfo.getId() == null) {
            throw new BusinessException(ErrorCode.SYSTEM_ERROR, "Linux Do 登录失败，获取用户信息失败");
        }

        // 核心逻辑：使用 user_third_auth 表查找或创建用户
        String platform = "linuxdo";
        String openid = String.valueOf(userInfo.getId());

        // 1. 查找第三方账号绑定记录
        UserThirdAuth thirdAuth = userThirdAuthMapper.selectOne(
                new LambdaQueryWrapper<UserThirdAuth>()
                        .eq(UserThirdAuth::getPlatform, platform)
                        .eq(UserThirdAuth::getOpenid, openid)
        );

        User user;
        if (thirdAuth == null) {
            // 2. 第三方账号未绑定，创建新用户并绑定
            log.info("Linux Do 账号未绑定，创建新用户: openid={}", openid);
            user = createUserWithLinuxDo(userInfo);

            // 创建第三方账号绑定记录
            saveThirdAuthBinding(user.getId(), platform, openid, userInfo, tokenResponse);
        } else {
            // 3. 第三方账号已绑定，直接获取用户
            log.info("Linux Do 账号已绑定，用户ID: {}", thirdAuth.getUserId());
            user = this.getById(thirdAuth.getUserId());

            if (user == null) {
                throw new BusinessException(ErrorCode.SYSTEM_ERROR, "用户不存在");
            }
        }

        // 4. 登录用户
        StpUtil.login(user.getId());
        StpUtil.getTokenSession().set(SystemConstants.USER_LOGIN_STATE, user);

        log.info("========== Linux Do 登录流程完成 ==========");
        return this.getTokenLoginUserVO(user);
    }

    /**
     * 创建 Linux Do 新用户
     *
     * @param userInfo Linux Do 用户信息
     * @return 创建的用户对象
     */
    private User createUserWithLinuxDo(LinuxDoUserInfo userInfo) {
        User user = new User();

        // 生成唯一账号：linuxdo_ + MD5(id + salt) 的前8位
        String uniqueId = DigestUtils.md5DigestAsHex((SALT + userInfo.getId()).getBytes());
        String userAccount = "linuxdo_" + uniqueId.substring(0, 8);

        // 默认密码 12345678
        String defaultPassword = "12345678";
        String encryptPassword = DigestUtils.md5DigestAsHex((SALT + defaultPassword).getBytes());

        user.setUserAccount(userAccount);
        user.setUserPassword(encryptPassword);
        user.setUserName(StringUtils.isNotBlank(userInfo.getName()) ? userInfo.getName() : userInfo.getUsername());
        user.setUserAvatar(userInfo.getAvatarTemplate());
        user.setUserRole(UserRoleEnum.USER.getValue());

        boolean saveResult = this.save(user);
        if (!saveResult) {
            throw new BusinessException(ErrorCode.SYSTEM_ERROR, "注册失败");
        }

        // 保存积分
        savePoints(user);

        return user;
    }

    /**
     * 保存第三方账号绑定记录
     *
     * @param userId        本地用户 ID
     * @param platform      平台名称
     * @param openid        第三方平台用户 ID（不可变）
     * @param userInfo      用户信息
     * @param tokenResponse Token 响应
     */
    private void saveThirdAuthBinding(Long userId, String platform, String openid, LinuxDoUserInfo userInfo, LinuxDoTokenResponse tokenResponse) {
        UserThirdAuth thirdAuth = new UserThirdAuth();
        thirdAuth.setUserId(userId);
        thirdAuth.setPlatform(platform);
        thirdAuth.setOpenid(openid);
        thirdAuth.setNickname(userInfo.getName());
        thirdAuth.setAvatar(userInfo.getAvatarTemplate());
        thirdAuth.setAccessToken(tokenResponse.getAccessToken());
        thirdAuth.setRefreshToken(tokenResponse.getRefreshToken());

        // 计算过期时间
        if (tokenResponse.getExpiresIn() != null) {
            thirdAuth.setExpireTime(new Date(System.currentTimeMillis() + tokenResponse.getExpiresIn() * 1000L));
        }

        // 保存原始数据（可选）
        // thirdAuth.setRawData(...);  // 如果需要保存完整的 JSON

        int result = userThirdAuthMapper.insert(thirdAuth);
        if (result <= 0) {
            throw new BusinessException(ErrorCode.SYSTEM_ERROR, "保存第三方账号绑定失败");
        }
    }

    @Override
    public UserDataWebVO getUserDataWebVO() {
        //获取登录用户
        getLoginUser();
        QueryWrapper<User> queryWrapper = new QueryWrapper<>();
        //删除状态 未删除
        queryWrapper.eq("isDelete", DeleteStatusEnum.NOT_DELETED);
        return userMapper.getUserDataWebVO(queryWrapper);
    }

    /**
     * 新增用户走势图
     *
     * @param request 新增用户数据请求
     * @return 用户新增数据
     */
    @Override
    public List<NewUserDataWebVO> getNewUserDataWebVO(NewUserDataWebRequest request) {
        validNewUserDataWebRequest(request);
        Integer type = request.getType();
        Date beginTime = request.getBeginTime();
        Date endTime = request.getEndTime();
        //每周新增
        if (NewUserDataTypeWebConstant.EVERY_WEEK.equals(type)) {
            return userMapper.getNewUserDataWebVOEveryWeek();
        }
        //每月新增
        if (NewUserDataTypeWebConstant.EVERY_MONTH.equals(type)) {
            return userMapper.getNewUserDataWebVOEveryMonth();
        }
        //每年新增
        if (NewUserDataTypeWebConstant.EVERY_YEAR.equals(type)) {
            return userMapper.getNewUserDataWebVOEveryYear();
        }
        //时间范围
        if (NewUserDataTypeWebConstant.TIME_RANGE.equals(type) && beginTime != null && endTime != null) {
            return userMapper.getNewUserDataWebVOByTime(beginTime, endTime);
        }
        return CollUtil.newArrayList();
    }

    /**
     * 生成当前登录用户的当年年度报告
     *
     * @return 年度总结 HTML 内容
     */
    @Override
    public String generateUserAnnualReport() {
        // 1. 获取当前登录用户
        User currentUser = getLoginUser();
        Long userId = currentUser.getId();
        ThrowUtils.throwIf(userId == null || userId <= 0, ErrorCode.PARAMS_ERROR, "用户ID不合法");

        // 2. 计算当前年份并做简单校验
        int year = DateUtil.year(new Date());
        ThrowUtils.throwIf(year < 2000 || year > 3000, ErrorCode.PARAMS_ERROR, "年度参数不合法");

        // 3. 聚合年度数据
        UserAnnualReportVO reportData = annualReportDataAssembler.assemble(currentUser, year);

        // 4. 生成年度总结文案（预留 AI 能力）
        String summary = annualReportAiService.generateSummary(reportData);

        // 5. 使用 FreeMarker 模板渲染 HTML
        return annualReportTemplateService.render(reportData, summary);
    }

    /**
     * 新增用户数据校验
     *
     * @param request 新增用户数据请求
     */
    private void validNewUserDataWebRequest(NewUserDataWebRequest request) {
        ThrowUtils.throwIf(request == null, ErrorCode.PARAMS_ERROR, "数据为空");
        Date beginTime = request.getBeginTime();
        Date endTime = request.getEndTime();
        //开始时间、结束时间必须同时为空或者同时不为空
        ThrowUtils.throwIf(beginTime == null & endTime != null, ErrorCode.PARAMS_ERROR, "开始时间不能为空");
        ThrowUtils.throwIf(beginTime != null & endTime == null, ErrorCode.PARAMS_ERROR, "结束时间不能为空");
        if (beginTime != null & endTime != null) {
            //开始时间和结束时间不为空，开始时间不能大于结束时间
            ThrowUtils.throwIf(beginTime.after(endTime), ErrorCode.PARAMS_ERROR, "开始时间不能大于结束时间");
            //开始时间和结束时间范围必须在31天内
            ThrowUtils.throwIf(DateUtil.between(beginTime, endTime, DateUnit.DAY) > 31, ErrorCode.PARAMS_ERROR, "时间范围必须在31天内");
        }
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
     * 校验邮箱
     *
     * @param email 邮箱地址
     */
    private void validateEmailFormat(String email) {
        List<String> emailList = Arrays.asList(
                "qq.com",
                "163.com",
                "gmail.com",
                "126.com",
                "outlook.com",
                "foxmail.com",
                "sina.com",
                "vip.qq.com",
                "139.com",
                "88.com",
                "icloud.com"
        );

        // 基本格式校验
        if (StringUtils.isBlank(email)) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "邮箱不能为空");
        }
        if (!EMAIL_VALIDATOR.isValid(email)) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "邮箱格式不正确");
        }
        String domain = email.substring(email.indexOf('@') + 1).toLowerCase();

        if (!emailList.contains(domain)) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "该邮箱禁止注册");
        }

        // 后缀校验
        String emailSuffix = StringUtils.substringAfter(email, "@").toLowerCase();
        //禁言不是 com 结尾的邮箱后缀
        if (!emailSuffix.endsWith("com")) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "除 com 邮箱外，其他邮箱禁止注册");
        }
        // 查询是否存在于黑名单中
        boolean isBanned = emailBanService.lambdaQuery()
                .eq(EmailBan::getEmailSuffix, emailSuffix)
                .exists();
        if (isBanned) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "该邮箱属于临时邮箱，已被系统封禁，禁止注册");
        }
    }

}
package com.cong.fishisland.service.impl.user;

import cn.dev33.satoken.stp.SaTokenInfo;
import cn.dev33.satoken.stp.StpUtil;
import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.text.CharSequenceUtil;
import com.alibaba.fastjson.JSONObject;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.UpdateWrapper;
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
import org.apache.commons.lang3.StringUtils;
import org.redisson.api.RRateLimiter;
import org.redisson.api.RateIntervalUnit;
import org.redisson.api.RateType;
import org.redisson.api.RedissonClient;
import org.springframework.beans.BeanUtils;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.DigestUtils;

import javax.annotation.Resource;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.ReentrantLock;
import java.util.stream.Collectors;

import static com.cong.fishisland.constant.SystemConstants.SALT;

/**
 * 第三方用户关联
 * # @author <a href="https://github.com/lhccong">程序员聪</a>
 */
@Service
@Slf4j
public class UserThirdAuthServiceImpl extends ServiceImpl<UserThirdAuthMapper, UserThirdAuth> implements UserThirdAuthService {

    @Resource
    private UserService userService;

    @Resource
    private UserPointsService userPointsService;

    private static final ConcurrentHashMap<String, ReentrantLock> LOCK_MAP = new ConcurrentHashMap<>();

    /**
     * 获取三方平台关联信息
     *
     * @param uuid   三方平台唯一id
     * @param source 平台类型
     * @return {@link UserThirdAuth }
     */
    @Override
    public UserThirdAuth getThirdAuth(String uuid, String source) {
        return this.getOne(Wrappers.lambdaQuery(UserThirdAuth.class)
                .eq(UserThirdAuth::getOpenid, uuid)
                .eq(UserThirdAuth::getPlatform, source));
    }

    /**
     * 用户注册
     *
     * @param rowData 三方平台信息
     * @return long
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public long userRegister(AuthUser rowData) {
        // 1. 校验
        // 生成用户账户
        String userAccount = rowData.getSource().toLowerCase() + "_" + rowData.getUuid();
        ReentrantLock lock = LOCK_MAP.computeIfAbsent(userAccount, k -> new ReentrantLock());
        lock.lock();
            try {
                // 账户不能重复
                QueryWrapper<User> queryWrapper = new QueryWrapper<>();
                queryWrapper.eq("userAccount", userAccount);
                long count = userService.getBaseMapper().selectCount(queryWrapper);
                if (count > 0) {
                    throw new BusinessException(ErrorCode.PARAMS_ERROR, "账号重复");
                }
                // 2. 加密
                // 生成随机密码
                String encryptPassword = DigestUtils.md5DigestAsHex(
                        (SALT + "OAUTH_" + rowData.getSource() + "_" + UUID.randomUUID()).getBytes()
                );
                // 3. 插入数据
                User user = new User();
                user.setUserAccount(userAccount);
                user.setUserPassword(encryptPassword);
                boolean saveResult = userService.save(user);
                if (!saveResult) {
                    throw new BusinessException(ErrorCode.SYSTEM_ERROR, "第三方平台注册失败，数据库错误");
                }
                // 保存积分
                savePoints(user);
                return user.getId();
            } finally {
                lock.unlock();
                // 防止内存泄漏
                LOCK_MAP.remove(userAccount);
        }
    }

    /**
     * 保存第三方身份信息
     *
     * @param userId  用户 ID
     * @param source  平台类型
     * @param rowData 三方平台信息
     * @return boolean
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean saveOrUpdateThirdAuth(long userId, String source, AuthUser rowData) {
        UserThirdAuth userThirdAuth = this.getOne(Wrappers.lambdaQuery(UserThirdAuth.class).eq(UserThirdAuth::getUserId, userId).eq(UserThirdAuth::getPlatform, source));
        if (null == userThirdAuth) {
            userThirdAuth = new UserThirdAuth();
        }
        userThirdAuth.setUserId(userId);
        userThirdAuth.setNickname(rowData.getNickname());
        userThirdAuth.setAvatar(rowData.getAvatar());
        userThirdAuth.setOpenid(rowData.getUuid());
        userThirdAuth.setPlatform(rowData.getSource().toLowerCase());
        userThirdAuth.setAccessToken(rowData.getToken().getAccessToken());
        userThirdAuth.setRefreshToken(rowData.getToken().getRefreshToken());
        userThirdAuth.setRawData(JSONObject.toJSONString(rowData));
        return this.saveOrUpdate(userThirdAuth);
    }

    /**
     * 获取 登录用户 Token vo
     *
     * @param userId 用户 ID
     * @return {@link TokenLoginUserVo }
     */
    @Override
    public TokenLoginUserVo getTokenLoginUserVO(long userId) {
        User user = userService.getById(userId);
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
     * 解绑
     *
     * @param source 平台类型
     * @return {@link Boolean }
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public Boolean unbind(String source) {

        User loginUser = userService.getLoginUser();
        Long userId = loginUser.getId();

        // UserThirdAuth userThirdAuth = this.getOne(Wrappers.lambdaQuery(UserThirdAuth.class).eq(UserThirdAuth::getUserId, userId).eq(UserThirdAuth::getPlatform, source));
        // UpdateWrapper<User> updateWrapper = new UpdateWrapper<>();
        // updateWrapper.eq("id", userId).set("isDelete", 1);
        // userService.update(updateWrapper);

        return this.remove(Wrappers.lambdaQuery(UserThirdAuth.class).eq(UserThirdAuth::getUserId, userId).eq(UserThirdAuth::getPlatform, source));
    }

    /**
     * 保存积分
     *
     * @param user 用户
     */
    private void savePoints(User user) {
        UserPoints userPoints = new UserPoints();
        userPoints.setUserId(user.getId());
        userPoints.setPoints(100);
        userPoints.setLevel(1);
        userPoints.setUsedPoints(0);
        userPointsService.save(userPoints);
    }
}

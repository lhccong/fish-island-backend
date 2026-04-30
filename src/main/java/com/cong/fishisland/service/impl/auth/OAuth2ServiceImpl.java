package com.cong.fishisland.service.impl.auth;

import cn.dev33.satoken.stp.StpUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.cong.fishisland.common.ErrorCode;
import com.cong.fishisland.common.exception.BusinessException;
import com.cong.fishisland.common.exception.ThrowUtils;
import com.cong.fishisland.mapper.auth.FishAuthCodeMapper;
import com.cong.fishisland.model.entity.auth.FishAuth;
import com.cong.fishisland.model.entity.auth.FishAuthCode;
import com.cong.fishisland.model.entity.user.User;
import com.cong.fishisland.model.vo.auth.OAuth2TokenVO;
import com.cong.fishisland.model.vo.auth.OAuth2UserInfoVO;
import com.cong.fishisland.service.UserService;
import com.cong.fishisland.service.auth.FishAuthService;
import com.cong.fishisland.service.auth.OAuth2Service;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.util.DigestUtils;
import org.springframework.web.util.UriComponentsBuilder;

import java.util.Arrays;
import java.util.Date;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

/**
 * OAuth2 授权服务端实现（授权码模式）
 *
 * @author cong
 */
@Service
@Slf4j
@RequiredArgsConstructor
public class OAuth2ServiceImpl implements OAuth2Service {

    /** access_token 有效期：2 小时 */
    private static final long TOKEN_EXPIRE_SECONDS = 7200L;

    /** 授权码有效期：5 分钟 */
    private static final long CODE_EXPIRE_MINUTES = 5L;

    /** Redis key 前缀 */
    private static final String TOKEN_KEY_PREFIX = "oauth2:token:";
    private static final String TOKEN_USER_KEY_PREFIX = "oauth2:token:user:";

    private final FishAuthService fishAuthService;
    private final FishAuthCodeMapper fishAuthCodeMapper;
    private final UserService userService;
    private final StringRedisTemplate stringRedisTemplate;

    @Override
    public String authorize(String clientId, String redirectUri, String scope, String state) {
        // 1. 校验客户端
        FishAuth app = getEnabledApp(clientId);

        // 2. 校验回调地址（必须是注册地址之一）
        validateRedirectUri(app, redirectUri);

        // 3. 当前用户必须已登录
        ThrowUtils.throwIf(!StpUtil.isLogin(), ErrorCode.NOT_LOGIN_ERROR);
        long userId = StpUtil.getLoginIdAsLong();

        // 4. 生成授权码并存库
        String code = generateCode();
        FishAuthCode authCode = new FishAuthCode();
        authCode.setCode(code);
        authCode.setClientId(clientId);
        authCode.setUserId(userId);
        authCode.setRedirectUri(redirectUri);
        authCode.setScope(scope != null ? scope : "read");
        authCode.setUsed(0);
        authCode.setExpireTime(new Date(System.currentTimeMillis() + CODE_EXPIRE_MINUTES * 60 * 1000));
        fishAuthCodeMapper.insert(authCode);

        // 5. 拼接回调 URL
        UriComponentsBuilder builder = UriComponentsBuilder.fromUriString(redirectUri)
                .queryParam("code", code);
        if (state != null) {
            builder.queryParam("state", state);
        }
        return builder.toUriString();
    }

    @Override
    public OAuth2TokenVO getToken(String clientId, String clientSecret, String code, String redirectUri) {
        // 1. 校验客户端凭证
        FishAuth app = getEnabledApp(clientId);
        ThrowUtils.throwIf(!app.getClientSecret().equals(clientSecret),
                ErrorCode.PARAMS_ERROR, "client_secret 错误");

        // 2. 查询授权码
        FishAuthCode authCode = fishAuthCodeMapper.selectOne(new LambdaQueryWrapper<FishAuthCode>()
                .eq(FishAuthCode::getCode, code)
                .eq(FishAuthCode::getClientId, clientId));
        ThrowUtils.throwIf(authCode == null, ErrorCode.PARAMS_ERROR, "授权码不存在");
        ThrowUtils.throwIf(authCode.getUsed() == 1, ErrorCode.PARAMS_ERROR, "授权码已使用");
        ThrowUtils.throwIf(authCode.getExpireTime().before(new Date()),
                ErrorCode.PARAMS_ERROR, "授权码已过期");

        // 3. 校验回调地址一致性
        ThrowUtils.throwIf(!authCode.getRedirectUri().equals(redirectUri),
                ErrorCode.PARAMS_ERROR, "redirect_uri 不匹配");

        // 4. 标记授权码已使用
        authCode.setUsed(1);
        fishAuthCodeMapper.updateById(authCode);

        // 5. 生成 access_token 并存入 Redis
        String accessToken = generateAccessToken(clientId, authCode.getUserId());
        String tokenKey = TOKEN_KEY_PREFIX + accessToken;
        // 存储 userId，供 getUserInfo 使用
        stringRedisTemplate.opsForValue().set(tokenKey,
                authCode.getUserId() + ":" + authCode.getScope(),
                TOKEN_EXPIRE_SECONDS, TimeUnit.SECONDS);

        OAuth2TokenVO vo = new OAuth2TokenVO();
        vo.setAccessToken(accessToken);
        vo.setExpiresIn(TOKEN_EXPIRE_SECONDS);
        vo.setScope(authCode.getScope());
        return vo;
    }

    @Override
    public OAuth2UserInfoVO getUserInfo(String accessToken) {
        String tokenKey = TOKEN_KEY_PREFIX + accessToken;
        String value = stringRedisTemplate.opsForValue().get(tokenKey);
        ThrowUtils.throwIf(value == null, ErrorCode.NO_AUTH_ERROR, "access_token 无效或已过期");

        long userId = Long.parseLong(value.split(":")[0]);
        User user = userService.getById(userId);
        ThrowUtils.throwIf(user == null, ErrorCode.NOT_FOUND_ERROR, "用户不存在");

        OAuth2UserInfoVO vo = new OAuth2UserInfoVO();
        vo.setId(String.valueOf(user.getId()));
        vo.setUsername(user.getUserAccount());
        vo.setName(user.getUserName());
        vo.setAvatar(user.getUserAvatar());
        vo.setEmail(user.getEmail());
        return vo;
    }

    // ---- 私有方法 ----

    private FishAuth getEnabledApp(String clientId) {
        FishAuth app = fishAuthService.getOne(new LambdaQueryWrapper<FishAuth>()
                .eq(FishAuth::getClientId, clientId));
        ThrowUtils.throwIf(app == null, ErrorCode.PARAMS_ERROR, "client_id 不存在");
        ThrowUtils.throwIf(app.getStatus() == 0, ErrorCode.FORBIDDEN_ERROR, "应用已被禁用");
        return app;
    }

    private void validateRedirectUri(FishAuth app, String redirectUri) {
        String[] registered = app.getRedirectUri().split(",");
        boolean match = Arrays.stream(registered)
                .map(String::trim)
                .anyMatch(r -> r.equals(redirectUri));
        ThrowUtils.throwIf(!match, ErrorCode.PARAMS_ERROR, "redirect_uri 未注册");
    }

    private String generateCode() {
        return UUID.randomUUID().toString().replace("-", "");
    }

    private String generateAccessToken(String clientId, long userId) {
        String raw = clientId + ":" + userId + ":" + UUID.randomUUID();
        return DigestUtils.md5DigestAsHex(raw.getBytes()) + UUID.randomUUID().toString().replace("-", "");
    }
}

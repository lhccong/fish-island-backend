package com.cong.fishisland.service.impl.auth;

import cn.dev33.satoken.stp.StpUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.cong.fishisland.common.ErrorCode;
import com.cong.fishisland.common.exception.BusinessException;
import com.cong.fishisland.common.exception.ThrowUtils;
import com.cong.fishisland.mapper.auth.FishAuthMapper;
import com.cong.fishisland.model.dto.auth.FishAuthAddRequest;
import com.cong.fishisland.model.dto.auth.FishAuthUpdateRequest;
import com.cong.fishisland.model.entity.auth.FishAuth;
import com.cong.fishisland.model.vo.auth.FishAuthDetailVO;
import com.cong.fishisland.model.vo.auth.FishAuthVO;
import com.cong.fishisland.service.auth.FishAuthService;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Service;
import org.springframework.util.DigestUtils;

import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * 第三方应用服务实现
 *
 * @author cong
 */
@Service
@RequiredArgsConstructor
public class FishAuthServiceImpl extends ServiceImpl<FishAuthMapper, FishAuth>
        implements FishAuthService {

    @Override
    public FishAuthDetailVO createApp(FishAuthAddRequest request) {
        ThrowUtils.throwIf(request.getAppName() == null || request.getAppName().trim().isEmpty(),
                ErrorCode.PARAMS_ERROR, "应用名称不能为空");
        ThrowUtils.throwIf(request.getRedirectUri() == null || request.getRedirectUri().trim().isEmpty(),
                ErrorCode.PARAMS_ERROR, "回调地址不能为空");

        long userId = StpUtil.getLoginIdAsLong();

        FishAuth fishAuth = new FishAuth();
        BeanUtils.copyProperties(request, fishAuth);
        fishAuth.setUserId(userId);
        fishAuth.setClientId(generateClientId());
        fishAuth.setClientSecret(generateClientSecret());
        fishAuth.setStatus(1);
        save(fishAuth);

        FishAuthDetailVO vo = new FishAuthDetailVO();
        BeanUtils.copyProperties(fishAuth, vo);
        return vo;
    }

    @Override
    public void updateApp(FishAuthUpdateRequest request) {
        ThrowUtils.throwIf(request.getId() == null, ErrorCode.PARAMS_ERROR, "应用 ID 不能为空");
        FishAuth fishAuth = getAndCheckOwner(request.getId());

        if (request.getAppName() != null) fishAuth.setAppName(request.getAppName());
        if (request.getAppWebsite() != null) fishAuth.setAppWebsite(request.getAppWebsite());
        if (request.getAppDesc() != null) fishAuth.setAppDesc(request.getAppDesc());
        if (request.getRedirectUri() != null) fishAuth.setRedirectUri(request.getRedirectUri());
        if (request.getStatus() != null) fishAuth.setStatus(request.getStatus());
        updateById(fishAuth);
    }

    @Override
    public void deleteApp(Long id) {
        getAndCheckOwner(id);
        removeById(id);
    }

    @Override
    public String resetSecret(Long id) {
        FishAuth fishAuth = getAndCheckOwner(id);
        String newSecret = generateClientSecret();
        fishAuth.setClientSecret(newSecret);
        updateById(fishAuth);
        return newSecret;
    }

    @Override
    public List<FishAuthVO> listMyApps() {
        long userId = StpUtil.getLoginIdAsLong();
        List<FishAuth> list = list(new LambdaQueryWrapper<FishAuth>()
                .eq(FishAuth::getUserId, userId)
                .orderByDesc(FishAuth::getCreateTime));
        return list.stream().map(app -> {
            FishAuthVO vo = new FishAuthVO();
            BeanUtils.copyProperties(app, vo);
            return vo;
        }).collect(Collectors.toList());
    }

    @Override
    public FishAuthDetailVO getAppDetail(Long id) {
        FishAuth fishAuth = getAndCheckOwner(id);
        FishAuthDetailVO vo = new FishAuthDetailVO();
        BeanUtils.copyProperties(fishAuth, vo);
        return vo;
    }

    @Override
    public FishAuthVO getAppByClientId(String clientId) {
        ThrowUtils.throwIf(clientId == null || clientId.trim().isEmpty(),
                ErrorCode.PARAMS_ERROR, "client_id 不能为空");
        FishAuth fishAuth = getOne(new LambdaQueryWrapper<FishAuth>()
                .eq(FishAuth::getClientId, clientId));
        ThrowUtils.throwIf(fishAuth == null, ErrorCode.NOT_FOUND_ERROR, "应用不存在");
        FishAuthVO vo = new FishAuthVO();
        BeanUtils.copyProperties(fishAuth, vo);
        return vo;
    }

    // ---- 私有方法 ----

    private FishAuth getAndCheckOwner(Long id) {
        FishAuth fishAuth = getById(id);
        ThrowUtils.throwIf(fishAuth == null, ErrorCode.NOT_FOUND_ERROR, "应用不存在");
        long userId = StpUtil.getLoginIdAsLong();
        if (!fishAuth.getUserId().equals(userId)) {
            throw new BusinessException(ErrorCode.NO_AUTH_ERROR, "无权操作该应用");
        }
        return fishAuth;
    }

    private String generateClientId() {
        return "fish_" + UUID.randomUUID().toString().replace("-", "").substring(0, 16);
    }

    private String generateClientSecret() {
        String raw = UUID.randomUUID().toString() + UUID.randomUUID().toString();
        return DigestUtils.md5DigestAsHex(raw.getBytes());
    }
}

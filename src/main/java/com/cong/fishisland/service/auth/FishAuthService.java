package com.cong.fishisland.service.auth;

import com.baomidou.mybatisplus.extension.service.IService;
import com.cong.fishisland.model.dto.auth.FishAuthAddRequest;
import com.cong.fishisland.model.dto.auth.FishAuthUpdateRequest;
import com.cong.fishisland.model.entity.auth.FishAuth;
import com.cong.fishisland.model.vo.auth.FishAuthDetailVO;
import com.cong.fishisland.model.vo.auth.FishAuthVO;

import java.util.List;

/**
 * 第三方应用服务
 *
 * @author cong
 */
public interface FishAuthService extends IService<FishAuth> {

    /**
     * 创建应用，自动生成 clientId / clientSecret
     */
    FishAuthDetailVO createApp(FishAuthAddRequest request);

    /**
     * 更新应用信息
     */
    void updateApp(FishAuthUpdateRequest request);

    /**
     * 删除应用（逻辑删除，仅创建者）
     */
    void deleteApp(Long id);

    /**
     * 重置 clientSecret
     */
    String resetSecret(Long id);

    /**
     * 查询当前用户的应用列表
     */
    List<FishAuthVO> listMyApps();

    /**
     * 获取应用详情（含 clientSecret，仅创建者）
     */
    FishAuthDetailVO getAppDetail(Long id);

    /**
     * 根据 clientId 查询应用公开信息（不含 clientSecret，任何人可访问）
     */
    FishAuthVO getAppByClientId(String clientId);
}

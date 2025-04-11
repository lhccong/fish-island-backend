package com.cong.fishisland.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.cong.fishisland.model.entity.user.UserThirdAuth;
import com.cong.fishisland.model.vo.user.TokenLoginUserVo;
import me.zhyd.oauth.model.AuthUser;

/**
 * 第三方用户关联
 * # @author <a href="https://github.com/lhccong">程序员聪</a>
 */
public interface UserThirdAuthService extends IService<UserThirdAuth> {

    /**
     * 获取三方平台关联信息
     *
     * @param uuid   三方平台唯一id
     * @param source 平台类型
     * @return {@link UserThirdAuth }
     */
    UserThirdAuth getThirdAuth(String uuid, String source);

    /**
     * 用户注册
     *
     * @param rowData 三方平台信息
     * @return long
     */
    long userRegister(AuthUser rowData);

    /**
     * 保存第三方身份信息
     *
     * @param userId  用户 ID
     * @param source  平台类型
     * @param rowData 三方平台信息
     * @return boolean 是否保存成功
     */
    boolean saveOrUpdateThirdAuth(long userId, String source, AuthUser rowData);

    /**
     * 获取 登录用户 Token vo
     *
     * @param userId 用户 ID
     * @return {@link TokenLoginUserVo }
     */
    TokenLoginUserVo getTokenLoginUserVO(long userId);

    /**
     * 解绑
     *
     * @param source 平台类型
     * @return {@link Boolean }
     */
    Boolean unbind(String source);
}

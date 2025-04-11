package com.cong.fishisland.model.entity.user;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import java.util.Date;

/**
 * 第三方用户关联表
 */
@TableName(value = "user_third_auth")
public class UserThirdAuth {
    @TableField(exist = false)
    private static final long serialVersionUID = 1L;
    @TableId(value = "id", type = IdType.ASSIGN_ID)
    private Long id;

    /**
     * 本地用户id
     */
    @TableField(value = "user_id")
    private Long userId;

    /**
     * 昵称
     */
    @TableField(value = "nickname")
    private String nickname;

    /**
     * 头像
     */
    @TableField(value = "avatar")
    private String avatar;

    /**
     * 平台：github/gitee
     */
    @TableField(value = "platform")
    private String platform;

    /**
     * 平台用户id
     */
    @TableField(value = "openid")
    private String openid;

    /**
     * access_token
     */
    @TableField(value = "access_token")
    private String accessToken;

    /**
     * refresh_token
     */
    @TableField(value = "refresh_token")
    private String refreshToken;

    /**
     * 过期时间
     */
    @TableField(value = "expire_time")
    private Date expireTime;

    /**
     * 原始响应数据
     */
    @TableField(value = "raw_data")
    private String rawData;

    /**
     * @return id
     */
    public Long getId() {
        return id;
    }

    /**
     * @param id
     */
    public void setId(Long id) {
        this.id = id;
    }

    /**
     * 获取本地用户id
     *
     * @return user_id - 本地用户id
     */
    public Long getUserId() {
        return userId;
    }

    /**
     * 设置本地用户id
     *
     * @param userId 本地用户id
     */
    public void setUserId(Long userId) {
        this.userId = userId;
    }

    /**
     * 获取昵称
     *
     * @return nickname - 昵称
     */
    public String getNickname() {
        return nickname;
    }

    /**
     * 设置昵称
     *
     * @param nickname 昵称
     */
    public void setNickname(String nickname) {
        this.nickname = nickname;
    }

    /**
     * 获取头像
     *
     * @return avatar - 头像
     */
    public String getAvatar() {
        return avatar;
    }

    /**
     * 设置头像
     *
     * @param avatar 头像
     */
    public void setAvatar(String avatar) {
        this.avatar = avatar;
    }

    /**
     * 获取平台：github/gitee
     *
     * @return platform - 平台：github/gitee
     */
    public String getPlatform() {
        return platform;
    }

    /**
     * 设置平台：github/gitee
     *
     * @param platform 平台：github/gitee
     */
    public void setPlatform(String platform) {
        this.platform = platform;
    }

    /**
     * 获取平台用户id
     *
     * @return openid - 平台用户id
     */
    public String getOpenid() {
        return openid;
    }

    /**
     * 设置平台用户id
     *
     * @param openid 平台用户id
     */
    public void setOpenid(String openid) {
        this.openid = openid;
    }

    /**
     * 获取access_token
     *
     * @return access_token - access_token
     */
    public String getAccessToken() {
        return accessToken;
    }

    /**
     * 设置access_token
     *
     * @param accessToken access_token
     */
    public void setAccessToken(String accessToken) {
        this.accessToken = accessToken;
    }

    /**
     * 获取refresh_token
     *
     * @return refresh_token - refresh_token
     */
    public String getRefreshToken() {
        return refreshToken;
    }

    /**
     * 设置refresh_token
     *
     * @param refreshToken refresh_token
     */
    public void setRefreshToken(String refreshToken) {
        this.refreshToken = refreshToken;
    }

    /**
     * 获取过期时间
     *
     * @return expire_time - 过期时间
     */
    public Date getExpireTime() {
        return expireTime;
    }

    /**
     * 设置过期时间
     *
     * @param expireTime 过期时间
     */
    public void setExpireTime(Date expireTime) {
        this.expireTime = expireTime;
    }

    /**
     * 获取原始响应数据
     *
     * @return raw_data - 原始响应数据
     */
    public String getRawData() {
        return rawData;
    }

    /**
     * 设置原始响应数据
     *
     * @param rawData 原始响应数据
     */
    public void setRawData(String rawData) {
        this.rawData = rawData;
    }
}
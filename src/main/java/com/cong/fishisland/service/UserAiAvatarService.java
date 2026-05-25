package com.cong.fishisland.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.cong.fishisland.model.dto.aiavatar.UserAiAvatarGenerateRequest;
import com.cong.fishisland.model.dto.aiavatar.UserAiAvatarSaveRequest;
import com.cong.fishisland.model.entity.aiavatar.UserAiAvatar;
import com.cong.fishisland.model.vo.aiavatar.UserAiAvatarGenerateVO;

/**
 * 用户 AI 分身 Service
 */
public interface UserAiAvatarService extends IService<UserAiAvatar> {

    /**
     * 保存分身配置（有则更新，无则新增）
     */
    boolean saveAvatar(UserAiAvatarSaveRequest request);

    /**
     * 获取当前登录用户的分身配置
     */
    UserAiAvatar getCurrentUserAvatar();

    /**
     * 根据用户 ID 获取已启用的分身配置（供对话等场景调用）
     */
    UserAiAvatar getEnabledAvatarByUserId(Long userId);

    /**
     * AI 生成系统提示词
     */
    UserAiAvatarGenerateVO generateSystemPrompt(UserAiAvatarGenerateRequest request);
}

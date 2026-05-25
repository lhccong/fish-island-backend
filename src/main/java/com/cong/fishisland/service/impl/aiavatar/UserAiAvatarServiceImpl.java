package com.cong.fishisland.service.impl.aiavatar;

import cn.dev33.satoken.stp.StpUtil;
import cn.hutool.core.text.CharSequenceUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.cong.fishisland.common.ErrorCode;
import com.cong.fishisland.common.exception.ThrowUtils;
import com.cong.fishisland.manager.AiManager;
import com.cong.fishisland.mapper.aiavatar.UserAiAvatarMapper;
import com.cong.fishisland.model.dto.aiavatar.UserAiAvatarGenerateRequest;
import com.cong.fishisland.model.dto.aiavatar.UserAiAvatarSaveRequest;
import com.cong.fishisland.model.entity.aiavatar.UserAiAvatar;
import com.cong.fishisland.model.enums.user.PointsRecordSourceEnum;
import com.cong.fishisland.model.vo.aiavatar.UserAiAvatarGenerateVO;
import com.cong.fishisland.service.UserAiAvatarService;
import com.cong.fishisland.service.UserPointsService;
import com.cong.fishisland.service.UserService;
import com.cong.fishisland.service.UserVipService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.Date;

/**
 * 用户 AI 分身 Service 实现
 */
@Service
@RequiredArgsConstructor
public class UserAiAvatarServiceImpl extends ServiceImpl<UserAiAvatarMapper, UserAiAvatar>
        implements UserAiAvatarService {

    private static final int GENERATE_PROMPT_COST = 50;

    private static final String GENERATE_META_PROMPT =
            "你是一位专业的 AI 角色提示词工程师。根据用户提供的个人描述，生成一段系统提示词（system prompt），"
                    + "用于塑造该用户的 AI 数字分身。\n"
                    + "要求：\n"
                    + "1. 提示词应描述分身的性格、说话风格、知识背景与互动方式\n"
                    + "2. 分身应以明确角色身份与用户对话，语气自然、有辨识度\n"
                    + "3. 只输出提示词正文，不要额外解释或 markdown 标题\n"
                    + "4. 长度控制在 300-1200 字";

    private final AiManager aiManager;
    private final UserVipService userVipService;
    private final UserService userService;
    private final UserPointsService userPointsService;

    @Override
    public boolean saveAvatar(UserAiAvatarSaveRequest request) {
        ThrowUtils.throwIf(request == null, ErrorCode.PARAMS_ERROR);
        ThrowUtils.throwIf(CharSequenceUtil.isBlank(request.getAvatarName()), ErrorCode.PARAMS_ERROR, "分身名称不能为空");
        ThrowUtils.throwIf(request.getEnabled() == null || (request.getEnabled() != 0 && request.getEnabled() != 1),
                ErrorCode.PARAMS_ERROR, "启用状态不合法");

        Long userId = StpUtil.getLoginIdAsLong();
        UserAiAvatar existAvatar = getAvatarByUserId(userId);

        if (existAvatar != null) {
            existAvatar.setAvatarName(request.getAvatarName().trim());
            existAvatar.setSystemPrompt(request.getSystemPrompt());
            existAvatar.setEnabled(request.getEnabled());
            existAvatar.setUpdateTime(new Date());
            return this.updateById(existAvatar);
        }

        UserAiAvatar avatar = new UserAiAvatar();
        avatar.setUserId(userId);
        avatar.setAvatarName(request.getAvatarName().trim());
        avatar.setSystemPrompt(request.getSystemPrompt());
        avatar.setEnabled(request.getEnabled());
        avatar.setCreateTime(new Date());
        avatar.setUpdateTime(new Date());
        avatar.setIsDelete(0);
        return this.save(avatar);
    }

    @Override
    public UserAiAvatar getCurrentUserAvatar() {
        Long userId = StpUtil.getLoginIdAsLong();
        return getAvatarByUserId(userId);
    }

    @Override
    public UserAiAvatar getEnabledAvatarByUserId(Long userId) {
        ThrowUtils.throwIf(userId == null, ErrorCode.PARAMS_ERROR);
        LambdaQueryWrapper<UserAiAvatar> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(UserAiAvatar::getUserId, userId)
                .eq(UserAiAvatar::getEnabled, 1)
                .eq(UserAiAvatar::getIsDelete, 0);
        UserAiAvatar avatar = this.getOne(queryWrapper);
        if (avatar == null || CharSequenceUtil.isBlank(avatar.getSystemPrompt())) {
            return null;
        }
        return avatar;
    }

    @Override
    public UserAiAvatarGenerateVO generateSystemPrompt(UserAiAvatarGenerateRequest request) {
        ThrowUtils.throwIf(request == null, ErrorCode.PARAMS_ERROR);
        ThrowUtils.throwIf(CharSequenceUtil.isBlank(request.getAvatarName()), ErrorCode.PARAMS_ERROR, "分身名称不能为空");
        ThrowUtils.throwIf(CharSequenceUtil.isBlank(request.getSourceContent()), ErrorCode.PARAMS_ERROR, "个人描述不能为空");

        Long userId = StpUtil.getLoginIdAsLong();
        boolean freeGenerate = userVipService.isPermanentVip(userId) || userService.isAdmin();
        if (!freeGenerate) {
            userPointsService.checkAvailablePoints(userId, GENERATE_PROMPT_COST);
        }

        String userPrompt = String.format(
                "分身名称：%s\n个人描述：\n%s",
                request.getAvatarName().trim(),
                request.getSourceContent().trim()
        );
        String systemPrompt = aiManager.doChat(GENERATE_META_PROMPT, userPrompt);
        ThrowUtils.throwIf(CharSequenceUtil.isBlank(systemPrompt), ErrorCode.OPERATION_ERROR, "提示词生成失败");

        if (!freeGenerate) {
            userPointsService.deductPoints(
                    userId,
                    GENERATE_PROMPT_COST,
                    PointsRecordSourceEnum.AI_AVATAR_GENERATE.getValue(),
                    null,
                    "AI分身提示词生成"
            );
        }

        UserAiAvatarGenerateVO vo = new UserAiAvatarGenerateVO();
        vo.setSystemPrompt(systemPrompt.trim());
        return vo;
    }

    private UserAiAvatar getAvatarByUserId(Long userId) {
        LambdaQueryWrapper<UserAiAvatar> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(UserAiAvatar::getUserId, userId)
                .eq(UserAiAvatar::getIsDelete, 0);
        return this.getOne(queryWrapper);
    }
}

package com.cong.fishisland.service.impl.emoticon;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.cong.fishisland.common.ErrorCode;
import com.cong.fishisland.common.exception.BusinessException;
import com.cong.fishisland.common.exception.ThrowUtils;
import com.cong.fishisland.mapper.emoticon.EmoticonFavourMapper;
import com.cong.fishisland.model.dto.emoticon.EmoticonFavourQueryRequest;
import com.cong.fishisland.model.entity.emoticon.EmoticonFavour;
import com.cong.fishisland.model.entity.user.User;
import com.cong.fishisland.service.EmoticonFavourService;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Service;

import java.util.Date;

/**
* @author 许林涛
* @description 针对表【emoticon_favour(收藏表情包表)】的数据库操作Service实现
* @createDate 2025-04-02 16:04:59
*/
@Service
public class EmoticonFavourServiceImpl extends ServiceImpl<EmoticonFavourMapper, EmoticonFavour>
    implements EmoticonFavourService{

    /**
     * 新增收藏表情包
     * @param emoticonSrc 收藏表情包路径
     * @param loginUser 登录用户
     * @return 是否收藏成功
     */
    @Override
    public Boolean addEmoticonFavour(String emoticonSrc, User loginUser) {
        ThrowUtils.throwIf(loginUser == null, ErrorCode.NOT_LOGIN_ERROR);
        ThrowUtils.throwIf(emoticonSrc == null, ErrorCode.PARAMS_ERROR, "收藏表情包路径为空");
        Long userId = loginUser.getId();
        // 判断是否已收藏
        boolean existed = existEmoticonFavour(emoticonSrc, userId);
        if (existed) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "该表情包已收藏过");
        }
        EmoticonFavour emoticonFavour = new EmoticonFavour();
        emoticonFavour.setEmoticonSrc(emoticonSrc);
        emoticonFavour.setUserId(userId);
        emoticonFavour.setOcrNextAttemptAt(new Date());
        return this.save(emoticonFavour);
    }

    @Override
    public Page<EmoticonFavour> listForUser(Long userId, EmoticonFavourQueryRequest request) {
        String keyword = StringUtils.normalizeSpace(StringUtils.defaultString(request.getKeyword()));
        ThrowUtils.throwIf(keyword.length() > 80, ErrorCode.PARAMS_ERROR, "搜索内容不能超过 80 字");
        QueryWrapper<EmoticonFavour> query = new QueryWrapper<>();
        query.eq("userId", userId).orderByDesc("createTime", "id");
        if (!keyword.isEmpty()) {
            String literal = keyword.replace("!", "!!").replace("%", "!%").replace("_", "!_");
            query.apply("ocrText LIKE {0} ESCAPE '!'", "%" + literal + "%");
        }
        return page(new Page<>(request.getCurrent(), request.getPageSize()), query);
    }

    /**
     * 判断是否已收藏
     * @param emoticonSrc 收藏表情包路径
     * @param userId 用户id
     * @return 是否已收藏
     */
    private boolean existEmoticonFavour(String emoticonSrc, Long userId) {
        ThrowUtils.throwIf(emoticonSrc == null, ErrorCode.PARAMS_ERROR, "收藏表情包路径为空");
        ThrowUtils.throwIf(userId == null, ErrorCode.PARAMS_ERROR, "用户ID为空");
        QueryWrapper<EmoticonFavour> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq("emoticonSrc", emoticonSrc)
                .eq("userId", userId);
        return baseMapper.exists(queryWrapper);
    }


}





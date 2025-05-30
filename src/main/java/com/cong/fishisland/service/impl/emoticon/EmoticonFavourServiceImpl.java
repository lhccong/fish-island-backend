package com.cong.fishisland.service.impl.emoticon;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.cong.fishisland.common.ErrorCode;
import com.cong.fishisland.common.exception.BusinessException;
import com.cong.fishisland.common.exception.ThrowUtils;
import com.cong.fishisland.mapper.emoticon.EmoticonFavourMapper;
import com.cong.fishisland.model.entity.emoticon.EmoticonFavour;
import com.cong.fishisland.model.entity.user.User;
import com.cong.fishisland.service.EmoticonFavourService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.aop.framework.AopContext;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.annotation.Resource;

/**
* @author 许林涛
* @description 针对表【emoticon_favour(收藏表情包表)】的数据库操作Service实现
* @createDate 2025-04-02 16:04:59
*/
@Slf4j
@Service
public class EmoticonFavourServiceImpl extends ServiceImpl<EmoticonFavourMapper, EmoticonFavour>
    implements EmoticonFavourService{
    @Resource
    private EmoticonFavourMapper emoticonFavourMapper;

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
        EmoticonFavourService emoticonFavourService = (EmoticonFavourService) AopContext.currentProxy();
        return emoticonFavourService.addEmoticonFavourInner(emoticonSrc, userId);
    }

    /**
     * 收藏表情包到数据库
     * @param emoticonSrc 收藏表情包路径
     * @param userId 用户id
     * @return 是否收藏成功
     */
    @Transactional(rollbackFor = Exception.class)
    @Override
    public Boolean addEmoticonFavourInner(String emoticonSrc, Long userId) {
        EmoticonFavour emoticonFavour = new EmoticonFavour();
        emoticonFavour.setEmoticonSrc(emoticonSrc);
        emoticonFavour.setUserId(userId);
        return this.save(emoticonFavour);
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
        return emoticonFavourMapper.exists(queryWrapper);
    }


}





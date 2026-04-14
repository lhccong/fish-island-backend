package com.cong.fishisland.service.impl.userremark;

import cn.dev33.satoken.stp.StpUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.cong.fishisland.common.ErrorCode;
import com.cong.fishisland.common.exception.BusinessException;
import com.cong.fishisland.mapper.userremark.UserRemarkMapper;
import com.cong.fishisland.model.entity.userremark.UserRemark;
import com.cong.fishisland.service.UserRemarkService;
import org.springframework.stereotype.Service;

import java.util.Date;
import java.util.List;

/**
 * @author cong
 * @description 用户备注表 Service 实现
 */
@Service
public class UserRemarkServiceImpl extends ServiceImpl<UserRemarkMapper, UserRemark>
        implements UserRemarkService {

    @Override
    public boolean saveRemark(String content) {
        Long userId = StpUtil.getLoginIdAsLong();
        
        // 校验内容长度
        if (content != null && content.length() > 512) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "内容超过512个字符");
        }
        
        // 查询当前用户是否已有备注
        LambdaQueryWrapper<UserRemark> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(UserRemark::getUserId, userId)
                   .eq(UserRemark::getIsDelete, 0);
        UserRemark existRemark = this.getOne(queryWrapper);
        
        if (existRemark != null) {
            // 更新
            existRemark.setContent(content);
            existRemark.setUpdateTime(new Date());
            return this.updateById(existRemark);
        } else {
            // 新增
            UserRemark remark = new UserRemark();
            remark.setUserId(userId);
            remark.setContent(content);
            remark.setCreateTime(new Date());
            remark.setUpdateTime(new Date());
            remark.setIsDelete(0);
            return this.save(remark);
        }
    }

    @Override
    public UserRemark getCurrentUserRemark() {
        Long userId = StpUtil.getLoginIdAsLong();
        
        LambdaQueryWrapper<UserRemark> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(UserRemark::getUserId, userId)
                   .eq(UserRemark::getIsDelete, 0);
        
        return this.getOne(queryWrapper);
    }
}

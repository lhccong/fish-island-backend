package com.cong.fishisland.service;

import com.cong.fishisland.model.entity.userremark.UserRemark;
import com.baomidou.mybatisplus.extension.service.IService;

/**
 * @author cong
 * @description 用户备注表 Service
 */
public interface UserRemarkService extends IService<UserRemark> {

    /**
     * 保存备注（有则更新，无则新增）
     * @param content 备注内容
     * @return 是否成功
     */
    boolean saveRemark(String content);

    /**
     * 获取当前用户的备注
     * @return 备注
     */
    UserRemark getCurrentUserRemark();
}

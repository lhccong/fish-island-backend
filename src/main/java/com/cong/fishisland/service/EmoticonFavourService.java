package com.cong.fishisland.service;


import com.baomidou.mybatisplus.extension.service.IService;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.cong.fishisland.model.dto.emoticon.EmoticonFavourQueryRequest;
import com.cong.fishisland.model.entity.emoticon.EmoticonFavour;
import com.cong.fishisland.model.entity.user.User;

/**
* @author 许林涛
* @description 针对表【emoticon_favour(收藏表情包表)】的数据库操作Service
* @createDate 2025-04-02 16:04:59
*/
public interface EmoticonFavourService extends IService<EmoticonFavour> {

    Boolean addEmoticonFavour(String emoticonSrc, User loginUser);

    Page<EmoticonFavour> listForUser(Long userId, EmoticonFavourQueryRequest request);
}

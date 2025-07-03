package com.cong.fishisland.websocket.cache;


import com.cong.fishisland.constant.RedisKey;
import com.cong.fishisland.utils.RedisUtils;
import org.springframework.stereotype.Component;

import java.util.Date;

/**
 * 用户缓存
 * Description: 用户相关缓存
 * @author cong
 * @date 2023/10/31
 */
@Component
public class UserCache {

    public boolean isOnline(Long uid) {
        String onlineKey = RedisKey.getKey(RedisKey.ONLINE_UID_ZET);
        return RedisUtils.zIsMember(onlineKey, uid);
    }

    public void online(Long uid, Date optTime) {
        String onlineKey = RedisKey.getKey(RedisKey.ONLINE_UID_ZET);
        String offlineKey = RedisKey.getKey(RedisKey.OFFLINE_UID_ZET);
        //移除离线表
        RedisUtils.zRemove(offlineKey, uid);
        //更新上线表
        RedisUtils.zAdd(onlineKey, uid, optTime.getTime());
    }
    public void offline(Long uid, Date lastOptTime) {
        String onlineKey = RedisKey.getKey(RedisKey.ONLINE_UID_ZET);
        String offlineKey = RedisKey.getKey(RedisKey.OFFLINE_UID_ZET);
        //移除离线表
        RedisUtils.zRemove(onlineKey, uid);
        //更新上线表
        RedisUtils.zAdd(offlineKey, uid, lastOptTime.getTime());
    }

}

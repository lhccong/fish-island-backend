package com.cong.fishisland.service.impl.hot;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.cong.fishisland.constant.RedisKey;
import com.cong.fishisland.model.entity.hot.HotPost;
import com.cong.fishisland.model.vo.hot.HotPostVO;
import com.cong.fishisland.service.HotPostService;
import com.cong.fishisland.mapper.host.HotPostMapper;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

/**
 * @author cong
 * @description 针对表【hot_post(热点表)】的数据库操作Service实现
 * @createDate 2025-02-21 08:37:06
 */
@Service
@RequiredArgsConstructor
public class HotPostServiceImpl extends ServiceImpl<HotPostMapper, HotPost>
        implements HotPostService {

    private final StringRedisTemplate redisTemplate;
    // 用于序列化和反序列化 JSON
    private final ObjectMapper objectMapper;

    // 缓存时间 30 分钟
    private static final long CACHE_EXPIRE_TIME = 30;

    @Override
    public List<HotPostVO> getHotPostList() {
        // 1. 尝试从 Redis 获取数据
        String hotPostJson = redisTemplate.opsForValue().get(RedisKey.HOT_POST_CACHE_KEY);
        if (hotPostJson != null) {
            try {
                return objectMapper.readValue(hotPostJson, new TypeReference<List<HotPostVO>>() {
                });
            } catch (Exception e) {
                log.error("从 Redis 读取热点文章列表失败", e);
            }
        }

        // 2. 如果 Redis 没有数据，则查询数据库
        List<HotPostVO> hotPostList = this.list(new LambdaQueryWrapper<HotPost>().orderByAsc(HotPost::getSort))
                .stream().map(HotPostVO::objToVo)
                .filter(vo -> vo != null) // 过滤掉转换失败的null值
                .collect(Collectors.toList());

        // 3. 将查询结果存入 Redis，并设置过期时间
        try {
            redisTemplate.opsForValue().set(RedisKey.HOT_POST_CACHE_KEY, objectMapper.writeValueAsString(hotPostList), CACHE_EXPIRE_TIME, TimeUnit.MINUTES);
        } catch (Exception e) {
            log.error("缓存热点文章列表失败", e);
        }

        // 4. 返回数据
        return hotPostList;
    }
}





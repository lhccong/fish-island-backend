package com.cong.fishisland.controller.hero;

import cn.dev33.satoken.annotation.SaCheckRole;
import com.alibaba.fastjson.JSON;
import com.cong.fishisland.common.BaseResponse;
import com.cong.fishisland.common.ErrorCode;
import com.cong.fishisland.common.ResultUtils;
import com.cong.fishisland.common.exception.ThrowUtils;
import com.cong.fishisland.constant.RedisKey;
import com.cong.fishisland.constant.UserConstant;
import com.cong.fishisland.model.entity.user.User;
import com.cong.fishisland.model.vo.hero.HeroRankingVO;
import com.cong.fishisland.model.vo.hero.HeroVO;
import com.cong.fishisland.model.vo.hero.SimpleHeroVO;
import com.cong.fishisland.service.HeroService;
import com.cong.fishisland.service.UserService;
import com.cong.fishisland.utils.CryptoUtils;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.ZSetOperations;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.annotation.Resource;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;
import java.util.stream.Collectors;

/**
 * 王者荣耀英雄控制器
 *
 * @author 许林涛
 * @date 2025年05月02日 16:50
 */
@RestController
@RequestMapping("/hero")
@Slf4j
public class HeroController {
    @Resource
    private HeroService heroService;
    @Resource
    private UserService userService;
    @Resource
    private RedisTemplate<String, Object> redisTemplate;


    /**
     * 初始化英雄列表（仅限管理员使用）
     *
     * @return
     */
    @SaCheckRole(UserConstant.ADMIN_ROLE)
    @PostMapping("/add")
    public BaseResponse<Boolean> addHeroList() {
        return ResultUtils.success(heroService.addHeroList());
    }

    /**
     * 随机获取一个英雄数据
     */
    @GetMapping("/get/random")
    public BaseResponse<String> getRandomHero() {
        // 获取当前用户信息
        User loginUser = userService.getLoginUserPermitNull();
        HeroVO hero = heroService.getRandomHero();
        //  判断当前用户是否登录
        if (loginUser != null && hero != null) {
            Long userId = loginUser.getId();
            // 构建 Redis Key 和 Value
            String redisKey = String.format(RedisKey.GUESS_HERO_USER_CURRENT_HERO, userId);
            // 存储到 Redis，设置过期时间10分钟
            redisTemplate.opsForValue().set(
                    redisKey,
                    Long.valueOf(hero.getId()),
                    10, TimeUnit.MINUTES
            );
        }
        String json = JSON.toJSONString(hero);
        return ResultUtils.success(CryptoUtils.encryptSafely(json));
    }


    /**
     * 获取最新英雄数据
     */
    @GetMapping("/get/new")
    public BaseResponse<HeroVO> getNewHero() {
        return ResultUtils.success(heroService.getNewHero());
    }

    /**
     * 简单英雄数据列表
     */
    @GetMapping("/list/simple")
    public BaseResponse<List<SimpleHeroVO>> listSimpleHero() {
        return ResultUtils.success(heroService.listSimpleHero());
    }

    /**
     * 通过id获取一个英雄数据
     */
    @GetMapping("/get")
    public BaseResponse<HeroVO> getHeroById(Long id) {
        return ResultUtils.success(heroService.getHeroById(id));
    }

    /**
     * 记录猜对英雄次数
     *
     * @return 是否记录成功
     */
    @PostMapping("/guess/success")
    public BaseResponse<Boolean> recordGuessSuccess(Long heroId) {
        User loginUser = userService.getLoginUser();
        Long userId = loginUser.getId();
        // 构建 Redis Key 和 Value
        String redisKey = String.format(RedisKey.GUESS_HERO_USER_CURRENT_HERO, userId);
        // 获取用户当前英雄
        Long storedHeroId = (Long) redisTemplate.opsForValue().get(redisKey);
        //如果不存在，提示错误请先开始游戏
        ThrowUtils.throwIf(storedHeroId == null, ErrorCode.OPERATION_ERROR, "请先开始游戏");
        //redisKey的value值与heroId比较,不相等提示盗刷错误
        ThrowUtils.throwIf(!Objects.equals(storedHeroId, heroId), ErrorCode.OPERATION_ERROR, "请不要进行非法操作");
        // 原子操作记录数据
        redisTemplate.opsForValue().increment(RedisKey.GUESS_HERO_SUCCESS_COUNT);
        redisTemplate.opsForZSet().incrementScore(
                RedisKey.GUESS_HERO_RANKING,
                loginUser.getId().toString(),
                1
        );
        // 成功后删除记录
        redisTemplate.delete(redisKey);
        return ResultUtils.success(true);
    }


    /**
     * 获取猜对英雄次数
     *
     * @return 猜对英雄次数
     */
    @GetMapping("/guess/count")
    public BaseResponse<Integer> getGuessCount() {
        Integer count = (Integer) redisTemplate.opsForValue().get(RedisKey.GUESS_HERO_SUCCESS_COUNT);
        return ResultUtils.success(count != null ? count : 0);
    }

    /**
     * 获取英雄排行榜
     *
     * @return 英雄排行榜
     */
    @GetMapping("/guess/ranking")
    public BaseResponse<List<HeroRankingVO>> getGuessRanking() {
        // 获取分数最高的前10条数据
        Set<ZSetOperations.TypedTuple<Object>> tuples = redisTemplate.opsForZSet()
                .reverseRangeWithScores(RedisKey.GUESS_HERO_RANKING, 0, 9);
        if (tuples == null || tuples.isEmpty()) {
            return ResultUtils.success(Collections.emptyList());
        }
        // 使用原子计数器维护排名
        AtomicLong rankCounter = new AtomicLong(1);
        // 转换数据结构
        List<HeroRankingVO> ranking = tuples.stream().map(tuple -> {
            HeroRankingVO vo = new HeroRankingVO();
            vo.setUserId(Long.parseLong(Objects.requireNonNull(tuple.getValue()).toString()));
            vo.setScore(Objects.requireNonNull(tuple.getScore()).intValue());
            // 获取用户信息
            User user = userService.getById(vo.getUserId());
            vo.setUserName(user.getUserName());
            vo.setUserAvatar(user.getUserAvatar());
            // 设置排名（从1开始递增）
            vo.setRank(rankCounter.getAndIncrement());
            return vo;
        }).collect(Collectors.toList());

        return ResultUtils.success(ranking);
    }

    /**
     * 获取当前用户猜英雄数据
     *
     * @return 当前用户猜英雄数据
     */
    @GetMapping("/guess/user")
    public BaseResponse<HeroRankingVO> getCurrentUserGuessData() {
        // 获取当前登录用户
        User loginUser = userService.getLoginUser();
        Long userId = loginUser.getId();
        String userIdStr = userId.toString();

        // 创建返回对象
        HeroRankingVO result = new HeroRankingVO();
        result.setUserId(userId);
        result.setUserName(loginUser.getUserName());
        result.setUserAvatar(loginUser.getUserAvatar());

        // 先查询用户是否在排行榜里
        Double score = redisTemplate.opsForZSet().score(RedisKey.GUESS_HERO_RANKING, userIdStr);
        if (score != null) {
            // 用户在排行榜中，查询排行榜数据
            result.setScore(score.intValue());
            // 计算排名（从0开始的排名）
            Long rank = redisTemplate.opsForZSet().reverseRank(RedisKey.GUESS_HERO_RANKING, userIdStr);
            if (rank != null) {
                // 设置排名（转换为从1开始）
                result.setRank(rank + 1);
            }
        } else {
            // 用户不在排行榜中，score设为0
            result.setScore(0);
            // 不设置rank属性（保持为null）
        }
        return ResultUtils.success(result);
    }

}

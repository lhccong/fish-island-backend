package com.cong.fishisland.service.impl.game;

import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.date.DateUtil;
import cn.hutool.core.exceptions.ExceptionUtil;
import cn.hutool.core.util.CharsetUtil;
import cn.hutool.http.HttpUtil;
import com.alibaba.fastjson.JSON;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.cong.fishisland.common.ErrorCode;
import com.cong.fishisland.common.exception.ThrowUtils;
import com.cong.fishisland.mapper.hero.HeroMapper;
import com.cong.fishisland.model.dto.hero.HeroDTO;
import com.cong.fishisland.model.dto.hero.HeroDetailDTO;
import com.cong.fishisland.model.entity.hero.Hero;
import com.cong.fishisland.model.vo.hero.HeroVO;
import com.cong.fishisland.model.vo.hero.SimpleHeroVO;
import com.cong.fishisland.service.HeroService;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

/**
 * @author 许林涛
 * @description 针对表【hero(王者荣耀英雄详情表)】的数据库操作Service实现
 * @createDate 2025-05-02 16:47:41
 */
@Slf4j
@Service
public class HeroServiceImpl extends ServiceImpl<HeroMapper, Hero>
        implements HeroService {
    @Resource
    private HeroMapper heroMapper;
    /**
     * 英雄列表接口路径
     */
    private static final String HERO_LIST_URL = "https://pvp.qq.com/web201605/js/herolist.json";
    /**
     * 英雄详情页面路径
     */
    private static final String HERO_DETAIL_URL = "https://pvp.qq.com/zlkdatasys/ip/hero/";

    /**
     * 批量添加英雄
     *
     * @return 是否添加成功
     */
    @Override
    public Boolean addHeroList() {
        try {
            //获取英雄列表
            List<Hero> heroes = getHeroList();
            // 3. 批量插入（使用MyBatis Plus的saveBatch方法）
            boolean saveResult = this.saveBatch(heroes);
            if (!saveResult) return false;
            // 4. 处理英雄详情数据
            heroes.forEach(hero -> {
                try {
                    // 拼接详情接口地址
                    String detailUrl = HERO_DETAIL_URL + "index" + hero.getEname() + ".json";
                    String detailJson = HttpUtil.get(detailUrl, CharsetUtil.CHARSET_UTF_8);
                    // 判断详情接口是否返回数据
                    if (StringUtils.isBlank(detailJson)) {
                        return;
                    }
                    // 解析JSON
                    HeroDetailDTO detailDTO = JSON.parseObject(detailJson, HeroDetailDTO.class);

                    // 获取基础信息
                    List<HeroDetailDTO.BasicInfo> basicInfoList = detailDTO.getBasicInfoList();
                    if (CollUtil.isNotEmpty(basicInfoList)) {
                        HeroDetailDTO.BasicInfo basicInfo = basicInfoList.get(0);
                        if (basicInfo == null) {
                            return;
                        }
                        // 更新实体字段
                        hero.setRace(basicInfo.getRace());
                        hero.setFaction(basicInfo.getFaction());
                        hero.setIdentity(basicInfo.getIdentity());
                        hero.setRegion(basicInfo.getRegion());
                        hero.setAbility(basicInfo.getAbility());
                        hero.setHeight(basicInfo.getHeight());
                        hero.setQuote(basicInfo.getQuote());
                        // 更新数据库
                        heroMapper.updateById(hero);
                    }
                } catch (Exception e) {
                    log.error("处理英雄[{}:{}]详情失败：{}",
                            hero.getCname(), hero.getEname(),
                            ExceptionUtil.getMessage(e));
                }
            });

            return true;
        } catch (Exception e) {
            log.error("批量添加英雄失败，错误信息：{}", ExceptionUtil.getMessage(e), e);
            return false;
        }


    }

    /**
     * 更新英雄皮肤
     *
     * @param ename 英雄编号
     * @return 是否更新成功
     */
    @Override
    public Boolean updateHeroSkins(String ename, String skins) {
        if (StringUtils.isAnyBlank(ename, skins)) {
            return false;
        }
        QueryWrapper<Hero> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq("ename", ename);
        Hero hero = heroMapper.selectOne(queryWrapper);
        if (hero != null) {
            hero.setSkins(skins);
            return heroMapper.updateById(hero) > 0;
        }
        return false;
    }

    /**
     * 更新英雄列表皮肤
     */
    @Override
    public void updateHeroListSkins() {
        try {
            //获取英雄列表
            List<Hero> heroes = getHeroList();
            //更新皮肤字段
            heroes.forEach(hero -> {
                updateHeroSkins(hero.getEname(), hero.getSkins());
            });
        } catch (Exception e) {
            log.error("更新英雄皮肤失败，错误信息：{}", ExceptionUtil.getMessage(e), e);
        }
    }

    /**
     * 随机获取一个英雄数据
     * @return 脱敏后的英雄数据
     */
    @Override
    public HeroVO getRandomHero() {
        Hero hero = heroMapper.selectRandomHero();
        ThrowUtils.throwIf(hero == null, ErrorCode.NOT_FOUND_ERROR,  "英雄数据不存在");
        return convertToHeroVO(hero);
    }

    /**
     * 获取最新英雄数据
     * @return 脱敏后的英雄数据
     */
    @Override
    public HeroVO getNewHero() {
        Hero hero = heroMapper.selectNewestHero();
        ThrowUtils.throwIf(hero == null, ErrorCode.NOT_FOUND_ERROR, "英雄数据不存在");
        return convertToHeroVO(hero);
    }

    /**
     * 简单英雄数据列表
     */
    @Override
    public List<SimpleHeroVO> listSimpleHero() {
        QueryWrapper<Hero> queryWrapper = new QueryWrapper<>();
        queryWrapper.select("id", "cname");
        queryWrapper.orderByAsc("releaseDate","ename");
        List<Hero> heroList = this.list(queryWrapper);
        return heroList.stream()
                .map(hero -> {
                    SimpleHeroVO vo = new SimpleHeroVO();
                    vo.setId(hero.getId());
                    vo.setCname(hero.getCname());
                    return vo;
                })
                .collect(Collectors.toList());
    }

    /**
     * 通过id获取一个英雄数据
     * @param id 英雄id
     * @return 英雄数据
     */
    @Override
    public HeroVO getHeroById(Long id) {
        ThrowUtils.throwIf(id == null, ErrorCode.PARAMS_ERROR, "英雄id不能为空");
        Hero hero = heroMapper.selectById(id);
        ThrowUtils.throwIf(hero == null, ErrorCode.NOT_FOUND_ERROR, "英雄数据不存在");
        return convertToHeroVO(hero);
    }

    /**
     * 将 Hero 实体转换为 HeroVO
     */
    private HeroVO convertToHeroVO(Hero hero) {
        HeroVO vo = new HeroVO();
        BeanUtils.copyProperties(hero, vo);
        Long id = hero.getId();
        if (id != null){
            vo.setId(id.toString());
        }
        vo.setSkinsNum(getSkinsNum(hero.getSkins()));
        return vo;
    }

    /**
     * 获取皮肤数量
     * @param skins 皮肤
     * @return 皮肤数量
     */
    private Integer getSkinsNum(String skins) {
        if (StringUtils.isBlank(skins)) {
            return 0;
        }
        String[] split = skins.split("\\|");
        return split.length;
    }


    /**
     * 获取英雄列表
     * @return
     */
    private List<Hero> getHeroList() {
        try {
            // 1. 获取英雄列表
            String response = HttpUtil.get(HERO_LIST_URL, CharsetUtil.CHARSET_UTF_8);
            if (response == null || response.isEmpty()) {
                // 可选：记录日志或返回空列表
                return Collections.emptyList();
            }

            // 2. JSON数组字符串转HeroDTO列表
            List<HeroDTO> dtos = JSON.parseArray(response, HeroDTO.class);
            if (dtos == null) {
                return Collections.emptyList();
            }

            return dtos.stream()
                    .map(this::convertToHero)
                    .collect(Collectors.toList());

        } catch (Exception e) {
            // 可选：记录日志
            log.error("获取英雄列表失败，错误信息：{}", ExceptionUtil.getMessage(e), e);
            return Collections.emptyList();
        }
    }

    /**
     * 转换 HeroDTO 到 Hero
     */
    private Hero convertToHero(HeroDTO dto) {
        Hero hero = new Hero();
        // Integer转String，避免null转成"null"
        hero.setEname(dto.getEname() == null ? null : dto.getEname().toString());
        hero.setCname(dto.getCname());
        hero.setTitle(dto.getTitle());
        // 日期转换，增加异常处理
        try {
            hero.setReleaseDate(DateUtil.parse(dto.getTime()));
        } catch (Exception e) {
            hero.setReleaseDate(null); // 设置为空或默认时间
        }
        hero.setNewType(dto.getNewType());
        hero.setPrimaryType(dto.getHeroType());
        hero.setSecondaryType(dto.getHeroType2());
        hero.setSkins(dto.getSkinName());
        hero.setOfficialLink(dto.getMBlLink());
        // Integer转Long，注意null处理
        hero.setMossId(dto.getMossId() == null ? null : dto.getMossId().longValue());
        return hero;
    }


}





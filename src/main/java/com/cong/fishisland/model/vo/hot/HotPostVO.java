package com.cong.fishisland.model.vo.hot;

import cn.hutool.json.JSONUtil;
import com.alibaba.fastjson.JSON;
import com.baomidou.mybatisplus.annotation.TableField;
import com.cong.fishisland.model.entity.hot.HotPost;
import com.cong.fishisland.model.enums.CategoryTypeEnum;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanUtils;

import java.io.Serializable;
import java.util.Date;
import java.util.List;

/**
 * 热榜视图
 * # @author <a href="https://github.com/lhccong">程序员聪</a>
 */
@Slf4j
@Data
public class HotPostVO implements Serializable {

    /**
     * id
     */
    private Long id;

    /**
     * 排行榜名称
     */
    private String name;

    /**
     * 热点类型
     */
    private String type;

    /**
     * 热点类型名称
     */
    private String typeName;

    /**
     * 图标地址
     */
    private String iconUrl;

    /**
     * 更新时间
     */
    private Date updateTime;

    /**
     * 分类
     */
    private Integer category;

    /**
     * 分类名称
     */
    private String categoryName;

    /**
     * 数据
     */
    private List<HotPostDataVO> data;

    /**
     * 对象转包装类
     *
     * @param hotPost 热门帖子
     * @return {@link HotPostVO }
     */
    public static HotPostVO objToVo(HotPost hotPost) {
        if (hotPost == null) {
            return null;
        }
        HotPostVO hotPostVO = new HotPostVO();
        try {
            hotPostVO.setId(hotPost.getId());
            hotPostVO.setName(hotPost.getName());
            hotPostVO.setType(hotPost.getType());
            hotPostVO.setTypeName(hotPost.getTypeName());
            hotPostVO.setIconUrl(hotPost.getIconUrl());
            hotPostVO.setUpdateTime(hotPost.getUpdateTime());
            hotPostVO.setCategory(hotPost.getCategory());
            hotPostVO.setCategoryName(CategoryTypeEnum.getEnumByValue(hotPost.getCategory()).getText());
            hotPostVO.setData(JSON.parseArray(hotPost.getHostJson(), HotPostDataVO.class));
            return hotPostVO;
        } catch (Exception e) {
            log.error("转换HotPost到HotPostVO失败，type={}, name={}, error={}", 
                hotPost.getType(), hotPost.getName(), e.getMessage());
            return null;
        }
    }

}

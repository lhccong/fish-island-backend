package com.cong.fishisland.model.vo.tags;

import com.cong.fishisland.model.entity.tags.Tags;
import lombok.Data;
import org.springframework.beans.BeanUtils;

import java.io.Serializable;

/**
 * 标签视图
 *
 * @author <a href="https://github.com/lhccong">聪</a>
 */
@Data
public class TagsVO implements Serializable {

    private Long id;

    /**
     * 标签名称
     */
    private String tagsName;

    /**
     * 类型
     */
    private Integer type;

    /**
     * 图标
     */
    private String icon;

    /**
     * 颜色
     */
    private String color;

    /**
     * 排序
     */
    private Integer sort;


    /**
     * 封装类转对象
     *
     * @param tagsVO
     * @return
     */
    public static Tags voToObj(TagsVO tagsVO) {
        if (tagsVO == null) {
            return null;
        }
        Tags tags = new Tags();
        BeanUtils.copyProperties(tagsVO, tags);
        return tags;
    }

    /**
     * 对象转封装类
     *
     * @param tags
     * @return
     */
    public static TagsVO objToVo(Tags tags) {
        if (tags == null) {
            return null;
        }
        TagsVO tagsVO = new TagsVO();
        BeanUtils.copyProperties(tags, tagsVO);
        return tagsVO;
    }
}

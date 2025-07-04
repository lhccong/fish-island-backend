package com.cong.fishisland.service;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.IService;
import com.cong.fishisland.model.dto.tags.TagsQueryRequest;
import com.cong.fishisland.model.entity.tags.Tags;
import com.cong.fishisland.model.vo.tags.TagsVO;

/**
 * 标签服务
 *
 * @author <a href="https://github.com/lhccong">聪</a>
 */
public interface TagsService extends IService<Tags> {

    /**
     * 校验数据
     *
     * @param tags 数据
     * @param add 对创建的数据进行校验
     */
    void validTags(Tags tags, boolean add);

    /**
     * 获取查询条件
     *
     * @param tagsQueryRequest 查询条件
     * @return QueryWrapper
     */
    QueryWrapper<Tags> getQueryWrapper(TagsQueryRequest tagsQueryRequest);
    
    /**
     * 获取标签封装
     *
     * @param tags 标签实体
     * @return TagsVO
     */
    TagsVO getTagsVO(Tags tags);

    /**
     * 分页获取标签封装
     *
     * @param tagsPage 分页数据
     * @return Page<TagsVO>
     */
    Page<TagsVO> getTagsVOPage(Page<Tags> tagsPage);
}

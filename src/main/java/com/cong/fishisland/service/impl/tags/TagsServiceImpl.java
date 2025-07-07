package com.cong.fishisland.service.impl.tags;

import cn.hutool.core.collection.CollUtil;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.cong.fishisland.common.ErrorCode;
import com.cong.fishisland.common.exception.ThrowUtils;
import com.cong.fishisland.constant.CommonConstant;
import com.cong.fishisland.mapper.tags.TagsMapper;
import com.cong.fishisland.model.dto.tags.TagsQueryRequest;
import com.cong.fishisland.model.entity.tags.Tags;
import com.cong.fishisland.model.vo.tags.TagsVO;
import com.cong.fishisland.service.TagsService;
import com.cong.fishisland.utils.SqlUtils;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.ObjectUtils;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;

/**
 * 标签服务实现
 *
 * @author <a href="https://github.com/lhccong">聪</a>
 */
@Service
@Slf4j
public class TagsServiceImpl extends ServiceImpl<TagsMapper, Tags> implements TagsService {

    /**
     * 校验数据
     *
     * @param tags
     * @param add  对创建的数据进行校验
     */
    @Override
    public void validTags(Tags tags, boolean add) {
        ThrowUtils.throwIf(tags == null, ErrorCode.PARAMS_ERROR);
        String tagsName = tags.getTagsName();
        String icon = tags.getIcon();
        String color = tags.getColor();
        Integer sort = tags.getSort();
        ThrowUtils.throwIf(StringUtils.isNotBlank(icon) && icon.length() > 20, ErrorCode.PARAMS_ERROR, "图标值过长");
        ThrowUtils.throwIf(StringUtils.isNotBlank(color) && color.length() > 20, ErrorCode.PARAMS_ERROR, "颜色值过长");
        if (sort != null) {
            ThrowUtils.throwIf(sort < 0 || sort > 100, ErrorCode.PARAMS_ERROR, "排序值超出范围");
        }
        // 创建数据时，参数不能为空
        if (add) {
            ThrowUtils.throwIf(StringUtils.isBlank(tagsName), ErrorCode.PARAMS_ERROR);
            // 检查标签名称是否已存在（排除自身）
            checkTagNameExist(tagsName, null);
        }

        // 修改数据时，有参数则校验
        if (StringUtils.isNotBlank(tagsName)) {
            ThrowUtils.throwIf(tagsName.length() > 80, ErrorCode.PARAMS_ERROR, "标签过长");
            // 检查标签名称是否已存在（排除自身）
            checkTagNameExist(tagsName, tags.getId());
        }
    }

    /**
     * 检查标签名称是否已存在
     *
     * @param tagName   要检查的标签名称
     * @param excludeId 要排除的标签ID（可为空）
     */
    private void checkTagNameExist(String tagName, Long excludeId) {
        QueryWrapper<Tags> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq("tagsName", tagName);

        // 排除指定ID的标签
        if (excludeId != null) {
            queryWrapper.ne("id", excludeId);
        }

        long count = this.count(queryWrapper);
        ThrowUtils.throwIf(count > 0, ErrorCode.PARAMS_ERROR, "标签名称已存在");
    }

    /**
     * 获取查询条件
     *
     * @param tagsQueryRequest
     * @return
     */
    @Override
    public QueryWrapper<Tags> getQueryWrapper(TagsQueryRequest tagsQueryRequest) {
        QueryWrapper<Tags> queryWrapper = new QueryWrapper<>();
        if (tagsQueryRequest == null) {
            return queryWrapper;
        }
        // 从对象中取值
        Long id = tagsQueryRequest.getId();
        String tagsName = tagsQueryRequest.getTagsName();
        Integer type = tagsQueryRequest.getType();
        String sortField = tagsQueryRequest.getSortField();
        String sortOrder = tagsQueryRequest.getSortOrder();
        // 精确查询
        queryWrapper.eq(ObjectUtils.isNotEmpty(type), "type", type);
        queryWrapper.eq(ObjectUtils.isNotEmpty(id), "id", id);
        // 模糊查询
        queryWrapper.like(StringUtils.isNotBlank(tagsName), "tagsName", tagsName);
        // 排序规则
        queryWrapper.orderBy(SqlUtils.validSortField(sortField),
                sortOrder.equals(CommonConstant.SORT_ORDER_ASC),
                sortField);
        return queryWrapper;
    }

    /**
     * 获取标签封装
     *
     * @param tags
     * @return
     */
    @Override
    public TagsVO getTagsVO(Tags tags) {
        // 对象转封装类
        return TagsVO.objToVo(tags);
    }

    /**
     * 分页获取标签封装
     *
     * @param tagsPage 分页对象
     * @return 分页对象
     */
    @Override
    public Page<TagsVO> getTagsVOPage(Page<Tags> tagsPage) {
        List<Tags> tagsList = tagsPage.getRecords();
        Page<TagsVO> tagsVOPage = new Page<>(tagsPage.getCurrent(), tagsPage.getSize(), tagsPage.getTotal());
        if (CollUtil.isEmpty(tagsList)) {
            return tagsVOPage;
        }
        // 对象列表 => 封装对象列表
        List<TagsVO> tagsVOList = tagsList.stream().map(TagsVO::objToVo).collect(Collectors.toList());
        tagsVOPage.setRecords(tagsVOList);
        return tagsVOPage;
    }

}

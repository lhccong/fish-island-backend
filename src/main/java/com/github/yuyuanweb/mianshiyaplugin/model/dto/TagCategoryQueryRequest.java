package com.github.yuyuanweb.mianshiyaplugin.model.dto;

import com.github.yuyuanweb.mianshiyaplugin.model.common.PageRequest;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.ToString;

import java.io.Serializable;
import java.util.List;


/**
 * 标签类别对象查询请求
 *
 * @author <a href="https://github.com/liyupi">程序员鱼皮</a>
 * @from <a href="https://yupi.icu">编程导航知识星球</a>
 */
@Data
@ToString
@EqualsAndHashCode(callSuper = true)
public class TagCategoryQueryRequest extends PageRequest implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * id
     */
    private Long id;


    /**
     * 名称
     */
    private String name;


    /**
     * 创建用户 id
     */
    private Long userId;


    /**
     * 标签列表
     */
    private List<String> tagList;


    /**
     * 至少有一个标签
     */
    private List<String> orTagList;

}

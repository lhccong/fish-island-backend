package com.github.yuyuanweb.mianshiyaplugin.model.response;

import lombok.Data;

import java.util.List;

/**
 * 分组标签
 * @author pine
 */
@Data
public class TagGroup {

    /**
     * 组名
     */
    String name;

    /**
     * 标签名列表
     */
    List<String> tagList;
}

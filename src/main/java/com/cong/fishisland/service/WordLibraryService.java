package com.cong.fishisland.service;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.service.IService;
import com.cong.fishisland.model.dto.word.WordLibraryQueryRequest;
import com.cong.fishisland.model.entity.word.WordLibrary;

import java.util.Map;
import java.util.Set;

/**
* @author 许林涛
* @description 针对表【word_library(词库表)】的数据库操作Service
* @createDate 2025-07-25 10:41:37
*/
public interface WordLibraryService extends IService<WordLibrary> {

    /**
     * 获取你画我猜词库
     * @param category 词库分类
     * @param usedWords 已使用的词
     * @return 词库
     */
    Map<String, String> getDrawGameWordLibrary(String category, Set<String> usedWords);

    /**
     * 获取谁是卧底词库
     * @param category 词库分类
     * @param usedWordPairs 已使用的词对
     * @return 词语对数组，第一个元素为平民词，第二个元素为卧底词
     */
    String[] getUndercoverGameWordLibrary(String category,Set<String> usedWordPairs);

    /**
     * 获取查询包装类
     *
     * @param wordLibraryQueryRequest 词库查询请求
     * @return 查询包装类
     */
    QueryWrapper<WordLibrary> getQueryWrapper(WordLibraryQueryRequest wordLibraryQueryRequest);

    /**
     * 判断词库是否存在
     * @param word 词语名称
     * @param category 词库分类
     * @param id 词库id
     * @return 词库是否存在
     */
    Boolean existWordLibrary(String word, String category, Long id);

}

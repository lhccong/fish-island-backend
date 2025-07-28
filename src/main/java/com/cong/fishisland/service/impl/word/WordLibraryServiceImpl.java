package com.cong.fishisland.service.impl.word;

import cn.hutool.core.collection.CollUtil;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.cong.fishisland.common.ErrorCode;
import com.cong.fishisland.common.exception.ThrowUtils;
import com.cong.fishisland.constant.CommonConstant;
import com.cong.fishisland.mapper.word.WordLibraryMapper;
import com.cong.fishisland.model.dto.word.WordLibraryQueryRequest;
import com.cong.fishisland.model.entity.word.WordLibrary;
import com.cong.fishisland.service.WordLibraryService;
import com.cong.fishisland.utils.SqlUtils;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.util.*;

/**
* @author 许林涛
* @description 针对表【word_library(词库表)】的数据库操作Service实现
* @createDate 2025-07-25 10:41:37
*/
@Service
public class WordLibraryServiceImpl extends ServiceImpl<WordLibraryMapper, WordLibrary>
    implements WordLibraryService{

    @Resource
    private WordLibraryMapper wordLibraryMapper;

    /**
     * 获取你画我猜词库
     * @param category 词库分类
     * @param usedWords 已使用的词
     * @return 词库
     */
    @Override
    public Map<String, String> getDrawGameWordLibrary(String category, Set<String> usedWords) {
        ThrowUtils.throwIf(StringUtils.isBlank(category), ErrorCode.PARAMS_ERROR,"词库分类为空");
        //随机查询一个你画我猜词
        QueryWrapper<WordLibrary> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq("category", category);
        queryWrapper.notIn(CollUtil.isNotEmpty(usedWords), "word", usedWords);
        queryWrapper.last("ORDER BY RAND() LIMIT 1");
        WordLibrary wordLibrary = wordLibraryMapper.selectOne(queryWrapper);
        if (wordLibrary != null){
            Map<String, String> map = new HashMap<>();
            map.put("word", wordLibrary.getWord());
            map.put("hint", wordLibrary.getWordType());
            return map;
        }
        return Collections.emptyMap();
    }

    /**
     * 获取卧底游戏词库
     * @param category 词库分类
     * @param usedWordPairs 已使用的词对
     * @return 词库
     */
    @Override
    public String[] getUndercoverGameWordLibrary(String category, Set<String> usedWordPairs) {
        ThrowUtils.throwIf(StringUtils.isBlank(category), ErrorCode.PARAMS_ERROR, "词库分类为空");
        // 随机查询一个你画我猜词
        QueryWrapper<WordLibrary> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq("category", category);
        queryWrapper.eq("wordType", getWordTypeRandom(category));
        queryWrapper.notIn(CollUtil.isNotEmpty(usedWordPairs), "word", usedWordPairs);
        queryWrapper.last("ORDER BY RAND() LIMIT 2");
        List<WordLibrary> wordLibraries = wordLibraryMapper.selectList(queryWrapper);
        String[] result = new String[2];
        if (CollUtil.isNotEmpty(wordLibraries) && wordLibraries.size() >= 2) {
            result[0] = wordLibraries.get(0).getWord();
            result[1] = wordLibraries.get(1).getWord();
        }
        return result;
    }

    /**
     * 获取一个随机的词语类型
     * @param category 词语分类
     * @return 词语类型
     */
    private String getWordTypeRandom(String category){
        // 使用数据库的随机函数获取一个随机的 wordType
        QueryWrapper<WordLibrary> queryWrapper = new QueryWrapper<>();
        queryWrapper.select("wordType");
        queryWrapper.eq("category", category);
        queryWrapper.groupBy("wordType");
        queryWrapper.last("ORDER BY RAND() LIMIT 1");
        WordLibrary wordLibrary = this.getOne(queryWrapper);
        ThrowUtils.throwIf(wordLibrary == null, ErrorCode.SYSTEM_ERROR, "获取词语类型失败");
        return wordLibrary.getWordType();
    }


    @Override
    public QueryWrapper<WordLibrary> getQueryWrapper(WordLibraryQueryRequest wordLibraryQueryRequest) {
        ThrowUtils.throwIf(wordLibraryQueryRequest == null, ErrorCode.PARAMS_ERROR, "请求参数为空");

        Long id = wordLibraryQueryRequest.getId();
        String word = wordLibraryQueryRequest.getWord();
        String category = wordLibraryQueryRequest.getCategory();
        String wordType = wordLibraryQueryRequest.getWordType();
        String sortField = wordLibraryQueryRequest.getSortField();
        String sortOrder = wordLibraryQueryRequest.getSortOrder();

        QueryWrapper<WordLibrary> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq(id != null, "id", id);
        queryWrapper.like(StringUtils.isNotBlank(word), "word", word);
        queryWrapper.eq(StringUtils.isNotBlank(category), "category", category);
        queryWrapper.like(StringUtils.isNotBlank(wordType), "wordType", wordType);
        queryWrapper.orderBy(SqlUtils.validSortField(sortField),
                sortOrder.equals(CommonConstant.SORT_ORDER_ASC), sortField);
        return queryWrapper;
    }

    /**
     * 判断词库项是否存在
     * @param word 词语名称
     * @param category 词语分类
     * @param id id
     * @return 是否存在
     */
    @Override
    public Boolean existWordLibrary(String word, String category, Long id) {
        ThrowUtils.throwIf(StringUtils.isBlank(word), ErrorCode.PARAMS_ERROR, "词语名称为空");
        ThrowUtils.throwIf(StringUtils.isBlank(category), ErrorCode.PARAMS_ERROR, "词语分类为空");
        QueryWrapper<WordLibrary> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq("word", word);
        queryWrapper.eq("category", category);
        queryWrapper.ne(id != null, "id", id);
        return wordLibraryMapper.exists(queryWrapper);
    }
}





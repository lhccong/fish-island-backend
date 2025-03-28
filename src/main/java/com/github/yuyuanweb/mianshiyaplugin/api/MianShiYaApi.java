package com.github.yuyuanweb.mianshiyaplugin.api;

import com.github.yuyuanweb.mianshiyaplugin.model.common.BaseResponse;
import com.github.yuyuanweb.mianshiyaplugin.model.common.Page;
import com.github.yuyuanweb.mianshiyaplugin.model.common.PageRequest;
import com.github.yuyuanweb.mianshiyaplugin.model.dto.*;
import com.github.yuyuanweb.mianshiyaplugin.model.response.*;
import retrofit2.Call;
import retrofit2.http.*;

import java.util.List;

/**
 * 面试鸭接口
 *
 * @author pine
 */
public interface MianShiYaApi {

    /**
     * 获取登录用户信息
     */
    @GET("user/get/login")
    Call<BaseResponse<User>> getLoginUser();

    /**
     * 获取题库列表
     */
    @POST("questionBankCategory/list_questionBank")
    Call<BaseResponse<Page<QuestionBank>>> getQuestionBankList(
            @Body QuestionBankCategoryBankQueryRequest queryRequest
    );

    /**
     * 获取题目列表
     */
    @POST("question/list/page/vo")
    Call<BaseResponse<Page<Question>>> getQuestionList(
            @Body QuestionQueryRequest queryRequest
    );

    /**
     * 搜索题目列表
     */
    @POST("question/search")
    Call<BaseResponse<Page<Question>>> searchQuestionList(
            @Body QuestionQueryRequest queryRequest
    );

    @POST("question_bank/list_question")
    Call<BaseResponse<Page<Question>>> listQuestionByQuestionBank(
            @Body QuestionQueryRequest queryRequest
    );


    /**
     * 获取刷题信息
     *
     * @param questionBankId 题库 id
     * @param questionId    题目 id
     * @return {@link Call }<{@link BaseResponse }<{@link DoQuestionInfoVO }>>
     */
    @GET("question_bank/do_question_info")
    Call<BaseResponse<DoQuestionInfoVO>> getDoQuestionInfo(
            @Query("questionBankId") long questionBankId,
            @Query("questionId") long questionId
    );

    /**
     * 获取回答列表
     */
    @POST("question_answer/list/by_question")
    Call<BaseResponse<Page<QuestionAnswer>>> listQuestionAnswerByQuestionId(
            @Body QuestionAnswerQueryRequest queryRequest
    );

    /**
     * 获取题库分类列表
     */
    @POST("questionBankCategory/list")
    Call<BaseResponse<List<QuestionBankCategory>>> listQuestionBankCategory(
            @Body PageRequest pageRequest
    );

    /**
     * 获取题库列表
     */
    @POST("question_bank/list/page/vo")
    Call<BaseResponse<Page<QuestionBank>>> listQuestionBankVoByPage(
            @Body QuestionBankQueryRequest queryRequest
    );

    /**
     * 获取标签分类列表
     */
    @POST("tagCategory/list")
    Call<BaseResponse<List<TagGroup>>> listTagCategory(
            @Body TagCategoryQueryRequest queryRequest
    );

    /**
     * 用户退出登录
     */
    @POST("user/logout")
    Call<BaseResponse<Boolean>> userLogout();

}

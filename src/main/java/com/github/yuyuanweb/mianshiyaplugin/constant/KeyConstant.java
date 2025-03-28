package com.github.yuyuanweb.mianshiyaplugin.constant;

import com.github.yuyuanweb.mianshiyaplugin.model.enums.WebTypeEnum;
import com.intellij.openapi.util.Key;

/**
 * key 常量
 *
 * @author pine
 */
public interface KeyConstant {

    String QUESTION_ID = "questionId";

    String QUESTION_BANK_ID = "QuestionBankId";

    String WEB_TYPE = "webType";

    Key<Long> QUESTION_ID_KEY = new Key<>(KeyConstant.QUESTION_ID);

    Key<WebTypeEnum> WEB_TYPE_KEY = new Key<>(KeyConstant.WEB_TYPE);

    Key<Long> QUESTION_BANK_ID_KEY = new Key<>(KeyConstant.QUESTION_BANK_ID);

    String QUESTION_BANK_ZH = "热榜类型";
    String QUESTION_BANK = "HOT_NEWS_TYPE";

    String QUESTION_ZH = "热榜列表";
    String QUESTION = "HOT_NEWS_LIST";

    String WEB_ZH = "网页端";
    String WEB = "WEB";

    String HELP_ZH = "帮助";
    String HELP = "HELP";

    String LOGIN_ZH = "登录";
    String LOGIN = "LOGIN";

    String LOGOUT_ZH = "注销";
    String LOGOUT = "LOGOUT";

    String VIP_ZH = "会员";
    String VIP = "VIP";

    String PLUGIN_NAME = "今日热榜";

    String ACTION_BAR = "ToolWindowToolbar";

    String EDITOR_FILE_POSTFIX = "msy";

    String EDITOR_FILE_POSTFIX_CONTENT = "msyc";

}

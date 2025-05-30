package com.cong.fishisland.model.ws.request;

import com.cong.fishisland.model.enums.WSReqTypeEnum;
import lombok.Data;

/**
 * Description: websocket前端请求体
 * Date: 2023-03-19
 * @author cong
 */
@Data
public class WSBaseReq {
    /**
     * 请求类型
     *
     * @see WSReqTypeEnum
     */
    private Integer type;

    /**
     * 接收的用户id
     */
    private String userId;

    /**
     * 每个请求包具体的数据，类型不同结果不同
     */
    private String data;
}

package com.cong.fishisland.model.dto.props;

import com.cong.fishisland.common.PageRequest;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.io.Serializable;

/**
 * 道具查询请求
 *
 * @author cong
 */
@EqualsAndHashCode(callSuper = true)
@Data
public class PropsQueryRequest extends PageRequest implements Serializable {

    /**
     * 道具名称
     */
    private String name;

    /**
     * 道具类型 1-摸鱼会员月卡 2-摸鱼称号
     */
    private String type;

    private static final long serialVersionUID = 1L;
} 
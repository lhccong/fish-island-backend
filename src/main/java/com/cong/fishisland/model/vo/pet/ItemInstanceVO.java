package com.cong.fishisland.model.vo.pet;

import com.baomidou.mybatisplus.annotation.TableField;
import com.cong.fishisland.model.entity.pet.ItemInstances;
import com.cong.fishisland.model.entity.pet.ItemTemplates;
import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Data;
import org.springframework.beans.BeanUtils;

import java.io.Serializable;
import java.util.Date;
import java.util.Map;

/**
 * 物品实例视图对象
 * （对应 item_instances 表，聚合模板信息）
 *
 * @author Shing
 */
@Data
public class ItemInstanceVO implements Serializable {
    /**
     * 实例ID
     */
    private Long id;

    /**
     * 物品模板ID
     */
    private Long templateId;

    /**
     * 持有者用户ID
     */
    @TableField(value = "ownerUserId")
    private Long ownerUserId;

    /**
     * 物品数量
     */
    private Integer quantity;

    /**
     * 是否绑定（1-绑定，0-未绑定）
     */
    private Integer bound;

    /**
     * 耐久度（部分装备适用）
     */
    private Integer durability;

    /**
     * 强化等级
     */
    private Integer enhanceLevel;

    /**
     * 扩展信息（JSON）
     */
    private transient Map<String, Object> extraData;

    /**
     * 创建时间
     */
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
    private Date createTime;

    /**
     * 更新时间
     */
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
    private Date updateTime;

    /**
     * 模板信息
     */
    private ItemTemplateVO template;

    /**
     * VO 转 实例对象
     */
    public static ItemInstances voToObj(ItemInstanceVO itemInstanceVO) {
        if (itemInstanceVO == null) {
            return null;
        }
        ItemInstances itemInstances = new ItemInstances();
        BeanUtils.copyProperties(itemInstanceVO, itemInstances);
        return itemInstances;
    }

    /**
     * 物品实例 + 模板 转 VO
     */
    public static ItemInstanceVO objToVo(ItemInstances itemInstance, ItemTemplates itemTemplates) {
        if (itemInstance == null) {
            return null;
        }
        ItemInstanceVO itemInstanceVO = new ItemInstanceVO();
        BeanUtils.copyProperties(itemInstance, itemInstanceVO);

        ItemTemplateVO templateVO = new ItemTemplateVO();
        BeanUtils.copyProperties(itemTemplates, templateVO);

        itemInstanceVO.setTemplate(templateVO);
        return itemInstanceVO;
    }
}
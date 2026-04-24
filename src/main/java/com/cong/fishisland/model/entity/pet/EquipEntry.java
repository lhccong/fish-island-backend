package com.cong.fishisland.model.entity.pet;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;

/**
 * 装备词条 JSON 结构
 * 对应数据库 entry1~entry4 字段
 *
 * @author cong
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class EquipEntry implements Serializable {

    /**
     * 词条属性，对应 EntryAttrEnum.value
     * 例如：attack、critRate、antiCrit
     */
    private String attr;

    /**
     * 词条等级，对应 EntryGradeEnum.level
     * 1-白 2-蓝 3-紫 4-金 5-红
     */
    private Integer grade;

    /**
     * 词条属性数值（根据等级区间随机生成，百分比类属性精度为 0.01%）
     * 例如：攻击力 120、暴击率 3.50（表示 3.50%）
     */
    private Double value;

    /**
     * 是否锁定（锁定后刷新词条额外消耗 50 积分/条）
     */
    private Boolean locked;

    private static final long serialVersionUID = 1L;
}

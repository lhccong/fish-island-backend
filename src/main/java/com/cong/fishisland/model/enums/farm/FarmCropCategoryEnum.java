package com.cong.fishisland.model.enums.farm;

import lombok.Getter;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

/**
 * 农场作物分类
 */
@Getter
public enum FarmCropCategoryEnum {

    GRAIN("grain", "粮食"),
    VEGETABLE("vegetable", "蔬菜"),
    FRUIT("fruit", "水果"),
    FLOWER("flower", "花卉");

    private final String value;

    private final String label;

    FarmCropCategoryEnum(String value, String label) {
        this.value = value;
        this.label = label;
    }

    public static FarmCropCategoryEnum fromValue(String value) {
        if (value == null) {
            return null;
        }
        for (FarmCropCategoryEnum category : values()) {
            if (category.value.equals(value)) {
                return category;
            }
        }
        return null;
    }

    public static List<FarmCropCategoryEnum> all() {
        return Arrays.asList(values());
    }

    public static List<String> allValues() {
        return all().stream().map(FarmCropCategoryEnum::getValue).collect(Collectors.toList());
    }
}

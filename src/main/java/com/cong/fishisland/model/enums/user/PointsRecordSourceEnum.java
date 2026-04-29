package com.cong.fishisland.model.enums.user;

import lombok.Getter;

/**
 * 积分记录来源类型枚举
 */
@Getter
public enum PointsRecordSourceEnum {

    SIGN_IN("sign_in", "每日签到"),
    SPEAK("speak", "房间发言"),
    RED_PACKET_SEND("red_packet_send", "发送红包"),
    RED_PACKET_GRAB("red_packet_grab", "抢红包"),
    PET_FEED("pet_feed", "宠物喂食"),
    PET_PAT("pet_pat", "宠物抚摸"),
    PET_RENAME("pet_rename", "宠物改名"),
    TURNTABLE("turntable", "转盘抽奖"),
    SKIN_EXCHANGE("skin_exchange", "兑换皮肤"),
    PROPS_PURCHASE("props_purchase", "购买道具"),
    ITEM_DECOMPOSE("item_decompose", "物品分解"),
    PET_DAILY("pet_daily", "宠物每日产出"),
    PRIZE_CONVERT("prize_convert", "奖品转积分"),
    ADMIN("admin", "管理员操作"),
    AVATAR_FRAME_EXCHANGE("avatar_frame_exchange", "兑换头像框"),
    VOTE_CREATE("vote_create", "创建投票"),
    TOWER_CLIMB("tower_climb", "爬塔奖励"),
    FISH_BATTLE("fish_battle", "摸鱼大乱斗"),
    OTHER("other", "其他");

    private final String value;
    private final String text;

    PointsRecordSourceEnum(String value, String text) {
        this.value = value;
        this.text = text;
    }

    /**
     * 根据值获取枚举
     */
    public static PointsRecordSourceEnum getEnumByValue(String value) {
        if (value == null) {
            return null;
        }
        for (PointsRecordSourceEnum sourceEnum : PointsRecordSourceEnum.values()) {
            if (sourceEnum.value.equals(value)) {
                return sourceEnum;
            }
        }
        return null;
    }
}

package com.cong.fishisland.game.model.dto.response;

import com.cong.fishisland.game.enums.GameTypeEnum;
import com.cong.fishisland.game.enums.RoomStateEnum;
import lombok.Builder;
import lombok.Data;

import java.util.List;

/**
 * 房间列表响应
 *
 * @author cong
 */
@Data
@Builder
public class RoomListResp {

    /**
     * 房间列表
     */
    private List<RoomInfoResp> rooms;

    /**
     * 总数
     */
    private Integer total;

    /**
     * 房间限制信息
     */
    private RoomRestrictionInfo restriction;

    /**
     * 房间限制信息
     */
    @Data
    @Builder
    public static class RoomRestrictionInfo {
        private String roomId;
        private GameTypeEnum gameType;
        private RoomStateEnum state;
        private String reason;
    }
}

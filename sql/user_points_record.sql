-- 用户积分记录表
CREATE TABLE IF NOT EXISTS user_points_record
(
    id              BIGINT AUTO_INCREMENT COMMENT '记录ID' PRIMARY KEY,
    user_id         BIGINT                               NOT NULL COMMENT '用户ID',
    change_type     TINYINT      DEFAULT 1             NOT NULL COMMENT '变动类型：1-增加，2-扣除',
    change_points   INT                                  NOT NULL COMMENT '变动积分数量（正数）',
    before_points   INT                                  NOT NULL COMMENT '变动前总积分',
    after_points    INT                                  NOT NULL COMMENT '变动后总积分',
    before_used_points INT       DEFAULT 0             NOT NULL COMMENT '变动前已用积分',
    after_used_points  INT       DEFAULT 0             NOT NULL COMMENT '变动后已用积分',
    source_type     VARCHAR(50)                          NOT NULL COMMENT '来源类型：sign_in-签到, speak-发言, red_packet-红包, pet_feed-宠物喂食, pet_pat-宠物抚摸, pet_rename-宠物改名, turntable-转盘抽奖, skin_exchange-皮肤兑换, props_purchase-道具购买, item_decompose-物品分解, pet_daily-宠物每日产出, prize_convert-奖品转积分, admin-管理员操作, other-其他',
    source_id       VARCHAR(64)  DEFAULT NULL COMMENT '来源ID（如红包ID、转盘ID等）',
    description     VARCHAR(256) DEFAULT NULL COMMENT '描述/备注',
    create_time     DATETIME     DEFAULT CURRENT_TIMESTAMP NOT NULL COMMENT '创建时间',
    INDEX idx_user_id (user_id),
    INDEX idx_source_type (source_type),
    INDEX idx_create_time (create_time)
) COMMENT '用户积分记录表' COLLATE = utf8mb4_unicode_ci;

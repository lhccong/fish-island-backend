-- 宠物卡包正式玩法。先执行 20261122_pet_card_catalog.sql，再执行本迁移一次，最后发布卡包接口。
-- 已有卡池先按默认价 100 积分初始化，再将海岛初遇调整为 50 积分。管理员可按卡池修改单价。
ALTER TABLE pet_card_pool ADD COLUMN unit_price INT NOT NULL DEFAULT 100 COMMENT '单包购买价格（积分），同款十包合计按九倍单价计算';
UPDATE pet_card_pool SET unit_price = 50 WHERE id = 'island';
ALTER TABLE pet_card_pool ADD CONSTRAINT chk_pet_card_pool_price CHECK (unit_price BETWEEN 1 AND 1000000);

-- 同一账号的领取、购买、开包和升星事务先锁定此行，再修改各项进度。
CREATE TABLE IF NOT EXISTS pet_card_player (
    user_id BIGINT NOT NULL PRIMARY KEY COMMENT '账号唯一编号，每个账号一条抽卡进度',
    claim_day DATE NULL COMMENT '最近一次免费领取的北京时间日期，未领取时为空',
    claims INT NOT NULL DEFAULT 0 COMMENT '领取日已领的海岛免费包数，每日最多两包',
    opened BIGINT NOT NULL DEFAULT 0 COMMENT '所有卡池累计开包数，每包记一点抽卡经验，供服务端计算等级',
    pending_open BIGINT NULL COMMENT '待确认的开包记录编号，用于刷新后继续翻牌，确认后清空'
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='账号抽卡进度与海岛每日免费领取用量';

CREATE TABLE IF NOT EXISTS pet_card_stock (
    user_id BIGINT NOT NULL COMMENT '库存所属账号编号',
    pool_id VARCHAR(48) CHARACTER SET ascii COLLATE ascii_bin NOT NULL COMMENT '库存所属卡池的稳定编号',
    quantity INT NOT NULL DEFAULT 0 COMMENT '领取和购买后尚未拆开的卡包数量，跨日保留',
    opened BIGINT NOT NULL DEFAULT 0 COMMENT '该账号在此卡池的累计开包数，海岛初遇用此值判断首包保底',
    PRIMARY KEY (user_id, pool_id),
    CONSTRAINT fk_pet_card_stock_pool FOREIGN KEY (pool_id) REFERENCES pet_card_pool (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='账号按卡池统计的待拆卡包库存与已开包数';

CREATE TABLE IF NOT EXISTS pet_card_collection (
    user_id BIGINT NOT NULL COMMENT '收藏所属账号编号',
    card_id VARCHAR(48) CHARACTER SET ascii COLLATE ascii_bin NOT NULL COMMENT '卡牌稳定编号，同一账号每种卡牌收藏一条',
    stars INT NOT NULL DEFAULT 1 COMMENT '已收藏卡牌的星级，初始一星，最高五星',
    duplicates BIGINT NOT NULL DEFAULT 0 COMMENT '尚未用于升星的同名重复卡数量',
    showcased BOOLEAN NOT NULL DEFAULT FALSE COMMENT '卡牌是否已上架账号展示柜',
    PRIMARY KEY (user_id, card_id),
    CONSTRAINT fk_pet_card_collection_definition FOREIGN KEY (card_id) REFERENCES pet_card_definition (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='账号宠物卡牌收藏、星级、升星材料与展示状态';

CREATE TABLE IF NOT EXISTS pet_card_opening (
    id BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY COMMENT '开包记录编号，也用作历史倒序分页游标',
    user_id BIGINT NOT NULL COMMENT '开包账号编号',
    pool_id VARCHAR(48) CHARACTER SET ascii COLLATE ascii_bin NOT NULL COMMENT '本次开启的卡池编号',
    pool_name VARCHAR(80) NOT NULL COMMENT '开包时的卡池名称快照',
    opened_at DATETIME(3) NOT NULL COMMENT '服务端结算开包的 UTC 时间，精确到毫秒',
    cards JSON NOT NULL COMMENT '按抽取顺序保存五张卡牌的编号、当时名称和重复标记',
    KEY idx_pet_card_opening_user (user_id, id) COMMENT '按账号和记录编号倒序滚动读取开包历史',
    CONSTRAINT fk_pet_card_opening_pool FOREIGN KEY (pool_id) REFERENCES pet_card_pool (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='账号每次开包的卡池、时间和抽取结果快照';

CREATE TABLE IF NOT EXISTS pet_card_operation (
    user_id BIGINT NOT NULL COMMENT '提交操作的账号编号',
    request_id CHAR(36) CHARACTER SET ascii COLLATE ascii_bin NOT NULL COMMENT '客户端生成的操作 UUID，同一操作重试时复用',
    command JSON NOT NULL COMMENT '成功操作的参数及批量结算汇总，用于校验重试并回传结果',
    PRIMARY KEY (user_id, request_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='卡包成功操作的幂等记录，与业务结果一并提交';

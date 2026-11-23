-- 先创建图集索引和卡牌，再创建引用卡牌的卡池及入池关系。
-- 图片文件保存在 MinIO，此迁移只建立卡池目录结构。
CREATE TABLE IF NOT EXISTS pet_card_atlas (
    id VARCHAR(80) CHARACTER SET ascii COLLATE ascii_bin NOT NULL PRIMARY KEY COMMENT '图集对象键后缀，内容 SHA-256 与扩展名',
    name VARCHAR(80) NOT NULL COMMENT '管理页显示的图集名称',
    portrait BOOLEAN NOT NULL COMMENT '1 表示 4:5 竖格，0 表示方格',
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '图集首次登记时间'
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='宠物卡牌十格图集索引，原图保存在 MinIO';

CREATE TABLE IF NOT EXISTS pet_card_definition (
    id VARCHAR(48) CHARACTER SET ascii COLLATE ascii_bin NOT NULL PRIMARY KEY COMMENT '卡牌稳定编号，供玩家收藏和卡池引用',
    name VARCHAR(80) NOT NULL COMMENT '卡牌显示名称',
    grade VARCHAR(16) NOT NULL COMMENT '品质编号，决定展示和卡池品质分组',
    stats JSON NOT NULL COMMENT '宠物基础属性，生命必填，攻击、防御和速度按卡牌配置',
    battle JSON NOT NULL COMMENT '结构化对战动作或技能，含触发条件、效果类型和数值',
    art_key VARCHAR(80) CHARACTER SET ascii COLLATE ascii_bin NOT NULL COMMENT '引用一张可被多张卡共用的图集',
    art_tile TINYINT NOT NULL COMMENT '图集格位 0 至 9，先按行再按列，从左上角开始',
    sort_order INT NOT NULL DEFAULT 0 COMMENT '卡牌显示排序，升序',
    version INT NOT NULL DEFAULT 1 COMMENT '配置版本，管理端用来检测并发修改',
    KEY idx_pet_card_order (sort_order, id),
    CONSTRAINT fk_pet_card_definition_atlas FOREIGN KEY (art_key) REFERENCES pet_card_atlas (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='宠物卡牌定义';

CREATE TABLE IF NOT EXISTS pet_card_pool (
    id VARCHAR(48) CHARACTER SET ascii COLLATE ascii_bin NOT NULL PRIMARY KEY COMMENT '卡池稳定编号，供待开卡包和入池关系引用',
    name VARCHAR(80) NOT NULL COMMENT '卡池显示名称',
    weights JSON NOT NULL COMMENT '五档品质的相对抽取权重，权重为 0 的品质不参与普通抽取',
    cover_id VARCHAR(48) CHARACTER SET ascii COLLATE ascii_bin NULL COMMENT '可选封面卡牌，须属于本卡池',
    sort_order INT NOT NULL DEFAULT 0 COMMENT '卡池显示排序，升序',
    version INT NOT NULL DEFAULT 1 COMMENT '配置版本，管理端用来检测并发修改',
    CONSTRAINT fk_pet_card_pool_cover FOREIGN KEY (cover_id) REFERENCES pet_card_definition (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='宠物卡池及品质权重配置';

CREATE TABLE IF NOT EXISTS pet_card_pool_entry (
    pool_id VARCHAR(48) CHARACTER SET ascii COLLATE ascii_bin NOT NULL COMMENT '接收卡牌的卡池编号',
    card_id VARCHAR(48) CHARACTER SET ascii COLLATE ascii_bin NOT NULL COMMENT '加入该卡池的卡牌编号',
    PRIMARY KEY (pool_id, card_id),
    KEY idx_pet_card_entry_card (card_id, pool_id),
    CONSTRAINT fk_pet_card_entry_pool FOREIGN KEY (pool_id) REFERENCES pet_card_pool (id),
    CONSTRAINT fk_pet_card_entry_card FOREIGN KEY (card_id) REFERENCES pet_card_definition (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='宠物卡池与卡牌多对多关系';

CREATE TABLE IF NOT EXISTS pet_card_catalog_migration (
    id VARCHAR(48) CHARACTER SET ascii COLLATE ascii_bin NOT NULL PRIMARY KEY COMMENT '已导入的种子版本编号',
    applied_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '种子导入时间，与目录数据同一事务提交'
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='宠物卡池种子迁移记录';

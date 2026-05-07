-- 作物模板表
create table if not exists crop_templates
(
    id              bigint auto_increment comment '作物ID' primary key,
    name            varchar(100)                       not null comment '作物名称',
    icon            varchar(255)                       null comment '作物图标URL',
    level           int      default 1                 not null comment '解锁所需农场等级',
    buyPrice        int      default 10                not null comment '种子购买价格（积分）',
    sellPrice       int      default 20                not null comment '果实出售价格（积分）',
    growTime        int      default 60                not null comment '成熟时间（分钟）',
    exp             int      default 5                 not null comment '收获获得经验值',
    output          int      default 1                 not null comment '收获产量',
    witherTime      int      default 1440              null comment '枯萎时间（分钟，null表示不枯萎）',
    description     varchar(512)                       null comment '作物描述',
    sort            int      default 0                 not null comment '排序',
    createTime      datetime default CURRENT_TIMESTAMP not null comment '创建时间',
    updateTime      datetime default CURRENT_TIMESTAMP not null on update CURRENT_TIMESTAMP comment '更新时间',
    isDelete        tinyint  default 0                 not null comment '是否删除',
    index idx_level (level),
    index idx_sort (sort)
    ) comment '作物模板表' collate = utf8mb4_unicode_ci;


-- 用户土地表
create table if not exists user_lands
(
    id              bigint auto_increment comment '土地ID' primary key,
    userId          bigint                             not null comment '用户ID',
    landIndex       int                                not null comment '土地编号（从1开始）',
    level           int      default 1                 not null comment '土地等级',
    status          tinyint  default 0                 not null comment '状态：0-空闲，1-已种植，2-可收获，3-已枯萎',
    cropId          bigint                             null comment '当前种植的作物ID',
    plantTime       datetime                           null comment '播种时间',
    harvestTime     datetime                           null comment '预计收获时间',
    witherTime      datetime                           null comment '枯萎时间',
    remainOutput    int      default 0                 not null comment '剩余可偷次数/产量',
    totalOutput     int      default 0                 not null comment '总产量',
    extendData      json                               null comment '扩展数据（JSON格式，如加速道具等）',
    createTime      datetime default CURRENT_TIMESTAMP not null comment '创建时间',
    updateTime      datetime default CURRENT_TIMESTAMP not null on update CURRENT_TIMESTAMP comment '更新时间',
    unique key uk_user_land (userId, landIndex),
    index idx_userId (userId),
    index idx_status (status),
    index idx_harvest_time (harvestTime)
    ) comment '用户土地表' collate = utf8mb4_unicode_ci;

-- 用户农场背包表
create table if not exists user_farm_inventory
(
    id              bigint auto_increment comment '背包ID' primary key,
    userId          bigint                             not null comment '用户ID',
    itemType        varchar(20)                        not null comment '物品类型：seed-种子，fruit-果实',
    cropId          bigint                             not null comment '作物ID',
    quantity        int      default 0                 not null comment '数量',
    createTime      datetime default CURRENT_TIMESTAMP not null comment '创建时间',
    updateTime      datetime default CURRENT_TIMESTAMP not null on update CURRENT_TIMESTAMP comment '更新时间',
    unique key uk_user_item (userId, itemType, cropId),
    index idx_userId (userId)
    ) comment '用户农场背包表' collate = utf8mb4_unicode_ci;

-- 偷菜记录表
create table if not exists farm_steal_records
(
    id              bigint auto_increment comment '记录ID' primary key,
    thiefUserId     bigint                             not null comment '偷菜者用户ID',
    ownerUserId     bigint                             not null comment '土地所有者用户ID',
    landId          bigint                             not null comment '土地ID',
    cropId          bigint                             not null comment '作物ID',
    stealQuantity   int      default 1                 not null comment '偷取数量',
    stealPoints     int      default 0                 not null comment '偷取获得的积分',
    createTime      datetime default CURRENT_TIMESTAMP not null comment '偷取时间',
    index idx_thief (thiefUserId),
    index idx_owner (ownerUserId),
    index idx_create_time (createTime)
    ) comment '偷菜记录表' collate = utf8mb4_unicode_ci;

-- 农场配置表
create table if not exists farm_config
(
    id              bigint auto_increment comment '配置ID' primary key,
    configKey       varchar(100)                       not null comment '配置键',
    configValue     varchar(512)                       not null comment '配置值',
    description     varchar(256)                       null comment '配置描述',
    createTime      datetime default CURRENT_TIMESTAMP not null comment '创建时间',
    updateTime      datetime default CURRENT_TIMESTAMP not null on update CURRENT_TIMESTAMP comment '更新时间',
    unique key uk_config_key (configKey)
    ) comment '农场配置表' collate = utf8mb4_unicode_ci;

-- 初始化配置数据
insert into farm_config (configKey, configValue, description) values
                                                                  ('max_land_count', '20', '最大土地数量'),
                                                                  ('initial_land_count', '6', '初始土地数量'),
                                                                  ('steal_rate', '0.3', '偷取比例（30%）'),
                                                                  ('max_steal_times_per_crop', '5', '单个作物最多被偷次数'),
                                                                  ('land_upgrade_base_price', '100', '土地升级基础价格（积分）'),
                                                                  ('protect_card_duration', '120', '保护卡持续时间（分钟）');
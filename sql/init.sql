#数据库初始化
# @author <a href="https://github.com/lhccong">程序员聪</a>
#

-- 创建库
create database if not exists fish;

-- 切换库
use fish;

-- 用户表
create table if not exists user
(
    id           bigint auto_increment comment 'id' primary key,
    userAccount  varchar(256)                           not null comment '账号',
    userPassword varchar(512)                           not null comment '密码',
    unionId      varchar(256)                           null comment '微信开放平台id',
    mpOpenId     varchar(256)                           null comment '公众号openId',
    userName     varchar(256)                           null comment '用户昵称',
    userAvatar   varchar(1024)                          null comment '用户头像',
    userProfile  varchar(512)                           null comment '用户简介',
    userRole     varchar(256) default 'user'            not null comment '用户角色：user/admin/ban',
    createTime   datetime     default CURRENT_TIMESTAMP not null comment '创建时间',
    updateTime   datetime     default CURRENT_TIMESTAMP not null on update CURRENT_TIMESTAMP comment '更新时间',
    isDelete     tinyint      default 0                 not null comment '是否删除',
    index idx_unionId (unionId)
    ) comment '用户' collate = utf8mb4_unicode_ci;
ALTER TABLE user
    ADD COLUMN email VARCHAR(256) NULL COMMENT '邮箱' after mpOpenId,
    ADD UNIQUE INDEX idx_email (email);

ALTER TABLE user
    ADD COLUMN avatarFramerUrl  VARCHAR(256) NULL COMMENT '用户头像框地址' after userAvatar,
    ADD COLUMN avatarFramerList VARCHAR(256) NULL COMMENT '用户头像框 ID Json 列表' after avatarFramerUrl;
ALTER TABLE user
    ADD COLUMN titleId int NULL default 0 COMMENT '用户称号 ID 默认为 0 等级称号 -1 为管理员称号' after userAvatar;
ALTER TABLE user
    ADD COLUMN titleIdList VARCHAR(256) NULL COMMENT '用户称号 ID Json 列表' after titleId;

-- 用户称号表
create table if not exists user_title
(
    titleId    BIGINT auto_increment comment '称号 ID' PRIMARY KEY,
    name       VARCHAR(256) comment '称号名称',
    createTime datetime default CURRENT_TIMESTAMP not null comment '创建时间',
    updateTime datetime default CURRENT_TIMESTAMP not null on update CURRENT_TIMESTAMP comment '更新时间',
    isDelete   tinyint  default 0                 not null comment '是否删除'
    ) comment '用户称号' collate = utf8mb4_unicode_ci;
ALTER TABLE `user_title`
    ADD COLUMN `titleImg` VARCHAR(256) NULL COMMENT '称号图标' after titleId;


-- 头像框表
create table if not exists avatar_frame
(
    frameId    BIGINT auto_increment comment '头像框 ID' PRIMARY KEY,
    url        VARCHAR(256) comment '头像框名称',
    name       VARCHAR(256) comment '头像框名称',
    points     INT      DEFAULT 1 comment '头像框所需兑换积分',
    createTime datetime default CURRENT_TIMESTAMP not null comment '创建时间',
    updateTime datetime default CURRENT_TIMESTAMP not null on update CURRENT_TIMESTAMP comment '更新时间',
    isDelete   tinyint  default 0                 not null comment '是否删除'
    ) comment '头像框' collate = utf8mb4_unicode_ci;

-- 用户积分表
create table if not exists user_points
(
    userId         BIGINT comment '用户 ID' PRIMARY KEY,
    points         INT      DEFAULT 100 comment '积分',     -- 初始100积分
    usedPoints     INT      DEFAULT 0 comment '已使用积分', -- 初始100积分
    level          INT      DEFAULT 1 comment '用户等级',   -- 用户等级（积分除以一百等于等级）
    lastSignInDate datetime comment '最后签到时间',         -- 最后签到时间
    createTime     datetime default CURRENT_TIMESTAMP not null comment '创建时间',
    updateTime     datetime default CURRENT_TIMESTAMP not null on update CURRENT_TIMESTAMP comment '更新时间',
    isDelete       tinyint  default 0                 not null comment '是否删除'
) comment '用户积分' collate = utf8mb4_unicode_ci;

-- 标签表
create table if not exists tags
(
    id         bigint auto_increment comment '标签id' primary key,
    tagsName   varchar(256)                       null comment '标签名',
    type       tinyint  default 0                 null comment '类型（0 官方创建，1 用户自定义）',
    icon       varchar(256)                       null comment '图标',
    color      varchar(20)                        null comment '颜色',
    sort       int      default 0                 not null comment '排序',
    createTime datetime default CURRENT_TIMESTAMP not null comment '创建时间',
    updateTime datetime default CURRENT_TIMESTAMP not null on update CURRENT_TIMESTAMP comment '更新时间',
    isDelete   tinyint  default 0                 not null comment '是否删除',
    index idx_tagsId (id)
    ) comment '标签表' collate = utf8mb4_general_ci;

-- 评论表
create table if not exists comment
(
    id         bigint auto_increment comment '评论id' primary key,
    postId     bigint                             not null comment '所属帖子id',
    userId     bigint                             not null comment '评论者用户id',
    rootId     bigint   default null comment '根评论id',
    parentId   bigint   default null comment '父评论id（为NULL则是顶级评论）',
    content    text                               not null comment '评论内容',
    thumbNum   int      default 0                 not null comment '点赞数',
    createTime datetime default CURRENT_TIMESTAMP not null comment '创建时间',
    updateTime datetime default CURRENT_TIMESTAMP not null on update CURRENT_TIMESTAMP comment '更新时间',
    isDelete   tinyint  default 0                 not null comment '是否删除',
    index idx_postId (postId),
    index idx_userId (userId),
    index idx_parentId (parentId)
    ) COMMENT '评论表' COLLATE = utf8mb4_unicode_ci;

-- 评论点赞表（硬删除）
create table if not exists comment_thumb
(
    id         bigint auto_increment comment '评论点赞id' primary key,
    commentId  bigint                             not null comment '评论id',
    userId     bigint                             not null comment '创建用户id',
    createTime datetime default CURRENT_TIMESTAMP not null comment '创建时间',
    updateTime datetime default CURRENT_TIMESTAMP not null on update CURRENT_TIMESTAMP comment '更新时间',
    index idx_postId (commentId),
    index idx_userId (userId)
    ) comment '评论点赞表';

-- 帖子表
create table if not exists post
(
    id         bigint auto_increment comment '帖子id' primary key,
    title      varchar(512)                       null comment '标题',
    content    text                               null comment '内容',
    tags       varchar(1024)                      null comment '标签列表（json 数组）',
    coverImage varchar(512)                       null comment '封面图片URL',
    thumbNum   int      default 0                 not null comment '点赞数',
    favourNum  int      default 0                 not null comment '收藏数',
    viewNum    int      default 0                 not null comment '浏览量',
    userId     bigint                             not null comment '创建用户id',
    createTime datetime default CURRENT_TIMESTAMP not null comment '创建时间',
    updateTime datetime default CURRENT_TIMESTAMP not null on update CURRENT_TIMESTAMP comment '更新时间',
    isDelete   tinyint  default 0                 not null comment '是否删除',
    isFeatured tinyint  default 0                 not null comment '是否加精（0-普通，1-加精）',
    index idx_userId (userId),
    index idx_featured (isFeatured)
    ) comment '帖子表' collate = utf8mb4_unicode_ci;
-- 修改帖子表，新增总结字段
ALTER TABLE post
    ADD COLUMN summary TEXT NULL COMMENT '总结';

-- 帖子点赞表（硬删除）
create table if not exists post_thumb
(
    id         bigint auto_increment comment '帖子点赞id' primary key,
    postId     bigint                             not null comment '帖子id',
    userId     bigint                             not null comment '创建用户id',
    createTime datetime default CURRENT_TIMESTAMP not null comment '创建时间',
    updateTime datetime default CURRENT_TIMESTAMP not null on update CURRENT_TIMESTAMP comment '更新时间',
    index idx_postId (postId),
    index idx_userId (userId)
    ) comment '帖子点赞表';

-- 帖子收藏表（硬删除）
create table if not exists post_favour
(
    id         bigint auto_increment comment '帖子收藏id' primary key,
    postId     bigint                             not null comment '帖子id',
    userId     bigint                             not null comment '创建用户id',
    createTime datetime default CURRENT_TIMESTAMP not null comment '创建时间',
    updateTime datetime default CURRENT_TIMESTAMP not null on update CURRENT_TIMESTAMP comment '更新时间',
    index idx_postId (postId),
    index idx_userId (userId)
    ) comment '帖子收藏表';

-- 热点表
create table if not exists hot_post
(
    id             bigint auto_increment comment 'id' primary key,
    name           varchar(256)                            null comment '排行榜名称',
    type           varchar(256)                            null comment ' 热点类型',
    typeName       varchar(256)                            null comment ' 热点类型名称',
    iconUrl        varchar(512)                            null comment '图标地址',
    hostJson       mediumtext                              null comment '热点数据（json）',
    category       int                                     null comment '分类',
    updateInterval decimal(7, 2) default 0.50              null comment '更新间隔，以小时为单位',
    sort           int           default 0                 not null comment ' 排序',
    createTime     datetime      default CURRENT_TIMESTAMP not null comment '创建时间',
    updateTime     datetime      default CURRENT_TIMESTAMP not null on update CURRENT_TIMESTAMP comment '更新时间',
    isDelete       tinyint       default 0                 not null comment '是否删除',
    index idx_postId (sort)
    ) comment '热点表' collate = utf8mb4_unicode_ci;

-- 待办表
create table if not exists todo
(
    id         bigint auto_increment comment 'id' primary key,
    userId     bigint                             not null comment '用户 id',
    todoJson   mediumtext                         null comment '待办数据（json）',
    createTime datetime default CURRENT_TIMESTAMP not null comment '创建时间',
    updateTime datetime default CURRENT_TIMESTAMP not null on update CURRENT_TIMESTAMP comment '更新时间',
    isDelete   tinyint  default 0                 not null comment '是否删除'
) comment '待办表' collate = utf8mb4_unicode_ci;

-- 基金持仓表
create table if not exists fund
(
    id         bigint auto_increment comment 'id' primary key,
    userId     bigint                             not null comment '用户 id',
    fundJson   mediumtext                         null comment '基金持仓数据（json数组，包含code、name、shares、cost等字段）',
    createTime datetime default CURRENT_TIMESTAMP not null comment '创建时间',
    updateTime datetime default CURRENT_TIMESTAMP not null on update CURRENT_TIMESTAMP comment '更新时间',
    isDelete   tinyint  default 0                 not null comment '是否删除',
    index idx_userId (userId)
    ) comment '基金持仓表' collate = utf8mb4_unicode_ci;

-- 指数交易记录表
create table if not exists index_trade_record(
    id                 bigint(20)     NOT NULL AUTO_INCREMENT COMMENT '主键',
    userId             bigint(20)     NOT NULL COMMENT '用户ID，关联user表',
    indexCode          varchar(32)    not null comment '指数代码（如：sh000001-上证指数，sz399001-深证成指）',
    tradeType          tinyint(4)     NOT NULL COMMENT '交易类型：1-买入，2-卖出',
    amount             bigint         not null comment '交易金额（积分）',
    nav                decimal(10, 4) NOT NULL COMMENT '成交时的指数净值',
    shares             decimal(20, 8) NOT NULL COMMENT '成交份额',
    status             tinyint(4)     NOT NULL DEFAULT 1 COMMENT '状态：1-已完成（买入立即完成，卖出T+1结算）',
    expectedSettleDate date           DEFAULT NULL COMMENT '已废弃：预计结算日期',
    actualSettleTime   datetime       DEFAULT NULL COMMENT '实际结算完成时间（卖出时记录）',
    profitLoss         bigint         DEFAULT NULL COMMENT '仅卖出有效：盈亏金额（积分）',
    createTime         datetime       NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '下单时间',
    PRIMARY KEY (`id`),
    KEY `idx_userId` (`userId`),
    KEY `idx_user_index_create` (`userId`, `indexCode`, `createTime`),
    KEY `idx_index_code` (`indexCode`)
    ) COMMENT ='指数交易记录表';

-- 指数持仓表
create table if not exists index_position(
                                             id              bigint auto_increment primary key,
                                             userId          bigint(20)     NOT NULL COMMENT '用户ID，关联user表',
    indexCode       varchar(32)    not null comment '指数代码（如：sh000001-上证指数，sz399001-深证成指）',
    totalShares     decimal(20, 8) not null default 0 comment '总份额（= availableShares + lockedShares）',
    availableShares decimal(20, 8) not null default 0 comment '可用份额（可卖出）',
    lockedShares    decimal(20, 8) not null default 0 comment '锁定份额（当日买入，次日09:30解锁）',
    avgCost         decimal(12, 6) not null default 0 comment '平均成本（净值）',
    createTime      datetime       default current_timestamp comment '创建时间',
    updateTime      datetime       default current_timestamp on update current_timestamp comment '更新时间',
    unique key uk_user_index (userId, indexCode),
    KEY `idx_index_code` (`indexCode`),
    KEY `idx_locked_shares` (`lockedShares`)
    ) comment '指数持仓表';

-- 房间消息表
create table if not exists room_message
(
    id          bigint auto_increment comment 'id' primary key,
    userId      bigint                             not null comment '用户 id',
    messageId   varchar(128)                       null comment '消息唯一标识',
    roomId      bigint                             not null comment '房间 id',
    messageJson mediumtext                         null comment '消息 Json 数据（json）',
    createTime  datetime default CURRENT_TIMESTAMP not null comment '创建时间',
    updateTime  datetime default CURRENT_TIMESTAMP not null on update CURRENT_TIMESTAMP comment '更新时间',
    isDelete    tinyint  default 0                 not null comment '是否删除'
    ) comment '房间消息表' collate = utf8mb4_unicode_ci;

-- 模拟面试表
create table if not exists mock_interview
(
    id             bigint auto_increment comment 'id' primary key,
    workExperience varchar(256)                       not null comment '工作年限',
    jobPosition    varchar(256)                       not null comment '工作岗位',
    difficulty     varchar(50)                        not null comment '面试难度',
    messages       mediumtext                         null comment '消息列表（JSON 对象数组字段，同时包括了总结）',
    status         int      default 0                 not null comment '状态（0-待开始、1-进行中、2-已结束）',
    userId         bigint                             not null comment '创建人（用户 id）',
    createTime     datetime default CURRENT_TIMESTAMP not null comment '创建时间',
    updateTime     datetime default CURRENT_TIMESTAMP not null on update CURRENT_TIMESTAMP comment '更新时间',
    isDelete       tinyint  default 0                 not null comment '是否删除（逻辑删除）',
    index idx_userId (userId)
    ) comment '模拟面试' collate = utf8mb4_unicode_ci;

-- 收藏表情包表（硬删除）
create table if not exists emoticon_favour
(
    id          bigint auto_increment comment 'id' primary key,
    userId      bigint                             not null comment '用户 id',
    emoticonSrc varchar(512)                       null comment '表情包地址',
    createTime  datetime default CURRENT_TIMESTAMP not null comment '创建时间',
    updateTime  datetime default CURRENT_TIMESTAMP not null on update CURRENT_TIMESTAMP comment '更新时间',
    index idx_userId (userId)
    ) comment '收藏表情包表' collate = utf8mb4_unicode_ci;

-- 第三方用户关联表
create table if not exists `user_third_auth`
(
    `id`            bigint(20)                                                    not null auto_increment,
    `user_id`       bigint(20)                                                    not null comment '本地用户id',
    `nickname`      varchar(200) collate utf8mb4_general_ci                       default null comment '昵称',
    `avatar`        varchar(200) collate utf8mb4_general_ci                       default null comment '头像',
    `platform`      varchar(20) character set utf8mb4 collate utf8mb4_general_ci  not null comment '平台：github/gitee',
    `openid`        varchar(100) character set utf8mb4 collate utf8mb4_general_ci not null comment '平台用户id',
    `access_token`  varchar(500) character set utf8mb4 collate utf8mb4_general_ci default null comment 'access_token',
    `refresh_token` varchar(500) character set utf8mb4 collate utf8mb4_general_ci default null comment 'refresh_token',
    `expire_time`   datetime                                                      default null comment '过期时间',
    `raw_data`      json                                                          default null comment '原始响应数据',
    primary key (`id`),
    unique key `idx_platform_openid` (`platform`, `openid`)
    ) comment '第三方用户关联表' collate = utf8mb4_general_ci;

-- 用户打赏记录表
CREATE TABLE if not exists `donation_records`
(
    `id`         BIGINT AUTO_INCREMENT COMMENT '打赏记录ID',
    `userId`     BIGINT COMMENT '打赏用户ID',
    `amount`     DECIMAL(15, 2) NOT NULL COMMENT '打赏金额（精度：分）',
    `remark`     VARCHAR(512)            DEFAULT NULL COMMENT '转账说明/备注',
    `isDelete`   tinyint                 default 0 not null comment '是否删除',
    `createTime` DATETIME       NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `updateTime` DATETIME       NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    PRIMARY KEY (`id`),
    -- 索引
    INDEX `idx_donor` (`userId`),
    -- 外键约束
    CONSTRAINT `fk_donor_user` FOREIGN KEY (`userId`) REFERENCES `user` (`id`)
    ) ENGINE = InnoDB
    DEFAULT CHARSET = utf8mb4
    COLLATE = utf8mb4_unicode_ci COMMENT ='用户打赏记录表';

-- 英雄表
CREATE TABLE IF NOT EXISTS hero
(
    id            BIGINT AUTO_INCREMENT COMMENT '主键ID' PRIMARY KEY,
    ename         VARCHAR(50)                        NOT NULL COMMENT '英雄英文标识(如177)',
    cname         VARCHAR(50)                        NOT NULL COMMENT '中文名(如苍)',
    title         VARCHAR(100)                       NOT NULL COMMENT '称号(如苍狼末裔)',
    releaseDate   DATE                               NULL COMMENT '上线时间',
    newType       TINYINT  DEFAULT 0 COMMENT '新英雄标识(0常规/1新英雄)',
    primaryType   TINYINT                            NOT NULL COMMENT '主定位(1战士/2法师/3坦克/4刺客/5射手/6辅助)',
    secondaryType TINYINT COMMENT '副定位(1战士/2法师/3坦克/4刺客/5射手/6辅助)',
    skins         VARCHAR(500) COMMENT '皮肤列表(用|分隔，如苍狼末裔|维京掠夺者|苍林狼骑)',
    officialLink  VARCHAR(255) COMMENT '官网详情页链接',
    mossId        BIGINT COMMENT '内部ID',
    race          VARCHAR(50) COMMENT '种族[yxzz_b8]',
    faction       VARCHAR(50) COMMENT '势力[yxsl_54]',
    identity      VARCHAR(50) COMMENT '身份[yxsf_48]',
    region        VARCHAR(50) COMMENT '区域[qym_e7]',
    ability       VARCHAR(50) COMMENT '能量[nl_96]',
    height        VARCHAR(20) COMMENT '身高[sg_30]',
    quote         VARCHAR(255) COMMENT '经典台词[rsy_49]',
    createTime    DATETIME DEFAULT CURRENT_TIMESTAMP NOT NULL COMMENT '创建时间',
    updateTime    DATETIME DEFAULT CURRENT_TIMESTAMP NOT NULL ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    INDEX idx_cname (cname),
    INDEX idx_type (primaryType)
    ) COMMENT '王者荣耀英雄详情表' COLLATE = utf8mb4_unicode_ci;

-- 邮箱封禁表
create table if not exists `email_ban`
(
    id          BIGINT(20)                             NOT NULL AUTO_INCREMENT COMMENT '主键ID' PRIMARY KEY,
    email       VARCHAR(256) COMMENT '被封禁的完整邮箱地址',
    emailSuffix VARCHAR(64)                            NOT NULL COMMENT '邮箱后缀（如 .com、.net）',
    reason      VARCHAR(256) default '临时邮箱' COMMENT '封禁理由',
    createTime  datetime     default CURRENT_TIMESTAMP not null comment '创建时间',
    updateTime  datetime     default CURRENT_TIMESTAMP not null on update CURRENT_TIMESTAMP comment '更新时间'
    ) comment '邮箱封禁表' collate = utf8mb4_unicode_ci;
ALTER TABLE `email_ban`
    ADD COLUMN `bannedIp` VARCHAR(45) NULL COMMENT '封禁的 IP 地址' after emailSuffix;

-- 事件提醒表
create table if not exists event_remind
(
    id            bigint auto_increment comment 'id' primary key,
    action        varchar(50)                        not null comment '动作类型：like-点赞、at-@提及、reply-回复、comment-评论、follow-关注、share-分享',
    sourceId      bigint                             not null comment '事件源 ID，如帖子ID、评论ID 等',
    sourceType    int                                null comment '事件源类型：1- 帖子、2- 评论等',
    sourceContent varchar(256)                       not null comment '事件源的内容，比如回复的内容，回复的评论等等',
    url           varchar(256)                       not null comment '事件所发生的地点链接 url',
    state         int      default 0                 not null comment '是否已读',
    senderId      bigint                             not null comment '操作者的 ID，即谁关注了你，谁艾特了你',
    recipientId   bigint                             not null comment '接受通知的用户的 ID',
    remindTime    datetime                           not null comment '提醒的时间',
    createTime    datetime default CURRENT_TIMESTAMP not null comment '创建时间',
    updateTime    datetime default CURRENT_TIMESTAMP not null on update CURRENT_TIMESTAMP comment '更新时间',
    isDelete      tinyint  default 0                 not null comment '是否删除（逻辑删除）',
    index idx_userId (recipientId)
    ) comment '事件提醒表' collate = utf8mb4_unicode_ci;

-- 用户会员表
CREATE TABLE if not exists `user_vip`
(
    `id`         BIGINT AUTO_INCREMENT COMMENT '会员ID',
    `userId`     BIGINT COMMENT '用户ID',
    `cardNo`     VARCHAR(256) NULL COMMENT '会员兑换卡号（永久会员无卡号）',
    `type`       tinyint               default 1 not null comment '1-月卡会员 2-永久会员',
    `validDays`  DATETIME              default null comment '会员到期时间，永久会员为null',
    `isDelete`   tinyint               default 0 not null comment '是否删除',
    `createTime` DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `updateTime` DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    PRIMARY KEY (`id`),
    -- 索引
    INDEX `idx_donor` (`userId`)
    ) ENGINE = InnoDB
    DEFAULT CHARSET = utf8mb4
    COLLATE = utf8mb4_unicode_ci COMMENT ='用户会员表';

-- 词库表
create table if not exists word_library
(
    id         BIGINT AUTO_INCREMENT COMMENT '词库ID' primary key,
    word       VARCHAR(100)                       NOT NULL COMMENT '词语名称',
    category   VARCHAR(50)                        NOT NULL COMMENT '词库分类: undercover-谁是卧底, draw-default-你画我猜默认, draw-hero-你画我猜王者荣耀, draw-idiom-你画我猜成语',
    wordType   VARCHAR(50) COMMENT '词语类型（如：水果、动物、王者英雄、成语等）',
    createTime datetime default CURRENT_TIMESTAMP not null comment '创建时间',
    updateTime datetime default CURRENT_TIMESTAMP not null on update CURRENT_TIMESTAMP comment '更新时间'
    ) COMMENT '词库表' collate = utf8mb4_unicode_ci;


-- 道具表
create table if not exists props
(
    frameId     BIGINT auto_increment comment '道具 ID' PRIMARY KEY,
    imgUrl      VARCHAR(256) comment '道具图片地址',
    type        VARCHAR(256) comment '道具类型 1-摸鱼会员月卡 2-摸鱼称号 ',
    description VARCHAR(256) comment '道具描述',
    name        VARCHAR(256) comment '道具名称',
    points      INT      DEFAULT 1 comment '道具所需兑换积分',
    createTime  datetime default CURRENT_TIMESTAMP not null comment '创建时间',
    updateTime  datetime default CURRENT_TIMESTAMP not null on update CURRENT_TIMESTAMP comment '更新时间',
    isDelete    tinyint  default 0                 not null comment '是否删除'
    ) comment '道具表' collate = utf8mb4_unicode_ci;


create table if not exists fish_pet
(
    petId      BIGINT auto_increment comment '宠物 ID' PRIMARY KEY,
    petUrl     VARCHAR(256) comment '宠物图片地址',
    name       VARCHAR(256) comment '宠物名称',
    userId     BIGINT comment '用户 ID',
    level      INT      default 1 comment '宠物等级',
    exp        INT      default 0 comment '当前经验值',
    mood       INT      default 100 comment '宠物心情值（0-100）',
    hunger     INT      default 0 comment '积饿度（越高越饿，建议范围 0-100）',
    extendData VARCHAR(1024) comment '宠物扩展数据（技能、形象等，JSON 格式）',
    createTime DATETIME default CURRENT_TIMESTAMP not null comment '创建时间',
    updateTime DATETIME default CURRENT_TIMESTAMP not null on update CURRENT_TIMESTAMP comment '更新时间',
    isDelete   TINYINT  default 0                 not null comment '是否删除'
    ) comment '摸鱼宠物表' collate = utf8mb4_unicode_ci;

-- 宠物皮肤表
create table if not exists pet_skin
(
    skinId      BIGINT auto_increment comment '皮肤 ID' PRIMARY KEY,
    url         VARCHAR(256) comment '皮肤地址',
    description VARCHAR(256) comment '皮肤描述',
    name        VARCHAR(256) comment '皮肤名称',
    points      INT      DEFAULT 1 comment '皮肤所需兑换积分',
    createTime  datetime default CURRENT_TIMESTAMP not null comment '创建时间',
    updateTime  datetime default CURRENT_TIMESTAMP not null on update CURRENT_TIMESTAMP comment '更新时间',
    isDelete    tinyint  default 0                 not null comment '是否删除'
    ) comment '宠物皮肤表' collate = utf8mb4_unicode_ci;


-- 1. 物品模板（通用）
-- auto-generated definition
create table item_templates
(
    id          bigint auto_increment comment '主键ID' primary key,
    code        varchar(64)                           null comment '模板唯一码，例如 sword_iron_01',
    name        varchar(100)                          null comment '物品名称',
    category    varchar(50) default 'equipment'       not null comment '物品大类：equipment-装备类（能穿戴的）、consumable-消耗品（药水/卷轴/食物）、material-材料（强化石/合成材料）',
    sub_type    varchar(50)                           null comment '物品子类型，例如 weapon 武器、head 头盔、foot 鞋子、hand 手套',
    equip_slot  varchar(50)                           null comment '可穿戴槽位: head-头部, hand-手部, foot-脚部, weapon-武器；NULL 表示无法穿戴',
    rarity      tinyint     default 1                 not null comment '稀有度等级（1-8，数字越高越稀有）',
    levelReq    int         default 1                 null comment '使用等级需求',
    baseAttack  int         default 0                 null comment '基础攻击力',
    baseDefense int         default 0                 null comment '基础防御力',
    baseHp      int         default 0                 null comment '基础生命值',
    mainAttr    varchar(512)                          null comment '非常规属性/词缀(JSON)，格式: [{k,v},...]',
    icon        varchar(255)                          null comment '物品图标地址',
    description text                                  null comment '物品描述',
    stackable   tinyint(1)  default 0                 null comment '是否可叠加，0-不可叠加，1-可叠加（如消耗品）',
    removePoint int         default 10                null comment '分解后获得的积分',
    createTime  datetime    default CURRENT_TIMESTAMP not null comment '创建时间',
    updateTime  datetime    default CURRENT_TIMESTAMP not null on update CURRENT_TIMESTAMP comment '更新时间',
    isDelete    tinyint     default 0                 not null comment '是否删除，0-正常，1-已删除',
    constraint code unique (code)
) comment '物品模板表（通用配置，包括装备、消耗品、材料等）';

create index category on item_templates (category) comment '物品大类索引';

create index equip_slot on item_templates (equip_slot) comment '装备槽位索引';

create index rarity on item_templates (rarity) comment '稀有度索引';


-- 2. 物品实例（玩家真正持有的物品）
CREATE TABLE item_instances
(
    id           BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '主键ID',
    templateId   BIGINT                               NOT NULL COMMENT '物品模板ID（关联 item_templates.id）',
    ownerUserId  BIGINT                               NOT NULL COMMENT '持有者用户ID',
    quantity     INT        DEFAULT 1 COMMENT '物品数量：若模板可叠加则 quantity>1，否则为1',
    bound        TINYINT(1) DEFAULT 1 COMMENT '是否绑定（1-绑定后不可交易，0-未绑定可交易）',
    durability   INT        DEFAULT NULL COMMENT '耐久度（可选，部分装备适用）',
    enhanceLevel INT        DEFAULT 0 COMMENT '强化等级',
    extraData    JSON COMMENT '扩展信息（如附魔、镶嵌孔、特殊属性等JSON数据）',
    createTime   datetime   default CURRENT_TIMESTAMP not null comment '创建时间',
    updateTime   datetime   default CURRENT_TIMESTAMP not null on update CURRENT_TIMESTAMP comment '更新时间',
    isDelete     tinyint    default 0                 not null comment '是否删除，0-正常，1-已删除',
    CONSTRAINT fk_item_instances_template FOREIGN KEY (templateId) REFERENCES item_templates (id),
    INDEX idx_owner_user_id (ownerUserId) COMMENT '持有者用户索引',
    INDEX idx_template_id (templateId) COMMENT '物品模板索引'
) COMMENT ='物品实例表（玩家真正持有的物品，每个实例可有强化、耐久、附魔等个性化信息）';

-- Boss表
create table if not exists boss
(
    id                  bigint auto_increment comment 'Boss ID' primary key,
    name                varchar(100)                            not null comment 'Boss名称',
    avatar              varchar(512)                            null comment 'Boss头像URL',
    health              int           default 1000              not null comment 'Boss血量',
    attack              int           default 100               not null comment 'Boss攻击力',
    rewardPoints        int           default 100               not null comment '击败奖励积分',
    critRate            decimal(5, 4) default 0.0000            null comment '暴击率(0-1)',
    comboRate           decimal(5, 4) default 0.0000            null comment '连击率(0-1)',
    dodgeRate           decimal(5, 4) default 0.0000            null comment '闪避率(0-1)',
    blockRate           decimal(5, 4) default 0.0000            null comment '格挡率(0-1)',
    lifesteal           decimal(5, 4) default 0.0000            null comment '吸血率(0-1)',
    critResistance      decimal(5, 4) default 0.0000            null comment '抗暴击率(0-1)',
    comboResistance     decimal(5, 4) default 0.0000            null comment '抗连击率(0-1)',
    dodgeResistance     decimal(5, 4) default 0.0000            null comment '抗闪避率(0-1)',
    blockResistance     decimal(5, 4) default 0.0000            null comment '抗格挡率(0-1)',
    lifestealResistance decimal(5, 4) default 0.0000            null comment '抗吸血率(0-1)',
    sort                int           default 0                 not null comment '排序',
    status              tinyint       default 1                 not null comment '状态：0-禁用，1-启用',
    createTime          datetime      default CURRENT_TIMESTAMP not null comment '创建时间',
    updateTime          datetime      default CURRENT_TIMESTAMP not null on update CURRENT_TIMESTAMP comment '更新时间',
    isDelete            tinyint       default 0                 not null comment '是否删除',
    index idx_status (status),
    index idx_sort (sort)
    ) comment 'Boss表' collate = utf8mb4_unicode_ci;


-- 聊天记录备份表
create table if not exists room_message_backup
(
    id          bigint comment '原始消息id' primary key,
    userId      bigint                             not null comment '用户 id',
    messageId   varchar(128)                       null comment '消息唯一标识',
    roomId      bigint                             not null comment '房间 id',
    messageJson mediumtext                         null comment '消息 Json 数据（json）',
    createTime  datetime default CURRENT_TIMESTAMP not null comment '创建时间',
    updateTime  datetime default CURRENT_TIMESTAMP not null on update CURRENT_TIMESTAMP comment '更新时间',
    isDelete    tinyint  default 0                 not null comment '是否删除',
    backupTime  datetime default CURRENT_TIMESTAMP not null comment '备份时间'
    ) comment '聊天记录备份表' collate = utf8mb4_unicode_ci;


SET NAMES utf8mb4;
SET FOREIGN_KEY_CHECKS = 0;

INSERT INTO `hero` VALUES (1918248698953805826, '105', '廉颇', '正义爆轰', '2015-11-26', 0, 3, NULL, '正义爆轰|地狱岩魂|无尽征程|寅虎·御盾|功夫炙烤|撼地雄心', NULL, 3627, '人类', '稷下学院', '甘丹族长', '逐鹿', '武道', '190cm', '心不老，身不灭。\r\n一次不行，那就再来十次。', '2025-05-02 18:18:23', '2025-05-02 21:36:46');
INSERT INTO `hero` VALUES (1918248698953805827, '106', '小乔', '恋之微风', '2015-11-26', 0, 2, NULL, '恋之微风|万圣前夜|天鹅之梦|纯白花嫁|缤纷独角兽|丁香结|青蛇|音你心动|山海·琳琅生|时之魔女|秘证寻踪', NULL, 3644, '人类', '吴', '魔道名门之后', '三分之地', '魔道', '157cm', '花会枯萎，爱永不凋零！\r\n小乔，要努力变强！', '2025-05-02 18:18:23', '2025-05-03 15:37:55');
INSERT INTO `hero` VALUES (1918248698953805828, '107', '赵云', '苍天翔龙', '2015-11-26', 0, 1, 4, '苍天翔龙|忍●炎影|未来纪元|皇家上将|嘻哈天王|白执事|引擎之心|龙胆|淬星耀世|百木心枪|乘龙铭钟鼎', NULL, 3661, '人类', '蜀', '龙枪战士', '三分之地', '武道', '186cm', '枪如惊雷，照一身肝胆!\r\n心怀不惧，方能翱翔于天际。', '2025-05-02 18:18:23', '2025-05-02 21:37:03');
INSERT INTO `hero` VALUES (1918248698953805829, '108', '墨子', '和平守望', '2015-11-26', 0, 2, 1, '和平守望|金属风暴|龙骑士|进击墨子号|神迹守卫|降魔|黄金天蝎座', NULL, 3547, '人类', '稷下学院', '稷下三贤者', '逐鹿', '机关', '182/512cm', '生存，就是最精彩的战斗。\r\n会让你印象深刻的！', '2025-05-02 18:18:23', '2025-05-02 21:37:00');
INSERT INTO `hero` VALUES (1918248698953805830, '109', '妲己', '魅力之狐', '2015-11-26', 0, 2, NULL, '魅惑之狐|女仆咖啡|魅力维加斯|仙境爱丽丝|少女阿狸|热情桑巴|时之彼端|紫罗兰之誓|时之奇旅|青丘·九尾|灵卜魔法|愿照·众生和', NULL, 3663, '人造人', '倒悬天', '姜子牙所造人偶', '建木', '魔道', '165cm', '没有心，就不会受伤。\r\n为什么会痛苦？一直微笑就好了。', '2025-05-02 18:18:23', '2025-05-02 21:37:07');
INSERT INTO `hero` VALUES (1918248698953805831, '110', '嬴政', '王者独尊', '2015-11-26', 0, 2, NULL, '王者独尊|摇滚巨星|暗夜贵公子|优雅恋人|白昼王子|玄雷天君', NULL, 3680, '人类', '玄雍', '玄雍君主', '逐鹿', '魔道', '182cm', '向所有人显现的东西，才叫公正。\r\n黑色，光明的预演。', '2025-05-02 18:18:23', '2025-05-02 21:37:15');
INSERT INTO `hero` VALUES (1918248698953805832, '111', '孙尚香', '千金重弩', '2015-11-26', 0, 5, NULL, '千金重弩|火炮千金|水果甜心|蔷薇恋人|杀手不太冷|末日机甲|沉稳之力|时之恋人|异界灵契|音你闪耀|乘龙问璇玑', NULL, 3577, '人类', '吴', '吴地小公主', '三分之地', '武道', '166cm', '淑女什么的……哼，才不屑呢！\r\n来发子弹吗？满足你！', '2025-05-02 18:18:23', '2025-05-02 21:37:21');
INSERT INTO `hero` VALUES (1918248698995748866, '112', '鲁班七号', '机关造物', '2015-11-26', 0, 5, NULL, '机关造物|木偶奇遇记|福禄兄弟|电玩小子|星空梦想|狮舞东方|黑桃队长|乒乒小将|寅虎·瑞焰|时之奇旅|蔬乡物语|江户川柯南', NULL, 3697, '人造人', '玄雍', '鲁班所造之物', '逐鹿', '机关', '149cm', '检测了对面的智商，看来无法发挥全部实力了。\r\n不得不承认，有时候肌肉比头脑管用。', '2025-05-02 18:18:23', '2025-05-02 21:37:22');
INSERT INTO `hero` VALUES (1918248698995748867, '113', '庄周', '逍遥梦幻', '2015-11-26', 0, 6, 3, '逍遥幻梦|鲤鱼之梦|蜃楼王|云端筑梦师|奇妙博物学|玄嵩|高山流水|天秀·幻梦|牧神诗旅', NULL, 3594, '？', '稷下学院', '稷下三贤者', '逐鹿', '魔道', '175cm', '美妙的长眠，值得高歌一曲。\r\n天地与我并生，万物与我为一。', '2025-05-02 18:18:23', '2025-05-02 21:37:27');
INSERT INTO `hero` VALUES (1918248698995748868, '114', '刘禅', '暴走机关', '2015-11-26', 0, 6, 3, '暴走机关|英喵野望|绅士熊喵|天才门将|秘密基地|唤灵魔甲|电玩·爆裂旋风|鸭鸭历险记', NULL, 3714, '人类', '蜀', '蜀地继承人', '三分之地', '机关', '145/210cm', '少爷我从不坑爹！\r\n身高，那是禁句！', '2025-05-02 18:18:23', '2025-05-02 21:37:33');
INSERT INTO `hero` VALUES (1918248698995748869, '115', '高渐离', '叛逆吟游', '2015-11-26', 0, 2, NULL, '叛逆吟游|金属狂潮|死亡摇滚|玩趣恶龙|天秀·音浪|燃音魔法', NULL, 3611, '人类', '南荒', '吟游乐师', '逐鹿', '魔道', '178cm', '来，听离哥替对面奏响离歌。\r\n原谅我一生，放浪不羁爱自由~', '2025-05-02 18:18:23', '2025-05-02 21:37:36');
INSERT INTO `hero` VALUES (1918248698995748870, '116', '阿轲', '信念之刃', '2015-11-26', 0, 4, NULL, '信念之刃|爱心护理|暗夜猫娘|致命风华|节奏热浪|迷踪丽影|化蝶舞', NULL, 3731, '人类', '南荒', '复仇刺客', '逐鹿', '武道', '171cm', '我，是你惹不起的！\r\n不知道你的名字，但清楚你的死期。', '2025-05-02 18:18:23', '2025-05-02 21:37:43');
INSERT INTO `hero` VALUES (1918248698995748871, '117', '钟无艳', '野蛮之锤', '2015-11-26', 0, 1, 3, '野蛮之锤|生化警戒|王者之锤|海滩丽影|超时空战士|聚星闪耀|春野之旅', NULL, 3628, '人魔混血', '稷下学院', '稷下学生', '逐鹿', '武道', '177cm', '金钱和混战我都爱！\n女人心，海底针！', '2025-05-02 18:18:23', '2025-05-02 21:37:44');
INSERT INTO `hero` VALUES (1918248698995748872, '118', '孙膑', '逆流之时', '2015-11-26', 0, 6, 2, '逆流之时|未来旅行|天使之翼|妖精王|归虚梦演|天狼运算者|寅虎·展翼|小动物乐团|茶境仙', NULL, 3645, '人类', '稷下学院', '稷下学生', '逐鹿', '机关', '155cm', '时间和波浪，变化无常！\r\n人家这么可爱，当然是男孩子！', '2025-05-02 18:18:23', '2025-05-02 21:37:45');
INSERT INTO `hero` VALUES (1918248698995748873, '119', '扁鹊', '善恶怪医', '2015-11-26', 0, 2, 1, '善恶怪医|救世之瞳|化身博士|炼金王|奇幻香踪|无尽旅途|灵蛊蚀心', NULL, 3662, '人类', '玄雍', '医者', '逐鹿', '魔道', '173cm', '别放弃治疗！\r\n善良的唯有永不开口之人。', '2025-05-02 18:18:23', '2025-05-02 21:37:48');
INSERT INTO `hero` VALUES (1918248698995748874, '120', '白起', '最终兵器', '2015-11-26', 0, 3, NULL, '最终兵器|白色死神|狰|星夜王子|夜都怪侠|乐园追猎者|苍鳞隐世', NULL, 3679, '人类', '玄雍', '玄雍武将', '逐鹿', '武道', '193cm', '我是伤口，又是刀锋。\r\n昨日今日，此心如一。', '2025-05-02 18:18:23', '2025-05-02 21:38:03');
INSERT INTO `hero` VALUES (1918248698995748875, '121', '芈月', '永恒之月', '2015-12-08', 0, 2, 1, '永恒之月|红桃皇后|大秦宣太后|重明|白晶晶|幻夜卜梦|浮光幕影|瑰影绮梦', NULL, 3696, '人类', '玄雍', '玄雍太后', '逐鹿', '魔道', '175cm', '拥有了青春，也就拥抱了永恒！\r\n流淌的痛苦，多么甜蜜！', '2025-05-02 18:18:23', '2025-05-02 21:43:06');
INSERT INTO `hero` VALUES (1918248698995748876, '123', '吕布', '无双之魔', '2015-12-22', 0, 1, 3, '无双之魔|圣诞狂欢|天魔缭乱|末日机甲|猎兽之王|野性能量|御风骁将|怒海麟威|遇见神鼓|曦玄引|逐霄战戟', NULL, 3713, '人类', '暂无阵营', '重生的战神', '三分之地', '武道', '192cm', '前方深渊，身后地狱！\r\n战争，为我而生！', '2025-05-02 18:18:23', '2025-05-02 21:43:44');
INSERT INTO `hero` VALUES (1918248698995748877, '124', '周瑜', '铁血都督', '2015-11-26', 0, 2, 1, '铁血都督|海军大将|真爱至上|赤莲之焰|音你心动|雪夜绮愿|熔金海岸', NULL, 3784, '人类', '吴', '吴地都督', '三分之地', '魔道', '180cm', '没有欲望，何来胜利？\r\n用头脑，而不是武力！', '2025-05-02 18:18:23', '2025-05-02 21:39:46');
INSERT INTO `hero` VALUES (1918248698995748878, '126', '夏侯惇', '不羁之风', '2016-07-19', 0, 3, 1, '不羁之风|战争骑士|乘风破浪|无限飓风号|朔风刀|匿光决锋者|霜北刀', NULL, 3730, '人类', '魏', '魏地武将', '三分之地', '武道', '186cm', '随心所欲，这是俺的人生信条。\r\n没错！俺就是呼唤胜利的男神！', '2025-05-02 18:18:23', '2025-05-03 07:01:42');
INSERT INTO `hero` VALUES (1918248698995748879, '127', '甄姬', '洛神降临', '2015-11-26', 0, 2, NULL, '洛神降临|冰雪圆舞曲|花好人间|游园惊梦|幽恒|女儿国国王|落雪兰心|至美·化雀舞|雪境奇遇', NULL, 3747, '人类', '魏', '魔道圣人之后', '三分之地', '魔道', '171cm', '随波逐流的痛苦，你们不懂。\r\n若轻云之蔽月，若流风之回雪。', '2025-05-02 18:18:23', '2025-05-02 21:38:09');
INSERT INTO `hero` VALUES (1918248698995748880, '128', '曹操', '鲜血枭雄', '2015-11-26', 0, 1, NULL, '鲜血枭雄|超能战警|幽灵船长|死神来了|烛龙|天狼征服者|决胜大满贯', NULL, 3765, '人类', '魏', '魏地主公', '三分之地', '武道', '179cm', '仁义，多么奢侈！\r\n忠诚，美妙的谎言！', '2025-05-02 18:18:23', '2025-05-02 21:39:44');
INSERT INTO `hero` VALUES (1918248698995748881, '129', '典韦', '狂战士', '2015-11-26', 0, 1, NULL, '狂战士|黄金武士|穷奇|蓝屏警告|岱宗|铁甲之心|战鼓燎原|铁骨偃魂', NULL, 3782, '人类', '魏', '魏地武将', '三分之地', '武道', '183cm', '你能感觉到痛楚吗？\r\n战斗，让我忘记疯狂。', '2025-05-02 18:18:23', '2025-05-02 21:38:16');
INSERT INTO `hero` VALUES (1918248698995748882, '130', '宫本武藏', '剑圣', '2015-11-26', 0, 1, 4, '剑圣|鬼剑武藏|未来纪元|万象初新|地狱之眼|霸王丸|惊梅引', NULL, 3799, '人类', '扶桑', '扶桑剑客', '东风海域', '武道', '181cm', '无敌，正是我所选择的道路。', '2025-05-02 18:18:23', '2025-05-02 21:39:59');
INSERT INTO `hero` VALUES (1918248698995748883, '131', '李白', '青莲剑仙', '2016-03-01', 0, 4, NULL, '青莲剑仙|范海辛|千年之狐|凤求凰|敏锐之力|鸣剑·曳影|诗剑行|碎月剑心|谪仙醉月', NULL, 3816, '人类', '长安', '天才剑客', '河洛', '武道', '184cm', '今朝有酒今朝醉！\r\n大河之剑天上来！', '2025-05-02 18:18:23', '2025-05-03 06:58:01');
INSERT INTO `hero` VALUES (1918248698995748884, '132', '马可波罗', '远游之枪', '2016-08-23', 0, 5, NULL, '远游之枪|激情绿茵|逐梦之星|暗影游猎|潮玩牛仔|深海之息|山海·玄木吟|妄想实况|星界特工|怪盗基德', NULL, 3764, '人类', '海都家族', '冒险家', '日落海', '武道', '179cm', '世界那么大，我想来看看~\r\n行动和欲望决定未来。', '2025-05-02 18:18:23', '2025-05-03 07:02:25');
INSERT INTO `hero` VALUES (1918248698995748885, '133', '狄仁杰', '断案大师', '2015-11-26', 0, 5, NULL, '断案大师|锦衣卫|魔术师|超时空战士|阴阳师|鹰眼统帅|万华元夜|星际治安官|神器·狴犴令|绮世丹青', NULL, 3781, '人类', '长安', '长安治安官', '河洛', '武道', '178cm', '真相只有一个！\r\n代表法律制裁你！', '2025-05-02 18:18:23', '2025-05-02 21:38:24');
INSERT INTO `hero` VALUES (1918248698995748886, '134', '达摩', '拳僧', '2015-11-26', 0, 1, 3, '拳僧|拳王|大发明家|黄金狮子座|星际陆战队|沙漠行僧|爆烈喵拳', NULL, 3798, '人类', '长安', '拳僧', '河洛', '武道', '177cm', '肩挑凡世，拳握初心。\r\n健美的身材？来自持久不懈的锻炼！', '2025-05-02 18:18:23', '2025-05-02 21:38:20');
INSERT INTO `hero` VALUES (1918248698995748887, '135', '项羽', '霸王', '2015-11-26', 0, 3, 1, '霸王|帝国元帅|苍穹之光|海滩派对|职棒王牌|霸王别姬|科学大爆炸|无限倾心|苍威无极', NULL, 3815, '人类', '鸿门', '霸王', '大河流域', '武道', '197cm', '我命由我！\r\n天不容我，我必逆天！', '2025-05-02 18:18:23', '2025-05-02 21:38:28');
INSERT INTO `hero` VALUES (1918248698995748888, '136', '武则天', '女帝', '2015-11-26', 0, 2, NULL, '女帝|东方不败|海洋之心|倪克斯神谕|神器·明辉仪', NULL, 3832, '人类', '长安', '长安女帝', '河洛', '魔道', '171cm', '奉我为主！\r\n叫我女王陛下！', '2025-05-02 18:18:23', '2025-05-02 21:38:39');
INSERT INTO `hero` VALUES (1918248698995748889, '139', '老夫子', '万古长明', '2015-11-26', 0, 1, NULL, '万古长明|潮流仙人|圣诞老人|功夫老勺|醍醐杖|航海奇遇记|百相守梦|藏狐·褐绒', NULL, 3849, '神职者', '稷下学院', '稷下三贤者', '逐鹿', '武道', '173cm', '教学生，顺便拯救世界。\r\n好好教导你什么是尊师重道！', '2025-05-02 18:18:23', '2025-05-02 21:38:32');
INSERT INTO `hero` VALUES (1918248698995748890, '140', '关羽', '一骑当千', '2016-06-28', 0, 1, 3, '一骑当千|龙腾万里|天启骑士|冰锋战神|武圣|赤影疾锋|百相守梦|决胜骁骑', NULL, 3866, '人类', '蜀', '蜀地武将', '三分之地', '武道', '191cm', '生命与信念，都交托阁下！\r\n屈辱比失败更难忍受。', '2025-05-02 18:18:23', '2025-05-03 07:00:58');
INSERT INTO `hero` VALUES (1918248698995748891, '141', '貂蝉', '绝世舞姬', '2015-12-15', 0, 2, NULL, '绝世舞姬|异域舞娘|圣诞恋歌|仲夏夜之梦|逐梦之音|猫影幻舞|遇见胡旋|唤灵魅影|幻阙歌|曦玄引|长夏之忆', NULL, 3883, '人类', '暂无阵营', '绝世舞姬', '三分之地', '魔道', '172cm', '不要爱上妾身哟！\r\n无尽的舞蹈，何日方休？', '2025-05-02 18:18:23', '2025-05-02 21:43:24');
INSERT INTO `hero` VALUES (1918248698995748892, '142', '安琪拉', '暗夜萝莉', '2015-11-26', 0, 2, NULL, '暗夜萝莉|玩偶对对碰|魔法小厨娘|心灵骇客|如懿|时之奇旅|追逃游戏|乘龙聚宝船|酷洛米之心', NULL, 3900, '人类', '日落圣殿', '神秘法师', '日落海', '魔道', '153cm', '知识就是力量。\r\n神秘会屈从于更高的神秘！', '2025-05-02 18:18:23', '2025-05-02 21:38:48');
INSERT INTO `hero` VALUES (1918248698995748893, '144', '程咬金', '热烈之斧', '2015-11-26', 0, 3, NULL, '热烈之斧|爱与正义|星际陆战队|华尔街大亨|功夫厨神|活力突击|演武夺筹|无双福将|暖冬絮语|群星魔术团', NULL, 3917, '人类', '长安', '河洛名将', '河洛', '武道', '200cm', '一个字：干！\r\n两个字：揍他！', '2025-05-02 18:18:23', '2025-05-02 21:38:46');
INSERT INTO `hero` VALUES (1918248698995748894, '146', '露娜', '月光之女', '2015-11-26', 0, 4, 2, '月光之女|哥特玫瑰|绯红之刃|紫霞仙子|一生所爱|瓷语鉴心|启示之音|霜月吟', NULL, 3934, '人类', '海都家族', '寒星家族继承人', '日落海', '魔道', '173cm', '替月行道。\r\n月光映照着我的生命，以及你的死期。', '2025-05-02 18:18:23', '2025-05-02 21:38:56');
INSERT INTO `hero` VALUES (1918248698995748895, '148', '姜子牙', '封神者', '2015-11-26', 0, 2, NULL, '太古魔导|时尚教父|炽热元素使|闲日渔趣|天穹之誓|星梦巡礼', 'https://pvp.qq.com/ingame/all/tobe/rebuild/1110jinagziya.html', 3951, '神职者', '倒悬天', '神职者', '建木', '魔道', '178cm', '愿者上钩！这是多么痛彻的领悟。\r\n强者生，弱者死！', '2025-05-02 18:18:23', '2025-05-02 21:39:17');
INSERT INTO `hero` VALUES (1918248699062857729, '149', '刘邦', '双面君主', '2016-04-26', 0, 3, 2, '双面君主|圣殿之光|德古拉伯爵|夺宝奇兵|虎啸剑宗|剑破天穹', NULL, 3978, '人类', '灞上', '灞上统治者', '大河流域', '武道', '179cm', '不客观的说，我是个好人！\r\n没有永恒的朋友，只有永恒的利益！', '2025-05-02 18:18:23', '2025-05-03 06:59:30');
INSERT INTO `hero` VALUES (1918248699067052033, '150', '韩信', '国士无双', '2015-11-26', 0, 4, NULL, '国士无双|街头霸王|教廷特使|白龙吟|逐梦之影|飞衡|傲雪梅枪|弑枪猎影|群星魔术团', NULL, 3985, '人类', '灞上', '灞上武将', '大河流域', '武道', '185cm', '到达胜利之前，无法回头。\r\n必将百倍奉还！', '2025-05-02 18:18:23', '2025-05-02 21:42:16');
INSERT INTO `hero` VALUES (1918248699067052034, '152', '王昭君', '冰雪之华', '2015-11-26', 0, 2, NULL, '冰雪之华|精灵公主|偶像歌手|凤凰于飞|幻想奇妙夜|乞巧织情|午后时光|星穹之声|映山客', NULL, 4002, '人类', '狼旗', '和亲公主', '北荒', '魔道', '170cm', '白梅落下之日，归去故里之时。\r\n身躯已然冰封，灵魂仍旧火热。', '2025-05-02 18:18:23', '2025-05-02 21:39:18');
INSERT INTO `hero` VALUES (1918248699067052035, '153', '兰陵王', '暗影刀锋', '2016-02-16', 0, 4, NULL, '暗影刀锋|隐刃|暗隐猎兽者|驯魔猎人|默契交锋|金庭之子|影龙天霄', NULL, 4019, '人类', '金庭国', '金庭国王子', '云中漠地', '武道', '179cm', '刀锋所划之地，便是疆土。\r\n一个人，没有同类。', '2025-05-02 18:18:23', '2025-05-03 06:57:19');
INSERT INTO `hero` VALUES (1918248699067052036, '154', '花木兰', '传说之刃', '2016-01-01', 0, 1, NULL, '传说之刃|剑舞者|兔女郎|水晶猎龙者|青春决赛季|冠军飞将|瑞麟志|默契交锋|九霄神辉|燃星之曲', NULL, 4036, '人类', '长城守卫军', '长城守卫军队长', '河洛', '武道', '174cm', '谁说女子不如男。\r\n不动如山，迅烈如火！', '2025-05-02 18:18:23', '2025-05-03 06:56:27');
INSERT INTO `hero` VALUES (1918248699067052037, '156', '张良', '言灵之书', '2015-11-26', 0, 2, NULL, '言灵之书|天堂福音|一千零一夜|幽兰居士|黄金白羊座|缤纷绘卷|千筹问战|古海寻踪|灵野札记', NULL, 4053, '人类', '灞上', '天才学者', '大河流域', '魔道', '180cm', '我思故我在。\r\n嘘，好奇心会害死猫！', '2025-05-02 18:18:23', '2025-05-02 21:42:12');
INSERT INTO `hero` VALUES (1918248699067052038, '157', '不知火舞', '明媚烈焰', '2016-05-12', 0, 4, 2, '明媚烈焰|魅语|绯月行|花合斗', NULL, 4070, '人类', '扶桑', '不知火流继承人', '东风海域', '魔道', '165cm', '放马过来！', '2025-05-02 18:18:23', '2025-05-03 07:00:11');
INSERT INTO `hero` VALUES (1918248699067052039, '162', '娜可露露', '鹰之守护', '2016-02-22', 0, 4, NULL, '鹰之守护|晚萤|前尘镜', NULL, 4087, '人类', '扶桑', '扶桑巫女', '东风海域', '武道', '154cm', '谢谢你，玛玛哈哈', '2025-05-02 18:18:23', '2025-05-03 06:57:50');
INSERT INTO `hero` VALUES (1918248699067052040, '163', '橘右京', '神梦一刀', '2016-08-30', 0, 4, 1, '神梦一刀|修罗|枫霜尽', NULL, 4104, '人类', '扶桑', '扶桑剑客', '东风海域', '武道', '176cm', '剑如风', '2025-05-02 18:18:23', '2025-05-03 07:03:06');
INSERT INTO `hero` VALUES (1918248699067052041, '166', '亚瑟', '圣骑之力', '2015-11-26', 0, 1, 3, '圣骑之力|死亡骑士|狮心王|心灵战警|潮玩骑士王|追逃游戏|鸿运当头|动物派对|布丁狗之誓', NULL, 4121, '人类', '日落圣殿', '圣骑士团领袖', '日落海', '武道', '188cm', '因剑而生！\n理想乡呼唤着我！', '2025-05-02 18:18:23', '2025-05-02 21:36:28');
INSERT INTO `hero` VALUES (1918248699067052042, '167', '孙悟空', '齐天大圣', '2015-11-30', 0, 4, NULL, '齐天大圣|地狱火|西部大镖客|美猴王|至尊宝|全息碎影|大圣娶亲|零号·赤焰|孙行者|齐天大圣|神迹守卫', NULL, 4138, '魔种', '日之塔', '魔种起义军将领', '建木', '武道', '180cm', '取经之路就在脚下！\r\n道行太浅，老实回家做宅男！', '2025-05-02 18:18:23', '2025-05-02 21:42:48');
INSERT INTO `hero` VALUES (1918248699067052043, '168', '牛魔', '精英酋长', '2015-11-26', 0, 6, 3, '精英酋长|西部大镖客|制霸全明星|御旌|奔雷神使|牛运亨通|星界战将', NULL, 4155, '魔种', '日之塔', '魔种起义军叛徒', '建木', '武道', '230cm', '牛气冲天，纯爷们！\r\n突进的野兽之道！', '2025-05-02 18:18:23', '2025-05-02 21:39:29');
INSERT INTO `hero` VALUES (1918248699067052044, '169', '后羿', '半神之弓', '2015-11-26', 0, 5, NULL, '半神之弓|精灵王|阿尔法小队|辉光之辰|黄金射手座|如梦令|圣弓游侠|无尽星芒|完美运算', NULL, 4172, '神职者', '日之塔', '神职者，神射手', '建木', '武道', '183cm', '发光的，一个就够了。\r\n最光明，最黑暗。', '2025-05-02 18:18:23', '2025-05-02 21:39:23');
INSERT INTO `hero` VALUES (1918248699067052045, '170', '刘备', '仁德义枪', '2016-02-02', 0, 1, NULL, '仁德义枪|万事如意|纽约教父|汉昭烈帝|时之恋人|潮玩造梦师|百相守梦|异域游侠|浪漫序章', NULL, 4189, '人类', '蜀', '蜀地主公', '三分之地', '武道', '178cm', '深刻而不深沉，平淡而不平庸。\r\n出来混，最重要的是讲义气！', '2025-05-02 18:18:23', '2025-05-03 06:57:08');
INSERT INTO `hero` VALUES (1918248699067052046, '171', '张飞', '禁血狂兽', '2016-01-19', 0, 3, 6, '禁血狂兽|五福同心|乱世虎臣|虎魂|百相守梦|兔狲·蓬尾', NULL, 4206, '人魔混血', '蜀', '蜀地武将', '三分之地', '武道', '186cm', '心有猛虎。\r\n有些罪不会消失，有些事非做不可。', '2025-05-02 18:18:23', '2025-05-03 06:56:52');
INSERT INTO `hero` VALUES (1918248699067052047, '173', '李元芳', '王都密探', '2016-03-21', 0, 5, NULL, '王都密探|特种部队|黑猫爱糖果|逐浪之夏|银河之约|飞鸢探春|云中旅人|妄想特派|匿光侦查者|蔬乡物语', NULL, 4223, '人魔混血', '长安', '长安密探', '河洛', '武道', '152cm', '暗夜才是密探的主场。\r\n给予破坏者正确的绝望。', '2025-05-02 18:18:23', '2025-05-03 06:58:24');
INSERT INTO `hero` VALUES (1918248699067052048, '174', '虞姬', '森之风灵', '2016-05-24', 0, 5, NULL, '森之风灵|加勒比小姐|霸王别姬|凯尔特女王|云霓雀翎|启明星使|无限倾心|神鉴启示录|夏日便利店|愿照·岁时盈', NULL, 4240, '人类', '鸿门', '自然巡游者（自然之灵）', '大河流域', '武道', '173cm', '明媚如风，轻盈似箭。\r\n啊，已经放弃了做个淑女~', '2025-05-02 18:18:23', '2025-05-03 07:00:41');
INSERT INTO `hero` VALUES (1918248699067052049, '175', '钟馗', '虚灵城判', '2016-03-24', 0, 6, 3, '虚灵城判|地府判官|神迹守卫|驱傩正仪|乐园奇幻夜|虚灵犬护', NULL, 4257, '人造人', '长安', '长安城守护者', '河洛', '机关', '355cm', '维持秩序！\r\n吾之内涵，有容乃大。', '2025-05-02 18:18:23', '2025-05-03 06:58:40');
INSERT INTO `hero` VALUES (1918248699067052050, '178', '杨戬', '根源之目', '2016-10-11', 0, 1, NULL, '根源之目|埃及法老|永曜之星|次元傲视|天秀·启明|潮玩骑兵|破阵·退雄兵', NULL, 4291, '神职者', '倒悬天', '神职者', '建木', '武道', '185cm', '执行人间的意志！\r\n尽情驰骋的，纵使天地也太狭小！', '2025-05-02 18:18:23', '2025-05-03 07:04:09');
INSERT INTO `hero` VALUES (1918248699067052051, '183', '雅典娜', '圣域余晖', '2016-08-02', 0, 1, 4, '圣域余晖|战争女神|冰冠公主|神奇女侠|单词大作战|黎明之约', NULL, 4308, '人类', '日落圣殿', '神的继承者', '日落海', '武道', '178cm', '祈祷无用，战争有理。\r\n正视你的邪恶！', '2025-05-02 18:18:23', '2025-05-03 07:01:58');
INSERT INTO `hero` VALUES (1918248699067052052, '184', '蔡文姬', '天籁弦音', '2016-07-08', 0, 6, 2, '天籁弦音|蔷薇王座|奇迹圣诞|舞动绿茵|繁星吟游|花朝如约|电玩·兔顽号|夏日便利店|愿照·福禄聚', NULL, 4325, '人类', '魏', '音乐天才少女', '三分之地', '魔道', '104/180cm', '心有多大，舞台就有多刺激~~    \r\n不要欺负我，会把你弄哭的哟~~                                           ', '2025-05-02 18:18:23', '2025-05-03 07:01:18');
INSERT INTO `hero` VALUES (1918248699067052053, '186', '太乙真人', '炼金大师', '2016-11-24', 0, 6, 3, '炼金大师|圆桌骑士|饕餮|华丽摇滚|劲辣红锅|谧流熔炉', NULL, 4342, '人类', '倒悬天', '炼金师', '建木', '魔道', '120/230cm', '道生一，一生二，二生……太二组合！\r\n你走过最长的路，都是我们的套路！', '2025-05-02 18:18:23', '2025-05-03 07:04:22');
INSERT INTO `hero` VALUES (1918248699067052054, '180', '哪吒', '桀骜炎枪', '2017-01-12', 0, 1, 3, '桀骜炎枪|三太子|逐梦之翼|次元突破|雪上飞焰|热血海滩', NULL, 4359, '人类', '倒悬天', '陈塘关长官之子', '建木', '武道', '175cm', '我可是突破常理的存在！\r\n谁是敌人，由我决定。', '2025-05-02 18:18:23', '2025-05-03 07:05:08');
INSERT INTO `hero` VALUES (1918248699067052055, '190', '诸葛亮', '绝代智谋', '2017-01-24', 0, 2, NULL, '绝代智谋|星航指挥官|黄金分割率|武陵仙君|掌控之力|时雨天司|星域神启|鹤羽星尊', NULL, 4376, '人类', '蜀', '蜀地军师', '三分之地', '魔道', '183cm', '鞠躬尽瘁，好让你死而后已！\r\n运筹帷幄之中，决胜千里之外！', '2025-05-02 18:18:23', '2025-05-03 07:05:16');
INSERT INTO `hero` VALUES (1918248699067052056, '192', '黄忠', '燃魂重炮', '2017-02-08', 0, 5, NULL, '燃魂重炮|芝加哥教父|烈魂|火炮绅士|怒海冲锋', NULL, 4393, '人类', '蜀', '机关炮老兵', '三分之地', '武道', '187cm', '彪悍的人生不需要解释。\r\n强者恒强！', '2025-05-02 18:18:23', '2025-05-03 07:05:36');
INSERT INTO `hero` VALUES (1918248699067052057, '191', '大乔', '沧海之曜', '2017-02-28', 0, 6, 2, '沧海之曜|伊势巫女|守护之力|猫狗日记|白蛇|白鹤梁神女|挚爱花嫁|时之奇旅|乘龙忆丹青|绒语心约', NULL, 4410, '人类', '吴', '魔道名门继承者', '三分之地', '魔道', '168cm', '完美是最无情的禁锢。\r\n点亮的心，不会轻易熄灭。', '2025-05-02 18:18:23', '2025-05-03 07:05:46');
INSERT INTO `hero` VALUES (1918248699067052058, '187', '东皇太一', '噬灭日蚀', '2017-03-30', 0, 6, 2, '噬灭日蚀|东海龙王|逐梦之光|灼幽烈阳|噬灭天穹|金福满堂', NULL, 4427, '人类', '云梦泽', '东神之城统治者', '大河流域', '魔道', '215cm', '万物皆可知！\r\n俯视低等种族的彷徨，循螺旋而上！', '2025-05-02 18:18:23', '2025-05-03 07:05:59');
INSERT INTO `hero` VALUES (1918248699067052059, '182', '干将莫邪', '淬命双剑', '2017-05-22', 0, 2, NULL, '淬命双剑|第七人偶|冰霜恋舞曲|久胜战神|真爱魔法|胡桃夹子|画中仙|雾都夜雨', NULL, 4444, '人类', '云梦泽', '铸剑师', '大河流域', '魔道', '237cm', '一分为二的生命，独一无二的魂灵。\r\n最好的剑，永远是下一把。', '2025-05-02 18:18:23', '2025-05-03 07:06:53');
INSERT INTO `hero` VALUES (1918248699067052060, '189', '鬼谷子', '万物有灵', '2017-06-29', 0, 6, 2, '万物有灵|阿摩司公爵|幻乐之宴|原初探秘者|五谷丰年|天穹祈灯', NULL, 4461, '人类', '云梦泽', '博学者', '大河流域', '魔道', '150cm', '理解世界，而非享受它。\r\n名为世外高人，实乃异类！', '2025-05-02 18:18:23', '2025-05-03 07:07:06');
INSERT INTO `hero` VALUES (1918248699067052061, '193', '铠', '破灭刃锋', '2017-07-18', 0, 1, 3, '破灭刃锋|龙域领主|曙光守护者|青龙志|绛天战甲|银白咏叹调|琥珀纪元|冥王哈迪斯', NULL, 3564, '人类', '长城守卫军', '长城守卫军成员', '河洛', '武道', '188cm', '长城，让你忘记自己孤身一人。\r\n磨砺的不止锋芒，还有灵魂。', '2025-05-02 18:18:23', '2025-05-03 07:07:24');
INSERT INTO `hero` VALUES (1918248699067052062, '196', '百里守约', '静谧之眼', '2017-08-08', 0, 5, 4, '静谧之眼|绝影神枪|特工魅影|朱雀志|碎云|真我赫兹|雪豹·霜爪', NULL, 4478, '人魔混血', '长城守卫军', '长城守卫军成员', '河洛', '武道', '182cm', '无论何时，何地，都会遵守约定。\r\n给我一个目标，还你一片寂静。', '2025-05-02 18:18:23', '2025-05-03 07:08:01');
INSERT INTO `hero` VALUES (1918248699067052063, '195', '百里玄策', '嚣狂之镰', '2017-08-24', 0, 4, NULL, '嚣狂之镰|威尼斯狂欢|白虎志|原初追逐者|热力回旋|超元猎域|百相守梦', NULL, 4495, '人魔混血', '长城守卫军', '长城守卫军成员', '河洛', '武道', '171cm', '讲道理，有哥哥罩的小疯子简直不讲道理！\r\n我有哥哥，你没有，这就是任性的理由。', '2025-05-02 18:18:23', '2025-05-03 07:07:52');
INSERT INTO `hero` VALUES (1918248699067052064, '194', '苏烈', '不屈铁壁', '2017-09-26', 0, 6, 3, '不屈铁壁|爱与和平|坚韧之力|玄武志|千军破阵|春野闲踪', NULL, 4521, '人类', '长城守卫军', '长城守卫军成员', '河洛', '武道', '208cm', '可战不可屈！\r\n历史书写于平凡人。', '2025-05-02 18:18:23', '2025-05-03 07:08:12');
INSERT INTO `hero` VALUES (1918248699067052065, '198', '梦奇', '入梦之灵', '2017-10-23', 0, 3, 2, '入梦之灵|美梦成真|胖达荣荣|天降福星|顽趣', NULL, 4529, '魔种', '倒悬天', '食梦者', '建木', '魔道', '97cm', '火锅好吃，刀下留人!', '2025-05-02 18:18:23', '2025-05-03 07:08:20');
INSERT INTO `hero` VALUES (1918248699067052066, '179', '女娲', '至高创世', '2017-11-21', 0, 2, NULL, '至高创世|尼罗河女神|朔望之晖|补天|愿照·千秋盛', NULL, 4546, '神', '倒悬天', '超智慧体', '建木', '魔道', '180cm', '遥远，蔚蓝的星球……终结即起源！\r\n服从你应该服从的对象。', '2025-05-02 18:18:23', '2025-05-03 07:08:43');
INSERT INTO `hero` VALUES (1918248699067052067, '501', '明世隐', '灵魂劫卜', '2017-12-13', 0, 6, NULL, '灵魂劫卜|占星术士|虹云星官|疑决卦|吟游魔法|夜落电台|景韶洛都', NULL, 4563, '人类', '长安', '尧天组织首领', '河洛', '魔道', '181cm', '福兮祸所伏，祸兮福所倚。\r\n人生乏味，我欲令之光怪陆离。', '2025-05-02 18:18:23', '2025-05-03 07:09:18');
INSERT INTO `hero` VALUES (1918248699067052068, '199', '公孙离', '幻舞玲珑', '2018-01-29', 0, 5, NULL, '幻舞玲珑|花间舞|蜜橘之夏|无限星赏官|祈雪灵祝|玉兔公主|记忆之芯|离恨烟|云诺千山', NULL, 4580, '人魔混血', '尧天', '长乐坊舞者', '河洛', '武道', '169cm', '花绽放于长安的春日，温暖又幸福~\r\n飘零的孤鸟，也有权利寻求幸福呀。', '2025-05-02 18:18:23', '2025-05-03 07:33:25');
INSERT INTO `hero` VALUES (1918248699067052069, '176', '杨玉环', '霓裳风华', '2018-02-12', 0, 2, NULL, '风华霓裳|霓裳曲|遇见飞天|寅虎·心曲|银翎春语', NULL, 4597, '人造人', '长安', '长乐坊乐师', '河洛', '魔道', '169cm', '聆听渴望的，深信珍视的，沉醉梦寐以求的\r\n春江花月照人生无穷，弦音流转听山河入梦。', '2025-05-02 18:18:23', '2025-05-03 07:33:45');
INSERT INTO `hero` VALUES (1918248699067052070, '502', '裴擒虎', '六合虎拳', '2018-02-28', 0, 4, NULL, '六合虎拳|街头霸王|梅西|天狼狩猎者|李小龙|寅虎·赤拳|擒涛扼浪|祥瑞亨通', NULL, 3581, '人魔混血', '长安', '拳师', '河洛', '武道', '177cm', '等俺这拳头砸下去，有人可能会变废柴哦。\r\n会牵挂的叫亲人，会回去的是故乡。', '2025-05-02 18:18:23', '2025-05-03 07:33:58');
INSERT INTO `hero` VALUES (1918248699067052071, '197', '弈星', '天元之弈', '2018-03-20', 0, 2, NULL, '天元之弈|踏雪寻梅|混沌棋|滕王阁序|炽奕燎原|万物之道', NULL, 3598, '人类', '长安', '少年棋手', '河洛', '魔道', '170cm', '纵横十九道内的，是无穷宇宙。\n若世有神明，亦会胜他半子。', '2025-05-02 18:18:23', '2025-05-03 07:34:21');
INSERT INTO `hero` VALUES (1918248699067052072, '503', '狂铁', '战车意志', '2018-04-20', 0, 1, 3, '战车意志|命运角斗场|御狮|特工战影|电玩高手|龙之律动', 'https://pvp.qq.com/ingame/all/tobe/rebuild/0413kt.html', 3615, '人类', '海都家族', '海都佣兵', '日落海', '武道', '171cm', '勇气是唯一的信仰。\r\n为何而战的意志，胜于钢铁之躯。', '2025-05-02 18:18:23', '2025-05-03 07:34:51');
INSERT INTO `hero` VALUES (1918248699067052073, '504', '米莱狄', '筑城者', '2018-05-15', 0, 2, NULL, '筑城者|精准探案法|御霄|胡桃异想国|契约魔法|完美假期|怪诞之夜', NULL, 3632, '人类', '海都家族', '海都执政者', '日落海', '机关', '169cm', '能源，永不枯竭。\r\n神的礼物，附带有昂贵的价格。', '2025-05-02 18:18:23', '2025-05-03 07:35:07');
INSERT INTO `hero` VALUES (1918248699067052074, '125', '元歌', '无间傀儡', '2018-07-04', 0, 4, NULL, '无间傀儡|午夜歌剧院|云间偶戏|无心', NULL, 3649, '人类', '蜀', '机关傀儡师', '三分之地', '机关', '182cm', '无欲无求，笑口常开~\r\n人生如戏，全靠演技。', '2025-05-02 18:18:23', '2025-05-03 07:35:16');
INSERT INTO `hero` VALUES (1918248699067052075, '510', '孙策', '光明之海', '2018-07-17', 0, 3, 1, '光明之海|海之征途|猫狗日记|末日机甲|挚爱之约|乘龙淬吴钩|时之奇旅', NULL, 3666, '人类', '吴', '江东小霸王', '三分之地', '武道', '185cm', '自由，是最热烈的远行~\r\n梦想着梦想中国家的梦想！', '2025-05-02 18:18:23', '2025-05-03 07:35:29');
INSERT INTO `hero` VALUES (1918248699067052076, '137', '司马懿', '寂灭之心', '2018-08-23', 0, 4, 2, '寂灭之心|魇语军师|暗渊魔法', NULL, 3683, '人类', '魏', '魏地军师', '三分之地', '魔道', '184cm', '一切都在破碎中。\r\n宇宙之中，从无公正。', '2025-05-02 18:18:23', '2025-05-03 07:35:49');
INSERT INTO `hero` VALUES (1918248699067052077, '509', '盾山', '无尽之盾', '2018-09-04', 0, 6, 3, '无尽之盾|极冰防御线|御銮|圆桌骑士|梦圆繁星', NULL, 3700, '人造人', '长城守卫军', '修筑长城的机械', '河洛', '机关', '283cm', 'emm....@Y%$&amp;^@#^', '2025-05-02 18:18:23', '2025-05-03 07:36:04');
INSERT INTO `hero` VALUES (1918248699067052078, '508', '伽罗', '破魔之箭', '2018-09-27', 0, 5, NULL, '破魔之箭|花见巫女|箭羽风息|太华|天狼溯光者|炽翼辉光|琥珀纪元', NULL, 3717, '人类', '长城守卫军/千窟城', '千窟城继承者', '云中漠地', '魔道', '172cm', '既皈依文明，绝不轻易令其破灭。\n羌笛何须怨杨柳，春风不度玉门关。', '2025-05-02 18:18:23', '2025-05-03 07:36:12');
INSERT INTO `hero` VALUES (1918248699067052079, '312', '沈梦溪', '爆弹怪猫', '2018-10-23', 0, 2, NULL, '爆弹怪猫|棒球奇才|鲨炮海盗猫|星空之诺|月团寄思|大漠名商|匿光破解者|龙舞盛年|鸭鸭历险记', NULL, 3734, '人魔混血', '长城守卫军', '制造爆弹的魔种猫，长城守卫军', '河洛', '机关', '151cm', '长城之子，归于长城。\r\n本猫守望的长城，屹立不倒。', '2025-05-02 18:18:23', '2025-05-03 07:36:27');
INSERT INTO `hero` VALUES (1918248699067052080, '507', '李信', '谋世之战', '2018-11-22', 0, 1, NULL, '谋世之战|灼热之刃|一念神魔|山海·炽霜斩', NULL, 3751, '人类', '长城守卫军', '长城守卫军成员', '河洛', '武道/魔道', '187cm', '背负守护的誓言，必以信成。\r\n不会让长安城，将我遗忘。', '2025-05-02 18:18:23', '2025-05-03 07:36:39');
INSERT INTO `hero` VALUES (1918248699067052081, '513', '上官婉儿', '惊鸿之笔', '2018-12-18', 0, 2, NULL, '惊鸿之笔|修竹墨客|梁祝|天狼绘梦者|神器·万象笔|妄想奇谈|群星魔术团', NULL, 4614, '人类', '长安', '女帝的密探', '河洛', '武道', '170cm', '上通自然之性，下取万类之象。\r\n横如千里阵云，折如百钧弩发。', '2025-05-02 18:18:23', '2025-05-03 07:36:50');
INSERT INTO `hero` VALUES (1918248699067052082, '515', '嫦娥', '寒月公主', '2019-01-17', 0, 2, 1, '寒月公主|露花倒影|如梦令|拒霜思|暖冬·兔眠|漠中幻影', NULL, 4631, '人类', '日之塔', '魔道家族公主', '建木', '魔道', '171cm', '夜晚的太阳，保护属于它的人……\r\n以前，我有一座花园……', '2025-05-02 18:18:23', '2025-05-03 07:37:14');
INSERT INTO `hero` VALUES (1918248699067052083, '511', '猪八戒', '无忧猛士', '2019-01-30', 0, 3, NULL, '无忧猛士|年年有余|西部大镖客|猪悟能|潮玩探月行', NULL, 4648, '魔种', '日之塔', '魔种起义军将领', '建木', '武道', '183cm', '天寒地冻，不长点脂肪怎么行？\r\n看我……看我……不收拾了你！', '2025-05-02 18:18:23', '2025-05-03 07:37:21');
INSERT INTO `hero` VALUES (1918248699067052084, '529', '盘古', '破晓之神', '2019-02-22', 0, 1, NULL, '破晓之神|创世神祝|重装意志|冰霜神祇|经纬探寻者|敬我三分', NULL, 4665, '神', '倒悬天', '超智慧体', '建木', '武道', '212cm', '开天辟地，万物新生！\r\n温室的实质是牢笼。', '2025-05-02 18:18:23', '2025-05-03 07:37:30');
INSERT INTO `hero` VALUES (1918248699067052085, '505', '瑶', '鹿灵守心', '2019-04-16', 0, 6, 2, '鹿灵守心|森|遇见神鹿|时之祈愿|时之愿境|山海·碧波行|真我赫兹|拾光映像|大耳狗之梦', NULL, 4682, '人类', '云梦泽', '鹿灵转生者', '大河流域', '魔道', '162cm', '过去生于未来~\r\n要把梦藏在树叶里，不然梦会坏。', '2025-05-02 18:18:23', '2025-05-03 07:37:46');
INSERT INTO `hero` VALUES (1918248699067052086, '506', '云中君', '流云之翼', '2019-05-11', 0, 4, NULL, '流云之翼|荷鲁斯之眼|纤云弄巧|时之祈愿|群星魔术团', NULL, 4699, '人类', '云梦泽', '孤鸟转生者', '大河流域', '武道', '182cm', '黑夜苏醒盲目，东边归去飞鸟~', '2025-05-02 18:18:23', '2025-05-03 07:37:56');
INSERT INTO `hero` VALUES (1918248699067052087, '522', '曜', '星辰之子', '2019-06-27', 0, 1, 4, '星辰之子|归虚梦演|云鹰飞将|李逍遥|海·苍雷引', NULL, 4716, '人类', '稷下学院', '稷下学生', '逐鹿', '武道', '176cm', '剑指的方向，就是天才的故乡！\r\n星光荡开宇宙，本人闪耀其中。', '2025-05-02 18:18:23', '2025-05-03 07:38:06');
INSERT INTO `hero` VALUES (1918248699067052088, '518', '马超', '冷晖之枪', '2019-08-15', 0, 1, NULL, '冷晖之枪|幸存者|神威|无双飞将|琥珀纪元|访茗客', NULL, 433, '人类', '蜀', '蜀地将领', '三分之地', '武道', '180cm', '只和理想结盟，只与纯洁立誓。\r\n不必辨认我，识我的枪吧！', '2025-05-02 18:18:23', '2025-05-03 07:38:18');
INSERT INTO `hero` VALUES (1918248699067052089, '523', '西施', '幻纱之灵', '2019-09-24', 0, 2, NULL, '幻纱之灵|归虚梦演|诗语江南|游龙清影|玲珑珍味|至美·乘鲤谣|雪境奇遇|续相思', NULL, 4750, '人类', '稷下学院', '稷下学生', '逐鹿', '魔道', '163cm', '每一种境遇都是命运的附赠品。\r\n有珍宝，不如有眼光。', '2025-05-02 18:18:23', '2025-05-03 07:38:31');
INSERT INTO `hero` VALUES (1918248699067052090, '525', '鲁班大师', '神匠', '2019-11-28', 0, 6, 3, '神匠|归虚梦演|乓乓大师|匿光启智者|探海日志', NULL, 3768, '人类', '稷下学院', '稷下学生', '逐鹿', '机关', '192cm', '机关是数字的哲学。\r\n人无法两次重现同一份杰作。', '2025-05-02 18:18:23', '2025-05-03 07:38:44');
INSERT INTO `hero` VALUES (1918248699067052091, '524', '蒙犽', '烈炮小子', '2020-01-09', 0, 5, NULL, '烈炮小子|归虚梦演|狂想玩偶喵|龙鼓争鸣|胡桃狂想曲|百解令|顽岩魄', NULL, 4767, '人类', '稷下学院', '稷下学生', '逐鹿', '机关', '150cm', '言语，有时比枪炮更伤人。\r\n黑白分明的，不只是我头发！', '2025-05-02 18:18:23', '2025-05-03 07:39:16');
INSERT INTO `hero` VALUES (1918248699067052092, '531', '镜', '破镜之刃', '2020-03-31', 0, 4, NULL, '破镜之刃|冰刃幻境|炽阳神光|匿光追影者|玫瑰异探|真我赫兹|青焰无极', NULL, 4784, '人类', '玄雍', '阴曲情报专员', '逐鹿', '武道', '170cm', '阻拦在眼前的，通通打碎就行了。\r\n第一个和最后一个敌人，都是自己。', '2025-05-02 18:18:23', '2025-05-03 07:39:38');
INSERT INTO `hero` VALUES (1918248699067052093, '527', '蒙恬', '秩序统将', '2020-06-02', 0, 3, NULL, '秩序统将|秩序猎龙将|蔚蓝守将|荣光圣徽', NULL, 4801, '人类', '玄雍', '玄雍护国大将军', '逐鹿', '武道', '190cm', '令则行，禁则止，无有所怠！\r\n用兵之道，在于用阵。', '2025-05-02 18:18:23', '2025-05-03 07:39:44');
INSERT INTO `hero` VALUES (1918248699067052094, '533', '阿古朵', '山林之子', '2020-08-04', 0, 5, NULL, '山林之子|熊喵少女|顽趣|江河有灵|布丁狗之约', NULL, 3785, '人类', '蜀', '山孩；益城奇兵', '三分之地', '武道', '145/209cm', '哪儿有好吃的，我们就去哪儿~！\r\n在山里打架，要用我们山民的法则！', '2025-05-02 18:18:23', '2025-05-03 07:39:56');
INSERT INTO `hero` VALUES (1918248699067052095, '536', '夏洛特', '玫瑰剑士', '2020-09-24', 0, 1, NULL, '玫瑰剑士|永昼|浮生妄', NULL, 3802, '人类', '日落圣殿', '贵族剑士', '日落海', '武道', '待调查', '我们珍视荣誉，但绝非过去的荣誉。\n已显现的命运，不会因逃避而改变。', '2025-05-02 18:18:23', '2025-05-03 07:40:08');
INSERT INTO `hero` VALUES (1918248699067052096, '528', '澜', '鲨之猎刃', '2020-12-08', 0, 4, 1, '鲨之猎刃|孤猎|赏金猎手|电玩·雷克斯|逐花归海|愿照·九州拓', NULL, 3819, '人类', '暂无阵营', '前魏都顶级杀手', '三分之地', '武道/魔道', '180cm', '狩猎目标的鲨鱼，会追索血的滋味！<br>痛苦会让恐惧离开身体。', '2025-05-02 18:18:23', '2025-05-03 07:40:24');
INSERT INTO `hero` VALUES (1918248699067052097, '537', '司空震', '雷霆之王', '2021-01-14', 0, 1, 2, '雷霆之王|启蛰|地狱燃心|愿照·山河定', NULL, 3836, '人类', '长安', '长安大司空', '河洛', '魔道', '190cm', '迈向光明之路，注定荆棘丛生\n一个人的强大，并不是真正的强大', '2025-05-02 18:18:23', '2025-05-03 07:40:43');
INSERT INTO `hero` VALUES (1918248699067052098, '155', '艾琳', '精灵之舞', '2021-04-08', 0, 5, NULL, '精灵之舞|女武神|陌上桑', NULL, 3853, '精灵', '黄金森林', '精灵公主', '日落海', '魔道', '165cm', '理想乡，永远在下一个地方~\n探索和意外总是形影不离。', '2025-05-02 18:18:23', '2025-05-03 07:40:50');
INSERT INTO `hero` VALUES (1918248699067052099, '538', '云缨', '燎原之心', '2021-06-23', 0, 1, 4, '燎原之心|赤焰之缨|鹤归松栖|幻光神枪', NULL, 3870, '人类', '长安', '大理寺新锐', '河洛', '武道', '165cm', '有什么麻烦，尽管找我好啦~\n六尺之内，我是无敌的！', '2025-05-02 18:18:23', '2025-05-03 07:41:04');
INSERT INTO `hero` VALUES (1918248699067052100, '540', '金蝉', '渡世行者', '2021-11-16', 0, 2, NULL, '渡世行者|前尘|唐三藏', NULL, 3887, '人类', '长安', '西行的修行者', '河洛', '魔道', '180cm', '心有所向，无畏求之\n众生之苦皆为吾苦', '2025-05-02 18:18:23', '2025-05-03 07:41:16');
INSERT INTO `hero` VALUES (1918248699067052101, '542', '暃', '玉城之子', '2022-01-06', 0, 4, NULL, '玉城之子|碧珀绯影|星界游侠|埋骨钱', NULL, 4818, '人类', '玉城', '玉城大王子', '云中漠地', '魔道/武道', '183cm', '人们也许存有偏见，但命运没有\n我可不想做什么英雄，太麻烦了', '2025-05-02 18:18:23', '2025-05-03 07:41:30');
INSERT INTO `hero` VALUES (1918248699067052102, '534', '桑启', '萤火之旅', '2022-04-14', 0, 6, 2, '萤火之旅|画中游|海盐诗旅|奇遇星旅|鸣野蒿', NULL, 4835, '人类', '鸣沙', '萤火的旅行者', '云中漠地', '魔道', '166cm', '只要眼里有光，黑夜就永远不会降临。\n最奇妙的旅行，往往开始于最不起眼的地方。', '2025-05-02 18:18:23', '2025-05-03 07:41:39');
INSERT INTO `hero` VALUES (1918248699067052103, '548', '戈娅', '沙海飞舟', '2022-06-23', 0, 5, NULL, '沙海飞舟|危途狂花|驭风魔法|玫蓝誓约', 'https://pvp.qq.com/ingame/all/tobe/newheros/0622geya.html', 4852, '人类', '瀚海图兰', '瀚海图兰领袖', '云中漠地', '武道', '170cm', '沙海从未宁静，人心永不满足。\n有多大能耐，吃多少地盘。', '2025-05-02 18:18:23', '2025-05-02 18:18:44');
INSERT INTO `hero` VALUES (1918248699067052104, '521', '海月', '永夜之心', '2022-09-22', 0, 2, NULL, '永夜之心|幻泉雾影|浮梦罗烟|王牌新星|金乌负日', 'https://pvp.qq.com/ingame/all/tobe/newheros/0915haiyue.html', 4937, '人类', '永夜酒肆', '神职者,月裔;云中蝶饲养人', '云中漠地', '\r\n魔道', '170cm', '流光一瞬，碧海桑田', '2025-05-02 18:18:23', '2025-05-02 20:50:59');
INSERT INTO `hero` VALUES (1918248699067052105, '544', '赵怀真', '自在之心', '2022-12-01', 0, 1, NULL, '自在之心|太极少年|鹤归松栖', 'https://pvp.qq.com/ingame/all/tobe/newheros/1202zhz.html', 5122, '人类', '长安', '两仪门镇守者', '河洛', '武道、魔道', '181cm', '心在寰宇，自得自在', '2025-05-02 18:18:23', '2025-05-02 20:52:38');
INSERT INTO `hero` VALUES (1918248699067052106, '545', '莱西奥', '火鹰船长', '2023-01-03', 0, 5, NULL, '火鹰船长|西部游侠|末日机甲', 'https://pvp.qq.com/ingame/all/tobe/newheros/0103laixiao.html', 5138, '人类', '\r\n海都', '海都外城区守护者', '日落海', '武道', '\r\n183cm', '别担心，火鹰来了！', '2025-05-02 18:18:23', '2025-05-02 20:54:11');
INSERT INTO `hero` VALUES (1918248699067052107, '564', '姬小满', '武道奇才', '2023-04-15', 0, 1, NULL, '武道奇才|零食大作战|妄想食味|战舞者|飞车小橘子', 'https://pvp.qq.com/ingame/all/tobe/newheros/0413jixiaoman.html', 5753, '人类', '海都家族', '稷下学生', '\r\n日落海', '武道', '163cm', '武道没有定式，人生亦是如此。', '2025-05-02 18:18:23', '2025-05-02 20:55:30');
INSERT INTO `hero` VALUES (1918248699067052108, '514', '亚连', '追忆之刃', '2023-06-27', 0, 1, 3, '追忆之刃|破局者|落雪白狼|破空之剑', 'https://pvp.qq.com/ingame/all/tobe/newheros/0627yalian.html', 5560, '人类', '\r\n暂无阵营', '海都佣兵', '\r\n日落海', '武道', '172cm', '需要我解决什么？什么都可以。', '2025-05-02 18:18:23', '2025-05-02 20:56:50');
INSERT INTO `hero` VALUES (1918248699067052109, '159', '朵莉亚', '人鱼之歌', '2023-11-03', 0, 6, 2, '人鱼之歌|金色潮汐|心动手记', 'https://pvp.qq.com/ingame/all/tobe/newheros/1103duoliya.html', 6177, '人鱼', '祝福水域', '人鱼公主、未来海祭司继承人', '\r\n日落海', '\r\n魔道', '162cm', '海风和浪花协奏着小人鱼动听的一天~', '2025-05-02 18:18:23', '2025-05-02 20:57:57');
INSERT INTO `hero` VALUES (1918248699067052110, '563', '海诺', '命运之引', '2023-11-30', 0, 1, 2, '命运之引|时空谍影|心动手记', 'https://pvp.qq.com/ingame/all/tobe/newheros/1130hainuo.html', 6387, '人类', '海都家族', '命运家族族长', '日落海', '\r\n魔道', '\r\n183cm', '凌驾于命运之上的，从不是神明，而是决心', '2025-05-02 18:18:23', '2025-05-02 20:59:13');
INSERT INTO `hero` VALUES (1918248699129966594, '519', '敖隐', '凌霄真龙', '2024-02-06', 0, 5, NULL, '凌霄真龙', 'https://pvp.qq.com/ingame/all/tobe/newheros/0206aoyin.html', 6367, '龙', '暂无阵营', '龙族后裔', '大河流域', '魔道', '176cm', '潜龙腾渊凌霄上，尘世无名誓不休！', '2025-05-02 18:18:23', '2025-05-02 18:18:46');
INSERT INTO `hero` VALUES (1918248699129966595, '517', '大司命', '肃归之戈', '2024-03-28', 0, 1, NULL, '肃归之戈|暗都幽影|东方月初', 'https://pvp.qq.com/ingame/all/tobe/newheros/0328dasiming.html', 6836, '魔种', '巫神祝\r\n巫神祝\r\n巫神祝', '云梦泽神巫，巫神祝领袖', '大河流域', '\r\n魔道', '\r\n186cm', '司命者，当无情，有情即孽。', '2025-05-02 18:18:23', '2025-05-02 21:00:50');
INSERT INTO `hero` VALUES (1918248699129966596, '582', '元流之子(法师)', '万妙之心', '2024-06-27', 0, 2, NULL, '万妙之心', 'https://pvp.qq.com/ingame/all/tobe/newheros/0627yuanliuzhizi.html', 7211, '人类', '\r\n稷下学院', '稷下学生', '\r\n逐鹿', '武道，魔道', '176/163cm', '稷下，我回来了。', '2025-05-02 18:18:23', '2025-05-02 21:02:18');
INSERT INTO `hero` VALUES (1918248699129966597, '581', '元流之子(坦克)', '止戈之道', '2024-06-27', 0, 3, NULL, '止戈之道', 'https://pvp.qq.com/ingame/all/tobe/newheros/0627yuanliuzhizi.html', 7194, '人类', '稷下学院', '稷下学生', '逐鹿', '武道，魔道', '176/163cm', '稷下，我回来了。', '2025-05-02 18:18:23', '2025-05-02 21:02:55');
INSERT INTO `hero` VALUES (1918248699129966598, '577', '少司缘', '聆愿之祝', '2024-08-08', 0, 6, NULL, '聆愿之祝|涂山红红|灵卦秘语', 'https://pvp.qq.com/ingame/all/tobe/newheros/0810shaosiyuan.html', 7513, '魔种', '\r\n巫神祝', '牵缘巫祝', '\r\n大河流域', '魔道', '167cm', '眼观六路尘缘事，线牵八方有情人~', '2025-05-02 18:18:23', '2025-05-02 21:04:33');
INSERT INTO `hero` VALUES (1918248699129966599, '558', '影', '黯羽东君', '2024-09-27', 0, 1, NULL, '黯羽东君|魅影绮裳', 'https://pvp.qq.com/ingame/all/tobe/newheros/0927ying.html', 8554, '\r\n魔种', '全知者', '前神巫东君；全知者盟友', '大河流域', '魔道、武道', '170cm', '我不是闲人，是坏人哟~', '2025-05-02 18:18:23', '2025-05-02 21:06:07');
INSERT INTO `hero` VALUES (1918248699129966600, '177', '苍', '苍狼末裔', '2024-12-25', 0, 5, NULL, '苍狼末裔|维京掠夺者|苍林狼骑', '//pvp.qq.com/ingame/all/tobe/rebuild/1225c.html', 4274, '人类', '狼旗', '狼旗首领', '北荒', '武道', '177cm', '雄鹰不为暴风折翼，狼群不因长夜畏惧。', '2025-05-02 18:18:23', '2025-05-02 18:18:48');
INSERT INTO `hero` VALUES (1918248699129966601, '550', '空空儿', '笑面诡手', '2025-03-27', 0, 6, NULL, '笑面诡手', '//pvp.qq.com/ingame/all/tobe/newheros/0327kongkonger.html', 8693, '人类', '诡市', '\r\n彩戏师', '\r\n河洛', '魔道', '180cm', '三球两碗一手鬼，六神无主四方惊。', '2025-05-02 18:18:23', '2025-05-02 21:07:05');
INSERT INTO `hero` VALUES (1918248699129966602, '584', '元流之子(射手)', '沉舟之志', '2025-04-23', 1, 5, NULL, '沉舟之志', '//pvp.qq.com/ingame/all/tobe/newheros/0627yuanliuzhizi.html', 8744, '人类', '稷下学院', '稷下学生', '逐鹿', '武道，魔道', '176/163cm', '稷下，我回来了。', '2025-05-02 18:18:23', '2025-05-02 21:02:59');

SET FOREIGN_KEY_CHECKS = 1;

UPDATE `fish`.`hero` SET `ename` = '519', `cname` = '敖隐', `title` = '凌霄真龙', `releaseDate` = '2024-02-06', `newType` = 0, `primaryType` = 5, `secondaryType` = NULL, `skins` = '凌霄真龙', `officialLink` = 'https://pvp.qq.com/ingame/all/tobe/newheros/0206aoyin.html', `mossId` = 6367, `race` = '龙', `faction` = '暂无阵营', `identity` = '龙族后裔', `region` = '大河流域', `ability` = '魔道', `height` = '176cm', `quote` = '潜龙腾渊凌霄上，尘世无名誓不休！', `createTime` = '2025-05-02 18:18:23', `updateTime` = '2025-05-02 18:18:46' WHERE `id` = 1918248699129966594;

-- 2025-07-03
UPDATE `fish`.`hero` SET `ename` = '584', `cname` = '元流之子(射手)', `title` = '沉舟之志', `releaseDate` = '2025-04-23', `newType` = 0, `primaryType` = 5, `secondaryType` = NULL, `skins` = '沉舟之志', `officialLink` = '//pvp.qq.com/ingame/all/tobe/newheros/0627yuanliuzhizi.html', `mossId` = 8744, `race` = '人类', `faction` = '稷下学院', `identity` = '稷下学生', `region` = '逐鹿', `ability` = '武道，魔道', `height` = '176/163cm', `quote` = '稷下，我回来了。', `createTime` = '2025-05-02 18:18:23', `updateTime` = '2025-07-03 14:23:27' WHERE `id` = 1918248699129966602;
INSERT INTO `hero` VALUES (1918248699129966603, '151', '孙权', '定旌之谋', '2025-07-03', 1, 5, NULL, '定旌之谋', '//pvp.qq.com/ingame/all/tobe/newheros/0703sunquan.html', 7004, '人类', '吴', '江郡新主', '三分之地', '武道', '172cm', '为帅者，当先计其败，后计其成。', '2025-07-03 14:18:23', '2025-07-03 14:18:23');
-- 2025-09-25
UPDATE `hero` SET newType = 0 WHERE `id` = 1918248699129966603;
INSERT INTO `hero` VALUES (1918248699129966604, '172', '蚩奼', '五兵之主', '2025-09-25', 1, 1, NULL, '五兵之主', '//pvp.qq.com/ingame/all/tobe/newheros/chicha.html', 7123, '人类', '吴', '归山一族', '建木', '武道', '155cm', '老话说“身小难举器，躯高可破天”，我可不信。', '2025-09-25 14:18:23', '2025-09-25 14:18:23');

/*
 Navicat Premium Data Transfer

 Source Server         : localhost
 Source Server Type    : MySQL
 Source Server Version : 50733
 Source Host           : localhost:3306
 Source Schema         : fish

 Target Server Type    : MySQL
 Target Server Version : 50733
 File Encoding         : 65001

 Date: 28/07/2025 12:53:35
*/

SET NAMES utf8mb4;
SET FOREIGN_KEY_CHECKS = 0;

-- ----------------------------
-- Records of word_library
-- ----------------------------
INSERT INTO `word_library` VALUES (1907331092369625001, '榴莲', 'draw-default', '水果', '2025-07-25 15:39:19', '2025-07-28 11:42:01');
INSERT INTO `word_library` VALUES (1907331092369625002, '香蕉', 'draw-default', '水果', '2025-07-25 15:39:19', '2025-07-25 15:39:19');
INSERT INTO `word_library` VALUES (1907331092369625003, '橙子', 'draw-default', '水果', '2025-07-25 15:39:19', '2025-07-25 15:39:19');
INSERT INTO `word_library` VALUES (1907331092369625004, '西瓜', 'draw-default', '水果', '2025-07-25 15:39:19', '2025-07-25 15:39:19');
INSERT INTO `word_library` VALUES (1907331092369625005, '猫咪', 'draw-default', '动物', '2025-07-25 15:39:19', '2025-07-25 15:39:19');
INSERT INTO `word_library` VALUES (1907331092369625006, '小狗', 'draw-default', '动物', '2025-07-25 15:39:19', '2025-07-25 15:39:19');
INSERT INTO `word_library` VALUES (1907331092369625007, '老虎', 'draw-default', '动物', '2025-07-25 15:39:19', '2025-07-25 15:39:19');
INSERT INTO `word_library` VALUES (1907331092369625008, '狮子', 'draw-default', '动物', '2025-07-25 15:39:19', '2025-07-25 15:39:19');
INSERT INTO `word_library` VALUES (1907331092369625009, '大象', 'draw-default', '动物', '2025-07-25 15:39:19', '2025-07-25 15:39:19');
INSERT INTO `word_library` VALUES (1907331092369625010, '熊猫', 'draw-default', '动物', '2025-07-25 15:39:19', '2025-07-25 15:39:19');
INSERT INTO `word_library` VALUES (1907331092369625011, '笔记本', 'draw-default', '电子产品', '2025-07-25 15:39:19', '2025-07-25 15:39:19');
INSERT INTO `word_library` VALUES (1907331092369625012, '手机', 'draw-default', '电子产品', '2025-07-25 15:39:19', '2025-07-25 15:39:19');
INSERT INTO `word_library` VALUES (1907331092369625013, '电视', 'draw-default', '电子产品', '2025-07-25 15:39:19', '2025-07-25 15:39:19');
INSERT INTO `word_library` VALUES (1907331092369625014, '冰箱', 'draw-default', '家电', '2025-07-25 15:39:19', '2025-07-25 15:39:19');
INSERT INTO `word_library` VALUES (1907331092369625015, '沙发', 'draw-default', '家具', '2025-07-25 15:39:19', '2025-07-25 15:39:19');
INSERT INTO `word_library` VALUES (1907331092369625016, '飞机', 'draw-default', '交通工具', '2025-07-25 15:39:19', '2025-07-25 15:39:19');
INSERT INTO `word_library` VALUES (1907331092369625017, '汽车', 'draw-default', '交通工具', '2025-07-25 15:39:19', '2025-07-25 15:39:19');
INSERT INTO `word_library` VALUES (1907331092369625018, '火车', 'draw-default', '交通工具', '2025-07-25 15:39:19', '2025-07-25 15:39:19');
INSERT INTO `word_library` VALUES (1907331092369625019, '自行车', 'draw-default', '交通工具', '2025-07-25 15:39:19', '2025-07-25 15:39:19');
INSERT INTO `word_library` VALUES (1907331092369625020, '摩托车', 'draw-default', '交通工具', '2025-07-25 15:39:19', '2025-07-25 15:39:19');
INSERT INTO `word_library` VALUES (1907331092369625021, '足球', 'draw-default', '体育用品', '2025-07-25 15:39:19', '2025-07-25 15:39:19');
INSERT INTO `word_library` VALUES (1907331092369625022, '篮球', 'draw-default', '体育用品', '2025-07-25 15:39:19', '2025-07-25 15:39:19');
INSERT INTO `word_library` VALUES (1907331092369625023, '游泳', 'draw-default', '运动', '2025-07-25 15:39:19', '2025-07-25 15:39:19');
INSERT INTO `word_library` VALUES (1907331092369625024, '网球', 'draw-default', '体育用品', '2025-07-25 15:39:19', '2025-07-25 15:39:19');
INSERT INTO `word_library` VALUES (1907331092369625025, '跑步', 'draw-default', '运动', '2025-07-25 15:39:19', '2025-07-25 15:39:19');
INSERT INTO `word_library` VALUES (1907331092369625026, '爬山', 'draw-default', '运动', '2025-07-25 15:39:19', '2025-07-25 15:39:19');
INSERT INTO `word_library` VALUES (1907331092369625027, '海洋', 'draw-default', '自然景观', '2025-07-25 15:39:19', '2025-07-25 15:39:19');
INSERT INTO `word_library` VALUES (1907331092369625028, '森林', 'draw-default', '自然景观', '2025-07-25 15:39:19', '2025-07-25 15:39:19');
INSERT INTO `word_library` VALUES (1907331092369625029, '沙漠', 'draw-default', '自然景观', '2025-07-25 15:39:19', '2025-07-25 15:39:19');
INSERT INTO `word_library` VALUES (1907331092369625030, '草原', 'draw-default', '自然景观', '2025-07-25 15:39:19', '2025-07-25 15:39:19');
INSERT INTO `word_library` VALUES (1907331092369625031, '太阳', 'draw-default', '天体', '2025-07-25 15:39:19', '2025-07-25 15:39:19');
INSERT INTO `word_library` VALUES (1907331092369625032, '月亮', 'draw-default', '天体', '2025-07-25 15:39:19', '2025-07-25 15:39:19');
INSERT INTO `word_library` VALUES (1907331092369625033, '星星', 'draw-default', '天体', '2025-07-25 15:39:19', '2025-07-25 15:39:19');
INSERT INTO `word_library` VALUES (1907331092369625034, '雨伞', 'draw-default', '日用品', '2025-07-25 15:39:19', '2025-07-25 15:39:19');
INSERT INTO `word_library` VALUES (1907331092369625035, '雪花', 'draw-default', '自然现象', '2025-07-25 15:39:19', '2025-07-25 15:39:19');
INSERT INTO `word_library` VALUES (1907331092369625036, '钢琴', 'draw-default', '乐器', '2025-07-25 15:39:19', '2025-07-25 15:39:19');
INSERT INTO `word_library` VALUES (1907331092369625037, '吉他', 'draw-default', '乐器', '2025-07-25 15:39:19', '2025-07-25 15:39:19');
INSERT INTO `word_library` VALUES (1907331092369625038, '小提琴', 'draw-default', '乐器', '2025-07-25 15:39:19', '2025-07-25 15:39:19');
INSERT INTO `word_library` VALUES (1907331092369625039, '鼓', 'draw-default', '乐器', '2025-07-25 15:39:19', '2025-07-25 15:39:19');
INSERT INTO `word_library` VALUES (1907331092369625040, '话筒', 'draw-default', '电子设备', '2025-07-25 15:39:19', '2025-07-25 15:39:19');
INSERT INTO `word_library` VALUES (1907331092369625041, '眼镜', 'draw-default', '日用品', '2025-07-25 15:39:19', '2025-07-25 15:39:19');
INSERT INTO `word_library` VALUES (1907331092369625042, '帽子', 'draw-default', '服饰', '2025-07-25 15:39:19', '2025-07-25 15:39:19');
INSERT INTO `word_library` VALUES (1907331092369625043, '手表', 'draw-default', '饰品', '2025-07-25 15:39:19', '2025-07-25 15:39:19');
INSERT INTO `word_library` VALUES (1907331092369625044, '鞋子', 'draw-default', '服饰', '2025-07-25 15:39:19', '2025-07-25 15:39:19');
INSERT INTO `word_library` VALUES (1907331092369625045, '衬衫', 'draw-default', '服饰', '2025-07-25 15:39:19', '2025-07-25 15:39:19');
INSERT INTO `word_library` VALUES (1907331092369625046, '裤子', 'draw-default', '服饰', '2025-07-25 15:39:19', '2025-07-25 15:39:19');
INSERT INTO `word_library` VALUES (1907331092369625047, '书桌', 'draw-default', '家具', '2025-07-25 15:39:19', '2025-07-25 15:39:19');
INSERT INTO `word_library` VALUES (1907331092369625048, '椅子', 'draw-default', '家具', '2025-07-25 15:39:19', '2025-07-25 15:39:19');
INSERT INTO `word_library` VALUES (1907331092369625049, '床', 'draw-default', '家具', '2025-07-25 15:39:19', '2025-07-25 15:39:19');
INSERT INTO `word_library` VALUES (1907331092369625050, '窗帘', 'draw-default', '家居用品', '2025-07-25 15:39:19', '2025-07-25 15:39:19');
INSERT INTO `word_library` VALUES (1907331092369625051, '电脑', 'draw-default', '电子产品', '2025-07-25 15:39:19', '2025-07-25 15:39:19');
INSERT INTO `word_library` VALUES (1907331092369625052, '键盘', 'draw-default', '电子产品', '2025-07-25 15:39:19', '2025-07-25 15:39:19');
INSERT INTO `word_library` VALUES (1907331092369625053, '鼠标', 'draw-default', '电子产品', '2025-07-25 15:39:19', '2025-07-25 15:39:19');
INSERT INTO `word_library` VALUES (1907331092369625054, '耳机', 'draw-default', '电子产品', '2025-07-25 15:39:19', '2025-07-25 15:39:19');
INSERT INTO `word_library` VALUES (1907331092369625055, '相机', 'draw-default', '电子产品', '2025-07-25 15:39:19', '2025-07-25 15:39:19');
INSERT INTO `word_library` VALUES (1907331092369625057, '水杯', 'draw-default', '餐具', '2025-07-25 15:39:19', '2025-07-25 15:39:19');
INSERT INTO `word_library` VALUES (1907331092369625058, '咖啡', 'draw-default', '饮料', '2025-07-25 15:39:19', '2025-07-25 15:39:19');
INSERT INTO `word_library` VALUES (1907331092369625059, '面包', 'draw-default', '食物', '2025-07-25 15:39:19', '2025-07-25 15:39:19');
INSERT INTO `word_library` VALUES (1907331092369625060, '牛奶', 'draw-default', '饮料', '2025-07-25 15:39:19', '2025-07-25 15:39:19');
INSERT INTO `word_library` VALUES (1907331092369625061, '蛋糕', 'draw-default', '食物', '2025-07-25 15:39:19', '2025-07-25 15:39:19');
INSERT INTO `word_library` VALUES (1907331092369625062, '披萨', 'draw-default', '食物', '2025-07-25 15:39:19', '2025-07-25 15:39:19');
INSERT INTO `word_library` VALUES (1907331092369625063, '汉堡', 'draw-default', '食物', '2025-07-25 15:39:19', '2025-07-25 15:39:19');
INSERT INTO `word_library` VALUES (1907331092369625064, '薯条', 'draw-default', '食物', '2025-07-25 15:39:19', '2025-07-25 15:39:19');
INSERT INTO `word_library` VALUES (1907331092369625065, '鸡蛋', 'draw-default', '食物', '2025-07-25 15:39:19', '2025-07-25 15:39:19');
INSERT INTO `word_library` VALUES (1907331092369625066, '巧克力', 'draw-default', '食物', '2025-07-25 15:39:19', '2025-07-25 15:39:19');
INSERT INTO `word_library` VALUES (1907331092369625067, '蝴蝶', 'draw-default', '昆虫', '2025-07-25 15:39:19', '2025-07-25 15:39:19');
INSERT INTO `word_library` VALUES (1907331092369625068, '蜜蜂', 'draw-default', '昆虫', '2025-07-25 15:39:19', '2025-07-25 15:39:19');
INSERT INTO `word_library` VALUES (1907331092369625069, '蚂蚁', 'draw-default', '昆虫', '2025-07-25 15:39:19', '2025-07-25 15:39:19');
INSERT INTO `word_library` VALUES (1907331092369625070, '蜘蛛', 'draw-default', '昆虫', '2025-07-25 15:39:19', '2025-07-25 15:39:19');
INSERT INTO `word_library` VALUES (1907331092369625071, '青蛙', 'draw-default', '两栖动物', '2025-07-25 15:39:19', '2025-07-25 15:39:19');
INSERT INTO `word_library` VALUES (1907331092369625072, '海豚', 'draw-default', '海洋动物', '2025-07-25 15:39:19', '2025-07-25 15:39:19');
INSERT INTO `word_library` VALUES (1907331092369625073, '鲨鱼', 'draw-default', '海洋动物', '2025-07-25 15:39:19', '2025-07-25 15:39:19');
INSERT INTO `word_library` VALUES (1907331092369625074, '鸭子', 'draw-default', '鸟类', '2025-07-25 15:39:19', '2025-07-25 15:39:19');
INSERT INTO `word_library` VALUES (1907331092369625075, '企鹅', 'draw-default', '鸟类', '2025-07-25 15:39:19', '2025-07-25 15:39:19');
INSERT INTO `word_library` VALUES (1907331092369625076, '猴子', 'draw-default', '动物', '2025-07-25 15:39:19', '2025-07-25 15:39:19');
INSERT INTO `word_library` VALUES (1907331092369625077, '长颈鹿', 'draw-default', '动物', '2025-07-25 15:39:19', '2025-07-25 15:39:19');
INSERT INTO `word_library` VALUES (1907331092369625078, '仙人掌', 'draw-default', '植物', '2025-07-25 15:39:19', '2025-07-25 15:39:19');
INSERT INTO `word_library` VALUES (1907331092369625079, '向日葵', 'draw-default', '植物', '2025-07-25 15:39:19', '2025-07-25 15:39:19');
INSERT INTO `word_library` VALUES (1907331092369625080, '玫瑰', 'draw-default', '花卉', '2025-07-25 15:39:19', '2025-07-25 15:39:19');
INSERT INTO `word_library` VALUES (1907331092369625081, '樱花', 'draw-default', '花卉', '2025-07-25 15:39:19', '2025-07-25 15:39:19');
INSERT INTO `word_library` VALUES (1907331092369625082, '松树', 'draw-default', '植物', '2025-07-25 15:39:19', '2025-07-25 15:39:19');
INSERT INTO `word_library` VALUES (1907331092369625083, '竹子', 'draw-default', '植物', '2025-07-25 15:39:19', '2025-07-25 15:39:19');
INSERT INTO `word_library` VALUES (1907331092369625084, '草莓', 'draw-default', '水果', '2025-07-25 15:39:19', '2025-07-25 15:39:19');
INSERT INTO `word_library` VALUES (1907331092369625085, '西红柿', 'draw-default', '蔬菜', '2025-07-25 15:39:19', '2025-07-25 15:39:19');
INSERT INTO `word_library` VALUES (1907331092369625086, '黄瓜', 'draw-default', '蔬菜', '2025-07-25 15:39:19', '2025-07-25 15:39:19');
INSERT INTO `word_library` VALUES (1907331092369625087, '辣椒', 'draw-default', '蔬菜', '2025-07-25 15:39:19', '2025-07-25 15:39:19');
INSERT INTO `word_library` VALUES (1907331092369625088, '南瓜', 'draw-default', '蔬菜', '2025-07-25 15:39:19', '2025-07-25 15:39:19');
INSERT INTO `word_library` VALUES (1907331092369625089, '月饼', 'draw-default', '食物', '2025-07-25 15:39:19', '2025-07-25 15:39:19');
INSERT INTO `word_library` VALUES (1907331092369625090, '元宵', 'draw-default', '食物', '2025-07-25 15:39:19', '2025-07-25 15:39:19');
INSERT INTO `word_library` VALUES (1907331092369625091, '饺子', 'draw-default', '食物', '2025-07-25 15:39:19', '2025-07-25 15:39:19');
INSERT INTO `word_library` VALUES (1907331092369625092, '粽子', 'draw-default', '食物', '2025-07-25 15:39:19', '2025-07-25 15:39:19');
INSERT INTO `word_library` VALUES (1907331092369625093, '火锅', 'draw-default', '食物', '2025-07-25 15:39:19', '2025-07-25 15:39:19');
INSERT INTO `word_library` VALUES (1907331092369625094, '烧烤', 'draw-default', '食物', '2025-07-25 15:39:19', '2025-07-25 15:39:19');
INSERT INTO `word_library` VALUES (1907331092369625095, '豆腐', 'draw-default', '食物', '2025-07-25 15:39:19', '2025-07-25 15:39:19');
INSERT INTO `word_library` VALUES (1907331092369625096, '豆浆', 'draw-default', '饮料', '2025-07-25 15:39:19', '2025-07-25 15:39:19');
INSERT INTO `word_library` VALUES (1907331092369625097, '米饭', 'draw-default', '食物', '2025-07-25 15:39:19', '2025-07-25 15:39:19');
INSERT INTO `word_library` VALUES (1907331092369625098, '面条', 'draw-default', '食物', '2025-07-25 15:39:19', '2025-07-25 15:39:19');
INSERT INTO `word_library` VALUES (1907331092369625099, '中国', 'draw-default', '国家', '2025-07-25 15:39:19', '2025-07-25 15:39:19');
INSERT INTO `word_library` VALUES (1907331092369625100, '美国', 'draw-default', '国家', '2025-07-25 15:39:19', '2025-07-25 15:39:19');
INSERT INTO `word_library` VALUES (1907331092369625101, '日本', 'draw-default', '国家', '2025-07-25 15:39:19', '2025-07-25 15:39:19');
INSERT INTO `word_library` VALUES (1907331092369625102, '韩国', 'draw-default', '国家', '2025-07-25 15:39:19', '2025-07-25 15:39:19');
INSERT INTO `word_library` VALUES (1907331092369625103, '法国', 'draw-default', '国家', '2025-07-25 15:39:19', '2025-07-25 15:39:19');
INSERT INTO `word_library` VALUES (1907331092369625104, '德国', 'draw-default', '国家', '2025-07-25 15:39:19', '2025-07-25 15:39:19');
INSERT INTO `word_library` VALUES (1907331092369625105, '英国', 'draw-default', '国家', '2025-07-25 15:39:19', '2025-07-25 15:39:19');
INSERT INTO `word_library` VALUES (1907331092369625106, '俄罗斯', 'draw-default', '国家', '2025-07-25 15:39:19', '2025-07-25 15:39:19');
INSERT INTO `word_library` VALUES (1907331092369625107, '巴西', 'draw-default', '国家', '2025-07-25 15:39:19', '2025-07-25 15:39:19');
INSERT INTO `word_library` VALUES (1907331092369625108, '澳大利亚', 'draw-default', '国家', '2025-07-25 15:39:19', '2025-07-25 15:39:19');
INSERT INTO `word_library` VALUES (1907331092369625109, '长城', 'draw-default', '建筑', '2025-07-25 15:39:19', '2025-07-25 15:39:19');
INSERT INTO `word_library` VALUES (1907331092369625110, '故宫', 'draw-default', '建筑', '2025-07-25 15:39:19', '2025-07-25 15:39:19');
INSERT INTO `word_library` VALUES (1907331092369625111, '埃菲尔铁塔', 'draw-default', '建筑', '2025-07-25 15:39:19', '2025-07-25 15:39:19');
INSERT INTO `word_library` VALUES (1907331092369625112, '自由女神像', 'draw-default', '建筑', '2025-07-25 15:39:19', '2025-07-25 15:39:19');
INSERT INTO `word_library` VALUES (1907331092369625113, '金字塔', 'draw-default', '建筑', '2025-07-25 15:39:19', '2025-07-25 15:39:19');
INSERT INTO `word_library` VALUES (1907331092369625114, '泰姬陵', 'draw-default', '建筑', '2025-07-25 15:39:19', '2025-07-25 15:39:19');
INSERT INTO `word_library` VALUES (1907331092369625115, '圣诞节', 'draw-default', '节日', '2025-07-25 15:39:19', '2025-07-25 15:39:19');
INSERT INTO `word_library` VALUES (1907331092369625116, '春节', 'draw-default', '节日', '2025-07-25 15:39:19', '2025-07-25 15:39:19');
INSERT INTO `word_library` VALUES (1907331092369625117, '元宵节', 'draw-default', '节日', '2025-07-25 15:39:19', '2025-07-25 15:39:19');
INSERT INTO `word_library` VALUES (1907331092369625118, '中秋节', 'draw-default', '节日', '2025-07-25 15:39:19', '2025-07-25 15:39:19');
INSERT INTO `word_library` VALUES (1907331092369625119, '七夕', 'draw-default', '节日', '2025-07-25 15:39:19', '2025-07-25 15:39:19');
INSERT INTO `word_library` VALUES (1907331092369625120, '清明节', 'draw-default', '节日', '2025-07-25 15:39:19', '2025-07-25 15:39:19');
INSERT INTO `word_library` VALUES (1907331092369625121, '端午节', 'draw-default', '节日', '2025-07-25 15:39:19', '2025-07-25 15:39:19');
INSERT INTO `word_library` VALUES (1907331092369625122, '重阳节', 'draw-default', '节日', '2025-07-25 15:39:19', '2025-07-25 15:39:19');
INSERT INTO `word_library` VALUES (1907331092369625123, '万圣节', 'draw-default', '节日', '2025-07-25 15:39:19', '2025-07-25 15:39:19');
INSERT INTO `word_library` VALUES (1907331092369625124, '感恩节', 'draw-default', '节日', '2025-07-25 15:39:19', '2025-07-25 15:39:19');
INSERT INTO `word_library` VALUES (1907331092369625125, '语文书', 'draw-default', '学习用品', '2025-07-25 15:39:19', '2025-07-25 15:39:19');
INSERT INTO `word_library` VALUES (1907331092369625126, '魔术', 'draw-default', '表演艺术', '2025-07-25 15:39:19', '2025-07-25 15:39:19');
INSERT INTO `word_library` VALUES (1907331092369625127, '摇头晃脑', 'draw-default', '动作', '2025-07-25 15:39:19', '2025-07-25 15:39:19');
INSERT INTO `word_library` VALUES (1907331092369625128, '母亲', 'draw-default', '亲属', '2025-07-25 15:39:19', '2025-07-25 15:39:19');
INSERT INTO `word_library` VALUES (1907331092369625129, '豆沙包', 'draw-default', '食物', '2025-07-25 15:39:19', '2025-07-25 15:39:19');
INSERT INTO `word_library` VALUES (1907331092369625130, '打草惊蛇', 'draw-default', '成语', '2025-07-25 15:39:19', '2025-07-25 15:39:19');
INSERT INTO `word_library` VALUES (1907331092369625131, '鸡鸣狗盗', 'draw-default', '成语', '2025-07-25 15:39:19', '2025-07-25 15:39:19');
INSERT INTO `word_library` VALUES (1907331092369625132, '鸡飞狗跳', 'draw-default', '成语', '2025-07-25 15:39:19', '2025-07-25 15:39:19');
INSERT INTO `word_library` VALUES (1907331092369625133, '结婚证', 'draw-default', '证件', '2025-07-25 15:39:19', '2025-07-25 15:39:19');
INSERT INTO `word_library` VALUES (1907331092369625134, '监狱', 'draw-default', '场所', '2025-07-25 15:39:19', '2025-07-25 15:39:19');
INSERT INTO `word_library` VALUES (1907331092369625135, '情非得已', 'draw-default', '成语', '2025-07-25 15:39:19', '2025-07-25 15:39:19');
INSERT INTO `word_library` VALUES (1907331092369625136, '饮水思源', 'draw-default', '成语', '2025-07-25 15:39:19', '2025-07-25 15:39:19');
INSERT INTO `word_library` VALUES (1907331092369625137, '沙拉酱', 'draw-default', '调味品', '2025-07-25 15:39:19', '2025-07-25 15:39:19');
INSERT INTO `word_library` VALUES (1907331092369625138, '风卷残云', 'draw-default', '成语', '2025-07-25 15:39:19', '2025-07-25 15:39:19');
INSERT INTO `word_library` VALUES (1907331092369625139, '君子好逑', 'draw-default', '成语', '2025-07-25 15:39:19', '2025-07-25 15:39:19');
INSERT INTO `word_library` VALUES (1907331092369625140, '地球仪', 'draw-default', '教学用品', '2025-07-25 15:39:19', '2025-07-25 15:39:19');
INSERT INTO `word_library` VALUES (1907331092369625141, '相见恨晚', 'draw-default', '成语', '2025-07-25 15:39:19', '2025-07-25 15:39:19');
INSERT INTO `word_library` VALUES (1907331092369625142, '教科书', 'draw-default', '学习用品', '2025-07-25 15:39:19', '2025-07-25 15:39:19');
INSERT INTO `word_library` VALUES (1907331092369625143, '古筝', 'draw-default', '乐器', '2025-07-25 15:39:19', '2025-07-25 15:39:19');
INSERT INTO `word_library` VALUES (1907331092369625144, '黑色', 'draw-default', '颜色', '2025-07-25 15:39:19', '2025-07-25 15:39:19');
INSERT INTO `word_library` VALUES (1907331092369625145, '电梯', 'draw-default', '设备', '2025-07-25 15:39:19', '2025-07-25 15:39:19');
INSERT INTO `word_library` VALUES (1907331092369625146, '中大奖', 'draw-default', '事件', '2025-07-25 15:39:19', '2025-07-25 15:39:19');
INSERT INTO `word_library` VALUES (1907331092369625148, '毛驴酸辣粉', 'draw-default', '食物', '2025-07-25 15:39:19', '2025-07-25 15:39:19');
INSERT INTO `word_library` VALUES (1907331092369625149, '冬瓜', 'draw-default', '蔬菜', '2025-07-25 15:39:19', '2025-07-25 15:39:19');
INSERT INTO `word_library` VALUES (1907331092369625150, '牛', 'draw-default', '动物', '2025-07-25 15:39:19', '2025-07-25 15:39:19');
INSERT INTO `word_library` VALUES (1907331092369625151, '大猩猩', 'draw-default', '动物', '2025-07-25 15:39:19', '2025-07-25 15:39:19');
INSERT INTO `word_library` VALUES (1907331092369625152, '乌贼', 'draw-default', '海洋动物', '2025-07-25 15:39:19', '2025-07-25 15:39:19');
INSERT INTO `word_library` VALUES (1907331092369625153, '橙', 'draw-default', '水果', '2025-07-25 15:39:19', '2025-07-25 15:39:19');
INSERT INTO `word_library` VALUES (1907331092369625154, '杏', 'draw-default', '水果', '2025-07-25 15:39:19', '2025-07-25 15:39:19');
INSERT INTO `word_library` VALUES (1907331092369625155, '海棠果', 'draw-default', '水果', '2025-07-25 15:39:19', '2025-07-25 15:39:19');
INSERT INTO `word_library` VALUES (1907331092369625156, '金桔', 'draw-default', '水果', '2025-07-25 15:39:19', '2025-07-25 15:39:19');
INSERT INTO `word_library` VALUES (1907331092369625157, '糯粑', 'draw-default', '食物', '2025-07-25 15:39:19', '2025-07-25 15:39:19');
INSERT INTO `word_library` VALUES (1907331092369625159, '对牛弹琴', 'draw-default', '成语', '2025-07-25 15:39:19', '2025-07-25 15:39:19');
INSERT INTO `word_library` VALUES (1907331092369625160, '和尚', 'draw-default', '职业', '2025-07-25 15:39:19', '2025-07-25 15:39:19');
INSERT INTO `word_library` VALUES (1907331092369625161, '冰糖葫芦', 'draw-default', '食物', '2025-07-25 15:39:19', '2025-07-25 15:39:19');
INSERT INTO `word_library` VALUES (1907331092369625162, '元旦', 'draw-default', '节日', '2025-07-25 15:39:19', '2025-07-25 15:39:19');
INSERT INTO `word_library` VALUES (1907331092369625163, '压岁钱', 'draw-default', '物品', '2025-07-25 15:39:19', '2025-07-25 15:39:19');
INSERT INTO `word_library` VALUES (1907331092369625164, '眼镜蛇', 'draw-default', '动物', '2025-07-25 15:39:19', '2025-07-25 15:39:19');
INSERT INTO `word_library` VALUES (1907331092369625165, '开心网', 'draw-default', '网站', '2025-07-25 15:39:19', '2025-07-25 15:39:19');
INSERT INTO `word_library` VALUES (1907331092369625166, '球拍', 'draw-default', '体育用品', '2025-07-25 15:39:19', '2025-07-25 15:39:19');
INSERT INTO `word_library` VALUES (1907331092369625168, '大兴安岭', 'draw-default', '地理', '2025-07-25 15:39:19', '2025-07-25 15:39:19');
INSERT INTO `word_library` VALUES (1907331092369625169, '考拉', 'draw-default', '动物', '2025-07-25 15:39:19', '2025-07-25 15:39:19');
INSERT INTO `word_library` VALUES (1907331092369625170, '跨栏', 'draw-default', '运动', '2025-07-25 15:39:19', '2025-07-25 15:39:19');
INSERT INTO `word_library` VALUES (1907331092369625171, '啃玉米', 'draw-default', '动作', '2025-07-25 15:39:19', '2025-07-25 15:39:19');
INSERT INTO `word_library` VALUES (1907331092369625172, '猫头鹰', 'draw-default', '鸟类', '2025-07-25 15:39:19', '2025-07-25 15:39:19');
INSERT INTO `word_library` VALUES (1907331092369625173, '蛋炒饭', 'draw-default', '食物', '2025-07-25 15:39:19', '2025-07-25 15:39:19');
INSERT INTO `word_library` VALUES (1907331092369625174, '开门', 'draw-default', '动作', '2025-07-25 15:39:19', '2025-07-25 15:39:19');
INSERT INTO `word_library` VALUES (1907331092369625175, '黑猫警长', 'draw-default', '动画角色', '2025-07-25 15:39:19', '2025-07-25 15:39:19');
INSERT INTO `word_library` VALUES (1907331092369625176, '环保', 'draw-default', '概念', '2025-07-25 15:39:19', '2025-07-25 15:39:19');
INSERT INTO `word_library` VALUES (1907331092369625177, '衣橱', 'draw-default', '家具', '2025-07-25 15:39:19', '2025-07-25 15:39:19');
INSERT INTO `word_library` VALUES (1907331092369625178, '单杠', 'draw-default', '体育器材', '2025-07-25 15:39:19', '2025-07-25 15:39:19');
INSERT INTO `word_library` VALUES (1907331092369625179, '三顾茅庐', 'draw-default', '成语', '2025-07-25 15:39:19', '2025-07-25 15:39:19');
INSERT INTO `word_library` VALUES (1907331092369625180, '口是心非', 'draw-default', '成语', '2025-07-25 15:39:19', '2025-07-25 15:39:19');
INSERT INTO `word_library` VALUES (1907331092369625181, '筷子', 'draw-default', '餐具', '2025-07-25 15:39:19', '2025-07-25 15:39:19');
INSERT INTO `word_library` VALUES (1907331092369625182, '丢三落四', 'draw-default', '成语', '2025-07-25 15:39:19', '2025-07-25 15:39:19');
INSERT INTO `word_library` VALUES (1907331092369625183, '虎头蛇尾', 'draw-default', '成语', '2025-07-25 15:39:19', '2025-07-25 15:39:19');
INSERT INTO `word_library` VALUES (1907331092369625184, '指手划脚', 'draw-default', '成语', '2025-07-25 15:39:19', '2025-07-25 15:39:19');
INSERT INTO `word_library` VALUES (1907331092369625185, '小偷', 'draw-default', '职业', '2025-07-25 15:39:19', '2025-07-25 15:39:19');
INSERT INTO `word_library` VALUES (1907331092369625186, '皮鞋', 'draw-default', '服饰', '2025-07-25 15:39:19', '2025-07-25 15:39:19');
INSERT INTO `word_library` VALUES (1907331092369625187, '打雷', 'draw-default', '自然现象', '2025-07-25 15:39:19', '2025-07-25 15:39:19');
INSERT INTO `word_library` VALUES (1907331092369625188, '哪吒', 'draw-default', '神话人物', '2025-07-25 15:39:19', '2025-07-25 15:39:19');
INSERT INTO `word_library` VALUES (1907331092369625189, '画饼充饥', 'draw-default', '成语', '2025-07-25 15:39:19', '2025-07-25 15:39:19');
INSERT INTO `word_library` VALUES (1907331092369625190, '掩耳盗铃', 'draw-default', '成语', '2025-07-25 15:39:19', '2025-07-25 15:39:19');
INSERT INTO `word_library` VALUES (1907331092369625191, '火柴', 'draw-default', '日用品', '2025-07-25 15:39:19', '2025-07-25 15:39:19');
INSERT INTO `word_library` VALUES (1907331092369625192, '卧虎藏龙', 'draw-default', '成语', '2025-07-25 15:39:19', '2025-07-25 15:39:19');
INSERT INTO `word_library` VALUES (1907331092369625193, '郎才女貌', 'draw-default', '成语', '2025-07-25 15:39:19', '2025-07-25 15:39:19');
INSERT INTO `word_library` VALUES (1907331092369625194, '花朵', 'draw-default', '植物', '2025-07-25 15:39:19', '2025-07-25 15:39:19');
INSERT INTO `word_library` VALUES (1907331092369625195, '帅呆了', 'draw-default', '形容词', '2025-07-25 15:39:19', '2025-07-25 15:39:19');
INSERT INTO `word_library` VALUES (1907331092369625196, '论功行赏', 'draw-default', '成语', '2025-07-25 15:39:19', '2025-07-25 15:39:19');
INSERT INTO `word_library` VALUES (1907331092369625197, '洋葱', 'draw-default', '蔬菜', '2025-07-25 15:39:19', '2025-07-25 15:39:19');
INSERT INTO `word_library` VALUES (1907331092369625198, '狂风暴雨', 'draw-default', '自然现象', '2025-07-25 15:39:19', '2025-07-25 15:39:19');
INSERT INTO `word_library` VALUES (1907331092369625199, '蝙蝠', 'draw-default', '动物', '2025-07-25 15:39:19', '2025-07-25 15:39:19');
INSERT INTO `word_library` VALUES (1907331092369625200, '打火机', 'draw-default', '日用品', '2025-07-25 15:39:19', '2025-07-25 15:39:19');
INSERT INTO `word_library` VALUES (1907331092369625201, '火龙果', 'draw-default', '水果', '2025-07-25 15:39:19', '2025-07-25 15:39:19');
INSERT INTO `word_library` VALUES (1907331092369625202, '极乐世界', 'draw-default', '宗教概念', '2025-07-25 15:39:19', '2025-07-25 15:39:19');
INSERT INTO `word_library` VALUES (1907331092369625203, '东倒西歪', 'draw-default', '成语', '2025-07-25 15:39:19', '2025-07-25 15:39:19');
INSERT INTO `word_library` VALUES (1907331092369625204, '方便面', 'draw-default', '食物', '2025-07-25 15:39:19', '2025-07-25 15:39:19');
INSERT INTO `word_library` VALUES (1907331092369625205, '照镜子', 'draw-default', '动作', '2025-07-25 15:39:19', '2025-07-25 15:39:19');
INSERT INTO `word_library` VALUES (1907331092369625206, '牛郎织女', 'draw-default', '神话人物', '2025-07-25 15:39:19', '2025-07-25 15:39:19');
INSERT INTO `word_library` VALUES (1907331092369625207, '狼吞虎咽', 'draw-default', '成语', '2025-07-25 15:39:19', '2025-07-25 15:39:19');
INSERT INTO `word_library` VALUES (1907331092369625208, '赤壁', 'draw-default', '地名', '2025-07-25 15:39:19', '2025-07-25 15:39:19');
INSERT INTO `word_library` VALUES (1907331092369625209, '周杰伦', 'draw-default', '歌手', '2025-07-25 15:39:19', '2025-07-25 15:39:19');
INSERT INTO `word_library` VALUES (1907331092369625210, '姚明', 'draw-default', '运动员', '2025-07-25 15:39:19', '2025-07-25 15:39:19');
INSERT INTO `word_library` VALUES (1907331092369625211, '乒乓球', 'draw-default', '体育用品', '2025-07-25 15:39:19', '2025-07-25 15:39:19');
INSERT INTO `word_library` VALUES (1907331092369625212, '张牙舞爪', 'draw-default', '成语', '2025-07-25 15:39:19', '2025-07-25 15:39:19');
INSERT INTO `word_library` VALUES (1907331092369625213, '开怀大笑', 'draw-default', '动作', '2025-07-25 15:39:19', '2025-07-25 15:39:19');
INSERT INTO `word_library` VALUES (1907331092369625214, '唇亡齿寒', 'draw-default', '成语', '2025-07-25 15:39:19', '2025-07-25 15:39:19');
INSERT INTO `word_library` VALUES (1907331092369625215, '饥寒交迫', 'draw-default', '成语', '2025-07-25 15:39:19', '2025-07-25 15:39:19');
INSERT INTO `word_library` VALUES (1907331092369625217, '暗送秋波', 'draw-default', '成语', '2025-07-25 15:39:19', '2025-07-25 15:39:19');
INSERT INTO `word_library` VALUES (1907331092369625218, '照片', 'draw-default', '物品', '2025-07-25 15:39:19', '2025-07-25 15:39:19');
INSERT INTO `word_library` VALUES (1907331092369625219, '国王', 'draw-default', '职位', '2025-07-25 15:39:19', '2025-07-25 15:39:19');
INSERT INTO `word_library` VALUES (1907331092369625220, '索尼', 'draw-default', '品牌', '2025-07-25 15:39:19', '2025-07-25 15:39:19');
INSERT INTO `word_library` VALUES (1907331092369625221, '台灯', 'draw-default', '家居用品', '2025-07-25 15:39:19', '2025-07-25 15:39:19');
INSERT INTO `word_library` VALUES (1907331092369625222, '秋裤', 'draw-default', '服饰', '2025-07-25 15:39:19', '2025-07-25 15:39:19');
INSERT INTO `word_library` VALUES (1907331092369625223, '螺丝刀', 'draw-default', '工具', '2025-07-25 15:39:19', '2025-07-25 15:39:19');
INSERT INTO `word_library` VALUES (1907331092369625224, '恐龙', 'draw-default', '动物', '2025-07-25 15:39:19', '2025-07-25 15:39:19');
INSERT INTO `word_library` VALUES (1907331092369625225, '吃面条', 'draw-default', '动作', '2025-07-25 15:39:19', '2025-07-25 15:39:19');
INSERT INTO `word_library` VALUES (1907331092369625226, '灯笼', 'draw-default', '物品', '2025-07-25 15:39:19', '2025-07-25 15:39:19');
INSERT INTO `word_library` VALUES (1907331092369625227, '馒头', 'draw-default', '食物', '2025-07-25 15:39:19', '2025-07-25 15:39:19');
INSERT INTO `word_library` VALUES (1907331092369625229, '空格键', 'draw-default', '电脑部件', '2025-07-25 15:39:19', '2025-07-25 15:39:19');
INSERT INTO `word_library` VALUES (1907331092369625230, '螃蟹', 'draw-default', '海洋动物', '2025-07-25 15:39:19', '2025-07-25 15:39:19');
INSERT INTO `word_library` VALUES (1907331092369625231, '山楂', 'draw-default', '水果', '2025-07-25 15:39:19', '2025-07-25 15:39:19');
INSERT INTO `word_library` VALUES (1907331092369625233, '皮笑肉不笑', 'draw-default', '成语', '2025-07-25 15:39:19', '2025-07-25 15:39:19');
INSERT INTO `word_library` VALUES (1907331092369625234, '狗熊', 'draw-default', '动物', '2025-07-25 15:39:19', '2025-07-25 15:39:19');
INSERT INTO `word_library` VALUES (1907331092369625235, '冰淇淋', 'draw-default', '食物', '2025-07-25 15:39:19', '2025-07-25 15:39:19');
INSERT INTO `word_library` VALUES (1907331092369625236, '大跌眼镜', 'draw-default', '成语', '2025-07-25 15:39:19', '2025-07-25 15:39:19');
INSERT INTO `word_library` VALUES (1907331092369625238, '孟母三迁', 'draw-default', '成语', '2025-07-25 15:39:19', '2025-07-25 15:39:19');
INSERT INTO `word_library` VALUES (1907331092369625239, '风筝', 'draw-default', '玩具', '2025-07-25 15:39:19', '2025-07-25 15:39:19');
INSERT INTO `word_library` VALUES (1907331092369625240, '铅笔', 'draw-default', '文具', '2025-07-25 15:39:19', '2025-07-25 15:39:19');
INSERT INTO `word_library` VALUES (1907331092369625243, '空调', 'draw-default', '家电', '2025-07-25 15:39:19', '2025-07-25 15:39:19');
INSERT INTO `word_library` VALUES (1907331092369625244, '沸羊羊', 'draw-default', '动画角色', '2025-07-25 15:39:19', '2025-07-25 15:39:19');
INSERT INTO `word_library` VALUES (1907331092369625245, '走马看花', 'draw-default', '成语', '2025-07-25 15:39:19', '2025-07-25 15:39:19');
INSERT INTO `word_library` VALUES (1907331092369625246, '闻鸡起舞', 'draw-default', '成语', '2025-07-25 15:39:19', '2025-07-25 15:39:19');
INSERT INTO `word_library` VALUES (1907331092369625247, '护身符', 'draw-default', '物品', '2025-07-25 15:39:19', '2025-07-25 15:39:19');
INSERT INTO `word_library` VALUES (1907331092369625248, '垂头丧气', 'draw-default', '成语', '2025-07-25 15:39:19', '2025-07-25 15:39:19');
INSERT INTO `word_library` VALUES (1907331092369625250, '股票', 'draw-default', '金融', '2025-07-25 15:39:19', '2025-07-25 15:39:19');
INSERT INTO `word_library` VALUES (1907331092369625251, '电吹风', 'draw-default', '家电', '2025-07-25 15:39:19', '2025-07-25 15:39:19');
INSERT INTO `word_library` VALUES (1907331092369625253, '破涕为笑', 'draw-default', '成语', '2025-07-25 15:39:19', '2025-07-25 15:39:19');
INSERT INTO `word_library` VALUES (1907331092369625255, '针筒', 'draw-default', '医疗器械', '2025-07-25 15:39:19', '2025-07-25 15:39:19');
INSERT INTO `word_library` VALUES (1907331092369625256, '认错', 'draw-default', '动作', '2025-07-25 15:39:19', '2025-07-25 15:39:19');
INSERT INTO `word_library` VALUES (1907331092369625257, '黑哨', 'draw-default', '体育术语', '2025-07-25 15:39:19', '2025-07-25 15:39:19');
INSERT INTO `word_library` VALUES (1907331092369625258, '打气排球', 'draw-default', '运动', '2025-07-25 15:39:19', '2025-07-25 15:39:19');
INSERT INTO `word_library` VALUES (1907331092369625259, '打乒乓球', 'draw-default', '运动', '2025-07-25 15:39:19', '2025-07-25 15:39:19');
INSERT INTO `word_library` VALUES (1907331092369625260, '相濡以沫', 'draw-default', '成语', '2025-07-25 15:39:19', '2025-07-25 15:39:19');
INSERT INTO `word_library` VALUES (1907331092369625261, '殊途同归', 'draw-default', '成语', '2025-07-25 15:39:19', '2025-07-25 15:39:19');
INSERT INTO `word_library` VALUES (1907331092369625262, '网址', 'draw-default', '互联网术语', '2025-07-25 15:39:19', '2025-07-25 15:39:19');
INSERT INTO `word_library` VALUES (1907331092369625263, '百里挑一', 'draw-default', '成语', '2025-07-25 15:39:19', '2025-07-25 15:39:19');
INSERT INTO `word_library` VALUES (1907331092369625264, '拖地板', 'draw-default', '家务', '2025-07-25 15:39:19', '2025-07-25 15:39:19');
INSERT INTO `word_library` VALUES (1907331092369625265, '耳目一新', 'draw-default', '成语', '2025-07-25 15:39:19', '2025-07-25 15:39:19');
INSERT INTO `word_library` VALUES (1907331092369625266, '摔跤', 'draw-default', '运动', '2025-07-25 15:39:19', '2025-07-25 15:39:19');
INSERT INTO `word_library` VALUES (1907331092369625267, '一见钟情', 'draw-default', '成语', '2025-07-25 15:39:19', '2025-07-25 15:39:19');
INSERT INTO `word_library` VALUES (1907331092369625268, '白日做梦', 'draw-default', '成语', '2025-07-25 15:39:19', '2025-07-25 15:39:19');
INSERT INTO `word_library` VALUES (1907331092369625269, '朝三暮四', 'draw-default', '成语', '2025-07-25 15:39:19', '2025-07-25 15:39:19');
INSERT INTO `word_library` VALUES (1907331092369625270, '刘德华', 'draw-default', '演员', '2025-07-25 15:39:19', '2025-07-25 15:39:19');
INSERT INTO `word_library` VALUES (1907331092369625271, '波斯猫', 'draw-default', '动物', '2025-07-25 15:39:19', '2025-07-25 15:39:19');
INSERT INTO `word_library` VALUES (1907331092369625272, '一泻千里', 'draw-default', '成语', '2025-07-25 15:39:19', '2025-07-25 15:39:19');
INSERT INTO `word_library` VALUES (1907331092369625273, '纪晓岚', 'draw-default', '历史人物', '2025-07-25 15:39:19', '2025-07-25 15:39:19');
INSERT INTO `word_library` VALUES (1907331092369625275, '皮带', 'draw-default', '服饰', '2025-07-25 15:39:19', '2025-07-25 15:39:19');
INSERT INTO `word_library` VALUES (1907331092369625276, '鼠目寸光', 'draw-default', '成语', '2025-07-25 15:39:19', '2025-07-25 15:39:19');
INSERT INTO `word_library` VALUES (1907331092369625277, '吹毛求疵', 'draw-default', '成语', '2025-07-25 15:39:19', '2025-07-25 15:39:19');
INSERT INTO `word_library` VALUES (1907331092369625278, '八仙过海', 'draw-default', '成语', '2025-07-25 15:39:19', '2025-07-25 15:39:19');
INSERT INTO `word_library` VALUES (1907331092369625280, '打麻将', 'draw-default', '游戏', '2025-07-25 15:39:19', '2025-07-25 15:39:19');
INSERT INTO `word_library` VALUES (1907331092369625281, '花花公子', 'draw-default', '杂志', '2025-07-25 15:39:19', '2025-07-25 15:39:19');
INSERT INTO `word_library` VALUES (1907331092369625282, '武术', 'draw-default', '运动', '2025-07-25 15:39:19', '2025-07-25 15:39:19');
INSERT INTO `word_library` VALUES (1907331092369625284, '树大招风', 'draw-default', '成语', '2025-07-25 15:39:19', '2025-07-25 15:39:19');
INSERT INTO `word_library` VALUES (1907331092369625285, '皆大欢喜', 'draw-default', '成语', '2025-07-25 15:39:19', '2025-07-25 15:39:19');
INSERT INTO `word_library` VALUES (1907331092369625286, '火鸡', 'draw-default', '动物', '2025-07-25 15:39:19', '2025-07-25 15:39:19');
INSERT INTO `word_library` VALUES (1907331092369625287, '薯片', 'draw-default', '食物', '2025-07-25 15:39:19', '2025-07-25 15:39:19');
INSERT INTO `word_library` VALUES (1907331092369625288, 'CD', 'draw-default', '存储介质', '2025-07-25 15:39:19', '2025-07-25 15:39:19');
INSERT INTO `word_library` VALUES (1907331092369625289, '橡皮', 'draw-default', '文具', '2025-07-25 15:39:19', '2025-07-25 15:39:19');
INSERT INTO `word_library` VALUES (1907331092369625290, '排球', 'draw-default', '体育用品', '2025-07-25 15:39:19', '2025-07-25 15:39:19');
INSERT INTO `word_library` VALUES (1907331092369625291, '灰色', 'draw-default', '颜色', '2025-07-25 15:39:19', '2025-07-25 15:39:19');
INSERT INTO `word_library` VALUES (1907331092369625292, '郭芙蓉', 'draw-default', '影视角色', '2025-07-25 15:39:19', '2025-07-25 15:39:19');
INSERT INTO `word_library` VALUES (1907331092369625293, '热带鱼', 'draw-default', '动物', '2025-07-25 15:39:19', '2025-07-25 15:39:19');
INSERT INTO `word_library` VALUES (1907331092369625294, '鹤立鸡群', 'draw-default', '成语', '2025-07-25 15:39:19', '2025-07-25 15:39:19');
INSERT INTO `word_library` VALUES (1907331092369625297, '鳄鱼', 'draw-default', '动物', '2025-07-25 15:39:19', '2025-07-25 15:39:19');
INSERT INTO `word_library` VALUES (1907331092369625300, '柠檬', 'draw-default', '水果', '2025-07-25 15:39:19', '2025-07-25 15:39:19');
INSERT INTO `word_library` VALUES (1907331092369625301, '果仁', 'draw-default', '食物', '2025-07-25 15:39:19', '2025-07-25 15:39:19');
INSERT INTO `word_library` VALUES (1907331092369625302, '杨桃', 'draw-default', '水果', '2025-07-25 15:39:19', '2025-07-25 15:39:19');
INSERT INTO `word_library` VALUES (1907331092369625303, '柚子', 'draw-default', '水果', '2025-07-25 15:39:19', '2025-07-25 15:39:19');
INSERT INTO `word_library` VALUES (1907331092369625304, '小夹豆', 'draw-default', '食物', '2025-07-25 15:39:19', '2025-07-25 15:39:19');
INSERT INTO `word_library` VALUES (1907331092369625305, '东施效颦', 'draw-default', '成语', '2025-07-25 15:39:19', '2025-07-25 15:39:19');
INSERT INTO `word_library` VALUES (1907331092369625306, '爱哭鬼', 'draw-default', '性格特点', '2025-07-25 15:39:19', '2025-07-25 15:39:19');
INSERT INTO `word_library` VALUES (1907331092369625307, '遥控器', 'draw-default', '电子产品', '2025-07-25 15:39:19', '2025-07-25 15:39:19');
INSERT INTO `word_library` VALUES (1907331092369625308, '对牛谈琴', 'draw-default', '成语', '2025-07-25 15:39:19', '2025-07-25 15:39:19');
INSERT INTO `word_library` VALUES (1907331092369625310, '暴跳如雷', 'draw-default', '成语', '2025-07-25 15:39:19', '2025-07-25 15:39:19');
INSERT INTO `word_library` VALUES (1907331092369625311, '油条', 'draw-default', '食物', '2025-07-25 15:39:19', '2025-07-25 15:39:19');
INSERT INTO `word_library` VALUES (1907331092369625312, '土豆', 'draw-default', '蔬菜', '2025-07-25 15:39:19', '2025-07-25 15:39:19');
INSERT INTO `word_library` VALUES (1907331092369625313, '泣不成声', 'draw-default', '状态', '2025-07-25 15:39:19', '2025-07-25 15:39:19');
INSERT INTO `word_library` VALUES (1907331092369625315, '开车', 'draw-default', '动作', '2025-07-25 15:39:19', '2025-07-25 15:39:19');
INSERT INTO `word_library` VALUES (1907331092369625316, '跳棋', 'draw-default', '游戏', '2025-07-25 15:39:19', '2025-07-25 15:39:19');
INSERT INTO `word_library` VALUES (1907331092369625318, '围裙', 'draw-default', '服饰', '2025-07-25 15:39:19', '2025-07-25 15:39:19');
INSERT INTO `word_library` VALUES (1907331092369625319, '鹦鹉', 'draw-default', '鸟类', '2025-07-25 15:39:19', '2025-07-25 15:39:19');
INSERT INTO `word_library` VALUES (1907331092369625320, '落地灯', 'draw-default', '家居用品', '2025-07-25 15:39:19', '2025-07-25 15:39:19');
INSERT INTO `word_library` VALUES (1907331092369625321, '击剑', 'draw-default', '运动', '2025-07-25 15:39:19', '2025-07-25 15:39:19');
INSERT INTO `word_library` VALUES (1907331092369625322, '老鼠', 'draw-default', '动物', '2025-07-25 15:39:19', '2025-07-25 15:39:19');
INSERT INTO `word_library` VALUES (1907331092369625327, '指鹿为马', 'draw-default', '成语', '2025-07-25 15:39:19', '2025-07-25 15:39:19');
INSERT INTO `word_library` VALUES (1907331092369625328, '寒战', 'draw-default', '状态', '2025-07-25 15:39:19', '2025-07-25 15:39:19');
INSERT INTO `word_library` VALUES (1907331092369625330, '哭闹', 'draw-default', '动作', '2025-07-25 15:39:19', '2025-07-25 15:39:19');
INSERT INTO `word_library` VALUES (1907331092369625331, '左右开弓', 'draw-default', '成语', '2025-07-25 15:39:19', '2025-07-25 15:39:19');
INSERT INTO `word_library` VALUES (1907331092369625332, '饿虎扑食', 'draw-default', '成语', '2025-07-25 15:39:19', '2025-07-25 15:39:19');
INSERT INTO `word_library` VALUES (1907331092369625334, '熊掌', 'draw-default', '食物', '2025-07-25 15:39:19', '2025-07-25 15:39:19');
INSERT INTO `word_library` VALUES (1907331092369625335, '落井下石', 'draw-default', '成语', '2025-07-25 15:39:19', '2025-07-25 15:39:19');
INSERT INTO `word_library` VALUES (1907331092369625336, '絮絮叨叨', 'draw-default', '状态', '2025-07-25 15:39:19', '2025-07-25 15:39:19');
INSERT INTO `word_library` VALUES (1907331092369625337, '刘若英', 'draw-default', '歌手', '2025-07-25 15:39:19', '2025-07-25 15:39:19');
INSERT INTO `word_library` VALUES (1907331092369625339, '见钱眼开', 'draw-default', '成语', '2025-07-25 15:39:19', '2025-07-25 15:39:19');
INSERT INTO `word_library` VALUES (1907331092369625340, '词不达意', 'draw-default', '成语', '2025-07-25 15:39:19', '2025-07-25 15:39:19');
INSERT INTO `word_library` VALUES (1907331092369625341, '实验室', 'draw-default', '场所', '2025-07-25 15:39:19', '2025-07-25 15:39:19');
INSERT INTO `word_library` VALUES (1907331092369625342, '电热毯', 'draw-default', '家居用品', '2025-07-25 15:39:19', '2025-07-25 15:39:19');
INSERT INTO `word_library` VALUES (1907331092369625343, '画龙点睛', 'draw-default', '成语', '2025-07-25 15:39:19', '2025-07-25 15:39:19');
INSERT INTO `word_library` VALUES (1907331092369625345, '射击', 'draw-default', '运动', '2025-07-25 15:39:19', '2025-07-25 15:39:19');
INSERT INTO `word_library` VALUES (1907331092369625347, '天下第一', 'draw-default', '形容词', '2025-07-25 15:39:19', '2025-07-25 15:39:19');
INSERT INTO `word_library` VALUES (1907331092369625348, '猩', 'draw-default', '动物', '2025-07-25 15:39:19', '2025-07-25 15:39:19');
INSERT INTO `word_library` VALUES (1907331092369625349, '文房四宝', 'draw-default', '文化用品', '2025-07-25 15:39:19', '2025-07-25 15:39:19');
INSERT INTO `word_library` VALUES (1907331092369625350, '吃辣椒', 'draw-default', '动作', '2025-07-25 15:39:19', '2025-07-25 15:39:19');
INSERT INTO `word_library` VALUES (1907331092369625351, '钓', 'draw-default', '动作', '2025-07-25 15:39:19', '2025-07-25 15:39:19');
INSERT INTO `word_library` VALUES (1907331092369625352, '铅球', 'draw-default', '体育用品', '2025-07-25 15:39:19', '2025-07-25 15:39:19');
INSERT INTO `word_library` VALUES (1907331092369625353, '蜂蜜', 'draw-default', '食品', '2025-07-25 15:39:19', '2025-07-25 15:39:19');
INSERT INTO `word_library` VALUES (1907331092369625354, '上京赶考', 'draw-default', '事件', '2025-07-25 15:39:19', '2025-07-25 15:39:19');
INSERT INTO `word_library` VALUES (1907331092369625355, '凤梨', 'draw-default', '水果', '2025-07-25 15:39:19', '2025-07-25 15:39:19');
INSERT INTO `word_library` VALUES (1907331092369625356, '扫帚', 'draw-default', '清洁工具', '2025-07-25 15:39:19', '2025-07-25 15:39:19');
INSERT INTO `word_library` VALUES (1907331092369625359, '北极熊', 'draw-default', '动物', '2025-07-25 15:39:19', '2025-07-25 15:39:19');
INSERT INTO `word_library` VALUES (1907331092369625360, '指甲刀', 'draw-default', '日用品', '2025-07-25 15:39:19', '2025-07-25 15:39:19');
INSERT INTO `word_library` VALUES (1907331092369625363, '犀牛', 'draw-default', '动物', '2025-07-25 15:39:19', '2025-07-25 15:39:19');
INSERT INTO `word_library` VALUES (1907331092369625364, '三长两短', 'draw-default', '成语', '2025-07-25 15:39:19', '2025-07-25 15:39:19');
INSERT INTO `word_library` VALUES (1907331092369625365, '发热', 'draw-default', '症状', '2025-07-25 15:39:19', '2025-07-25 15:39:19');
INSERT INTO `word_library` VALUES (1907331092369625366, '师跃进', 'draw-default', '人名', '2025-07-25 15:39:19', '2025-07-25 15:39:19');
INSERT INTO `word_library` VALUES (1907331092369625367, '插线板', 'draw-default', '电器', '2025-07-25 15:39:19', '2025-07-25 15:39:19');
INSERT INTO `word_library` VALUES (1907331092369625368, '丹顶鹤', 'draw-default', '鸟类', '2025-07-25 15:39:19', '2025-07-25 15:39:19');
INSERT INTO `word_library` VALUES (1907331092369625369, '熊', 'draw-default', '动物', '2025-07-25 15:39:19', '2025-07-25 15:39:19');
INSERT INTO `word_library` VALUES (1907331092369625370, '鲍菇', 'draw-default', '食物', '2025-07-25 15:39:19', '2025-07-25 15:39:19');
INSERT INTO `word_library` VALUES (1907331092369625371, '跳水', 'draw-default', '运动', '2025-07-25 15:39:19', '2025-07-25 15:39:19');
INSERT INTO `word_library` VALUES (1907331092369625372, '东北虎', 'draw-default', '动物', '2025-07-25 15:39:19', '2025-07-25 15:39:19');
INSERT INTO `word_library` VALUES (1907331092369625373, '化学', 'draw-default', '学科', '2025-07-25 15:39:19', '2025-07-25 15:39:19');
INSERT INTO `word_library` VALUES (1907331092369625374, '老鹰', 'draw-default', '鸟类', '2025-07-25 15:39:19', '2025-07-25 15:39:19');
INSERT INTO `word_library` VALUES (1907331092369625375, '教师', 'draw-default', '职业', '2025-07-25 15:39:19', '2025-07-25 15:39:19');
INSERT INTO `word_library` VALUES (1907331092369625377, '虎入羊群', 'draw-default', '成语', '2025-07-25 15:39:19', '2025-07-25 15:39:19');
INSERT INTO `word_library` VALUES (1907331092369625378, '神气十足', 'draw-default', '状态', '2025-07-25 15:39:19', '2025-07-25 15:39:19');
INSERT INTO `word_library` VALUES (1907331092369625380, '蜻蜓', 'draw-default', '昆虫', '2025-07-25 15:39:19', '2025-07-25 15:39:19');
INSERT INTO `word_library` VALUES (1907331092369625381, '水波', 'draw-default', '自然现象', '2025-07-25 15:39:19', '2025-07-25 15:39:19');
INSERT INTO `word_library` VALUES (1907331092369625382, '嬉皮笑脸', 'draw-default', '状态', '2025-07-25 15:39:19', '2025-07-25 15:39:19');
INSERT INTO `word_library` VALUES (1907331092369625383, '红光满面', 'draw-default', '状态', '2025-07-25 15:39:19', '2025-07-25 15:39:19');
INSERT INTO `word_library` VALUES (1907331092369625384, '手术刀', 'draw-default', '医疗器械', '2025-07-25 15:39:19', '2025-07-25 15:39:19');
INSERT INTO `word_library` VALUES (1907331092369625385, '骑马', 'draw-default', '动作', '2025-07-25 15:39:19', '2025-07-25 15:39:19');
INSERT INTO `word_library` VALUES (1907331092369625386, '烟斗', 'draw-default', '物品', '2025-07-25 15:39:19', '2025-07-25 15:39:19');
INSERT INTO `word_library` VALUES (1907331092369625387, '自作主张', 'draw-default', '成语', '2025-07-25 15:39:19', '2025-07-25 15:39:19');
INSERT INTO `word_library` VALUES (1907331092369625388, '蚊子', 'draw-default', '昆虫', '2025-07-25 15:39:19', '2025-07-25 15:39:19');
INSERT INTO `word_library` VALUES (1907331092369625389, '西山', 'draw-default', '地名', '2025-07-25 15:39:19', '2025-07-25 15:39:19');
INSERT INTO `word_library` VALUES (1907331092369625390, '草船借箭', 'draw-default', '成语', '2025-07-25 15:39:19', '2025-07-25 15:39:19');
INSERT INTO `word_library` VALUES (1907331092369625392, '快马加鞭', 'draw-default', '成语', '2025-07-25 15:39:19', '2025-07-25 15:39:19');
INSERT INTO `word_library` VALUES (1907331092369625393, '金钱豹', 'draw-default', '动物', '2025-07-25 15:39:19', '2025-07-25 15:39:19');
INSERT INTO `word_library` VALUES (1907331092369625395, '跳伞', 'draw-default', '运动', '2025-07-25 15:39:19', '2025-07-25 15:39:19');
INSERT INTO `word_library` VALUES (1907331092369625396, '名字', 'draw-default', '概念', '2025-07-25 15:39:19', '2025-07-25 15:39:19');
INSERT INTO `word_library` VALUES (1907331092369625399, '电风扇', 'draw-default', '家电', '2025-07-25 15:39:19', '2025-07-25 15:39:19');
INSERT INTO `word_library` VALUES (1907331092369625404, '工资', 'draw-default', '概念', '2025-07-25 15:39:19', '2025-07-25 15:39:19');
INSERT INTO `word_library` VALUES (1907331092369625405, '菜刀', 'draw-default', '厨具', '2025-07-25 15:39:19', '2025-07-25 15:39:19');
INSERT INTO `word_library` VALUES (1907331092369625407, '卫生纸', 'draw-default', '日用品', '2025-07-25 15:39:19', '2025-07-25 15:39:19');
INSERT INTO `word_library` VALUES (1907331092369625408, '莲藕', 'draw-default', '蔬菜', '2025-07-25 15:39:19', '2025-07-25 15:39:19');
INSERT INTO `word_library` VALUES (1907331092369625410, '一休哥', 'draw-default', '动画角色', '2025-07-25 15:39:19', '2025-07-25 15:39:19');
INSERT INTO `word_library` VALUES (1907331092369625411, '升旗', 'draw-default', '仪式', '2025-07-25 15:39:19', '2025-07-25 15:39:19');
INSERT INTO `word_library` VALUES (1907331092369625412, '黑猩猩', 'draw-default', '动物', '2025-07-25 15:39:19', '2025-07-25 15:39:19');
INSERT INTO `word_library` VALUES (1907331092369625415, '骆驼', 'draw-default', '动物', '2025-07-25 15:39:19', '2025-07-25 15:39:19');
INSERT INTO `word_library` VALUES (1907331092369625416, '啄木鸟', 'draw-default', '鸟类', '2025-07-25 15:39:19', '2025-07-25 15:39:19');
INSERT INTO `word_library` VALUES (1907331092369625417, '米老鼠', 'draw-default', '动画角色', '2025-07-25 15:39:19', '2025-07-25 15:39:19');
INSERT INTO `word_library` VALUES (1907331092369625418, '天天向上', 'draw-default', '成语', '2025-07-25 15:39:19', '2025-07-25 15:39:19');
INSERT INTO `word_library` VALUES (1907331092369625419, '头破血流', 'draw-default', '成语', '2025-07-25 15:39:19', '2025-07-25 15:39:19');
INSERT INTO `word_library` VALUES (1907331092369625420, '微薄', 'draw-default', '社交媒体', '2025-07-25 15:39:19', '2025-07-25 15:39:19');
INSERT INTO `word_library` VALUES (1907331092369625421, '鸽子', 'draw-default', '鸟类', '2025-07-25 15:39:19', '2025-07-25 15:39:19');
INSERT INTO `word_library` VALUES (1907331092369625422, '勇往直前', 'draw-default', '成语', '2025-07-25 15:39:19', '2025-07-25 15:39:19');
INSERT INTO `word_library` VALUES (1907331092369625423, '跳绳', 'draw-default', '运动', '2025-07-25 15:39:19', '2025-07-25 15:39:19');
INSERT INTO `word_library` VALUES (1907331092369625426, '拳击', 'draw-default', '运动', '2025-07-25 15:39:19', '2025-07-25 15:39:19');
INSERT INTO `word_library` VALUES (1907331092369625427, '明信片', 'draw-default', '物品', '2025-07-25 15:39:19', '2025-07-25 15:39:19');
INSERT INTO `word_library` VALUES (1907331092369625428, '贾宝玉', 'draw-default', '文学人物', '2025-07-25 15:39:19', '2025-07-25 15:39:19');
INSERT INTO `word_library` VALUES (1907331092369625430, '火山爆发', 'draw-default', '自然现象', '2025-07-25 15:39:19', '2025-07-25 15:39:19');
INSERT INTO `word_library` VALUES (1907331092369625431, '医生', 'draw-default', '职业', '2025-07-25 15:39:19', '2025-07-25 15:39:19');
INSERT INTO `word_library` VALUES (1907331092369625435, '武松', 'draw-default', '文学人物', '2025-07-25 15:39:19', '2025-07-25 15:39:19');
INSERT INTO `word_library` VALUES (1907331092369625436, '一手遮天', 'draw-default', '成语', '2025-07-25 15:39:19', '2025-07-25 15:39:19');
INSERT INTO `word_library` VALUES (1907331092369625438, '愚公移山', 'draw-default', '成语', '2025-07-25 15:39:19', '2025-07-25 15:39:19');
INSERT INTO `word_library` VALUES (1907331092369625439, '情不自禁', 'draw-default', '成语', '2025-07-25 15:39:19', '2025-07-25 15:39:19');
INSERT INTO `word_library` VALUES (1907331092369625440, '一叶知秋', 'draw-default', '成语', '2025-07-25 15:39:19', '2025-07-25 15:39:19');
INSERT INTO `word_library` VALUES (1907331092369625442, '猎鹰', 'draw-default', '鸟类', '2025-07-25 15:39:19', '2025-07-25 15:39:19');
INSERT INTO `word_library` VALUES (1907331092369625443, '秃顶', 'draw-default', '状态', '2025-07-25 15:39:19', '2025-07-25 15:39:19');
INSERT INTO `word_library` VALUES (1907331092369625444, '孙悟空', 'draw-default', '神话人物', '2025-07-25 15:39:19', '2025-07-25 15:39:19');
INSERT INTO `word_library` VALUES (1907331092369625445, '鸡飞蛋打', 'draw-default', '成语', '2025-07-25 15:39:19', '2025-07-25 15:39:19');
INSERT INTO `word_library` VALUES (1907331092369625446, '回眸一笑', 'draw-default', '动作', '2025-07-25 15:39:19', '2025-07-25 15:39:19');
INSERT INTO `word_library` VALUES (1907331092369625447, '肯德基', 'draw-default', '品牌', '2025-07-25 15:39:19', '2025-07-25 15:39:19');
INSERT INTO `word_library` VALUES (1907331092369625448, '尖嘴猴腮', 'draw-default', '成语', '2025-07-25 15:39:19', '2025-07-25 15:39:19');
INSERT INTO `word_library` VALUES (1907331092369625449, '头悬梁锥刺股', 'draw-default', '成语', '2025-07-25 15:39:19', '2025-07-25 15:39:19');
INSERT INTO `word_library` VALUES (1907331092369625450, '呆若木鸡', 'draw-default', '成语', '2025-07-25 15:39:19', '2025-07-25 15:39:19');
INSERT INTO `word_library` VALUES (1907331092369625451, '形影不离', 'draw-default', '成语', '2025-07-25 15:39:19', '2025-07-25 15:39:19');
INSERT INTO `word_library` VALUES (1907331092369625453, '话剧', 'draw-default', '表演艺术', '2025-07-25 15:39:19', '2025-07-25 15:39:19');
INSERT INTO `word_library` VALUES (1907331092369625454, '黄色', 'draw-default', '颜色', '2025-07-25 15:39:19', '2025-07-25 15:39:19');
INSERT INTO `word_library` VALUES (1907331092369625455, '金针菇', 'draw-default', '食物', '2025-07-25 15:39:19', '2025-07-25 15:39:19');
INSERT INTO `word_library` VALUES (1907331092369625456, '打印机', 'draw-default', '电子产品', '2025-07-25 15:39:19', '2025-07-25 15:39:19');
INSERT INTO `word_library` VALUES (1907331092369625458, '电闪雷鸣', 'draw-default', '自然现象', '2025-07-25 15:39:19', '2025-07-25 15:39:19');
INSERT INTO `word_library` VALUES (1907331092369625460, '干冰', 'draw-default', '物质', '2025-07-25 15:39:19', '2025-07-25 15:39:19');
INSERT INTO `word_library` VALUES (1907331092369625462, '象棋', 'draw-default', '游戏', '2025-07-25 15:39:19', '2025-07-25 15:39:19');
INSERT INTO `word_library` VALUES (1907331092369625464, '虾', 'draw-default', '海洋动物', '2025-07-25 15:39:19', '2025-07-25 15:39:19');
INSERT INTO `word_library` VALUES (1907331092369625465, '圆规', 'draw-default', '文具', '2025-07-25 15:39:19', '2025-07-25 15:39:19');
INSERT INTO `word_library` VALUES (1907331092369625466, '菊花茶', 'draw-default', '饮料', '2025-07-25 15:39:19', '2025-07-25 15:39:19');
INSERT INTO `word_library` VALUES (1907331092369625467, '钻戒', 'draw-default', '饰品', '2025-07-25 15:39:19', '2025-07-25 15:39:19');
INSERT INTO `word_library` VALUES (1907331092369625468, '鸡翅', 'draw-default', '食物', '2025-07-25 15:39:19', '2025-07-25 15:39:19');
INSERT INTO `word_library` VALUES (1907331092369625469, '大头鱼', 'draw-default', '海洋动物', '2025-07-25 15:39:19', '2025-07-25 15:39:19');
INSERT INTO `word_library` VALUES (1907331092369625470, '龙虾', 'draw-default', '海洋动物', '2025-07-25 15:39:19', '2025-07-25 15:39:19');
INSERT INTO `word_library` VALUES (1907331092369625471, '蟠桃', 'draw-default', '神话食物', '2025-07-25 15:39:19', '2025-07-25 15:39:19');
INSERT INTO `word_library` VALUES (1907331092369625472, '一刀两断', 'draw-default', '成语', '2025-07-25 15:39:19', '2025-07-25 15:39:19');
INSERT INTO `word_library` VALUES (1907331092369625473, '白斩狗', 'draw-default', '食物', '2025-07-25 15:39:19', '2025-07-25 15:39:19');
INSERT INTO `word_library` VALUES (1907331092369625474, '兔子', 'draw-default', '动物', '2025-07-25 15:39:19', '2025-07-25 15:39:19');
INSERT INTO `word_library` VALUES (1907331092369625475, '尿布', 'draw-default', '婴儿用品', '2025-07-25 15:39:19', '2025-07-25 15:39:19');
INSERT INTO `word_library` VALUES (1907331092369625476, '俯卧撑', 'draw-default', '运动', '2025-07-25 15:39:19', '2025-07-25 15:39:19');
INSERT INTO `word_library` VALUES (1907331092369625478, '饮水机', 'draw-default', '电器', '2025-07-25 15:39:19', '2025-07-25 15:39:19');
INSERT INTO `word_library` VALUES (1907331092369625479, '猫哭老鼠', 'draw-default', '成语', '2025-07-25 15:39:19', '2025-07-25 15:39:19');
INSERT INTO `word_library` VALUES (1907331092369625480, '老大徒伤悲', 'draw-default', '成语', '2025-07-25 15:39:19', '2025-07-25 15:39:19');
INSERT INTO `word_library` VALUES (1907331092369625482, '吃田螺', 'draw-default', '动作', '2025-07-25 15:39:19', '2025-07-25 15:39:19');
INSERT INTO `word_library` VALUES (1907331092369625483, '山盟海誓', 'draw-default', '成语', '2025-07-25 15:39:19', '2025-07-25 15:39:19');
INSERT INTO `word_library` VALUES (1907331092369625484, '挤眉弄眼', 'draw-default', '动作', '2025-07-25 15:39:19', '2025-07-25 15:39:19');
INSERT INTO `word_library` VALUES (1907331092369625487, '护士节', 'draw-default', '节日', '2025-07-25 15:39:19', '2025-07-25 15:39:19');
INSERT INTO `word_library` VALUES (1907331092369625488, '桃花', 'draw-default', '花卉', '2025-07-25 15:39:19', '2025-07-25 15:39:19');
INSERT INTO `word_library` VALUES (1907331092369625489, '中暑', 'draw-default', '症状', '2025-07-25 15:39:19', '2025-07-25 15:39:19');
INSERT INTO `word_library` VALUES (1907331092369625491, '豚鼠', 'draw-default', '动物', '2025-07-25 15:39:19', '2025-07-25 15:39:19');
INSERT INTO `word_library` VALUES (1907331092369625493, '跪地求饶', 'draw-default', '动作', '2025-07-25 15:39:19', '2025-07-25 15:39:19');
INSERT INTO `word_library` VALUES (1907331092369625494, '愁眉苦脸', 'draw-default', '状态', '2025-07-25 15:39:19', '2025-07-25 15:39:19');
INSERT INTO `word_library` VALUES (1907331092369625495, '保龄球', 'draw-default', '运动', '2025-07-25 15:39:19', '2025-07-25 15:39:19');
INSERT INTO `word_library` VALUES (1907331092369625497, '苹果', 'draw-default', '水果', '2025-07-25 15:39:19', '2025-07-25 15:39:19');
INSERT INTO `word_library` VALUES (1907331092369625498, '毛巾', 'draw-default', '日用品', '2025-07-25 15:39:19', '2025-07-25 15:39:19');
INSERT INTO `word_library` VALUES (1907331092369625499, '红领巾', 'draw-default', '服饰', '2025-07-25 15:39:19', '2025-07-25 15:39:19');
INSERT INTO `word_library` VALUES (1907331092369625500, '螳螂', 'draw-default', '昆虫', '2025-07-25 15:39:19', '2025-07-25 15:39:19');
INSERT INTO `word_library` VALUES (1907331092369625502, '画蛇添足', 'draw-default', '成语', '2025-07-25 15:39:19', '2025-07-25 15:39:19');
INSERT INTO `word_library` VALUES (1907331092369625505, '泪流满面', 'draw-default', '状态', '2025-07-25 15:39:19', '2025-07-25 15:39:19');
INSERT INTO `word_library` VALUES (1907331092369625506, '牛肉干', 'draw-default', '食物', '2025-07-25 15:39:19', '2025-07-25 15:39:19');
INSERT INTO `word_library` VALUES (1907331092369625508, '一丝不挂', 'draw-default', '成语', '2025-07-25 15:39:19', '2025-07-25 15:39:19');
INSERT INTO `word_library` VALUES (1907331092369625509, '胆小如鼠', 'draw-default', '成语', '2025-07-25 15:39:19', '2025-07-25 15:39:19');
INSERT INTO `word_library` VALUES (1907331092369625510, '逍遥法外', 'draw-default', '成语', '2025-07-25 15:39:19', '2025-07-25 15:39:19');
INSERT INTO `word_library` VALUES (1907331092369625511, '争分夺秒', 'draw-default', '成语', '2025-07-25 15:39:19', '2025-07-25 15:39:19');
INSERT INTO `word_library` VALUES (1907331092369625515, 'KTV', 'draw-default', '娱乐场所', '2025-07-25 15:39:19', '2025-07-25 15:39:19');
INSERT INTO `word_library` VALUES (1907331092369625516, '抽刀断水', 'draw-default', '成语', '2025-07-25 15:39:19', '2025-07-25 15:39:19');
INSERT INTO `word_library` VALUES (1907331092369625517, '手舞足蹈', 'draw-default', '成语', '2025-07-25 15:39:19', '2025-07-25 15:39:19');
INSERT INTO `word_library` VALUES (1907331092369625518, '窈窕淑女', 'draw-default', '成语', '2025-07-25 15:39:19', '2025-07-25 15:39:19');
INSERT INTO `word_library` VALUES (1907331092369625520, '护肤品', 'draw-default', '化妆品', '2025-07-25 15:39:19', '2025-07-25 15:39:19');
INSERT INTO `word_library` VALUES (1907331092369625521, '餐巾纸', 'draw-default', '餐具', '2025-07-25 15:39:19', '2025-07-25 15:39:19');
INSERT INTO `word_library` VALUES (1907331092369625524, '可爱', 'draw-default', '形容词', '2025-07-25 15:39:19', '2025-07-25 15:39:19');
INSERT INTO `word_library` VALUES (1907331092369625525, '舞蹈', 'draw-default', '艺术形式', '2025-07-25 15:39:19', '2025-07-25 15:39:19');
INSERT INTO `word_library` VALUES (1907331092369625526, '猪八戒', 'draw-default', '神话人物', '2025-07-25 15:39:19', '2025-07-25 15:39:19');
INSERT INTO `word_library` VALUES (1907331092369625527, '麋鹿', 'draw-default', '动物', '2025-07-25 15:39:19', '2025-07-25 15:39:19');
INSERT INTO `word_library` VALUES (1907331092369625529, '三明治', 'draw-default', '食物', '2025-07-25 15:39:19', '2025-07-25 15:39:19');
INSERT INTO `word_library` VALUES (1907331092369625530, '野鸭', 'draw-default', '鸟类', '2025-07-25 15:39:19', '2025-07-25 15:39:19');
INSERT INTO `word_library` VALUES (1907331092369625531, '电鳗', 'draw-default', '海洋动物', '2025-07-25 15:39:19', '2025-07-25 15:39:19');
INSERT INTO `word_library` VALUES (1907331092369625532, '甘蔗', 'draw-default', '植物', '2025-07-25 15:39:19', '2025-07-25 15:39:19');
INSERT INTO `word_library` VALUES (1907331092369625533, '红枣', 'draw-default', '水果', '2025-07-25 15:39:19', '2025-07-25 15:39:19');
INSERT INTO `word_library` VALUES (1907331092369625534, '乌梅', 'draw-default', '水果', '2025-07-25 15:39:19', '2025-07-25 15:39:19');
INSERT INTO `word_library` VALUES (1907331092369625535, '莲雾', 'draw-default', '水果', '2025-07-25 15:39:19', '2025-07-25 15:39:19');
INSERT INTO `word_library` VALUES (1907331092369625536, '蜡烛', 'draw-default', '日用品', '2025-07-25 15:39:19', '2025-07-25 15:39:19');
INSERT INTO `word_library` VALUES (1907331092369625538, '李连杰', 'draw-default', '演员', '2025-07-25 15:39:19', '2025-07-25 15:39:19');
INSERT INTO `word_library` VALUES (1907331092369625539, '羽毛球', 'draw-default', '体育用品', '2025-07-25 15:39:19', '2025-07-25 15:39:19');
INSERT INTO `word_library` VALUES (1907331092369625540, '上官婉儿', 'draw-hero', '王者英雄', '2025-07-25 15:48:06', '2025-07-25 15:48:06');
INSERT INTO `word_library` VALUES (1907331092369625541, '不知火舞', 'draw-hero', '王者英雄', '2025-07-25 15:48:06', '2025-07-25 15:48:06');
INSERT INTO `word_library` VALUES (1907331092369625542, '东皇太一', 'draw-hero', '王者英雄', '2025-07-25 15:48:06', '2025-07-25 15:48:06');
INSERT INTO `word_library` VALUES (1907331092369625543, '云中君', 'draw-hero', '王者英雄', '2025-07-25 15:48:06', '2025-07-25 15:48:06');
INSERT INTO `word_library` VALUES (1907331092369625544, '云缨', 'draw-hero', '王者英雄', '2025-07-25 15:48:06', '2025-07-25 15:48:06');
INSERT INTO `word_library` VALUES (1907331092369625545, '亚瑟', 'draw-hero', '王者英雄', '2025-07-25 15:48:06', '2025-07-25 15:48:06');
INSERT INTO `word_library` VALUES (1907331092369625546, '亚连', 'draw-hero', '王者英雄', '2025-07-25 15:48:06', '2025-07-25 15:48:06');
INSERT INTO `word_library` VALUES (1907331092369625547, '伽罗', 'draw-hero', '王者英雄', '2025-07-25 15:48:06', '2025-07-25 15:48:06');
INSERT INTO `word_library` VALUES (1907331092369625548, '元歌', 'draw-hero', '王者英雄', '2025-07-25 15:48:06', '2025-07-25 15:48:06');
INSERT INTO `word_library` VALUES (1907331092369625549, '元流之子(坦克)', 'draw-hero', '王者英雄', '2025-07-25 15:48:06', '2025-07-25 15:48:06');
INSERT INTO `word_library` VALUES (1907331092369625550, '元流之子(射手)', 'draw-hero', '王者英雄', '2025-07-25 15:48:06', '2025-07-25 15:48:06');
INSERT INTO `word_library` VALUES (1907331092369625551, '元流之子(法师)', 'draw-hero', '王者英雄', '2025-07-25 15:48:06', '2025-07-25 15:48:06');
INSERT INTO `word_library` VALUES (1907331092369625552, '公孙离', 'draw-hero', '王者英雄', '2025-07-25 15:48:06', '2025-07-25 15:48:06');
INSERT INTO `word_library` VALUES (1907331092369625553, '兰陵王', 'draw-hero', '王者英雄', '2025-07-25 15:48:06', '2025-07-25 15:48:06');
INSERT INTO `word_library` VALUES (1907331092369625554, '关羽', 'draw-hero', '王者英雄', '2025-07-25 15:48:06', '2025-07-25 15:48:06');
INSERT INTO `word_library` VALUES (1907331092369625555, '典韦', 'draw-hero', '王者英雄', '2025-07-25 15:48:06', '2025-07-25 15:48:06');
INSERT INTO `word_library` VALUES (1907331092369625556, '刘备', 'draw-hero', '王者英雄', '2025-07-25 15:48:06', '2025-07-25 15:48:06');
INSERT INTO `word_library` VALUES (1907331092369625557, '刘禅', 'draw-hero', '王者英雄', '2025-07-25 15:48:06', '2025-07-25 15:48:06');
INSERT INTO `word_library` VALUES (1907331092369625558, '刘邦', 'draw-hero', '王者英雄', '2025-07-25 15:48:06', '2025-07-25 15:48:06');
INSERT INTO `word_library` VALUES (1907331092369625559, '司空震', 'draw-hero', '王者英雄', '2025-07-25 15:48:06', '2025-07-25 15:48:06');
INSERT INTO `word_library` VALUES (1907331092369625560, '司马懿', 'draw-hero', '王者英雄', '2025-07-25 15:48:06', '2025-07-25 15:48:06');
INSERT INTO `word_library` VALUES (1907331092369625561, '后羿', 'draw-hero', '王者英雄', '2025-07-25 15:48:06', '2025-07-25 15:48:06');
INSERT INTO `word_library` VALUES (1907331092369625562, '吕布', 'draw-hero', '王者英雄', '2025-07-25 15:48:06', '2025-07-25 15:48:06');
INSERT INTO `word_library` VALUES (1907331092369625563, '周瑜', 'draw-hero', '王者英雄', '2025-07-25 15:48:07', '2025-07-25 15:48:07');
INSERT INTO `word_library` VALUES (1907331092369625564, '哪吒', 'draw-hero', '王者英雄', '2025-07-25 15:48:07', '2025-07-25 15:48:07');
INSERT INTO `word_library` VALUES (1907331092369625565, '墨子', 'draw-hero', '王者英雄', '2025-07-25 15:48:07', '2025-07-25 15:48:07');
INSERT INTO `word_library` VALUES (1907331092369625566, '夏侯惇', 'draw-hero', '王者英雄', '2025-07-25 15:48:07', '2025-07-25 15:48:07');
INSERT INTO `word_library` VALUES (1907331092369625567, '夏洛特', 'draw-hero', '王者英雄', '2025-07-25 15:48:07', '2025-07-25 15:48:07');
INSERT INTO `word_library` VALUES (1907331092369625568, '大乔', 'draw-hero', '王者英雄', '2025-07-25 15:48:07', '2025-07-25 15:48:07');
INSERT INTO `word_library` VALUES (1907331092369625569, '大司命', 'draw-hero', '王者英雄', '2025-07-25 15:48:07', '2025-07-25 15:48:07');
INSERT INTO `word_library` VALUES (1907331092369625570, '太乙真人', 'draw-hero', '王者英雄', '2025-07-25 15:48:07', '2025-07-25 15:48:07');
INSERT INTO `word_library` VALUES (1907331092369625571, '女娲', 'draw-hero', '王者英雄', '2025-07-25 15:48:07', '2025-07-25 15:48:07');
INSERT INTO `word_library` VALUES (1907331092369625572, '妲己', 'draw-hero', '王者英雄', '2025-07-25 15:48:07', '2025-07-25 15:48:07');
INSERT INTO `word_library` VALUES (1907331092369625573, '姜子牙', 'draw-hero', '王者英雄', '2025-07-25 15:48:07', '2025-07-25 15:48:07');
INSERT INTO `word_library` VALUES (1907331092369625574, '姬小满', 'draw-hero', '王者英雄', '2025-07-25 15:48:07', '2025-07-25 15:48:07');
INSERT INTO `word_library` VALUES (1907331092369625575, '娜可露露', 'draw-hero', '王者英雄', '2025-07-25 15:48:07', '2025-07-25 15:48:07');
INSERT INTO `word_library` VALUES (1907331092369625576, '嫦娥', 'draw-hero', '王者英雄', '2025-07-25 15:48:07', '2025-07-25 15:48:07');
INSERT INTO `word_library` VALUES (1907331092369625577, '嬴政', 'draw-hero', '王者英雄', '2025-07-25 15:48:07', '2025-07-25 15:48:07');
INSERT INTO `word_library` VALUES (1907331092369625578, '孙尚香', 'draw-hero', '王者英雄', '2025-07-25 15:48:07', '2025-07-25 15:48:07');
INSERT INTO `word_library` VALUES (1907331092369625579, '孙悟空', 'draw-hero', '王者英雄', '2025-07-25 15:48:07', '2025-07-25 15:48:07');
INSERT INTO `word_library` VALUES (1907331092369625580, '孙权', 'draw-hero', '王者英雄', '2025-07-25 15:48:07', '2025-07-25 15:48:07');
INSERT INTO `word_library` VALUES (1907331092369625581, '孙策', 'draw-hero', '王者英雄', '2025-07-25 15:48:07', '2025-07-25 15:48:07');
INSERT INTO `word_library` VALUES (1907331092369625582, '孙膑', 'draw-hero', '王者英雄', '2025-07-25 15:48:07', '2025-07-25 15:48:07');
INSERT INTO `word_library` VALUES (1907331092369625583, '安琪拉', 'draw-hero', '王者英雄', '2025-07-25 15:48:07', '2025-07-25 15:48:07');
INSERT INTO `word_library` VALUES (1907331092369625584, '宫本武藏', 'draw-hero', '王者英雄', '2025-07-25 15:48:07', '2025-07-25 15:48:07');
INSERT INTO `word_library` VALUES (1907331092369625585, '小乔', 'draw-hero', '王者英雄', '2025-07-25 15:48:07', '2025-07-25 15:48:07');
INSERT INTO `word_library` VALUES (1907331092369625586, '少司缘', 'draw-hero', '王者英雄', '2025-07-25 15:48:07', '2025-07-25 15:48:07');
INSERT INTO `word_library` VALUES (1907331092369625587, '干将莫邪', 'draw-hero', '王者英雄', '2025-07-25 15:48:07', '2025-07-25 15:48:07');
INSERT INTO `word_library` VALUES (1907331092369625588, '庄周', 'draw-hero', '王者英雄', '2025-07-25 15:48:07', '2025-07-25 15:48:07');
INSERT INTO `word_library` VALUES (1907331092369625589, '廉颇', 'draw-hero', '王者英雄', '2025-07-25 15:48:07', '2025-07-25 15:48:07');
INSERT INTO `word_library` VALUES (1907331092369625590, '弈星', 'draw-hero', '王者英雄', '2025-07-25 15:48:07', '2025-07-25 15:48:07');
INSERT INTO `word_library` VALUES (1907331092369625591, '张良', 'draw-hero', '王者英雄', '2025-07-25 15:48:07', '2025-07-25 15:48:07');
INSERT INTO `word_library` VALUES (1907331092369625592, '张飞', 'draw-hero', '王者英雄', '2025-07-25 15:48:07', '2025-07-25 15:48:07');
INSERT INTO `word_library` VALUES (1907331092369625593, '影', 'draw-hero', '王者英雄', '2025-07-25 15:48:07', '2025-07-25 15:48:07');
INSERT INTO `word_library` VALUES (1907331092369625594, '戈娅', 'draw-hero', '王者英雄', '2025-07-25 15:48:07', '2025-07-25 15:48:07');
INSERT INTO `word_library` VALUES (1907331092369625595, '扁鹊', 'draw-hero', '王者英雄', '2025-07-25 15:48:07', '2025-07-25 15:48:07');
INSERT INTO `word_library` VALUES (1907331092369625596, '敖隐', 'draw-hero', '王者英雄', '2025-07-25 15:48:07', '2025-07-25 15:48:07');
INSERT INTO `word_library` VALUES (1907331092369625597, '明世隐', 'draw-hero', '王者英雄', '2025-07-25 15:48:07', '2025-07-25 15:48:07');
INSERT INTO `word_library` VALUES (1907331092369625598, '暃', 'draw-hero', '王者英雄', '2025-07-25 15:48:07', '2025-07-25 15:48:07');
INSERT INTO `word_library` VALUES (1907331092369625599, '曜', 'draw-hero', '王者英雄', '2025-07-25 15:48:07', '2025-07-25 15:48:07');
INSERT INTO `word_library` VALUES (1907331092369625600, '曹操', 'draw-hero', '王者英雄', '2025-07-25 15:48:07', '2025-07-25 15:48:07');
INSERT INTO `word_library` VALUES (1907331092369625601, '朵莉亚', 'draw-hero', '王者英雄', '2025-07-25 15:48:07', '2025-07-25 15:48:07');
INSERT INTO `word_library` VALUES (1907331092369625602, '李信', 'draw-hero', '王者英雄', '2025-07-25 15:48:07', '2025-07-25 15:48:07');
INSERT INTO `word_library` VALUES (1907331092369625603, '李元芳', 'draw-hero', '王者英雄', '2025-07-25 15:48:07', '2025-07-25 15:48:07');
INSERT INTO `word_library` VALUES (1907331092369625604, '李白', 'draw-hero', '王者英雄', '2025-07-25 15:48:07', '2025-07-25 15:48:07');
INSERT INTO `word_library` VALUES (1907331092369625605, '杨戬', 'draw-hero', '王者英雄', '2025-07-25 15:48:07', '2025-07-25 15:48:07');
INSERT INTO `word_library` VALUES (1907331092369625606, '杨玉环', 'draw-hero', '王者英雄', '2025-07-25 15:48:07', '2025-07-25 15:48:07');
INSERT INTO `word_library` VALUES (1907331092369625607, '桑启', 'draw-hero', '王者英雄', '2025-07-25 15:48:07', '2025-07-25 15:48:07');
INSERT INTO `word_library` VALUES (1907331092369625608, '梦奇', 'draw-hero', '王者英雄', '2025-07-25 15:48:07', '2025-07-25 15:48:07');
INSERT INTO `word_library` VALUES (1907331092369625609, '橘右京', 'draw-hero', '王者英雄', '2025-07-25 15:48:07', '2025-07-25 15:48:07');
INSERT INTO `word_library` VALUES (1907331092369625610, '武则天', 'draw-hero', '王者英雄', '2025-07-25 15:48:07', '2025-07-25 15:48:07');
INSERT INTO `word_library` VALUES (1907331092369625611, '沈梦溪', 'draw-hero', '王者英雄', '2025-07-25 15:48:07', '2025-07-25 15:48:07');
INSERT INTO `word_library` VALUES (1907331092369625612, '海月', 'draw-hero', '王者英雄', '2025-07-25 15:48:07', '2025-07-25 15:48:07');
INSERT INTO `word_library` VALUES (1907331092369625613, '海诺', 'draw-hero', '王者英雄', '2025-07-25 15:48:07', '2025-07-25 15:48:07');
INSERT INTO `word_library` VALUES (1907331092369625614, '澜', 'draw-hero', '王者英雄', '2025-07-25 15:48:07', '2025-07-25 15:48:07');
INSERT INTO `word_library` VALUES (1907331092369625615, '牛魔', 'draw-hero', '王者英雄', '2025-07-25 15:48:07', '2025-07-25 15:48:07');
INSERT INTO `word_library` VALUES (1907331092369625616, '狂铁', 'draw-hero', '王者英雄', '2025-07-25 15:48:07', '2025-07-25 15:48:07');
INSERT INTO `word_library` VALUES (1907331092369625617, '狄仁杰', 'draw-hero', '王者英雄', '2025-07-25 15:48:07', '2025-07-25 15:48:07');
INSERT INTO `word_library` VALUES (1907331092369625618, '猪八戒', 'draw-hero', '王者英雄', '2025-07-25 15:48:07', '2025-07-25 15:48:07');
INSERT INTO `word_library` VALUES (1907331092369625619, '王昭君', 'draw-hero', '王者英雄', '2025-07-25 15:48:07', '2025-07-25 15:48:07');
INSERT INTO `word_library` VALUES (1907331092369625620, '瑶', 'draw-hero', '王者英雄', '2025-07-25 15:48:07', '2025-07-25 15:48:07');
INSERT INTO `word_library` VALUES (1907331092369625621, '甄姬', 'draw-hero', '王者英雄', '2025-07-25 15:48:07', '2025-07-25 15:48:07');
INSERT INTO `word_library` VALUES (1907331092369625622, '白起', 'draw-hero', '王者英雄', '2025-07-25 15:48:07', '2025-07-25 15:48:07');
INSERT INTO `word_library` VALUES (1907331092369625623, '百里守约', 'draw-hero', '王者英雄', '2025-07-25 15:48:07', '2025-07-25 15:48:07');
INSERT INTO `word_library` VALUES (1907331092369625624, '百里玄策', 'draw-hero', '王者英雄', '2025-07-25 15:48:07', '2025-07-25 15:48:07');
INSERT INTO `word_library` VALUES (1907331092369625625, '盘古', 'draw-hero', '王者英雄', '2025-07-25 15:48:07', '2025-07-25 15:48:07');
INSERT INTO `word_library` VALUES (1907331092369625626, '盾山', 'draw-hero', '王者英雄', '2025-07-25 15:48:07', '2025-07-25 15:48:07');
INSERT INTO `word_library` VALUES (1907331092369625627, '程咬金', 'draw-hero', '王者英雄', '2025-07-25 15:48:07', '2025-07-25 15:48:07');
INSERT INTO `word_library` VALUES (1907331092369625628, '空空儿', 'draw-hero', '王者英雄', '2025-07-25 15:48:07', '2025-07-25 15:48:07');
INSERT INTO `word_library` VALUES (1907331092369625629, '米莱狄', 'draw-hero', '王者英雄', '2025-07-25 15:48:07', '2025-07-25 15:48:07');
INSERT INTO `word_library` VALUES (1907331092369625630, '老夫子', 'draw-hero', '王者英雄', '2025-07-25 15:48:07', '2025-07-25 15:48:07');
INSERT INTO `word_library` VALUES (1907331092369625631, '艾琳', 'draw-hero', '王者英雄', '2025-07-25 15:48:07', '2025-07-25 15:48:07');
INSERT INTO `word_library` VALUES (1907331092369625632, '芈月', 'draw-hero', '王者英雄', '2025-07-25 15:48:07', '2025-07-25 15:48:07');
INSERT INTO `word_library` VALUES (1907331092369625633, '花木兰', 'draw-hero', '王者英雄', '2025-07-25 15:48:07', '2025-07-25 15:48:07');
INSERT INTO `word_library` VALUES (1907331092369625634, '苍', 'draw-hero', '王者英雄', '2025-07-25 15:48:07', '2025-07-25 15:48:07');
INSERT INTO `word_library` VALUES (1907331092369625635, '苏烈', 'draw-hero', '王者英雄', '2025-07-25 15:48:07', '2025-07-25 15:48:07');
INSERT INTO `word_library` VALUES (1907331092369625636, '莱西奥', 'draw-hero', '王者英雄', '2025-07-25 15:48:07', '2025-07-25 15:48:07');
INSERT INTO `word_library` VALUES (1907331092369625637, '蒙恬', 'draw-hero', '王者英雄', '2025-07-25 15:48:07', '2025-07-25 15:48:07');
INSERT INTO `word_library` VALUES (1907331092369625638, '蒙犽', 'draw-hero', '王者英雄', '2025-07-25 15:48:07', '2025-07-25 15:48:07');
INSERT INTO `word_library` VALUES (1907331092369625639, '蔡文姬', 'draw-hero', '王者英雄', '2025-07-25 15:48:07', '2025-07-25 15:48:07');
INSERT INTO `word_library` VALUES (1907331092369625640, '虞姬', 'draw-hero', '王者英雄', '2025-07-25 15:48:07', '2025-07-25 15:48:07');
INSERT INTO `word_library` VALUES (1907331092369625641, '裴擒虎', 'draw-hero', '王者英雄', '2025-07-25 15:48:07', '2025-07-25 15:48:07');
INSERT INTO `word_library` VALUES (1907331092369625642, '西施', 'draw-hero', '王者英雄', '2025-07-25 15:48:07', '2025-07-25 15:48:07');
INSERT INTO `word_library` VALUES (1907331092369625643, '诸葛亮', 'draw-hero', '王者英雄', '2025-07-25 15:48:07', '2025-07-25 15:48:07');
INSERT INTO `word_library` VALUES (1907331092369625644, '貂蝉', 'draw-hero', '王者英雄', '2025-07-25 15:48:07', '2025-07-25 15:48:07');
INSERT INTO `word_library` VALUES (1907331092369625645, '赵云', 'draw-hero', '王者英雄', '2025-07-25 15:48:07', '2025-07-25 15:48:07');
INSERT INTO `word_library` VALUES (1907331092369625646, '赵怀真', 'draw-hero', '王者英雄', '2025-07-25 15:48:07', '2025-07-25 15:48:07');
INSERT INTO `word_library` VALUES (1907331092369625647, '达摩', 'draw-hero', '王者英雄', '2025-07-25 15:48:07', '2025-07-25 15:48:07');
INSERT INTO `word_library` VALUES (1907331092369625648, '金蝉', 'draw-hero', '王者英雄', '2025-07-25 15:48:07', '2025-07-25 15:48:07');
INSERT INTO `word_library` VALUES (1907331092369625649, '钟无艳', 'draw-hero', '王者英雄', '2025-07-25 15:48:07', '2025-07-25 15:48:07');
INSERT INTO `word_library` VALUES (1907331092369625650, '钟馗', 'draw-hero', '王者英雄', '2025-07-25 15:48:07', '2025-07-25 15:48:07');
INSERT INTO `word_library` VALUES (1907331092369625651, '铠', 'draw-hero', '王者英雄', '2025-07-25 15:48:07', '2025-07-25 15:48:07');
INSERT INTO `word_library` VALUES (1907331092369625652, '镜', 'draw-hero', '王者英雄', '2025-07-25 15:48:07', '2025-07-25 15:48:07');
INSERT INTO `word_library` VALUES (1907331092369625653, '阿古朵', 'draw-hero', '王者英雄', '2025-07-25 15:48:07', '2025-07-25 15:48:07');
INSERT INTO `word_library` VALUES (1907331092369625654, '阿轲', 'draw-hero', '王者英雄', '2025-07-25 15:48:07', '2025-07-25 15:48:07');
INSERT INTO `word_library` VALUES (1907331092369625655, '雅典娜', 'draw-hero', '王者英雄', '2025-07-25 15:48:07', '2025-07-25 15:48:07');
INSERT INTO `word_library` VALUES (1907331092369625656, '露娜', 'draw-hero', '王者英雄', '2025-07-25 15:48:07', '2025-07-25 15:48:07');
INSERT INTO `word_library` VALUES (1907331092369625657, '韩信', 'draw-hero', '王者英雄', '2025-07-25 15:48:07', '2025-07-25 15:48:07');
INSERT INTO `word_library` VALUES (1907331092369625658, '项羽', 'draw-hero', '王者英雄', '2025-07-25 15:48:07', '2025-07-25 15:48:07');
INSERT INTO `word_library` VALUES (1907331092369625659, '马可波罗', 'draw-hero', '王者英雄', '2025-07-25 15:48:07', '2025-07-25 15:48:07');
INSERT INTO `word_library` VALUES (1907331092369625660, '马超', 'draw-hero', '王者英雄', '2025-07-25 15:48:07', '2025-07-25 15:48:07');
INSERT INTO `word_library` VALUES (1907331092369625661, '高渐离', 'draw-hero', '王者英雄', '2025-07-25 15:48:07', '2025-07-25 15:48:07');
INSERT INTO `word_library` VALUES (1907331092369625662, '鬼谷子', 'draw-hero', '王者英雄', '2025-07-25 15:48:07', '2025-07-25 15:48:07');
INSERT INTO `word_library` VALUES (1907331092369625663, '鲁班七号', 'draw-hero', '王者英雄', '2025-07-25 15:48:07', '2025-07-25 15:48:07');
INSERT INTO `word_library` VALUES (1907331092369625664, '鲁班大师', 'draw-hero', '王者英雄', '2025-07-25 15:48:07', '2025-07-25 15:48:07');
INSERT INTO `word_library` VALUES (1907331092369625665, '黄忠', 'draw-hero', '王者英雄', '2025-07-25 15:48:07', '2025-07-25 15:48:07');
INSERT INTO `word_library` VALUES (1907331092369625666, '形昭之鉴', 'draw-hero', '王者装备', '2025-07-25 15:48:07', '2025-07-25 15:48:07');
INSERT INTO `word_library` VALUES (1907331092369625667, '出影晶石', 'draw-hero', '王者装备', '2025-07-25 15:48:07', '2025-07-25 15:48:07');
INSERT INTO `word_library` VALUES (1907331092369625668, '魔道之石', 'draw-hero', '王者装备', '2025-07-25 15:48:07', '2025-07-25 15:48:07');
INSERT INTO `word_library` VALUES (1907331092369625669, '云灵木', 'draw-hero', '王者装备', '2025-07-25 15:48:07', '2025-07-25 15:48:07');
INSERT INTO `word_library` VALUES (1907331092369625670, '凛冬', 'draw-hero', '王者装备', '2025-07-25 15:48:07', '2025-07-25 15:48:07');
INSERT INTO `word_library` VALUES (1907331092369625671, '怒魂', 'draw-hero', '王者装备', '2025-07-25 15:48:07', '2025-07-25 15:48:07');
INSERT INTO `word_library` VALUES (1907331092369625672, '月神', 'draw-hero', '王者装备', '2025-07-25 15:48:07', '2025-07-25 15:48:07');
INSERT INTO `word_library` VALUES (1907331092369625673, '天穹', 'draw-hero', '王者装备', '2025-07-25 15:48:07', '2025-07-25 15:48:07');
INSERT INTO `word_library` VALUES (1907331092369625674, '日渊', 'draw-hero', '王者装备', '2025-07-25 15:48:07', '2025-07-25 15:48:07');
INSERT INTO `word_library` VALUES (1907331092369625675, '原初遗珠', 'draw-hero', '王者装备', '2025-07-25 15:48:07', '2025-07-25 15:48:07');
INSERT INTO `word_library` VALUES (1907331092369625676, '永夜守护', 'draw-hero', '王者装备', '2025-07-25 15:48:07', '2025-07-25 15:48:07');
INSERT INTO `word_library` VALUES (1907331092369625677, '暗夜小甲', 'draw-hero', '王者装备', '2025-07-25 15:48:07', '2025-07-25 15:48:07');
INSERT INTO `word_library` VALUES (1907331092369625678, '荆棘护手', 'draw-hero', '王者装备', '2025-07-25 15:48:07', '2025-07-25 15:48:07');
INSERT INTO `word_library` VALUES (1907331092369625679, '浴火之怒', 'draw-hero', '王者装备', '2025-07-25 15:48:07', '2025-07-25 15:48:07');
INSERT INTO `word_library` VALUES (1907331092369625680, '龙鳞利剑', 'draw-hero', '王者装备', '2025-07-25 15:48:07', '2025-07-25 15:48:07');
INSERT INTO `word_library` VALUES (1907331092369625681, '弃鳞短刃', 'draw-hero', '王者装备', '2025-07-25 15:48:07', '2025-07-25 15:48:07');
INSERT INTO `word_library` VALUES (1907331092369625682, '冰霜冲击', 'draw-hero', '王者装备', '2025-07-25 15:48:07', '2025-07-25 15:48:07');
INSERT INTO `word_library` VALUES (1907331092369625683, '金色圣剑', 'draw-hero', '王者装备', '2025-07-25 15:48:07', '2025-07-25 15:48:07');
INSERT INTO `word_library` VALUES (1907331092369625684, '符文大剑', 'draw-hero', '王者装备', '2025-07-25 15:48:07', '2025-07-25 15:48:07');
INSERT INTO `word_library` VALUES (1907331092369625685, '日暮之流', 'draw-hero', '王者装备', '2025-07-25 15:48:07', '2025-07-25 15:48:07');
INSERT INTO `word_library` VALUES (1907331092369625686, '星泉', 'draw-hero', '王者装备', '2025-07-25 15:48:07', '2025-07-25 15:48:07');
INSERT INTO `word_library` VALUES (1907331092369625687, '星之配饰', 'draw-hero', '王者装备', '2025-07-25 15:48:07', '2025-07-25 15:48:07');
INSERT INTO `word_library` VALUES (1907331092369625688, '破晓', 'draw-hero', '王者装备', '2025-07-25 15:48:07', '2025-07-25 15:48:07');
INSERT INTO `word_library` VALUES (1907331092369625689, '穿云弓', 'draw-hero', '王者装备', '2025-07-25 15:48:07', '2025-07-25 15:48:07');
INSERT INTO `word_library` VALUES (1907331092369625690, '无尽战刃', 'draw-hero', '王者装备', '2025-07-25 15:48:07', '2025-07-25 15:48:07');
INSERT INTO `word_library` VALUES (1907331092369625691, '近卫荣耀', 'draw-hero', '王者装备', '2025-07-25 15:48:07', '2025-07-25 15:48:07');
INSERT INTO `word_library` VALUES (1907331092369625692, '奔狼纹章', 'draw-hero', '王者装备', '2025-07-25 15:48:07', '2025-07-25 15:48:07');
INSERT INTO `word_library` VALUES (1907331092369625693, '救赎之翼', 'draw-hero', '王者装备', '2025-07-25 15:48:07', '2025-07-25 15:48:07');
INSERT INTO `word_library` VALUES (1907331092369625694, '极影', 'draw-hero', '王者装备', '2025-07-25 15:48:07', '2025-07-25 15:48:07');
INSERT INTO `word_library` VALUES (1907331092369625695, '鼓舞之盾', 'draw-hero', '王者装备', '2025-07-25 15:48:07', '2025-07-25 15:48:07');
INSERT INTO `word_library` VALUES (1907331092369625696, '风灵纹章', 'draw-hero', '王者装备', '2025-07-25 15:48:07', '2025-07-25 15:48:07');
INSERT INTO `word_library` VALUES (1907331092369625697, '风之轻语', 'draw-hero', '王者装备', '2025-07-25 15:48:07', '2025-07-25 15:48:07');
INSERT INTO `word_library` VALUES (1907331092369625698, '凤鸣指环', 'draw-hero', '王者装备', '2025-07-25 15:48:07', '2025-07-25 15:48:07');
INSERT INTO `word_library` VALUES (1907331092369625699, '学识宝石', 'draw-hero', '王者装备', '2025-07-25 15:48:07', '2025-07-25 15:48:07');
INSERT INTO `word_library` VALUES (1907331092369625700, '寒霜侵袭', 'draw-hero', '王者装备', '2025-07-25 15:48:07', '2025-07-25 15:48:07');
INSERT INTO `word_library` VALUES (1907331092369625701, '逐日之弓', 'draw-hero', '王者装备', '2025-07-25 15:48:07', '2025-07-25 15:48:07');
INSERT INTO `word_library` VALUES (1907331092369625702, '宗师之力', 'draw-hero', '王者装备', '2025-07-25 15:48:07', '2025-07-25 15:48:07');
INSERT INTO `word_library` VALUES (1907331092369625703, '辉月', 'draw-hero', '王者装备', '2025-07-25 15:48:07', '2025-07-25 15:48:07');
INSERT INTO `word_library` VALUES (1907331092369625704, '噬神之书', 'draw-hero', '王者装备', '2025-07-25 15:48:07', '2025-07-25 15:48:07');
INSERT INTO `word_library` VALUES (1907331092369625705, '纯净苍穹', 'draw-hero', '王者装备', '2025-07-25 15:48:07', '2025-07-25 15:48:07');
INSERT INTO `word_library` VALUES (1907331092369625706, '贤者的庇护', 'draw-hero', '王者装备', '2025-07-25 15:48:07', '2025-07-25 15:48:07');
INSERT INTO `word_library` VALUES (1907331092369625707, '冰痕之握', 'draw-hero', '王者装备', '2025-07-25 15:48:07', '2025-07-25 15:48:07');
INSERT INTO `word_library` VALUES (1907331092369625708, '血魔之怒', 'draw-hero', '王者装备', '2025-07-25 15:48:07', '2025-07-25 15:48:07');
INSERT INTO `word_library` VALUES (1907331092369625709, '追击刀锋', 'draw-hero', '王者装备', '2025-07-25 15:48:07', '2025-07-25 15:48:07');
INSERT INTO `word_library` VALUES (1907331092369625710, '巡守利斧', 'draw-hero', '王者装备', '2025-07-25 15:48:07', '2025-07-25 15:48:07');
INSERT INTO `word_library` VALUES (1907331092369625711, '游击弯刀', 'draw-hero', '王者装备', '2025-07-25 15:48:07', '2025-07-25 15:48:07');
INSERT INTO `word_library` VALUES (1907331092369625712, '疾步之靴', 'draw-hero', '王者装备', '2025-07-25 15:48:07', '2025-07-25 15:48:07');
INSERT INTO `word_library` VALUES (1907331092369625713, '贤者之书', 'draw-hero', '王者装备', '2025-07-25 15:48:07', '2025-07-25 15:48:07');
INSERT INTO `word_library` VALUES (1907331092369625714, '圣杯', 'draw-hero', '王者装备', '2025-07-25 15:48:07', '2025-07-25 15:48:07');
INSERT INTO `word_library` VALUES (1907331092369625715, '梦魇之牙', 'draw-hero', '王者装备', '2025-07-25 15:48:07', '2025-07-25 15:48:07');
INSERT INTO `word_library` VALUES (1907331092369625716, '元素杖', 'draw-hero', '王者装备', '2025-07-25 15:48:07', '2025-07-25 15:48:07');
INSERT INTO `word_library` VALUES (1907331092369625717, '制裁之刃', 'draw-hero', '王者装备', '2025-07-25 15:48:07', '2025-07-25 15:48:07');
INSERT INTO `word_library` VALUES (1907331092369625718, '速击之枪', 'draw-hero', '王者装备', '2025-07-25 15:48:07', '2025-07-25 15:48:07');
INSERT INTO `word_library` VALUES (1907331092369625719, '冲能拳套', 'draw-hero', '王者装备', '2025-07-25 15:48:07', '2025-07-25 15:48:07');
INSERT INTO `word_library` VALUES (1907331092369625720, '雷鸣刃', 'draw-hero', '王者装备', '2025-07-25 15:48:07', '2025-07-25 15:48:07');
INSERT INTO `word_library` VALUES (1907331092369625721, '狩猎宽刃', 'draw-hero', '王者装备', '2025-07-25 15:48:07', '2025-07-25 15:48:07');
INSERT INTO `word_library` VALUES (1907331092369625722, '巨人之握', 'draw-hero', '王者装备', '2025-07-25 15:48:07', '2025-07-25 15:48:07');
INSERT INTO `word_library` VALUES (1907331092369625723, '秘法之靴', 'draw-hero', '王者装备', '2025-07-25 15:48:07', '2025-07-25 15:48:07');
INSERT INTO `word_library` VALUES (1907331092369625724, '冷静之靴', 'draw-hero', '王者装备', '2025-07-25 15:48:07', '2025-07-25 15:48:07');
INSERT INTO `word_library` VALUES (1907331092369625725, '抵抗之靴', 'draw-hero', '王者装备', '2025-07-25 15:48:07', '2025-07-25 15:48:07');
INSERT INTO `word_library` VALUES (1907331092369625726, '影忍之足', 'draw-hero', '王者装备', '2025-07-25 15:48:07', '2025-07-25 15:48:07');
INSERT INTO `word_library` VALUES (1907331092369625727, '神速之靴', 'draw-hero', '王者装备', '2025-07-25 15:48:07', '2025-07-25 15:48:07');
INSERT INTO `word_library` VALUES (1907331092369625728, '红玛瑙', 'draw-hero', '王者装备', '2025-07-25 15:48:07', '2025-07-25 15:48:07');
INSERT INTO `word_library` VALUES (1907331092369625729, '布甲', 'draw-hero', '王者装备', '2025-07-25 15:48:07', '2025-07-25 15:48:07');
INSERT INTO `word_library` VALUES (1907331092369625730, '抗魔披风', 'draw-hero', '王者装备', '2025-07-25 15:48:07', '2025-07-25 15:48:07');
INSERT INTO `word_library` VALUES (1907331092369625731, '提神水晶', 'draw-hero', '王者装备', '2025-07-25 15:48:07', '2025-07-25 15:48:07');
INSERT INTO `word_library` VALUES (1907331092369625732, '力量腰带', 'draw-hero', '王者装备', '2025-07-25 15:48:07', '2025-07-25 15:48:07');
INSERT INTO `word_library` VALUES (1907331092369625733, '熔炼之心', 'draw-hero', '王者装备', '2025-07-25 15:48:07', '2025-07-25 15:48:07');
INSERT INTO `word_library` VALUES (1907331092369625734, '神隐斗篷', 'draw-hero', '王者装备', '2025-07-25 15:48:07', '2025-07-25 15:48:07');
INSERT INTO `word_library` VALUES (1907331092369625735, '雪山圆盾', 'draw-hero', '王者装备', '2025-07-25 15:48:07', '2025-07-25 15:48:07');
INSERT INTO `word_library` VALUES (1907331092369625736, '守护者之铠', 'draw-hero', '王者装备', '2025-07-25 15:48:07', '2025-07-25 15:48:07');
INSERT INTO `word_library` VALUES (1907331092369625737, '反伤刺甲', 'draw-hero', '王者装备', '2025-07-25 15:48:07', '2025-07-25 15:48:07');
INSERT INTO `word_library` VALUES (1907331092369625738, '红莲斗篷', 'draw-hero', '王者装备', '2025-07-25 15:48:07', '2025-07-25 15:48:07');
INSERT INTO `word_library` VALUES (1907331092369625739, '霸者重装', 'draw-hero', '王者装备', '2025-07-25 15:48:07', '2025-07-25 15:48:07');
INSERT INTO `word_library` VALUES (1907331092369625740, '不详征兆', 'draw-hero', '王者装备', '2025-07-25 15:48:07', '2025-07-25 15:48:07');
INSERT INTO `word_library` VALUES (1907331092369625741, '不死鸟之眼', 'draw-hero', '王者装备', '2025-07-25 15:48:07', '2025-07-25 15:48:07');
INSERT INTO `word_library` VALUES (1907331092369625742, '魔女斗篷', 'draw-hero', '王者装备', '2025-07-25 15:48:07', '2025-07-25 15:48:07');
INSERT INTO `word_library` VALUES (1907331092369625743, '咒术典籍', 'draw-hero', '王者装备', '2025-07-25 15:48:07', '2025-07-25 15:48:07');
INSERT INTO `word_library` VALUES (1907331092369625744, '蓝宝石', 'draw-hero', '王者装备', '2025-07-25 15:48:07', '2025-07-25 15:48:07');
INSERT INTO `word_library` VALUES (1907331092369625745, '炼金护符', 'draw-hero', '王者装备', '2025-07-25 15:48:07', '2025-07-25 15:48:07');
INSERT INTO `word_library` VALUES (1907331092369625746, '圣者法典', 'draw-hero', '王者装备', '2025-07-25 15:48:07', '2025-07-25 15:48:07');
INSERT INTO `word_library` VALUES (1907331092369625747, '大棒', 'draw-hero', '王者装备', '2025-07-25 15:48:07', '2025-07-25 15:48:07');
INSERT INTO `word_library` VALUES (1907331092369625748, '血族之书', 'draw-hero', '王者装备', '2025-07-25 15:48:07', '2025-07-25 15:48:07');
INSERT INTO `word_library` VALUES (1907331092369625749, '光辉之剑', 'draw-hero', '王者装备', '2025-07-25 15:48:07', '2025-07-25 15:48:07');
INSERT INTO `word_library` VALUES (1907331092369625750, '魅影面罩', 'draw-hero', '王者装备', '2025-07-25 15:48:07', '2025-07-25 15:48:07');
INSERT INTO `word_library` VALUES (1907331092369625751, '进化水晶', 'draw-hero', '王者装备', '2025-07-25 15:48:07', '2025-07-25 15:48:07');
INSERT INTO `word_library` VALUES (1907331092369625752, '破碎圣杯', 'draw-hero', '王者装备', '2025-07-25 15:48:07', '2025-07-25 15:48:07');
INSERT INTO `word_library` VALUES (1907331092369625753, '炽热支配者', 'draw-hero', '王者装备', '2025-07-25 15:48:07', '2025-07-25 15:48:07');
INSERT INTO `word_library` VALUES (1907331092369625754, '虚无法杖', 'draw-hero', '王者装备', '2025-07-25 15:48:07', '2025-07-25 15:48:07');
INSERT INTO `word_library` VALUES (1907331092369625755, '博学者之怒', 'draw-hero', '王者装备', '2025-07-25 15:48:07', '2025-07-25 15:48:07');
INSERT INTO `word_library` VALUES (1907331092369625756, '铁剑', 'draw-hero', '王者装备', '2025-07-25 15:48:07', '2025-07-25 15:48:07');
INSERT INTO `word_library` VALUES (1907331092369625757, '匕首', 'draw-hero', '王者装备', '2025-07-25 15:48:07', '2025-07-25 15:48:07');
INSERT INTO `word_library` VALUES (1907331092369625758, '搏击拳套', 'draw-hero', '王者装备', '2025-07-25 15:48:07', '2025-07-25 15:48:07');
INSERT INTO `word_library` VALUES (1907331092369625759, '风暴巨剑', 'draw-hero', '王者装备', '2025-07-25 15:48:07', '2025-07-25 15:48:07');
INSERT INTO `word_library` VALUES (1907331092369625760, '日冕', 'draw-hero', '王者装备', '2025-07-25 15:48:07', '2025-07-25 15:48:07');
INSERT INTO `word_library` VALUES (1907331092369625761, '狂暴双刃', 'draw-hero', '王者装备', '2025-07-25 15:48:07', '2025-07-25 15:48:07');
INSERT INTO `word_library` VALUES (1907331092369625762, '吸血之镰', 'draw-hero', '王者装备', '2025-07-25 15:48:07', '2025-07-25 15:48:07');
INSERT INTO `word_library` VALUES (1907331092369625763, '陨星', 'draw-hero', '王者装备', '2025-07-25 15:48:07', '2025-07-25 15:48:07');
INSERT INTO `word_library` VALUES (1907331092369625764, '破魔刀', 'draw-hero', '王者装备', '2025-07-25 15:48:07', '2025-07-25 15:48:07');
INSERT INTO `word_library` VALUES (1907331092369625765, '末世', 'draw-hero', '王者装备', '2025-07-25 15:48:07', '2025-07-25 15:48:07');
INSERT INTO `word_library` VALUES (1907331092369625766, '名刀司命', 'draw-hero', '王者装备', '2025-07-25 15:48:07', '2025-07-25 15:48:07');
INSERT INTO `word_library` VALUES (1907331092369625767, '贪婪之噬', 'draw-hero', '王者装备', '2025-07-25 15:48:07', '2025-07-25 15:48:07');
INSERT INTO `word_library` VALUES (1907331092369625768, '急速战靴', 'draw-hero', '王者装备', '2025-07-25 15:48:07', '2025-07-25 15:48:07');
INSERT INTO `word_library` VALUES (1907331092369625769, '极寒风暴', 'draw-hero', '王者装备', '2025-07-25 15:48:07', '2025-07-25 15:48:07');
INSERT INTO `word_library` VALUES (1907331092369625770, '暴烈之甲', 'draw-hero', '王者装备', '2025-07-25 15:48:07', '2025-07-25 15:48:07');
INSERT INTO `word_library` VALUES (1907331092369625771, '回响之杖', 'draw-hero', '王者装备', '2025-07-25 15:48:07', '2025-07-25 15:48:07');
INSERT INTO `word_library` VALUES (1907331092369625772, '凝冰之息', 'draw-hero', '王者装备', '2025-07-25 15:48:07', '2025-07-25 15:48:07');
INSERT INTO `word_library` VALUES (1907331092369625773, '痛苦面具', 'draw-hero', '王者装备', '2025-07-25 15:48:07', '2025-07-25 15:48:07');
INSERT INTO `word_library` VALUES (1907331092369625774, '巫术法杖', 'draw-hero', '王者装备', '2025-07-25 15:48:07', '2025-07-25 15:48:07');
INSERT INTO `word_library` VALUES (1907331092369625775, '时之预言', 'draw-hero', '王者装备', '2025-07-25 15:48:07', '2025-07-25 15:48:07');
INSERT INTO `word_library` VALUES (1907331092369625776, '碎星锤', 'draw-hero', '王者装备', '2025-07-25 15:48:07', '2025-07-25 15:48:07');
INSERT INTO `word_library` VALUES (1907331092369625777, '泣血之刃', 'draw-hero', '王者装备', '2025-07-25 15:48:07', '2025-07-25 15:48:07');
INSERT INTO `word_library` VALUES (1907331092369625778, '破军', 'draw-hero', '王者装备', '2025-07-25 15:48:07', '2025-07-25 15:48:07');
INSERT INTO `word_library` VALUES (1907331092369625779, '闪电匕首', 'draw-hero', '王者装备', '2025-07-25 15:48:07', '2025-07-25 15:48:07');
INSERT INTO `word_library` VALUES (1907331092369625780, '影刃', 'draw-hero', '王者装备', '2025-07-25 15:48:07', '2025-07-25 15:48:07');
INSERT INTO `word_library` VALUES (1907331092369625781, '暗影战斧', 'draw-hero', '王者装备', '2025-07-25 15:48:07', '2025-07-25 15:48:07');
INSERT INTO `word_library` VALUES (1907331092369625782, '画蛇添足', 'draw-idiom', '成语', '2025-07-25 15:57:45', '2025-07-25 15:57:45');
INSERT INTO `word_library` VALUES (1907331092369625783, '守株待兔', 'draw-idiom', '成语', '2025-07-25 15:57:45', '2025-07-25 15:57:45');
INSERT INTO `word_library` VALUES (1907331092369625784, '亡羊补牢', 'draw-idiom', '成语', '2025-07-25 15:57:45', '2025-07-25 15:57:45');
INSERT INTO `word_library` VALUES (1907331092369625785, '鹤立鸡群', 'draw-idiom', '成语', '2025-07-25 15:57:45', '2025-07-25 15:57:45');
INSERT INTO `word_library` VALUES (1907331092369625786, '鸡飞蛋打', 'draw-idiom', '成语', '2025-07-25 15:57:45', '2025-07-25 15:57:45');
INSERT INTO `word_library` VALUES (1907331092369625787, '鱼目混珠', 'draw-idiom', '成语', '2025-07-25 15:57:45', '2025-07-25 15:57:45');
INSERT INTO `word_library` VALUES (1907331092369625788, '狐假虎威', 'draw-idiom', '成语', '2025-07-25 15:57:45', '2025-07-25 15:57:45');
INSERT INTO `word_library` VALUES (1907331092369625789, '井底之蛙', 'draw-idiom', '成语', '2025-07-25 15:57:45', '2025-07-25 15:57:45');
INSERT INTO `word_library` VALUES (1907331092369625790, '如鱼得水', 'draw-idiom', '成语', '2025-07-25 15:57:45', '2025-07-25 15:57:45');
INSERT INTO `word_library` VALUES (1907331092369625791, '叶公好龙', 'draw-idiom', '成语', '2025-07-25 15:57:45', '2025-07-25 15:57:45');
INSERT INTO `word_library` VALUES (1907331092369625792, '对牛弹琴', 'draw-idiom', '成语', '2025-07-25 15:57:45', '2025-07-25 15:57:45');
INSERT INTO `word_library` VALUES (1907331092369625793, '蜻蜓点水', 'draw-idiom', '成语', '2025-07-25 15:57:45', '2025-07-25 15:57:45');
INSERT INTO `word_library` VALUES (1907331092369625794, '螳臂当车', 'draw-idiom', '成语', '2025-07-25 15:57:45', '2025-07-25 15:57:45');
INSERT INTO `word_library` VALUES (1907331092369625795, '杯弓蛇影', 'draw-idiom', '成语', '2025-07-25 15:57:45', '2025-07-25 15:57:45');
INSERT INTO `word_library` VALUES (1907331092369625796, '鹬蚌相争', 'draw-idiom', '成语', '2025-07-25 15:57:45', '2025-07-25 15:57:45');
INSERT INTO `word_library` VALUES (1907331092369625797, '鸟语花香', 'draw-idiom', '成语', '2025-07-25 15:57:45', '2025-07-25 15:57:45');
INSERT INTO `word_library` VALUES (1907331092369625798, '望穿秋水', 'draw-idiom', '成语', '2025-07-25 15:57:45', '2025-07-25 15:57:45');
INSERT INTO `word_library` VALUES (1907331092369625799, '水到渠成', 'draw-idiom', '成语', '2025-07-25 15:57:45', '2025-07-25 15:57:45');
INSERT INTO `word_library` VALUES (1907331092369625800, '山穷水尽', 'draw-idiom', '成语', '2025-07-25 15:57:45', '2025-07-25 15:57:45');
INSERT INTO `word_library` VALUES (1907331092369625801, '高山流水', 'draw-idiom', '成语', '2025-07-25 15:57:45', '2025-07-25 15:57:45');
INSERT INTO `word_library` VALUES (1907331092369625802, '风和日丽', 'draw-idiom', '成语', '2025-07-25 15:57:45', '2025-07-25 15:57:45');
INSERT INTO `word_library` VALUES (1907331092369625803, '风雨同舟', 'draw-idiom', '成语', '2025-07-25 15:57:45', '2025-07-25 15:57:45');
INSERT INTO `word_library` VALUES (1907331092369625804, '风调雨顺', 'draw-idiom', '成语', '2025-07-25 15:57:45', '2025-07-25 15:57:45');
INSERT INTO `word_library` VALUES (1907331092369625805, '雨后春笋', 'draw-idiom', '成语', '2025-07-25 15:57:45', '2025-07-25 15:57:45');
INSERT INTO `word_library` VALUES (1907331092369625806, '如火如荼', 'draw-idiom', '成语', '2025-07-25 15:57:45', '2025-07-25 15:57:45');
INSERT INTO `word_library` VALUES (1907331092369625807, '滴水穿石', 'draw-idiom', '成语', '2025-07-25 15:57:45', '2025-07-25 15:57:45');
INSERT INTO `word_library` VALUES (1907331092369625808, '洛阳纸贵', 'draw-idiom', '成语', '2025-07-25 15:57:45', '2025-07-25 15:57:45');
INSERT INTO `word_library` VALUES (1907331092369625809, '纸上谈兵', 'draw-idiom', '成语', '2025-07-25 15:57:45', '2025-07-25 15:57:45');
INSERT INTO `word_library` VALUES (1907331092369625810, '金玉满堂', 'draw-idiom', '成语', '2025-07-25 15:57:45', '2025-07-25 15:57:45');
INSERT INTO `word_library` VALUES (1907331092369625811, '掌上明珠', 'draw-idiom', '成语', '2025-07-25 15:57:45', '2025-07-25 15:57:45');
INSERT INTO `word_library` VALUES (1907331092369625812, '明珠暗投', 'draw-idiom', '成语', '2025-07-25 15:57:45', '2025-07-25 15:57:45');
INSERT INTO `word_library` VALUES (1907331092369625813, '一针见血', 'draw-idiom', '成语', '2025-07-25 15:57:45', '2025-07-25 15:57:45');
INSERT INTO `word_library` VALUES (1907331092369625814, '刀光剑影', 'draw-idiom', '成语', '2025-07-25 15:57:45', '2025-07-25 15:57:45');
INSERT INTO `word_library` VALUES (1907331092369625815, '一石二鸟', 'draw-idiom', '成语', '2025-07-25 15:57:45', '2025-07-25 15:57:45');
INSERT INTO `word_library` VALUES (1907331092369625816, '画龙点睛', 'draw-idiom', '成语', '2025-07-25 15:57:45', '2025-07-25 15:57:45');
INSERT INTO `word_library` VALUES (1907331092369625817, '锦上添花', 'draw-idiom', '成语', '2025-07-25 15:57:45', '2025-07-25 15:57:45');
INSERT INTO `word_library` VALUES (1907331092369625818, '四面楚歌', 'draw-idiom', '成语', '2025-07-25 15:57:45', '2025-07-25 15:57:45');
INSERT INTO `word_library` VALUES (1907331092369625819, '一鸣惊人', 'draw-idiom', '成语', '2025-07-25 15:57:45', '2025-07-25 15:57:45');
INSERT INTO `word_library` VALUES (1907331092369625820, '一目了然', 'draw-idiom', '成语', '2025-07-25 15:57:45', '2025-07-25 15:57:45');
INSERT INTO `word_library` VALUES (1907331092369625821, '手舞足蹈', 'draw-idiom', '成语', '2025-07-25 15:57:45', '2025-07-25 15:57:45');
INSERT INTO `word_library` VALUES (1907331092369625822, '手忙脚乱', 'draw-idiom', '成语', '2025-07-25 15:57:45', '2025-07-25 15:57:45');
INSERT INTO `word_library` VALUES (1907331092369625823, '眼高手低', 'draw-idiom', '成语', '2025-07-25 15:57:45', '2025-07-25 15:57:45');
INSERT INTO `word_library` VALUES (1907331092369625824, '目不转睛', 'draw-idiom', '成语', '2025-07-25 15:57:45', '2025-07-25 15:57:45');
INSERT INTO `word_library` VALUES (1907331092369625825, '眼花缭乱', 'draw-idiom', '成语', '2025-07-25 15:57:45', '2025-07-25 15:57:45');
INSERT INTO `word_library` VALUES (1907331092369625826, '手到擒来', 'draw-idiom', '成语', '2025-07-25 15:57:45', '2025-07-25 15:57:45');
INSERT INTO `word_library` VALUES (1907331092369625827, '手足无措', 'draw-idiom', '成语', '2025-07-25 15:57:45', '2025-07-25 15:57:45');
INSERT INTO `word_library` VALUES (1907331092369625828, '心口如一', 'draw-idiom', '成语', '2025-07-25 15:57:45', '2025-07-25 15:57:45');
INSERT INTO `word_library` VALUES (1907331092369625829, '口若悬河', 'draw-idiom', '成语', '2025-07-25 15:57:45', '2025-07-25 15:57:45');
INSERT INTO `word_library` VALUES (1907331092369625830, '胸有成竹', 'draw-idiom', '成语', '2025-07-25 15:57:45', '2025-07-25 15:57:45');
INSERT INTO `word_library` VALUES (1907331092369625831, '背水一战', 'draw-idiom', '成语', '2025-07-25 15:57:45', '2025-07-25 15:57:45');
INSERT INTO `word_library` VALUES (1907331092369625832, '四分五裂', 'draw-idiom', '成语', '2025-07-25 15:57:45', '2025-07-25 15:57:45');
INSERT INTO `word_library` VALUES (1907331092369625833, '七上八下', 'draw-idiom', '成语', '2025-07-25 15:57:45', '2025-07-25 15:57:45');
INSERT INTO `word_library` VALUES (1907331092369625834, '三思而行', 'draw-idiom', '成语', '2025-07-25 15:57:45', '2025-07-25 15:57:45');
INSERT INTO `word_library` VALUES (1907331092369625835, '九牛一毛', 'draw-idiom', '成语', '2025-07-25 15:57:45', '2025-07-25 15:57:45');
INSERT INTO `word_library` VALUES (1907331092369625836, '八仙过海', 'draw-idiom', '成语', '2025-07-25 15:57:45', '2025-07-25 15:57:45');
INSERT INTO `word_library` VALUES (1907331092369625837, '一步登天', 'draw-idiom', '成语', '2025-07-25 15:57:45', '2025-07-25 15:57:45');
INSERT INTO `word_library` VALUES (1907331092369625838, '千军万马', 'draw-idiom', '成语', '2025-07-25 15:57:45', '2025-07-25 15:57:45');
INSERT INTO `word_library` VALUES (1907331092369625839, '万人空巷', 'draw-idiom', '成语', '2025-07-25 15:57:45', '2025-07-25 15:57:45');
INSERT INTO `word_library` VALUES (1907331092369625840, '百发百中', 'draw-idiom', '成语', '2025-07-25 15:57:45', '2025-07-25 15:57:45');
INSERT INTO `word_library` VALUES (1907331092369625841, '十全十美', 'draw-idiom', '成语', '2025-07-25 15:57:45', '2025-07-25 15:57:45');
INSERT INTO `word_library` VALUES (1907331092369625842, '五体投地', 'draw-idiom', '成语', '2025-07-25 15:57:45', '2025-07-25 15:57:45');
INSERT INTO `word_library` VALUES (1907331092369625843, '六神无主', 'draw-idiom', '成语', '2025-07-25 15:57:45', '2025-07-25 15:57:45');
INSERT INTO `word_library` VALUES (1907331092369625844, '四通八达', 'draw-idiom', '成语', '2025-07-25 15:57:45', '2025-07-25 15:57:45');
INSERT INTO `word_library` VALUES (1907331092369625845, '一日千里', 'draw-idiom', '成语', '2025-07-25 15:57:45', '2025-07-25 15:57:45');
INSERT INTO `word_library` VALUES (1907331092369625846, '半途而废', 'draw-idiom', '成语', '2025-07-25 15:57:45', '2025-07-25 15:57:45');
INSERT INTO `word_library` VALUES (1907331092369625847, '虎头蛇尾', 'draw-idiom', '成语', '2025-07-25 15:57:45', '2025-07-25 15:57:45');
INSERT INTO `word_library` VALUES (1907331092369625848, '走马观花', 'draw-idiom', '成语', '2025-07-25 15:57:45', '2025-07-25 15:57:45');
INSERT INTO `word_library` VALUES (1907331092369625849, '左右逢源', 'draw-idiom', '成语', '2025-07-25 15:57:45', '2025-07-25 15:57:45');
INSERT INTO `word_library` VALUES (1907331092369625850, '前呼后拥', 'draw-idiom', '成语', '2025-07-25 15:57:45', '2025-07-25 15:57:45');
INSERT INTO `word_library` VALUES (1907331092369625851, '东张西望', 'draw-idiom', '成语', '2025-07-25 15:57:45', '2025-07-25 15:57:45');
INSERT INTO `word_library` VALUES (1907331092369625852, '南辕北辙', 'draw-idiom', '成语', '2025-07-25 15:57:45', '2025-07-25 15:57:45');
INSERT INTO `word_library` VALUES (1907331092369625853, '天上人间', 'draw-idiom', '成语', '2025-07-25 15:57:45', '2025-07-25 15:57:45');
INSERT INTO `word_library` VALUES (1907331092369625854, '天长地久', 'draw-idiom', '成语', '2025-07-25 15:57:45', '2025-07-25 15:57:45');
INSERT INTO `word_library` VALUES (1907331092369625855, '天衣无缝', 'draw-idiom', '成语', '2025-07-25 15:57:45', '2025-07-25 15:57:45');
INSERT INTO `word_library` VALUES (1907331092369625856, '地动山摇', 'draw-idiom', '成语', '2025-07-25 15:57:45', '2025-07-25 15:57:45');
INSERT INTO `word_library` VALUES (1907331092369625857, '四海为家', 'draw-idiom', '成语', '2025-07-25 15:57:45', '2025-07-25 15:57:45');
INSERT INTO `word_library` VALUES (1907331092369625858, '四面八方', 'draw-idiom', '成语', '2025-07-25 15:57:45', '2025-07-25 15:57:45');
INSERT INTO `word_library` VALUES (1907331092369625859, '入木三分', 'draw-idiom', '成语', '2025-07-25 15:57:45', '2025-07-25 15:57:45');
INSERT INTO `word_library` VALUES (1907331092369625860, '指鹿为马', 'draw-idiom', '成语', '2025-07-25 15:57:45', '2025-07-25 15:57:45');
INSERT INTO `word_library` VALUES (1907331092369625861, '草木皆兵', 'draw-idiom', '成语', '2025-07-25 15:57:45', '2025-07-25 15:57:45');
INSERT INTO `word_library` VALUES (1907331092369625862, '望梅止渴', 'draw-idiom', '成语', '2025-07-25 15:57:45', '2025-07-25 15:57:45');
INSERT INTO `word_library` VALUES (1907331092369625863, '完璧归赵', 'draw-idiom', '成语', '2025-07-25 15:57:45', '2025-07-25 15:57:45');
INSERT INTO `word_library` VALUES (1907331092369625864, '精卫填海', 'draw-idiom', '成语', '2025-07-25 15:57:45', '2025-07-25 15:57:45');
INSERT INTO `word_library` VALUES (1907331092369625865, '愚公移山', 'draw-idiom', '成语', '2025-07-25 15:57:45', '2025-07-25 15:57:45');
INSERT INTO `word_library` VALUES (1907331092369625866, '盲人摸象', 'draw-idiom', '成语', '2025-07-25 15:57:45', '2025-07-25 15:57:45');
INSERT INTO `word_library` VALUES (1907331092369625867, '坐井观天', 'draw-idiom', '成语', '2025-07-25 15:57:45', '2025-07-25 15:57:45');
INSERT INTO `word_library` VALUES (1907331092369625868, '刻舟求剑', 'draw-idiom', '成语', '2025-07-25 15:57:45', '2025-07-25 15:57:45');
INSERT INTO `word_library` VALUES (1907331092369625869, '杞人忧天', 'draw-idiom', '成语', '2025-07-25 15:57:45', '2025-07-25 15:57:45');
INSERT INTO `word_library` VALUES (1907331092369625870, '画饼充饥', 'draw-idiom', '成语', '2025-07-25 15:57:45', '2025-07-25 15:57:45');
INSERT INTO `word_library` VALUES (1907331092369625871, '掩耳盗铃', 'draw-idiom', '成语', '2025-07-25 15:57:45', '2025-07-25 15:57:45');
INSERT INTO `word_library` VALUES (1907331092369625872, '自相矛盾', 'draw-idiom', '成语', '2025-07-25 15:57:45', '2025-07-25 15:57:45');
INSERT INTO `word_library` VALUES (1907331092369625873, '拔苗助长', 'draw-idiom', '成语', '2025-07-25 15:57:45', '2025-07-25 15:57:45');
INSERT INTO `word_library` VALUES (1907331092369625874, '一叶障目', 'draw-idiom', '成语', '2025-07-25 15:57:45', '2025-07-25 15:57:45');
INSERT INTO `word_library` VALUES (1907331092369625875, '沧海一粟', 'draw-idiom', '成语', '2025-07-25 15:57:45', '2025-07-25 15:57:45');
INSERT INTO `word_library` VALUES (1907331092369625876, '大器晚成', 'draw-idiom', '成语', '2025-07-25 15:57:45', '2025-07-25 15:57:45');
INSERT INTO `word_library` VALUES (1907331092369625877, '胆大心细', 'draw-idiom', '成语', '2025-07-25 15:57:45', '2025-07-25 15:57:45');
INSERT INTO `word_library` VALUES (1907331092369625878, '胆小如鼠', 'draw-idiom', '成语', '2025-07-25 15:57:45', '2025-07-25 15:57:45');
INSERT INTO `word_library` VALUES (1907331092369625879, '当机立断', 'draw-idiom', '成语', '2025-07-25 15:57:45', '2025-07-25 15:57:45');
INSERT INTO `word_library` VALUES (1907331092369625880, '道听途说', 'draw-idiom', '成语', '2025-07-25 15:57:45', '2025-07-25 15:57:45');
INSERT INTO `word_library` VALUES (1907331092369625881, '得意忘形', 'draw-idiom', '成语', '2025-07-25 15:57:45', '2025-07-25 15:57:45');
INSERT INTO `word_library` VALUES (1907331092369625882, '灯红酒绿', 'draw-idiom', '成语', '2025-07-25 15:57:45', '2025-07-25 15:57:45');
INSERT INTO `word_library` VALUES (1907331092369625883, '东山再起', 'draw-idiom', '成语', '2025-07-25 15:57:45', '2025-07-25 15:57:45');
INSERT INTO `word_library` VALUES (1907331092369625884, '独树一帜', 'draw-idiom', '成语', '2025-07-25 15:57:45', '2025-07-25 15:57:45');
INSERT INTO `word_library` VALUES (1907331092369625885, '耳濡目染', 'draw-idiom', '成语', '2025-07-25 15:57:45', '2025-07-25 15:57:45');
INSERT INTO `word_library` VALUES (1907331092369625886, '翻云覆雨', 'draw-idiom', '成语', '2025-07-25 15:57:45', '2025-07-25 15:57:45');
INSERT INTO `word_library` VALUES (1907331092369625887, '风尘仆仆', 'draw-idiom', '成语', '2025-07-25 15:57:45', '2025-07-25 15:57:45');
INSERT INTO `word_library` VALUES (1907331092369625888, '高屋建瓴', 'draw-idiom', '成语', '2025-07-25 15:57:45', '2025-07-25 15:57:45');
INSERT INTO `word_library` VALUES (1907331092369625889, '各行其是', 'draw-idiom', '成语', '2025-07-25 15:57:45', '2025-07-25 15:57:45');
INSERT INTO `word_library` VALUES (1907331092369625890, '固若金汤', 'draw-idiom', '成语', '2025-07-25 15:57:45', '2025-07-25 15:57:45');
INSERT INTO `word_library` VALUES (1907331092369625891, '瓜田李下', 'draw-idiom', '成语', '2025-07-25 15:57:45', '2025-07-25 15:57:45');
INSERT INTO `word_library` VALUES (1907331092369625892, '鬼斧神工', 'draw-idiom', '成语', '2025-07-25 15:57:45', '2025-07-25 15:57:45');
INSERT INTO `word_library` VALUES (1907331092369625893, '含沙射影', 'draw-idiom', '成语', '2025-07-25 15:57:45', '2025-07-25 15:57:45');
INSERT INTO `word_library` VALUES (1907331092369625894, '和风细雨', 'draw-idiom', '成语', '2025-07-25 15:57:45', '2025-07-25 15:57:45');
INSERT INTO `word_library` VALUES (1907331092369625895, '横扫千军', 'draw-idiom', '成语', '2025-07-25 15:57:45', '2025-07-25 15:57:45');
INSERT INTO `word_library` VALUES (1907331092369625896, '厚积薄发', 'draw-idiom', '成语', '2025-07-25 15:57:45', '2025-07-25 15:57:45');
INSERT INTO `word_library` VALUES (1907331092369625897, '花团锦簇', 'draw-idiom', '成语', '2025-07-25 15:57:45', '2025-07-25 15:57:45');
INSERT INTO `word_library` VALUES (1907331092369625898, '魂飞魄散', 'draw-idiom', '成语', '2025-07-25 15:57:45', '2025-07-25 15:57:45');
INSERT INTO `word_library` VALUES (1907331092369625899, '火上浇油', 'draw-idiom', '成语', '2025-07-25 15:57:45', '2025-07-25 15:57:45');
INSERT INTO `word_library` VALUES (1907331092369625900, '鸡鸣狗盗', 'draw-idiom', '成语', '2025-07-25 15:57:45', '2025-07-25 15:57:45');
INSERT INTO `word_library` VALUES (1907331092369625901, '集思广益', 'draw-idiom', '成语', '2025-07-25 15:57:45', '2025-07-25 15:57:45');
INSERT INTO `word_library` VALUES (1907331092369625902, '继往开来', 'draw-idiom', '成语', '2025-07-25 15:57:45', '2025-07-25 15:57:45');
INSERT INTO `word_library` VALUES (1907331092369625903, '见微知著', 'draw-idiom', '成语', '2025-07-25 15:57:45', '2025-07-25 15:57:45');
INSERT INTO `word_library` VALUES (1907331092369625904, '江郎才尽', 'draw-idiom', '成语', '2025-07-25 15:57:45', '2025-07-25 15:57:45');
INSERT INTO `word_library` VALUES (1907331092369625905, '脚踏实地', 'draw-idiom', '成语', '2025-07-25 15:57:45', '2025-07-25 15:57:45');
INSERT INTO `word_library` VALUES (1907331092369625906, '金戈铁马', 'draw-idiom', '成语', '2025-07-25 15:57:45', '2025-07-25 15:57:45');
INSERT INTO `word_library` VALUES (1907331092369625907, '锦绣前程', 'draw-idiom', '成语', '2025-07-25 15:57:45', '2025-07-25 15:57:45');
INSERT INTO `word_library` VALUES (1907331092369625908, '精益求精', 'draw-idiom', '成语', '2025-07-25 15:57:45', '2025-07-25 15:57:45');
INSERT INTO `word_library` VALUES (1907331092369625909, '举一反三', 'draw-idiom', '成语', '2025-07-25 15:57:45', '2025-07-25 15:57:45');
INSERT INTO `word_library` VALUES (1907331092369625910, '绝处逢生', 'draw-idiom', '成语', '2025-07-25 15:57:45', '2025-07-25 15:57:45');
INSERT INTO `word_library` VALUES (1907331092369625911, '开门见山', 'draw-idiom', '成语', '2025-07-25 15:57:45', '2025-07-25 15:57:45');
INSERT INTO `word_library` VALUES (1907331092369625912, '空穴来风', 'draw-idiom', '成语', '2025-07-25 15:57:45', '2025-07-25 15:57:45');
INSERT INTO `word_library` VALUES (1907331092369625913, '苦心孤诣', 'draw-idiom', '成语', '2025-07-25 15:57:45', '2025-07-25 15:57:45');
INSERT INTO `word_library` VALUES (1907331092369625914, '狼吞虎咽', 'draw-idiom', '成语', '2025-07-25 15:57:45', '2025-07-25 15:57:45');
INSERT INTO `word_library` VALUES (1907331092369625915, '老马识途', 'draw-idiom', '成语', '2025-07-25 15:57:45', '2025-07-25 15:57:45');
INSERT INTO `word_library` VALUES (1907331092369625916, '雷厉风行', 'draw-idiom', '成语', '2025-07-25 15:57:45', '2025-07-25 15:57:45');
INSERT INTO `word_library` VALUES (1907331092369625917, '冷若冰霜', 'draw-idiom', '成语', '2025-07-25 15:57:45', '2025-07-25 15:57:45');
INSERT INTO `word_library` VALUES (1907331092369625918, '量体裁衣', 'draw-idiom', '成语', '2025-07-25 15:57:45', '2025-07-25 15:57:45');
INSERT INTO `word_library` VALUES (1907331092369625919, '临危不惧', 'draw-idiom', '成语', '2025-07-25 15:57:45', '2025-07-25 15:57:45');
INSERT INTO `word_library` VALUES (1907331092369625920, '炉火纯青', 'draw-idiom', '成语', '2025-07-25 15:57:45', '2025-07-25 15:57:45');
INSERT INTO `word_library` VALUES (1907331092369625921, '屡试不爽', 'draw-idiom', '成语', '2025-07-25 15:57:45', '2025-07-25 15:57:45');
INSERT INTO `word_library` VALUES (1907331092369625922, '落井下石', 'draw-idiom', '成语', '2025-07-25 15:57:45', '2025-07-25 15:57:45');
INSERT INTO `word_library` VALUES (1907331092369625923, '马到成功', 'draw-idiom', '成语', '2025-07-25 15:57:45', '2025-07-25 15:57:45');
INSERT INTO `word_library` VALUES (1907331092369625924, '满腹经纶', 'draw-idiom', '成语', '2025-07-25 15:57:45', '2025-07-25 15:57:45');
INSERT INTO `word_library` VALUES (1907331092369625925, '毛遂自荐', 'draw-idiom', '成语', '2025-07-25 15:57:45', '2025-07-25 15:57:45');
INSERT INTO `word_library` VALUES (1907331092369625926, '妙手回春', 'draw-idiom', '成语', '2025-07-25 15:57:45', '2025-07-25 15:57:45');
INSERT INTO `word_library` VALUES (1907331092369625927, '明察秋毫', 'draw-idiom', '成语', '2025-07-25 15:57:45', '2025-07-25 15:57:45');
INSERT INTO `word_library` VALUES (1907331092369625928, '明枪暗箭', 'draw-idiom', '成语', '2025-07-25 15:57:45', '2025-07-25 15:57:45');
INSERT INTO `word_library` VALUES (1907331092369625929, '目无全牛', 'draw-idiom', '成语', '2025-07-25 15:57:45', '2025-07-25 15:57:45');
INSERT INTO `word_library` VALUES (1907331092369625931, '泥牛入海', 'draw-idiom', '成语', '2025-07-25 15:57:45', '2025-07-25 15:57:45');
INSERT INTO `word_library` VALUES (1907331092369625932, '逆水行舟', 'draw-idiom', '成语', '2025-07-25 15:57:45', '2025-07-25 15:57:45');
INSERT INTO `word_library` VALUES (1907331092369625933, '浓墨重彩', 'draw-idiom', '成语', '2025-07-25 15:57:45', '2025-07-25 15:57:45');
INSERT INTO `word_library` VALUES (1907331092369625934, '抛砖引玉', 'draw-idiom', '成语', '2025-07-25 15:57:45', '2025-07-25 15:57:45');
INSERT INTO `word_library` VALUES (1907331092369625935, '披荆斩棘', 'draw-idiom', '成语', '2025-07-25 15:57:45', '2025-07-25 15:57:45');
INSERT INTO `word_library` VALUES (1907331092369625936, '平步青云', 'draw-idiom', '成语', '2025-07-25 15:57:45', '2025-07-25 15:57:45');
INSERT INTO `word_library` VALUES (1907331092369625937, '破釜沉舟', 'draw-idiom', '成语', '2025-07-25 15:57:45', '2025-07-25 15:57:45');
INSERT INTO `word_library` VALUES (1907331092369625938, '七手八脚', 'draw-idiom', '成语', '2025-07-25 15:57:45', '2025-07-25 15:57:45');
INSERT INTO `word_library` VALUES (1907331092369625939, '千钧一发', 'draw-idiom', '成语', '2025-07-25 15:57:45', '2025-07-25 15:57:45');
INSERT INTO `word_library` VALUES (1907331092369625940, '千篇一律', 'draw-idiom', '成语', '2025-07-25 15:57:45', '2025-07-25 15:57:45');
INSERT INTO `word_library` VALUES (1907331092369625941, '千丝万缕', 'draw-idiom', '成语', '2025-07-25 15:57:45', '2025-07-25 15:57:45');
INSERT INTO `word_library` VALUES (1907331092369625942, '前车之鉴', 'draw-idiom', '成语', '2025-07-25 15:57:45', '2025-07-25 15:57:45');
INSERT INTO `word_library` VALUES (1907331092369625943, '前仆后继', 'draw-idiom', '成语', '2025-07-25 15:57:45', '2025-07-25 15:57:45');
INSERT INTO `word_library` VALUES (1907331092369625944, '青梅竹马', 'draw-idiom', '成语', '2025-07-25 15:57:45', '2025-07-25 15:57:45');
INSERT INTO `word_library` VALUES (1907331092369625945, '轻而易举', 'draw-idiom', '成语', '2025-07-25 15:57:45', '2025-07-25 15:57:45');
INSERT INTO `word_library` VALUES (1907331092369625946, '倾国倾城', 'draw-idiom', '成语', '2025-07-25 15:57:45', '2025-07-25 15:57:45');
INSERT INTO `word_library` VALUES (1907331092369625947, '穷途末路', 'draw-idiom', '成语', '2025-07-25 15:57:45', '2025-07-25 15:57:45');
INSERT INTO `word_library` VALUES (1907331092369625948, '群雄逐鹿', 'draw-idiom', '成语', '2025-07-25 15:57:45', '2025-07-25 15:57:45');
INSERT INTO `word_library` VALUES (1907331092369625949, '人山人海', 'draw-idiom', '成语', '2025-07-25 15:57:45', '2025-07-25 15:57:45');
INSERT INTO `word_library` VALUES (1907331092369625950, '如履薄冰', 'draw-idiom', '成语', '2025-07-25 15:57:45', '2025-07-25 15:57:45');
INSERT INTO `word_library` VALUES (1907331092369625951, '如日中天', 'draw-idiom', '成语', '2025-07-25 15:57:45', '2025-07-25 15:57:45');
INSERT INTO `word_library` VALUES (1907331092369625952, '三顾茅庐', 'draw-idiom', '成语', '2025-07-25 15:57:45', '2025-07-25 15:57:45');
INSERT INTO `word_library` VALUES (1907331092369625953, '三言两语', 'draw-idiom', '成语', '2025-07-25 15:57:45', '2025-07-25 15:57:45');
INSERT INTO `word_library` VALUES (1907331092369625954, '杀鸡儆猴', 'draw-idiom', '成语', '2025-07-25 15:57:45', '2025-07-25 15:57:45');
INSERT INTO `word_library` VALUES (1907331092369625955, '煞费苦心', 'draw-idiom', '成语', '2025-07-25 15:57:45', '2025-07-25 15:57:45');
INSERT INTO `word_library` VALUES (1907331092369625956, '上下其手', 'draw-idiom', '成语', '2025-07-25 15:57:45', '2025-07-25 15:57:45');
INSERT INTO `word_library` VALUES (1907331092369625957, '身先士卒', 'draw-idiom', '成语', '2025-07-25 15:57:45', '2025-07-25 15:57:45');
INSERT INTO `word_library` VALUES (1907331092369625958, '生龙活虎', 'draw-idiom', '成语', '2025-07-25 15:57:45', '2025-07-25 15:57:45');
INSERT INTO `word_library` VALUES (1907331092369625959, '师出有名', 'draw-idiom', '成语', '2025-07-25 15:57:45', '2025-07-25 15:57:45');
INSERT INTO `word_library` VALUES (1907331092369625960, '石破天惊', 'draw-idiom', '成语', '2025-07-25 15:57:45', '2025-07-25 15:57:45');
INSERT INTO `word_library` VALUES (1907331092369625961, '势如破竹', 'draw-idiom', '成语', '2025-07-25 15:57:45', '2025-07-25 15:57:45');
INSERT INTO `word_library` VALUES (1907331092369625962, '手不释卷', 'draw-idiom', '成语', '2025-07-25 15:57:45', '2025-07-25 15:57:45');
INSERT INTO `word_library` VALUES (1907331092369625963, '水滴石穿', 'draw-idiom', '成语', '2025-07-25 15:57:45', '2025-07-25 15:57:45');
INSERT INTO `word_library` VALUES (1907331092369625964, '水落石出', 'draw-idiom', '成语', '2025-07-25 15:57:45', '2025-07-25 15:57:45');
INSERT INTO `word_library` VALUES (1907331092369625965, '司空见惯', 'draw-idiom', '成语', '2025-07-25 15:57:45', '2025-07-25 15:57:45');
INSERT INTO `word_library` VALUES (1907331092369625967, '死灰复燃', 'draw-idiom', '成语', '2025-07-25 15:57:45', '2025-07-25 15:57:45');
INSERT INTO `word_library` VALUES (1907331092369625968, '随波逐流', 'draw-idiom', '成语', '2025-07-25 15:57:45', '2025-07-25 15:57:45');
INSERT INTO `word_library` VALUES (1907331092369625969, '谈笑风生', 'draw-idiom', '成语', '2025-07-25 15:57:45', '2025-07-25 15:57:45');
INSERT INTO `word_library` VALUES (1907331092369625970, '谈虎色变', 'draw-idiom', '成语', '2025-07-25 15:57:45', '2025-07-25 15:57:45');
INSERT INTO `word_library` VALUES (1907331092369625971, '滔滔不绝', 'draw-idiom', '成语', '2025-07-25 15:57:45', '2025-07-25 15:57:45');
INSERT INTO `word_library` VALUES (1907331092369625972, '天方夜谭', 'draw-idiom', '成语', '2025-07-25 15:57:45', '2025-07-25 15:57:45');
INSERT INTO `word_library` VALUES (1907331092369625973, '天花乱坠', 'draw-idiom', '成语', '2025-07-25 15:57:45', '2025-07-25 15:57:45');
INSERT INTO `word_library` VALUES (1907331092369625974, '天经地义', 'draw-idiom', '成语', '2025-07-25 15:57:45', '2025-07-25 15:57:45');
INSERT INTO `word_library` VALUES (1907331092369625975, '天罗地网', 'draw-idiom', '成语', '2025-07-25 15:57:45', '2025-07-25 15:57:45');
INSERT INTO `word_library` VALUES (1907331092369625976, '天马行空', 'draw-idiom', '成语', '2025-07-25 15:57:45', '2025-07-25 15:57:45');
INSERT INTO `word_library` VALUES (1907331092369625977, '天南地北', 'draw-idiom', '成语', '2025-07-25 15:57:45', '2025-07-25 15:57:45');
INSERT INTO `word_library` VALUES (1907331092369625978, '天涯海角', 'draw-idiom', '成语', '2025-07-25 15:57:45', '2025-07-25 15:57:45');
INSERT INTO `word_library` VALUES (1907331092369625979, '铁杵成针', 'draw-idiom', '成语', '2025-07-25 15:57:45', '2025-07-25 15:57:45');
INSERT INTO `word_library` VALUES (1907331092369625980, '铁证如山', 'draw-idiom', '成语', '2025-07-25 15:57:45', '2025-07-25 15:57:45');
INSERT INTO `word_library` VALUES (1907331092369625981, '投桃报李', 'draw-idiom', '成语', '2025-07-25 15:57:45', '2025-07-25 15:57:45');
INSERT INTO `word_library` VALUES (1907331092369625982, '脱颖而出', 'draw-idiom', '成语', '2025-07-25 15:57:45', '2025-07-25 15:57:45');
INSERT INTO `word_library` VALUES (1907331092369625984, '万马奔腾', 'draw-idiom', '成语', '2025-07-25 15:57:45', '2025-07-25 15:57:45');
INSERT INTO `word_library` VALUES (1907331092369625985, '万水千山', 'draw-idiom', '成语', '2025-07-25 15:57:45', '2025-07-25 15:57:45');
INSERT INTO `word_library` VALUES (1907331092369625986, '望尘莫及', 'draw-idiom', '成语', '2025-07-25 15:57:45', '2025-07-25 15:57:45');
INSERT INTO `word_library` VALUES (1907331092369625987, '望洋兴叹', 'draw-idiom', '成语', '2025-07-25 15:57:45', '2025-07-25 15:57:45');
INSERT INTO `word_library` VALUES (1907331092369625988, '微不足道', 'draw-idiom', '成语', '2025-07-25 15:57:45', '2025-07-25 15:57:45');
INSERT INTO `word_library` VALUES (1907331092369625989, '未雨绸缪', 'draw-idiom', '成语', '2025-07-25 15:57:45', '2025-07-25 15:57:45');
INSERT INTO `word_library` VALUES (1907331092369625990, '文不加点', 'draw-idiom', '成语', '2025-07-25 15:57:45', '2025-07-25 15:57:45');
INSERT INTO `word_library` VALUES (1907331092369625991, '文过饰非', 'draw-idiom', '成语', '2025-07-25 15:57:45', '2025-07-25 15:57:45');
INSERT INTO `word_library` VALUES (1907331092369625992, '无独有偶', 'draw-idiom', '成语', '2025-07-25 15:57:45', '2025-07-25 15:57:45');
INSERT INTO `word_library` VALUES (1907331092369625993, '无可厚非', 'draw-idiom', '成语', '2025-07-25 15:57:45', '2025-07-25 15:57:45');
INSERT INTO `word_library` VALUES (1907331092369625994, '无可奈何', 'draw-idiom', '成语', '2025-07-25 15:57:45', '2025-07-25 15:57:45');
INSERT INTO `word_library` VALUES (1907331092369625995, '无懈可击', 'draw-idiom', '成语', '2025-07-25 15:57:45', '2025-07-25 15:57:45');
INSERT INTO `word_library` VALUES (1907331092369625996, '无中生有', 'draw-idiom', '成语', '2025-07-25 15:57:45', '2025-07-25 15:57:45');
INSERT INTO `word_library` VALUES (1907331092369625997, '息息相关', 'draw-idiom', '成语', '2025-07-25 15:57:45', '2025-07-25 15:57:45');
INSERT INTO `word_library` VALUES (1907331092369625998, '惜字如金', 'draw-idiom', '成语', '2025-07-25 15:57:45', '2025-07-25 15:57:45');
INSERT INTO `word_library` VALUES (1907331092369625999, '下不为例', 'draw-idiom', '成语', '2025-07-25 15:57:45', '2025-07-25 15:57:45');
INSERT INTO `word_library` VALUES (1907331092369626000, '先发制人', 'draw-idiom', '成语', '2025-07-25 15:57:45', '2025-07-25 15:57:45');
INSERT INTO `word_library` VALUES (1907331092369626001, '先入为主', 'draw-idiom', '成语', '2025-07-25 15:57:45', '2025-07-25 15:57:45');
INSERT INTO `word_library` VALUES (1907331092369626002, '相敬如宾', 'draw-idiom', '成语', '2025-07-25 15:57:45', '2025-07-25 15:57:45');
INSERT INTO `word_library` VALUES (1907331092369626003, '相提并论', 'draw-idiom', '成语', '2025-07-25 15:57:45', '2025-07-25 15:57:45');
INSERT INTO `word_library` VALUES (1907331092369626004, '心猿意马', 'draw-idiom', '成语', '2025-07-25 15:57:45', '2025-07-25 15:57:45');
INSERT INTO `word_library` VALUES (1907331092369626005, '兴致勃勃', 'draw-idiom', '成语', '2025-07-25 15:57:45', '2025-07-25 15:57:45');
INSERT INTO `word_library` VALUES (1907331092369626006, '兴高采烈', 'draw-idiom', '成语', '2025-07-25 15:57:45', '2025-07-25 15:57:45');
INSERT INTO `word_library` VALUES (1907331092369626007, '虚怀若谷', 'draw-idiom', '成语', '2025-07-25 15:57:45', '2025-07-25 15:57:45');
INSERT INTO `word_library` VALUES (1907331092369626008, '悬崖勒马', 'draw-idiom', '成语', '2025-07-25 15:57:45', '2025-07-25 15:57:45');
INSERT INTO `word_library` VALUES (1907331092369626009, '言简意赅', 'draw-idiom', '成语', '2025-07-25 15:57:45', '2025-07-25 15:57:45');
INSERT INTO `word_library` VALUES (1907331092369626010, '言无不尽', 'draw-idiom', '成语', '2025-07-25 15:57:45', '2025-07-25 15:57:45');
INSERT INTO `word_library` VALUES (1907331092369626011, '一触即发', 'draw-idiom', '成语', '2025-07-25 15:57:45', '2025-07-25 15:57:45');
INSERT INTO `word_library` VALUES (1907331092369626012, '一蹴而就', 'draw-idiom', '成语', '2025-07-25 15:57:45', '2025-07-25 15:57:45');
INSERT INTO `word_library` VALUES (1907331092369626013, '一帆风顺', 'draw-idiom', '成语', '2025-07-25 15:57:45', '2025-07-25 15:57:45');
INSERT INTO `word_library` VALUES (1907331092369626014, '一丝不苟', 'draw-idiom', '成语', '2025-07-25 15:57:45', '2025-07-25 15:57:45');
INSERT INTO `word_library` VALUES (1907331092369626015, '一网打尽', 'draw-idiom', '成语', '2025-07-25 15:57:45', '2025-07-25 15:57:45');
INSERT INTO `word_library` VALUES (1907331092369626016, '一言九鼎', 'draw-idiom', '成语', '2025-07-25 16:06:21', '2025-07-25 16:06:21');
INSERT INTO `word_library` VALUES (1907331092369626017, '一语道破', 'draw-idiom', '成语', '2025-07-25 16:06:21', '2025-07-25 16:06:21');
INSERT INTO `word_library` VALUES (1907331092369626018, '一知半解', 'draw-idiom', '成语', '2025-07-25 16:06:21', '2025-07-25 16:06:21');
INSERT INTO `word_library` VALUES (1907331092369626019, '义无反顾', 'draw-idiom', '成语', '2025-07-25 16:06:21', '2025-07-25 16:06:21');
INSERT INTO `word_library` VALUES (1907331092369626020, '引人入胜', 'draw-idiom', '成语', '2025-07-25 16:06:21', '2025-07-25 16:06:21');
INSERT INTO `word_library` VALUES (1907331092369626021, '饮鸩止渴', 'draw-idiom', '成语', '2025-07-25 16:06:21', '2025-07-25 16:06:21');
INSERT INTO `word_library` VALUES (1907331092369626022, '迎刃而解', 'draw-idiom', '成语', '2025-07-25 16:06:21', '2025-07-25 16:06:21');
INSERT INTO `word_library` VALUES (1907331092369626023, '鹰击长空', 'draw-idiom', '成语', '2025-07-25 16:06:21', '2025-07-25 16:06:21');
INSERT INTO `word_library` VALUES (1907331092369626024, '优柔寡断', 'draw-idiom', '成语', '2025-07-25 16:06:21', '2025-07-25 16:06:21');
INSERT INTO `word_library` VALUES (1907331092369626025, '与日俱增', 'draw-idiom', '成语', '2025-07-25 16:06:21', '2025-07-25 16:06:21');
INSERT INTO `word_library` VALUES (1907331092369626026, '玉石俱焚', 'draw-idiom', '成语', '2025-07-25 16:06:21', '2025-07-25 16:06:21');
INSERT INTO `word_library` VALUES (1907331092369626027, '缘木求鱼', 'draw-idiom', '成语', '2025-07-25 16:06:21', '2025-07-25 16:06:21');
INSERT INTO `word_library` VALUES (1907331092369626028, '远见卓识', 'draw-idiom', '成语', '2025-07-25 16:06:21', '2025-07-25 16:06:21');
INSERT INTO `word_library` VALUES (1907331092369626029, '云开见日', 'draw-idiom', '成语', '2025-07-25 16:06:21', '2025-07-25 16:06:21');
INSERT INTO `word_library` VALUES (1907331092369626030, '云泥之别', 'draw-idiom', '成语', '2025-07-25 16:06:21', '2025-07-25 16:06:21');
INSERT INTO `word_library` VALUES (1907331092369626031, '云消雾散', 'draw-idiom', '成语', '2025-07-25 16:06:21', '2025-07-25 16:06:21');
INSERT INTO `word_library` VALUES (1907331092369626032, '运筹帷幄', 'draw-idiom', '成语', '2025-07-25 16:06:21', '2025-07-25 16:06:21');
INSERT INTO `word_library` VALUES (1907331092369626033, '在劫难逃', 'draw-idiom', '成语', '2025-07-25 16:06:21', '2025-07-25 16:06:21');
INSERT INTO `word_library` VALUES (1907331092369626034, '张冠李戴', 'draw-idiom', '成语', '2025-07-25 16:06:21', '2025-07-25 16:06:21');
INSERT INTO `word_library` VALUES (1907331092369626035, '张灯结彩', 'draw-idiom', '成语', '2025-07-25 16:06:21', '2025-07-25 16:06:21');
INSERT INTO `word_library` VALUES (1907331092369626036, '众目睽睽', 'draw-idiom', '成语', '2025-07-25 16:06:21', '2025-07-25 16:06:21');
INSERT INTO `word_library` VALUES (1907331092369626037, '众志成城', 'draw-idiom', '成语', '2025-07-25 16:06:21', '2025-07-25 16:06:21');
INSERT INTO `word_library` VALUES (1907331092369626038, '转危为安', 'draw-idiom', '成语', '2025-07-25 16:06:21', '2025-07-25 16:06:21');
INSERT INTO `word_library` VALUES (1907331092369626039, '自强不息', 'draw-idiom', '成语', '2025-07-25 16:06:21', '2025-07-25 16:06:21');
INSERT INTO `word_library` VALUES (1907331092369626040, '自圆其说', 'draw-idiom', '成语', '2025-07-25 16:06:21', '2025-07-25 16:06:21');
INSERT INTO `word_library` VALUES (1907331092369626041, '走投无路', 'draw-idiom', '成语', '2025-07-25 16:06:21', '2025-07-25 16:06:21');
INSERT INTO `word_library` VALUES (1907331092369626042, '醉生梦死', 'draw-idiom', '成语', '2025-07-25 16:06:21', '2025-07-25 16:06:21');
INSERT INTO `word_library` VALUES (1907331092369626043, '坐吃山空', 'draw-idiom', '成语', '2025-07-25 16:06:21', '2025-07-25 16:06:21');
INSERT INTO `word_library` VALUES (1907331092369626044, '一木难支', 'draw-idiom', '成语', '2025-07-25 16:06:21', '2025-07-25 16:06:21');
INSERT INTO `word_library` VALUES (1907331092369626046, '一见如故', 'draw-idiom', '成语', '2025-07-25 16:06:21', '2025-07-25 16:06:21');
INSERT INTO `word_library` VALUES (1907331092369626047, '一毛不拔', 'draw-idiom', '成语', '2025-07-25 16:06:21', '2025-07-25 16:06:21');
INSERT INTO `word_library` VALUES (1907331092369626048, '一以当十', 'draw-idiom', '成语', '2025-07-25 16:06:21', '2025-07-25 16:06:21');
INSERT INTO `word_library` VALUES (1907331092369626049, '一龙一猪', 'draw-idiom', '成语', '2025-07-25 16:06:21', '2025-07-25 16:06:21');
INSERT INTO `word_library` VALUES (1907331092369626051, '一丘之貉', 'draw-idiom', '成语', '2025-07-25 16:06:21', '2025-07-25 16:06:21');
INSERT INTO `word_library` VALUES (1907331092369626052, '一发千钧', 'draw-idiom', '成语', '2025-07-25 16:06:21', '2025-07-25 16:06:21');
INSERT INTO `word_library` VALUES (1907331092369626054, '一死一生', 'draw-idiom', '成语', '2025-07-25 16:06:21', '2025-07-25 16:06:21');
INSERT INTO `word_library` VALUES (1907331092369626056, '一衣带水', 'draw-idiom', '成语', '2025-07-25 16:06:21', '2025-07-25 16:06:21');
INSERT INTO `word_library` VALUES (1907331092369626057, '一字千金', 'draw-idiom', '成语', '2025-07-25 16:06:21', '2025-07-25 16:06:21');
INSERT INTO `word_library` VALUES (1907331092369626058, '一字之师', 'draw-idiom', '成语', '2025-07-25 16:06:21', '2025-07-25 16:06:21');
INSERT INTO `word_library` VALUES (1907331092369626059, '一抔黄土', 'draw-idiom', '成语', '2025-07-25 16:06:21', '2025-07-25 16:06:21');
INSERT INTO `word_library` VALUES (1907331092369626060, '一时之秀', 'draw-idiom', '成语', '2025-07-25 16:06:21', '2025-07-25 16:06:21');
INSERT INTO `word_library` VALUES (1907331092369626061, '一身是胆', 'draw-idiom', '成语', '2025-07-25 16:06:21', '2025-07-25 16:06:21');
INSERT INTO `word_library` VALUES (1907331092369626062, '一身两任', 'draw-idiom', '成语', '2025-07-25 16:06:21', '2025-07-25 16:06:21');
INSERT INTO `word_library` VALUES (1907331092369626063, '一饭千金', 'draw-idiom', '成语', '2025-07-25 16:06:21', '2025-07-25 16:06:21');
INSERT INTO `word_library` VALUES (1907331092369626064, '一国三公', 'draw-idiom', '成语', '2025-07-25 16:06:21', '2025-07-25 16:06:21');
INSERT INTO `word_library` VALUES (1907331092369626066, '一败涂地', 'draw-idiom', '成语', '2025-07-25 16:06:21', '2025-07-25 16:06:21');
INSERT INTO `word_library` VALUES (1907331092369626067, '一往情深', 'draw-idiom', '成语', '2025-07-25 16:06:21', '2025-07-25 16:06:21');
INSERT INTO `word_library` VALUES (1907331092369626068, '一挥而就', 'draw-idiom', '成语', '2025-07-25 16:06:21', '2025-07-25 16:06:21');
INSERT INTO `word_library` VALUES (1907331092369626069, '一举两得', 'draw-idiom', '成语', '2025-07-25 16:06:21', '2025-07-25 16:06:21');
INSERT INTO `word_library` VALUES (1907331092369626070, '一误再误', 'draw-idiom', '成语', '2025-07-25 16:06:21', '2025-07-25 16:06:21');
INSERT INTO `word_library` VALUES (1907331092369626071, '一钱不值', 'draw-idiom', '成语', '2025-07-25 16:06:21', '2025-07-25 16:06:21');
INSERT INTO `word_library` VALUES (1907331092369626072, '一笔勾销', 'draw-idiom', '成语', '2025-07-25 16:06:21', '2025-07-25 16:06:21');
INSERT INTO `word_library` VALUES (1907331092369626073, '一窍不通', 'draw-idiom', '成语', '2025-07-25 16:06:21', '2025-07-25 16:06:21');
INSERT INTO `word_library` VALUES (1907331092369626074, '一诺千金', 'draw-idiom', '成语', '2025-07-25 16:06:21', '2025-07-25 16:06:21');
INSERT INTO `word_library` VALUES (1907331092369626075, '一厢情愿', 'draw-idiom', '成语', '2025-07-25 16:06:21', '2025-07-25 16:06:21');
INSERT INTO `word_library` VALUES (1907331092369626076, '一琴一鹤', 'draw-idiom', '成语', '2025-07-25 16:06:21', '2025-07-25 16:06:21');
INSERT INTO `word_library` VALUES (1907331092369626077, '一朝一夕', 'draw-idiom', '成语', '2025-07-25 16:06:21', '2025-07-25 16:06:21');
INSERT INTO `word_library` VALUES (1907331092369626078, '一傅众咻', 'draw-idiom', '成语', '2025-07-25 16:06:21', '2025-07-25 16:06:21');
INSERT INTO `word_library` VALUES (1907331092369626079, '一寒如此', 'draw-idiom', '成语', '2025-07-25 16:06:21', '2025-07-25 16:06:21');
INSERT INTO `word_library` VALUES (1907331092369626080, '一鼓作气', 'draw-idiom', '成语', '2025-07-25 16:06:21', '2025-07-25 16:06:21');
INSERT INTO `word_library` VALUES (1907331092369626081, '一筹莫展', 'draw-idiom', '成语', '2025-07-25 16:06:21', '2025-07-25 16:06:21');
INSERT INTO `word_library` VALUES (1907331092369626082, '一意孤行', 'draw-idiom', '成语', '2025-07-25 16:06:21', '2025-07-25 16:06:21');
INSERT INTO `word_library` VALUES (1907331092369626083, '一箭双雕', 'draw-idiom', '成语', '2025-07-25 16:06:21', '2025-07-25 16:06:21');
INSERT INTO `word_library` VALUES (1907331092369626084, '一夔已足', 'draw-idiom', '成语', '2025-07-25 16:06:21', '2025-07-25 16:06:21');
INSERT INTO `word_library` VALUES (1907331092369626085, '一去不复返', 'draw-idiom', '成语', '2025-07-25 16:06:21', '2025-07-25 16:06:21');
INSERT INTO `word_library` VALUES (1907331092369626086, '一问三不知', 'draw-idiom', '成语', '2025-07-25 16:06:21', '2025-07-25 16:06:21');
INSERT INTO `word_library` VALUES (1907331092369626087, '二桃杀三士', 'draw-idiom', '成语', '2025-07-25 16:06:21', '2025-07-25 16:06:21');
INSERT INTO `word_library` VALUES (1907331092369626088, '七步之才', 'draw-idiom', '成语', '2025-07-25 16:06:21', '2025-07-25 16:06:21');
INSERT INTO `word_library` VALUES (1907331092369626089, '七擒七纵', 'draw-idiom', '成语', '2025-07-25 16:06:21', '2025-07-25 16:06:21');
INSERT INTO `word_library` VALUES (1907331092369626090, '十二金牌', 'draw-idiom', '成语', '2025-07-25 16:06:21', '2025-07-25 16:06:21');
INSERT INTO `word_library` VALUES (1907331092369626091, '十行俱下', 'draw-idiom', '成语', '2025-07-25 16:06:21', '2025-07-25 16:06:21');
INSERT INTO `word_library` VALUES (1907331092369626092, '十羊九牧', 'draw-idiom', '成语', '2025-07-25 16:06:21', '2025-07-25 16:06:21');
INSERT INTO `word_library` VALUES (1907331092369626093, '十鼠同穴', 'draw-idiom', '成语', '2025-07-25 16:06:21', '2025-07-25 16:06:21');
INSERT INTO `word_library` VALUES (1907331092369626096, '入幕之宾', 'draw-idiom', '成语', '2025-07-25 16:06:21', '2025-07-25 16:06:21');
INSERT INTO `word_library` VALUES (1907331092369626098, '人人自危', 'draw-idiom', '成语', '2025-07-25 16:06:21', '2025-07-25 16:06:21');
INSERT INTO `word_library` VALUES (1907331092369626099, '人中之龙', 'draw-idiom', '成语', '2025-07-25 16:06:21', '2025-07-25 16:06:21');
INSERT INTO `word_library` VALUES (1907331092369626100, '人心如面', 'draw-idiom', '成语', '2025-07-25 16:06:21', '2025-07-25 16:06:21');
INSERT INTO `word_library` VALUES (1907331092369626101, '人自为战', 'draw-idiom', '成语', '2025-07-25 16:06:21', '2025-07-25 16:06:21');
INSERT INTO `word_library` VALUES (1907331092369626102, '人言可畏', 'draw-idiom', '成语', '2025-07-25 16:06:21', '2025-07-25 16:06:21');
INSERT INTO `word_library` VALUES (1907331092369626103, '人弃我取', 'draw-idiom', '成语', '2025-07-25 16:06:21', '2025-07-25 16:06:21');
INSERT INTO `word_library` VALUES (1907331092369626104, '人杰地灵', 'draw-idiom', '成语', '2025-07-25 16:06:21', '2025-07-25 16:06:21');
INSERT INTO `word_library` VALUES (1907331092369626105, '人面桃花', 'draw-idiom', '成语', '2025-07-25 16:06:21', '2025-07-25 16:06:21');
INSERT INTO `word_library` VALUES (1907331092369626106, '人给家足', 'draw-idiom', '成语', '2025-07-25 16:06:21', '2025-07-25 16:06:21');
INSERT INTO `word_library` VALUES (1907331092369626107, '人琴俱亡', 'draw-idiom', '成语', '2025-07-25 16:06:21', '2025-07-25 16:06:21');
INSERT INTO `word_library` VALUES (1907331092369626108, '人非圣贤，孰能无过', 'draw-idiom', '成语', '2025-07-25 16:06:21', '2025-07-25 16:06:21');
INSERT INTO `word_library` VALUES (1907331092369626109, '卜昼卜夜', 'draw-idiom', '成语', '2025-07-25 16:06:21', '2025-07-25 16:06:21');
INSERT INTO `word_library` VALUES (1907331092369626110, '土崩瓦解', 'draw-idiom', '成语', '2025-07-25 16:06:21', '2025-07-25 16:06:21');
INSERT INTO `word_library` VALUES (1907331092369626111, '下车泣罪', 'draw-idiom', '成语', '2025-07-25 16:06:21', '2025-07-25 16:06:21');
INSERT INTO `word_library` VALUES (1907331092369626112, '下笔成章', 'draw-idiom', '成语', '2025-07-25 16:06:21', '2025-07-25 16:06:21');
INSERT INTO `word_library` VALUES (1907331092369626113, '才气无双', 'draw-idiom', '成语', '2025-07-25 16:06:21', '2025-07-25 16:06:21');
INSERT INTO `word_library` VALUES (1907331092369626114, '才高八斗', 'draw-idiom', '成语', '2025-07-25 16:06:21', '2025-07-25 16:06:21');
INSERT INTO `word_library` VALUES (1907331092369626115, '才疏意广', 'draw-idiom', '成语', '2025-07-25 16:06:21', '2025-07-25 16:06:21');
INSERT INTO `word_library` VALUES (1907331092369626116, '与虎谋皮', 'draw-idiom', '成语', '2025-07-25 16:06:21', '2025-07-25 16:06:21');
INSERT INTO `word_library` VALUES (1907331092369626117, '万人之敌', 'draw-idiom', '成语', '2025-07-25 16:06:21', '2025-07-25 16:06:21');
INSERT INTO `word_library` VALUES (1907331092369626118, '万马齐喑', 'draw-idiom', '成语', '2025-07-25 16:06:21', '2025-07-25 16:06:21');
INSERT INTO `word_library` VALUES (1907331092369626119, '万死不辞', 'draw-idiom', '成语', '2025-07-25 16:06:21', '2025-07-25 16:06:21');
INSERT INTO `word_library` VALUES (1907331092369626120, '万全之策', 'draw-idiom', '成语', '2025-07-25 16:06:21', '2025-07-25 16:06:21');
INSERT INTO `word_library` VALUES (1907331092369626121, '万事俱备，只欠东风', 'draw-idiom', '成语', '2025-07-25 16:06:21', '2025-07-25 16:06:21');
INSERT INTO `word_library` VALUES (1907331092369626122, '三人成虎', 'draw-idiom', '成语', '2025-07-25 16:06:21', '2025-07-25 16:06:21');
INSERT INTO `word_library` VALUES (1907331092369626123, '三寸之舌', 'draw-idiom', '成语', '2025-07-25 16:06:21', '2025-07-25 16:06:21');
INSERT INTO `word_library` VALUES (1907331092369626124, '三千珠履', 'draw-idiom', '成语', '2025-07-25 16:06:21', '2025-07-25 16:06:21');
INSERT INTO `word_library` VALUES (1907331092369626125, '三分鼎足', 'draw-idiom', '成语', '2025-07-25 16:06:21', '2025-07-25 16:06:21');
INSERT INTO `word_library` VALUES (1907331092369626126, '三户亡秦', 'draw-idiom', '成语', '2025-07-25 16:06:21', '2025-07-25 16:06:21');
INSERT INTO `word_library` VALUES (1907331092369626127, '三迁之教', 'draw-idiom', '成语', '2025-07-25 16:06:21', '2025-07-25 16:06:21');
INSERT INTO `word_library` VALUES (1907331092369626128, '三豕涉河', 'draw-idiom', '成语', '2025-07-25 16:06:21', '2025-07-25 16:06:21');
INSERT INTO `word_library` VALUES (1907331092369626129, '三纸无驴', 'draw-idiom', '成语', '2025-07-25 16:06:21', '2025-07-25 16:06:21');
INSERT INTO `word_library` VALUES (1907331092369626131, '三衅三沐', 'draw-idiom', '成语', '2025-07-25 16:06:21', '2025-07-25 16:06:21');
INSERT INTO `word_library` VALUES (1907331092369626132, '三过其门而不入', 'draw-idiom', '成语', '2025-07-25 16:06:21', '2025-07-25 16:06:21');
INSERT INTO `word_library` VALUES (1907331092369626133, '大义灭亲', 'draw-idiom', '成语', '2025-07-25 16:06:21', '2025-07-25 16:06:21');
INSERT INTO `word_library` VALUES (1907331092369626134, '大公无私', 'draw-idiom', '成语', '2025-07-25 16:06:21', '2025-07-25 16:06:21');
INSERT INTO `word_library` VALUES (1907331092369626135, '大功毕成', 'draw-idiom', '成语', '2025-07-25 16:06:21', '2025-07-25 16:06:21');
INSERT INTO `word_library` VALUES (1907331092369626136, '大失所望', 'draw-idiom', '成语', '2025-07-25 16:06:21', '2025-07-25 16:06:21');
INSERT INTO `word_library` VALUES (1907331092369626137, '大材小用', 'draw-idiom', '成语', '2025-07-25 16:06:21', '2025-07-25 16:06:21');
INSERT INTO `word_library` VALUES (1907331092369626138, '大逆不道', 'draw-idiom', '成语', '2025-07-25 16:06:21', '2025-07-25 16:06:21');
INSERT INTO `word_library` VALUES (1907331092369626139, '大笔如椽', 'draw-idiom', '成语', '2025-07-25 16:06:21', '2025-07-25 16:06:21');
INSERT INTO `word_library` VALUES (1907331092369626140, '大腹便便', 'draw-idiom', '成语', '2025-07-25 16:06:21', '2025-07-25 16:06:21');
INSERT INTO `word_library` VALUES (1907331092369626141, '大谬不然', 'draw-idiom', '成语', '2025-07-25 16:06:21', '2025-07-25 16:06:21');
INSERT INTO `word_library` VALUES (1907331092369626143, '口血未干', 'draw-idiom', '成语', '2025-07-25 16:06:21', '2025-07-25 16:06:21');
INSERT INTO `word_library` VALUES (1907331092369626145, '口尚乳臭', 'draw-idiom', '成语', '2025-07-25 16:06:21', '2025-07-25 16:06:21');
INSERT INTO `word_library` VALUES (1907331092369626146, '口无择言', 'draw-idiom', '成语', '2025-07-25 16:06:21', '2025-07-25 16:06:21');
INSERT INTO `word_library` VALUES (1907331092369626147, '口蜜腹剑', 'draw-idiom', '成语', '2025-07-25 16:06:21', '2025-07-25 16:06:21');
INSERT INTO `word_library` VALUES (1907331092369626148, '山鸡舞镜', 'draw-idiom', '成语', '2025-07-25 16:06:21', '2025-07-25 16:06:21');
INSERT INTO `word_library` VALUES (1907331092369626150, '及瓜而代', 'draw-idiom', '成语', '2025-07-25 16:06:21', '2025-07-25 16:06:21');
INSERT INTO `word_library` VALUES (1907331092369626151, '千万买邻', 'draw-idiom', '成语', '2025-07-25 16:06:21', '2025-07-25 16:06:21');
INSERT INTO `word_library` VALUES (1907331092369626152, '千夫所指', 'draw-idiom', '成语', '2025-07-25 16:06:21', '2025-07-25 16:06:21');
INSERT INTO `word_library` VALUES (1907331092369626153, '千金市骨', 'draw-idiom', '成语', '2025-07-25 16:06:21', '2025-07-25 16:06:21');
INSERT INTO `word_library` VALUES (1907331092369626154, '千载难逢', 'draw-idiom', '成语', '2025-07-25 16:06:21', '2025-07-25 16:06:21');
INSERT INTO `word_library` VALUES (1907331092369626155, '千虑一得', 'draw-idiom', '成语', '2025-07-25 16:06:21', '2025-07-25 16:06:21');
INSERT INTO `word_library` VALUES (1907331092369626156, '之乎者也', 'draw-idiom', '成语', '2025-07-25 16:06:21', '2025-07-25 16:06:21');
INSERT INTO `word_library` VALUES (1907331092369626158, '亡命之徒', 'draw-idiom', '成语', '2025-07-25 16:06:21', '2025-07-25 16:06:21');
INSERT INTO `word_library` VALUES (1907331092369626159, '亡戟得矛', 'draw-idiom', '成语', '2025-07-25 16:06:21', '2025-07-25 16:06:21');
INSERT INTO `word_library` VALUES (1907331092369626160, '门无杂宾', 'draw-idiom', '成语', '2025-07-25 16:06:21', '2025-07-25 16:06:21');
INSERT INTO `word_library` VALUES (1907331092369626161, '门可罗雀', 'draw-idiom', '成语', '2025-07-25 16:06:21', '2025-07-25 16:06:21');
INSERT INTO `word_library` VALUES (1907331092369626162, '门庭若市', 'draw-idiom', '成语', '2025-07-25 16:06:21', '2025-07-25 16:06:21');
INSERT INTO `word_library` VALUES (1907331092369626163, '尸位素餐', 'draw-idiom', '成语', '2025-07-25 16:06:21', '2025-07-25 16:06:21');
INSERT INTO `word_library` VALUES (1907331092369626164, '尸居余气', 'draw-idiom', '成语', '2025-07-25 16:06:21', '2025-07-25 16:06:21');
INSERT INTO `word_library` VALUES (1907331092369626165, '女中尧舜', 'draw-idiom', '成语', '2025-07-25 16:06:21', '2025-07-25 16:06:21');
INSERT INTO `word_library` VALUES (1907331092369626166, '马齿徒增', 'draw-idiom', '成语', '2025-07-25 16:06:21', '2025-07-25 16:06:21');
INSERT INTO `word_library` VALUES (1907331092369626167, '马革裹尸', 'draw-idiom', '成语', '2025-07-25 16:06:21', '2025-07-25 16:06:21');
INSERT INTO `word_library` VALUES (1907331092369626168, '马首是瞻', 'draw-idiom', '成语', '2025-07-25 16:06:21', '2025-07-25 16:06:21');
INSERT INTO `word_library` VALUES (1907331092369626169, '小心翼翼', 'draw-idiom', '成语', '2025-07-25 16:06:21', '2025-07-25 16:06:21');
INSERT INTO `word_library` VALUES (1907331092369626170, '小丑跳梁', 'draw-idiom', '成语', '2025-07-25 16:06:21', '2025-07-25 16:06:21');
INSERT INTO `word_library` VALUES (1907331092369626171, '小时了了', 'draw-idiom', '成语', '2025-07-25 16:06:21', '2025-07-25 16:06:21');
INSERT INTO `word_library` VALUES (1907331092369626172, '小巫见大巫', 'draw-idiom', '成语', '2025-07-25 16:06:21', '2025-07-25 16:06:21');
INSERT INTO `word_library` VALUES (1907331092369626173, '飞扬跋扈', 'draw-idiom', '成语', '2025-07-25 16:06:21', '2025-07-25 16:06:21');
INSERT INTO `word_library` VALUES (1907331092369626174, '飞沙转石', 'draw-idiom', '成语', '2025-07-25 16:06:21', '2025-07-25 16:06:21');
INSERT INTO `word_library` VALUES (1907331092369626175, '飞蛾扑火', 'draw-idiom', '成语', '2025-07-25 16:06:21', '2025-07-25 16:06:21');
INSERT INTO `word_library` VALUES (1907331092369626176, '飞熊入梦', 'draw-idiom', '成语', '2025-07-25 16:06:21', '2025-07-25 16:06:21');
INSERT INTO `word_library` VALUES (1907331092369626177, '飞鹰走狗', 'draw-idiom', '成语', '2025-07-25 16:06:21', '2025-07-25 16:06:21');
INSERT INTO `word_library` VALUES (1907331092369626178, '韦编三绝', 'draw-idiom', '成语', '2025-07-25 16:06:21', '2025-07-25 16:06:21');
INSERT INTO `word_library` VALUES (1907331092369626179, '匹夫之勇', 'draw-idiom', '成语', '2025-07-25 16:06:21', '2025-07-25 16:06:21');
INSERT INTO `word_library` VALUES (1907331092369626180, '专心致志', 'draw-idiom', '成语', '2025-07-25 16:06:21', '2025-07-25 16:06:21');
INSERT INTO `word_library` VALUES (1907331092369626181, '专横跋扈', 'draw-idiom', '成语', '2025-07-25 16:06:21', '2025-07-25 16:06:21');
INSERT INTO `word_library` VALUES (1907331092369626182, '王祥卧冰', 'draw-idiom', '成语', '2025-07-25 16:06:21', '2025-07-25 16:06:21');
INSERT INTO `word_library` VALUES (1907331092369626183, '瓦器蚌盘', 'draw-idiom', '成语', '2025-07-25 16:06:21', '2025-07-25 16:06:21');
INSERT INTO `word_library` VALUES (1907331092369626184, '井臼亲操', 'draw-idiom', '成语', '2025-07-25 16:06:21', '2025-07-25 16:06:21');
INSERT INTO `word_library` VALUES (1907331092369626186, '木人石心', 'draw-idiom', '成语', '2025-07-25 16:06:21', '2025-07-25 16:06:21');
INSERT INTO `word_library` VALUES (1907331092369626187, '木鸡养到', 'draw-idiom', '成语', '2025-07-25 16:06:21', '2025-07-25 16:06:21');
INSERT INTO `word_library` VALUES (1907331092369626188, '太丘道广', 'draw-idiom', '成语', '2025-07-25 16:06:21', '2025-07-25 16:06:21');
INSERT INTO `word_library` VALUES (1907331092369626189, '太公钓鱼', 'draw-idiom', '成语', '2025-07-25 16:06:21', '2025-07-25 16:06:21');
INSERT INTO `word_library` VALUES (1907331092369626190, '车水马龙', 'draw-idiom', '成语', '2025-07-25 16:06:21', '2025-07-25 16:06:21');
INSERT INTO `word_library` VALUES (1907331092369626191, '车载斗量', 'draw-idiom', '成语', '2025-07-25 16:06:21', '2025-07-25 16:06:21');
INSERT INTO `word_library` VALUES (1907331092369626192, '车辙马迹', 'draw-idiom', '成语', '2025-07-25 16:06:21', '2025-07-25 16:06:21');
INSERT INTO `word_library` VALUES (1907331092369626193, '比肩继踵', 'draw-idiom', '成语', '2025-07-25 16:06:21', '2025-07-25 16:06:21');
INSERT INTO `word_library` VALUES (1907331092369626194, '开门揖盗', 'draw-idiom', '成语', '2025-07-25 16:06:21', '2025-07-25 16:06:21');
INSERT INTO `word_library` VALUES (1907331092369626195, '开天辟地', 'draw-idiom', '成语', '2025-07-25 16:06:21', '2025-07-25 16:06:21');
INSERT INTO `word_library` VALUES (1907331092369626196, '开卷有益', 'draw-idiom', '成语', '2025-07-25 16:06:21', '2025-07-25 16:06:21');
INSERT INTO `word_library` VALUES (1907331092369626197, '开诚布公', 'draw-idiom', '成语', '2025-07-25 16:06:21', '2025-07-25 16:06:21');
INSERT INTO `word_library` VALUES (1907331092369626198, '犬牙交错', 'draw-idiom', '成语', '2025-07-25 16:06:21', '2025-07-25 16:06:21');
INSERT INTO `word_library` VALUES (1907331092369626199, '犬兔俱毙', 'draw-idiom', '成语', '2025-07-25 16:06:21', '2025-07-25 16:06:21');
INSERT INTO `word_library` VALUES (1907331092369626200, '五马分尸', 'draw-idiom', '成语', '2025-07-25 16:06:21', '2025-07-25 16:06:21');
INSERT INTO `word_library` VALUES (1907331092369626201, '五日京兆', 'draw-idiom', '成语', '2025-07-25 16:06:21', '2025-07-25 16:06:21');
INSERT INTO `word_library` VALUES (1907331092369626202, '五世其昌', 'draw-idiom', '成语', '2025-07-25 16:06:21', '2025-07-25 16:06:21');
INSERT INTO `word_library` VALUES (1907331092369626203, '五色无主', 'draw-idiom', '成语', '2025-07-25 16:06:21', '2025-07-25 16:06:21');
INSERT INTO `word_library` VALUES (1907331092369626204, '五里雾中', 'draw-idiom', '成语', '2025-07-25 16:06:21', '2025-07-25 16:06:21');
INSERT INTO `word_library` VALUES (1907331092369626205, '五十步笑百步', 'draw-idiom', '成语', '2025-07-25 16:06:21', '2025-07-25 16:06:21');
INSERT INTO `word_library` VALUES (1907331092369626206, '天下无双', 'draw-idiom', '成语', '2025-07-25 16:06:21', '2025-07-25 16:06:21');
INSERT INTO `word_library` VALUES (1907331092369626207, '天上石麟', 'draw-idiom', '成语', '2025-07-25 16:06:21', '2025-07-25 16:06:21');
INSERT INTO `word_library` VALUES (1907331092369626208, '天之骄子', 'draw-idiom', '成语', '2025-07-25 16:06:21', '2025-07-25 16:06:21');
INSERT INTO `word_library` VALUES (1907331092369626213, '天真烂漫', 'draw-idiom', '成语', '2025-07-25 16:06:21', '2025-07-25 16:06:21');
INSERT INTO `word_library` VALUES (1907331092369626214, '天崩地裂', 'draw-idiom', '成语', '2025-07-25 16:06:21', '2025-07-25 16:06:21');
INSERT INTO `word_library` VALUES (1907331092369626215, '天壤王郎', 'draw-idiom', '成语', '2025-07-25 16:06:21', '2025-07-25 16:06:21');
INSERT INTO `word_library` VALUES (1907331092369626216, '天子无戏言', 'draw-idiom', '成语', '2025-07-25 16:06:21', '2025-07-25 16:06:21');
INSERT INTO `word_library` VALUES (1907331092369626217, '无人之境', 'draw-idiom', '成语', '2025-07-25 16:06:21', '2025-07-25 16:06:21');
INSERT INTO `word_library` VALUES (1907331092369626218, '无功受禄', 'draw-idiom', '成语', '2025-07-25 16:06:21', '2025-07-25 16:06:21');
INSERT INTO `word_library` VALUES (1907331092369626220, '无出其右', 'draw-idiom', '成语', '2025-07-25 16:06:21', '2025-07-25 16:06:21');
INSERT INTO `word_library` VALUES (1907331092369626221, '无所不容', 'draw-idiom', '成语', '2025-07-25 16:06:21', '2025-07-25 16:06:21');
INSERT INTO `word_library` VALUES (1907331092369626222, '无所畏忌', 'draw-idiom', '成语', '2025-07-25 16:06:21', '2025-07-25 16:06:21');
INSERT INTO `word_library` VALUES (1907331092369626223, '无所畏惧', 'draw-idiom', '成语', '2025-07-25 16:06:21', '2025-07-25 16:06:21');
INSERT INTO `word_library` VALUES (1907331092369626224, '无面目见江东父老', 'draw-idiom', '成语', '2025-07-25 16:06:21', '2025-07-25 16:06:21');
INSERT INTO `word_library` VALUES (1907331092369626225, '不一而足', 'draw-idiom', '成语', '2025-07-25 16:06:21', '2025-07-25 16:06:21');
INSERT INTO `word_library` VALUES (1907331092369626226, '不可多得', 'draw-idiom', '成语', '2025-07-25 16:06:21', '2025-07-25 16:06:21');
INSERT INTO `word_library` VALUES (1907331092369626227, '不可胜数', 'draw-idiom', '成语', '2025-07-25 16:06:21', '2025-07-25 16:06:21');
INSERT INTO `word_library` VALUES (1907331092369626228, '不可救药', 'draw-idiom', '成语', '2025-07-25 16:06:21', '2025-07-25 16:06:21');
INSERT INTO `word_library` VALUES (1907331092369626229, '不因人热', 'draw-idiom', '成语', '2025-07-25 16:06:21', '2025-07-25 16:06:21');
INSERT INTO `word_library` VALUES (1907331092369626230, '不自量力', 'draw-idiom', '成语', '2025-07-25 16:06:21', '2025-07-25 16:06:21');
INSERT INTO `word_library` VALUES (1907331092369626231, '不合时宜', 'draw-idiom', '成语', '2025-07-25 16:06:21', '2025-07-25 16:06:21');
INSERT INTO `word_library` VALUES (1907331092369626232, '不名一钱', 'draw-idiom', '成语', '2025-07-25 16:06:21', '2025-07-25 16:06:21');
INSERT INTO `word_library` VALUES (1907331092369626233, '不远千里', 'draw-idiom', '成语', '2025-07-25 16:06:21', '2025-07-25 16:06:21');
INSERT INTO `word_library` VALUES (1907331092369626234, '不求甚解', 'draw-idiom', '成语', '2025-07-25 16:06:21', '2025-07-25 16:06:21');
INSERT INTO `word_library` VALUES (1907331092369626235, '不求闻达', 'draw-idiom', '成语', '2025-07-25 16:06:21', '2025-07-25 16:06:21');
INSERT INTO `word_library` VALUES (1907331092369626236, '不足为意', 'draw-idiom', '成语', '2025-07-25 16:06:21', '2025-07-25 16:06:21');
INSERT INTO `word_library` VALUES (1907331092369626237, '不拘一格', 'draw-idiom', '成语', '2025-07-25 16:06:21', '2025-07-25 16:06:21');
INSERT INTO `word_library` VALUES (1907331092369626238, '不拘小节', 'draw-idiom', '成语', '2025-07-25 16:06:21', '2025-07-25 16:06:21');
INSERT INTO `word_library` VALUES (1907331092369626239, '不知所云', 'draw-idiom', '成语', '2025-07-25 16:06:21', '2025-07-25 16:06:21');
INSERT INTO `word_library` VALUES (1907331092369626240, '不念旧恶', 'draw-idiom', '成语', '2025-07-25 16:06:21', '2025-07-25 16:06:21');
INSERT INTO `word_library` VALUES (1907331092369626241, '不贪为宝', 'draw-idiom', '成语', '2025-07-25 16:06:21', '2025-07-25 16:06:21');
INSERT INTO `word_library` VALUES (1907331092369626242, '不学无术', 'draw-idiom', '成语', '2025-07-25 16:06:21', '2025-07-25 16:06:21');
INSERT INTO `word_library` VALUES (1907331092369626243, '不屈不挠', 'draw-idiom', '成语', '2025-07-25 16:06:21', '2025-07-25 16:06:21');
INSERT INTO `word_library` VALUES (1907331092369626244, '不甚了了', 'draw-idiom', '成语', '2025-07-25 16:06:21', '2025-07-25 16:06:21');
INSERT INTO `word_library` VALUES (1907331092369626245, '不食周粟', 'draw-idiom', '成语', '2025-07-25 16:06:21', '2025-07-25 16:06:21');
INSERT INTO `word_library` VALUES (1907331092369626246, '不急之务', 'draw-idiom', '成语', '2025-07-25 16:06:21', '2025-07-25 16:06:21');
INSERT INTO `word_library` VALUES (1907331092369626247, '不耻下问', 'draw-idiom', '成语', '2025-07-25 16:06:21', '2025-07-25 16:06:21');
INSERT INTO `word_library` VALUES (1907331092369626248, '不能自拔', 'draw-idiom', '成语', '2025-07-25 16:06:21', '2025-07-25 16:06:21');
INSERT INTO `word_library` VALUES (1907331092369626249, '不得人心', 'draw-idiom', '成语', '2025-07-25 16:06:21', '2025-07-25 16:06:21');
INSERT INTO `word_library` VALUES (1907331092369626250, '不得要领', 'draw-idiom', '成语', '2025-07-25 16:54:24', '2025-07-25 16:54:24');
INSERT INTO `word_library` VALUES (1907331092369626251, '不谋而同', 'draw-idiom', '成语', '2025-07-25 16:54:24', '2025-07-25 16:54:24');
INSERT INTO `word_library` VALUES (1907331092369626252, '不堪回首', 'draw-idiom', '成语', '2025-07-25 16:54:24', '2025-07-25 16:54:24');
INSERT INTO `word_library` VALUES (1907331092369626253, '不欺暗室', 'draw-idiom', '成语', '2025-07-25 16:54:24', '2025-07-25 16:54:24');
INSERT INTO `word_library` VALUES (1907331092369626254, '不遗余力', 'draw-idiom', '成语', '2025-07-25 16:54:24', '2025-07-25 16:54:24');
INSERT INTO `word_library` VALUES (1907331092369626255, '不寒而栗', 'draw-idiom', '成语', '2025-07-25 16:54:24', '2025-07-25 16:54:24');
INSERT INTO `word_library` VALUES (1907331092369626256, '不舞之鹤', 'draw-idiom', '成语', '2025-07-25 16:54:24', '2025-07-25 16:54:24');
INSERT INTO `word_library` VALUES (1907331092369626257, '不辨菽麦', 'draw-idiom', '成语', '2025-07-25 16:54:24', '2025-07-25 16:54:24');
INSERT INTO `word_library` VALUES (1907331092369626258, '不翼而飞', 'draw-idiom', '成语', '2025-07-25 16:54:24', '2025-07-25 16:54:24');
INSERT INTO `word_library` VALUES (1907331092369626259, '不为五斗米折腰', 'draw-idiom', '成语', '2025-07-25 16:54:24', '2025-07-25 16:54:24');
INSERT INTO `word_library` VALUES (1907331092369626260, '不识庐山真面目', 'draw-idiom', '成语', '2025-07-25 16:54:24', '2025-07-25 16:54:24');
INSERT INTO `word_library` VALUES (1907331092369626261, '不入虎穴，焉得虎子', 'draw-idiom', '成语', '2025-07-25 16:54:24', '2025-07-25 16:54:24');
INSERT INTO `word_library` VALUES (1907331092369626262, '中原逐鹿', 'draw-idiom', '成语', '2025-07-25 16:54:24', '2025-07-25 16:54:24');
INSERT INTO `word_library` VALUES (1907331092369626263, '中流击楫', 'draw-idiom', '成语', '2025-07-25 16:54:24', '2025-07-25 16:54:24');
INSERT INTO `word_library` VALUES (1907331092369626264, '见利忘义', 'draw-idiom', '成语', '2025-07-25 16:54:24', '2025-07-25 16:54:24');
INSERT INTO `word_library` VALUES (1907331092369626265, '见怪不怪', 'draw-idiom', '成语', '2025-07-25 16:54:24', '2025-07-25 16:54:24');
INSERT INTO `word_library` VALUES (1907331092369626266, '见猎心喜', 'draw-idiom', '成语', '2025-07-25 16:54:24', '2025-07-25 16:54:24');
INSERT INTO `word_library` VALUES (1907331092369626267, '日下无双', 'draw-idiom', '成语', '2025-07-25 16:54:24', '2025-07-25 16:54:24');
INSERT INTO `word_library` VALUES (1907331092369626268, '日不暇给', 'draw-idiom', '成语', '2025-07-25 16:54:24', '2025-07-25 16:54:24');
INSERT INTO `word_library` VALUES (1907331092369626269, '日月入怀', 'draw-idiom', '成语', '2025-07-25 16:54:24', '2025-07-25 16:54:24');
INSERT INTO `word_library` VALUES (1907331092369626270, '日行千里', 'draw-idiom', '成语', '2025-07-25 16:54:24', '2025-07-25 16:54:24');
INSERT INTO `word_library` VALUES (1907331092369626271, '日暮途穷', 'draw-idiom', '成语', '2025-07-25 16:54:24', '2025-07-25 16:54:24');
INSERT INTO `word_library` VALUES (1907331092369626272, '日近长安远', 'draw-idiom', '成语', '2025-07-25 16:54:24', '2025-07-25 16:54:24');
INSERT INTO `word_library` VALUES (1907331092369626273, '升堂入室', 'draw-idiom', '成语', '2025-07-25 16:54:24', '2025-07-25 16:54:24');
INSERT INTO `word_library` VALUES (1907331092369626274, '升堂拜母', 'draw-idiom', '成语', '2025-07-25 16:54:24', '2025-07-25 16:54:24');
INSERT INTO `word_library` VALUES (1907331092369626275, '仅以身免', 'draw-idiom', '成语', '2025-07-25 16:54:24', '2025-07-25 16:54:24');
INSERT INTO `word_library` VALUES (1907331092369626276, '今是昨非', 'draw-idiom', '成语', '2025-07-25 16:54:24', '2025-07-25 16:54:24');
INSERT INTO `word_library` VALUES (1907331092369626278, '乌合之众', 'draw-idiom', '成语', '2025-07-25 16:54:24', '2025-07-25 16:54:24');
INSERT INTO `word_library` VALUES (1907331092369626279, '乌白马角', 'draw-idiom', '成语', '2025-07-25 16:54:24', '2025-07-25 16:54:24');
INSERT INTO `word_library` VALUES (1907331092369626280, '牛刀小试', 'draw-idiom', '成语', '2025-07-25 16:54:24', '2025-07-25 16:54:24');
INSERT INTO `word_library` VALUES (1907331092369626281, '牛衣对泣', 'draw-idiom', '成语', '2025-07-25 16:54:24', '2025-07-25 16:54:24');
INSERT INTO `word_library` VALUES (1907331092369626282, '牛角挂书', 'draw-idiom', '成语', '2025-07-25 16:54:24', '2025-07-25 16:54:24');
INSERT INTO `word_library` VALUES (1907331092369626283, '牛饩退敌', 'draw-idiom', '成语', '2025-07-25 16:54:24', '2025-07-25 16:54:24');
INSERT INTO `word_library` VALUES (1907331092369626284, '牛郎织女', 'draw-idiom', '成语', '2025-07-25 16:54:24', '2025-07-25 16:54:24');
INSERT INTO `word_library` VALUES (1907331092369626285, '从容不迫', 'draw-idiom', '成语', '2025-07-25 16:54:24', '2025-07-25 16:54:24');
INSERT INTO `word_library` VALUES (1907331092369626286, '从善如流', 'draw-idiom', '成语', '2025-07-25 16:54:24', '2025-07-25 16:54:24');
INSERT INTO `word_library` VALUES (1907331092369626287, '分我杯羹', 'draw-idiom', '成语', '2025-07-25 16:54:24', '2025-07-25 16:54:24');
INSERT INTO `word_library` VALUES (1907331092369626288, '分庭抗礼', 'draw-idiom', '成语', '2025-07-25 16:54:24', '2025-07-25 16:54:24');
INSERT INTO `word_library` VALUES (1907331092369626289, '分崩离析', 'draw-idiom', '成语', '2025-07-25 16:54:24', '2025-07-25 16:54:24');
INSERT INTO `word_library` VALUES (1907331092369626290, '分道扬镳', 'draw-idiom', '成语', '2025-07-25 16:54:24', '2025-07-25 16:54:24');
INSERT INTO `word_library` VALUES (1907331092369626292, '长驱直入', 'draw-idiom', '成语', '2025-07-25 16:54:24', '2025-07-25 16:54:24');
INSERT INTO `word_library` VALUES (1907331092369626293, '长林丰草', 'draw-idiom', '成语', '2025-07-25 16:54:24', '2025-07-25 16:54:24');
INSERT INTO `word_library` VALUES (1907331092369626294, '长袖善舞', 'draw-idiom', '成语', '2025-07-25 16:54:24', '2025-07-25 16:54:24');
INSERT INTO `word_library` VALUES (1907331092369626295, '长揖不拜', 'draw-idiom', '成语', '2025-07-25 16:54:24', '2025-07-25 16:54:24');
INSERT INTO `word_library` VALUES (1907331092369626296, '月下老人', 'draw-idiom', '成语', '2025-07-25 16:54:24', '2025-07-25 16:54:24');
INSERT INTO `word_library` VALUES (1907331092369626297, '月里嫦娥', 'draw-idiom', '成语', '2025-07-25 16:54:24', '2025-07-25 16:54:24');
INSERT INTO `word_library` VALUES (1907331092369626298, '气壮山河', 'draw-idiom', '成语', '2025-07-25 16:54:24', '2025-07-25 16:54:24');
INSERT INTO `word_library` VALUES (1907331092369626299, '风声鹤唳', 'draw-idiom', '成语', '2025-07-25 16:54:24', '2025-07-25 16:54:24');
INSERT INTO `word_library` VALUES (1907331092369626300, '火中取栗', 'draw-idiom', '成语', '2025-07-25 16:54:24', '2025-07-25 16:54:24');
INSERT INTO `word_library` VALUES (1907331092369626301, '斗南一人', 'draw-idiom', '成语', '2025-07-25 16:54:24', '2025-07-25 16:54:24');
INSERT INTO `word_library` VALUES (1907331092369626302, '斗酒只鸡', 'draw-idiom', '成语', '2025-07-25 16:54:24', '2025-07-25 16:54:24');
INSERT INTO `word_library` VALUES (1907331092369626303, '斗酒学士', 'draw-idiom', '成语', '2025-07-25 16:54:24', '2025-07-25 16:54:24');
INSERT INTO `word_library` VALUES (1907331092369626304, '斗粟尺布', 'draw-idiom', '成语', '2025-07-25 16:54:24', '2025-07-25 16:54:24');
INSERT INTO `word_library` VALUES (1907331092369626305, '为民请命', 'draw-idiom', '成语', '2025-07-25 16:54:24', '2025-07-25 16:54:24');
INSERT INTO `word_library` VALUES (1907331092369626306, '为虎作伥', 'draw-idiom', '成语', '2025-07-25 16:54:24', '2025-07-25 16:54:24');
INSERT INTO `word_library` VALUES (1907331092369626307, '计无所出', 'draw-idiom', '成语', '2025-07-25 16:54:24', '2025-07-25 16:54:24');
INSERT INTO `word_library` VALUES (1907331092369626308, '计日可待', 'draw-idiom', '成语', '2025-07-25 16:54:24', '2025-07-25 16:54:24');
INSERT INTO `word_library` VALUES (1907331092369626309, '计功行赏', 'draw-idiom', '成语', '2025-07-25 16:54:24', '2025-07-25 16:54:24');
INSERT INTO `word_library` VALUES (1907331092369626310, '计将安出', 'draw-idiom', '成语', '2025-07-25 16:54:24', '2025-07-25 16:54:24');
INSERT INTO `word_library` VALUES (1907331092369626311, '六出奇计', 'draw-idiom', '成语', '2025-07-25 16:54:24', '2025-07-25 16:54:24');
INSERT INTO `word_library` VALUES (1907331092369626312, '方寸已乱', 'draw-idiom', '成语', '2025-07-25 16:54:24', '2025-07-25 16:54:24');
INSERT INTO `word_library` VALUES (1907331092369626313, '方面大耳', 'draw-idiom', '成语', '2025-07-25 16:54:24', '2025-07-25 16:54:24');
INSERT INTO `word_library` VALUES (1907331092369626314, '文君司马', 'draw-idiom', '成语', '2025-07-25 16:54:24', '2025-07-25 16:54:24');
INSERT INTO `word_library` VALUES (1907331092369626315, '心旷神怡', 'draw-idiom', '成语', '2025-07-25 16:54:24', '2025-07-25 16:54:24');
INSERT INTO `word_library` VALUES (1907331092369626316, '心怀叵测', 'draw-idiom', '成语', '2025-07-25 16:54:24', '2025-07-25 16:54:24');
INSERT INTO `word_library` VALUES (1907331092369626317, '心贯白日', 'draw-idiom', '成语', '2025-07-25 16:54:24', '2025-07-25 16:54:24');
INSERT INTO `word_library` VALUES (1907331092369626318, '心旌摇曳', 'draw-idiom', '成语', '2025-07-25 16:54:24', '2025-07-25 16:54:24');
INSERT INTO `word_library` VALUES (1907331092369626319, '心腹之患', 'draw-idiom', '成语', '2025-07-25 16:54:24', '2025-07-25 16:54:24');
INSERT INTO `word_library` VALUES (1907331092369626320, '予取予求', 'draw-idiom', '成语', '2025-07-25 16:54:24', '2025-07-25 16:54:24');
INSERT INTO `word_library` VALUES (1907331092369626321, '双管齐下', 'draw-idiom', '成语', '2025-07-25 16:54:24', '2025-07-25 16:54:24');
INSERT INTO `word_library` VALUES (1907331092369626322, '水中捞月', 'draw-idiom', '成语', '2025-07-25 16:54:24', '2025-07-25 16:54:24');
INSERT INTO `word_library` VALUES (1907331092369626323, '水火无交', 'draw-idiom', '成语', '2025-07-25 16:54:24', '2025-07-25 16:54:24');
INSERT INTO `word_library` VALUES (1907331092369626324, '水火不辞', 'draw-idiom', '成语', '2025-07-25 16:54:24', '2025-07-25 16:54:24');
INSERT INTO `word_library` VALUES (1907331092369626325, '水深火热', 'draw-idiom', '成语', '2025-07-25 16:54:24', '2025-07-25 16:54:24');
INSERT INTO `word_library` VALUES (1907331092369626327, '以人废言', 'draw-idiom', '成语', '2025-07-25 16:54:24', '2025-07-25 16:54:24');
INSERT INTO `word_library` VALUES (1907331092369626328, '以功赎罪', 'draw-idiom', '成语', '2025-07-25 16:54:24', '2025-07-25 16:54:24');
INSERT INTO `word_library` VALUES (1907331092369626329, '以古非今', 'draw-idiom', '成语', '2025-07-25 16:54:24', '2025-07-25 16:54:24');
INSERT INTO `word_library` VALUES (1907331092369626330, '以身试法', 'draw-idiom', '成语', '2025-07-25 16:54:24', '2025-07-25 16:54:24');
INSERT INTO `word_library` VALUES (1907331092369626331, '以邻为壑', 'draw-idiom', '成语', '2025-07-25 16:54:24', '2025-07-25 16:54:24');
INSERT INTO `word_library` VALUES (1907331092369626332, '以卵击石', 'draw-idiom', '成语', '2025-07-25 16:54:24', '2025-07-25 16:54:24');
INSERT INTO `word_library` VALUES (1907331092369626333, '以规为瑱', 'draw-idiom', '成语', '2025-07-25 16:54:24', '2025-07-25 16:54:24');
INSERT INTO `word_library` VALUES (1907331092369626334, '以强凌弱', 'draw-idiom', '成语', '2025-07-25 16:54:24', '2025-07-25 16:54:24');
INSERT INTO `word_library` VALUES (1907331092369626335, '以管窥天', 'draw-idiom', '成语', '2025-07-25 16:54:24', '2025-07-25 16:54:24');
INSERT INTO `word_library` VALUES (1907331092369626336, '以貌取人', 'draw-idiom', '成语', '2025-07-25 16:54:24', '2025-07-25 16:54:24');
INSERT INTO `word_library` VALUES (1907331092369626337, '灭此朝食', 'draw-idiom', '成语', '2025-07-25 16:54:24', '2025-07-25 16:54:24');
INSERT INTO `word_library` VALUES (1907331092369626338, '世外桃源', 'draw-idiom', '成语', '2025-07-25 16:54:24', '2025-07-25 16:54:24');
INSERT INTO `word_library` VALUES (1907331092369626339, '节俭力行', 'draw-idiom', '成语', '2025-07-25 16:54:24', '2025-07-25 16:54:24');
INSERT INTO `word_library` VALUES (1907331092369626340, '布衣之交', 'draw-idiom', '成语', '2025-07-25 16:54:24', '2025-07-25 16:54:24');
INSERT INTO `word_library` VALUES (1907331092369626341, '左提右挈', 'draw-idiom', '成语', '2025-07-25 16:54:24', '2025-07-25 16:54:24');
INSERT INTO `word_library` VALUES (1907331092369626342, '巧取豪夺', 'draw-idiom', '成语', '2025-07-25 16:54:24', '2025-07-25 16:54:24');
INSERT INTO `word_library` VALUES (1907331092369626343, '功亏一篑', 'draw-idiom', '成语', '2025-07-25 16:54:24', '2025-07-25 16:54:24');
INSERT INTO `word_library` VALUES (1907331092369626344, '功败垂成', 'draw-idiom', '成语', '2025-07-25 16:54:24', '2025-07-25 16:54:24');
INSERT INTO `word_library` VALUES (1907331092369626345, '平易近人', 'draw-idiom', '成语', '2025-07-25 16:54:24', '2025-07-25 16:54:24');
INSERT INTO `word_library` VALUES (1907331092369626346, '未可厚非', 'draw-idiom', '成语', '2025-07-25 16:54:24', '2025-07-25 16:54:24');
INSERT INTO `word_library` VALUES (1907331092369626348, '未能免俗', 'draw-idiom', '成语', '2025-07-25 16:54:24', '2025-07-25 16:54:24');
INSERT INTO `word_library` VALUES (1907331092369626349, '龙蛇飞动', 'draw-idiom', '成语', '2025-07-25 16:54:24', '2025-07-25 16:54:24');
INSERT INTO `word_library` VALUES (1907331092369626350, '龙骧虎步', 'draw-idiom', '成语', '2025-07-25 17:06:49', '2025-07-25 17:06:49');
INSERT INTO `word_library` VALUES (1907331092369626351, '扑朔迷离', 'draw-idiom', '成语', '2025-07-25 17:06:49', '2025-07-25 17:06:49');
INSERT INTO `word_library` VALUES (1907331092369626352, '打草惊蛇', 'draw-idiom', '成语', '2025-07-25 17:06:49', '2025-07-25 17:06:49');
INSERT INTO `word_library` VALUES (1907331092369626353, '甘棠遗爱', 'draw-idiom', '成语', '2025-07-25 17:06:49', '2025-07-25 17:06:49');
INSERT INTO `word_library` VALUES (1907331092369626354, '玉汝于成', 'draw-idiom', '成语', '2025-07-25 17:06:49', '2025-07-25 17:06:49');
INSERT INTO `word_library` VALUES (1907331092369626356, '东门黄犬', 'draw-idiom', '成语', '2025-07-25 17:06:49', '2025-07-25 17:06:49');
INSERT INTO `word_library` VALUES (1907331092369626357, '东市朝衣', 'draw-idiom', '成语', '2025-07-25 17:06:49', '2025-07-25 17:06:49');
INSERT INTO `word_library` VALUES (1907331092369626358, '东床坦腹', 'draw-idiom', '成语', '2025-07-25 17:06:49', '2025-07-25 17:06:49');
INSERT INTO `word_library` VALUES (1907331092369626359, '东食西宿', 'draw-idiom', '成语', '2025-07-25 17:06:49', '2025-07-25 17:06:49');
INSERT INTO `word_library` VALUES (1907331092369626360, '东家之女', 'draw-idiom', '成语', '2025-07-25 17:06:49', '2025-07-25 17:06:49');
INSERT INTO `word_library` VALUES (1907331092369626361, '东窗事发', 'draw-idiom', '成语', '2025-07-25 17:06:49', '2025-07-25 17:06:49');
INSERT INTO `word_library` VALUES (1907331092369626362, '东施效颦', 'draw-idiom', '成语', '2025-07-25 17:06:49', '2025-07-25 17:06:49');
INSERT INTO `word_library` VALUES (1907331092369626363, '叹为观止', 'draw-idiom', '成语', '2025-07-25 17:06:49', '2025-07-25 17:06:49');
INSERT INTO `word_library` VALUES (1907331092369626365, '申旦达夕', 'draw-idiom', '成语', '2025-07-25 17:06:49', '2025-07-25 17:06:49');
INSERT INTO `word_library` VALUES (1907331092369626366, '号令如山', 'draw-idiom', '成语', '2025-07-25 17:06:49', '2025-07-25 17:06:49');
INSERT INTO `word_library` VALUES (1907331092369626367, '归马放牛', 'draw-idiom', '成语', '2025-07-25 17:06:49', '2025-07-25 17:06:49');
INSERT INTO `word_library` VALUES (1907331092369626370, '出人头地', 'draw-idiom', '成语', '2025-07-25 17:06:49', '2025-07-25 17:06:49');
INSERT INTO `word_library` VALUES (1907331092369626371, '出尔反尔', 'draw-idiom', '成语', '2025-07-25 17:06:49', '2025-07-25 17:06:49');
INSERT INTO `word_library` VALUES (1907331092369626372, '出言不逊', 'draw-idiom', '成语', '2025-07-25 17:06:49', '2025-07-25 17:06:49');
INSERT INTO `word_library` VALUES (1907331092369626373, '出奇制胜', 'draw-idiom', '成语', '2025-07-25 17:06:49', '2025-07-25 17:06:49');
INSERT INTO `word_library` VALUES (1907331092369626374, '出类拔萃', 'draw-idiom', '成语', '2025-07-25 17:06:49', '2025-07-25 17:06:49');
INSERT INTO `word_library` VALUES (1907331092369626375, '目无余子', 'draw-idiom', '成语', '2025-07-25 17:06:49', '2025-07-25 17:06:49');
INSERT INTO `word_library` VALUES (1907331092369626376, '目不识丁', 'draw-idiom', '成语', '2025-07-25 17:06:49', '2025-07-25 17:06:49');
INSERT INTO `word_library` VALUES (1907331092369626377, '目光如炬', 'draw-idiom', '成语', '2025-07-25 17:06:49', '2025-07-25 17:06:49');
INSERT INTO `word_library` VALUES (1907331092369626378, '处之泰然', 'draw-idiom', '成语', '2025-07-25 17:06:49', '2025-07-25 17:06:49');
INSERT INTO `word_library` VALUES (1907331092369626379, '尔虞我诈', 'draw-idiom', '成语', '2025-07-25 17:06:49', '2025-07-25 17:06:49');
INSERT INTO `word_library` VALUES (1907331092369626380, '包藏祸心', 'draw-idiom', '成语', '2025-07-25 17:06:49', '2025-07-25 17:06:49');
INSERT INTO `word_library` VALUES (1907331092369626381, '鸟尽弓藏', 'draw-idiom', '成语', '2025-07-25 17:06:49', '2025-07-25 17:06:49');
INSERT INTO `word_library` VALUES (1907331092369626383, '令人发指', 'draw-idiom', '成语', '2025-07-25 17:06:49', '2025-07-25 17:06:49');
INSERT INTO `word_library` VALUES (1907331092369626384, '令行禁止', 'draw-idiom', '成语', '2025-07-25 17:06:49', '2025-07-25 17:06:49');
INSERT INTO `word_library` VALUES (1907331092369626385, '外强中干', 'draw-idiom', '成语', '2025-07-25 17:06:49', '2025-07-25 17:06:49');
INSERT INTO `word_library` VALUES (1907331092369626386, '外愚内智', 'draw-idiom', '成语', '2025-07-25 17:06:49', '2025-07-25 17:06:49');
INSERT INTO `word_library` VALUES (1907331092369626387, '饥寒交迫', 'draw-idiom', '成语', '2025-07-25 17:06:49', '2025-07-25 17:06:49');
INSERT INTO `word_library` VALUES (1907331092369626388, '用兵如神', 'draw-idiom', '成语', '2025-07-25 17:06:49', '2025-07-25 17:06:49');
INSERT INTO `word_library` VALUES (1907331092369626389, '乐不可支', 'draw-idiom', '成语', '2025-07-25 17:06:49', '2025-07-25 17:06:49');
INSERT INTO `word_library` VALUES (1907331092369626390, '乐不思蜀', 'draw-idiom', '成语', '2025-07-25 17:06:49', '2025-07-25 17:06:49');
INSERT INTO `word_library` VALUES (1907331092369626391, '乐此不疲', 'draw-idiom', '成语', '2025-07-25 17:06:49', '2025-07-25 17:06:49');
INSERT INTO `word_library` VALUES (1907331092369626392, '乐极生悲', 'draw-idiom', '成语', '2025-07-25 17:06:49', '2025-07-25 17:06:49');
INSERT INTO `word_library` VALUES (1907331092369626393, '生生世世', 'draw-idiom', '成语', '2025-07-25 17:06:49', '2025-07-25 17:06:49');
INSERT INTO `word_library` VALUES (1907331092369626394, '生吞活剥', 'draw-idiom', '成语', '2025-07-25 17:06:49', '2025-07-25 17:06:49');
INSERT INTO `word_library` VALUES (1907331092369626395, '生灵涂炭', 'draw-idiom', '成语', '2025-07-25 17:06:49', '2025-07-25 17:06:49');
INSERT INTO `word_library` VALUES (1907331092369626396, '生离死别', 'draw-idiom', '成语', '2025-07-25 17:06:49', '2025-07-25 17:06:49');
INSERT INTO `word_library` VALUES (1907331092369626397, '生聚教训', 'draw-idiom', '成语', '2025-07-25 17:06:49', '2025-07-25 17:06:49');
INSERT INTO `word_library` VALUES (1907331092369626398, '白云苍狗', 'draw-idiom', '成语', '2025-07-25 17:06:49', '2025-07-25 17:06:49');
INSERT INTO `word_library` VALUES (1907331092369626399, '白头如新', 'draw-idiom', '成语', '2025-07-25 17:06:49', '2025-07-25 17:06:49');
INSERT INTO `word_library` VALUES (1907331092369626400, '白驹过隙', 'draw-idiom', '成语', '2025-07-25 17:06:49', '2025-07-25 17:06:49');
INSERT INTO `word_library` VALUES (1907331092369626401, '白虹贯日', 'draw-idiom', '成语', '2025-07-25 17:06:49', '2025-07-25 17:06:49');
INSERT INTO `word_library` VALUES (1907331092369626402, '必争之地', 'draw-idiom', '成语', '2025-07-25 17:06:49', '2025-07-25 17:06:49');
INSERT INTO `word_library` VALUES (1907331092369626403, '必恭必敬', 'draw-idiom', '成语', '2025-07-25 17:06:49', '2025-07-25 17:06:49');
INSERT INTO `word_library` VALUES (1907331092369626404, '市无二价', 'draw-idiom', '成语', '2025-07-25 17:06:49', '2025-07-25 17:06:49');
INSERT INTO `word_library` VALUES (1907331092369626405, '礼贤下士', 'draw-idiom', '成语', '2025-07-25 17:06:49', '2025-07-25 17:06:49');
INSERT INTO `word_library` VALUES (1907331092369626406, '礼顺人情', 'draw-idiom', '成语', '2025-07-25 17:06:49', '2025-07-25 17:06:49');
INSERT INTO `word_library` VALUES (1907331092369626407, '半部论语', 'draw-idiom', '成语', '2025-07-25 17:06:49', '2025-07-25 17:06:49');
INSERT INTO `word_library` VALUES (1907331092369626408, '立功赎罪', 'draw-idiom', '成语', '2025-07-25 17:06:49', '2025-07-25 17:06:49');
INSERT INTO `word_library` VALUES (1907331092369626409, '立地书橱', 'draw-idiom', '成语', '2025-07-25 17:06:49', '2025-07-25 17:06:49');
INSERT INTO `word_library` VALUES (1907331092369626410, '辽东白豕', 'draw-idiom', '成语', '2025-07-25 17:06:49', '2025-07-25 17:06:49');
INSERT INTO `word_library` VALUES (1907331092369626413, '对床夜雨', 'draw-idiom', '成语', '2025-07-25 17:06:49', '2025-07-25 17:06:49');
INSERT INTO `word_library` VALUES (1907331092369626414, '对症下药', 'draw-idiom', '成语', '2025-07-25 17:06:49', '2025-07-25 17:06:49');
INSERT INTO `word_library` VALUES (1907331092369626415, '发愤忘食', 'draw-idiom', '成语', '2025-07-25 17:06:49', '2025-07-25 17:06:49');
INSERT INTO `word_library` VALUES (1907331092369626416, '民不聊生', 'draw-idiom', '成语', '2025-07-25 17:06:49', '2025-07-25 17:06:49');
INSERT INTO `word_library` VALUES (1907331092369626417, '芒刺在背', 'draw-idiom', '成语', '2025-07-25 17:06:49', '2025-07-25 17:06:49');
INSERT INTO `word_library` VALUES (1907331092369626418, '朴素无华', 'draw-idiom', '成语', '2025-07-25 17:06:49', '2025-07-25 17:06:49');
INSERT INTO `word_library` VALUES (1907331092369626419, '机不可失', 'draw-idiom', '成语', '2025-07-25 17:06:49', '2025-07-25 17:06:49');
INSERT INTO `word_library` VALUES (1907331092369626420, '夸父逐日', 'draw-idiom', '成语', '2025-07-25 17:06:49', '2025-07-25 17:06:49');
INSERT INTO `word_library` VALUES (1907331092369626421, '至死不悟', 'draw-idiom', '成语', '2025-07-25 17:06:49', '2025-07-25 17:06:49');
INSERT INTO `word_library` VALUES (1907331092369626422, '过河拆桥', 'draw-idiom', '成语', '2025-07-25 17:06:49', '2025-07-25 17:06:49');
INSERT INTO `word_library` VALUES (1907331092369626423, '扫除天下', 'draw-idiom', '成语', '2025-07-25 17:06:49', '2025-07-25 17:06:49');
INSERT INTO `word_library` VALUES (1907331092369626424, '扬扬自得', 'draw-idiom', '成语', '2025-07-25 17:06:49', '2025-07-25 17:06:49');
INSERT INTO `word_library` VALUES (1907331092369626425, '扬扬得意', 'draw-idiom', '成语', '2025-07-25 17:06:49', '2025-07-25 17:06:49');
INSERT INTO `word_library` VALUES (1907331092369626427, '老生常谈', 'draw-idiom', '成语', '2025-07-25 17:06:49', '2025-07-25 17:06:49');
INSERT INTO `word_library` VALUES (1907331092369626428, '老当益壮', 'draw-idiom', '成语', '2025-07-25 17:06:49', '2025-07-25 17:06:49');
INSERT INTO `word_library` VALUES (1907331092369626429, '老妪能解', 'draw-idiom', '成语', '2025-07-25 17:06:49', '2025-07-25 17:06:49');
INSERT INTO `word_library` VALUES (1907331092369626430, '老莱娱亲', 'draw-idiom', '成语', '2025-07-25 17:06:49', '2025-07-25 17:06:49');
INSERT INTO `word_library` VALUES (1907331092369626431, '老罴当道', 'draw-idiom', '成语', '2025-07-25 17:06:49', '2025-07-25 17:06:49');
INSERT INTO `word_library` VALUES (1907331092369626432, '老鹤乘轩', 'draw-idiom', '成语', '2025-07-25 17:06:49', '2025-07-25 17:06:49');
INSERT INTO `word_library` VALUES (1907331092369626433, '死不旋踵', 'draw-idiom', '成语', '2025-07-25 17:06:49', '2025-07-25 17:06:49');
INSERT INTO `word_library` VALUES (1907331092369626434, '死不瞑目', 'draw-idiom', '成语', '2025-07-25 17:06:49', '2025-07-25 17:06:49');
INSERT INTO `word_library` VALUES (1907331092369626435, '死中求生', 'draw-idiom', '成语', '2025-07-25 17:06:49', '2025-07-25 17:06:49');
INSERT INTO `word_library` VALUES (1907331092369626436, '死有余辜', 'draw-idiom', '成语', '2025-07-25 17:06:49', '2025-07-25 17:06:49');
INSERT INTO `word_library` VALUES (1907331092369626437, '死而不朽', 'draw-idiom', '成语', '2025-07-25 17:06:49', '2025-07-25 17:06:49');
INSERT INTO `word_library` VALUES (1907331092369626439, '有名无实', 'draw-idiom', '成语', '2025-07-25 17:06:49', '2025-07-25 17:06:49');
INSERT INTO `word_library` VALUES (1907331092369626440, '有志竟成', 'draw-idiom', '成语', '2025-07-25 17:06:49', '2025-07-25 17:06:49');
INSERT INTO `word_library` VALUES (1907331092369626441, '有备无患', 'draw-idiom', '成语', '2025-07-25 17:06:49', '2025-07-25 17:06:49');
INSERT INTO `word_library` VALUES (1907331092369626442, '有恃无恐', 'draw-idiom', '成语', '2025-07-25 17:06:49', '2025-07-25 17:06:49');
INSERT INTO `word_library` VALUES (1907331092369626443, '有脚阳春', 'draw-idiom', '成语', '2025-07-25 17:06:49', '2025-07-25 17:06:49');
INSERT INTO `word_library` VALUES (1907331092369626444, '百川归海', 'draw-idiom', '成语', '2025-07-25 17:06:49', '2025-07-25 17:06:49');
INSERT INTO `word_library` VALUES (1907331092369626445, '百无聊赖', 'draw-idiom', '成语', '2025-07-25 17:06:49', '2025-07-25 17:06:49');
INSERT INTO `word_library` VALUES (1907331092369626446, '百不当一', 'draw-idiom', '成语', '2025-07-25 17:06:49', '2025-07-25 17:06:49');
INSERT INTO `word_library` VALUES (1907331092369626447, '百尺竿头', 'draw-idiom', '成语', '2025-07-25 17:06:49', '2025-07-25 17:06:49');
INSERT INTO `word_library` VALUES (1907331092369626449, '百废俱兴', 'draw-idiom', '成语', '2025-07-25 17:06:49', '2025-07-25 17:06:49');
INSERT INTO `word_library` VALUES (1907331092369626450, '百感交集', 'draw-idiom', '成语', '2025-07-25 17:06:49', '2025-07-25 17:06:49');
INSERT INTO `word_library` VALUES (1907331092369626451, '百步穿杨', 'draw-idiom', '成语', '2025-07-25 17:06:49', '2025-07-25 17:06:49');
INSERT INTO `word_library` VALUES (1907331092369626452, '百锻千炼', 'draw-idiom', '成语', '2025-07-25 17:06:49', '2025-07-25 17:06:49');
INSERT INTO `word_library` VALUES (1907331092369626453, '曳尾涂中', 'draw-idiom', '成语', '2025-07-25 17:06:49', '2025-07-25 17:06:49');
INSERT INTO `word_library` VALUES (1907331092369626454, '刚愎自用', 'draw-idiom', '成语', '2025-07-25 17:06:49', '2025-07-25 17:06:49');
INSERT INTO `word_library` VALUES (1907331092369626455, '曲尽其妙', 'draw-idiom', '成语', '2025-07-25 17:06:49', '2025-07-25 17:06:49');
INSERT INTO `word_library` VALUES (1907331092369626456, '曲突徙薪', 'draw-idiom', '成语', '2025-07-25 17:06:49', '2025-07-25 17:06:49');
INSERT INTO `word_library` VALUES (1907331092369626457, '曲高和寡', 'draw-idiom', '成语', '2025-07-25 17:06:49', '2025-07-25 17:06:49');
INSERT INTO `word_library` VALUES (1907331092369626458, '肉食者鄙', 'draw-idiom', '成语', '2025-07-25 17:06:49', '2025-07-25 17:06:49');
INSERT INTO `word_library` VALUES (1907331092369626459, '岁在龙蛇', 'draw-idiom', '成语', '2025-07-25 17:06:49', '2025-07-25 17:06:49');
INSERT INTO `word_library` VALUES (1907331092369626460, '光彩夺目', 'draw-idiom', '成语', '2025-07-25 17:06:49', '2025-07-25 17:06:49');
INSERT INTO `word_library` VALUES (1907331092369626461, '当务之急', 'draw-idiom', '成语', '2025-07-25 17:06:49', '2025-07-25 17:06:49');
INSERT INTO `word_library` VALUES (1907331092369626462, '当头棒喝', 'draw-idiom', '成语', '2025-07-25 17:06:49', '2025-07-25 17:06:49');
INSERT INTO `word_library` VALUES (1907331092369626463, '当局者迷', 'draw-idiom', '成语', '2025-07-25 17:06:49', '2025-07-25 17:06:49');
INSERT INTO `word_library` VALUES (1907331092369626464, '当断不断', 'draw-idiom', '成语', '2025-07-25 17:06:49', '2025-07-25 17:06:49');
INSERT INTO `word_library` VALUES (1907331092369626465, '回旋余地', 'draw-idiom', '成语', '2025-07-25 17:06:49', '2025-07-25 17:06:49');
INSERT INTO `word_library` VALUES (1907331092369626466, '因势利导', 'draw-idiom', '成语', '2025-07-25 17:06:49', '2025-07-25 17:06:49');
INSERT INTO `word_library` VALUES (1907331092369626467, '同工异曲', 'draw-idiom', '成语', '2025-07-25 17:06:49', '2025-07-25 17:06:49');
INSERT INTO `word_library` VALUES (1907331092369626468, '同仇敌忾', 'draw-idiom', '成语', '2025-07-25 17:06:49', '2025-07-25 17:06:49');
INSERT INTO `word_library` VALUES (1907331092369626469, '同甘共苦', 'draw-idiom', '成语', '2025-07-25 17:06:49', '2025-07-25 17:06:49');
INSERT INTO `word_library` VALUES (1907331092369626470, '同病相怜', 'draw-idiom', '成语', '2025-07-25 17:06:49', '2025-07-25 17:06:49');
INSERT INTO `word_library` VALUES (1907331092369626471, '舟中敌国', 'draw-idiom', '成语', '2025-07-25 17:06:49', '2025-07-25 17:06:49');
INSERT INTO `word_library` VALUES (1907331092369626472, '创业维艰', 'draw-idiom', '成语', '2025-07-25 17:06:49', '2025-07-25 17:06:49');
INSERT INTO `word_library` VALUES (1907331092369626473, '刎颈之交', 'draw-idiom', '成语', '2025-07-25 17:06:49', '2025-07-25 17:06:49');
INSERT INTO `word_library` VALUES (1907331092369626474, '华而不实', 'draw-idiom', '成语', '2025-07-25 17:06:49', '2025-07-25 17:06:49');
INSERT INTO `word_library` VALUES (1907331092369626475, '华亭鹤唳', 'draw-idiom', '成语', '2025-07-25 17:06:49', '2025-07-25 17:06:49');
INSERT INTO `word_library` VALUES (1907331092369626476, '牝牡骊黄', 'draw-idiom', '成语', '2025-07-25 17:06:49', '2025-07-25 17:06:49');
INSERT INTO `word_library` VALUES (1907331092369626477, '向火乞儿', 'draw-idiom', '成语', '2025-07-25 17:06:49', '2025-07-25 17:06:49');
INSERT INTO `word_library` VALUES (1907331092369626478, '优孟衣冠', 'draw-idiom', '成语', '2025-07-25 17:06:49', '2025-07-25 17:06:49');
INSERT INTO `word_library` VALUES (1907331092369626479, '休牛放马', 'draw-idiom', '成语', '2025-07-25 17:06:49', '2025-07-25 17:06:49');
INSERT INTO `word_library` VALUES (1907331092369626480, '覆水难收', 'draw-idiom', '成语', '2025-07-25 17:06:49', '2025-07-25 17:06:49');
INSERT INTO `word_library` VALUES (1907331092369626481, '翻然悔悟', 'draw-idiom', '成语', '2025-07-25 17:06:49', '2025-07-25 17:06:49');
INSERT INTO `word_library` VALUES (1907331092369626482, '魑魅魍魉', 'draw-idiom', '成语', '2025-07-25 17:06:49', '2025-07-25 17:06:49');
INSERT INTO `word_library` VALUES (1907331092369626483, '攀龙附凤', 'draw-idiom', '成语', '2025-07-25 17:06:49', '2025-07-25 17:06:49');
INSERT INTO `word_library` VALUES (1907431092369625090, '西瓜', 'undercover', '水果', '2025-07-25 17:36:10', '2025-07-25 17:36:10');
INSERT INTO `word_library` VALUES (1907431092369625091, '苹果', 'undercover', '水果', '2025-07-25 17:36:10', '2025-07-25 17:36:10');
INSERT INTO `word_library` VALUES (1907431092369625092, '香蕉', 'undercover', '水果', '2025-07-25 17:36:10', '2025-07-25 17:36:10');
INSERT INTO `word_library` VALUES (1907431092369625093, '葡萄', 'undercover', '水果', '2025-07-25 17:36:10', '2025-07-25 17:36:10');
INSERT INTO `word_library` VALUES (1907431092369625094, '橙子', 'undercover', '水果', '2025-07-25 17:36:10', '2025-07-25 17:36:10');
INSERT INTO `word_library` VALUES (1907431092369625095, '草莓', 'undercover', '水果', '2025-07-25 17:36:10', '2025-07-25 17:36:10');
INSERT INTO `word_library` VALUES (1907431092369625096, '菠萝', 'undercover', '水果', '2025-07-25 17:36:10', '2025-07-25 17:36:10');
INSERT INTO `word_library` VALUES (1907431092369625097, '芒果', 'undercover', '水果', '2025-07-25 17:36:10', '2025-07-25 17:36:10');
INSERT INTO `word_library` VALUES (1907431092369625098, '桃子', 'undercover', '水果', '2025-07-25 17:36:10', '2025-07-25 17:36:10');
INSERT INTO `word_library` VALUES (1907431092369625099, '梨子', 'undercover', '水果', '2025-07-25 17:36:10', '2025-07-25 17:36:10');
INSERT INTO `word_library` VALUES (1907431092369625100, '狮子', 'undercover', '动物', '2025-07-25 17:36:10', '2025-07-25 17:36:10');
INSERT INTO `word_library` VALUES (1907431092369625101, '老虎', 'undercover', '动物', '2025-07-25 17:36:10', '2025-07-25 17:36:10');
INSERT INTO `word_library` VALUES (1907431092369625102, '大象', 'undercover', '动物', '2025-07-25 17:36:10', '2025-07-25 17:36:10');
INSERT INTO `word_library` VALUES (1907431092369625103, '长颈鹿', 'undercover', '动物', '2025-07-25 17:36:10', '2025-07-25 17:36:10');
INSERT INTO `word_library` VALUES (1907431092369625104, '熊猫', 'undercover', '动物', '2025-07-25 17:36:10', '2025-07-25 17:36:10');
INSERT INTO `word_library` VALUES (1907431092369625105, '斑马', 'undercover', '动物', '2025-07-25 17:36:10', '2025-07-25 17:36:10');
INSERT INTO `word_library` VALUES (1907431092369625106, '河马', 'undercover', '动物', '2025-07-25 17:36:10', '2025-07-25 17:36:10');
INSERT INTO `word_library` VALUES (1907431092369625107, '犀牛', 'undercover', '动物', '2025-07-25 17:36:10', '2025-07-25 17:36:10');
INSERT INTO `word_library` VALUES (1907431092369625108, '袋鼠', 'undercover', '动物', '2025-07-25 17:36:10', '2025-07-25 17:36:10');
INSERT INTO `word_library` VALUES (1907431092369625109, '考拉', 'undercover', '动物', '2025-07-25 17:36:10', '2025-07-25 17:36:10');
INSERT INTO `word_library` VALUES (1907431092369625110, '胡萝卜', 'undercover', '蔬菜', '2025-07-25 17:36:10', '2025-07-25 17:36:10');
INSERT INTO `word_library` VALUES (1907431092369625111, '土豆', 'undercover', '蔬菜', '2025-07-25 17:36:10', '2025-07-25 17:36:10');
INSERT INTO `word_library` VALUES (1907431092369625112, '西红柿', 'undercover', '蔬菜', '2025-07-25 17:36:10', '2025-07-25 17:36:10');
INSERT INTO `word_library` VALUES (1907431092369625113, '黄瓜', 'undercover', '蔬菜', '2025-07-25 17:36:10', '2025-07-25 17:36:10');
INSERT INTO `word_library` VALUES (1907431092369625114, '茄子', 'undercover', '蔬菜', '2025-07-25 17:36:10', '2025-07-25 17:36:10');
INSERT INTO `word_library` VALUES (1907431092369625115, '青椒', 'undercover', '蔬菜', '2025-07-25 17:36:10', '2025-07-25 17:36:10');
INSERT INTO `word_library` VALUES (1907431092369625116, '白菜', 'undercover', '蔬菜', '2025-07-25 17:36:11', '2025-07-25 17:36:11');
INSERT INTO `word_library` VALUES (1907431092369625117, '菠菜', 'undercover', '蔬菜', '2025-07-25 17:36:11', '2025-07-25 17:36:11');
INSERT INTO `word_library` VALUES (1907431092369625118, '芹菜', 'undercover', '蔬菜', '2025-07-25 17:36:11', '2025-07-25 17:36:11');
INSERT INTO `word_library` VALUES (1907431092369625119, '洋葱', 'undercover', '蔬菜', '2025-07-25 17:36:11', '2025-07-25 17:36:11');
INSERT INTO `word_library` VALUES (1907431092369625120, '汽车', 'undercover', '交通工具', '2025-07-25 17:36:11', '2025-07-25 17:36:11');
INSERT INTO `word_library` VALUES (1907431092369625121, '飞机', 'undercover', '交通工具', '2025-07-25 17:36:11', '2025-07-25 17:36:11');
INSERT INTO `word_library` VALUES (1907431092369625122, '火车', 'undercover', '交通工具', '2025-07-25 17:36:11', '2025-07-25 17:36:11');
INSERT INTO `word_library` VALUES (1907431092369625123, '轮船', 'undercover', '交通工具', '2025-07-25 17:36:11', '2025-07-25 17:36:11');
INSERT INTO `word_library` VALUES (1907431092369625124, '自行车', 'undercover', '交通工具', '2025-07-25 17:36:11', '2025-07-25 17:36:11');
INSERT INTO `word_library` VALUES (1907431092369625125, '摩托车', 'undercover', '交通工具', '2025-07-25 17:36:11', '2025-07-25 17:36:11');
INSERT INTO `word_library` VALUES (1907431092369625126, '公交车', 'undercover', '交通工具', '2025-07-25 17:36:11', '2025-07-25 17:36:11');
INSERT INTO `word_library` VALUES (1907431092369625127, '地铁', 'undercover', '交通工具', '2025-07-25 17:36:11', '2025-07-25 17:36:11');
INSERT INTO `word_library` VALUES (1907431092369625128, '出租车', 'undercover', '交通工具', '2025-07-25 17:36:11', '2025-07-25 17:36:11');
INSERT INTO `word_library` VALUES (1907431092369625129, '电动车', 'undercover', '交通工具', '2025-07-25 17:36:11', '2025-07-25 17:36:11');
INSERT INTO `word_library` VALUES (1907431092369625130, '手机', 'undercover', '电子产品', '2025-07-25 17:36:11', '2025-07-25 17:36:11');
INSERT INTO `word_library` VALUES (1907431092369625131, '电脑', 'undercover', '电子产品', '2025-07-25 17:36:11', '2025-07-25 17:36:11');
INSERT INTO `word_library` VALUES (1907431092369625132, '平板', 'undercover', '电子产品', '2025-07-25 17:36:11', '2025-07-25 17:36:11');
INSERT INTO `word_library` VALUES (1907431092369625133, '相机', 'undercover', '电子产品', '2025-07-25 17:36:11', '2025-07-25 17:36:11');
INSERT INTO `word_library` VALUES (1907431092369625134, '耳机', 'undercover', '电子产品', '2025-07-25 17:36:11', '2025-07-25 17:36:11');
INSERT INTO `word_library` VALUES (1907431092369625135, '音响', 'undercover', '电子产品', '2025-07-25 17:36:11', '2025-07-25 17:36:11');
INSERT INTO `word_library` VALUES (1907431092369625136, '电视', 'undercover', '电子产品', '2025-07-25 17:36:11', '2025-07-25 17:36:11');
INSERT INTO `word_library` VALUES (1907431092369625137, '游戏机', 'undercover', '电子产品', '2025-07-25 17:36:11', '2025-07-25 17:36:11');
INSERT INTO `word_library` VALUES (1907431092369625138, '智能手表', 'undercover', '电子产品', '2025-07-25 17:36:11', '2025-07-25 17:36:11');
INSERT INTO `word_library` VALUES (1907431092369625139, '路由器', 'undercover', '电子产品', '2025-07-25 17:36:11', '2025-07-25 17:36:11');
INSERT INTO `word_library` VALUES (1907431092369625140, '冰箱', 'undercover', '家电', '2025-07-25 17:36:11', '2025-07-25 17:36:11');
INSERT INTO `word_library` VALUES (1907431092369625141, '洗衣机', 'undercover', '家电', '2025-07-25 17:36:11', '2025-07-25 17:36:11');
INSERT INTO `word_library` VALUES (1907431092369625142, '空调', 'undercover', '家电', '2025-07-25 17:36:11', '2025-07-25 17:36:11');
INSERT INTO `word_library` VALUES (1907431092369625143, '微波炉', 'undercover', '家电', '2025-07-25 17:36:11', '2025-07-25 17:36:11');
INSERT INTO `word_library` VALUES (1907431092369625144, '电饭煲', 'undercover', '家电', '2025-07-25 17:36:11', '2025-07-25 17:36:11');
INSERT INTO `word_library` VALUES (1907431092369625145, '吸尘器', 'undercover', '家电', '2025-07-25 17:36:11', '2025-07-25 17:36:11');
INSERT INTO `word_library` VALUES (1907431092369625146, '电风扇', 'undercover', '家电', '2025-07-25 17:36:11', '2025-07-25 17:36:11');
INSERT INTO `word_library` VALUES (1907431092369625147, '热水器', 'undercover', '家电', '2025-07-25 17:36:11', '2025-07-25 17:36:11');
INSERT INTO `word_library` VALUES (1907431092369625148, '电磁炉', 'undercover', '家电', '2025-07-25 17:36:11', '2025-07-25 17:36:11');
INSERT INTO `word_library` VALUES (1907431092369625149, '烤箱', 'undercover', '家电', '2025-07-25 17:36:11', '2025-07-25 17:36:11');
INSERT INTO `word_library` VALUES (1907431092369625150, '篮球', 'undercover', '体育用品', '2025-07-25 17:36:11', '2025-07-25 17:36:11');
INSERT INTO `word_library` VALUES (1907431092369625151, '足球', 'undercover', '体育用品', '2025-07-25 17:36:11', '2025-07-25 17:36:11');
INSERT INTO `word_library` VALUES (1907431092369625152, '乒乓球', 'undercover', '体育用品', '2025-07-25 17:36:11', '2025-07-25 17:36:11');
INSERT INTO `word_library` VALUES (1907431092369625153, '羽毛球', 'undercover', '体育用品', '2025-07-25 17:36:11', '2025-07-25 17:36:11');
INSERT INTO `word_library` VALUES (1907431092369625154, '网球', 'undercover', '体育用品', '2025-07-25 17:36:11', '2025-07-25 17:36:11');
INSERT INTO `word_library` VALUES (1907431092369625155, '排球', 'undercover', '体育用品', '2025-07-25 17:36:11', '2025-07-25 17:36:11');
INSERT INTO `word_library` VALUES (1907431092369625156, '高尔夫球', 'undercover', '体育用品', '2025-07-25 17:36:11', '2025-07-25 17:36:11');
INSERT INTO `word_library` VALUES (1907431092369625157, '跳绳', 'undercover', '体育用品', '2025-07-25 17:36:11', '2025-07-25 17:36:11');
INSERT INTO `word_library` VALUES (1907431092369625158, '哑铃', 'undercover', '体育用品', '2025-07-25 17:36:11', '2025-07-25 17:36:11');
INSERT INTO `word_library` VALUES (1907431092369625159, '瑜伽垫', 'undercover', '体育用品', '2025-07-25 17:36:11', '2025-07-25 17:36:11');
INSERT INTO `word_library` VALUES (1907431092369625160, 'T恤', 'undercover', '服装', '2025-07-25 17:36:11', '2025-07-25 17:36:11');
INSERT INTO `word_library` VALUES (1907431092369625161, '衬衫', 'undercover', '服装', '2025-07-25 17:36:11', '2025-07-25 17:36:11');
INSERT INTO `word_library` VALUES (1907431092369625162, '牛仔裤', 'undercover', '服装', '2025-07-25 17:36:11', '2025-07-25 17:36:11');
INSERT INTO `word_library` VALUES (1907431092369625163, '外套', 'undercover', '服装', '2025-07-25 17:36:11', '2025-07-25 17:36:11');
INSERT INTO `word_library` VALUES (1907431092369625164, '毛衣', 'undercover', '服装', '2025-07-25 17:36:11', '2025-07-25 17:36:11');
INSERT INTO `word_library` VALUES (1907431092369625165, '裙子', 'undercover', '服装', '2025-07-25 17:36:11', '2025-07-25 17:36:11');
INSERT INTO `word_library` VALUES (1907431092369625166, '短裤', 'undercover', '服装', '2025-07-25 17:36:11', '2025-07-25 17:36:11');
INSERT INTO `word_library` VALUES (1907431092369625167, '西装', 'undercover', '服装', '2025-07-25 17:36:11', '2025-07-25 17:36:11');
INSERT INTO `word_library` VALUES (1907431092369625168, '羽绒服', 'undercover', '服装', '2025-07-25 17:36:11', '2025-07-25 17:36:11');
INSERT INTO `word_library` VALUES (1907431092369625169, '运动服', 'undercover', '服装', '2025-07-25 17:36:11', '2025-07-25 17:36:11');
INSERT INTO `word_library` VALUES (1907431092369625170, '医生', 'undercover', '职业', '2025-07-25 17:36:11', '2025-07-25 17:36:11');
INSERT INTO `word_library` VALUES (1907431092369625171, '教师', 'undercover', '职业', '2025-07-25 17:36:11', '2025-07-25 17:36:11');
INSERT INTO `word_library` VALUES (1907431092369625172, '警察', 'undercover', '职业', '2025-07-25 17:36:11', '2025-07-25 17:36:11');
INSERT INTO `word_library` VALUES (1907431092369625173, '厨师', 'undercover', '职业', '2025-07-25 17:36:11', '2025-07-25 17:36:11');
INSERT INTO `word_library` VALUES (1907431092369625174, '司机', 'undercover', '职业', '2025-07-25 17:36:11', '2025-07-25 17:36:11');
INSERT INTO `word_library` VALUES (1907431092369625175, '律师', 'undercover', '职业', '2025-07-25 17:36:11', '2025-07-25 17:36:11');
INSERT INTO `word_library` VALUES (1907431092369625176, '工程师', 'undercover', '职业', '2025-07-25 17:36:11', '2025-07-25 17:36:11');
INSERT INTO `word_library` VALUES (1907431092369625177, '护士', 'undercover', '职业', '2025-07-25 17:36:11', '2025-07-25 17:36:11');
INSERT INTO `word_library` VALUES (1907431092369625178, '记者', 'undercover', '职业', '2025-07-25 17:36:11', '2025-07-25 17:36:11');
INSERT INTO `word_library` VALUES (1907431092369625179, '程序员', 'undercover', '职业', '2025-07-25 17:36:11', '2025-07-25 17:36:11');
INSERT INTO `word_library` VALUES (1907431092369625180, '中国', 'undercover', '国家', '2025-07-25 17:36:11', '2025-07-25 17:36:11');
INSERT INTO `word_library` VALUES (1907431092369625181, '美国', 'undercover', '国家', '2025-07-25 17:36:11', '2025-07-25 17:36:11');
INSERT INTO `word_library` VALUES (1907431092369625182, '日本', 'undercover', '国家', '2025-07-25 17:36:11', '2025-07-25 17:36:11');
INSERT INTO `word_library` VALUES (1907431092369625183, '英国', 'undercover', '国家', '2025-07-25 17:36:11', '2025-07-25 17:36:11');
INSERT INTO `word_library` VALUES (1907431092369625184, '法国', 'undercover', '国家', '2025-07-25 17:36:11', '2025-07-25 17:36:11');
INSERT INTO `word_library` VALUES (1907431092369625185, '德国', 'undercover', '国家', '2025-07-25 17:36:11', '2025-07-25 17:36:11');
INSERT INTO `word_library` VALUES (1907431092369625186, '俄罗斯', 'undercover', '国家', '2025-07-25 17:36:11', '2025-07-25 17:36:11');
INSERT INTO `word_library` VALUES (1907431092369625187, '加拿大', 'undercover', '国家', '2025-07-25 17:36:11', '2025-07-25 17:36:11');
INSERT INTO `word_library` VALUES (1907431092369625188, '澳大利亚', 'undercover', '国家', '2025-07-25 17:36:11', '2025-07-25 17:36:11');
INSERT INTO `word_library` VALUES (1907431092369625189, '巴西', 'undercover', '国家', '2025-07-25 17:36:11', '2025-07-25 17:36:11');
INSERT INTO `word_library` VALUES (1907431092369625190, '北京', 'undercover', '城市', '2025-07-25 17:36:11', '2025-07-25 17:36:11');
INSERT INTO `word_library` VALUES (1907431092369625191, '上海', 'undercover', '城市', '2025-07-25 17:36:11', '2025-07-25 17:36:11');
INSERT INTO `word_library` VALUES (1907431092369625192, '广州', 'undercover', '城市', '2025-07-25 17:36:11', '2025-07-25 17:36:11');
INSERT INTO `word_library` VALUES (1907431092369625193, '深圳', 'undercover', '城市', '2025-07-25 17:36:11', '2025-07-25 17:36:11');
INSERT INTO `word_library` VALUES (1907431092369625194, '杭州', 'undercover', '城市', '2025-07-25 17:36:11', '2025-07-25 17:36:11');
INSERT INTO `word_library` VALUES (1907431092369625195, '成都', 'undercover', '城市', '2025-07-25 17:36:11', '2025-07-25 17:36:11');
INSERT INTO `word_library` VALUES (1907431092369625196, '重庆', 'undercover', '城市', '2025-07-25 17:36:11', '2025-07-25 17:36:11');
INSERT INTO `word_library` VALUES (1907431092369625197, '武汉', 'undercover', '城市', '2025-07-25 17:36:11', '2025-07-25 17:36:11');
INSERT INTO `word_library` VALUES (1907431092369625198, '西安', 'undercover', '城市', '2025-07-25 17:36:11', '2025-07-25 17:36:11');
INSERT INTO `word_library` VALUES (1907431092369625199, '南京', 'undercover', '城市', '2025-07-25 17:36:11', '2025-07-25 17:36:11');
INSERT INTO `word_library` VALUES (1907431092369625200, '春节', 'undercover', '节日', '2025-07-25 17:36:11', '2025-07-25 17:36:11');
INSERT INTO `word_library` VALUES (1907431092369625201, '中秋节', 'undercover', '节日', '2025-07-25 17:36:11', '2025-07-25 17:36:11');
INSERT INTO `word_library` VALUES (1907431092369625202, '端午节', 'undercover', '节日', '2025-07-25 17:36:11', '2025-07-25 17:36:11');
INSERT INTO `word_library` VALUES (1907431092369625203, '国庆节', 'undercover', '节日', '2025-07-25 17:36:11', '2025-07-25 17:36:11');
INSERT INTO `word_library` VALUES (1907431092369625204, '元旦', 'undercover', '节日', '2025-07-25 17:36:11', '2025-07-25 17:36:11');
INSERT INTO `word_library` VALUES (1907431092369625205, '清明节', 'undercover', '节日', '2025-07-25 17:36:11', '2025-07-25 17:36:11');
INSERT INTO `word_library` VALUES (1907431092369625206, '劳动节', 'undercover', '节日', '2025-07-25 17:36:11', '2025-07-25 17:36:11');
INSERT INTO `word_library` VALUES (1907431092369625207, '儿童节', 'undercover', '节日', '2025-07-25 17:36:11', '2025-07-25 17:36:11');
INSERT INTO `word_library` VALUES (1907431092369625208, '圣诞节', 'undercover', '节日', '2025-07-25 17:36:11', '2025-07-25 17:36:11');
INSERT INTO `word_library` VALUES (1907431092369625209, '情人节', 'undercover', '节日', '2025-07-25 17:36:11', '2025-07-25 17:36:11');
INSERT INTO `word_library` VALUES (1907431092369625210, '可乐', 'undercover', '饮料', '2025-07-25 17:36:11', '2025-07-25 17:36:11');
INSERT INTO `word_library` VALUES (1907431092369625211, '雪碧', 'undercover', '饮料', '2025-07-25 17:36:11', '2025-07-25 17:36:11');
INSERT INTO `word_library` VALUES (1907431092369625212, '果汁', 'undercover', '饮料', '2025-07-25 17:36:11', '2025-07-25 17:36:11');
INSERT INTO `word_library` VALUES (1907431092369625213, '牛奶', 'undercover', '饮料', '2025-07-25 17:36:11', '2025-07-25 17:36:11');
INSERT INTO `word_library` VALUES (1907431092369625214, '咖啡', 'undercover', '饮料', '2025-07-25 17:36:11', '2025-07-25 17:36:11');
INSERT INTO `word_library` VALUES (1907431092369625215, '茶', 'undercover', '饮料', '2025-07-25 17:36:11', '2025-07-25 17:36:11');
INSERT INTO `word_library` VALUES (1907431092369625216, '啤酒', 'undercover', '饮料', '2025-07-25 17:36:11', '2025-07-25 17:36:11');
INSERT INTO `word_library` VALUES (1907431092369625217, '红酒', 'undercover', '饮料', '2025-07-25 17:36:11', '2025-07-25 17:36:11');
INSERT INTO `word_library` VALUES (1907431092369625218, '白酒', 'undercover', '饮料', '2025-07-25 17:36:11', '2025-07-25 17:36:11');
INSERT INTO `word_library` VALUES (1907431092369625219, '矿泉水', 'undercover', '饮料', '2025-07-25 17:36:11', '2025-07-25 17:36:11');
INSERT INTO `word_library` VALUES (1907431092369625220, '米饭', 'undercover', '食物', '2025-07-25 17:36:11', '2025-07-25 17:36:11');
INSERT INTO `word_library` VALUES (1907431092369625221, '面条', 'undercover', '食物', '2025-07-25 17:36:11', '2025-07-25 17:36:11');
INSERT INTO `word_library` VALUES (1907431092369625222, '饺子', 'undercover', '食物', '2025-07-25 17:36:11', '2025-07-25 17:36:11');
INSERT INTO `word_library` VALUES (1907431092369625223, '包子', 'undercover', '食物', '2025-07-25 17:36:11', '2025-07-25 17:36:11');
INSERT INTO `word_library` VALUES (1907431092369625224, '汉堡', 'undercover', '食物', '2025-07-25 17:36:11', '2025-07-25 17:36:11');
INSERT INTO `word_library` VALUES (1907431092369625225, '披萨', 'undercover', '食物', '2025-07-25 17:36:11', '2025-07-25 17:36:11');
INSERT INTO `word_library` VALUES (1907431092369625226, '寿司', 'undercover', '食物', '2025-07-25 17:36:11', '2025-07-25 17:36:11');
INSERT INTO `word_library` VALUES (1907431092369625227, '牛排', 'undercover', '食物', '2025-07-25 17:36:11', '2025-07-25 17:36:11');
INSERT INTO `word_library` VALUES (1907431092369625228, '炸鸡', 'undercover', '食物', '2025-07-25 17:36:11', '2025-07-25 17:36:11');
INSERT INTO `word_library` VALUES (1907431092369625229, '沙拉', 'undercover', '食物', '2025-07-25 17:36:11', '2025-07-25 17:36:11');
INSERT INTO `word_library` VALUES (1907431092369625230, '钢琴', 'undercover', '乐器', '2025-07-25 17:36:11', '2025-07-25 17:36:11');
INSERT INTO `word_library` VALUES (1907431092369625231, '吉他', 'undercover', '乐器', '2025-07-25 17:36:11', '2025-07-25 17:36:11');
INSERT INTO `word_library` VALUES (1907431092369625232, '小提琴', 'undercover', '乐器', '2025-07-25 17:36:11', '2025-07-25 17:36:11');
INSERT INTO `word_library` VALUES (1907431092369625233, '鼓', 'undercover', '乐器', '2025-07-25 17:36:11', '2025-07-25 17:36:11');
INSERT INTO `word_library` VALUES (1907431092369625234, '笛子', 'undercover', '乐器', '2025-07-25 17:36:11', '2025-07-25 17:36:11');
INSERT INTO `word_library` VALUES (1907431092369625235, '萨克斯', 'undercover', '乐器', '2025-07-25 17:36:11', '2025-07-25 17:36:11');
INSERT INTO `word_library` VALUES (1907431092369625236, '二胡', 'undercover', '乐器', '2025-07-25 17:36:11', '2025-07-25 17:36:11');
INSERT INTO `word_library` VALUES (1907431092369625237, '口琴', 'undercover', '乐器', '2025-07-25 17:36:11', '2025-07-25 17:36:11');
INSERT INTO `word_library` VALUES (1907431092369625238, '古筝', 'undercover', '乐器', '2025-07-25 17:36:11', '2025-07-25 17:36:11');
INSERT INTO `word_library` VALUES (1907431092369625239, '电子琴', 'undercover', '乐器', '2025-07-25 17:36:11', '2025-07-25 17:36:11');
INSERT INTO `word_library` VALUES (1907431092369625240, '红色', 'undercover', '颜色', '2025-07-25 17:36:11', '2025-07-25 17:36:11');
INSERT INTO `word_library` VALUES (1907431092369625241, '蓝色', 'undercover', '颜色', '2025-07-25 17:36:11', '2025-07-25 17:36:11');
INSERT INTO `word_library` VALUES (1907431092369625242, '绿色', 'undercover', '颜色', '2025-07-25 17:36:11', '2025-07-25 17:36:11');
INSERT INTO `word_library` VALUES (1907431092369625243, '黄色', 'undercover', '颜色', '2025-07-25 17:36:11', '2025-07-25 17:36:11');
INSERT INTO `word_library` VALUES (1907431092369625244, '黑色', 'undercover', '颜色', '2025-07-25 17:36:11', '2025-07-25 17:36:11');
INSERT INTO `word_library` VALUES (1907431092369625245, '白色', 'undercover', '颜色', '2025-07-25 17:36:11', '2025-07-25 17:36:11');
INSERT INTO `word_library` VALUES (1907431092369625246, '紫色', 'undercover', '颜色', '2025-07-25 17:36:11', '2025-07-25 17:36:11');
INSERT INTO `word_library` VALUES (1907431092369625247, '橙色', 'undercover', '颜色', '2025-07-25 17:36:11', '2025-07-25 17:36:11');
INSERT INTO `word_library` VALUES (1907431092369625248, '粉色', 'undercover', '颜色', '2025-07-25 17:36:11', '2025-07-25 17:36:11');
INSERT INTO `word_library` VALUES (1907431092369625249, '灰色', 'undercover', '颜色', '2025-07-25 17:36:11', '2025-07-25 17:36:11');
INSERT INTO `word_library` VALUES (1907431092369625250, '泰坦尼克号', 'undercover', '电影', '2025-07-25 17:36:11', '2025-07-25 17:36:11');
INSERT INTO `word_library` VALUES (1907431092369625251, '阿凡达', 'undercover', '电影', '2025-07-25 17:36:11', '2025-07-25 17:36:11');
INSERT INTO `word_library` VALUES (1907431092369625252, '肖申克的救赎', 'undercover', '电影', '2025-07-25 17:36:11', '2025-07-25 17:36:11');
INSERT INTO `word_library` VALUES (1907431092369625253, '盗梦空间', 'undercover', '电影', '2025-07-25 17:36:11', '2025-07-25 17:36:11');
INSERT INTO `word_library` VALUES (1907431092369625254, '星际穿越', 'undercover', '电影', '2025-07-25 17:36:11', '2025-07-25 17:36:11');
INSERT INTO `word_library` VALUES (1907431092369625255, '复仇者联盟', 'undercover', '电影', '2025-07-25 17:36:11', '2025-07-25 17:36:11');
INSERT INTO `word_library` VALUES (1907431092369625256, '哈利波特', 'undercover', '电影', '2025-07-25 17:36:11', '2025-07-25 17:36:11');
INSERT INTO `word_library` VALUES (1907431092369625257, '指环王', 'undercover', '电影', '2025-07-25 17:36:11', '2025-07-25 17:36:11');
INSERT INTO `word_library` VALUES (1907431092369625258, '速度与激情', 'undercover', '电影', '2025-07-25 17:36:11', '2025-07-25 17:36:11');
INSERT INTO `word_library` VALUES (1907431092369625259, '变形金刚', 'undercover', '电影', '2025-07-25 17:36:11', '2025-07-25 17:36:11');
INSERT INTO `word_library` VALUES (1907431092369625260, '红楼梦', 'undercover', '书籍', '2025-07-25 17:36:11', '2025-07-25 17:36:11');
INSERT INTO `word_library` VALUES (1907431092369625261, '三国演义', 'undercover', '书籍', '2025-07-25 17:36:11', '2025-07-25 17:36:11');
INSERT INTO `word_library` VALUES (1907431092369625262, '水浒传', 'undercover', '书籍', '2025-07-25 17:36:11', '2025-07-25 17:36:11');
INSERT INTO `word_library` VALUES (1907431092369625263, '西游记', 'undercover', '书籍', '2025-07-25 17:36:11', '2025-07-25 17:36:11');
INSERT INTO `word_library` VALUES (1907431092369625264, '围城', 'undercover', '书籍', '2025-07-25 17:36:11', '2025-07-25 17:36:11');
INSERT INTO `word_library` VALUES (1907431092369625265, '活着', 'undercover', '书籍', '2025-07-25 17:36:11', '2025-07-25 17:36:11');
INSERT INTO `word_library` VALUES (1907431092369625266, '平凡的世界', 'undercover', '书籍', '2025-07-25 17:36:11', '2025-07-25 17:36:11');
INSERT INTO `word_library` VALUES (1907431092369625267, '百年孤独', 'undercover', '书籍', '2025-07-25 17:36:11', '2025-07-25 17:36:11');
INSERT INTO `word_library` VALUES (1907431092369625268, '小王子', 'undercover', '书籍', '2025-07-25 17:36:11', '2025-07-25 17:36:11');
INSERT INTO `word_library` VALUES (1907431092369625269, '哈利波特', 'undercover', '书籍', '2025-07-25 17:36:11', '2025-07-25 17:36:11');
INSERT INTO `word_library` VALUES (1907431092369625270, '数学', 'undercover', '学科', '2025-07-25 17:36:11', '2025-07-25 17:36:11');
INSERT INTO `word_library` VALUES (1907431092369625271, '语文', 'undercover', '学科', '2025-07-25 17:36:11', '2025-07-25 17:36:11');
INSERT INTO `word_library` VALUES (1907431092369625272, '英语', 'undercover', '学科', '2025-07-25 17:36:11', '2025-07-25 17:36:11');
INSERT INTO `word_library` VALUES (1907431092369625273, '物理', 'undercover', '学科', '2025-07-25 17:36:11', '2025-07-25 17:36:11');
INSERT INTO `word_library` VALUES (1907431092369625274, '化学', 'undercover', '学科', '2025-07-25 17:36:11', '2025-07-25 17:36:11');
INSERT INTO `word_library` VALUES (1907431092369625275, '生物', 'undercover', '学科', '2025-07-25 17:36:11', '2025-07-25 17:36:11');
INSERT INTO `word_library` VALUES (1907431092369625276, '历史', 'undercover', '学科', '2025-07-25 17:36:11', '2025-07-25 17:36:11');
INSERT INTO `word_library` VALUES (1907431092369625277, '地理', 'undercover', '学科', '2025-07-25 17:36:11', '2025-07-25 17:36:11');
INSERT INTO `word_library` VALUES (1907431092369625278, '政治', 'undercover', '学科', '2025-07-25 17:36:11', '2025-07-25 17:36:11');
INSERT INTO `word_library` VALUES (1907431092369625279, '计算机', 'undercover', '学科', '2025-07-25 17:36:11', '2025-07-25 17:36:11');
INSERT INTO `word_library` VALUES (1907431092369625280, '跑步', 'undercover', '运动', '2025-07-25 17:36:11', '2025-07-25 17:36:11');
INSERT INTO `word_library` VALUES (1907431092369625281, '游泳', 'undercover', '运动', '2025-07-25 17:36:11', '2025-07-25 17:36:11');
INSERT INTO `word_library` VALUES (1907431092369625282, '篮球', 'undercover', '运动', '2025-07-25 17:36:11', '2025-07-25 17:36:11');
INSERT INTO `word_library` VALUES (1907431092369625283, '足球', 'undercover', '运动', '2025-07-25 17:36:11', '2025-07-25 17:36:11');
INSERT INTO `word_library` VALUES (1907431092369625284, '羽毛球', 'undercover', '运动', '2025-07-25 17:36:11', '2025-07-25 17:36:11');
INSERT INTO `word_library` VALUES (1907431092369625285, '乒乓球', 'undercover', '运动', '2025-07-25 17:36:11', '2025-07-25 17:36:11');
INSERT INTO `word_library` VALUES (1907431092369625286, '网球', 'undercover', '运动', '2025-07-25 17:36:11', '2025-07-25 17:36:11');
INSERT INTO `word_library` VALUES (1907431092369625287, '高尔夫', 'undercover', '运动', '2025-07-25 17:36:11', '2025-07-25 17:36:11');
INSERT INTO `word_library` VALUES (1907431092369625288, '瑜伽', 'undercover', '运动', '2025-07-25 17:36:11', '2025-07-25 17:36:11');
INSERT INTO `word_library` VALUES (1907431092369625289, '滑雪', 'undercover', '运动', '2025-07-25 17:36:11', '2025-07-25 17:36:11');
INSERT INTO `word_library` VALUES (1907431092369630001, '沙发', 'undercover', '家具', '2025-07-25 17:36:11', '2025-07-25 17:36:11');
INSERT INTO `word_library` VALUES (1907431092369630002, '床', 'undercover', '家具', '2025-07-25 17:36:11', '2025-07-25 17:36:11');
INSERT INTO `word_library` VALUES (1907431092369630003, '书桌', 'undercover', '家具', '2025-07-25 17:36:11', '2025-07-25 17:36:11');
INSERT INTO `word_library` VALUES (1907431092369630004, '衣柜', 'undercover', '家具', '2025-07-25 17:36:11', '2025-07-25 17:36:11');
INSERT INTO `word_library` VALUES (1907431092369630005, '椅子', 'undercover', '家具', '2025-07-25 17:36:11', '2025-07-25 17:36:11');
INSERT INTO `word_library` VALUES (1907431092369630006, '茶几', 'undercover', '家具', '2025-07-25 17:36:11', '2025-07-25 17:36:11');
INSERT INTO `word_library` VALUES (1907431092369630007, '梳妆台', 'undercover', '家具', '2025-07-25 17:36:11', '2025-07-25 17:36:11');
INSERT INTO `word_library` VALUES (1907431092369630008, '餐桌', 'undercover', '家具', '2025-07-25 17:36:11', '2025-07-25 17:36:11');
INSERT INTO `word_library` VALUES (1907431092369630009, '书架', 'undercover', '家具', '2025-07-25 17:36:11', '2025-07-25 17:36:11');
INSERT INTO `word_library` VALUES (1907431092369630010, '床头柜', 'undercover', '家具', '2025-07-25 17:36:11', '2025-07-25 17:36:11');
INSERT INTO `word_library` VALUES (1907431092369630011, '晴天', 'undercover', '天气', '2025-07-25 17:36:11', '2025-07-25 17:36:11');
INSERT INTO `word_library` VALUES (1907431092369630012, '雨天', 'undercover', '天气', '2025-07-25 17:36:11', '2025-07-25 17:36:11');
INSERT INTO `word_library` VALUES (1907431092369630013, '雪天', 'undercover', '天气', '2025-07-25 17:36:11', '2025-07-25 17:36:11');
INSERT INTO `word_library` VALUES (1907431092369630014, '阴天', 'undercover', '天气', '2025-07-25 17:36:11', '2025-07-25 17:36:11');
INSERT INTO `word_library` VALUES (1907431092369630015, '雾霾', 'undercover', '天气', '2025-07-25 17:36:11', '2025-07-25 17:36:11');
INSERT INTO `word_library` VALUES (1907431092369630016, '冰雹', 'undercover', '天气', '2025-07-25 17:36:11', '2025-07-25 17:36:11');
INSERT INTO `word_library` VALUES (1907431092369630017, '台风', 'undercover', '天气', '2025-07-25 17:36:11', '2025-07-25 17:36:11');
INSERT INTO `word_library` VALUES (1907431092369630018, '雷电', 'undercover', '天气', '2025-07-25 17:36:11', '2025-07-25 17:36:11');
INSERT INTO `word_library` VALUES (1907431092369630019, '彩虹', 'undercover', '天气', '2025-07-25 17:36:11', '2025-07-25 17:36:11');
INSERT INTO `word_library` VALUES (1907431092369630020, '霜冻', 'undercover', '天气', '2025-07-25 17:36:11', '2025-07-25 17:36:11');
INSERT INTO `word_library` VALUES (1907431092369630021, '开心', 'undercover', '情绪', '2025-07-25 17:36:11', '2025-07-25 17:36:11');
INSERT INTO `word_library` VALUES (1907431092369630022, '悲伤', 'undercover', '情绪', '2025-07-25 17:36:11', '2025-07-25 17:36:11');
INSERT INTO `word_library` VALUES (1907431092369630023, '愤怒', 'undercover', '情绪', '2025-07-25 17:36:11', '2025-07-25 17:36:11');
INSERT INTO `word_library` VALUES (1907431092369630024, '惊讶', 'undercover', '情绪', '2025-07-25 17:36:11', '2025-07-25 17:36:11');
INSERT INTO `word_library` VALUES (1907431092369630025, '恐惧', 'undercover', '情绪', '2025-07-25 17:36:11', '2025-07-25 17:36:11');
INSERT INTO `word_library` VALUES (1907431092369630026, '厌恶', 'undercover', '情绪', '2025-07-25 17:36:11', '2025-07-25 17:36:11');
INSERT INTO `word_library` VALUES (1907431092369630027, '喜悦', 'undercover', '情绪', '2025-07-25 17:36:11', '2025-07-25 17:36:11');
INSERT INTO `word_library` VALUES (1907431092369630028, '焦虑', 'undercover', '情绪', '2025-07-25 17:36:11', '2025-07-25 17:36:11');
INSERT INTO `word_library` VALUES (1907431092369630029, '平静', 'undercover', '情绪', '2025-07-25 17:36:11', '2025-07-25 17:36:11');
INSERT INTO `word_library` VALUES (1907431092369630030, '兴奋', 'undercover', '情绪', '2025-07-25 17:36:11', '2025-07-25 17:36:11');
INSERT INTO `word_library` VALUES (1907431092369630031, '春天', 'undercover', '季节', '2025-07-25 17:36:11', '2025-07-25 17:36:11');
INSERT INTO `word_library` VALUES (1907431092369630032, '夏天', 'undercover', '季节', '2025-07-25 17:36:11', '2025-07-25 17:36:11');
INSERT INTO `word_library` VALUES (1907431092369630033, '秋天', 'undercover', '季节', '2025-07-25 17:36:11', '2025-07-25 17:36:11');
INSERT INTO `word_library` VALUES (1907431092369630034, '冬天', 'undercover', '季节', '2025-07-25 17:36:11', '2025-07-25 17:36:11');
INSERT INTO `word_library` VALUES (1907431092369630035, '春季', 'undercover', '季节', '2025-07-25 17:36:11', '2025-07-25 17:36:11');
INSERT INTO `word_library` VALUES (1907431092369630036, '夏季', 'undercover', '季节', '2025-07-25 17:36:11', '2025-07-25 17:36:11');
INSERT INTO `word_library` VALUES (1907431092369630037, '秋季', 'undercover', '季节', '2025-07-25 17:36:11', '2025-07-25 17:36:11');
INSERT INTO `word_library` VALUES (1907431092369630038, '冬季', 'undercover', '季节', '2025-07-25 17:36:11', '2025-07-25 17:36:11');
INSERT INTO `word_library` VALUES (1907431092369630039, '暖春', 'undercover', '季节', '2025-07-25 17:36:11', '2025-07-25 17:36:11');
INSERT INTO `word_library` VALUES (1907431092369630040, '酷夏', 'undercover', '季节', '2025-07-25 17:36:11', '2025-07-25 17:36:11');
INSERT INTO `word_library` VALUES (1907431092369630041, '早晨', 'undercover', '时间', '2025-07-25 17:36:11', '2025-07-25 17:36:11');
INSERT INTO `word_library` VALUES (1907431092369630042, '中午', 'undercover', '时间', '2025-07-25 17:36:11', '2025-07-25 17:36:11');
INSERT INTO `word_library` VALUES (1907431092369630043, '晚上', 'undercover', '时间', '2025-07-25 17:36:11', '2025-07-25 17:36:11');
INSERT INTO `word_library` VALUES (1907431092369630044, '黎明', 'undercover', '时间', '2025-07-25 17:36:11', '2025-07-25 17:36:11');
INSERT INTO `word_library` VALUES (1907431092369630045, '黄昏', 'undercover', '时间', '2025-07-25 17:36:11', '2025-07-25 17:36:11');
INSERT INTO `word_library` VALUES (1907431092369630046, '午夜', 'undercover', '时间', '2025-07-25 17:36:11', '2025-07-25 17:36:11');
INSERT INTO `word_library` VALUES (1907431092369630047, '清晨', 'undercover', '时间', '2025-07-25 17:36:11', '2025-07-25 17:36:11');
INSERT INTO `word_library` VALUES (1907431092369630048, '傍晚', 'undercover', '时间', '2025-07-25 17:36:11', '2025-07-25 17:36:11');
INSERT INTO `word_library` VALUES (1907431092369630049, '凌晨', 'undercover', '时间', '2025-07-25 17:36:11', '2025-07-25 17:36:11');
INSERT INTO `word_library` VALUES (1907431092369630050, '深夜', 'undercover', '时间', '2025-07-25 17:36:11', '2025-07-25 17:36:11');
INSERT INTO `word_library` VALUES (1907431092369630051, '山川', 'undercover', '自然景观', '2025-07-25 17:36:11', '2025-07-25 17:36:11');
INSERT INTO `word_library` VALUES (1907431092369630052, '河流', 'undercover', '自然景观', '2025-07-25 17:36:11', '2025-07-25 17:36:11');
INSERT INTO `word_library` VALUES (1907431092369630053, '湖泊', 'undercover', '自然景观', '2025-07-25 17:36:11', '2025-07-25 17:36:11');
INSERT INTO `word_library` VALUES (1907431092369630054, '海洋', 'undercover', '自然景观', '2025-07-25 17:36:11', '2025-07-25 17:36:11');
INSERT INTO `word_library` VALUES (1907431092369630055, '森林', 'undercover', '自然景观', '2025-07-25 17:36:11', '2025-07-25 17:36:11');
INSERT INTO `word_library` VALUES (1907431092369630056, '沙漠', 'undercover', '自然景观', '2025-07-25 17:36:11', '2025-07-25 17:36:11');
INSERT INTO `word_library` VALUES (1907431092369630057, '草原', 'undercover', '自然景观', '2025-07-25 17:36:11', '2025-07-25 17:36:11');
INSERT INTO `word_library` VALUES (1907431092369630058, '峡谷', 'undercover', '自然景观', '2025-07-25 17:36:11', '2025-07-25 17:36:11');
INSERT INTO `word_library` VALUES (1907431092369630059, '瀑布', 'undercover', '自然景观', '2025-07-25 17:36:11', '2025-07-25 17:36:11');
INSERT INTO `word_library` VALUES (1907431092369630060, '岛屿', 'undercover', '自然景观', '2025-07-25 17:36:11', '2025-07-25 17:36:11');
INSERT INTO `word_library` VALUES (1907431092369630061, '高楼', 'undercover', '建筑物', '2025-07-25 17:36:11', '2025-07-25 17:36:11');
INSERT INTO `word_library` VALUES (1907431092369630062, '桥梁', 'undercover', '建筑物', '2025-07-25 17:36:11', '2025-07-25 17:36:11');
INSERT INTO `word_library` VALUES (1907431092369630063, '塔楼', 'undercover', '建筑物', '2025-07-25 17:36:11', '2025-07-25 17:36:11');
INSERT INTO `word_library` VALUES (1907431092369630064, '宫殿', 'undercover', '建筑物', '2025-07-25 17:36:11', '2025-07-25 17:36:11');
INSERT INTO `word_library` VALUES (1907431092369630065, '教堂', 'undercover', '建筑物', '2025-07-25 17:36:11', '2025-07-25 17:36:11');
INSERT INTO `word_library` VALUES (1907431092369630066, '城堡', 'undercover', '建筑物', '2025-07-25 17:36:11', '2025-07-25 17:36:11');
INSERT INTO `word_library` VALUES (1907431092369630067, '寺庙', 'undercover', '建筑物', '2025-07-25 17:36:11', '2025-07-25 17:36:11');
INSERT INTO `word_library` VALUES (1907431092369630068, '图书馆', 'undercover', '建筑物', '2025-07-25 17:36:11', '2025-07-25 17:36:11');
INSERT INTO `word_library` VALUES (1907431092369630069, '博物馆', 'undercover', '建筑物', '2025-07-25 17:36:11', '2025-07-25 17:36:11');
INSERT INTO `word_library` VALUES (1907431092369630070, '体育馆', 'undercover', '建筑物', '2025-07-25 17:36:11', '2025-07-25 17:36:11');

SET FOREIGN_KEY_CHECKS = 1;


-- 转盘表
CREATE TABLE IF NOT EXISTS turntable
(
    id             BIGINT AUTO_INCREMENT COMMENT '转盘 ID' PRIMARY KEY,
    type           INT COMMENT '转盘类型 1-宠物装备转盘 2-称号转盘',
    name           VARCHAR(256) COMMENT '转盘名称',
    costPoints     INT      DEFAULT 1 COMMENT '每次抽奖消耗积分',
    guaranteeCount INT      DEFAULT 300 COMMENT '保底触发次数，0 表示无保底',
    status         INT      DEFAULT 1 COMMENT '1启用 0禁用',
    createTime     DATETIME DEFAULT CURRENT_TIMESTAMP NOT NULL COMMENT '创建时间',
    updateTime     DATETIME DEFAULT CURRENT_TIMESTAMP NOT NULL ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    isDelete       TINYINT  DEFAULT 0                 NOT NULL COMMENT '是否删除'
    ) COMMENT '转盘表' COLLATE = utf8mb4_unicode_ci;

-- 转盘奖励表
CREATE TABLE IF NOT EXISTS turntable_prize
(
    id           BIGINT AUTO_INCREMENT COMMENT '奖励ID' PRIMARY KEY,
    turntableId  BIGINT COMMENT '转盘 ID',
    prizeId      BIGINT COMMENT '物品 ID',
    quality      INT NOT NULL COMMENT '奖励品质 1-普通(N) 2-稀有(R) 3-史诗(SR) 4-传说(SSR)',
    prizeType    INT COMMENT '物品类型 1-装备 2-称号',
    probability  INT NOT NULL COMMENT '概率权重，总权重1000 ',
    stock        INT DEFAULT -1 COMMENT '奖品数量 -1表示无限',
    createTime   DATETIME DEFAULT CURRENT_TIMESTAMP NOT NULL COMMENT '创建时间',
    updateTime   DATETIME DEFAULT CURRENT_TIMESTAMP NOT NULL ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    isDelete     TINYINT  DEFAULT 0                 NOT NULL COMMENT '是否删除'
) COMMENT '奖项表' COLLATE = utf8mb4_unicode_ci;

-- 抽奖记录表
CREATE TABLE IF NOT EXISTS turntable_draw_record
(
    id               BIGINT AUTO_INCREMENT PRIMARY KEY COMMENT '抽奖记录ID',
    userId           BIGINT NOT NULL COMMENT '用户ID',
    turntableId      BIGINT NOT NULL COMMENT '转盘 ID',
    turntablePrizeId BIGINT NOT NULL COMMENT '装盘奖励ID',
    name             VARCHAR(128) COMMENT '奖品名称',
    prizeType        INT COMMENT '奖励类型 1-装备 2-称号',
    prizeId          BIGINT COMMENT '奖励ID',
    quality          INT COMMENT '奖励品质',
    costPoints       INT COMMENT '本次消耗积分',
    isGuarantee      TINYINT DEFAULT 0 COMMENT '是否触发保底 0-否 1-是',
    guaranteeType    INT COMMENT '保底类型 1-小保底 2-大保底',
    createTime       DATETIME DEFAULT CURRENT_TIMESTAMP NOT NULL COMMENT '创建时间',
    updateTime       DATETIME DEFAULT CURRENT_TIMESTAMP NOT NULL ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    isDelete         TINYINT DEFAULT 0 NOT NULL COMMENT '是否删除 0-否 1-是'
    ) COMMENT '转盘抽奖记录表' COLLATE = utf8mb4_unicode_ci;

-- 用户转盘进度表
CREATE TABLE IF NOT EXISTS turntable_user_progress
(
    id             BIGINT AUTO_INCREMENT PRIMARY KEY COMMENT '用户转盘进度ID',
    userId         BIGINT NOT NULL COMMENT '用户ID',
    turntableId    BIGINT NOT NULL COMMENT '转盘ID',
    smallFailCount INT DEFAULT 0 COMMENT '小保底失败次数 10次小保底命中清零',
    totalDrawCount INT DEFAULT 0 COMMENT '累计抽奖次数 大保底命中重置或减300',
    guaranteeCount INT DEFAULT 300 COMMENT '保底阈值（冗余快照）',
    lastDrawTime   DATETIME COMMENT '上次抽奖时间',
    createTime     DATETIME DEFAULT CURRENT_TIMESTAMP NOT NULL COMMENT '创建时间',
    updateTime     DATETIME DEFAULT CURRENT_TIMESTAMP NOT NULL ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    isDelete       TINYINT DEFAULT 0 NOT NULL COMMENT '是否删除 0-否 1-是',
    UNIQUE KEY uk_user_turntable (userId, turntableId)
    ) COMMENT '用户转盘进度表' COLLATE = utf8mb4_unicode_ci;

-- 添加索引
CREATE INDEX idx_turntable_prize_turntableId ON turntable_prize(turntableId);
CREATE INDEX idx_draw_record_userId ON turntable_draw_record(userId);
CREATE INDEX idx_draw_record_turntableId ON turntable_draw_record(turntableId);
CREATE INDEX idx_user_progress_userId ON turntable_user_progress(userId);


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

-- 用户备注表
CREATE TABLE IF NOT EXISTS user_remark
(
    id          BIGINT AUTO_INCREMENT COMMENT '备注ID' PRIMARY KEY,
    userId     BIGINT                            NOT NULL COMMENT '用户ID',
    content     VARCHAR(512)                      NULL COMMENT '备注内容',
    createTime DATETIME DEFAULT CURRENT_TIMESTAMP NOT NULL COMMENT '创建时间',
    updateTime DATETIME DEFAULT CURRENT_TIMESTAMP NOT NULL ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    isDelete   TINYINT  DEFAULT 0               NOT NULL COMMENT '是否删除：0-未删除，1-已删除',
    UNIQUE INDEX uk_user_id (userId),
    INDEX idx_create_time (createTime)
    ) COMMENT '用户备注表' COLLATE = utf8mb4_unicode_ci;

-- 爬塔记录表
CREATE TABLE IF NOT EXISTS tower_climb_record
(
    id          BIGINT AUTO_INCREMENT COMMENT '主键ID' PRIMARY KEY,
    userId      BIGINT                             NOT NULL COMMENT '用户ID',
    floor       INT        DEFAULT 1               NOT NULL COMMENT '当前挑战层数',
    maxFloor    INT        DEFAULT 0               NOT NULL COMMENT '历史最高通关层数',
    result      TINYINT    DEFAULT 0               NOT NULL COMMENT '本次挑战结果：0-失败，1-胜利',
    petLevel    INT        DEFAULT 1               NOT NULL COMMENT '挑战时宠物等级',
    petHpLeft   INT        DEFAULT 0               NOT NULL COMMENT '挑战结束时宠物剩余血量',
    rewardPoints INT       DEFAULT 0               NOT NULL COMMENT '本次获得积分奖励',
    createTime  DATETIME   DEFAULT CURRENT_TIMESTAMP NOT NULL COMMENT '创建时间',
    updateTime  DATETIME   DEFAULT CURRENT_TIMESTAMP NOT NULL ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    isDelete    TINYINT    DEFAULT 0               NOT NULL COMMENT '是否删除',
    INDEX idx_user_id (userId),
    INDEX idx_floor (floor),
    INDEX idx_max_floor (maxFloor)
    ) COMMENT = '爬塔挑战记录表' COLLATE = utf8mb4_unicode_ci;

-- 用户爬塔进度表（记录每个用户当前最高层）
CREATE TABLE IF NOT EXISTS tower_climb_progress
(
    id          BIGINT AUTO_INCREMENT COMMENT '主键ID' PRIMARY KEY,
    userId      BIGINT                             NOT NULL COMMENT '用户ID',
    maxFloor    INT        DEFAULT 0               NOT NULL COMMENT '历史最高通关层数',
    createTime  DATETIME   DEFAULT CURRENT_TIMESTAMP NOT NULL COMMENT '创建时间',
    updateTime  DATETIME   DEFAULT CURRENT_TIMESTAMP NOT NULL ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    isDelete    TINYINT    DEFAULT 0               NOT NULL COMMENT '是否删除',
    UNIQUE INDEX uk_user_id (userId)
    ) COMMENT = '用户爬塔进度表' COLLATE = utf8mb4_unicode_ci;

-- 连续签到相关表
use fish;

-- 用户签到记录表
create table if not exists user_sign_in
(
    id             bigint auto_increment comment '记录ID' primary key,
    userId         bigint                             not null comment '用户ID',
    signDate       date                               not null comment '签到日期',
    signType       tinyint  default 1                 not null comment '签到类型：1-正常签到，2-补签',
    continuousDays int      default 1                 not null comment '当次签到后的连续天数',
    rewardPoints   int      default 0                 not null comment '本次签到获得的积分奖励（含连续奖励）',
    createTime     datetime default CURRENT_TIMESTAMP not null comment '创建时间',
    updateTime     datetime default CURRENT_TIMESTAMP not null on update CURRENT_TIMESTAMP comment '更新时间',
    isDelete       tinyint  default 0                 not null comment '是否删除',
    unique key uk_userId_signDate (userId, signDate),
    index idx_userId (userId),
    index idx_signDate (signDate)
    ) comment '用户签到记录表' collate = utf8mb4_unicode_ci;

-- 连续签到奖励配置表
create table if not exists sign_in_reward_config
(
    id             bigint auto_increment comment '配置ID' primary key,
    continuousDays int          default 0  not null comment '连续签到天数（达到该天数触发，0表示基础签到）',
    rewardPoints   int          default 0  not null comment '额外奖励积分（叠加在基础签到积分之上）',
    rewardDesc     varchar(128) default '' not null comment '奖励描述',
    isCycle        tinyint      default 0  not null comment '是否按周期循环：0-不循环，1-循环',
    cycleDays      int          default 7  not null comment '循环周期天数（isCycle=1 时有效）',
    createTime     datetime default CURRENT_TIMESTAMP not null comment '创建时间',
    updateTime     datetime default CURRENT_TIMESTAMP not null on update CURRENT_TIMESTAMP comment '更新时间',
    isDelete       tinyint  default 0                 not null comment '是否删除',
    index idx_continuousDays (continuousDays)
    ) comment '连续签到奖励配置表' collate = utf8mb4_unicode_ci;

-- 初始化 7 天循环奖励配置
-- 基础签到积分 10（由 PointConstant.SIGN_IN_POINT 控制），此处为额外奖励
insert into sign_in_reward_config (continuousDays, rewardPoints, rewardDesc, isCycle, cycleDays) values
                                                                                                     (1, 0,  '第1天签到',              1, 7),
                                                                                                     (2, 5,  '连续2天，额外+5积分',    1, 7),
                                                                                                     (3, 5,  '连续3天，额外+5积分',    1, 7),
                                                                                                     (4, 10, '连续4天，额外+10积分',   1, 7),
                                                                                                     (5, 10, '连续5天，额外+10积分',   1, 7),
                                                                                                     (6, 15, '连续6天，额外+15积分',   1, 7),
                                                                                                     (7, 30, '连续7天，额外+30积分',   1, 7);

-- 宠物装备锻造表
-- 装备位置：1-武器 2-手套 3-鞋子 4-头盔 5-项链 6-翅膀
-- 词条等级：WHITE=白 BLUE=蓝 PURPLE=紫 GOLD=金 RED=红
-- 词条属性：ATTACK=攻击力 MAX_HP=最大生命值 DEFENSE=防御力
--           CRIT_RATE=暴击率 COMBO_RATE=连击率 DODGE_RATE=闪避率
--           BLOCK_RATE=格挡率 LIFESTEAL=吸血率
--           及对应抗性：ANTI_CRIT ANTI_COMBO ANTI_DODGE ANTI_BLOCK ANTI_LIFESTEAL

create table if not exists pet_equip_forge
(
    id          BIGINT       auto_increment comment '主键ID' PRIMARY KEY,
    petId       BIGINT       not null comment '宠物ID',
    equipSlot   TINYINT      not null comment '装备位置 1-武器 2-手套 3-鞋子 4-头盔 5-项链 6-翅膀',
    equipLevel  TINYINT      not null default 0 comment '装备等级（默认0）',
    entry1      JSON         default null comment '词条1 {attr: 词条属性, grade: 词条等级, value: 属性数值, locked: 是否锁定}',
    entry2      JSON         default null comment '词条2 {attr: 词条属性, grade: 词条等级, value: 属性数值, locked: 是否锁定}',
    entry3      JSON         default null comment '词条3 {attr: 词条属性, grade: 词条等级, value: 属性数值, locked: 是否锁定}',
    entry4      JSON         default null comment '词条4 {attr: 词条属性, grade: 词条等级, value: 属性数值, locked: 是否锁定}',
    createTime  DATETIME     default CURRENT_TIMESTAMP not null comment '创建时间',
    updateTime  DATETIME     default CURRENT_TIMESTAMP not null on update CURRENT_TIMESTAMP comment '更新时间',
    isDelete    TINYINT      default 0 not null comment '是否删除',
    UNIQUE KEY uk_pet_slot (petId, equipSlot)
    ) comment '宠物装备锻造表' collate = utf8mb4_unicode_ci;

-- 微信朋友圈相关表设计
-- @author fish-island

USE fish;

-- =============================================
-- 1. 朋友圈动态表
-- =============================================
CREATE TABLE IF NOT EXISTS moments
(
    id          BIGINT AUTO_INCREMENT COMMENT '动态ID' PRIMARY KEY,
    userId      BIGINT                             NOT NULL COMMENT '发布者用户ID',
    content     VARCHAR(2000)                      NULL COMMENT '文字内容',
    mediaJson   JSON                               NULL COMMENT '媒体资源列表（图片/视频），格式：[{type:"image",url:"..."},{type:"video",url:"...",cover:"..."}]',
    location    VARCHAR(128)                       NULL COMMENT '位置信息',
    visibility  TINYINT  DEFAULT 0                 NOT NULL COMMENT '可见范围：0-所有朋友，1-仅自己，2-部分可见，3-不给谁看',
    allowList   JSON                               NULL COMMENT '部分可见的用户ID列表（visibility=2时有效）',
    blockList   JSON                               NULL COMMENT '不给谁看的用户ID列表（visibility=3时有效）',
    likeNum     INT      DEFAULT 0                 NOT NULL COMMENT '点赞数',
    commentNum  INT      DEFAULT 0                 NOT NULL COMMENT '评论数',
    createTime  DATETIME DEFAULT CURRENT_TIMESTAMP NOT NULL COMMENT '发布时间',
    updateTime  DATETIME DEFAULT CURRENT_TIMESTAMP NOT NULL ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    isDelete    TINYINT  DEFAULT 0                 NOT NULL COMMENT '是否删除',
    INDEX idx_userId (userId),
    INDEX idx_createTime (createTime)
    ) COMMENT '朋友圈动态表' COLLATE = utf8mb4_unicode_ci;

-- =============================================
-- 2. 动态点赞表（硬删除）
-- =============================================
CREATE TABLE IF NOT EXISTS moments_like
(
    id         BIGINT AUTO_INCREMENT COMMENT '主键ID' PRIMARY KEY,
    momentId   BIGINT                             NOT NULL COMMENT '动态ID',
    userId     BIGINT                             NOT NULL COMMENT '点赞用户ID',
    createTime DATETIME DEFAULT CURRENT_TIMESTAMP NOT NULL COMMENT '点赞时间',
    UNIQUE KEY uk_moment_user (momentId, userId),
    INDEX idx_momentId (momentId),
    INDEX idx_userId (userId)
    ) COMMENT '朋友圈点赞表' COLLATE = utf8mb4_unicode_ci;

-- =============================================
-- 3. 动态评论表
-- =============================================
CREATE TABLE IF NOT EXISTS moments_comment
(
    id         BIGINT AUTO_INCREMENT COMMENT '评论ID' PRIMARY KEY,
    momentId   BIGINT                             NOT NULL COMMENT '动态ID',
    userId     BIGINT                             NOT NULL COMMENT '评论者用户ID',
    replyUserId BIGINT                            NULL COMMENT '被回复的用户ID（NULL表示直接评论动态）',
    parentId   BIGINT                             NULL COMMENT '父评论ID（NULL表示顶级评论）',
    content    VARCHAR(500)                       NOT NULL COMMENT '评论内容',
    createTime DATETIME DEFAULT CURRENT_TIMESTAMP NOT NULL COMMENT '评论时间',
    updateTime DATETIME DEFAULT CURRENT_TIMESTAMP NOT NULL ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    isDelete   TINYINT  DEFAULT 0                 NOT NULL COMMENT '是否删除',
    INDEX idx_momentId (momentId),
    INDEX idx_userId (userId),
    INDEX idx_parentId (parentId)
    ) COMMENT '朋友圈评论表' COLLATE = utf8mb4_unicode_ci;


-- fish_auth 第三方应用表（OAuth2 客户端）
CREATE TABLE IF NOT EXISTS fish_auth
(
    id          BIGINT AUTO_INCREMENT COMMENT 'id' PRIMARY KEY,
    appName     VARCHAR(128)                        NOT NULL COMMENT '应用名称',
    appWebsite  VARCHAR(512)                        NULL COMMENT '应用网站地址',
    appDesc     VARCHAR(1024)                       NULL COMMENT '应用描述',
    redirectUri VARCHAR(1024)                       NOT NULL COMMENT '回调地址（多个用逗号分隔）',
    clientId    VARCHAR(64)                         NOT NULL COMMENT 'Client ID',
    clientSecret VARCHAR(128)                       NOT NULL COMMENT 'Client Secret（加密存储）',
    userId      BIGINT                              NOT NULL COMMENT '创建者用户 ID',
    status      TINYINT  DEFAULT 1                  NOT NULL COMMENT '状态：0-禁用，1-启用',
    createTime  DATETIME DEFAULT CURRENT_TIMESTAMP  NOT NULL COMMENT '创建时间',
    updateTime  DATETIME DEFAULT CURRENT_TIMESTAMP  NOT NULL ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    isDelete    TINYINT  DEFAULT 0                  NOT NULL COMMENT '是否删除',
    UNIQUE KEY uk_clientId (clientId)
    ) COMMENT '第三方应用（OAuth2 客户端）' COLLATE = utf8mb4_unicode_ci;

-- fish_auth_code 授权码表（临时，5 分钟过期）
CREATE TABLE IF NOT EXISTS fish_auth_code
(
    id          BIGINT AUTO_INCREMENT COMMENT 'id' PRIMARY KEY,
    code        VARCHAR(128)                        NOT NULL COMMENT '授权码',
    clientId    VARCHAR(64)                         NOT NULL COMMENT 'Client ID',
    userId      BIGINT                              NOT NULL COMMENT '授权用户 ID',
    redirectUri VARCHAR(1024)                       NOT NULL COMMENT '回调地址',
    scope       VARCHAR(256)  DEFAULT 'read'        NOT NULL COMMENT '授权范围',
    used        TINYINT       DEFAULT 0             NOT NULL COMMENT '是否已使用：0-未使用，1-已使用',
    expireTime  DATETIME                            NOT NULL COMMENT '过期时间',
    createTime  DATETIME DEFAULT CURRENT_TIMESTAMP  NOT NULL COMMENT '创建时间',
    UNIQUE KEY uk_code (code),
    INDEX idx_clientId (clientId)
    ) COMMENT 'OAuth2 授权码' COLLATE = utf8mb4_unicode_ci;

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


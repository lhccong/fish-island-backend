# 数据库初始化
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

-- 头像框表
create table if not exists avatar_frame
(
    frameId        BIGINT auto_increment comment '头像框 ID' PRIMARY KEY,
    url            VARCHAR(256) comment '头像框名称',
    name           VARCHAR(256) comment '头像框名称',
    points         INT      DEFAULT 1 comment '头像框所需兑换积分',
    createTime     datetime default CURRENT_TIMESTAMP not null comment '创建时间',
    updateTime     datetime default CURRENT_TIMESTAMP not null on update CURRENT_TIMESTAMP comment '更新时间',
    isDelete       tinyint  default 0                 not null comment '是否删除'
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

-- 帖子表
create table if not exists post
(
    id         bigint auto_increment comment 'id' primary key,
    title      varchar(512)                       null comment '标题',
    content    text                               null comment '内容',
    tags       varchar(1024)                      null comment '标签列表（json 数组）',
    thumbNum   int      default 0                 not null comment '点赞数',
    favourNum  int      default 0                 not null comment '收藏数',
    userId     bigint                             not null comment '创建用户 id',
    createTime datetime default CURRENT_TIMESTAMP not null comment '创建时间',
    updateTime datetime default CURRENT_TIMESTAMP not null on update CURRENT_TIMESTAMP comment '更新时间',
    isDelete   tinyint  default 0                 not null comment '是否删除',
    index idx_userId (userId)
) comment '帖子' collate = utf8mb4_unicode_ci;

-- 帖子点赞表（硬删除）
create table if not exists post_thumb
(
    id         bigint auto_increment comment 'id' primary key,
    postId     bigint                             not null comment '帖子 id',
    userId     bigint                             not null comment '创建用户 id',
    createTime datetime default CURRENT_TIMESTAMP not null comment '创建时间',
    updateTime datetime default CURRENT_TIMESTAMP not null on update CURRENT_TIMESTAMP comment '更新时间',
    index idx_postId (postId),
    index idx_userId (userId)
) comment '帖子点赞';

-- 帖子收藏表（硬删除）
create table if not exists post_favour
(
    id         bigint auto_increment comment 'id' primary key,
    postId     bigint                             not null comment '帖子 id',
    userId     bigint                             not null comment '创建用户 id',
    createTime datetime default CURRENT_TIMESTAMP not null comment '创建时间',
    updateTime datetime default CURRENT_TIMESTAMP not null on update CURRENT_TIMESTAMP comment '更新时间',
    index idx_postId (postId),
    index idx_userId (userId)
) comment '帖子收藏';

-- 热点表
drop table if exists hot_post;
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
    id             bigint auto_increment comment 'id' primary key,
    userId      bigint                             not null comment '用户 id',
    emoticonSrc       varchar(512)                            null comment '表情包地址',
    createTime     datetime      default CURRENT_TIMESTAMP not null comment '创建时间',
    updateTime     datetime      default CURRENT_TIMESTAMP not null on update CURRENT_TIMESTAMP comment '更新时间',
    index idx_userId (userId)
) comment '收藏表情包表' collate = utf8mb4_unicode_ci;

-- 第三方用户关联表
create table if not exists  `user_third_auth` (
   `id` bigint(20) not null auto_increment,
   `user_id` bigint(20) not null comment '本地用户id',
   `nickname` varchar(200) collate utf8mb4_general_ci default null comment '昵称',
   `avatar` varchar(200) collate utf8mb4_general_ci default null comment '头像',
   `platform` varchar(20) character set utf8mb4 collate utf8mb4_general_ci not null comment '平台：github/gitee',
   `openid` varchar(100) character set utf8mb4 collate utf8mb4_general_ci not null comment '平台用户id',
   `access_token` varchar(500) character set utf8mb4 collate utf8mb4_general_ci default null comment 'access_token',
   `refresh_token` varchar(500) character set utf8mb4 collate utf8mb4_general_ci default null comment 'refresh_token',
   `expire_time` datetime default null comment '过期时间',
   `raw_data` json default null comment '原始响应数据',
   primary key (`id`),
   unique key `idx_platform_openid` (`platform`,`openid`)
) comment '第三方用户关联表' collate = utf8mb4_general_ci;
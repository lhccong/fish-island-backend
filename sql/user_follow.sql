-- 用户关注表
use fish;

create table if not exists user_follow
(
    id           bigint auto_increment comment 'id' primary key,
    userId       bigint                             not null comment '关注者用户ID（我）',
    followUserId bigint                             not null comment '被关注者用户ID（TA）',
    createTime   datetime default CURRENT_TIMESTAMP not null comment '创建时间',
    updateTime   datetime default CURRENT_TIMESTAMP not null on update CURRENT_TIMESTAMP comment '更新时间',
    unique key uk_userId_followUserId (userId, followUserId),
    index idx_userId (userId),
    index idx_followUserId (followUserId)
) comment '用户关注表' collate = utf8mb4_unicode_ci;

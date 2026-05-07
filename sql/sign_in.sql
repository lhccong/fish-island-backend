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

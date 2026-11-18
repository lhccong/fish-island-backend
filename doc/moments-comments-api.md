# 鱼小圈多级回复

`POST /moments/comment/add` 的 `parentId` 为直接被回复评论 ID，不是根评论 ID。服务端校验父评论存在且属于同一动态，并用其作者覆盖 `replyUserId`；顶级评论不接受虚构的被回复用户。通知发送给直接被回复的作者。

`POST /moments/comment/list` 仍按顶级评论分页，total 为顶级评论数量；每个节点的 children 返回直接子评论，逐级嵌套。每层批量查询并去重，同层按时间、ID 倒序。超出最后一页时保留真实 total。

`POST /moments/comment/delete` 按原有本人/管理员权限删除所选评论及其后代，动态计数按实际删除行数扣减。仅顶级评论可置顶。

多级回复无需数据库迁移。历史上被客户端压平的回复缺少唯一父评论信息，不自动重建。应与 Next 前端的 ADR-054 同步发布；旧版客户端继续使用原字段也能读取根节点及直接子项。

## 评论点赞

部署前在已应用 `add_top_field.sql` 的数据库执行 `sql/20261118_moments_comment_like.sql`，现有评论的 `likeNum` 初始化为 0。

`POST /moments/comment/like` 接收 `{ "commentId": "123", "liked": true }`，取消点赞传 `false`。接口须登录且遵循动态可见范围。成功返回 `data: true`，重复提交相同目标状态也成功，且赞数只在关系发生变化时调整。不发通知或发放积分。评论采用软删除，已删除评论的点赞关系随原评论保留，无法再写入或通过评论列表读取。

`POST /moments/comment/list` 在各级节点返回 `likeNum` 和当前账号的 `liked`。顶级评论在分页前按置顶、赞数、创建时间、ID 倒序；回复在同一父评论下按赞数、创建时间、ID 倒序。

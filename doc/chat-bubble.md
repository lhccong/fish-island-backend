# 聊天气泡兑换

部署前执行 `sql/20261119_chat_bubble.sql`。目录和样式 ID 随服务发布，价格固定为 300 积分，有效期固定为兑换成功后 30 × 24 小时。

| 端点 | 请求 | 响应 data |
| --- | --- | --- |
| GET /api/chat/bubble/list | 登录会话 | 气泡目录和当前账号权益 |
| POST /api/chat/bubble/exchange | `{"bubbleId":"berry-cat-paw"}` | 兑换后的目录和权益 |
| POST /api/chat/bubble/equip | `{"bubbleId":"berry-cat-paw"}` 或 `{"bubbleId":null}` | 启用或恢复默认后的目录和权益 |

目录项包含 `id`、`name`、`points`、`durationDays`、`expiresAt`（ISO UTC 字符串或空值）和 `equipped`。到期或未兑换的款式不能启用。有效期内重复兑换不会扣费。权益写入、积分扣费与流水在同一事务中完成。

聊天消息、回执和历史增加可选的 `bubbleId`，表示该条消息发送时有效的样式。旧客户端可忽略该字段。到期和更换装扮影响后续发送。历史消息保留原样。

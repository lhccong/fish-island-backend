# 聊天气泡兑换

部署前依次执行 `sql/20261119_chat_bubble.sql` 和 `sql/20261119_chat_bubble_product.sql`。目录表 `chat_bubble_product` 初始化原六款为各 300 积分，重复执行迁移保留已修改的价格和已有账号权益。有效期为兑换成功后 30 × 24 小时。

| 端点 | 请求 | 响应 data |
| --- | --- | --- |
| GET /api/chat/bubble/list | 登录会话 | 气泡目录和当前账号权益 |
| POST /api/chat/bubble/exchange | `{"bubbleId":"berry-cat-paw","expectedPoints":300}` | 兑换后的目录和权益 |
| POST /api/chat/bubble/equip | `{"bubbleId":"berry-cat-paw"}` 或 `{"bubbleId":null}` | 启用或恢复默认后的目录和权益 |

目录项包含 `id`、`name`、`points`、`durationDays`、`expiresAt`（ISO UTC 字符串或空值）、`equipped` 和可选的 `artwork`。到期或未兑换的款式不能启用。有效期内重复兑换不会扣费。扣费以数据库中的价格为准，`expectedPoints` 必须与当前价格一致。价格变动或旧客户端未传报价时，重新读取目录并确认后才能扣费。权益写入、积分扣费与流水在同一事务中完成。

## 管理员接口

| 端点 | 用途 |
| --- | --- |
| GET /api/chat/bubble/admin/list | 读取商品目录及现价 |
| POST /api/chat/bubble/admin/create | 新增气泡 |
| POST /api/chat/bubble/admin/price | 修改现价 |

新增请求示例

```json
{
  "id": "cloud-kite",
  "name": "云上风筝",
  "points": 420,
  "imageUrl": "https://assets.example.com/bubbles/cloud-kite.svg",
  "darkImageUrl": "https://assets.example.com/bubbles/cloud-kite-dark.svg",
  "textTone": "theme"
}
```

调价请求示例

```json
{"id":"berry-cat-paw","points":450}
```

编号以小写字母开头，只包含小写字母、数字及分隔用连字符，最多 32 位。价格为正整数。图片使用长期可访问的公开 HTTPS 地址，推荐透明底 280 × 112 图像，九宫格切片从上、右、下、左分别为 49、68、50、72。暗夜图片可省略，省略时使用同一图片。文字选项为 `theme`（随应用明暗）、`light`（白字）或 `dark`（深字）。`artwork` 返回这三个图片及文字配置字段。

新商品下次读取商城时可见。聊天消息、回执和历史保留发送时的 `bubbleId` 和可选的 `bubbleArtwork`。到期、调价和更换装扮影响后续操作，历史消息和已有有效期保持原样。

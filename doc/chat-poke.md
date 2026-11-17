# 聊天戳一戳

前端架构决策：fish-island-next/docs/decisions/096-chat-poke.md。

部署顺序：先在目标数据库执行 `sql/20261117_user_poke_suffix.sql`，再部署后端与新版前端。全新数据库的 `sql/create_table.sql` 已包含该字段；不要对已包含字段的数据库重复执行迁移。迁移未在开发者/生产数据库自动执行。

个人资料沿用 `POST /user/update/my`，增加可选 `pokeSuffix`：最多 30 个 UTF-16 代码单元，与原有资料和浏览器 maxlength 计数一致（补充平面 emoji 通常计 2）；禁止换行与控制字符，保存时去除首尾空白。空串清除，省略字段保留原值。登录和会话恢复接口通过 LoginUserVO 返回设置。

`POST /chat/message/send` 的戳一戳请求：`{ "clientMessageId": "UUID", "pokeTargetId": "用户编号", "content": "" }`，不允许引用。身份、目标昵称和后缀由服务端提供。被封禁或不存在的目标不可用；发送者须登录、未封禁且未禁言。成功发送后同一发送者间隔 3 秒；重试同一个 clientMessageId 返回原已落库快照，不重读目标设置或重复发放积分，也不会复活已撤回消息。

响应与原有实时、历史协议一致，message 增加 `poke: { targetId, targetName, suffix }`；content 保存兼容旧客户端的完整纯文字。历史保留当时的设置，新前端按当前观看者显示“我”；后缀只作为文字显示，不解析聊天标签。原有 WebSocket 客户端提交的顶层 poke 字段会被剔除，避免伪造结构化快照。

验证命令（项目默认跳过测试，必须显式覆盖）：

```powershell
mvn '-Dmaven.test.skip=false' '-Dtest=HttpChatMessageServiceTest,MessageRevokePolicyTest,PokeProfileTest' test
```

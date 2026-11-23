# 宠物卡池目录

MySQL 保存卡牌、卡池、图集索引及多对多入池关系。卡牌编号与现有前端编号一致，技能触发条件和数值以结构化 JSON 保存。图集文件在 MinIO 的 `pet-cards/atlases/` 下按 SHA-256 存储，卡牌引用文件及 0–9 格位。重复上传同一张图不产生多个对象。

## 首次部署

1. 对目标数据库执行 `sql/20261122_pet_card_catalog.sql`。
2. 确保现有 MinIO 配置指向已创建且允许应用读写的桶。把前端同版本的 `public/assets/pet` 目录提供给后端进程读取，目录需包含原来的 10 个 `card-art*.jpg`。
3. 首次启动设置 `FISH_PET_CARD_SEED_ENABLED=true` 和 `FISH_PET_CARD_SEED_DIR`（图集目录的绝对路径）。导入 2 个卡池、100 张卡、100 条入池关系和 10 份图集。原画原样上传，保留现有 5 列 2 行的裁切模式。数据库事务成功后记录种子版本，重启时跳过已成功的导入。失败时数据库变更回滚，重试复用相同 MinIO 对象键。
4. 访问 `GET /api/pet/cards/catalog` 核对卡池与卡牌，再访问任一卡片的 `GET /api/pet/cards/art/{artKey}` 核对图片。种子导入完成后可去掉两个种子变量。前端管理入口为 `/admin/pet-cards`。

本地进程使用共享 `pre` 环境时，可在后端启动参数中设置 `--fish.game.redis.startup-cleanup=false`，保留已有游戏房间。前端开发模式通过 `VITE_DEV_API_TARGET=http://127.0.0.1:8123` 和 `VITE_DEV_WS_TARGET=ws://127.0.0.1:8090` 转发请求。启用代理时，前端自动使用当前页面的 Vite 地址。

## 接口

| 方法 | 路径 | 用途 |
| --- | --- | --- |
| GET | `/api/pet/cards/catalog` | 玩家卡池目录，空池不参与抽取 |
| GET | `/api/pet/cards/art/{artKey}` | 读取一张十格图集 |
| GET / POST | `/api/admin/pet-cards/pools` | 列出、新增或编辑卡池 |
| GET / POST | `/api/admin/pet-cards/cards` | 按卡池、关键词和页读取、新增或编辑卡牌 |
| GET / POST | `/api/admin/pet-cards/atlases` | 列出图集或上传一张图，上传使用 multipart 的 `name`、`portrait`、`file` |

后台接口走原有 Sa-Token 管理员校验。新增配置提交空版本，编辑提交读取时的版本。卡池编辑支持封面和五档权重，卡牌编辑支持属性、技能、图集、格位及所属卡池。卡池至少留一张可抽卡，已存在的卡牌编号持续可读。领取进度、抽取、试玩积分、收藏与实际 PvP 结算仍各自遵循原有边界。

上传支持 JPG 或 PNG，最大 8 MB。图集为 5 列 2 行，方格图集宽高比 5:2，4:5 竖格图集宽高比 2:1。管理员在每张卡上选择同一图集及不同格位。

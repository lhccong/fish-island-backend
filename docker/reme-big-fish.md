# 大肥鱼长期记忆部署

适用于现有 ReMe `0.4.2.3` 和通过 Docker 启动 JAR 的摸鱼岛后端。ReMe 继续使用原来的数据目录和 `127.0.0.1:2333` 端口发布。

## 给 ReMe 加载检索配置

将此目录中的 `reme-big-fish.yaml` 和 `compose.big-fish.yaml` 上传到服务器 `/thome/reme-server/`。前者定义按 `session_id` 过滤的检索，后者在现有 Compose 配置上挂载并加载它。配置文件需能被 ReMe 容器用户读取。

```bash
cd /thome/reme-server
chmod 644 reme-big-fish.yaml
docker compose -f compose.yaml -f compose.big-fish.yaml up -d --wait reme
curl -fsS -H 'Content-Type: application/json' -d '{}' http://127.0.0.1:2333/version
```

后续重建 ReMe 时继续带上两个 `-f` 参数。

## 让后端容器访问 ReMe

在同一 Docker 主机创建一个共享网络，然后将 ReMe 加入网络并设置名称 `reme`。

```bash
docker network create fish-island-memory
docker network connect --alias reme fish-island-memory reme-server-reme-1
```

后端照常构建镜像。启动时给现有 `docker run` 传入以下环境变量，并加入 `fish-island-memory` 网络。启动命令已使用其他网络时，保留原网络并在启动后通过 `docker network connect fish-island-memory <后端容器名>` 加入此网络。容器重建后需重新执行手动连接，也可在各自的部署配置中将该网络声明为外部网络。

| 后端环境变量 | 值 |
| --- | --- |
| `BIG_FISH_REME_BASE_URL` | `http://reme:2333` |
| `BIG_FISH_REME_SCOPE_KEY` | 用 `openssl rand -hex 32` 生成一次，作为后端环境秘密保存 |

两个变量一起提供后启用。保持会话密钥稳定，应用重启后才能继续访问原来的会话记忆。后端直接在宿主机运行时，URL 可设为 `http://127.0.0.1:2333`。

## 验证

用测试账号在群聊里和大肥鱼对话，直接陈述一个虚构的个人喜好。大肥鱼回复后等 ReMe 提炼和索引，再隔超过 10 秒用关键词提起同一话题。也可明确请大肥鱼记录一条虚构约定。用一句临时想法验证没有记忆请求或明确喜好时跳过提交。用另一个测试账号在相同话题下核对隔离。重启后端并使用相同的会话密钥后，原账号仍可召回已保存的记忆。

ReMe 在现有 `/data` 卷保存提炼笔记和选中的用户发言片段。此配置关闭 ReMe 的应用日志，因为默认检索日志包含查询正文。大肥鱼召回最多使用三段记忆，台词仍由原有 DeepSeek 接口生成。检索失败时沿用短期上下文，提炼失败可在后端日志中查到故障类型。服务端长期记忆可通过清空 `BIG_FISH_REME_SCOPE_KEY` 并重启后端关闭。

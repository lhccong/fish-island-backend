# 第三方个人音乐代理

音乐包完全位于 `src/main/java/com/cong/fishisland/thirdparty/music`，删除此包及 yml 中的 `fishisland.third-party.music` 即可移除。不依赖数据库或聊天 WebSocket。

主配置 `application.yml` 不放音乐配置。`application-dev.yml`、`application-pre.yml`、`application-prod.yml` 使用以下占位配置，提交的文件不得包含真实地址或密钥：

```yaml
fishisland:
  third-party:
    music:
      api-url: ${MUSIC_API_URL:}
      token: ${MUSIC_API_TOKEN:}
```

启用对应的 Spring profile，并由服务端环境变量 `MUSIC_API_URL`、`MUSIC_API_TOKEN` 提供真实值；也可在不提交 Git 的外部 `application-{profile}.yml` 中覆盖这两项。现有 `application-pre.yml` 已被 Git 忽略，仍建议保留占位配置，实际值由部署环境管理。Java 配置类没有默认上游地址；地址或密钥为空时明确返回尚未配置，不以空搜索冒充成功。前端只读取自己的后端接口，不读取本配置。

已登录 GET `/api/third-party/music/search?source=netease|tencent&keyword=...&page=1` 每页最多 20 首，最多 5 页；GET `/api/third-party/music/url?source=...&id=...` 按需取播放链接。沿用 BaseResponse。来源与操作固定，不接收任意目标地址；Bearer 仅发送给配置的上游，禁跟随重定向，媒体 URL 只返回公开 HTTPS 地址，绝不带上游 Token。搜索缓存最多 200 项/30秒，最多 6 个并发上游请求；超载直接返回可重试错误。连接4秒、单次读取8秒、正文512KiB上限；不缓存播放签名链接、不下载或转码音频。

本功能是个人播放，不广播点歌、不改变聊天消息格式。前端采用单个原生音频实例和懒加载模块，关闭面板继续播放，退出账号释放。播放成功依赖上游凭证、歌曲权限、CDN 可达及浏览器媒体支持；部署后须实际点歌验证。

# 收藏表情包文字识别

已有库在部署新后端前执行 `sql/20261118_emoticon_favour_ocr.sql`。新建库的建表脚本已经包含新字段。OCR 原生库随应用打包，运行环境为 Windows/Linux x64；JVM 临时目录需要允许写入及加载原生库。

后端 Dockerfile 在构建时用 `patchelf --clear-execstack` 处理 Linux 原生库，并把它放在 RapidOCR 的默认加载位置。直接使用 JAR 在 Linux 部署时，取出 `BOOT-INF/lib/rapidocr-onnx-linux-x86_64-*.jar` 中的 `lib/libRapidOcr.so`，执行同一命令，再放到 JVM 的 `java.io.tmpdir` 下的 `ocrJava/onnx/libRapidOcr.so`。

1Panel 部署时将新打包的 `fish-island-backend-0.0.1-SNAPSHOT.jar` 和 `Dockerfile` 放到同一个构建目录，直接执行 `docker build -t fish .`。容器内工作目录为 `/thome/1panel/cong/fish`。从源码仓库构建时，通过 `--build-arg JAR_FILE=target/fish-island-backend-0.0.1-SNAPSHOT.jar` 使用 Maven 输出，CI 也使用该参数。

`POST /api/emoticon_favour/add` 仍接收原始 URL 字符串。收藏成功后，后台从已保存记录领取识别任务，正常情况下数秒内即可查询。暂时失败最多尝试三次。支持公开 HTTP(S) 图片，动图识别首帧。识别不到文字的收藏仍可在未筛选列表中使用。

`POST /api/emoticon_favour/list/page` 接收可选的 `keyword`。省略或传空白时返回当前账号的全部收藏；传值时按识别文字做字面包含匹配，最多 80 个 UTF-16 代码单元。返回记录包含 `ocrText`，旧收藏、无法读取的图片和等待识别的图片该字段为 null。识别任务只对部署后的新增收藏启用。

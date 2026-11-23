<!--@nrg.languages=cn,en-->
<!--@nrg.defaultLanguage=cn-->

<p align="right">
   <strong>中文</strong> | <a href="./README.en.md">English</a><!--cn-->
   <strong>English</strong> | <a href="./README.md">中文</a><!--en-->
</p>




<p align="center">
  <a href="https://github.com/lhccong/fish-island-backend"><img src="./doc/img/moyu.png" width="300" height="300" alt="摸鱼岛 logo"></a>
</p>



# 摸鱼岛<!--cn-->

_✨ 开源🌟一站式摸鱼网 ✨_<!--cn-->
# Fish Island<!--en-->
_✨ Open Source 🌟 One-Stop Procrastination Website ✨_<!--en-->

<p align="center">
  <a href="https://github.com/lhccong/fish-island-backend#部署">部署教程</a><!--cn-->
  <a href="https://github.com/lhccong/fish-island-backend#deployment">Deployment Guide</a><!--en-->
  ·
  <a href="https://github.com/lhccong/fish-island-backend#目前现状">目前现状</a><!--cn-->
  <a href="https://github.com/lhccong/fish-island-backend#current-status">Current Status</a><!--en-->
  ·
  <a href="https://fish.codebug.icu/rank/about">意见反馈</a><!--cn-->
  <a href="https://fish.codebug.icu/rank/about">Feedback</a><!--en-->
  ·
  <a href="https://github.com/lhccong/fish-island-backend#截图展示">截图展示</a><!--cn-->
  <a href="https://github.com/lhccong/fish-island-backend#screenshots">Screenshots</a><!--en-->
  ·
  <a href="https://fish.codebug.icu/index/">在线演示</a><!--cn-->
  <a href="https://fish.codebug.icu/index/">Live Demo</a><!--en-->
  ·
  <a href="https://github.com/lhccong/fish-island-backend#开源与贡献">开源与贡献</a><!--cn-->
  <a href="https://github.com/lhccong/fish-island-backend#open-source-and-contribution">Open Source & Contribution</a><!--en-->
  ·
  <a href="https://github.com/lhccong/fish-island-backend#相关项目">相关项目</a><!--cn-->
  <a href="https://github.com/lhccong/fish-island-backend#related-projects">Related Projects</a><!--en-->
  ·
  <a href="https://fish.codebug.icu/rank/reward">赞赏支持</a><!--cn-->
  <a href="https://fish.codebug.icu/rank/reward">Support</a><!--en-->
</p>

![image-20250426195022714](./doc/img/image-20250426195022714.png)

> [!NOTE]
> 本项目为开源项目，使用者必须在网站标注作者名称以及指向本项目的链接。如果不想保留署名，必须首先获得授权。不得用于非法用途。<!--cn-->
> This is an open-source project. Users must credit the author's name and link to this project on their website. Authorization is required if you wish to remove the attribution. Not to be used for illegal purposes.<!--en-->

> [!NOTE]
>
> 在线体验地址🔗<!--cn-->
> Live Demo Links 🔗<!--en-->
>
> 稳定版：https://yucoder.cn/<!--cn-->
> Latest Version (Domain expires 2025.09): https://fish.codebug.icu/<!--en-->
> Stable Version: https://yucoder.cn/<!--en-->
>
> 后端地址🌈：https://github.com/lhccong/fish-island-backend<!--cn-->
> Backend Repository 🌈: https://github.com/lhccong/fish-island-backend<!--en-->
>
> 前端地址🏖️：https://github.com/lhccong/fish-island-frontend<!--cn-->
> Frontend Repository 🏖️: https://github.com/lhccong/fish-island-frontend<!--en-->

> [!WARNING]
> 私部署时记得修改后端接口地址路径指向。<!--cn-->
> Remember to modify the backend API address path when deploying privately.<!--en-->

## 功能<!--cn-->
## Features<!--en-->

1. 支持多种数据源聚合：<!--cn-->
    + [✅] 知乎热榜<!--cn-->
    + [✅] 微博热榜<!--cn-->
    + [✅] 虎扑步行街热榜<!--cn-->
    + [✅] 编程导航热榜<!--cn-->
    + [✅] CSDN 热榜<!--cn-->
    + [✅] 掘金热榜<!--cn-->
    + [✅] B 站热门<!--cn-->
    + [✅] 抖音热搜<!--cn-->
    + [✅] 网易云热歌榜（支持网站点击播放）<!--cn-->
    + [✅] 什么值得买热榜<!--cn-->
    + [✅] 待补充...<!--cn-->
2. 每日待办功能。<!--cn-->
3. 摸鱼聊天室：<!--cn-->
    + [✅] 发送 emoji 表情包<!--cn-->
    + [✅] 发送搜狗在线表情包<!--cn-->
    + [✅] 支持网站链接解析<!--cn-->
    + [✅] 支持 markdown 文本解析<!--cn-->
    + [✅] 支持 AI 助手回答（接入硅基流动模型）<!--cn-->
    + [✅] 头像框功能<!--cn-->
    + [✅] 用户地理位置显示功能<!--cn-->
    + [✅] 用户称号功能<!--cn-->
    + [✅] 五子棋、象棋对战邀请功能<!--cn-->
    + [✅] 积分红包🧧发送功能<!--cn-->
    + [✅] 支持用户 CV 发送图片功能<!--cn-->
4. 摸鱼阅读：<!--cn-->
    + [✅] 在线搜书功能<!--cn-->
    + [✅] 小窗口观看功能<!--cn-->
    + [✅] 支持自定义书源<!--cn-->
5. 小游戏：<!--cn-->
    + [✅] 五子棋（人机/在线对战）<!--cn-->
    + [✅] 象棋（人机/在线对战）<!--cn-->
1. Support for Multiple Data Source Aggregation:<!--en-->
    + [✅] Zhihu Hot Topics<!--en-->
    + [✅] Weibo Hot Topics<!--en-->
    + [✅] Hupu Street Hot Topics<!--en-->
    + [✅] Programming Navigation Hot Topics<!--en-->
    + [✅] CSDN Hot Topics<!--en-->
    + [✅] Juejin Hot Topics<!--en-->
    + [✅] Bilibili Trending<!--en-->
    + [✅] Douyin Hot Search<!--en-->
    + [✅] NetEase Cloud Music Hot Songs (supports website playback)<!--en-->
    + [✅] Smzdm Hot Topics<!--en-->
    + [✅] More to come...<!--en-->
2. Daily Todo Feature<!--en-->
3. Chat Room:<!--en-->
    + [✅] Send emoji stickers<!--en-->
    + [✅] Send Sogou online stickers<!--en-->
    + [✅] Website link parsing support<!--en-->
    + [✅] Markdown text parsing support<!--en-->
    + [✅] AI assistant responses (integrated with Silicon-based Flow Model)<!--en-->
    + [✅] Avatar frame feature<!--en-->
    + [✅] User location display<!--en-->
    + [✅] User title feature<!--en-->
    + [✅] Gomoku and Chinese Chess game invitations<!--en-->
    + [✅] Points-based red packet 🧧 sending<!--en-->
    + [✅] User image upload support<!--en-->
4. Reading Section:<!--en-->
    + [✅] Online book search<!--en-->
    + [✅] Mini window viewing<!--en-->
    + [✅] Custom book source support<!--en-->
5. Mini Games:<!--en-->
    + [✅] Gomoku (AI/Online multiplayer)<!--en-->
    + [✅] Chinese Chess (AI/Online multiplayer)<!--en-->
    + [✅] 2048
6. 工具箱：<!--cn-->
    + [✅] JSON 格式化<!--cn-->
    + [✅] 文本比对<!--cn-->
    + [✅] 聚合翻译<!--cn-->
    + [✅] Git 提交格式生成<!--cn-->
    + [✅] AI 智能体<!--cn-->
    + [✅] AI 周报助手<!--cn-->
7. 头像框兑换功能。<!--cn-->
8. 其他：<!--cn-->
    + [✅] 音乐播放器<!--cn-->
    + [✅] 下班薪资计算器（放假倒计时）<!--cn-->
    + [✅] 修改网站图标<!--cn-->
    + [✅] 网站标题闪烁消息提醒<!--cn-->
    + [✅] 摸鱼初始页<!--cn-->
6. Toolbox:<!--en-->
    + [✅] JSON formatter<!--en-->
    + [✅] Text comparison<!--en-->
    + [✅] Aggregated translation<!--en-->
    + [✅] Git commit format generator<!--en-->
    + [✅] AI agent<!--en-->
    + [✅] AI weekly report assistant<!--en-->
7. Avatar Frame Exchange Feature<!--en-->
8. Others:<!--en-->
    + [✅] Music player<!--en-->
    + [✅] After-work salary calculator (holiday countdown)<!--en-->
    + [✅] Website icon customization<!--en-->
    + [✅] Website title flash message notifications<!--en-->
    + [✅] Initial landing page<!--en-->

## Star History

[![Star History Chart](https://api.star-history.com/svg?repos=lhccong/fish-island-backend&type=Date)](https://www.star-history.com/#lhccong/fish-island-backend&Date)

## Screenshots<!--en-->

## 截图展示<!--cn-->

### 信息聚合<!--cn-->
### Information Aggregation<!--en-->

<img src="./doc/img/image-20250426170535140.png" alt="image-20250426170535140" style="zoom:33%;" />

### 每日待办<!--cn-->
### Daily Todo<!--en-->

<img src="./doc/img/image-20250426170619142.png" alt="image-20250426170619142" style="zoom: 33%;" />

### 摸鱼室<!--cn-->
### Chat Room<!--en-->

<img src="./doc/img/image-20250426171114575.png" alt="image-20250426171114575" style="zoom:33%;" />

### 摸鱼阅读<!--cn-->
### Reading Section<!--en-->

<img src="./doc/img/image-20250426170827125.png" alt="image-20250426170827125" style="zoom:33%;" />

<img src="./doc/img/image-20250426170856799.png" alt="image-20250426170856799" style="zoom: 50%;" />

### 小游戏<!--cn-->
### Mini Games<!--en-->

- 五子棋<!--cn-->
- Gomoku<!--en-->

<img src="./doc/img/image-20250426171345531.png" alt="image-20250426171345531" style="zoom:33%;" />

- 象棋<!--cn-->
- Chinese Chess<!--en-->

<img src="./doc/img/image-20250426171248993.png" alt="image-20250426171248993" style="zoom:33%;" />

- 2048

<img src="./doc/img/image-20250426171310675.png" alt="image-20250426171310675" style="zoom: 50%;" />

### 工具箱<!--cn-->
### Toolbox<!--en-->

- JSON 格式化工具<!--cn-->
- JSON Formatter<!--en-->

<img src="./doc/img/image-20250426171413448.png" alt="image-20250426171413448" style="zoom:25%;" />

- 文本比对<!--cn-->
- Text Comparison<!--en-->

<img src="./doc/img/image-20250426171435468.png" alt="image-20250426171435468" style="zoom:25%;" />

### 头像框兑换<!--cn-->
### Avatar Frame Exchange<!--en-->

<img src="./doc/img/image-20250426171832381.png" alt="image-20250426171832381" style="zoom: 33%;" />

## 目前现状<!--cn-->
## Current Status<!--en-->

- 各大公众号转发。<!--cn-->
- Shared across major WeChat official accounts<!--en-->

  <img src="./doc/img/image-wchat.png" alt="wchat" style="zoom: 50%;" />

- Personal website with over 1k users<!--en-->


- 用户突破 1k 的个人网站。<!--cn-->

- 最高峰实时在线人数达 80 +。<!--cn-->
- Peak concurrent online users reaching 80+<!--en-->

  <img src="./doc/img/image-20250426165418718.png" alt="image-20250426165418718" width="20%" style="zoom:25%;" >

## 部署教程<!--cn-->
## Deployment Guide<!--en-->

### 后端<!--cn-->
### Backend<!--en-->

- 执行初始化 SQL  [create_table.sql](./sql/create_table.sql)<!--cn-->
- Execute initialization SQL [create_table.sql](./sql/create_table.sql)<!--en-->

- 更改 MySQL 地址、Redis 地址、Minio 地址、邮箱发送配置<!--cn-->
- Update MySQL address, Redis address, Minio address, and email sending configuration<!--en-->

- Maven 打包<!--cn-->
- Maven packaging<!--en-->

- docker 部署<!--cn-->
- Docker deployment<!--en-->

- dockerfile 文件<!--cn-->
- Dockerfile<!--en-->

  ```dockerfile
  FROM openjdk:8
  ENV workdir=/cong/fish
  COPY . ${workdir}
  WORKDIR ${workdir}
  EXPOSE 8123
  CMD ["java","-jar","-Duser.timezone=GMT+08","fish-island-backend-0.0.1-SNAPSHOT.jar"]
  ```

- 打包命令<!--cn-->
- Build command<!--en-->

  ```shell
  docker build -f ./dockerfile -t fish .
  
  启动命令：docker run -d -e TZ=CST -p 8123:8123 -p 8090:8090 --name "fish" fish:latest<!--cn-->
  Start command: docker run -d -e TZ=CST -p 8123:8123 -p 8090:8090 --name "fish" fish:latest<!--en-->
  ```

- nginx 配置<!--cn-->
- Nginx configuration<!--en-->

  ```nginx
  server {
      listen       80;
      listen  [::]:80;
      server_name  moyuapi.codebug.icu;
  
      rewrite ^(.*) https://$server_name$1 permanent;
  }
  
  server {
      listen       443 ssl;
      server_name  moyuapi.codebug.icu;
  
      ssl_certificate      /etc/nginx/ssl/cert.pem;
      ssl_certificate_key  /etc/nginx/ssl/key.pem;
      ssl_session_cache    shared:SSL:1m;
      ssl_session_timeout  5m;
  
      ssl_ciphers  HIGH:!aNULL:!MD5;
      ssl_prefer_server_ciphers  on;
  
      location / {
           root /usr/share/nginx/fish;
           index index.html;
  					try_files $uri $uri/ /index.html;<!--cn-->
           try_files $uri $uri/ /index.html;<!--en-->
      }
  

     location /fish/ {<!--cn-->
      location /fish/ {<!--en-->
           proxy_pass http://fish:8123/;    
      }
  
  # WebSocket代理配置，处理 wss:// 请求<!--cn-->
      # WebSocket proxy configuration for wss:// requests<!--en-->
      location /ws/ {
          proxy_pass http://fish:8090/;  # 后端 WebSocket 服务地址<!--cn-->
          proxy_http_version 1.1;  # 使用 HTTP/1.1 协议，WebSocket 需要这个版本<!--cn-->
          proxy_set_header Upgrade $http_upgrade;  # 必须设置这些头来支持 WebSocket 协议的升级<!--cn-->
          proxy_set_header Connection 'upgrade';  # 维持 WebSocket 连接<!--cn-->
          proxy_set_header Host $host;  # 确保 Host 头部传递正确<!--cn-->
          proxy_cache_bypass $http_upgrade;  # 禁用缓存<!--cn-->
          proxy_pass http://fish:8090/;  # Backend WebSocket service address<!--en-->
          proxy_http_version 1.1;  # Using HTTP/1.1 protocol, required for WebSocket<!--en-->
          proxy_set_header Upgrade $http_upgrade;  # Required headers for WebSocket protocol upgrade<!--en-->
          proxy_set_header Connection 'upgrade';  # Maintain WebSocket connection<!--en-->
          proxy_set_header Host $host;  # Ensure correct Host header<!--en-->
          proxy_cache_bypass $http_upgrade;  # Disable cache<!--en-->
      }
  
  location /sogou-api/ {<!--cn-->
      location /sogou-api/ {<!--en-->
          proxy_pass https://pic.sogou.com/;
          proxy_set_header Host pic.sogou.com;
          proxy_set_header X-Real-IP $remote_addr;
          proxy_set_header X-Forwarded-For $proxy_add_x_forwarded_for;
          proxy_ssl_server_name on;
  
          # 解决 CORS 问题<!--cn-->
          # Resolve CORS issues<!--en-->
          add_header Access-Control-Allow-Origin *;
          add_header Access-Control-Allow-Methods "GET, POST, OPTIONS";
          add_header Access-Control-Allow-Headers "DNT,User-Agent,X-Requested-With,If-Modified-Since,Cache-Control,Content-Type,Range";
          add_header Access-Control-Expose-Headers "Content-Length,Content-Range";
  
          # 处理 OPTIONS 预检请求<!--cn-->
          # Handle OPTIONS preflight requests<!--en-->
          if ($request_method = OPTIONS) {
              return 204;
          }
      }
  
  location /holiday/ {<!--cn-->
      proxy_pass https://date.appworlds.cn/;<!--cn-->

      # 保持目标 API 的 Host，避免返回默认网页<!--cn-->
      proxy_set_header Host date.appworlds.cn;<!--cn-->
      location /holiday/ {<!--en-->
          proxy_pass https://date.appworlds.cn/;<!--en-->
          # Maintain target API's Host to avoid default webpage return<!--en-->
          proxy_set_header Host date.appworlds.cn;<!--en-->
  
      # 伪装成浏览器，防止服务器根据 User-Agent 返回 HTML<!--cn-->
      proxy_set_header User-Agent "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Safari/537.36";<!--cn-->
          # Disguise as browser to prevent HTML return based on User-Agent<!--en-->
          proxy_set_header User-Agent "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Safari/537.36";<!--en-->
  
      # 强制服务器返回 JSON，而不是 HTML<!--cn-->
      proxy_set_header Accept "application/json";<!--cn-->
          # Force server to return JSON instead of HTML<!--en-->
          proxy_set_header Accept "application/json";<!--en-->
  
      # CORS 允许跨域<!--cn-->
      add_header Access-Control-Allow-Origin *;<!--cn-->
      add_header Access-Control-Allow-Methods "GET, POST, OPTIONS";<!--cn-->
      add_header Access-Control-Allow-Headers "DNT,User-Agent,X-Requested-With,If-Modified-Since,Cache-Control,Content-Type,Range";<!--cn-->
      add_header Access-Control-Expose-Headers "Content-Length,Content-Range";<!--cn-->
          # CORS allow cross-origin<!--en-->
          add_header Access-Control-Allow-Origin *;<!--en-->
          add_header Access-Control-Allow-Methods "GET, POST, OPTIONS";<!--en-->
          add_header Access-Control-Allow-Headers "DNT,User-Agent,X-Requested-With,If-Modified-Since,Cache-Control,Content-Type,Range";<!--en-->
          add_header Access-Control-Expose-Headers "Content-Length,Content-Range";<!--en-->
  
      # 处理 OPTIONS 预检请求<!--cn-->
      if ($request_method = OPTIONS) {<!--cn-->
          return 204;<!--cn-->
          # Handle OPTIONS preflight requests<!--en-->
          if ($request_method = OPTIONS) {<!--en-->
              return 204;<!--en-->
          }<!--en-->
      }
  }<!--cn-->
  

  location /img-api/ {<!--cn-->
      location /img-api/ {<!--en-->
          proxy_pass https://i.111666.best/;
          proxy_set_header Host pic.sogou.com;
          proxy_set_header X-Real-IP $remote_addr;
          proxy_set_header X-Forwarded-For $proxy_add_x_forwarded_for;
          proxy_ssl_server_name on;
  
          # 解决 CORS 问题<!--cn-->
          # Resolve CORS issues<!--en-->
          add_header Access-Control-Allow-Origin *;
          add_header Access-Control-Allow-Methods "GET, POST, OPTIONS";
          add_header Access-Control-Allow-Headers "DNT,User-Agent,X-Requested-With,If-Modified-Since,Cache-Control,Content-Type,Range";
          add_header Access-Control-Expose-Headers "Content-Length,Content-Range";
  
          # 处理 OPTIONS 预检请求<!--cn-->
          # Handle OPTIONS preflight requests<!--en-->
          if ($request_method = OPTIONS) {
              return 204;
          }
      }
  
      error_page   500 502 503 504  /50x.html;
      location = /50x.html {
          root   /usr/share/nginx/html;
      }
  }
  ```

### 前端<!--cn-->
### Frontend<!--en-->

- 修改 src/constants/index.ts 的接口地址。<!--cn-->
- max build  --打包命令。<!--cn-->
- 部署 dist 文件。<!--cn-->
- Modify the API address in src/constants/index.ts<!--en-->
- Run `max build` command for packaging<!--en-->
- Deploy the dist files<!--en-->

## 开源与贡献<!--cn-->
## Open Source and Contribution<!--en-->

### 项目支持者 🔥<!--cn-->
### Project Supporters 🔥<!--en-->

<img src="./doc/img/image-20250426171939913.png" alt="image-20250426171939913" style="zoom: 33%;" />

### 前端贡献者 🌟<!--cn-->
### Frontend Contributors 🌟<!--en-->

<a href="https://github.com/lhccong/fish-island-frontend/graphs/contributors">
<img src="https://contrib.rocks/image?repo=lhccong/fish-island-frontend" />
</a>

### 后端贡献者 🌟:<!--cn-->
### Backend Contributors 🌟:<!--en-->

<a href="https://github.com/lhccong/fish-island-backend/graphs/contributors">
  <img src="https://contrib.rocks/image?repo=lhccong/fish-island-backend" />
</a>  

### 📌 贡献方式<!--cn-->
### 📌 How to Contribute<!--en-->

如果你也有希望聚合的数据源不妨来参加一下贡献，将你的数据源爬取出来放入其中。<!--cn-->
If you have data sources you'd like to aggregate, feel free to contribute by implementing your data source crawler.<!--en-->

1️⃣ 页面元素抓取<!--cn-->
1️⃣ Page Element Scraping<!--en-->

📌 **适用于**：目标网站未提供 API，数据嵌入在 HTML 结构中。<!--cn-->
📌 **Suitable for**: Target websites without APIs, where data is embedded in HTML structure.<!--en-->

✅ 贡献要求<!--cn-->
✅ Contribution Requirements<!--en-->

- **推荐使用**：<!--cn-->
    - `Jsoup`（Java）<!--cn-->
    - `BeautifulSoup`（Python）<!--cn-->
    - `Cheerio`（Node.js）<!--cn-->
- **选择器精准**：避免因页面结构变化导致抓取失败。<!--cn-->
- **减少 HTTP 请求**：优化抓取效率，避免重复请求。<!--cn-->
- **遵守网站爬取规则**（`robots.txt` ）。<!--cn-->
- **Recommended Tools**:<!--en-->
    - `Jsoup` (Java)<!--en-->
    - `BeautifulSoup` (Python)<!--en-->
    - `Cheerio` (Node.js)<!--en-->
- **Precise Selectors**: Avoid scraping failures due to page structure changes<!--en-->
- **Minimize HTTP Requests**: Optimize scraping efficiency, avoid duplicate requests<!--en-->
- **Follow Website Crawling Rules** (`robots.txt`)<!--en-->

💡 示例代码<!--cn-->
💡 Example Code<!--en-->

```java
Document doc = Jsoup.connect("https://example.com").get();
String title = doc.select("h1.article-title").text();
```

2️⃣ 页面接口返回数据抓取<!--cn-->
2️⃣ API Response Data Scraping<!--en-->

📌 **适用于**：目标网站提供 API，可直接调用接口获取 JSON/XML 数据。<!--cn-->
📌 **Suitable for**: Target websites with APIs that return JSON/XML data.<!--en-->

✅ 贡献要求<!--cn-->
✅ Contribution Requirements<!--en-->

- **推荐使用**：<!--cn-->
    - `HttpClient`（Java）<!--cn-->
    - `axios`（Node.js）<!--cn-->
    - `requests`（Python）<!--cn-->
- **分析 API 请求**：确保请求参数完整（`headers`、`cookies`、`token`）。<!--cn-->
- **减少不必要的请求**：优化调用频率，避免触发反爬机制。<!--cn-->
- **异常处理**：确保代码稳定运行。<!--cn-->
- **Recommended Tools**:<!--en-->
    - `HttpClient` (Java)<!--en-->
    - `axios` (Node.js)<!--en-->
    - `requests` (Python)<!--en-->
- **Analyze API Requests**: Ensure complete request parameters (`headers`, `cookies`, `token`)<!--en-->
- **Minimize Unnecessary Requests**: Optimize call frequency, avoid triggering anti-crawling mechanisms<!--en-->
- **Error Handling**: Ensure code stability<!--en-->

💡 示例代码<!--cn-->
💡 Example Code<!--en-->

```java
String apiUrl = "https://api.example.com/data";
String response = HttpRequest.get(apiUrl).execute().body();
JSONObject json = JSON.parseObject(response);
```

---

### 🔗 数据源注册<!--cn-->
### 🔗 Data Source Registration<!--en-->

数据抓取完成后，需要注册数据源，以便系统能够正确使用。<!--cn-->
After completing data scraping, register the data source for system use.<!--en-->

🚀 注册流程<!--cn-->
🚀 Registration Process<!--en-->

1. **添加数据源 Key**：<!--cn-->
   `/src/main/java/com/cong/fishisland/model/enums/HotDataKeyEnum.java` 定义新的数据源 Key。<!--cn-->
1. **Add Data Source Key**:<!--en-->
   Define new data source key in `/src/main/java/com/cong/fishisland/model/enums/HotDataKeyEnum.java`<!--en-->

2. **更新数据源映射**：<!--cn-->
2. **Update Data Source Mapping**:<!--en-->
    - Add new data source configuration in `/src/main/java/com/lhccong/fish/backend/config/DatabaseConfig.java`<!--en-->

    -  `/src/main/java/com/lhccong/fish/backend/config/DatabaseConfig.java` 文件中，添加新的数据源配置。<!--cn-->
3. **Create Data Source Class**:<!--en-->
    - Create new data source class in `src/main/java/com/cong/fishisland/datasource`, extend `DataSource`, implement `getHotPost` method<!--en-->

3. **创建数据源类**：<!--cn-->
4. **Implement Data Retrieval Logic**:<!--en-->
    - Return data in `HotPostDataVO` format<!--en-->
    - Use `@Builder` annotation for correct data parsing<!--en-->

    -  `src/main/java/com/cong/fishisland/datasource` 目录下，新建数据源类，继承 `DataSource`，实现 `getHotPost` 方法。<!--cn-->

4. **实现数据获取逻辑**：<!--cn-->

    - 按照 `HotPostDataVO` 格式返回数据。<!--cn-->
    - 使用 `@Builder` 注解，确保数据能正确解析。<!--cn-->

💡 示例代码<!--cn-->
💡 Example Code<!--en-->

```java
HotPostDataVO.builder()
            .title(title)
            .url(url)
            .followerCount(followerCount)
            .excerpt(excerpt)
            .build();
```

---

### 🚀 贡献流程<!--cn-->
### 🚀 Contribution Process<!--en-->

1. **Fork 仓库** ➜ 点击 GitHub 右上角 `Fork` 按钮。<!--cn-->
2. **创建分支** ➜ 推荐使用有意义的分支名，如 `feature/data-scraper-optimization`。<!--cn-->
3. **提交代码** ➜ 确保代码可读性高，符合规范。<!--cn-->
4. **提交 Pull Request（PR）** ➜ 详细描述您的更改内容，并关联相关 issue（如有）。<!--cn-->
5. **等待审核** ➜ 维护者会进行代码审核并合并。<!--cn-->
1. **Fork Repository** ➜ Click `Fork` button in top right of GitHub<!--en-->
2. **Create Branch** ➜ Use meaningful branch names, e.g., `feature/data-scraper-optimization`<!--en-->
3. **Submit Code** ➜ Ensure code readability and follows standards<!--en-->
4. **Submit Pull Request (PR)** ➜ Describe your changes in detail and link related issues (if any)<!--en-->
5. **Wait for Review** ➜ Maintainers will review and merge code<!--en-->

以上讲解如果对你有帮助，不妨给我的项目点个小小的 star 🌟，成为一下我的精神股东呢<!--cn-->
If this guide helps you, consider giving our project a star 🌟 and becoming our spiritual shareholder!<!--en-->

### 🎉 感谢您的贡献！<!--cn-->
### 🎉 Thank You for Contributing!<!--en-->

您的每一份贡献都让 **fish-island** 变得更好！💪<!--cn-->
Every contribution makes **Fish Island** better! 💪<!--en-->




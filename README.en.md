<p align="right">
   <strong>English</strong> | <a href="./README.md">中文</a>
</p>




<p align="center">
  <a href="https://github.com/lhccong/fish-island-backend"><img src="./doc/img/moyu.png" width="300" height="300" alt="摸鱼岛 logo"></a>
</p>




# Fish Island

_✨ Open Source 🌟 One-Stop Procrastination Website ✨_

<p align="center">
  <a href="https://github.com/lhccong/fish-island-backend#deployment">Deployment Guide</a>
  ·
  <a href="https://github.com/lhccong/fish-island-backend#current-status">Current Status</a>
  ·
  <a href="https://fish.codebug.icu/rank/about">Feedback</a>
  ·
  <a href="https://github.com/lhccong/fish-island-backend#screenshots">Screenshots</a>
  ·
  <a href="https://fish.codebug.icu/index/">Live Demo</a>
  ·
  <a href="https://github.com/lhccong/fish-island-backend#open-source-and-contribution">Open Source & Contribution</a>
  ·
  <a href="https://github.com/lhccong/fish-island-backend#related-projects">Related Projects</a>
  ·
  <a href="https://fish.codebug.icu/rank/reward">Support</a>
</p>

![image-20250426195022714](./doc/img/image-20250426195022714.png)

> [!NOTE]
> This is an open-source project. Users must credit the author's name and link to this project on their website. Authorization is required if you wish to remove the attribution. Not to be used for illegal purposes.

> [!NOTE]
>
> Live Demo Links 🔗
>
> Latest Version (Domain expires 2025.09): https://fish.codebug.icu/
> Stable Version: https://yucoder.cn/
>
> Backend Repository 🌈: https://github.com/lhccong/fish-island-backend
>
> Frontend Repository 🏖️: https://github.com/lhccong/fish-island-frontend

> [!WARNING]
> Remember to modify the backend API address path when deploying privately.

## Features

1. Support for Multiple Data Source Aggregation:
    + [✅] Zhihu Hot Topics
    + [✅] Weibo Hot Topics
    + [✅] Hupu Street Hot Topics
    + [✅] Programming Navigation Hot Topics
    + [✅] CSDN Hot Topics
    + [✅] Juejin Hot Topics
    + [✅] Bilibili Trending
    + [✅] Douyin Hot Search
    + [✅] NetEase Cloud Music Hot Songs (supports website playback)
    + [✅] Smzdm Hot Topics
    + [✅] More to come...
2. Daily Todo Feature
3. Chat Room:
    + [✅] Send emoji stickers
    + [✅] Send Sogou online stickers
    + [✅] Website link parsing support
    + [✅] Markdown text parsing support
    + [✅] AI assistant responses (integrated with Silicon-based Flow Model)
    + [✅] Avatar frame feature
    + [✅] User location display
    + [✅] User title feature
    + [✅] Gomoku and Chinese Chess game invitations
    + [✅] Points-based red packet 🧧 sending
    + [✅] User image upload support
4. Reading Section:
    + [✅] Online book search
    + [✅] Mini window viewing
    + [✅] Custom book source support
5. Mini Games:
    + [✅] Gomoku (AI/Online multiplayer)
    + [✅] Chinese Chess (AI/Online multiplayer)
    + [✅] 2048
6. Toolbox:
    + [✅] JSON formatter
    + [✅] Text comparison
    + [✅] Aggregated translation
    + [✅] Git commit format generator
    + [✅] AI agent
    + [✅] AI weekly report assistant
7. Avatar Frame Exchange Feature
8. Others:
    + [✅] Music player
    + [✅] After-work salary calculator (holiday countdown)
    + [✅] Website icon customization
    + [✅] Website title flash message notifications
    + [✅] Initial landing page

## Star History

[![Star History Chart](https://api.star-history.com/svg?repos=lhccong/fish-island-backend&type=Date)](https://www.star-history.com/#lhccong/fish-island-backend&Date)

## Screenshots

### Information Aggregation

<img src="./doc/img/image-20250426170535140.png" alt="image-20250426170535140" style="zoom:33%;" />

### Daily Todo

<img src="./doc/img/image-20250426170619142.png" alt="image-20250426170619142" style="zoom: 33%;" />

### Chat Room

<img src="./doc/img/image-20250426171114575.png" alt="image-20250426171114575" style="zoom:33%;" />

### Reading Section

<img src="./doc/img/image-20250426170827125.png" alt="image-20250426170827125" style="zoom:33%;" />

<img src="./doc/img/image-20250426170856799.png" alt="image-20250426170856799" style="zoom: 50%;" />

### Mini Games

- Gomoku

<img src="./doc/img/image-20250426171345531.png" alt="image-20250426171345531" style="zoom:33%;" />

- Chinese Chess

<img src="./doc/img/image-20250426171248993.png" alt="image-20250426171248993" style="zoom:33%;" />

- 2048

<img src="./doc/img/image-20250426171310675.png" alt="image-20250426171310675" style="zoom: 50%;" />

### Toolbox

- JSON Formatter

<img src="./doc/img/image-20250426171413448.png" alt="image-20250426171413448" style="zoom:25%;" />

- Text Comparison

<img src="./doc/img/image-20250426171435468.png" alt="image-20250426171435468" style="zoom:25%;" />

### Avatar Frame Exchange

<img src="./doc/img/image-20250426171832381.png" alt="image-20250426171832381" style="zoom: 33%;" />

## Current Status

- Shared across major WeChat official accounts

  <img src="./doc/img/image-wchat.png" alt="wchat" style="zoom: 50%;" />

- Personal website with over 1k users

- Peak concurrent online users reaching 80+

  <img src="./doc/img/image-20250426165418718.png" alt="image-20250426165418718" width="20%" style="zoom:25%;" >

## Deployment Guide

### Backend

- Execute initialization SQL [create_table.sql](./sql/create_table.sql)

- Update MySQL address, Redis address, Minio address, and email sending configuration

- Maven packaging

- Docker deployment

- Dockerfile

  ```dockerfile
  FROM openjdk:8
  ENV workdir=/cong/fish
  COPY . ${workdir}
  WORKDIR ${workdir}
  EXPOSE 8123
  CMD ["java","-jar","-Duser.timezone=GMT+08","fish-island-backend-0.0.1-SNAPSHOT.jar"]
  ```

- Build command

  ```shell
  docker build -f ./dockerfile -t fish .
  
  Start command: docker run -d -e TZ=CST -p 8123:8123 -p 8090:8090 --name "fish" fish:latest
  ```

- Nginx configuration

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
           try_files $uri $uri/ /index.html;
      }
  
      location /fish/ {
           proxy_pass http://fish:8123/;    
      }
  
      # WebSocket proxy configuration for wss:// requests
      location /ws/ {
          proxy_pass http://fish:8090/;  # Backend WebSocket service address
          proxy_http_version 1.1;  # Using HTTP/1.1 protocol, required for WebSocket
          proxy_set_header Upgrade $http_upgrade;  # Required headers for WebSocket protocol upgrade
          proxy_set_header Connection 'upgrade';  # Maintain WebSocket connection
          proxy_set_header Host $host;  # Ensure correct Host header
          proxy_cache_bypass $http_upgrade;  # Disable cache
      }
  
      location /sogou-api/ {
          proxy_pass https://pic.sogou.com/;
          proxy_set_header Host pic.sogou.com;
          proxy_set_header X-Real-IP $remote_addr;
          proxy_set_header X-Forwarded-For $proxy_add_x_forwarded_for;
          proxy_ssl_server_name on;
  
          # Resolve CORS issues
          add_header Access-Control-Allow-Origin *;
          add_header Access-Control-Allow-Methods "GET, POST, OPTIONS";
          add_header Access-Control-Allow-Headers "DNT,User-Agent,X-Requested-With,If-Modified-Since,Cache-Control,Content-Type,Range";
          add_header Access-Control-Expose-Headers "Content-Length,Content-Range";
  
          # Handle OPTIONS preflight requests
          if ($request_method = OPTIONS) {
              return 204;
          }
      }
  
      location /holiday/ {
          proxy_pass https://date.appworlds.cn/;
          
          # Maintain target API's Host to avoid default webpage return
          proxy_set_header Host date.appworlds.cn;
  
          # Disguise as browser to prevent HTML return based on User-Agent
          proxy_set_header User-Agent "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Safari/537.36";
  
          # Force server to return JSON instead of HTML
          proxy_set_header Accept "application/json";
  
          # CORS allow cross-origin
          add_header Access-Control-Allow-Origin *;
          add_header Access-Control-Allow-Methods "GET, POST, OPTIONS";
          add_header Access-Control-Allow-Headers "DNT,User-Agent,X-Requested-With,If-Modified-Since,Cache-Control,Content-Type,Range";
          add_header Access-Control-Expose-Headers "Content-Length,Content-Range";
  
          # Handle OPTIONS preflight requests
          if ($request_method = OPTIONS) {
              return 204;
          }
      }
  
      location /img-api/ {
          proxy_pass https://i.111666.best/;
          proxy_set_header Host pic.sogou.com;
          proxy_set_header X-Real-IP $remote_addr;
          proxy_set_header X-Forwarded-For $proxy_add_x_forwarded_for;
          proxy_ssl_server_name on;
  
          # Resolve CORS issues
          add_header Access-Control-Allow-Origin *;
          add_header Access-Control-Allow-Methods "GET, POST, OPTIONS";
          add_header Access-Control-Allow-Headers "DNT,User-Agent,X-Requested-With,If-Modified-Since,Cache-Control,Content-Type,Range";
          add_header Access-Control-Expose-Headers "Content-Length,Content-Range";
  
          # Handle OPTIONS preflight requests
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

### Frontend

- Modify the API address in src/constants/index.ts
- Run `max build` command for packaging
- Deploy the dist files

## Open Source and Contribution

### Project Supporters 🔥

<img src="./doc/img/image-20250426171939913.png" alt="image-20250426171939913" style="zoom: 33%;" />

### Frontend Contributors 🌟

<a href="https://github.com/lhccong/fish-island-frontend/graphs/contributors">
<img src="https://contrib.rocks/image?repo=lhccong/fish-island-frontend" />
</a>

### Backend Contributors 🌟:

<a href="https://github.com/lhccong/fish-island-backend/graphs/contributors">
  <img src="https://contrib.rocks/image?repo=lhccong/fish-island-backend" />
</a>  

### 📌 How to Contribute

If you have data sources you'd like to aggregate, feel free to contribute by implementing your data source crawler.

1️⃣ Page Element Scraping

📌 **Suitable for**: Target websites without APIs, where data is embedded in HTML structure.

✅ Contribution Requirements

- **Recommended Tools**:
    - `Jsoup` (Java)
    - `BeautifulSoup` (Python)
    - `Cheerio` (Node.js)
- **Precise Selectors**: Avoid scraping failures due to page structure changes
- **Minimize HTTP Requests**: Optimize scraping efficiency, avoid duplicate requests
- **Follow Website Crawling Rules** (`robots.txt`)

💡 Example Code

```java
Document doc = Jsoup.connect("https://example.com").get();
String title = doc.select("h1.article-title").text();
```

2️⃣ API Response Data Scraping

📌 **Suitable for**: Target websites with APIs that return JSON/XML data.

✅ Contribution Requirements

- **Recommended Tools**:
    - `HttpClient` (Java)
    - `axios` (Node.js)
    - `requests` (Python)
- **Analyze API Requests**: Ensure complete request parameters (`headers`, `cookies`, `token`)
- **Minimize Unnecessary Requests**: Optimize call frequency, avoid triggering anti-crawling mechanisms
- **Error Handling**: Ensure code stability

💡 Example Code

```java
String apiUrl = "https://api.example.com/data";
String response = HttpRequest.get(apiUrl).execute().body();
JSONObject json = JSON.parseObject(response);
```

---

### 🔗 Data Source Registration

After completing data scraping, register the data source for system use.

🚀 Registration Process

1. **Add Data Source Key**:
   Define new data source key in `/src/main/java/com/cong/fishisland/model/enums/HotDataKeyEnum.java`

2. **Update Data Source Mapping**:
    - Add new data source configuration in `/src/main/java/com/lhccong/fish/backend/config/DatabaseConfig.java`

3. **Create Data Source Class**:
    - Create new data source class in `src/main/java/com/cong/fishisland/datasource`, extend `DataSource`, implement `getHotPost` method

4. **Implement Data Retrieval Logic**:
    - Return data in `HotPostDataVO` format
    - Use `@Builder` annotation for correct data parsing

💡 Example Code

```java
HotPostDataVO.builder()
            .title(title)
            .url(url)
            .followerCount(followerCount)
            .excerpt(excerpt)
            .build();
```

---

### 🚀 Contribution Process

1. **Fork Repository** ➜ Click `Fork` button in top right of GitHub
2. **Create Branch** ➜ Use meaningful branch names, e.g., `feature/data-scraper-optimization`
3. **Submit Code** ➜ Ensure code readability and follows standards
4. **Submit Pull Request (PR)** ➜ Describe your changes in detail and link related issues (if any)
5. **Wait for Review** ➜ Maintainers will review and merge code

If this guide helps you, consider giving our project a star 🌟 and becoming our spiritual shareholder!

### 🎉 Thank You for Contributing!

Every contribution makes **Fish Island** better! 💪




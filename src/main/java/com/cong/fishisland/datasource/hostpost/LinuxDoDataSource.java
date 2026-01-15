package com.cong.fishisland.datasource.hostpost;

import cn.hutool.http.HttpRequest;
import cn.hutool.http.HttpResponse;
import com.alibaba.fastjson.JSON;
import com.cong.fishisland.model.entity.hot.HotPost;
import com.cong.fishisland.model.enums.CategoryTypeEnum;
import com.cong.fishisland.model.enums.UpdateIntervalEnum;
import com.cong.fishisland.model.vo.hot.HotPostDataVO;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import java.io.ByteArrayInputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * LinuxDo 热榜数据源
 * <p>
 * 数据来源：LinuxDo官方RSS订阅
 * 更新频率：每30分钟
 * 分类：技术 & 编程
 *
 * @author JackyST0
 * @date 2026/01/15
 */
@Slf4j
@Component
public class LinuxDoDataSource implements DataSource {
    
    /**
     * LinuxDo 官方RSS地址
     */
    private static final String RSS_URL = "https://linux.do/latest.rss";
    
    /**
     * 请求超时时间（毫秒）
     */
    private static final int TIMEOUT = 20000;
    
    /**
     * 最大返回数量
     */
    private static final int MAX_SIZE = 20;
    
    @Override
    public HotPost getHotPost() {
        List<HotPostDataVO> dataList = new ArrayList<>();
        
        try {
            // 发送HTTP请求获取RSS数据
            HttpResponse response = HttpRequest.get(RSS_URL)
                    .header("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Safari/537.36")
                    .timeout(TIMEOUT)
                    .execute();
            
            // 检查HTTP状态码
            if (!response.isOk()) {
                log.warn("LinuxDo RSS返回异常状态码: {}", response.getStatus());
                return buildEmptyHotPost();
            }
            
            String rssContent = response.body();
            
            // 解析RSS XML
            DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
            DocumentBuilder builder = factory.newDocumentBuilder();
            Document doc = builder.parse(new ByteArrayInputStream(rssContent.getBytes(StandardCharsets.UTF_8)));
            
            // 获取所有item节点
            NodeList items = doc.getElementsByTagName("item");
            
            // 转换为标准格式
            for (int i = 0; i < Math.min(items.getLength(), MAX_SIZE); i++) {
                try {
                    Element item = (Element) items.item(i);
                    
                    // 提取数据
                    String title = getElementText(item, "title");
                    String link = getElementText(item, "link");
                    String description = getElementText(item, "description");
                    
                    // 从description中提取参与人数（格式：X 个帖子 - X 位参与者）
                    int followerCount = extractParticipantCount(description);
                    
                    // 数据校验
                    if (title == null || link == null) {
                        log.warn("LinuxDo数据项缺少必要字段");
                        continue;
                    }
                    
                    dataList.add(HotPostDataVO.builder()
                            .title(title)
                            .url(link)
                            .followerCount(followerCount)
                            .build());
                            
                } catch (Exception e) {
                    log.error("解析LinuxDo数据项失败", e);
                }
            }
            
            log.info("成功获取LinuxDo热榜数据，共{}条", dataList.size());
            
        } catch (Exception e) {
            log.error("获取LinuxDo热榜失败", e);
            return buildEmptyHotPost();
        }
        
        return buildHotPost(dataList);
    }
    
    /**
     * 获取XML元素的文本内容
     *
     * @param element 父元素
     * @param tagName 标签名
     * @return 文本内容
     */
    private String getElementText(Element element, String tagName) {
        NodeList nodeList = element.getElementsByTagName(tagName);
        if (nodeList.getLength() > 0) {
            return nodeList.item(0).getTextContent();
        }
        return null;
    }
    
    /**
     * 从description中提取参与人数
     * 格式示例：<small>20 个帖子 - 14 位参与者</small>
     *
     * @param description 描述文本
     * @return 参与人数
     */
    private int extractParticipantCount(String description) {
        if (description == null) {
            return 0;
        }
        
        try {
            // 匹配 "X 位参与者" 的模式
            Pattern pattern = Pattern.compile("(\\d+)\\s*位参与者");
            Matcher matcher = pattern.matcher(description);
            
            if (matcher.find()) {
                return Integer.parseInt(matcher.group(1));
            }
            
            // 如果没找到参与者，尝试匹配回复数 "X 个帖子"
            pattern = Pattern.compile("(\\d+)\\s*个帖子");
            matcher = pattern.matcher(description);
            
            if (matcher.find()) {
                return Integer.parseInt(matcher.group(1));
            }
        } catch (Exception e) {
            log.debug("提取LinuxDo参与人数失败", e);
        }
        
        return 0;
    }
    
    /**
     * 构建热榜对象
     *
     * @param dataList 数据列表
     * @return HotPost对象
     */
    private HotPost buildHotPost(List<HotPostDataVO> dataList) {
        return HotPost.builder()
                .sort(CategoryTypeEnum.TECH_PROGRAMMING.getValue())
                .category(CategoryTypeEnum.TECH_PROGRAMMING.getValue())
                .name("LinuxDo热榜")
                .updateInterval(UpdateIntervalEnum.HALF_HOUR.getValue())
                .iconUrl("🐧")
                .hostJson(JSON.toJSONString(dataList))
                .typeName("LinuxDo")
                .build();
    }
    
    /**
     * 构建空数据的热榜对象（当抓取失败时返回）
     *
     * @return 空数据的HotPost对象
     */
    private HotPost buildEmptyHotPost() {
        return buildHotPost(new ArrayList<>());
    }
}

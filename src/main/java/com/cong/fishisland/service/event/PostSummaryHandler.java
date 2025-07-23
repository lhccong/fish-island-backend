package com.cong.fishisland.service.event;

import com.cong.fishisland.datasource.ai.AIChatDataSource;
import com.cong.fishisland.model.entity.post.Post;
import com.cong.fishisland.model.vo.ai.AiResponse;
import com.cong.fishisland.model.vo.ai.SiliconFlowRequest;
import com.cong.fishisland.service.PostService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.util.ArrayList;
import java.util.List;

/**
 * @author 许林涛
 * @date 2025年07月22日 17:11
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class PostSummaryHandler {
    @Qualifier("siliconFlowDataSource")
    private final AIChatDataSource siliconFlowDataSource;
    @Resource
    private final PostService postService;

    // 系统预设提示词
    private static final String SYSTEM_PROMPT =  "你是一名专业的内容摘要生成助手。请基于以下要求处理用户内容：\n"
            + "1. 使用客观第三人称视角总结内容（避免使用'我'等第一人称代词）\n"
            + "2. 精准提取核心事实和关键信息\n"
            + "3. 完全保持原文的客观立场和事实表述\n"
            + "4. 使用简洁中立的语言表达（不超过100字）\n"
            + "5. 保留原文的专业术语和关键数据\n"
            + "6. 输出格式为纯事实陈述文本";

    /**
     * 异步生成帖子总结
     *
     * @param postId 帖子ID
     */
    @Async("taskExecutor")
    public void generateSummaryAsync(Long postId) {
        if (postId == null){
            return;
        }
        Post post = postService.getById(postId);
        if (post == null || post.getContent() == null) {
            log.error("帖子不存在或内容为空: postId={}", postId);
            return;
        }

        // 内容长度不足200个字，不生成总结
        if (post.getContent().length() < 200) {
            return;
        }

        try {
            // 构建AI请求
            List<SiliconFlowRequest.Message> messages = new ArrayList<>();

            // 系统消息
            messages.add(new SiliconFlowRequest.Message() {{
                setRole("system");
                setContent(SYSTEM_PROMPT);
            }});

            // 用户消息（帖子内容）
            messages.add(new SiliconFlowRequest.Message() {{
                setRole("user");
                setContent(post.getContent());
            }});

            // 调用AI生成总结
            AiResponse aiResponse = siliconFlowDataSource.getAiResponse(
                    messages,
                    "Qwen/Qwen2.5-14B-Instruct"
            );
            log.info("aiResponse:{}", aiResponse);
            // 更新帖子总结
            post.setSummary(aiResponse.getAnswer());
            postService.updateById(post);

            log.info("帖子总结生成成功: postId={}", postId);
        } catch (Exception e) {
            log.error("帖子总结生成失败: postId={}, error={}", postId, e.getMessage());
        }
    }
}

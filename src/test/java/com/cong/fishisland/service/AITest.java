package com.cong.fishisland.service;

import cn.hutool.http.HttpRequest;
import cn.hutool.http.HttpResponse;
import cn.hutool.json.JSONUtil;
import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.cong.fishisland.common.TestBase;
import com.cong.fishisland.config.AIModelConfig;
import com.cong.fishisland.datasource.ai.AIChatDataSource;
import com.cong.fishisland.model.vo.ai.AiResponse;
import com.cong.fishisland.model.vo.ai.ImageAIRequest;
import lombok.extern.slf4j.Slf4j;
import okhttp3.*;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import javax.annotation.Resource;
import java.io.File;
import java.io.FileOutputStream;
import java.util.Base64;
import java.util.Map;

@Slf4j
class AITest extends TestBase {

    @Resource
    private AIChatDataSource aiChatDataSource;
    @Resource
    private AIModelConfig aiModelConfig;

    @Test
    void testSiliconFlowDataSource() {
        AiResponse response = aiChatDataSource.getAiResponse("今天会下雨吗？", "Qwen/Qwen2.5-14B-Instruct");
        log.info(response.toString());
        log.info(response.getAnswer());
        Assertions.assertNotNull(response.getAnswer());
    }

    @Test
    void createImgTest() {
        ImageAIRequest imageAIRequest = new ImageAIRequest();
        imageAIRequest.setModel("flux.1-dev");
        imageAIRequest.setPrompt("a cute cat");
        imageAIRequest.setSize("128x128");
        imageAIRequest.setResponse_format("b64_json");

        // 发送 HTTP 请求
        HttpResponse response = HttpRequest.post(aiModelConfig.getChutesAi2() + "/images/generations")
                .header("Authorization", "ccong")
                .header("Content-Type", "application/json")
                .body(JSONUtil.toJsonStr(imageAIRequest))
                .execute();
        String result = response.body();
        JSONObject jsonObject = JSON.parseObject(result);
        JSONArray jsonArray = jsonObject.getJSONArray("data");
        String b64Json = jsonArray.getJSONObject(0).getString("b64_json");

        // 将 base64 转换为图片文件
        try {
            byte[] imageBytes = Base64.getDecoder().decode(b64Json);
            File tempFile = File.createTempFile("generated_image", ".png");
            try (FileOutputStream fos = new FileOutputStream(tempFile)) {
                fos.write(imageBytes);
            }

            // 创建 OkHttpClient
            OkHttpClient client = new OkHttpClient();

            // 创建 MultipartBody
            RequestBody requestBody = new MultipartBody.Builder()
                    .setType(MultipartBody.FORM)
                    .addFormDataPart("image", tempFile.getName(),
                            RequestBody.create(MediaType.parse("image/png"), tempFile))
                    .build();

            // 创建请求
            Request request = new Request.Builder()
                    .url("https://i.111666.best/image")
                    .addHeader("Auth-Token", "YOUR-TOKEN")
                    .post(requestBody)
                    .build();

            // 发送请求
            try (Response uploadResponse = client.newCall(request).execute()) {
                // 验证响应
                Assertions.assertTrue(uploadResponse.isSuccessful(), "上传失败：" + uploadResponse.code());
                String responseBody = uploadResponse.body().string();
                log.info("上传成功，响应内容：{}", responseBody);
            }

            // 删除临时文件
            tempFile.delete();

        } catch (Exception e) {
            log.error("处理图片时发生错误", e);
            throw new RuntimeException(e);
        }
    }
}

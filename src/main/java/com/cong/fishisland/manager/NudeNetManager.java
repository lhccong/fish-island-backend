package com.cong.fishisland.manager;

import cn.hutool.core.io.FileUtil;
import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.cong.fishisland.common.ErrorCode;
import com.cong.fishisland.common.exception.BusinessException;
import com.cong.fishisland.config.NudeNetProperties;
import com.cong.fishisland.service.EventRemindService;
import lombok.extern.slf4j.Slf4j;
import okhttp3.MediaType;
import okhttp3.MultipartBody;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import javax.annotation.Resource;
import java.io.IOException;
import java.util.Arrays;
import java.util.Set;
import java.util.concurrent.TimeUnit;

/**
 * NudeNet 图片内容检测
 */
@Service
@Slf4j
public class NudeNetManager {

    private static final Set<String> IMAGE_SUFFIXES = new java.util.HashSet<>(
            Arrays.asList("jpeg", "jpg", "png", "webp", "gif", "bmp")
    );

    @Resource
    private NudeNetProperties nudeNetProperties;

    @Resource
    private EventRemindService eventRemindService;

    private volatile OkHttpClient client;

    /**
     * 检测上传图片是否违规，违规则抛出业务异常
     */
    public void checkImage(MultipartFile multipartFile, Long userId) {
        if (!nudeNetProperties.isEnabled() || !isImage(multipartFile)) {
            return;
        }
        try {
            String responseBody = infer(multipartFile);
            validatePrediction(responseBody, multipartFile, userId);
        } catch (BusinessException e) {
            throw e;
        } catch (Exception e) {
            log.error("NudeNet detect failed, filename={}", multipartFile.getOriginalFilename(), e);
            if (nudeNetProperties.isFailOpen()) {
                return;
            }
            throw new BusinessException(ErrorCode.OPERATION_ERROR, "图片检测服务暂不可用，请稍后重试");
        }
    }

    private String infer(MultipartFile multipartFile) throws IOException {
        String filename = multipartFile.getOriginalFilename();
        if (filename == null || filename.isEmpty()) {
            filename = "upload.jpg";
        }
        String contentType = multipartFile.getContentType();
        if (contentType == null || contentType.isEmpty()) {
            contentType = "application/octet-stream";
        }

        RequestBody fileBody = RequestBody.create(
                MediaType.parse(contentType),
                multipartFile.getBytes()
        );
        RequestBody requestBody = new MultipartBody.Builder()
                .setType(MultipartBody.FORM)
                .addFormDataPart("f1", filename, fileBody)
                .build();

        Request request = new Request.Builder()
                .url(nudeNetProperties.getInferUrl())
                .post(requestBody)
                .build();

        try (Response response = getClient().newCall(request).execute()) {
            if (!response.isSuccessful() || response.body() == null) {
                throw new IOException("NudeNet infer failed, code=" + response.code());
            }
            return response.body().string();
        }
    }

    private void validatePrediction(String responseBody, MultipartFile multipartFile, Long userId) {
        JSONObject jsonObject = JSONObject.parseObject(responseBody);
        if (jsonObject == null || !jsonObject.getBooleanValue("success")) {
            throw new BusinessException(ErrorCode.OPERATION_ERROR, "图片检测失败，请稍后重试");
        }

        JSONArray prediction = jsonObject.getJSONArray("prediction");
        if (prediction == null || prediction.isEmpty()) {
            return;
        }

        JSONArray items = prediction.getJSONArray(0);
        if (items == null || items.isEmpty()) {
            return;
        }

        Set<String> blockedClasses = nudeNetProperties.blockedClassSet();
        double threshold = nudeNetProperties.getScoreThreshold();

        for (int i = 0; i < items.size(); i++) {
            JSONObject item = items.getJSONObject(i);
            if (item == null) {
                continue;
            }
            String clazz = item.getString("class");
            double score = item.getDoubleValue("score");
            if (clazz != null && blockedClasses.contains(clazz) && score >= threshold) {
                log.warn("Image blocked by NudeNet, userId={}, class={}, score={}", userId, clazz, score);
                notifyViolation(userId, clazz, score, multipartFile.getOriginalFilename());
                throw new BusinessException(ErrorCode.OPERATION_ERROR, "图片内容违规，请更换后重试");
            }
        }
    }

    private void notifyViolation(Long userId, String clazz, double score, String filename) {
        String userPart = userId != null ? "用户 " + userId : "未知用户";
        String filePart = filename != null && !filename.isEmpty() ? filename : "未知文件";
        String message = String.format("%s 上传违规图片被拦截：检测到 %s（score=%.2f），文件名 %s",
                userPart, clazz, score, filePart);
        eventRemindService.sendSystemNotify(nudeNetProperties.getNotifyUserId(), message, "nudenet_violation");
    }

    private boolean isImage(MultipartFile multipartFile) {
        String contentType = multipartFile.getContentType();
        if (contentType != null && contentType.startsWith("image/")) {
            return !contentType.contains("svg");
        }
        String suffix = FileUtil.getSuffix(multipartFile.getOriginalFilename());
        return suffix != null && IMAGE_SUFFIXES.contains(suffix.toLowerCase());
    }

    private OkHttpClient getClient() {
        if (client == null) {
            synchronized (this) {
                if (client == null) {
                    int timeout = nudeNetProperties.getTimeoutSeconds();
                    client = new OkHttpClient.Builder()
                            .connectTimeout(timeout, TimeUnit.SECONDS)
                            .readTimeout(timeout, TimeUnit.SECONDS)
                            .writeTimeout(timeout, TimeUnit.SECONDS)
                            .build();
                }
            }
        }
        return client;
    }
}

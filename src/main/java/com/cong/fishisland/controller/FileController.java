package com.cong.fishisland.controller;

import cn.hutool.core.io.FileUtil;
import com.alibaba.fastjson.JSONObject;
import com.cong.fishisland.common.BaseResponse;
import com.cong.fishisland.common.ErrorCode;
import com.cong.fishisland.common.ResultUtils;
import com.cong.fishisland.common.exception.BusinessException;
import com.cong.fishisland.constant.FileConstant;
import com.cong.fishisland.manager.CosManager;
import com.cong.fishisland.manager.MinioManager;
import com.cong.fishisland.manager.NudeNetManager;
import com.cong.fishisland.model.dto.file.UploadFileRequest;
import com.cong.fishisland.model.entity.user.User;
import com.cong.fishisland.model.enums.FileUploadBizEnum;
import com.cong.fishisland.model.vo.file.CosCredentialVo;
import com.cong.fishisland.service.UserService;
import io.swagger.annotations.ApiOperation;
import lombok.extern.slf4j.Slf4j;
import okhttp3.*;
import okhttp3.RequestBody;
import org.apache.commons.lang3.RandomStringUtils;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import javax.annotation.Resource;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.Arrays;

/**
 * 文件接口
 * # @author <a href="https://github.com/lhccong">程序员聪</a>
 */
@RestController
@RequestMapping("/file")
@Slf4j
//@Api(tags = "文件")
public class FileController {

    @Resource
    private UserService userService;

    @Resource
    private CosManager cosManager;

    @Resource
    private MinioManager minioManager;

    @Resource
    private NudeNetManager nudeNetManager;

    @GetMapping("/cos/credential")
    @ApiOperation(value = "获取cos临时凭证")
    public BaseResponse<CosCredentialVo> getCosCredential(String fileName) {
        return ResultUtils.success(cosManager.getCredential(fileName));
    }

    /**
     * 上传文件
     * 文件上传
     *
     * @param multipartFile     multipart 文件
     * @param uploadFileRequest 上传文件请求
     * @return {@link BaseResponse}<{@link String}>
     */
    @PostMapping("/upload")
    @ApiOperation(value = "文件上传")
    public BaseResponse<String> uploadFile(@RequestPart("file") MultipartFile multipartFile,
                                           UploadFileRequest uploadFileRequest) {
        String biz = uploadFileRequest.getBiz();
        FileUploadBizEnum fileUploadBizEnum = FileUploadBizEnum.getEnumByValue(biz);
        if (fileUploadBizEnum == null) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }
        validFile(multipartFile, fileUploadBizEnum);
        User loginUser = userService.getLoginUser();
        nudeNetManager.checkImage(multipartFile, loginUser.getId());
        // 文件目录：根据业务、用户来划分
        String uuid = RandomStringUtils.randomAlphanumeric(8);
        String filename = uuid + "-" + multipartFile.getOriginalFilename();
        String filepath = String.format("/%s/%s/%s", fileUploadBizEnum.getValue(), loginUser.getId(), filename);
        File file = null;
        try {
            // 上传文件
            file = File.createTempFile(filepath, null);
            multipartFile.transferTo(file);
            cosManager.putObject(filepath, file);
            // 返回可访问地址
            return ResultUtils.success(FileConstant.COS_HOST + filepath);
        } catch (Exception e) {
            log.error("file upload error, filepath = " + filepath, e);
            throw new BusinessException(ErrorCode.SYSTEM_ERROR, "上传失败");
        } finally {
            if (file != null) {
                // 删除临时文件
                boolean delete = file.delete();
                if (!delete) {
                    log.error("file delete error, filepath = {}", filepath);
                }
            }
        }
    }

    @PostMapping("/minio/upload")
    @ApiOperation(value = "Minio 文件上传")
    public BaseResponse<String> uploadFileByMinio(@RequestPart("file") MultipartFile multipartFile,
                                                  UploadFileRequest uploadFileRequest) {
        String biz = uploadFileRequest.getBiz();
        FileUploadBizEnum fileUploadBizEnum = FileUploadBizEnum.getEnumByValue(biz);
        if (fileUploadBizEnum == null) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }
        validFile(multipartFile, fileUploadBizEnum);
        User loginUser = userService.getLoginUser();
        nudeNetManager.checkImage(multipartFile, loginUser.getId());
        // 生成存储路径
        String uuid = RandomStringUtils.randomAlphanumeric(8);
        String filename = uuid + "-" + multipartFile.getOriginalFilename();
        String filepath = String.format("%s/%s/%s", fileUploadBizEnum.getValue(), loginUser.getId(), filename);
        try {
            // 获取文件流
            InputStream is = multipartFile.getInputStream();
            String contentType = multipartFile.getContentType();

            // 上传文件到 MinIO
            return ResultUtils.success(minioManager.uploadObject(is, filepath, contentType));

        } catch (Exception e) {
            log.error("File upload failed, filePath = " + filepath, e);
            throw new BusinessException(ErrorCode.OPERATION_ERROR, "上传失败");
        }
    }

    @GetMapping("/minio/presigned/upload")
    @ApiOperation(value = "获取 minio 上传预签名URL")
    public BaseResponse<String> getMinioPresigned(String fileName) {
        String url = minioManager.generatePresignedUploadUrl(fileName);
        return ResultUtils.success(url);
    }

    @GetMapping("/minio/Presigned/download")
    @ApiOperation("获取 minio 下载预签名URL")
    public BaseResponse<String> generatePresignedDownloadUrl(@RequestParam("fileName") String fileName) {
        String url = minioManager.generatePresignedDownloadUrl(fileName);
        return ResultUtils.success(url);
    }

    @PostMapping("/111666/upload")
    @ApiOperation(value = "上传图片到111666.best")
    public BaseResponse<String> uploadTo111666(@RequestPart("file") MultipartFile multipartFile) {
        try {
            User loginUser = userService.getLoginUserPermitNull();
            Long userId = loginUser != null ? loginUser.getId() : null;
            nudeNetManager.checkImage(multipartFile, userId);
            // 1. 校验文件
            // 2. 创建 OkHttpClient
            OkHttpClient client = new OkHttpClient();

            // 3. 创建请求体
            RequestBody requestBody = new MultipartBody.Builder()
                    .setType(MultipartBody.FORM)
                    .addFormDataPart("image", multipartFile.getOriginalFilename(),
                            RequestBody.create(MediaType.parse(multipartFile.getContentType()), multipartFile.getBytes()))
                    .build();

            // 4. 创建请求
            Request request = new Request.Builder()
                    .url("https://i.111666.best/image")
                    //时间戳
                    .addHeader("Auth-Token", String.valueOf(System.currentTimeMillis()))
                    .post(requestBody)
                    .build();

            // 5. 发送请求
            try (Response response = client.newCall(request).execute()) {
                if (!response.isSuccessful()) {
                    throw new BusinessException(ErrorCode.OPERATION_ERROR, "上传失败：" + response.code());
                }
                String responseBody = response.body().string();
                log.info("上传成功，响应内容：{}", responseBody);
                //获取 src 字段
                JSONObject jsonObject = JSONObject.parseObject(responseBody);
                String src = jsonObject.getString("src");
                return ResultUtils.success("https://i.111666.best" + src);
            }
        } catch (IOException e) {
            log.error("文件上传失败", e);
            throw new BusinessException(ErrorCode.OPERATION_ERROR, "上传失败：" + e.getMessage());
        }
    }

    /**
     * 校验文件
     *
     * @param fileUploadBizEnum 业务类型
     * @param multipartFile     multipart 文件
     */
    @ApiOperation(value = "校验文件")
    private void validFile(MultipartFile multipartFile, FileUploadBizEnum fileUploadBizEnum) {
        // 文件大小
        long fileSize = multipartFile.getSize();
        // 文件后缀
        String fileSuffix = FileUtil.getSuffix(multipartFile.getOriginalFilename());
        final long oneM = 1024 * 1024L;
        if (FileUploadBizEnum.USER_AVATAR.equals(fileUploadBizEnum)) {
            if (fileSize > oneM) {
                throw new BusinessException(ErrorCode.PARAMS_ERROR, "文件大小不能超过 1M");
            }
            if (!Arrays.asList("jpeg", "jpg", "svg", "png", "webp","gif").contains(fileSuffix)) {
                throw new BusinessException(ErrorCode.PARAMS_ERROR, "文件类型错误");
            }
        }
    }
}

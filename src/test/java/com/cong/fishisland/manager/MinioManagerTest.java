package com.cong.fishisland.manager;

import com.cong.fishisland.common.TestBase;
import com.cong.fishisland.config.MinioConfig;
import com.cong.fishisland.model.entity.emoticon.EmoticonFavour;
import com.cong.fishisland.model.entity.pet.ItemTemplates;
import com.cong.fishisland.service.EmoticonFavourService;
import com.cong.fishisland.service.ItemTemplatesService;
import io.minio.ListObjectsArgs;
import io.minio.MinioClient;
import io.minio.Result;
import io.minio.messages.Item;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

import javax.annotation.Resource;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.List;
import java.util.stream.Collectors;

/**
 * MinIO 操作测试
 * <p>
 * # @author <a href="https://github.com/lhccong">程序员聪</a>
 */
@SpringBootTest
@Slf4j
class MinioManagerTest extends TestBase {

    @Resource
    private MinioManager minioManager;

    @Resource
    private MinioClient minioClient;

    @Resource
    private MinioConfig minioConfig;

    @Resource
    private EmoticonFavourService emoticonFavourService;

    @Resource
    private ItemTemplatesService itemTemplatesService;

    /**
     * 测试列出所有对象
     */
    @Test
    void testListAllObjects() {
        List<String> objectNames = minioManager.listObjects();
        log.info("MinIO中的所有对象: {}", objectNames);

        // 打印对象总数
        log.info("对象总数: {}", objectNames.size());
    }

    /**
     * 测试列出所有图片并打印完整URL
     */
    @Test
    void testListAllImages() {
        List<String> imageExtensions = Arrays.asList(".jpg", ".jpeg", ".png", ".gif", ".bmp", ".webp");
        List<String> imageUrls = new ArrayList<>();

        try {
            // 获取所有对象
            List<String> allObjects = minioManager.listObjects();

            // 过滤出图片对象
            for (String objectName : allObjects) {
                String lowerCaseName = objectName.toLowerCase();
                boolean isImage = imageExtensions.stream().anyMatch(lowerCaseName::endsWith);

                if (isImage) {
                    // 获取对象的URL
                    String imageUrl = minioManager.getObjectUrl(objectName);
                    imageUrls.add(imageUrl);
                    log.info("图片: {} - URL: {}", objectName, imageUrl);
                }
            }

            // 打印图片总数
            log.info("图片总数: {}", imageUrls.size());

        } catch (Exception e) {
            log.error("列出图片时出错: {}", e.getMessage(), e);
        }
    }

    /**
     * 直接使用MinioClient列出所有图片
     */
    @Test
    void testListAllImagesWithClient() {
        List<String> imageExtensions = Arrays.asList(".jpg", ".jpeg", ".png", ".gif", ".bmp", ".webp");
        List<String> imageUrls = new ArrayList<>();

        try {
            // 构建列出对象的参数
            ListObjectsArgs listObjectsArgs = ListObjectsArgs.builder()
                    .bucket(minioConfig.getBucketName()) // 使用配置的桶名
                    .recursive(true) // 递归列出所有对象，包括子目录
                    .build();

            // 列出所有对象
            Iterable<Result<Item>> results = minioClient.listObjects(listObjectsArgs);

            // 遍历结果
            for (Result<Item> result : results) {
                Item item = result.get();
                String objectName = item.objectName();
                String lowerCaseName = objectName.toLowerCase();

                // 检查是否为图片
                boolean isImage = imageExtensions.stream().anyMatch(lowerCaseName::endsWith);

                if (isImage) {
                    // 获取图片URL
                    String imageUrl = minioManager.getObjectUrl(objectName);
                    imageUrls.add(imageUrl);

                    // 打印图片信息
                    log.info("图片: {} - 大小: {} bytes - 最后修改: {} - URL: {}",
                            objectName,
                            item.size(),
                            item.lastModified(),
                            imageUrl);
                }
            }

            // 打印图片总数
            log.info("图片总数: {}", imageUrls.size());

        } catch (Exception e) {
            log.error("列出图片时出错: {}", e.getMessage(), e);
        }
    }

    /**
     * 测试列出指定user_file目录下的所有图片
     */
    @Test
    void testListUserFileImages() {
        String userFilePrefix = "user_file/";
        List<String> imageExtensions = Arrays.asList(".jpg", ".jpeg", ".png", ".gif", ".bmp", ".webp");
        List<String> imageUrls = new ArrayList<>();

        try {
            // 构建列出对象的参数，指定前缀为user_file/
            ListObjectsArgs listObjectsArgs = ListObjectsArgs.builder()
                    .bucket(minioConfig.getBucketName())
                    .prefix(userFilePrefix)
                    .recursive(true)
                    .build();

            // 列出所有对象
            Iterable<Result<Item>> results = minioClient.listObjects(listObjectsArgs);

            // 遍历结果
            for (Result<Item> result : results) {
                Item item = result.get();
                String objectName = item.objectName();
                String lowerCaseName = objectName.toLowerCase();

                // 检查是否为图片
                boolean isImage = imageExtensions.stream().anyMatch(lowerCaseName::endsWith);

                if (isImage) {
                    // 获取图片URL
                    String imageUrl = minioManager.getObjectUrl(objectName);
                    imageUrls.add(imageUrl);

                    // 打印图片信息
                    log.info("用户文件图片: {} - 大小: {} bytes - 最后修改: {} - URL: {}",
                            objectName,
                            item.size(),
                            item.lastModified(),
                            imageUrl);
                }
            }

            // 打印图片总数
            log.info("user_file目录下图片总数: {}", imageUrls.size());

        } catch (Exception e) {
            log.error("列出user_file目录下图片时出错: {}", e.getMessage(), e);
        }
    }

    /**
     * 删除除指定文件外的所有user_file目录下的图片
     * 注意：此方法会实际删除文件，请谨慎使用
     */
    @Test
    void testDeleteAllExceptSpecificImage() {
        List<EmoticonFavour> list = emoticonFavourService.list();
        List<String> srcList = list.stream()
                .map(EmoticonFavour::getEmoticonSrc).collect(Collectors.toList());
        String userFilePrefix = "user_file/";
        List<String> imageExtensions = Arrays.asList(".jpg", ".jpeg", ".png", ".gif", ".bmp", ".webp");
        int deletedCount = 0;

        try {
            // 构建列出对象的参数，指定前缀为user_file/
            ListObjectsArgs listObjectsArgs = ListObjectsArgs.builder()
                    .bucket(minioConfig.getBucketName())
                    .prefix(userFilePrefix)
                    .recursive(true)
                    .build();

            // 列出所有对象
            Iterable<Result<Item>> results = minioClient.listObjects(listObjectsArgs);

            // 遍历结果
            for (Result<Item> result : results) {
                Item item = result.get();
                String objectName = item.objectName();
                String lowerCaseName = objectName.toLowerCase();

                // 检查是否为图片
                boolean isImage = imageExtensions.stream().anyMatch(lowerCaseName::endsWith);

                if (isImage && !srcList.contains(("https://oss.cqbo.com/moyu/" + objectName))) {
                    // 删除该对象
                    log.info("正在删除文件: {}", objectName);
                    minioManager.deleteObject(objectName);
                    deletedCount++;
                } else if (srcList.contains(("https://oss.cqbo.com/moyu/" + objectName))) {
                    log.info("保留文件: {}", objectName);
                }
            }

            // 打印删除总数
            log.info("成功删除文件数量: {}", deletedCount);

        } catch (Exception e) {
            log.error("删除文件时出错: {}", e.getMessage(), e);
        }
    }

    /**
     * 测试读取equipment文件夹下的所有图片数据存入item_templates表
     */
    @Test
    void testSaveEquipmentImagesToItemTemplates() {
        String equipmentPrefix = "equipment/";
        List<String> imageExtensions = Arrays.asList(".jpg", ".jpeg", ".png", ".gif", ".bmp", ".webp");
        int savedCount = 0;
        int skippedCount = 0;

        try {
            // 构建列出对象的参数，指定前缀为equipment/
            ListObjectsArgs listObjectsArgs = ListObjectsArgs.builder()
                    .bucket(minioConfig.getBucketName())
                    .prefix(equipmentPrefix)
                    .recursive(true)
                    .build();

            // 列出所有对象
            Iterable<Result<Item>> results = minioClient.listObjects(listObjectsArgs);

            // 遍历结果
            for (Result<Item> result : results) {
                Item item = result.get();
                String objectName = item.objectName();
                String lowerCaseName = objectName.toLowerCase();

                // 检查是否为图片
                boolean isImage = imageExtensions.stream().anyMatch(lowerCaseName::endsWith);

                if (isImage) {
                    // 直接使用域名构建图片URL
                    String imageUrl = "https://oss.cqbo.com/moyu/" + objectName;

                    // 从文件名生成code和name
                    // 例如: equipment/sword_01.png -> code: equipment_sword_01, name: sword_01
                    String fileName = objectName.substring(equipmentPrefix.length());
                    int lastDotIndex = fileName.lastIndexOf('.');
                    String fileNameWithoutExt = lastDotIndex > 0 ? fileName.substring(0, lastDotIndex) : fileName;
                    
                    // 生成code: 将路径分隔符替换为下划线，并添加equipment前缀
                    String code = "equipment_" + fileNameWithoutExt.replace("/", "_").replace("\\", "_");
                    
                    // 生成name: 使用文件名（去掉路径）
                    String name = fileNameWithoutExt;
                    if (name.contains("/")) {
                        name = name.substring(name.lastIndexOf("/") + 1);
                    }
                    if (name.contains("\\")) {
                        name = name.substring(name.lastIndexOf("\\") + 1);
                    }

                    // 检查code是否已存在
                    long existed = itemTemplatesService.lambdaQuery()
                            .eq(ItemTemplates::getCode, code)
                            .eq(ItemTemplates::getIsDelete, 0)
                            .count();
                    
                    if (existed > 0) {
                        log.info("跳过已存在的物品模板: code={}, name={}, url={}", code, name, imageUrl);
                        skippedCount++;
                        continue;
                    }

                    // 创建ItemTemplates对象
                    ItemTemplates itemTemplate = new ItemTemplates();
                    itemTemplate.setCode(code);
                    itemTemplate.setName(name);
                    itemTemplate.setCategory("equipment");
                    itemTemplate.setIcon(imageUrl);
                    itemTemplate.setRarity(1);
                    itemTemplate.setLevelReq(1);
                    itemTemplate.setBaseAttack(0);
                    itemTemplate.setBaseDefense(0);
                    itemTemplate.setBaseHp(0);
                    itemTemplate.setStackable(0);
                    itemTemplate.setRemovePoint(10);
                    itemTemplate.setIsDelete(0);
                    itemTemplate.setCreateTime(new Date());
                    itemTemplate.setUpdateTime(new Date());

                    // 保存到数据库
                    boolean saveResult = itemTemplatesService.save(itemTemplate);
                    if (saveResult) {
                        savedCount++;
                        log.info("成功保存物品模板: code={}, name={}, url={}, id={}", 
                                code, name, imageUrl, itemTemplate.getId());
                    } else {
                        log.error("保存物品模板失败: code={}, name={}", code, name);
                    }
                }
            }

            // 打印统计信息
            log.info("equipment目录下图片处理完成 - 成功保存: {}, 跳过(已存在): {}", savedCount, skippedCount);

        } catch (Exception e) {
            log.error("读取equipment目录下图片并存入item_templates表时出错: {}", e.getMessage(), e);
        }
    }
} 
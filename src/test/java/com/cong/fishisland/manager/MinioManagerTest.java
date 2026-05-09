package com.cong.fishisland.manager;

import com.cong.fishisland.common.TestBase;
import com.cong.fishisland.config.MinioConfig;
import com.cong.fishisland.datasource.hostpost.JueJinBoilingDataSource;
import com.cong.fishisland.job.cycle.IncSyncHostPostToMySQL;
import com.cong.fishisland.model.entity.emoticon.EmoticonFavour;
import com.cong.fishisland.model.entity.hot.HotPost;
import com.cong.fishisland.model.entity.pet.ItemTemplates;
import com.cong.fishisland.model.entity.pet.PetSkin;
import com.cong.fishisland.model.entity.user.AvatarFrame;
import com.cong.fishisland.service.AvatarFrameService;
import com.cong.fishisland.service.EmoticonFavourService;
import com.cong.fishisland.service.ItemTemplatesService;
import com.cong.fishisland.service.PetSkinService;
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

    @Resource
    private PetSkinService petSkinService;

    @Resource
    private AvatarFrameService avatarFrameService;

    @Resource
    IncSyncHostPostToMySQL incSyncHostPostToMySQL;

    @Test
    void testUploadObject() {
       incSyncHostPostToMySQL.run();
    }

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
     * 扫描 MinIO equipment/ 目录下的图片，批量写入 item_templates 表。
     * 文件名规则（不区分大小写关键词）：
     *   weapon/sword/bow/staff/axe/gun → weapon
     *   helmet/head/hat              → head
     *   glove/hand                   → hand
     *   shoe/boot/foot               → foot
     *   necklace/neck/amulet         → necklace
     *   wing/wings                   → wings
     * code 取文件名去掉扩展名，icon 为完整访问 URL。
     */
    @Test
    void batchInsertEquipmentFromMinio() {
        String equipmentPrefix = "equipment/";
        List<String> imageExtensions = Arrays.asList(".jpg", ".jpeg", ".png", ".gif", ".bmp", ".webp");
        List<ItemTemplates> toInsert = new ArrayList<>();

        try {
            ListObjectsArgs args = ListObjectsArgs.builder()
                    .bucket(minioConfig.getBucketName())
                    .prefix(equipmentPrefix)
                    .recursive(true)
                    .build();

            Iterable<Result<Item>> results = minioClient.listObjects(args);
            for (Result<Item> result : results) {
                String objectName = result.get().objectName();
                String lower = objectName.toLowerCase();

                boolean isImage = imageExtensions.stream().anyMatch(lower::endsWith);
                if (!isImage) {
                    continue;
                }

                // 取文件名（去掉目录前缀）
                String fileName = objectName.contains("/")
                        ? objectName.substring(objectName.lastIndexOf('/') + 1)
                        : objectName;
                // code = 文件名去掉扩展名
                String code = fileName.contains(".")
                        ? fileName.substring(0, fileName.lastIndexOf('.'))
                        : fileName;


                // 获取图标 URL（永久直链，使用 minioConfig.getUrl()）
                String iconUrl = minioConfig.getUrl() + objectName;

                ItemTemplates item = new ItemTemplates();
                item.setCode(code);
                item.setName(code);          // 名称先用 code，后续可手动改
                item.setCategory("equipment");
                item.setSubType(null);
                item.setEquipSlot(null);
                item.setRarity(1);
                item.setLevelReq(1);
                item.setBaseAttack(0);
                item.setBaseDefense(0);
                item.setBaseHp(0);
                item.setBaseSpeed(0);
                item.setStackable(0);
                item.setRemovePoint(10);
                item.setIcon(iconUrl);
                item.setIsDelete(0);

                toInsert.add(item);
                log.info("准备插入装备: code={},  icon={}", code, iconUrl);
            }

            if (toInsert.isEmpty()) {
                log.warn("equipment/ 目录下未找到任何图片，请检查 MinIO 路径");
                return;
            }

            // 批量保存（MyBatis-Plus saveBatch，忽略已存在的 code）
            itemTemplatesService.saveBatch(toInsert);
            log.info("批量插入完成，共插入 {} 条装备模板", toInsert.size());

        } catch (Exception e) {
            log.error("批量插入装备模板失败: {}", e.getMessage(), e);
        }
    }

    /**
     * 扫描 MinIO pet/ 目录下的图片，批量写入 pet_skin 表。
     * name 取文件名去掉扩展名，url 为完整访问 URL，points 默认 0。
     */
    @Test
    void batchInsertPetSkinFromMinio() {
        String petPrefix = "pet/";
        List<String> imageExtensions = Arrays.asList(".jpg", ".jpeg", ".png", ".gif", ".bmp", ".webp");
        List<PetSkin> toInsert = new ArrayList<>();

        try {
            ListObjectsArgs args = ListObjectsArgs.builder()
                    .bucket(minioConfig.getBucketName())
                    .prefix(petPrefix)
                    .recursive(true)
                    .build();

            Iterable<Result<Item>> results = minioClient.listObjects(args);
            for (Result<Item> result : results) {
                String objectName = result.get().objectName();
                String lower = objectName.toLowerCase();

                boolean isImage = imageExtensions.stream().anyMatch(lower::endsWith);
                if (!isImage) {
                    continue;
                }

                // 取文件名（去掉目录前缀）
                String fileName = objectName.contains("/")
                        ? objectName.substring(objectName.lastIndexOf('/') + 1)
                        : objectName;
                // name = 文件名去掉扩展名
                String name = fileName.contains(".")
                        ? fileName.substring(0, fileName.lastIndexOf('.'))
                        : fileName;

                String url = minioConfig.getUrl() + objectName;

                PetSkin skin = new PetSkin();
                skin.setName(name);
                skin.setUrl(url);
                skin.setDescription(name);
                skin.setPoints(10000);
                skin.setIsDelete(0);

                toInsert.add(skin);
                log.info("准备插入皮肤: name={}, url={}", name, url);
            }

            if (toInsert.isEmpty()) {
                log.warn("pet/ 目录下未找到任何图片，请检查 MinIO 路径");
                return;
            }

            petSkinService.saveBatch(toInsert);
            log.info("批量插入完成，共插入 {} 条宠物皮肤", toInsert.size());

        } catch (Exception e) {
            log.error("批量插入宠物皮肤失败: {}", e.getMessage(), e);
        }
    }

    /**
     * 扫描 MinIO 指定目录下的图片，批量写入 emoticon_favour 表。
     * 传入目录前缀（如 "emoticon/"）和用户 ID，将目录下所有图片的完整 URL 作为 emoticonSrc 插入。
     */
    @Test
    void batchInsertEmoticonFavourFromMinio() {
        // ★ 修改这两个参数即可
        String prefix = "user_file/1935639117732880386/";
        Long userId = 2052619705395548162L;

        List<String> imageExtensions = Arrays.asList(".jpg", ".jpeg", ".png", ".gif", ".bmp", ".webp");
        List<EmoticonFavour> toInsert = new ArrayList<>();

        try {
            ListObjectsArgs args = ListObjectsArgs.builder()
                    .bucket(minioConfig.getBucketName())
                    .prefix(prefix)
                    .recursive(true)
                    .build();

            Iterable<Result<Item>> results = minioClient.listObjects(args);
            for (Result<Item> result : results) {
                String objectName = result.get().objectName();
                String lower = objectName.toLowerCase();

                if (imageExtensions.stream().noneMatch(lower::endsWith)) {
                    continue;
                }

                String url = minioConfig.getUrl() + objectName;

                EmoticonFavour favour = new EmoticonFavour();
                favour.setUserId(userId);
                favour.setEmoticonSrc(url);

                toInsert.add(favour);
                log.info("准备插入表情包收藏: userId={}, url={}", userId, url);
            }

            if (toInsert.isEmpty()) {
                log.warn("{} 目录下未找到任何图片，请检查 MinIO 路径", prefix);
                return;
            }

            emoticonFavourService.saveBatch(toInsert);
            log.info("批量插入完成，共插入 {} 条表情包收藏记录", toInsert.size());

        } catch (Exception e) {
            log.error("批量插入表情包收藏失败: {}", e.getMessage(), e);
        }
    }

    /**
     * 扫描 MinIO avatar_frame/ 目录下的图片，批量写入 avatar_frame 表。
     * name 取文件名去掉扩展名，url 为完整访问 URL，points 默认 0。
     */
    @Test
    void batchInsertAvatarFrameFromMinio() {
        String prefix = "avatar_frame/";
        List<String> imageExtensions = Arrays.asList(".jpg", ".jpeg", ".png", ".gif", ".bmp", ".webp");
        List<AvatarFrame> toInsert = new ArrayList<>();

        try {
            ListObjectsArgs args = ListObjectsArgs.builder()
                    .bucket(minioConfig.getBucketName())
                    .prefix(prefix)
                    .recursive(true)
                    .build();

            Iterable<Result<Item>> results = minioClient.listObjects(args);
            for (Result<Item> result : results) {
                String objectName = result.get().objectName();
                String lower = objectName.toLowerCase();

                if (imageExtensions.stream().noneMatch(lower::endsWith)) {
                    continue;
                }

                String fileName = objectName.contains("/")
                        ? objectName.substring(objectName.lastIndexOf('/') + 1)
                        : objectName;
                String name = fileName.contains(".")
                        ? fileName.substring(0, fileName.lastIndexOf('.'))
                        : fileName;

                String url = minioConfig.getUrl() + objectName;

                AvatarFrame frame = new AvatarFrame();
                frame.setName(name);
                frame.setUrl(url);
                frame.setPoints(10000);
                frame.setIsDelete(0);

                toInsert.add(frame);
                log.info("准备插入头像框: name={}, url={}", name, url);
            }

            if (toInsert.isEmpty()) {
                log.warn("avatar_frame/ 目录下未找到任何图片，请检查 MinIO 路径");
                return;
            }

            avatarFrameService.saveBatch(toInsert);
            log.info("批量插入完成，共插入 {} 条头像框", toInsert.size());

        } catch (Exception e) {
            log.error("批量插入头像框失败: {}", e.getMessage(), e);
        }
    }


} 
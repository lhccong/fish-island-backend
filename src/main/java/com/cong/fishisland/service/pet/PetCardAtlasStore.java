package com.cong.fishisland.service.pet;

import com.cong.fishisland.common.ErrorCode;
import com.cong.fishisland.common.exception.BusinessException;
import com.cong.fishisland.manager.MinioManager;
import lombok.RequiredArgsConstructor;
import org.apache.commons.codec.digest.DigestUtils;
import org.springframework.stereotype.Component;

import javax.imageio.ImageIO;
import javax.imageio.ImageReader;
import javax.imageio.stream.ImageInputStream;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Iterator;

import static com.cong.fishisland.service.pet.PetCardCatalogRules.require;

@Component
@RequiredArgsConstructor
public class PetCardAtlasStore {
    private static final String PREFIX = "pet-cards/atlases/";
    public static final int MAX_BYTES = 8 * 1024 * 1024;
    private final MinioManager minio;

    static boolean isAtlasKey(String key) {
        return key != null && key.matches("[0-9a-f]{64}\\.(jpg|png)");
    }

    String write(byte[] image, boolean portrait, boolean legacy) {
        require(image.length > 0 && image.length <= MAX_BYTES, "图集大小须在 8 MB 以内");
        String extension = detect(image, portrait, legacy);
        String key = DigestUtils.sha256Hex(image) + "." + extension;
        minio.uploadObject(new ByteArrayInputStream(image), PREFIX + key, contentType(key));
        return key;
    }

    public InputStream read(String key) {
        return isAtlasKey(key) ? minio.getObject(PREFIX + key) : null;
    }

    public static String contentType(String key) {
        return key.endsWith(".png") ? "image/png" : "image/jpeg";
    }

    private static String detect(byte[] image, boolean portrait, boolean legacy) {
        try (ImageInputStream input = ImageIO.createImageInputStream(new ByteArrayInputStream(image))) {
            require(input != null, "无法读取图集");
            Iterator<ImageReader> readers = ImageIO.getImageReaders(input);
            require(readers.hasNext(), "仅支持 JPG 或 PNG 图集");
            ImageReader reader = readers.next();
            try {
                reader.setInput(input, true, true);
                String format = reader.getFormatName();
                require("PNG".equalsIgnoreCase(format) || "JPEG".equalsIgnoreCase(format), "仅支持 JPG 或 PNG 图集");
                int width = reader.getWidth(0);
                int height = reader.getHeight(0);
                require(width >= 5 && height >= 2 && width <= 8192 && height <= 8192
                        && ((long) width) * height <= 24000000, "图集像素尺寸超出范围");
                // New uploads must be composed as ten equal square or 4:5 cells.
                int target = portrait ? 4 : 5;
                require(legacy || Math.abs(width * 2.0 / height - target) <= 0.04,
                        "请上传 5 列 2 行图集，正方格宽高比 5:2，4:5 竖格图集宽高比 2:1");
                return "PNG".equalsIgnoreCase(format) ? "png" : "jpg";
            } finally {
                reader.dispose();
            }
        } catch (IOException e) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "无法读取图集，请检查图片文件");
        }
    }
}

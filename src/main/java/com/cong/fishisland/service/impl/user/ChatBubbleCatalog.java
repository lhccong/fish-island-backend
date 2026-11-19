package com.cong.fishisland.service.impl.user;

import com.cong.fishisland.common.ErrorCode;
import com.cong.fishisland.common.exception.BusinessException;
import com.cong.fishisland.common.exception.ThrowUtils;
import com.cong.fishisland.mapper.user.ChatBubbleProductMapper;
import com.cong.fishisland.model.entity.user.ChatBubbleProduct;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Service;

import java.net.URI;
import java.util.List;
import java.util.regex.Pattern;

@Service
@RequiredArgsConstructor
public class ChatBubbleCatalog {
    private static final Pattern ID = Pattern.compile("[a-z][a-z0-9]*(?:-[a-z0-9]+)*");
    private final ChatBubbleProductMapper products;

    public List<ChatBubbleProduct> list() {
        return products.list();
    }

    public ChatBubbleProduct forExchange(String id) {
        return requireProduct(products.findForExchange(requireId(id)));
    }

    public ChatBubbleProduct create(String id, String name, Integer points, String imageUrl,
                                    String darkImageUrl, String textTone) {
        requireId(id);
        requirePrice(points);
        String displayName = name == null ? "" : name.trim();
        ThrowUtils.throwIf(displayName.isEmpty() || displayName.length() > 64,
                ErrorCode.PARAMS_ERROR, "气泡名称需为 1 至 64 个字符");
        String tone = textTone == null ? "theme" : textTone;
        ThrowUtils.throwIf(!"theme".equals(tone) && !"light".equals(tone) && !"dark".equals(tone),
                ErrorCode.PARAMS_ERROR, "气泡文字颜色选项无效");
        ChatBubbleProduct product = new ChatBubbleProduct(id, displayName, points,
                requireImageUrl(imageUrl), darkImageUrl == null ? null : requireImageUrl(darkImageUrl), tone);
        try {
            ThrowUtils.throwIf(products.create(product) != 1, ErrorCode.OPERATION_ERROR, "添加气泡失败");
        } catch (DuplicateKeyException e) {
            throw new BusinessException(ErrorCode.OPERATION_ERROR, "气泡编号已存在");
        }
        return product;
    }

    public ChatBubbleProduct setPrice(String id, Integer points) {
        requireId(id);
        requirePrice(points);
        products.setPrice(id, points);
        return requireProduct(products.find(id));
    }

    static String requireId(String id) {
        ThrowUtils.throwIf(id == null || id.length() > 32 || !ID.matcher(id).matches(),
                ErrorCode.PARAMS_ERROR, "气泡编号需为 1 至 32 位小写字母、数字或连字符，并以字母开头");
        return id;
    }

    private static void requirePrice(Integer points) {
        ThrowUtils.throwIf(points == null || points <= 0, ErrorCode.PARAMS_ERROR, "价格必须为正整数积分");
    }

    private static ChatBubbleProduct requireProduct(ChatBubbleProduct product) {
        ThrowUtils.throwIf(product == null, ErrorCode.NOT_FOUND_ERROR, "气泡不存在");
        return product;
    }

    private static String requireImageUrl(String value) {
        if (value != null && value.length() <= 1024) {
            try {
                URI uri = URI.create(value);
                if ("https".equalsIgnoreCase(uri.getScheme()) && uri.getHost() != null
                        && uri.getUserInfo() == null && uri.getFragment() == null
                        && uri.toASCIIString().length() <= 1024) {
                    return uri.toASCIIString();
                }
            } catch (IllegalArgumentException e) {
                // Report invalid addresses using the same validation error.
            }
        }
        throw new BusinessException(ErrorCode.PARAMS_ERROR, "图片需为有效的公开 HTTPS 地址");
    }
}

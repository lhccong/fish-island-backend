package com.cong.fishisland.controller.admin;

import cn.dev33.satoken.annotation.SaCheckRole;
import com.cong.fishisland.common.BaseResponse;
import com.cong.fishisland.common.ErrorCode;
import com.cong.fishisland.common.ResultUtils;
import com.cong.fishisland.common.exception.BusinessException;
import com.cong.fishisland.constant.UserConstant;
import com.cong.fishisland.model.dto.pet.PetCardCatalog.Atlas;
import com.cong.fishisland.model.dto.pet.PetCardCatalog.Card;
import com.cong.fishisland.model.dto.pet.PetCardCatalog.CardPage;
import com.cong.fishisland.model.dto.pet.PetCardCatalog.Pool;
import com.cong.fishisland.service.pet.PetCardAtlasStore;
import com.cong.fishisland.service.pet.PetCardCatalogService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.List;

@RestController
@RequestMapping("/admin/pet-cards")
@SaCheckRole(UserConstant.ADMIN_ROLE)
@RequiredArgsConstructor
public class PetCardAdminController {
    private final PetCardCatalogService service;

    @GetMapping("/pools")
    public BaseResponse<List<Pool>> pools() {
        return ResultUtils.success(service.pools());
    }

    @PostMapping("/pools")
    public BaseResponse<Boolean> savePool(@RequestBody Pool pool) {
        service.savePool(pool);
        return ResultUtils.success(true);
    }

    @GetMapping("/cards")
    public BaseResponse<CardPage> cards(@RequestParam(required = false) String poolId,
                                       @RequestParam(required = false) String query,
                                       @RequestParam(defaultValue = "1") int page) {
        return ResultUtils.success(service.cards(poolId, query, page));
    }

    @PostMapping("/cards")
    public BaseResponse<Boolean> saveCard(@RequestBody Card card) {
        service.saveCard(card);
        return ResultUtils.success(true);
    }

    @GetMapping("/atlases")
    public BaseResponse<List<Atlas>> atlases() {
        return ResultUtils.success(service.atlases());
    }

    @PostMapping("/atlases")
    public BaseResponse<Atlas> uploadAtlas(@RequestParam String name, @RequestParam boolean portrait,
                                           @RequestPart("file") MultipartFile file) throws IOException {
        if (file.isEmpty() || file.getSize() > PetCardAtlasStore.MAX_BYTES) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "图集大小须在 8 MB 以内");
        }
        return ResultUtils.success(service.uploadAtlas(name, portrait, file.getBytes()));
    }
}

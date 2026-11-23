package com.cong.fishisland.controller.pet;

import com.cong.fishisland.common.BaseResponse;
import com.cong.fishisland.common.ResultUtils;
import com.cong.fishisland.model.dto.pet.PetCardCatalog.Catalog;
import com.cong.fishisland.service.pet.PetCardAtlasStore;
import com.cong.fishisland.service.pet.PetCardCatalogService;
import lombok.RequiredArgsConstructor;
import org.springframework.core.io.InputStreamResource;
import org.springframework.http.CacheControl;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.io.InputStream;
import java.util.concurrent.TimeUnit;

@RestController
@RequestMapping("/pet/cards")
@RequiredArgsConstructor
public class PetCardCatalogController {
    private final PetCardCatalogService service;
    private final PetCardAtlasStore images;

    @GetMapping("/catalog")
    public BaseResponse<Catalog> catalog() {
        return ResultUtils.success(service.catalog());
    }

    @GetMapping("/art/{key:.+}")
    public ResponseEntity<InputStreamResource> art(@PathVariable String key) {
        InputStream stream = images.read(key);
        if (stream == null) return ResponseEntity.notFound().build();
        return ResponseEntity.ok()
                .cacheControl(CacheControl.maxAge(365, TimeUnit.DAYS).cachePublic())
                .contentType(MediaType.parseMediaType(PetCardAtlasStore.contentType(key)))
                .body(new InputStreamResource(stream));
    }
}

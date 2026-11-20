package com.apishield.registry.controller;

import com.apishield.registry.dto.CatalogDetailResponse;
import com.apishield.registry.dto.CatalogItemResponse;
import com.apishield.registry.service.CatalogService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/catalog")
@RequiredArgsConstructor
public class CatalogController {

    private final CatalogService catalogService;

    @GetMapping
    public ResponseEntity<Page<CatalogItemResponse>> getCatalog(
            @RequestParam(required = false) String category,
            @RequestParam(required = false) String search,
            @PageableDefault(size = 20) Pageable pageable) {
        Page<CatalogItemResponse> response = catalogService.getCatalog(category, search, pageable);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/{slug}")
    public ResponseEntity<CatalogDetailResponse> getCatalogItem(@PathVariable String slug) {
        CatalogDetailResponse response = catalogService.getCatalogItemBySlug(slug);
        return ResponseEntity.ok(response);
    }
}

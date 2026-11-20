package com.apishield.registry.service;

import com.apishield.registry.dto.CatalogDetailResponse;
import com.apishield.registry.dto.CatalogItemResponse;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface CatalogService {

    Page<CatalogItemResponse> getCatalog(String category, String search, Pageable pageable);

    CatalogDetailResponse getCatalogItemBySlug(String slug);
}

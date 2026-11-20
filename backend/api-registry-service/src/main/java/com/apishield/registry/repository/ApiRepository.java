package com.apishield.registry.repository;

import com.apishield.registry.entity.Api;
import com.apishield.registry.model.ApiStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface ApiRepository extends JpaRepository<Api, UUID>, JpaSpecificationExecutor<Api> {

    Optional<Api> findBySlug(String slug);

    boolean existsBySlug(String slug);

    Page<Api> findByOwnerId(String ownerId, Pageable pageable);

    Page<Api> findByStatusAndIsPublicTrue(ApiStatus status, Pageable pageable);

    @Query("SELECT a FROM Api a WHERE a.status = :status AND a.isPublic = true " +
           "AND (:category IS NULL OR LOWER(a.category) = LOWER(:category)) " +
           "AND (:search IS NULL OR LOWER(a.name) LIKE LOWER(CONCAT('%', :search, '%')) " +
           "     OR LOWER(a.description) LIKE LOWER(CONCAT('%', :search, '%')))")
    Page<Api> searchCatalog(
            @Param("status") ApiStatus status,
            @Param("category") String category,
            @Param("search") String search,
            Pageable pageable
    );
}

package com.apishield.registry.dto;

import com.fasterxml.jackson.annotation.JsonAlias;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class UpdateApiRequest {

    @NotBlank(message = "API name is required")
    @Size(max = 150, message = "API name must not exceed 150 characters")
    private String name;

    private String description;

    @NotBlank(message = "Category is required")
    @Size(max = 50, message = "Category must not exceed 50 characters")
    private String category;

    @NotBlank(message = "Target backend URL is required")
    @Size(max = 500, message = "Target backend URL must not exceed 500 characters")
    @JsonAlias({"targetBaseUrl", "target_backend_url"})
    private String targetBackendUrl;

    @JsonAlias({"version", "version_label"})
    private String versionLabel;

    private Boolean isPublic;
}

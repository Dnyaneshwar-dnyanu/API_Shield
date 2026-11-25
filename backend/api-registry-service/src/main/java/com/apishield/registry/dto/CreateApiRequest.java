package com.apishield.registry.dto;

import com.fasterxml.jackson.annotation.JsonAlias;
import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CreateApiRequest {

    @NotBlank(message = "API name is required")
    @Size(max = 150, message = "API name must not exceed 150 characters")
    private String name;

    @NotBlank(message = "API slug is required")
    @Size(max = 100, message = "Slug must not exceed 100 characters")
    @Pattern(regexp = "^[a-z0-9]+(?:-[a-z0-9]+)*$", message = "Slug must be lowercase alphanumeric with hyphens (e.g. weather-api)")
    private String slug;

    private String description;

    @NotBlank(message = "Category is required")
    @Size(max = 50, message = "Category must not exceed 50 characters")
    private String category;

    @NotBlank(message = "Target backend URL is required")
    @Size(max = 500, message = "Target backend URL must not exceed 500 characters")
    @JsonAlias({"targetBaseUrl", "target_backend_url"})
    private String targetBackendUrl;

    @Builder.Default
    @JsonAlias({"version", "version_label"})
    private String versionLabel = "v1";

    @JsonProperty("isPublic")
    @Builder.Default
    private Boolean isPublic = true;

    public Boolean isPublic() {
        return isPublic != null ? isPublic : true;
    }
}

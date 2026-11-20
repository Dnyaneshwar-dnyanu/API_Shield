package com.apishield.registry.dto;

import com.apishield.registry.model.HttpMethodType;
import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CreateEndpointRequest {

    @NotBlank(message = "Path pattern is required")
    @Size(max = 255, message = "Path pattern must not exceed 255 characters")
    @Pattern(regexp = "^/.*", message = "Path pattern must start with '/' (e.g. /current or /forecast/**)")
    private String pathPattern;

    @NotNull(message = "HTTP method is required")
    private HttpMethodType httpMethod;

    @Size(max = 255, message = "Description must not exceed 255 characters")
    private String description;

    @JsonProperty("isActive")
    @Builder.Default
    private Boolean isActive = true;

    public Boolean isActive() {
        return isActive != null ? isActive : true;
    }
}

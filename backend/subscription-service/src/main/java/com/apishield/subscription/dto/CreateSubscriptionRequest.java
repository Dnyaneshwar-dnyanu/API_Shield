package com.apishield.subscription.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CreateSubscriptionRequest {

    @NotBlank(message = "API ID is required")
    private String apiId;

    @NotBlank(message = "Plan ID is required")
    private String planId;
}

package com.apishield.userservice.dto.request;

import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DeactivateAccountRequest {

    @NotBlank(message = "Password confirmation is required to deactivate account.")
    private String password;
}

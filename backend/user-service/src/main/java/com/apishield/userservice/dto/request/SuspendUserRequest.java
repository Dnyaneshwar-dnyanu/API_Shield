package com.apishield.userservice.dto.request;

import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SuspendUserRequest {

    @Size(max = 255, message = "Suspension reason cannot exceed 255 characters.")
    private String reason;
}

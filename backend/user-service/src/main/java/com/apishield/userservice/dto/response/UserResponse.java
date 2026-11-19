package com.apishield.userservice.dto.response;

import com.apishield.userservice.enums.AccountStatus;
import com.apishield.userservice.enums.Role;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UserResponse {

    private UUID id;
    private String firstName;
    private String lastName;
    private String email;
    private Role role;
    private AccountStatus accountStatus;
    private boolean emailVerified;
    private Instant createdAt;
}

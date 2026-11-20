package com.apishield.userservice.service;

import com.apishield.userservice.dto.request.AdminUpdateUserRequest;
import com.apishield.userservice.dto.request.SuspendUserRequest;
import com.apishield.userservice.dto.request.UpdateUserRoleRequest;
import com.apishield.userservice.dto.response.PageResponse;
import com.apishield.userservice.dto.response.UserResponse;
import com.apishield.userservice.enums.AccountStatus;
import com.apishield.userservice.enums.Role;
import org.springframework.data.domain.Pageable;

import java.util.UUID;

public interface AdminUserService {

    PageResponse<UserResponse> listUsers(String search, Role role, AccountStatus status, Pageable pageable);

    UserResponse getUserById(UUID userId);

    UserResponse updateUserDetails(UUID userId, AdminUpdateUserRequest request, UUID adminId);

    UserResponse updateUserRole(UUID userId, UpdateUserRoleRequest request, UUID adminId);

    UserResponse activateUser(UUID userId, UUID adminId);

    UserResponse deactivateUser(UUID userId, UUID adminId);

    UserResponse suspendUser(UUID userId, SuspendUserRequest request, UUID adminId);

    UserResponse reactivateUser(UUID userId, UUID adminId);

    void deleteUser(UUID userId, UUID adminId);
}

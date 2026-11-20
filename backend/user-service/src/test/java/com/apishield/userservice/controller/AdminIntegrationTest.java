package com.apishield.userservice.controller;

import com.apishield.userservice.dto.request.AdminUpdateUserRequest;
import com.apishield.userservice.dto.request.SuspendUserRequest;
import com.apishield.userservice.dto.request.UpdateUserRoleRequest;
import com.apishield.userservice.entity.User;
import com.apishield.userservice.enums.AccountStatus;
import com.apishield.userservice.enums.Role;
import com.apishield.userservice.repository.AuditLogRepository;
import com.apishield.userservice.repository.RefreshTokenRepository;
import com.apishield.userservice.repository.UserRepository;
import com.apishield.userservice.security.JwtService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import tools.jackson.databind.ObjectMapper;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class AdminIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private RefreshTokenRepository refreshTokenRepository;

    @Autowired
    private AuditLogRepository auditLogRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private JwtService jwtService;

    private final ObjectMapper objectMapper = new ObjectMapper();

    @BeforeEach
    void setUp() {
        auditLogRepository.deleteAll();
        refreshTokenRepository.deleteAll();
        userRepository.deleteAll();
    }

    private User createTestUser(String email, Role role, AccountStatus status) {
        return userRepository.save(User.builder()
                .firstName("Test")
                .lastName("User")
                .email(email)
                .passwordHash(passwordEncoder.encode("Password123!"))
                .role(role)
                .accountStatus(status)
                .emailVerified(true)
                .build());
    }

    @Test
    @DisplayName("ADMIN role can list users with pagination and filtering")
    void testAdminCanListUsers() throws Exception {
        User admin = createTestUser("admin@apishield.io", Role.ADMIN, AccountStatus.ACTIVE);
        createTestUser("dev1@apishield.io", Role.DEVELOPER, AccountStatus.ACTIVE);
        createTestUser("dev2@apishield.io", Role.DEVELOPER, AccountStatus.ACTIVE);

        String adminToken = jwtService.generateToken(admin);

        mockMvc.perform(get("/api/v1/admin/users")
                        .header("Authorization", "Bearer " + adminToken)
                        .param("page", "0")
                        .param("size", "10")
                        .param("role", "DEVELOPER"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content", hasSize(2)))
                .andExpect(jsonPath("$.totalElements").value(2));
    }

    @Test
    @DisplayName("DEVELOPER and API_OWNER receive 403 Forbidden when accessing admin endpoints (RBAC)")
    void testNonAdminForbiddenFromAdminEndpoints() throws Exception {
        User developer = createTestUser("dev@apishield.io", Role.DEVELOPER, AccountStatus.ACTIVE);
        User apiOwner = createTestUser("owner@apishield.io", Role.API_OWNER, AccountStatus.ACTIVE);

        String devToken = jwtService.generateToken(developer);
        String ownerToken = jwtService.generateToken(apiOwner);

        // Developer forbidden
        mockMvc.perform(get("/api/v1/admin/users")
                        .header("Authorization", "Bearer " + devToken))
                .andExpect(status().isForbidden());

        // API Owner forbidden
        mockMvc.perform(get("/api/v1/admin/users")
                        .header("Authorization", "Bearer " + ownerToken))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("ADMIN can change a user's role and audit log is recorded")
    void testAdminUpdateUserRole() throws Exception {
        User admin = createTestUser("admin@apishield.io", Role.ADMIN, AccountStatus.ACTIVE);
        User target = createTestUser("target@apishield.io", Role.DEVELOPER, AccountStatus.ACTIVE);

        String adminToken = jwtService.generateToken(admin);

        mockMvc.perform(patch("/api/v1/admin/users/" + target.getId() + "/role")
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new UpdateUserRoleRequest(Role.API_OWNER))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.role").value("API_OWNER"));

        // Verify in DB
        User updated = userRepository.findById(target.getId()).orElseThrow();
        assertThat(updated.getRole()).isEqualTo(Role.API_OWNER);

        // Verify audit log
        assertThat(auditLogRepository.findAll()).hasSize(1);
        assertThat(auditLogRepository.findAll().get(0).getAction()).isEqualTo("ROLE_CHANGE");
    }

    @Test
    @DisplayName("ADMIN cannot change their own role")
    void testAdminCannotChangeOwnRole() throws Exception {
        User admin = createTestUser("admin@apishield.io", Role.ADMIN, AccountStatus.ACTIVE);
        String adminToken = jwtService.generateToken(admin);

        mockMvc.perform(patch("/api/v1/admin/users/" + admin.getId() + "/role")
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new UpdateUserRoleRequest(Role.DEVELOPER))))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value(containsString("cannot change their own role")));
    }

    @Test
    @DisplayName("ADMIN can suspend a user with a reason and view audit logs")
    void testAdminSuspendUser() throws Exception {
        User admin = createTestUser("admin@apishield.io", Role.ADMIN, AccountStatus.ACTIVE);
        User target = createTestUser("target@apishield.io", Role.DEVELOPER, AccountStatus.ACTIVE);

        String adminToken = jwtService.generateToken(admin);

        mockMvc.perform(post("/api/v1/admin/users/" + target.getId() + "/suspend")
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new SuspendUserRequest("Repeated API rate limit violations"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.accountStatus").value("SUSPENDED"));

        // Verify in DB
        User updated = userRepository.findById(target.getId()).orElseThrow();
        assertThat(updated.getAccountStatus()).isEqualTo(AccountStatus.SUSPENDED);
        assertThat(updated.getSuspensionReason()).isEqualTo("Repeated API rate limit violations");
        assertThat(updated.getSuspendedBy()).isEqualTo(admin.getId());

        // Verify audit log endpoint
        mockMvc.perform(get("/api/v1/admin/audit-logs")
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content", hasSize(1)))
                .andExpect(jsonPath("$.content[0].action").value("SUSPEND_USER"))
                .andExpect(jsonPath("$.content[0].reason").value("Repeated API rate limit violations"));
    }

    @Test
    @DisplayName("ADMIN can reactivate a suspended user")
    void testAdminReactivateUser() throws Exception {
        User admin = createTestUser("admin@apishield.io", Role.ADMIN, AccountStatus.ACTIVE);
        User target = createTestUser("target@apishield.io", Role.DEVELOPER, AccountStatus.SUSPENDED);

        String adminToken = jwtService.generateToken(admin);

        mockMvc.perform(post("/api/v1/admin/users/" + target.getId() + "/reactivate")
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.accountStatus").value("ACTIVE"));

        User reloaded = userRepository.findById(target.getId()).orElseThrow();
        assertThat(reloaded.getAccountStatus()).isEqualTo(AccountStatus.ACTIVE);
        assertThat(reloaded.getSuspensionReason()).isNull();
    }
}

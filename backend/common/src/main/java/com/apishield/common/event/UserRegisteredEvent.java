package com.apishield.common.event;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Builder;
import lombok.Getter;
import lombok.ToString;

import java.time.Instant;

/**
 * Domain event payload published when a user registers on the platform.
 */
@Getter
@ToString
@Builder
public class UserRegisteredEvent {

    @JsonProperty("userId")
    private final String userId;

    @JsonProperty("email")
    private final String email;

    @JsonProperty("firstName")
    private final String firstName;

    @JsonProperty("lastName")
    private final String lastName;

    @JsonProperty("role")
    private final String role;

    @JsonProperty("registeredAt")
    private final Instant registeredAt;

    @JsonCreator
    public UserRegisteredEvent(
            @JsonProperty("userId") String userId,
            @JsonProperty("email") String email,
            @JsonProperty("firstName") String firstName,
            @JsonProperty("lastName") String lastName,
            @JsonProperty("role") String role,
            @JsonProperty("registeredAt") Instant registeredAt) {
        this.userId = userId;
        this.email = email;
        this.firstName = firstName;
        this.lastName = lastName;
        this.role = role;
        this.registeredAt = registeredAt != null ? registeredAt : Instant.now();
    }
}

package com.apishield.apikey.service;

import com.apishield.apikey.dto.*;

import java.util.List;
import java.util.UUID;

public interface ApiKeyService {

    CreateKeyResponse generateKey(CreateKeyRequest request, String developerId);

    List<ApiKeyResponse> listKeys(String developerId);

    ApiKeyResponse getKeyById(UUID id, String developerId);

    ApiKeyResponse revokeKey(UUID id, String developerId, String reason);

    CreateKeyResponse rollKey(UUID id, String developerId);

    void revokeKeysBySubscriptionId(String subscriptionId, String reason);

    KeyValidationResponse verifyKey(VerifyKeyRequest request);
}

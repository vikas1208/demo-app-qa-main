package com.example.demo.contract;

import au.com.dius.pact.consumer.MockServer;
import au.com.dius.pact.consumer.dsl.PactDslJsonArray;
import au.com.dius.pact.consumer.dsl.PactDslWithProvider;
import au.com.dius.pact.consumer.junit5.PactConsumerTestExt;
import au.com.dius.pact.consumer.junit5.PactTestFor;
import au.com.dius.pact.core.model.PactSpecVersion;
import au.com.dius.pact.core.model.V4Pact;
import au.com.dius.pact.core.model.annotations.Pact;
import com.example.bff.adapter.out.claims.ApiClient;
import com.example.demo.adapter.out.claimsservice.ClaimsServiceAdapter;
import com.example.demo.application.auth.ServiceAccountTokenProvider;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mockito;

import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

@ExtendWith(PactConsumerTestExt.class)
@PactTestFor(providerName = "ClaimsService", pactVersion = PactSpecVersion.V4)
class ClaimsServicePactConsumerTest {

    private static final UUID CLAIM_ID = UUID.fromString("660e8400-e29b-41d4-a716-446655440001");

    @Pact(consumer = "BFFService")
    V4Pact listClaimsPact(PactDslWithProvider builder) {
        var body = new PactDslJsonArray()
                .object()
                .uuid("claimId", CLAIM_ID)
                .stringType("incidentDate", "2026-11-18")
                .stringType("description", "Vehicle accident")
                .numberType("claimAmount", 2500.0)
                .stringType("status", "SUBMITTED")
                .stringType("createdAt", "2026-11-19T10:15:30Z")
                .closeObject();

        return builder
                .given("a claimant with submitted claims")
                .uponReceiving("a request to list claims")
                .method("GET")
                .path("/api/claims")
                .headers(Map.of("Authorization", "Bearer user-jwt"))
                .willRespondWith()
                .status(200)
                .headers(Map.of("Content-Type", "application/json"))
                .body(body)
                .toPact(V4Pact.class);
    }

    @Test
    @PactTestFor(pactMethod = "listClaimsPact")
    void should_list_claims_from_claims_service(MockServer mockServer) {
        var tokenProvider = Mockito.mock(ServiceAccountTokenProvider.class);
        var apiClient = new ApiClient();
        apiClient.setBasePath(mockServer.getUrl());

        var adapter = new ClaimsServiceAdapter(
                apiClient,
                tokenProvider,
                new ObjectMapper().findAndRegisterModules()
        );

        var claims = adapter.listClaims("user-jwt", UUID.fromString("f4cb8793-c3d9-4faa-9ccb-7bd7245eb151"));

        assertThat(claims).hasSize(1);
        assertThat(claims.get(0).claimId()).isEqualTo(CLAIM_ID);
        assertThat(claims.get(0).status()).isEqualTo("SUBMITTED");
    }
}

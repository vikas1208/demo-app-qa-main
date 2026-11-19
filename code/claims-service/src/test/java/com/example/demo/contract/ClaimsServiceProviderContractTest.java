package com.example.demo.contract;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.boot.test.web.server.LocalServerPort;

import java.io.File;
import java.math.BigDecimal;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Instant;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("contract-test")
class ClaimsServiceProviderContractTest {

    private static final UUID TEST_USER_ID = UUID.fromString("550e8400-e29b-41d4-a716-446655440000");
    private static final UUID CLAIM_ID = UUID.fromString("660e8400-e29b-41d4-a716-446655440001");

    @Autowired
    private JdbcTemplate jdbcTemplate;

    private final ObjectMapper objectMapper = new ObjectMapper().findAndRegisterModules();
    private HttpClient httpClient;

    @LocalServerPort
    private int port;

    @BeforeEach
    void setUp() {
        httpClient = HttpClient.newHttpClient();
    }

    @BeforeEach
    void seedProviderState() {
        jdbcTemplate.update("DELETE FROM claims");
        jdbcTemplate.update("DELETE FROM users");
        jdbcTemplate.update(
                "INSERT INTO users (user_id, email, name, role, created_at) VALUES (?, ?, ?, ?, ?)",
                TEST_USER_ID, "contract-test@example.com", "Contract Test", "CLAIMANT", Instant.now()
        );
        jdbcTemplate.update(
                "INSERT INTO claims (claim_id, user_id, incident_date, incident_location, description, claim_amount, status, created_at, updated_at) "
                        + "VALUES (?, ?, DATE '2026-11-18', ?, ?, ?, ?, ?, ?)",
                CLAIM_ID, TEST_USER_ID, "Singapore", "Vehicle accident",
                new BigDecimal("2500.00"), "SUBMITTED", Instant.now(), Instant.now()
        );
    }

    @Test
    void should_satisfy_claims_list_consumer_contract() throws Exception {
        File pactFile = new File("../bff-service/pacts/BFFService-default.json");
        assertThat(pactFile).exists();

        JsonNode pact = objectMapper.readTree(pactFile);
        JsonNode provider = pact.path("provider");
        assertThat(provider.path("name").asText()).isEqualTo("default");

        JsonNode interaction = pact.path("interactions").get(0);
        assertThat(interaction.path("providerStates").get(0).path("name").asText())
                .isEqualTo("a claimant with submitted claims");

        JsonNode request = interaction.path("request");
        assertThat(request.path("method").asText()).isEqualTo("GET");
        assertThat(request.path("path").asText()).isEqualTo("/api/claims");

        var response = httpClient.send(
                HttpRequest.newBuilder()
                        .uri(URI.create("http://localhost:" + port + "/api/claims"))
                        .GET()
                        .build(),
                HttpResponse.BodyHandlers.ofString()
        );

        JsonNode expected = interaction.path("response");
        assertThat(response.statusCode()).isEqualTo(expected.path("status").asInt());
        assertThat(response.headers().firstValue("Content-Type")).isPresent();

        JsonNode body = objectMapper.readTree(response.body());
        assertThat(body.isArray()).isTrue();
        assertThat(body.get(0).path("claimId").asText()).isEqualTo(CLAIM_ID.toString());
        assertThat(body.get(0).path("incidentDate").asText()).isEqualTo("2026-11-18");
        assertThat(body.get(0).path("description").asText()).isEqualTo("Vehicle accident");
        assertThat(body.get(0).path("claimAmount").decimalValue()).isEqualByComparingTo("2500.0");
        assertThat(body.get(0).path("status").asText()).isEqualTo("SUBMITTED");
    }
}

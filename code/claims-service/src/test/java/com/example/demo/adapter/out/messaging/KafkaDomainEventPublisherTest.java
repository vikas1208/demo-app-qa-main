package com.example.demo.adapter.out.messaging;

import com.example.demo.domain.claim.ClaimStatus;
import com.example.demo.domain.claim.events.ClaimStatusChanged;
import com.example.demo.domain.claim.events.ClaimSubmitted;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.kafka.core.KafkaTemplate;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class KafkaDomainEventPublisherTest {

    @Mock
    private KafkaTemplate<String, String> kafkaTemplate;

    private ObjectMapper objectMapper;

    @Mock
    private KafkaMessageSchemaValidator schemaValidator;

    private KafkaDomainEventPublisher publisher;

    @BeforeEach
    void setUp() {
        objectMapper = new ObjectMapper();
        objectMapper.findAndRegisterModules();
        publisher = new KafkaDomainEventPublisher(kafkaTemplate, objectMapper, schemaValidator);
        lenient().doNothing().when(schemaValidator).validate(anyString(), anyString());
        lenient().when(kafkaTemplate.send(anyString(), anyString(), anyString()))
                .thenReturn(CompletableFuture.completedFuture(null));
    }

    // TC-077: Event published to Kafka with correct topic and message key
    @Test
    void should_PublishEventToKafka_When_ClaimSubmittedEventProvided() throws Exception {
        UUID claimId = UUID.randomUUID();
        var event = new ClaimSubmitted(
                UUID.randomUUID().toString(),
                claimId,
                UUID.randomUUID(),
                LocalDate.now(),
                BigDecimal.TEN,
                Instant.now()
        );

        publisher.publish(event);

        ArgumentCaptor<String> payloadCaptor = ArgumentCaptor.forClass(String.class);
        verify(kafkaTemplate).send(eq("claim-events"), eq(claimId.toString()), payloadCaptor.capture());
        JsonNode payload = objectMapper.readTree(payloadCaptor.getValue());
        assertThat(payload.path("eventType").asText()).isEqualTo("claim-submitted");
        assertThat(payload.path("payload").path("claimId").asText()).isEqualTo(claimId.toString());
        verify(schemaValidator).validate(payloadCaptor.getValue(), "ClaimSubmittedMessage");
    }

    // TC-077b: ClaimStatusChanged published with correct key
    @Test
    void should_PublishEventToKafka_When_ClaimStatusChangedEventProvided() throws Exception {
        UUID claimId = UUID.randomUUID();
        var event = new ClaimStatusChanged(
                UUID.randomUUID().toString(),
                claimId,
                ClaimStatus.SUBMITTED,
                ClaimStatus.UNDER_REVIEW,
                UUID.randomUUID(),
                Instant.now()
        );

        publisher.publish(event);

        ArgumentCaptor<String> payloadCaptor = ArgumentCaptor.forClass(String.class);
        verify(kafkaTemplate).send(eq("claim-events"), eq(claimId.toString()), payloadCaptor.capture());
        JsonNode payload = objectMapper.readTree(payloadCaptor.getValue());
        assertThat(payload.path("eventType").asText()).isEqualTo("claim-status-changed");
        assertThat(payload.path("payload").path("oldStatus").asText()).isEqualTo("SUBMITTED");
        assertThat(payload.path("payload").path("newStatus").asText()).isEqualTo("UNDER_REVIEW");
        verify(schemaValidator).validate(payloadCaptor.getValue(), "ClaimStatusChangedMessage");
    }

    // TC-NEW-002: Publisher propagates KafkaMessageValidationException when validator rejects missing required field
    @Test
    void should_PropagateValidationException_When_MissingRequiredField() {
        UUID claimId = UUID.randomUUID();
        var event = new ClaimSubmitted(UUID.randomUUID().toString(), claimId, UUID.randomUUID(),
                LocalDate.now(), BigDecimal.TEN, Instant.now());

        doThrow(new KafkaMessageValidationException("claimId: is missing but it is required"))
                .when(schemaValidator).validate(anyString(), eq("ClaimSubmittedMessage"));

        assertThatThrownBy(() -> publisher.publish(event))
                .isInstanceOf(KafkaMessageValidationException.class)
                .hasMessageContaining("claimId");
    }

    // TC-NEW-003: Publisher propagates KafkaMessageValidationException when validator rejects invalid enum value
    @Test
    void should_PropagateValidationException_When_InvalidStatus() {
        UUID claimId = UUID.randomUUID();
        var event = new ClaimStatusChanged(UUID.randomUUID().toString(), claimId,
                ClaimStatus.SUBMITTED, ClaimStatus.UNDER_REVIEW, UUID.randomUUID(), Instant.now());

        doThrow(new KafkaMessageValidationException("oldStatus: invalid enum value"))
                .when(schemaValidator).validate(anyString(), eq("ClaimStatusChangedMessage"));

        assertThatThrownBy(() -> publisher.publish(event))
                .isInstanceOf(KafkaMessageValidationException.class)
                .hasMessageContaining("oldStatus");
    }

    // TC-NEW-004: Publisher sends null userId as empty string when changedBy is null
    @Test
    void should_PublishEvenWithNullChangedBy_When_StatusChanged() throws Exception {
        UUID claimId = UUID.randomUUID();
        var event = new ClaimStatusChanged(
                UUID.randomUUID().toString(),
                claimId,
                ClaimStatus.SUBMITTED,
                ClaimStatus.APPROVED,
                null,
                Instant.now()
        );

        publisher.publish(event);

        ArgumentCaptor<String> payloadCaptor = ArgumentCaptor.forClass(String.class);
        verify(kafkaTemplate).send(eq("claim-events"), eq(claimId.toString()), payloadCaptor.capture());
        JsonNode payload = objectMapper.readTree(payloadCaptor.getValue());
        assertThat(payload.path("payload").has("userId")).isFalse();
    }

    // TC-NEW-005: Publisher rejects unknown domain events
    @Test
    void should_RejectUnknownDomainEvent_When_Published() {
        org.assertj.core.api.Assertions.assertThatThrownBy(() -> publisher.publish(new UnknownEvent()))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Unknown domain event type");
    }

    private record UnknownEvent() implements com.example.demo.domain.DomainEvent {
    }
}

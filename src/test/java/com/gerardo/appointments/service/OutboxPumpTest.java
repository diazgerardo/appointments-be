package com.gerardo.appointments.service;

import com.gerardo.appointments.domain.OutboxEvent;
import com.gerardo.appointments.repo.OutboxEventRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class OutboxPumpTest {

    @Mock
    private OutboxEventRepository outboxEventRepository;

    @InjectMocks
    private OutboxPump outboxPump;

    private OutboxEvent unprocessedEvent1;
    private OutboxEvent unprocessedEvent2;

    @BeforeEach
    void setUp() {
        unprocessedEvent1 = new OutboxEvent();
        unprocessedEvent1.setId("event1");
        unprocessedEvent1.setType("AppointmentCancelled");
        unprocessedEvent1.setPayloadJson("{\"appointmentId\":\"123\",\"reason\":\"test\"}");
        unprocessedEvent1.setCreatedAt(Instant.now().minusSeconds(60));
        unprocessedEvent1.setProcessedAt(null);

        unprocessedEvent2 = new OutboxEvent();
        unprocessedEvent2.setId("event2");
        unprocessedEvent2.setType("AppointmentCancelled");
        unprocessedEvent2.setPayloadJson("{\"appointmentId\":\"456\",\"reason\":\"test2\"}");
        unprocessedEvent2.setCreatedAt(Instant.now().minusSeconds(30));
        unprocessedEvent2.setProcessedAt(null);
    }

    @Test
    void testPumpCancelled_ProcessesUnprocessedEvents() {
        // Given
        List<OutboxEvent> unprocessedEvents = List.of(unprocessedEvent1, unprocessedEvent2);

        when(outboxEventRepository.findByProcessedAtIsNullAndType("AppointmentCancelled"))
                .thenReturn(unprocessedEvents);

        when(outboxEventRepository.save(any(OutboxEvent.class))).thenAnswer(invocation -> invocation.getArgument(0));

        // When
        outboxPump.pumpCancelled();

        // Then
        verify(outboxEventRepository).findByProcessedAtIsNullAndType("AppointmentCancelled");

        ArgumentCaptor<OutboxEvent> eventCaptor = ArgumentCaptor.forClass(OutboxEvent.class);
        verify(outboxEventRepository, times(2)).save(eventCaptor.capture());

        List<OutboxEvent> savedEvents = eventCaptor.getAllValues();
        assertEquals(2, savedEvents.size());

        // Verify both events were marked as processed
        assertNotNull(savedEvents.get(0).getProcessedAt());
        assertNotNull(savedEvents.get(1).getProcessedAt());
    }

    @Test
    void testPumpCancelled_NoUnprocessedEvents_DoesNothing() {
        // Given
        when(outboxEventRepository.findByProcessedAtIsNullAndType("AppointmentCancelled"))
                .thenReturn(new ArrayList<>());

        // When
        outboxPump.pumpCancelled();

        // Then
        verify(outboxEventRepository).findByProcessedAtIsNullAndType("AppointmentCancelled");
        verify(outboxEventRepository, never()).save(any(OutboxEvent.class));
    }

    @Test
    void testPumpCancelled_SetsProcessedAtTimestamp() {
        // Given
        List<OutboxEvent> unprocessedEvents = List.of(unprocessedEvent1);

        when(outboxEventRepository.findByProcessedAtIsNullAndType("AppointmentCancelled"))
                .thenReturn(unprocessedEvents);

        when(outboxEventRepository.save(any(OutboxEvent.class))).thenAnswer(invocation -> invocation.getArgument(0));

        Instant beforePump = Instant.now().minusSeconds(1);

        // When
        outboxPump.pumpCancelled();

        Instant afterPump = Instant.now().plusSeconds(1);

        // Then
        ArgumentCaptor<OutboxEvent> eventCaptor = ArgumentCaptor.forClass(OutboxEvent.class);
        verify(outboxEventRepository).save(eventCaptor.capture());

        OutboxEvent savedEvent = eventCaptor.getValue();
        assertNotNull(savedEvent.getProcessedAt());

        // Verify timestamp is within reasonable range (set during the pump operation)
        assertTrue(savedEvent.getProcessedAt().isAfter(beforePump));
        assertTrue(savedEvent.getProcessedAt().isBefore(afterPump));
    }

    @Test
    void testPumpCancelled_ProcessesOnlyAppointmentCancelledType() {
        // Given
        OutboxEvent otherTypeEvent = new OutboxEvent();
        otherTypeEvent.setId("event3");
        otherTypeEvent.setType("OtherEventType");
        otherTypeEvent.setPayloadJson("{\"data\":\"test\"}");
        otherTypeEvent.setCreatedAt(Instant.now());
        otherTypeEvent.setProcessedAt(null);

        // Only AppointmentCancelled type events should be returned
        List<OutboxEvent> unprocessedEvents = List.of(unprocessedEvent1);

        when(outboxEventRepository.findByProcessedAtIsNullAndType("AppointmentCancelled"))
                .thenReturn(unprocessedEvents);

        when(outboxEventRepository.save(any(OutboxEvent.class))).thenAnswer(invocation -> invocation.getArgument(0));

        // When
        outboxPump.pumpCancelled();

        // Then
        verify(outboxEventRepository).findByProcessedAtIsNullAndType("AppointmentCancelled");
        verify(outboxEventRepository, times(1)).save(any(OutboxEvent.class));

        ArgumentCaptor<OutboxEvent> eventCaptor = ArgumentCaptor.forClass(OutboxEvent.class);
        verify(outboxEventRepository).save(eventCaptor.capture());

        assertEquals("AppointmentCancelled", eventCaptor.getValue().getType());
    }

    @Test
    void testPumpCancelled_PreservesEventPayload() {
        // Given
        String originalPayload = "{\"appointmentId\":\"123\",\"professionalId\":\"prof1\",\"patientId\":\"pat1\"}";
        unprocessedEvent1.setPayloadJson(originalPayload);

        List<OutboxEvent> unprocessedEvents = List.of(unprocessedEvent1);

        when(outboxEventRepository.findByProcessedAtIsNullAndType("AppointmentCancelled"))
                .thenReturn(unprocessedEvents);

        when(outboxEventRepository.save(any(OutboxEvent.class))).thenAnswer(invocation -> invocation.getArgument(0));

        // When
        outboxPump.pumpCancelled();

        // Then
        ArgumentCaptor<OutboxEvent> eventCaptor = ArgumentCaptor.forClass(OutboxEvent.class);
        verify(outboxEventRepository).save(eventCaptor.capture());

        OutboxEvent savedEvent = eventCaptor.getValue();
        assertEquals(originalPayload, savedEvent.getPayloadJson());
        assertEquals("AppointmentCancelled", savedEvent.getType());
        assertEquals("event1", savedEvent.getId());
    }

    @Test
    void testPumpCancelled_HandlesMultipleEventsIndependently() {
        // Given
        OutboxEvent event3 = new OutboxEvent();
        event3.setId("event3");
        event3.setType("AppointmentCancelled");
        event3.setPayloadJson("{\"appointmentId\":\"789\",\"reason\":\"test3\"}");
        event3.setCreatedAt(Instant.now().minusSeconds(90));
        event3.setProcessedAt(null);

        List<OutboxEvent> unprocessedEvents = List.of(unprocessedEvent1, unprocessedEvent2, event3);

        when(outboxEventRepository.findByProcessedAtIsNullAndType("AppointmentCancelled"))
                .thenReturn(unprocessedEvents);

        when(outboxEventRepository.save(any(OutboxEvent.class))).thenAnswer(invocation -> invocation.getArgument(0));

        // When
        outboxPump.pumpCancelled();

        // Then
        ArgumentCaptor<OutboxEvent> eventCaptor = ArgumentCaptor.forClass(OutboxEvent.class);
        verify(outboxEventRepository, times(3)).save(eventCaptor.capture());

        List<OutboxEvent> savedEvents = eventCaptor.getAllValues();
        assertEquals(3, savedEvents.size());

        // All events should be processed
        for (OutboxEvent event : savedEvents) {
            assertNotNull(event.getProcessedAt());
        }
    }

    @Test
    void testPumpCancelled_DoesNotReprocessAlreadyProcessedEvents() {
        // Given
        // Event already processed
        OutboxEvent processedEvent = new OutboxEvent();
        processedEvent.setId("processed1");
        processedEvent.setType("AppointmentCancelled");
        processedEvent.setPayloadJson("{\"appointmentId\":\"999\",\"reason\":\"old\"}");
        processedEvent.setCreatedAt(Instant.now().minusSeconds(120));
        processedEvent.setProcessedAt(Instant.now().minusSeconds(60)); // Already processed

        // Repository should only return unprocessed events
        List<OutboxEvent> unprocessedEvents = List.of(unprocessedEvent1);

        when(outboxEventRepository.findByProcessedAtIsNullAndType("AppointmentCancelled"))
                .thenReturn(unprocessedEvents); // Does not include processedEvent

        when(outboxEventRepository.save(any(OutboxEvent.class))).thenAnswer(invocation -> invocation.getArgument(0));

        // When
        outboxPump.pumpCancelled();

        // Then
        verify(outboxEventRepository, times(1)).save(any(OutboxEvent.class));

        ArgumentCaptor<OutboxEvent> eventCaptor = ArgumentCaptor.forClass(OutboxEvent.class);
        verify(outboxEventRepository).save(eventCaptor.capture());

        // Only the unprocessed event should be saved
        assertEquals("event1", eventCaptor.getValue().getId());
    }
}

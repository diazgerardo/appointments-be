package com.gerardo.appointments.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.gerardo.appointments.domain.Appointment;
import com.gerardo.appointments.domain.AppointmentStatus;
import com.gerardo.appointments.domain.OutboxEvent;
import com.gerardo.appointments.repo.AppointmentRepository;
import com.gerardo.appointments.repo.OutboxEventRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AppointmentServiceTest {

    @Mock
    private AppointmentRepository appointmentRepository;

    @Mock
    private OutboxEventRepository outboxEventRepository;

    @InjectMocks
    private AppointmentService appointmentService;

    private String professionalId;
    private String patientId;
    private OffsetDateTime startTime;
    private OffsetDateTime endTime;
    private Instant startInstant;
    private Instant endInstant;

    @BeforeEach
    void setUp() {
        professionalId = "prof123";
        patientId = "pat456";
        startTime = OffsetDateTime.of(2025, 10, 25, 10, 0, 0, 0, ZoneOffset.UTC);
        endTime = OffsetDateTime.of(2025, 10, 25, 11, 0, 0, 0, ZoneOffset.UTC);
        startInstant = startTime.toInstant();
        endInstant = endTime.toInstant();
    }

    @Test
    void testBookAppointment_Success() {
        // Given
        when(appointmentRepository.existsByProfessionalIdAndStatusAndEndTsAfterAndStartTsBefore(
                eq(professionalId),
                eq(AppointmentStatus.SCHEDULED),
                eq(startInstant),
                eq(endInstant)
        )).thenReturn(false);

        Appointment savedAppointment = new Appointment();
        savedAppointment.setId("appt789");
        savedAppointment.setProfessionalId(professionalId);
        savedAppointment.setPatientId(patientId);
        savedAppointment.setStartTs(startInstant);
        savedAppointment.setEndTs(endInstant);
        savedAppointment.setStatus(AppointmentStatus.SCHEDULED);

        when(appointmentRepository.save(any(Appointment.class))).thenReturn(savedAppointment);

        // When
        Appointment result = appointmentService.book(professionalId, patientId, startTime, endTime);

        // Then
        assertNotNull(result);
        assertEquals("appt789", result.getId());
        assertEquals(professionalId, result.getProfessionalId());
        assertEquals(patientId, result.getPatientId());
        assertEquals(startInstant, result.getStartTs());
        assertEquals(endInstant, result.getEndTs());
        assertEquals(AppointmentStatus.SCHEDULED, result.getStatus());

        verify(appointmentRepository).existsByProfessionalIdAndStatusAndEndTsAfterAndStartTsBefore(
                professionalId, AppointmentStatus.SCHEDULED, startInstant, endInstant
        );
        verify(appointmentRepository).save(any(Appointment.class));
    }

    @Test
    void testBookAppointment_SlotNotAvailable_ThrowsException() {
        // Given
        when(appointmentRepository.existsByProfessionalIdAndStatusAndEndTsAfterAndStartTsBefore(
                eq(professionalId),
                eq(AppointmentStatus.SCHEDULED),
                eq(startInstant),
                eq(endInstant)
        )).thenReturn(true);

        // When & Then
        IllegalStateException exception = assertThrows(IllegalStateException.class, () ->
                appointmentService.book(professionalId, patientId, startTime, endTime)
        );

        assertEquals("slot not available", exception.getMessage());
        verify(appointmentRepository).existsByProfessionalIdAndStatusAndEndTsAfterAndStartTsBefore(
                professionalId, AppointmentStatus.SCHEDULED, startInstant, endInstant
        );
        verify(appointmentRepository, never()).save(any(Appointment.class));
    }

    @Test
    void testCancelAppointment_Success() {
        // Given
        String appointmentId = "appt789";
        String cancelReason = "Patient requested cancellation";

        Appointment appointment = new Appointment();
        appointment.setId(appointmentId);
        appointment.setProfessionalId(professionalId);
        appointment.setPatientId(patientId);
        appointment.setStartTs(startInstant);
        appointment.setEndTs(endInstant);
        appointment.setStatus(AppointmentStatus.SCHEDULED);

        when(appointmentRepository.findById(appointmentId)).thenReturn(Optional.of(appointment));
        when(appointmentRepository.save(any(Appointment.class))).thenReturn(appointment);
        when(outboxEventRepository.save(any(OutboxEvent.class))).thenReturn(new OutboxEvent());

        // Mock the static NotificationHub method to avoid UnsupportedOperationException
        try (MockedStatic<NotificationHub> mockedHub = mockStatic(NotificationHub.class)) {
            mockedHub.when(() -> NotificationHub.broadcastToPatient(anyString(), anyString(), anyString()))
                    .thenAnswer(invocation -> null);

            // When
            appointmentService.cancel(appointmentId, cancelReason);

            // Then
            assertTrue(appointment.isCancelled());
            assertEquals(cancelReason, appointment.getCancelReason());

            ArgumentCaptor<Appointment> appointmentCaptor = ArgumentCaptor.forClass(Appointment.class);
            verify(appointmentRepository).save(appointmentCaptor.capture());
            assertEquals(AppointmentStatus.CANCELLED, appointmentCaptor.getValue().getStatus());

            ArgumentCaptor<OutboxEvent> eventCaptor = ArgumentCaptor.forClass(OutboxEvent.class);
            verify(outboxEventRepository).save(eventCaptor.capture());

            OutboxEvent capturedEvent = eventCaptor.getValue();
            assertEquals("AppointmentCancelled", capturedEvent.getType());
            assertNotNull(capturedEvent.getPayloadJson());
            assertTrue(capturedEvent.getPayloadJson().contains(appointmentId));
            assertTrue(capturedEvent.getPayloadJson().contains(cancelReason));

            // Verify that NotificationHub.broadcastToPatient was called
            mockedHub.verify(() -> NotificationHub.broadcastToPatient(
                    eq(patientId),
                    eq("AppointmentCancelled"),
                    anyString()
            ));
        }
    }

    @Test
    void testCancelAppointment_AlreadyCancelled_NoAction() {
        // Given
        String appointmentId = "appt789";
        String cancelReason = "Already cancelled";

        Appointment appointment = new Appointment();
        appointment.setId(appointmentId);
        appointment.setProfessionalId(professionalId);
        appointment.setPatientId(patientId);
        appointment.setStartTs(startInstant);
        appointment.setEndTs(endInstant);
        appointment.setStatus(AppointmentStatus.CANCELLED);
        appointment.setCancelReason("Previous cancellation");

        when(appointmentRepository.findById(appointmentId)).thenReturn(Optional.of(appointment));

        // When
        appointmentService.cancel(appointmentId, cancelReason);

        // Then
        verify(appointmentRepository, never()).save(any(Appointment.class));
        verify(outboxEventRepository, never()).save(any(OutboxEvent.class));
    }

    @Test
    void testCancelAppointment_NotFound_ThrowsException() {
        // Given
        String appointmentId = "nonexistent";
        String cancelReason = "Cancellation reason";

        when(appointmentRepository.findById(appointmentId)).thenReturn(Optional.empty());

        // When & Then
        assertThrows(RuntimeException.class, () ->
                appointmentService.cancel(appointmentId, cancelReason)
        );

        verify(appointmentRepository, never()).save(any(Appointment.class));
        verify(outboxEventRepository, never()).save(any(OutboxEvent.class));
    }

    @Test
    void testBookAppointment_VerifyCorrectDataMapping() {
        // Given
        when(appointmentRepository.existsByProfessionalIdAndStatusAndEndTsAfterAndStartTsBefore(
                anyString(), any(AppointmentStatus.class), any(Instant.class), any(Instant.class)
        )).thenReturn(false);

        when(appointmentRepository.save(any(Appointment.class))).thenAnswer(invocation -> {
            Appointment arg = invocation.getArgument(0);
            arg.setId("generated-id");
            return arg;
        });

        // When
        Appointment result = appointmentService.book(professionalId, patientId, startTime, endTime);

        // Then
        ArgumentCaptor<Appointment> captor = ArgumentCaptor.forClass(Appointment.class);
        verify(appointmentRepository).save(captor.capture());

        Appointment capturedAppointment = captor.getValue();
        assertEquals(professionalId, capturedAppointment.getProfessionalId());
        assertEquals(patientId, capturedAppointment.getPatientId());
        assertEquals(startInstant, capturedAppointment.getStartTs());
        assertEquals(endInstant, capturedAppointment.getEndTs());
        assertEquals(AppointmentStatus.SCHEDULED, capturedAppointment.getStatus());
    }
}

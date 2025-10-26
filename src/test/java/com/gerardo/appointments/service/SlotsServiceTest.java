package com.gerardo.appointments.service;

import com.gerardo.appointments.domain.*;
import com.gerardo.appointments.dto.SlotViewDTO;
import com.gerardo.appointments.repo.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class SlotsServiceTest {

    @Mock
    private AvailabilityBlockRepo blockRepository;

    @Mock
    private AppointmentRepo appointmentRepository;

    @Mock
    private ProfessionalRepo professionalRepository;

    @Mock
    private PatientRepo patientRepository;

    @InjectMocks
    private SlotsService slotsService;

    private String professionalId;
    private String patientId;
    private OffsetDateTime startTime;
    private OffsetDateTime endTime;
    private Professional professional;
    private Patient patient;

    @BeforeEach
    void setUp() {
        professionalId = "prof123";
        patientId = "pat456";
        startTime = OffsetDateTime.of(2025, 10, 25, 9, 0, 0, 0, ZoneOffset.UTC);
        endTime = OffsetDateTime.of(2025, 10, 25, 12, 0, 0, 0, ZoneOffset.UTC);

        professional = new Professional();
        professional.setId(professionalId);
        professional.setFullName("Dr. John Smith");
        professional.setSpecialty("Cardiology");

        patient = new Patient();
        patient.setId(patientId);
        patient.setFullName("Jane Doe");
    }

    @Test
    void testProfessionalSlots_NoAppointments_ReturnsAllAvailableSlots() {
        // Given
        int slotMinutes = 30;
        String include = "all";

        AvailabilityBlock block = new AvailabilityBlock();
        block.setId("block123");
        block.setProfessionalId(professionalId);
        block.setStartTs(startTime.toInstant());
        block.setEndTs(endTime.toInstant());
        block.setOpen(true);
        block.setLocationType(LocationType.HOSPITAL);

        when(blockRepository.findByProfessionalIdAndEndTsAfterAndStartTsBefore(
                professionalId, startTime.toInstant(), endTime.toInstant()
        )).thenReturn(List.of(block));

        when(professionalRepository.findById(professionalId)).thenReturn(Optional.of(professional));

        when(appointmentRepository.findByProfessionalIdAndStatusAndEndTsAfterAndStartTsBefore(
                professionalId, AppointmentStatus.SCHEDULED, startTime.toInstant(), endTime.toInstant()
        )).thenReturn(new ArrayList<>());

        when(patientRepository.findAllById(anySet())).thenReturn(new ArrayList<>());

        // When
        List<SlotViewDTO> slots = slotsService.professionalSlots(professionalId, startTime, endTime, slotMinutes, include);

        // Then
        assertNotNull(slots);
        assertEquals(6, slots.size()); // 3 hours * 2 slots per hour

        for (SlotViewDTO slot : slots) {
            assertEquals(professionalId, slot.getProfessionalId());
            assertEquals("Dr. John Smith", slot.getProfessionalName());
            assertEquals("AVAILABLE", slot.getStatus());
            assertEquals(0, slot.getOverlapCount());
            assertEquals(LocationType.HOSPITAL, slot.getLocationType());
            assertTrue(slot.getBookings().isEmpty());
        }
    }

    @Test
    void testProfessionalSlots_WithAppointments_ReturnsBookedSlots() {
        // Given
        int slotMinutes = 60;
        String include = "all";

        AvailabilityBlock block = new AvailabilityBlock();
        block.setId("block123");
        block.setProfessionalId(professionalId);
        block.setStartTs(startTime.toInstant());
        block.setEndTs(endTime.toInstant());
        block.setOpen(true);
        block.setLocationType(LocationType.HOSPITAL);

        // Appointment at 10:00 - 11:00
        Appointment appointment = new Appointment();
        appointment.setId("appt123");
        appointment.setProfessionalId(professionalId);
        appointment.setPatientId(patientId);
        appointment.setStartTs(startTime.plusHours(1).toInstant());
        appointment.setEndTs(startTime.plusHours(2).toInstant());
        appointment.setStatus(AppointmentStatus.SCHEDULED);

        when(blockRepository.findByProfessionalIdAndEndTsAfterAndStartTsBefore(
                professionalId, startTime.toInstant(), endTime.toInstant()
        )).thenReturn(List.of(block));

        when(professionalRepository.findById(professionalId)).thenReturn(Optional.of(professional));

        when(appointmentRepository.findByProfessionalIdAndStatusAndEndTsAfterAndStartTsBefore(
                professionalId, AppointmentStatus.SCHEDULED, startTime.toInstant(), endTime.toInstant()
        )).thenReturn(List.of(appointment));

        when(patientRepository.findAllById(Set.of(patientId))).thenReturn(List.of(patient));

        // When
        List<SlotViewDTO> slots = slotsService.professionalSlots(professionalId, startTime, endTime, slotMinutes, include);

        // Then
        assertNotNull(slots);
        assertEquals(3, slots.size());

        // First slot: 9:00-10:00 (available)
        SlotViewDTO firstSlot = slots.getFirst();
        assertEquals("AVAILABLE", firstSlot.getStatus());
        assertEquals(0, firstSlot.getOverlapCount());

        // Second slot: 10:00-11:00 (booked)
        SlotViewDTO secondSlot = slots.get(1);
        assertEquals("BOOKED", secondSlot.getStatus());
        assertEquals(1, secondSlot.getOverlapCount());
        assertEquals(1, secondSlot.getBookings().size());
        assertEquals(patientId, secondSlot.getBookings().getFirst().getPatientId());
        assertEquals("Jane Doe", secondSlot.getBookings().getFirst().getPatientName());

        // Third slot: 11:00-12:00 (available)
        SlotViewDTO thirdSlot = slots.get(2);
        assertEquals("AVAILABLE", thirdSlot.getStatus());
        assertEquals(0, thirdSlot.getOverlapCount());
    }

    @Test
    void testProfessionalSlots_IncludeAvailable_OnlyReturnsAvailableSlots() {
        // Given
        int slotMinutes = 60;
        String include = "available";

        AvailabilityBlock block = new AvailabilityBlock();
        block.setId("block123");
        block.setProfessionalId(professionalId);
        block.setStartTs(startTime.toInstant());
        block.setEndTs(endTime.toInstant());
        block.setOpen(true);
        block.setLocationType(LocationType.HOSPITAL);

        Appointment appointment = new Appointment();
        appointment.setId("appt123");
        appointment.setProfessionalId(professionalId);
        appointment.setPatientId(patientId);
        appointment.setStartTs(startTime.plusHours(1).toInstant());
        appointment.setEndTs(startTime.plusHours(2).toInstant());
        appointment.setStatus(AppointmentStatus.SCHEDULED);

        when(blockRepository.findByProfessionalIdAndEndTsAfterAndStartTsBefore(
                professionalId, startTime.toInstant(), endTime.toInstant()
        )).thenReturn(List.of(block));

        when(professionalRepository.findById(professionalId)).thenReturn(Optional.of(professional));

        when(appointmentRepository.findByProfessionalIdAndStatusAndEndTsAfterAndStartTsBefore(
                professionalId, AppointmentStatus.SCHEDULED, startTime.toInstant(), endTime.toInstant()
        )).thenReturn(List.of(appointment));

        when(patientRepository.findAllById(anySet())).thenReturn(List.of(patient));

        // When
        List<SlotViewDTO> slots = slotsService.professionalSlots(professionalId, startTime, endTime, slotMinutes, include);

        // Then
        assertNotNull(slots);
        assertEquals(2, slots.size()); // Only 9:00-10:00 and 11:00-12:00

        for (SlotViewDTO slot : slots) {
            assertEquals("AVAILABLE", slot.getStatus());
            assertEquals(0, slot.getOverlapCount());
        }
    }

    @Test
    void testProfessionalSlots_IncludeBooked_OnlyReturnsBookedSlots() {
        // Given
        int slotMinutes = 60;
        String include = "booked";

        AvailabilityBlock block = new AvailabilityBlock();
        block.setId("block123");
        block.setProfessionalId(professionalId);
        block.setStartTs(startTime.toInstant());
        block.setEndTs(endTime.toInstant());
        block.setOpen(true);
        block.setLocationType(LocationType.HOSPITAL);

        Appointment appointment = new Appointment();
        appointment.setId("appt123");
        appointment.setProfessionalId(professionalId);
        appointment.setPatientId(patientId);
        appointment.setStartTs(startTime.plusHours(1).toInstant());
        appointment.setEndTs(startTime.plusHours(2).toInstant());
        appointment.setStatus(AppointmentStatus.SCHEDULED);

        when(blockRepository.findByProfessionalIdAndEndTsAfterAndStartTsBefore(
                professionalId, startTime.toInstant(), endTime.toInstant()
        )).thenReturn(List.of(block));

        when(professionalRepository.findById(professionalId)).thenReturn(Optional.of(professional));

        when(appointmentRepository.findByProfessionalIdAndStatusAndEndTsAfterAndStartTsBefore(
                professionalId, AppointmentStatus.SCHEDULED, startTime.toInstant(), endTime.toInstant()
        )).thenReturn(List.of(appointment));

        when(patientRepository.findAllById(Set.of(patientId))).thenReturn(List.of(patient));

        // When
        List<SlotViewDTO> slots = slotsService.professionalSlots(professionalId, startTime, endTime, slotMinutes, include);

        // Then
        assertNotNull(slots);
        assertEquals(1, slots.size()); // Only 10:00-11:00

        SlotViewDTO slot = slots.getFirst();
        assertEquals("BOOKED", slot.getStatus());
        assertEquals(1, slot.getOverlapCount());
        assertEquals(1, slot.getBookings().size());
    }

    @Test
    void testProfessionalSlots_MultipleBookingsInSameSlot_CountsOverlaps() {
        // Given
        int slotMinutes = 60;
        String include = "all";

        AvailabilityBlock block = new AvailabilityBlock();
        block.setId("block123");
        block.setProfessionalId(professionalId);
        block.setStartTs(startTime.toInstant());
        block.setEndTs(endTime.toInstant());
        block.setOpen(true);
        block.setLocationType(LocationType.HOSPITAL);

        // Two appointments in the same slot
        Appointment appointment1 = new Appointment();
        appointment1.setId("appt1");
        appointment1.setProfessionalId(professionalId);
        appointment1.setPatientId("pat1");
        appointment1.setStartTs(startTime.plusHours(1).toInstant());
        appointment1.setEndTs(startTime.plusHours(2).toInstant());
        appointment1.setStatus(AppointmentStatus.SCHEDULED);

        Appointment appointment2 = new Appointment();
        appointment2.setId("appt2");
        appointment2.setProfessionalId(professionalId);
        appointment2.setPatientId("pat2");
        appointment2.setStartTs(startTime.plusHours(1).toInstant());
        appointment2.setEndTs(startTime.plusHours(2).toInstant());
        appointment2.setStatus(AppointmentStatus.SCHEDULED);

        Patient patient1 = new Patient();
        patient1.setId("pat1");
        patient1.setFullName("Patient One");

        Patient patient2 = new Patient();
        patient2.setId("pat2");
        patient2.setFullName("Patient Two");

        when(blockRepository.findByProfessionalIdAndEndTsAfterAndStartTsBefore(
                professionalId, startTime.toInstant(), endTime.toInstant()
        )).thenReturn(List.of(block));

        when(professionalRepository.findById(professionalId)).thenReturn(Optional.of(professional));

        when(appointmentRepository.findByProfessionalIdAndStatusAndEndTsAfterAndStartTsBefore(
                professionalId, AppointmentStatus.SCHEDULED, startTime.toInstant(), endTime.toInstant()
        )).thenReturn(List.of(appointment1, appointment2));

        when(patientRepository.findAllById(anySet())).thenReturn(List.of(patient1, patient2));

        // When
        List<SlotViewDTO> slots = slotsService.professionalSlots(professionalId, startTime, endTime, slotMinutes, include);

        // Then
        assertNotNull(slots);
        assertEquals(3, slots.size());

        // Second slot should have 2 overlapping bookings
        SlotViewDTO secondSlot = slots.get(1);
        assertEquals("BOOKED", secondSlot.getStatus());
        assertEquals(2, secondSlot.getOverlapCount());
        assertEquals(2, secondSlot.getBookings().size());
    }

    @Test
    void testPatientSlots_ReturnsPatientAppointments() {
        // Given
        int slotMinutes = 30;

        // Patient has one appointment
        Appointment appointment = new Appointment();
        appointment.setId("appt123");
        appointment.setProfessionalId(professionalId);
        appointment.setPatientId(patientId);
        appointment.setStartTs(startTime.plusHours(1).toInstant());
        appointment.setEndTs(startTime.plusHours(2).toInstant());
        appointment.setStatus(AppointmentStatus.SCHEDULED);

        AvailabilityBlock block = new AvailabilityBlock();
        block.setId("block123");
        block.setProfessionalId(professionalId);
        block.setStartTs(startTime.toInstant());
        block.setEndTs(endTime.toInstant());
        block.setOpen(true);
        block.setLocationType(LocationType.PRIVATE);

        when(appointmentRepository.findByPatientIdAndStatusAndEndTsAfterAndStartTsBefore(
                patientId, AppointmentStatus.SCHEDULED, startTime.toInstant(), endTime.toInstant()
        )).thenReturn(List.of(appointment));

        when(professionalRepository.findAllById(Set.of(professionalId))).thenReturn(List.of(professional));

        when(blockRepository.findByProfessionalIdAndEndTsAfterAndStartTsBefore(
                professionalId, appointment.getStartTs(), appointment.getEndTs()
        )).thenReturn(List.of(block));

        // When
        List<SlotViewDTO> slots = slotsService.patientSlots(patientId, startTime, endTime, slotMinutes);

        // Then
        assertNotNull(slots);
        assertEquals(1, slots.size());

        SlotViewDTO slot = slots.getFirst();
        assertEquals(professionalId, slot.getProfessionalId());
        assertEquals("Dr. John Smith", slot.getProfessionalName());
        assertEquals("BOOKED", slot.getStatus());
        assertEquals(1, slot.getOverlapCount());
        assertEquals(LocationType.PRIVATE, slot.getLocationType());
        assertEquals(1, slot.getBookings().size());
        assertEquals("appt123", slot.getBookings().getFirst().getAppointmentId());
    }

    @Test
    void testPatientSlots_NoAppointments_ReturnsEmptyList() {
        // Given
        int slotMinutes = 30;

        when(appointmentRepository.findByPatientIdAndStatusAndEndTsAfterAndStartTsBefore(
                patientId, AppointmentStatus.SCHEDULED, startTime.toInstant(), endTime.toInstant()
        )).thenReturn(new ArrayList<>());

        when(professionalRepository.findAllById(anySet())).thenReturn(new ArrayList<>());

        // When
        List<SlotViewDTO> slots = slotsService.patientSlots(patientId, startTime, endTime, slotMinutes);

        // Then
        assertNotNull(slots);
        assertTrue(slots.isEmpty());
    }

    @Test
    void testPatientSlots_NoAvailabilityBlock_DefaultsToHospital() {
        // Given
        int slotMinutes = 30;

        Appointment appointment = new Appointment();
        appointment.setId("appt123");
        appointment.setProfessionalId(professionalId);
        appointment.setPatientId(patientId);
        appointment.setStartTs(startTime.plusHours(1).toInstant());
        appointment.setEndTs(startTime.plusHours(2).toInstant());
        appointment.setStatus(AppointmentStatus.SCHEDULED);

        when(appointmentRepository.findByPatientIdAndStatusAndEndTsAfterAndStartTsBefore(
                patientId, AppointmentStatus.SCHEDULED, startTime.toInstant(), endTime.toInstant()
        )).thenReturn(List.of(appointment));

        when(professionalRepository.findAllById(Set.of(professionalId))).thenReturn(List.of(professional));

        when(blockRepository.findByProfessionalIdAndEndTsAfterAndStartTsBefore(
                professionalId, appointment.getStartTs(), appointment.getEndTs()
        )).thenReturn(new ArrayList<>()); // No availability blocks

        // When
        List<SlotViewDTO> slots = slotsService.patientSlots(patientId, startTime, endTime, slotMinutes);

        // Then
        assertNotNull(slots);
        assertEquals(1, slots.size());
        assertEquals(LocationType.HOSPITAL, slots.getFirst().getLocationType()); // Default
    }

    @Test
    void testProfessionalSlots_ProfessionalNotFound_NullName() {
        // Given
        int slotMinutes = 30;
        String include = "all";

        AvailabilityBlock block = new AvailabilityBlock();
        block.setId("block123");
        block.setProfessionalId(professionalId);
        block.setStartTs(startTime.toInstant());
        block.setEndTs(endTime.toInstant());
        block.setOpen(true);

        when(blockRepository.findByProfessionalIdAndEndTsAfterAndStartTsBefore(
                professionalId, startTime.toInstant(), endTime.toInstant()
        )).thenReturn(List.of(block));

        when(professionalRepository.findById(professionalId)).thenReturn(Optional.empty());

        when(appointmentRepository.findByProfessionalIdAndStatusAndEndTsAfterAndStartTsBefore(
                professionalId, AppointmentStatus.SCHEDULED, startTime.toInstant(), endTime.toInstant()
        )).thenReturn(new ArrayList<>());

        when(patientRepository.findAllById(anySet())).thenReturn(new ArrayList<>());

        // When
        List<SlotViewDTO> slots = slotsService.professionalSlots(professionalId, startTime, endTime, slotMinutes, include);

        // Then
        assertNotNull(slots);
        assertFalse(slots.isEmpty());
        assertNull(slots.getFirst().getProfessionalName());
    }
}

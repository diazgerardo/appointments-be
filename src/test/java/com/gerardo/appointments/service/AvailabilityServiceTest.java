package com.gerardo.appointments.service;

import com.gerardo.appointments.domain.Appointment;
import com.gerardo.appointments.domain.AppointmentStatus;
import com.gerardo.appointments.domain.AvailabilityBlock;
import com.gerardo.appointments.dto.SlotDTO;
import com.gerardo.appointments.repo.AppointmentRepository;
import com.gerardo.appointments.repo.AvailabilityBlockRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Duration;
import java.time.Instant;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AvailabilityServiceTest {

    @Mock
    private AvailabilityBlockRepository blockRepository;

    @Mock
    private AppointmentRepository appointmentRepository;

    @InjectMocks
    private AvailabilityService availabilityService;

    private String professionalId;
    private OffsetDateTime startTime;
    private OffsetDateTime endTime;
    private Instant startInstant;
    private Instant endInstant;

    @BeforeEach
    void setUp() {
        professionalId = "prof123";
        startTime = OffsetDateTime.of(2025, 10, 25, 9, 0, 0, 0, ZoneOffset.UTC);
        endTime = OffsetDateTime.of(2025, 10, 25, 17, 0, 0, 0, ZoneOffset.UTC);
        startInstant = startTime.toInstant();
        endInstant = endTime.toInstant();
    }

    @Test
    void testAddOpenBlock_Success() {
        // Given
        String reason = "Regular consultation hours";
        AvailabilityBlock savedBlock = new AvailabilityBlock();
        savedBlock.setId("block123");
        savedBlock.setProfessionalId(professionalId);
        savedBlock.setStartTs(startInstant);
        savedBlock.setEndTs(endInstant);
        savedBlock.setReason(reason);
        savedBlock.setOpen(true);

        when(blockRepository.save(any(AvailabilityBlock.class))).thenReturn(savedBlock);

        // When
        AvailabilityBlock result = availabilityService.addOpenBlock(professionalId, startTime, endTime, reason);

        // Then
        assertNotNull(result);
        assertEquals("block123", result.getId());
        assertEquals(professionalId, result.getProfessionalId());
        assertEquals(startInstant, result.getStartTs());
        assertEquals(endInstant, result.getEndTs());
        assertEquals(reason, result.getReason());
        assertTrue(result.isOpen());

        ArgumentCaptor<AvailabilityBlock> captor = ArgumentCaptor.forClass(AvailabilityBlock.class);
        verify(blockRepository).save(captor.capture());

        AvailabilityBlock capturedBlock = captor.getValue();
        assertEquals(professionalId, capturedBlock.getProfessionalId());
        assertEquals(startInstant, capturedBlock.getStartTs());
        assertEquals(endInstant, capturedBlock.getEndTs());
        assertEquals(reason, capturedBlock.getReason());
        assertTrue(capturedBlock.isOpen());
    }

    @Test
    void testRemoveBlock_Success() {
        // Given
        String blockId = "block123";

        // When
        availabilityService.removeBlock(blockId);

        // Then
        verify(blockRepository).deleteById(blockId);
    }

    @Test
    void testComputeSlots_NoAppointments_ReturnsAllSlots() {
        // Given
        Duration slotDuration = Duration.ofMinutes(30);

        AvailabilityBlock block = new AvailabilityBlock();
        block.setId("block123");
        block.setProfessionalId(professionalId);
        block.setStartTs(startInstant);
        block.setEndTs(endInstant);
        block.setOpen(true);

        when(blockRepository.findByProfessionalIdAndEndTsAfterAndStartTsBefore(
                professionalId, startInstant, endInstant
        )).thenReturn(List.of(block));

        when(appointmentRepository.findByProfessionalIdAndStatusAndEndTsAfterAndStartTsBefore(
                professionalId, AppointmentStatus.SCHEDULED, startInstant, endInstant
        )).thenReturn(new ArrayList<>());

        // When
        List<SlotDTO> slots = availabilityService.computeSlots(professionalId, startTime, endTime, slotDuration);

        // Then
        assertNotNull(slots);
        assertFalse(slots.isEmpty());
        // 8 hours * 2 slots per hour = 16 slots
        assertEquals(16, slots.size());

        // Verify first slot
        SlotDTO firstSlot = slots.get(0);
        assertEquals(startTime, firstSlot.getStartTs());
        assertEquals(startTime.plus(slotDuration), firstSlot.getEndTs());

        // Verify last slot
        SlotDTO lastSlot = slots.get(slots.size() - 1);
        assertEquals(endTime.minus(slotDuration), lastSlot.getStartTs());
        assertEquals(endTime, lastSlot.getEndTs());
    }

    @Test
    void testComputeSlots_WithAppointments_ExcludesBookedSlots() {
        // Given
        Duration slotDuration = Duration.ofMinutes(60);

        // Availability block: 9:00 - 12:00
        OffsetDateTime blockStart = OffsetDateTime.of(2025, 10, 25, 9, 0, 0, 0, ZoneOffset.UTC);
        OffsetDateTime blockEnd = OffsetDateTime.of(2025, 10, 25, 12, 0, 0, 0, ZoneOffset.UTC);

        AvailabilityBlock block = new AvailabilityBlock();
        block.setId("block123");
        block.setProfessionalId(professionalId);
        block.setStartTs(blockStart.toInstant());
        block.setEndTs(blockEnd.toInstant());
        block.setOpen(true);

        // Appointment: 10:00 - 11:00
        Appointment appointment = new Appointment();
        appointment.setId("appt123");
        appointment.setProfessionalId(professionalId);
        appointment.setPatientId("pat456");
        appointment.setStartTs(blockStart.plusHours(1).toInstant());
        appointment.setEndTs(blockStart.plusHours(2).toInstant());
        appointment.setStatus(AppointmentStatus.SCHEDULED);

        when(blockRepository.findByProfessionalIdAndEndTsAfterAndStartTsBefore(
                professionalId, blockStart.toInstant(), blockEnd.toInstant()
        )).thenReturn(List.of(block));

        when(appointmentRepository.findByProfessionalIdAndStatusAndEndTsAfterAndStartTsBefore(
                professionalId, AppointmentStatus.SCHEDULED, blockStart.toInstant(), blockEnd.toInstant()
        )).thenReturn(List.of(appointment));

        // When
        List<SlotDTO> slots = availabilityService.computeSlots(professionalId, blockStart, blockEnd, slotDuration);

        // Then
        assertNotNull(slots);
        // Should have 2 slots: 9:00-10:00 and 11:00-12:00 (10:00-11:00 is booked)
        assertEquals(2, slots.size());

        SlotDTO firstSlot = slots.get(0);
        assertEquals(blockStart, firstSlot.getStartTs());
        assertEquals(blockStart.plusHours(1), firstSlot.getEndTs());

        SlotDTO secondSlot = slots.get(1);
        assertEquals(blockStart.plusHours(2), secondSlot.getStartTs());
        assertEquals(blockEnd, secondSlot.getEndTs());
    }

    @Test
    void testComputeSlots_NoOpenBlocks_ReturnsEmptyList() {
        // Given
        Duration slotDuration = Duration.ofMinutes(30);

        AvailabilityBlock closedBlock = new AvailabilityBlock();
        closedBlock.setId("block123");
        closedBlock.setProfessionalId(professionalId);
        closedBlock.setStartTs(startInstant);
        closedBlock.setEndTs(endInstant);
        closedBlock.setOpen(false); // Closed block

        when(blockRepository.findByProfessionalIdAndEndTsAfterAndStartTsBefore(
                professionalId, startInstant, endInstant
        )).thenReturn(List.of(closedBlock));

        when(appointmentRepository.findByProfessionalIdAndStatusAndEndTsAfterAndStartTsBefore(
                professionalId, AppointmentStatus.SCHEDULED, startInstant, endInstant
        )).thenReturn(new ArrayList<>());

        // When
        List<SlotDTO> slots = availabilityService.computeSlots(professionalId, startTime, endTime, slotDuration);

        // Then
        assertNotNull(slots);
        assertTrue(slots.isEmpty());
    }

    @Test
    void testComputeSlots_MultipleBlocks_MergesSlots() {
        // Given
        Duration slotDuration = Duration.ofMinutes(30);

        // First block: 9:00 - 12:00
        OffsetDateTime block1Start = OffsetDateTime.of(2025, 10, 25, 9, 0, 0, 0, ZoneOffset.UTC);
        OffsetDateTime block1End = OffsetDateTime.of(2025, 10, 25, 12, 0, 0, 0, ZoneOffset.UTC);

        AvailabilityBlock block1 = new AvailabilityBlock();
        block1.setId("block1");
        block1.setProfessionalId(professionalId);
        block1.setStartTs(block1Start.toInstant());
        block1.setEndTs(block1End.toInstant());
        block1.setOpen(true);

        // Second block: 14:00 - 17:00
        OffsetDateTime block2Start = OffsetDateTime.of(2025, 10, 25, 14, 0, 0, 0, ZoneOffset.UTC);
        OffsetDateTime block2End = OffsetDateTime.of(2025, 10, 25, 17, 0, 0, 0, ZoneOffset.UTC);

        AvailabilityBlock block2 = new AvailabilityBlock();
        block2.setId("block2");
        block2.setProfessionalId(professionalId);
        block2.setStartTs(block2Start.toInstant());
        block2.setEndTs(block2End.toInstant());
        block2.setOpen(true);

        OffsetDateTime queryStart = OffsetDateTime.of(2025, 10, 25, 8, 0, 0, 0, ZoneOffset.UTC);
        OffsetDateTime queryEnd = OffsetDateTime.of(2025, 10, 25, 18, 0, 0, 0, ZoneOffset.UTC);

        when(blockRepository.findByProfessionalIdAndEndTsAfterAndStartTsBefore(
                professionalId, queryStart.toInstant(), queryEnd.toInstant()
        )).thenReturn(List.of(block1, block2));

        when(appointmentRepository.findByProfessionalIdAndStatusAndEndTsAfterAndStartTsBefore(
                professionalId, AppointmentStatus.SCHEDULED, queryStart.toInstant(), queryEnd.toInstant()
        )).thenReturn(new ArrayList<>());

        // When
        List<SlotDTO> slots = availabilityService.computeSlots(professionalId, queryStart, queryEnd, slotDuration);

        // Then
        assertNotNull(slots);
        // Block1: 3 hours * 2 = 6 slots
        // Block2: 3 hours * 2 = 6 slots
        // Total: 12 slots
        assertEquals(12, slots.size());
    }

    @Test
    void testComputeSlots_PartialOverlap_HandlesCorrectly() {
        // Given
        Duration slotDuration = Duration.ofMinutes(30);

        // Availability block: 9:00 - 12:00
        OffsetDateTime blockStart = OffsetDateTime.of(2025, 10, 25, 9, 0, 0, 0, ZoneOffset.UTC);
        OffsetDateTime blockEnd = OffsetDateTime.of(2025, 10, 25, 12, 0, 0, 0, ZoneOffset.UTC);

        AvailabilityBlock block = new AvailabilityBlock();
        block.setId("block123");
        block.setProfessionalId(professionalId);
        block.setStartTs(blockStart.toInstant());
        block.setEndTs(blockEnd.toInstant());
        block.setOpen(true);

        // Appointment partially overlaps slot: 9:15 - 10:15
        Appointment appointment = new Appointment();
        appointment.setId("appt123");
        appointment.setProfessionalId(professionalId);
        appointment.setPatientId("pat456");
        appointment.setStartTs(blockStart.plusMinutes(15).toInstant());
        appointment.setEndTs(blockStart.plusMinutes(75).toInstant());
        appointment.setStatus(AppointmentStatus.SCHEDULED);

        when(blockRepository.findByProfessionalIdAndEndTsAfterAndStartTsBefore(
                professionalId, blockStart.toInstant(), blockEnd.toInstant()
        )).thenReturn(List.of(block));

        when(appointmentRepository.findByProfessionalIdAndStatusAndEndTsAfterAndStartTsBefore(
                professionalId, AppointmentStatus.SCHEDULED, blockStart.toInstant(), blockEnd.toInstant()
        )).thenReturn(List.of(appointment));

        // When
        List<SlotDTO> slots = availabilityService.computeSlots(professionalId, blockStart, blockEnd, slotDuration);

        // Then
        assertNotNull(slots);
        // 9:00-9:30 overlaps (excluded)
        // 9:30-10:00 overlaps (excluded)
        // 10:00-10:30 overlaps (excluded)
        // 10:30-11:00 available
        // 11:00-11:30 available
        // 11:30-12:00 available
        assertEquals(3, slots.size());
    }
}

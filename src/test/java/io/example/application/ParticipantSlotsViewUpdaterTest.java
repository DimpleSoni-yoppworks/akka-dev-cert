package io.example.application;

import io.example.application.ParticipantSlotEntity.Event.*;
import io.example.domain.Participant.ParticipantType;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

public class ParticipantSlotsViewUpdaterTest {

    private final String slotId = "test-slot";
    private final String participantId = "student1";
    private final String bookingId = "booking1";

    @Test
    void testMarkedAvailableCreatesCorrectRow() {
        var event = new MarkedAvailable(slotId, participantId, ParticipantType.STUDENT);

        var row = switch (event) {
            case MarkedAvailable e -> new ParticipantSlotsView.SlotRow(
                    e.slotId(), e.participantId(), e.participantType().name(), "", "AVAILABLE");
        };

        assertEquals("AVAILABLE", row.status());
        assertEquals(slotId, row.slotId());
        assertEquals(participantId, row.participantId());
        assertEquals("STUDENT", row.participantType());
    }

    @Test
    void testUnmarkedAvailableCreatesCorrectRow() {
        var event = new ParticipantSlotEntity.Event.UnmarkedAvailable(slotId, participantId, ParticipantType.STUDENT);

        var row = switch (event) {
            case ParticipantSlotEntity.Event.UnmarkedAvailable e ->
                new ParticipantSlotsView.SlotRow(
                        e.slotId(), e.participantId(), e.participantType().name(), "", "UNAVAILABLE");
        };

        assertEquals("UNAVAILABLE", row.status());
        assertEquals(slotId, row.slotId());
        assertEquals(participantId, row.participantId());
        assertEquals("STUDENT", row.participantType());
    }

    @Test
    void testBookedCreatesCorrectRow() {
        var event = new Booked(slotId, participantId, ParticipantType.STUDENT, bookingId);

        var row = switch (event) {
            case Booked e -> new ParticipantSlotsView.SlotRow(
                    e.slotId(), e.participantId(), e.participantType().name(), e.bookingId(), "BOOKED");
        };

        assertEquals("BOOKED", row.status());
        assertEquals(slotId, row.slotId());
        assertEquals(participantId, row.participantId());
        assertEquals("STUDENT", row.participantType());
        assertEquals(bookingId, row.bookingId());
    }

    @Test
    void testCanceledCreatesCorrectRow() {
        var event = new Canceled(slotId, participantId, ParticipantType.STUDENT, bookingId);

        var row = switch (event) {
            case Canceled e -> new ParticipantSlotsView.SlotRow(
                    e.slotId(), e.participantId(), e.participantType().name(), e.bookingId(), "CANCELED");
        };

        assertEquals("CANCELED", row.status());
        assertEquals(slotId, row.slotId());
        assertEquals(participantId, row.participantId());
        assertEquals("STUDENT", row.participantType());
        assertEquals(bookingId, row.bookingId());
    }
}

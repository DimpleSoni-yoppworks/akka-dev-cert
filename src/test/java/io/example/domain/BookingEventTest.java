package io.example.domain;

import io.example.domain.Participant.ParticipantType;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

public class BookingEventTest {

    @Test
    void testParticipantMarkedAvailableFields() {
        var event = new BookingEvent.ParticipantMarkedAvailable("slot1", "student1", ParticipantType.STUDENT);

        assertEquals("slot1", event.slotId());
        assertEquals("student1", event.participantId());
        assertEquals(ParticipantType.STUDENT, event.participantType());
    }

    @Test
    void testParticipantUnmarkedAvailableFields() {
        var event = new BookingEvent.ParticipantUnmarkedAvailable("slot1", "student1", ParticipantType.STUDENT);

        assertEquals("slot1", event.slotId());
        assertEquals("student1", event.participantId());
        assertEquals(ParticipantType.STUDENT, event.participantType());
    }

    @Test
    void testParticipantBookedFields() {
        var event = new BookingEvent.ParticipantBooked("slot1", "student1", ParticipantType.STUDENT, "booking1");

        assertEquals("slot1", event.slotId());
        assertEquals("student1", event.participantId());
        assertEquals(ParticipantType.STUDENT, event.participantType());
        assertEquals("booking1", event.bookingId());
    }

    @Test
    void testParticipantCanceledFields() {
        var event = new BookingEvent.ParticipantCanceled("slot1", "student1", ParticipantType.STUDENT, "booking1");

        assertEquals("slot1", event.slotId());
        assertEquals("student1", event.participantId());
        assertEquals(ParticipantType.STUDENT, event.participantType());
        assertEquals("booking1", event.bookingId());
    }
}

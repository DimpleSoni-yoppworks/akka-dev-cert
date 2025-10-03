package io.example.domain;

import io.example.domain.Participant.ParticipantType;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.HashSet;

import static org.junit.jupiter.api.Assertions.*;

public class TimeslotTest {

    private Timeslot timeslot;

    @BeforeEach
    void setup() {
        timeslot = new Timeslot(new HashSet<>(), new HashSet<>());
    }

    @Test
    void testReserveAddsParticipant() {
        BookingEvent.ParticipantMarkedAvailable event = new BookingEvent.ParticipantMarkedAvailable("slot1", "student1",
                ParticipantType.STUDENT);
        Timeslot updated = timeslot.reserve(event);

        assertTrue(updated.isWaiting("student1", ParticipantType.STUDENT));
    }

    @Test
    void testUnreserveRemovesParticipant() {
        BookingEvent.ParticipantMarkedAvailable markEvent = new BookingEvent.ParticipantMarkedAvailable("slot1",
                "student1", ParticipantType.STUDENT);
        Timeslot reserved = timeslot.reserve(markEvent);

        BookingEvent.ParticipantUnmarkedAvailable unmarkEvent = new BookingEvent.ParticipantUnmarkedAvailable("slot1",
                "student1", ParticipantType.STUDENT);
        Timeslot updated = reserved.unreserve(unmarkEvent);

        assertFalse(updated.isWaiting("student1", ParticipantType.STUDENT));
    }

    @Test
    void testBookMovesParticipantToBooking() {
        BookingEvent.ParticipantMarkedAvailable markEvent = new BookingEvent.ParticipantMarkedAvailable("slot1",
                "student1", ParticipantType.STUDENT);
        Timeslot reserved = timeslot.reserve(markEvent);

        BookingEvent.ParticipantBooked bookEvent = new BookingEvent.ParticipantBooked("slot1", "student1",
                ParticipantType.STUDENT, "booking1");
        Timeslot booked = reserved.book(bookEvent);

        assertFalse(booked.isWaiting("student1", ParticipantType.STUDENT));
        assertEquals(1, booked.findBooking("booking1").size());
    }

    @Test
    void testIsBookable() {
        Timeslot t = timeslot
                .reserve(new BookingEvent.ParticipantMarkedAvailable("slot1", "student1", ParticipantType.STUDENT))
                .reserve(new BookingEvent.ParticipantMarkedAvailable("slot1", "aircraft1", ParticipantType.AIRCRAFT))
                .reserve(new BookingEvent.ParticipantMarkedAvailable("slot1", "instructor1",
                        ParticipantType.INSTRUCTOR));

        assertTrue(t.isBookable("student1", "aircraft1", "instructor1"));
    }

    @Test
    void testCancelBookingRemovesBooking() {
        Timeslot t = timeslot
                .reserve(new BookingEvent.ParticipantMarkedAvailable("slot1", "student1", ParticipantType.STUDENT));

        BookingEvent.ParticipantBooked bookEvent = new BookingEvent.ParticipantBooked("slot1", "student1",
                ParticipantType.STUDENT, "booking1");

        Timeslot booked = t.book(bookEvent);

        Timeslot canceled = booked.cancelBooking("booking1");
        assertEquals(0, canceled.findBooking("booking1").size());
    }
}

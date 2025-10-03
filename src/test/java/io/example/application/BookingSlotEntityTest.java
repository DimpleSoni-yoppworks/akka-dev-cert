package io.example.application;

import akka.Done;
import akka.javasdk.testkit.EventSourcedTestKit;
import io.example.domain.Participant;
import io.example.domain.Participant.ParticipantType;
import io.example.domain.Timeslot;
import io.example.domain.Timeslot.Booking;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

public class BookingSlotEntityTest {

    @Test
    void testInitialStateIsEmpty() {
        var testKit = EventSourcedTestKit.of(BookingSlotEntity::new);
        Timeslot state = testKit.getState();

        assertNotNull(state);
        assertTrue(state.bookings().isEmpty());
        assertTrue(state.available().isEmpty());
    }

    @Test
    void testMarkSlotAvailablePersistsEventAndUpdatesState() {
        var testKit = EventSourcedTestKit.of(BookingSlotEntity::new);
        var participant = new Participant("student1", ParticipantType.STUDENT);

        var result = testKit.method(BookingSlotEntity::markSlotAvailable)
                .invoke(new BookingSlotEntity.Command.MarkSlotAvailable(participant));

        assertEquals(Done.getInstance(), result.getReply());
        assertEquals(1, testKit.getAllEvents().size());

        Timeslot state = testKit.getState();
        assertTrue(state.available().contains(participant));
    }

    @Test
    void testUnmarkSlotAvailablePersistsEventAndUpdatesState() {
        var testKit = EventSourcedTestKit.of(BookingSlotEntity::new);
        var participant = new Participant("student1", ParticipantType.STUDENT);

        testKit.method(BookingSlotEntity::markSlotAvailable)
                .invoke(new BookingSlotEntity.Command.MarkSlotAvailable(participant));

        var result = testKit.method(BookingSlotEntity::unmarkSlotAvailable)
                .invoke(new BookingSlotEntity.Command.UnmarkSlotAvailable(participant));

        assertEquals(Done.getInstance(), result.getReply());

        Timeslot state = testKit.getState();
        assertFalse(state.available().contains(participant));
    }

    @Test
    void testBookSlotPersistsEventsAndUpdatesState() {
        var testKit = EventSourcedTestKit.of(BookingSlotEntity::new);

        // Make participants available first
        var student = new Participant("student1", ParticipantType.STUDENT);
        var instructor = new Participant("instructor1", ParticipantType.INSTRUCTOR);
        var aircraft = new Participant("aircraft1", ParticipantType.AIRCRAFT);

        testKit.method(BookingSlotEntity::markSlotAvailable)
                .invoke(new BookingSlotEntity.Command.MarkSlotAvailable(student));
        testKit.method(BookingSlotEntity::markSlotAvailable)
                .invoke(new BookingSlotEntity.Command.MarkSlotAvailable(instructor));
        testKit.method(BookingSlotEntity::markSlotAvailable)
                .invoke(new BookingSlotEntity.Command.MarkSlotAvailable(aircraft));

        String bookingId = "booking1";
        var result = testKit.method(BookingSlotEntity::bookSlot)
                .invoke(new BookingSlotEntity.Command.BookReservation(
                        student.id(), aircraft.id(), instructor.id(), bookingId));

        assertEquals(Done.getInstance(), result.getReply());

        Timeslot state = testKit.getState();
        List<Booking> bookings = state.findBooking(bookingId);
        assertEquals(3, bookings.size());
        assertTrue(bookings.stream().anyMatch(b -> b.participant().equals(student)));
        assertTrue(bookings.stream().anyMatch(b -> b.participant().equals(instructor)));
        assertTrue(bookings.stream().anyMatch(b -> b.participant().equals(aircraft)));
    }

    @Test
    void testBookSlotFailsIfParticipantsNotAvailable() {
        var testKit = EventSourcedTestKit.of(BookingSlotEntity::new);

        String bookingId = "booking1";
        var result = testKit.method(BookingSlotEntity::bookSlot)
                .invoke(new BookingSlotEntity.Command.BookReservation(
                        "student1", "aircraft1", "instructor1", bookingId));

        assertTrue(result.isError());
        assertEquals("All participants must be available before booking", result.getError());
    }

    @Test
    void testCancelBookingPersistsEventsAndUpdatesState() {
        var testKit = EventSourcedTestKit.of(BookingSlotEntity::new);

        // Prepare participants and book slot
        var student = new Participant("student1", ParticipantType.STUDENT);
        var instructor = new Participant("instructor1", ParticipantType.INSTRUCTOR);
        var aircraft = new Participant("aircraft1", ParticipantType.AIRCRAFT);

        testKit.method(BookingSlotEntity::markSlotAvailable)
                .invoke(new BookingSlotEntity.Command.MarkSlotAvailable(student));
        testKit.method(BookingSlotEntity::markSlotAvailable)
                .invoke(new BookingSlotEntity.Command.MarkSlotAvailable(instructor));
        testKit.method(BookingSlotEntity::markSlotAvailable)
                .invoke(new BookingSlotEntity.Command.MarkSlotAvailable(aircraft));

        String bookingId = "booking1";
        testKit.method(BookingSlotEntity::bookSlot)
                .invoke(new BookingSlotEntity.Command.BookReservation(
                        student.id(), aircraft.id(), instructor.id(), bookingId));

        // Cancel booking
        var result = testKit.method(BookingSlotEntity::cancelBooking).invoke(bookingId);
        assertEquals(Done.getInstance(), result.getReply());

        Timeslot state = testKit.getState();
        assertTrue(state.findBooking(bookingId).isEmpty());
    }

    @Test
    void testCancelNonExistentBookingReturnsError() {
        var testKit = EventSourcedTestKit.of(BookingSlotEntity::new);

        var result = testKit.method(BookingSlotEntity::cancelBooking).invoke("nonexistent");
        assertTrue(result.isError());
        assertEquals("Booking not found for bookingId: nonexistent", result.getError());
    }
}

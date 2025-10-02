package io.example.application;

import akka.Done;
import akka.javasdk.annotations.ComponentId;
import akka.javasdk.eventsourcedentity.EventSourcedEntity;
import akka.javasdk.eventsourcedentity.EventSourcedEntityContext;
import io.example.domain.BookingEvent;
import io.example.domain.Participant;
import io.example.domain.Timeslot;

import java.util.HashSet;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import java.util.stream.Collectors;

@ComponentId("booking-slot")
public class BookingSlotEntity extends EventSourcedEntity<Timeslot, BookingEvent> {

    private final String entityId;
    private static final Logger logger = LoggerFactory.getLogger(BookingSlotEntity.class);

    public BookingSlotEntity(EventSourcedEntityContext context) {
        this.entityId = context.entityId();
    }

    public Effect<Done> markSlotAvailable(Command.MarkSlotAvailable cmd) {
        var participant = cmd.participant();
        var event = new BookingEvent.ParticipantMarkedAvailable(entityId, participant.id(),
                participant.participantType());
        return effects()
                .persist(event)
                .thenReply(newState -> Done.getInstance());
    }

    public Effect<Done> unmarkSlotAvailable(Command.UnmarkSlotAvailable cmd) {
        var participant = cmd.participant();
        var event = new BookingEvent.ParticipantUnmarkedAvailable(entityId, participant.id(),
                participant.participantType());
        return effects()
                .persist(event)
                .thenReply(newState -> Done.getInstance());
    }

    // NOTE: booking a slot should produce 3
    // `ParticipantBooked` events
    public Effect<Done> bookSlot(Command.BookReservation cmd) {
        var state = currentState();
        if (!state.isBookable(cmd.studentId(), cmd.aircraftId(), cmd.instructorId())) {
            return effects().error("All participants must be available before booking");
        }

        if (!state.findBooking(cmd.bookingId()).isEmpty()) {
            return effects().error("Booking already exists with this id: " + cmd.bookingId());
        }

        var studentEvent = new BookingEvent.ParticipantBooked(entityId, cmd.studentId(),
                Participant.ParticipantType.STUDENT, cmd.bookingId());
        var instructorEvent = new BookingEvent.ParticipantBooked(entityId, cmd.instructorId(),
                Participant.ParticipantType.INSTRUCTOR, cmd.bookingId());
        var aircraftEvent = new BookingEvent.ParticipantBooked(entityId, cmd.aircraftId(),
                Participant.ParticipantType.AIRCRAFT, cmd.bookingId());

        return effects()
                .persistAll(List.of(studentEvent, instructorEvent, aircraftEvent))
                .thenReply(newState -> Done.getInstance());
    }

    // NOTE: canceling a booking should produce 3
    // `ParticipantCanceled` events
    public Effect<Done> cancelBooking(String bookingId) {
        // First find the existing booking from state for a given bookingId.
        var bookings = currentState().findBooking(bookingId);

        if (bookings.isEmpty()) {
            logger.warn("Attempt to cancel a non existing booking = {}, slotId = {}", bookingId, entityId);
            return effects().error("Booking not found for bookingId: " + bookingId);
        }

        // Build cancellation events for all participants in the reservation
        List<BookingEvent> cancelEvents = bookings.stream().map(b -> new BookingEvent.ParticipantCanceled(
                entityId,
                b.participant().id(),
                b.participant().participantType(),
                b.bookingId())).collect(Collectors.toList());

        return effects().persistAll(cancelEvents).thenReply(newState -> Done.getInstance());
    }

    public ReadOnlyEffect<Timeslot> getSlot() {
        return effects().reply(currentState());
    }

    @Override
    public Timeslot emptyState() {
        return new Timeslot(new HashSet<>(), new HashSet<>());
    }

    @Override
    public Timeslot applyEvent(BookingEvent event) {
        return switch (event) {
            case BookingEvent.ParticipantBooked participantBooked -> currentState().book(participantBooked);
            case BookingEvent.ParticipantCanceled participantCanceled ->
                currentState().cancelBooking(participantCanceled.bookingId());
            case BookingEvent.ParticipantMarkedAvailable participantMarkedAvailable ->
                currentState().reserve(participantMarkedAvailable);
            case BookingEvent.ParticipantUnmarkedAvailable participantUnmarkedAvailable ->
                currentState().unreserve(participantUnmarkedAvailable);
        };
    }

    public sealed interface Command {
        record MarkSlotAvailable(Participant participant) implements Command {
        }

        record UnmarkSlotAvailable(Participant participant) implements Command {
        }

        record BookReservation(
                String studentId, String aircraftId, String instructorId, String bookingId)
                implements Command {
        }
    }
}

package io.example.application;

import akka.javasdk.annotations.ComponentId;
import akka.javasdk.annotations.Consume;
import akka.javasdk.client.ComponentClient;
import akka.javasdk.consumer.Consumer;
import io.example.domain.BookingEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

// This class is responsible for consuming events from the booking
// slot entity and turning those into command calls on the
// participant slot entity
@ComponentId("blooking-slot-consumer")
@Consume.FromEventSourcedEntity(BookingSlotEntity.class)
public class SlotToParticipantConsumer extends Consumer {

    private final ComponentClient client;
    private final Logger logger = LoggerFactory.getLogger(getClass());

    public SlotToParticipantConsumer(ComponentClient client) {
        this.client = client;
    }

    public Effect onEvent(BookingEvent event) {
        try {
            switch (event) {
                case BookingEvent.ParticipantMarkedAvailable participantMarkedAvailable -> {
                    logger.info("Processing ParticipantMarkedAvailable event: {}", participantMarkedAvailable);
                    client.forEventSourcedEntity(participantSlotId(participantMarkedAvailable))
                            .method(ParticipantSlotEntity::markAvailable)
                            .invoke(new ParticipantSlotEntity.Commands.MarkAvailable(
                                    participantMarkedAvailable.slotId(),
                                    participantMarkedAvailable.participantId(),
                                    participantMarkedAvailable.participantType()));
                }

                case BookingEvent.ParticipantUnmarkedAvailable participantUnmarkedAvailable -> {
                    logger.info("Processing UnmarkedAvailable: {}", participantUnmarkedAvailable);
                    client.forEventSourcedEntity(participantSlotId(participantUnmarkedAvailable))
                            .method(ParticipantSlotEntity::unmarkAvailable)
                            .invoke(new ParticipantSlotEntity.Commands.UnmarkAvailable(
                                    participantUnmarkedAvailable.slotId(),
                                    participantUnmarkedAvailable.participantId(),
                                    participantUnmarkedAvailable.participantType()));
                }
                case BookingEvent.ParticipantBooked participantBooked -> {
                    logger.info("Processing Booked: {}", participantBooked);
                    client.forEventSourcedEntity(participantSlotId(participantBooked))
                            .method(ParticipantSlotEntity::book)
                            .invoke(new ParticipantSlotEntity.Commands.Book(
                                    participantBooked.slotId(),
                                    participantBooked.participantId(),
                                    participantBooked.participantType(),
                                    participantBooked.bookingId()));
                }
                case BookingEvent.ParticipantCanceled participantCanceled -> {
                    logger.info("Processing Canceled: {}", participantCanceled);
                    client.forEventSourcedEntity(participantSlotId(participantCanceled))
                            .method(ParticipantSlotEntity::cancel)
                            .invoke(new ParticipantSlotEntity.Commands.Cancel(
                                    participantCanceled.slotId(),
                                    participantCanceled.participantId(),
                                    participantCanceled.participantType(),
                                    participantCanceled.bookingId()));
                }
                default -> {
                    logger.warn("Unknown event type: {}", event.getClass().getSimpleName());
                    effects().ignore();
                }
            }
            return effects().done();
        } catch (Exception ex) {
            logger.error("Error processing event {}: {}", event, ex.getMessage(), ex);
            return effects().ignore();
        }
    }

    // Participant slots are keyed by a derived key made up of
    // {slotId}-{participantId}
    // We don't need the participant type here because the participant IDs
    // should always be unique/UUIDs
    private String participantSlotId(BookingEvent event) {
        return switch (event) {
            case BookingEvent.ParticipantBooked evt -> evt.slotId() + "-" + evt.participantId();
            case BookingEvent.ParticipantUnmarkedAvailable evt ->
                evt.slotId() + "-" + evt.participantId();
            case BookingEvent.ParticipantMarkedAvailable evt -> evt.slotId() + "-" + evt.participantId();
            case BookingEvent.ParticipantCanceled evt -> evt.slotId() + "-" + evt.participantId();
        };
    }
}

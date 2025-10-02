package io.example.application;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import akka.Done;
import akka.javasdk.annotations.ComponentId;
import akka.javasdk.annotations.TypeName;
import akka.javasdk.eventsourcedentity.EventSourcedEntity;
import akka.javasdk.eventsourcedentity.EventSourcedEntityContext;
import io.example.domain.Participant.ParticipantType;

@ComponentId("participant-slot")
public class ParticipantSlotEntity
    extends EventSourcedEntity<ParticipantSlotEntity.State, ParticipantSlotEntity.Event> {

  private final String entityId;
  private static final Logger logger = LoggerFactory.getLogger(ParticipantSlotEntity.class);

  public ParticipantSlotEntity(EventSourcedEntityContext context) {
    this.entityId = context.entityId();
  }

  @Override
  public State emptyState() {
    return new State(entityId, "", null, "UNAVAILABLE");
  }

  public Effect<Done> unmarkAvailable(ParticipantSlotEntity.Commands.UnmarkAvailable unmark) {
    var event = new Event.UnmarkedAvailable(entityId, unmark.participantId, unmark.participantType);
    logger.info("Persisting UnmarkedAvailable event: slotId={}, participantId={}, participantType={}",
        entityId, unmark.participantId(), unmark.participantType());
    return effects()
        .persist(event)
        .thenReply(newState -> Done.getInstance());
  }

  public Effect<Done> markAvailable(ParticipantSlotEntity.Commands.MarkAvailable mark) {
    var event = new Event.MarkedAvailable(entityId, mark.participantId, mark.participantType);
    logger.info("Persisting MarkedAvailable event: slotId={}, participantId={}, participantType={}",
        entityId, mark.participantId(), mark.participantType());
    return effects()
        .persist(event)
        .thenReply(newState -> Done.getInstance());
  }

  public Effect<Done> book(ParticipantSlotEntity.Commands.Book book) {
    var state = currentState();

    if ("BOOKED".equalsIgnoreCase(state.status())) {
      logger.warn("Cannot book participant {}: already booked", book.participantId());
      return effects().error("Participant already booked.");
    }
    var event = new Event.Booked(entityId, book.participantId(), book.participantType(), book.bookingId());
    logger.info("Persisting Booked event: slotId={}, participantId={}, participantType={}, bookingId={}",
        entityId, book.participantId(), book.participantType(), book.bookingId());
    return effects()
        .persist(event)
        .thenReply(newState -> Done.getInstance());
  }

  public Effect<Done> cancel(ParticipantSlotEntity.Commands.Cancel cancel) {
    var state = currentState();
    if ("BOOKED".equalsIgnoreCase(state.status())) {
      return effects().error("Cannot cancel: Participant is not booked");
    }
    var event = new Event.Canceled(entityId, cancel.participantId(), cancel.participantType(), cancel.bookingId());
    logger.info("Persisting Canceled event: slotId={}, participantId={}, participantType={}, bookingId={}",
        entityId, cancel.participantId(), cancel.participantType(), cancel.bookingId());
    return effects()
        .persist(event)
        .thenReply(newState -> Done.getInstance());
  }

  record State(String slotId, String participantId, ParticipantType participantType, String status) {
  }

  public sealed interface Commands {
    record MarkAvailable(String slotId, String participantId, ParticipantType participantType)
        implements Commands {
    }

    record UnmarkAvailable(String slotId, String participantId, ParticipantType participantType)
        implements Commands {
    }

    record Book(
        String slotId, String participantId, ParticipantType participantType, String bookingId)
        implements Commands {
    }

    record Cancel(
        String slotId, String participantId, ParticipantType participantType, String bookingId)
        implements Commands {
    }
  }

  public sealed interface Event {
    @TypeName("marked-available")
    record MarkedAvailable(String slotId, String participantId, ParticipantType participantType)
        implements Event {
    }

    @TypeName("unmarked-available")
    record UnmarkedAvailable(String slotId, String participantId, ParticipantType participantType)
        implements Event {
    }

    @TypeName("participant-booked")
    record Booked(
        String slotId, String participantId, ParticipantType participantType, String bookingId)
        implements Event {
    }

    @TypeName("participant-canceled")
    record Canceled(
        String slotId, String participantId, ParticipantType participantType, String bookingId)
        implements Event {
    }
  }

  @Override
  public ParticipantSlotEntity.State applyEvent(ParticipantSlotEntity.Event event) {
    return switch (event) {
      case Event.MarkedAvailable e ->
        new State(e.slotId(), e.participantId(), e.participantType(), "AVAILABLE");

      case Event.UnmarkedAvailable e ->
        new State(e.slotId(), e.participantId(), e.participantType(), "UNAVAILABLE");

      case Event.Booked e ->
        new State(e.slotId(), e.participantId(), e.participantType(), "BOOKED");

      case Event.Canceled e ->
        new State(e.slotId(), e.participantId(), e.participantType(), "CANCELED");
    };
  }
}

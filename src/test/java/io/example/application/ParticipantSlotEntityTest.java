package io.example.application;

import akka.Done;
import akka.javasdk.testkit.EventSourcedTestKit;
import io.example.domain.Participant.ParticipantType;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

public class ParticipantSlotEntityTest {

    private final String slotId = "testkit-entity-id";

    @Test
    void testInitialStateIsUnavailable() {
        var testKit = EventSourcedTestKit.of(ParticipantSlotEntity::new);

        var state = testKit.getState();

        assertEquals("", state.participantId());
        assertNull(state.participantType());
        assertEquals("UNAVAILABLE", state.status());
    }

    @Test
    void testMarkAvailablePersistsEvent() {
        var testKit = EventSourcedTestKit.of(ParticipantSlotEntity::new);

        var command = new ParticipantSlotEntity.Commands.MarkAvailable(slotId, "student1", ParticipantType.STUDENT);
        var result = testKit.method(ParticipantSlotEntity::markAvailable).invoke(command);

        assertEquals(Done.getInstance(), result.getReply());

        var event = result.getNextEventOfType(ParticipantSlotEntity.Event.MarkedAvailable.class);
        assertEquals("student1", event.participantId());
        assertEquals(ParticipantType.STUDENT, event.participantType());
        assertEquals("AVAILABLE", testKit.getState().status());
    }

    @Test
    void testUnmarkAvailablePersistsEvent() {
        var testKit = EventSourcedTestKit.of(ParticipantSlotEntity::new);

        testKit.method(ParticipantSlotEntity::markAvailable)
                .invoke(new ParticipantSlotEntity.Commands.MarkAvailable(slotId, "student1", ParticipantType.STUDENT));

        var unmarkCommand = new ParticipantSlotEntity.Commands.UnmarkAvailable(slotId, "student1",
                ParticipantType.STUDENT);
        var result = testKit.method(ParticipantSlotEntity::unmarkAvailable).invoke(unmarkCommand);

        assertEquals(Done.getInstance(), result.getReply());

        var event = result.getNextEventOfType(ParticipantSlotEntity.Event.UnmarkedAvailable.class);
        assertEquals("student1", event.participantId());
        assertEquals(ParticipantType.STUDENT, event.participantType());
        assertEquals("UNAVAILABLE", testKit.getState().status());
    }

    @Test
    void testBookPersistsEvent() {
        var testKit = EventSourcedTestKit.of(ParticipantSlotEntity::new);

        var bookCommand = new ParticipantSlotEntity.Commands.Book(slotId, "student1", ParticipantType.STUDENT,
                "booking1");
        var result = testKit.method(ParticipantSlotEntity::book).invoke(bookCommand);

        assertEquals(Done.getInstance(), result.getReply());

        var event = result.getNextEventOfType(ParticipantSlotEntity.Event.Booked.class);
        assertEquals("BOOKED", testKit.getState().status());
        assertEquals("booking1", event.bookingId());
    }

    @Test
    void testCancelPersistsEvent() {
        var testKit = EventSourcedTestKit.of(ParticipantSlotEntity::new);

        testKit.method(ParticipantSlotEntity::book)
                .invoke(new ParticipantSlotEntity.Commands.Book(slotId, "student1", ParticipantType.STUDENT,
                        "booking1"));

        var cancelCommand = new ParticipantSlotEntity.Commands.Cancel(slotId, "student1", ParticipantType.STUDENT,
                "booking1");
        var result = testKit.method(ParticipantSlotEntity::cancel).invoke(cancelCommand);

        assertEquals(Done.getInstance(), result.getReply());

        var event = result.getNextEventOfType(ParticipantSlotEntity.Event.Canceled.class);
        assertEquals("student1", event.participantId());
        assertEquals("booking1", event.bookingId());

        var state = testKit.getState();
        assertEquals("CANCELED", state.status());
        assertEquals("student1", state.participantId());
    }
}

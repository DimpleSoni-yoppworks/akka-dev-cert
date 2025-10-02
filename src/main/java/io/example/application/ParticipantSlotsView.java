package io.example.application;

import akka.javasdk.annotations.ComponentId;
import akka.javasdk.annotations.Consume;
import akka.javasdk.annotations.Query;
import akka.javasdk.view.TableUpdater;
import akka.javasdk.view.View;
import io.example.application.ParticipantSlotEntity.Event.Booked;
import io.example.application.ParticipantSlotEntity.Event.Canceled;
import io.example.application.ParticipantSlotEntity.Event.MarkedAvailable;
import io.example.application.ParticipantSlotEntity.Event.UnmarkedAvailable;

import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@ComponentId("view-participant-slots")
public class ParticipantSlotsView extends View {

    private static Logger logger = LoggerFactory.getLogger(ParticipantSlotsView.class);

    @Consume.FromEventSourcedEntity(ParticipantSlotEntity.class)
    public static class ParticipantSlotsViewUpdater extends TableUpdater<SlotRow> {

        public Effect<SlotRow> onEvent(ParticipantSlotEntity.Event event) {
        
            SlotRow row = switch (event) {
                case Booked e ->
                    new SlotRow(e.slotId(), e.participantId(), e.participantType().name(), e.bookingId(), "BOOKED");
                case Canceled e ->
                    new SlotRow(e.slotId(), e.participantId(), e.participantType().name(), e.bookingId(), "CANCELED");
                case MarkedAvailable e ->
                    new SlotRow(e.slotId(), e.participantId(), e.participantType().name(), "NA", "AVAILABLE");
                case UnmarkedAvailable e ->
                    new SlotRow(e.slotId(), e.participantId(), e.participantType().name(), "NA", "UNAVAILABLE");
            };

            logger.info("Updating participant slot row: {}", row);
            return effects().updateRow(row);
        }
    }
    public record SlotRow(
            String slotId,
            String participantId,
            String participantType,
            String bookingId,
            String status) {
    }

    public record ParticipantStatusInput(String participantId, String status) {
    }

    public record SlotList(List<SlotRow> slots) {
    }

    /**
     * @param participantId
     * @return With the @Query annotation, Akka will return all rows persisted by
     *         updateRow(row) matching that participantId.
     */
    @Query("SELECT * FROM SlotRow WHERE participantId = :participantId")
    public QueryEffect<SlotRow> getSlotsByParticipant(String participantId) {
        return queryResult();
    }

    @Query("SELECT * FROM SlotRow WHERE participantId = :participantId AND status = :status")
    public QueryEffect<SlotRow> getSlotsByParticipantAndStatus(ParticipantStatusInput input) {
        return queryResult();
    }

}

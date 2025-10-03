package io.example.api;

import java.util.UUID;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import akka.http.javadsl.model.StatusCodes;
import akka.javasdk.http.StrictResponse;
import akka.javasdk.testkit.TestKitSupport;
import akka.util.ByteString;
import io.example.application.BookingSlotEntity;
import io.example.domain.Participant;
import io.example.domain.Participant.ParticipantType;

public class FlightEndpointIntegrationTest extends TestKitSupport {

    @Test
    public void testCreateBooking() {
        String slotId = UUID.randomUUID().toString();

        componentClient
                .forEventSourcedEntity(slotId)
                .method(BookingSlotEntity::markSlotAvailable)
                .invoke(new BookingSlotEntity.Command.MarkSlotAvailable(
                        new Participant("student1", ParticipantType.STUDENT)));
        componentClient
                .forEventSourcedEntity(slotId)
                .method(BookingSlotEntity::markSlotAvailable)
                .invoke(new BookingSlotEntity.Command.MarkSlotAvailable(
                        new Participant("instructor1", ParticipantType.INSTRUCTOR)));
        componentClient
                .forEventSourcedEntity(slotId)
                .method(BookingSlotEntity::markSlotAvailable)
                .invoke(new BookingSlotEntity.Command.MarkSlotAvailable(
                        new Participant("aircraft1", ParticipantType.AIRCRAFT)));

        var request = new FlightEndpoint.BookingRequest(
                "student1", "aircraft1", "instructor1", "booking1");

        var postResponse = httpClient
                .POST("/flight/bookings/" + slotId)
                .withRequestBody(request)
                .invoke();

        Assertions.assertEquals(StatusCodes.CREATED, postResponse.status());
    }

    @Test
    public void testCancelBooking() {
        String slotId = UUID.randomUUID().toString();
        String bookingId = "booking1";

        componentClient
                .forEventSourcedEntity(slotId)
                .method(BookingSlotEntity::markSlotAvailable)
                .invoke(new BookingSlotEntity.Command.MarkSlotAvailable(
                        new Participant("student1", ParticipantType.STUDENT)));
        componentClient
                .forEventSourcedEntity(slotId)
                .method(BookingSlotEntity::markSlotAvailable)
                .invoke(new BookingSlotEntity.Command.MarkSlotAvailable(
                        new Participant("instructor1", ParticipantType.INSTRUCTOR)));
        componentClient
                .forEventSourcedEntity(slotId)
                .method(BookingSlotEntity::markSlotAvailable)
                .invoke(new BookingSlotEntity.Command.MarkSlotAvailable(
                        new Participant("aircraft1", ParticipantType.AIRCRAFT)));
        componentClient
                .forEventSourcedEntity(slotId)
                .method(BookingSlotEntity::bookSlot)
                .invoke(new BookingSlotEntity.Command.BookReservation("student1", "aircraft1", "instructor1",
                        bookingId));

        StrictResponse<ByteString> deleteResponse = httpClient
                .DELETE("/flight/bookings/" + slotId + "/" + bookingId)
                .invoke();

        Assertions.assertEquals(StatusCodes.OK, deleteResponse.status());
    }

    @Test
    public void testMarkAvailable() {
        String slotId = UUID.randomUUID().toString();

        var request = new FlightEndpoint.AvailabilityRequest("student1", "STUDENT");

        StrictResponse<ByteString> response = httpClient
                .POST("/flight/availability/" + slotId)
                .withRequestBody(request)
                .invoke();

        Assertions.assertEquals(StatusCodes.OK, response.httpResponse().status());
    }

    @Test
    public void testUnmarkAvailable() {
        String slotId = UUID.randomUUID().toString();
        var markRequest = new FlightEndpoint.AvailabilityRequest("student1", "STUDENT");
        httpClient.POST("/flight/availability/" + slotId)
                .withRequestBody(markRequest)
                .invoke();

        var unmarkRequest = new FlightEndpoint.AvailabilityRequest("student1", "STUDENT");
        StrictResponse<ByteString> deleteResponse = httpClient
                .DELETE("/flight/availability/" + slotId)
                .withRequestBody(unmarkRequest)
                .invoke();

        Assertions.assertEquals(StatusCodes.OK, deleteResponse.httpResponse().status());
    }

    @Test
    public void testGetSlotsByStatus() {
        String participantId = "student1";
        String status = "AVAILABLE";

        String slotId = UUID.randomUUID().toString();
        httpClient.POST("/flight/availability/" + slotId)
                .withRequestBody(new FlightEndpoint.AvailabilityRequest(participantId, "STUDENT"))
                .invoke();

        StrictResponse<ByteString> getResponse = httpClient
                .GET("/flight/slots/" + participantId + "/" + status)
                .invoke();

        Assertions.assertEquals(StatusCodes.OK, getResponse.httpResponse().status());
    }
}

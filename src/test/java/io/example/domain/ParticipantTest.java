package io.example.domain;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

public class ParticipantTest {

    @Test
    void testStudentParticipant() {
        Participant participant = new Participant("student1", Participant.ParticipantType.STUDENT);

        assertEquals("student1", participant.id());
        assertEquals(Participant.ParticipantType.STUDENT, participant.participantType());
    }

    @Test
    void testInstructorParticipant() {
        Participant participant = new Participant("instructor1", Participant.ParticipantType.INSTRUCTOR);

        assertEquals("instructor1", participant.id());
        assertEquals(Participant.ParticipantType.INSTRUCTOR, participant.participantType());
    }

    @Test
    void testAircraftParticipant() {
        Participant participant = new Participant("aircraft1", Participant.ParticipantType.AIRCRAFT);

        assertEquals("aircraft1", participant.id());
        assertEquals(Participant.ParticipantType.AIRCRAFT, participant.participantType());
    }
}

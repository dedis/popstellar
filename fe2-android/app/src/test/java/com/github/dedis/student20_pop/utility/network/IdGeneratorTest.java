package com.github.dedis.student20_pop.utility.network;

import android.util.ArraySet;

import com.github.dedis.student20_pop.model.event.EventState;
import com.github.dedis.student20_pop.model.event.EventType;
import com.github.dedis.student20_pop.model.network.method.message.data.election.ElectionSetup;
import com.github.dedis.student20_pop.model.network.method.message.data.ElectionVote;
import com.github.dedis.student20_pop.model.network.method.message.data.lao.CreateLao;
import com.github.dedis.student20_pop.model.network.method.message.data.lao.UpdateLao;
import com.github.dedis.student20_pop.model.network.method.message.data.meeting.CreateMeeting;
import com.github.dedis.student20_pop.model.network.method.message.data.rollcall.CloseRollCall;
import com.github.dedis.student20_pop.model.network.method.message.data.rollcall.CreateRollCall;
import com.github.dedis.student20_pop.model.network.method.message.data.rollcall.OpenRollCall;
import com.github.dedis.student20_pop.utility.security.Hash;

import org.junit.Test;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Arrays;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.Assert.assertNull;

public class IdGeneratorTest {

    private final String organizer = Hash.hash("organizer");
    private final String laoId = Hash.hash("laoId");
    private final String id = Hash.hash("id");
    private final String name = "name";
    private final long time = Instant.now().getEpochSecond();
    private final String votingMethod = "Plurality";
    private final String question = "Question";
    private final String writeIn = "Write In";
    private final String location = "Location";

    @Test
    public void generateCreateLaoIdTest() {
        CreateLao createLao = new CreateLao(name, organizer);
        // Hash(organizer||creation||name)
        String expectedId = Hash.hash(createLao.getOrganizer(), Long.toString(createLao.getCreation()), createLao.getName());
        assertThat(createLao.getId(), is(expectedId));
    }

    @Test
    public void generateUpdateLaoIdTest() {
        UpdateLao updateLao = new UpdateLao(organizer, time, name, time, new ArraySet<>());
        // Hash(organizer||creation||name)
        String expectedId = Hash.hash(organizer, Long.toString(time), updateLao.getName());
        assertThat(updateLao.getId(), is(expectedId));
    }

    @Test
    public void generateCreateMeetingIdTest() {
        CreateMeeting createMeeting = new CreateMeeting(laoId, name, time, location, time, time);
        // Hash('M'||lao_id||creation||name)
        String expectedId = Hash.hash(EventType.MEETING.getSuffix(), laoId, Long.toString(createMeeting.getCreation()), createMeeting.getName());
        assertThat(createMeeting.getId(), is(expectedId));
    }

    @Test
    public void generateCreateRollCallIdTest() {
        CreateRollCall createRollCall = new CreateRollCall(name, time, time, location, null, laoId);
        // Hash('R'||lao_id||creation||name)
        String expectedId = Hash.hash(EventType.ROLL_CALL.getSuffix(), laoId, Long.toString(createRollCall.getCreation()), createRollCall.getName());
        assertThat(createRollCall.getId(), is(expectedId));
    }

    @Test
    public void generateOpenRollCallIdTest() {
        OpenRollCall openRollCall = new OpenRollCall(laoId, id, time, EventState.CLOSED);
        // Hash('R'||lao_id||opens||opened_at)
        String expectedId = Hash.hash(EventType.ROLL_CALL.getSuffix(), laoId, openRollCall.getOpens(), Long.toString(openRollCall.getOpenedAt()));
        assertThat(openRollCall.getUpdateId(), is(expectedId));
    }

    @Test
    public void generateCloseRollCallIdTest() {
        CloseRollCall closeRollCall = new CloseRollCall(laoId, id, time, new ArrayList<>());
        // Hash('R'||lao_id||closes||closed_at)
        String expectedId = Hash.hash(EventType.ROLL_CALL.getSuffix(), laoId, closeRollCall.getCloses(), Long.toString(closeRollCall.getClosedAt()));
        assertThat(closeRollCall.getUpdateId(), is(expectedId));
    }

    @Test
    public void generateElectionSetupIdTest() {
        ElectionSetup electionSetup = new ElectionSetup(name, time, time, votingMethod, true, new ArrayList<>(), question, laoId);
        // Hash('Election'||lao_id||created_at||name)
        String expectedId = Hash.hash(EventType.ELECTION.getSuffix(), electionSetup.getLao(), Long.toString(electionSetup.getCreation()), electionSetup.getName());
        assertThat(electionSetup.getId(), is(expectedId));
    }

    @Test
    public void generateElectionQuestionIdTest() {
        ElectionSetup electionSetup = new ElectionSetup(name, time, time, votingMethod, true, new ArrayList<>(), question, laoId);
        // Hash(“Question”||election_id||question)
        String expectedId = Hash.hash(IdGenerator.SUFFIX_ELECTION_QUESTION, electionSetup.getId(), question);
        assertThat(electionSetup.getQuestions().get(0).getId(), is(expectedId));
    }

    @Test
    public void generateElectionVoteIdWriteInEnabledTest() {
        ElectionVote electionVote = new ElectionVote(id, new ArrayList<>(), true, writeIn, id);
        // WriteIn enabled so id is Hash('Vote'||election_id||question_id||write_in)
        String expectedId = Hash.hash(IdGenerator.SUFFIX_ELECTION_VOTE, id, electionVote.getQuestionId(), electionVote.getWriteIn());
        assertThat(electionVote.getId(), is(expectedId));
        assertNull(electionVote.getVotes());
    }

    @Test
    public void generateElectionVoteIdWriteDisabledTest() {
        ArrayList<Integer> voteIndex = new ArrayList<>(Arrays.asList(1, 2, 3));
        ElectionVote electionVote = new ElectionVote(id, voteIndex, false, writeIn, id);
        // WriteIn enabled so id is Hash('Vote'||election_id||question_id||vote_index(es)) with concatenated vote indexes
        String expectedId = Hash.hash(IdGenerator.SUFFIX_ELECTION_VOTE, id, electionVote.getQuestionId(), electionVote.getVotes().toString());
        assertThat(electionVote.getId(), is(expectedId));
        assertNull(electionVote.getWriteIn());
    }
}

package com.github.dedis.popstellar.model.objects;

import androidx.arch.core.executor.testing.InstantTaskExecutorRule;

import com.github.dedis.popstellar.model.network.method.message.data.election.*;
import com.github.dedis.popstellar.model.objects.security.*;
import com.github.dedis.popstellar.model.objects.security.elGamal.*;
import com.github.dedis.popstellar.utility.security.Hash;

import java.time.Instant;
import java.util.*;

import ch.epfl.dedis.lib.exception.CothorityCryptoException;

import static com.github.dedis.popstellar.model.network.method.message.data.election.ElectionVersion.OPEN_BALLOT;
import static com.github.dedis.popstellar.model.objects.event.EventState.OPENED;
import static com.github.dedis.popstellar.model.objects.event.EventType.ELECTION;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertThrows;

public class ElectionTest {

  private static final PublicKey SENDER_1 =
      new PublicKey("oOcKZjUeandJOFVgn-E6e-7QksviBBbHUPicdzUgIm8");
  private static final PublicKey SENDER_2 =
      new PublicKey("TrWJNl4kA9VUBydvUwfWw9A-EJlLL6xLaQqRdynvhYw");

  private static final MessageID MESSAGE_ID_1 =
      new MessageID("bVuVwESHUTTlMb_-Ks-pIv88S0_1fsxMDgPyZjPEJrg");
  private static final MessageID MESSAGE_ID_2 =
      new MessageID("kwCQ-Es_Ysu_c_ZGVA77Bh_lx61aq0_H0DJgZahx7RA");

  private final ElectionQuestion electionQuestion =
      new ElectionQuestion(
          "my question",
          "Plurality",
          false,
          Arrays.asList("candidate1", "candidate2"),
          "my election id");
  private final String name = "my election name";
  private final String id = "my election id";
  private final long startTime = 0;
  private final long endTime = 1;
  private final Channel channel = Channel.ROOT.subChannel("election_channel");
  private final Election election =
      new Election("lao id", Instant.now().getEpochSecond(), name, OPEN_BALLOT);

  // Add some vote for decryption/encryption testing purposes
  private final String questionId1 = " myQuestion1";
  private final String laoId = "lao";

  // Set up a open ballot election
  private final ElectionVote electionVote1 = new ElectionVote(questionId1, 1, false, null, id);
  private final List<ElectionVote> electionVotes = Collections.singletonList(electionVote1);

  // Generate public key and populate the election key field
  ElectionKeyPair encryptionKeys = ElectionKeyPair.generateKeyPair();
  ElectionPublicKey electionPublicKey = encryptionKeys.getEncryptionScheme();
  ElectionPrivateKey electionPrivateKey = encryptionKeys.getDecryptionScheme();

  // Required to test encryption
  @Before
  public void setUp() {
    Base64URLData encodedKey = new Base64URLData(electionPublicKey.getPublicKey().toBytes());
    election.setElectionKey(encodedKey.getEncoded());
  }

  @Rule public InstantTaskExecutorRule rule = new InstantTaskExecutorRule();

  @Test
  public void settingNullParametersThrowsException() {
    assertThrows(IllegalArgumentException.class, () -> election.setName(null));
    assertThrows(IllegalArgumentException.class, () -> election.setId(null));
  }

  @Test
  public void settingAndGettingReturnsCorrespondingName() {
    election.setName(name);
    assertThat(election.getName(), is(name));
  }

  @Test
  public void settingAndGettingReturnsCorrespondingState() {
    election.setEventState(OPENED);
    assertThat(election.getState().getValue(), is(OPENED));
  }

  @Test
  public void settingAndGettingReturnsCorrespondingId() {
    election.setId(id);
    assertThat(election.getId(), is(id));
  }

  @Test
  public void settingAndGettingReturnsCorrespondingElectionQuestion() {
    election.setElectionQuestions(Collections.singletonList(electionQuestion));
    assertThat(election.getElectionQuestions().get(0), is(electionQuestion));
  }

  @Test
  public void settingAndGettingReturnsCorrespondingChannel() {
    election.setChannel(channel);
    assertThat(election.getChannel(), is(channel));
  }

  @Test
  public void settingNegativeTimesThrowsException() {
    assertThrows(IllegalArgumentException.class, () -> election.setStart(-1));
    assertThrows(IllegalArgumentException.class, () -> election.setEnd(-1));
    assertThrows(IllegalArgumentException.class, () -> election.setCreation(-1));
  }

  @Test
  public void settingNullElectionQuestionsThrowsException() {
    assertThrows(IllegalArgumentException.class, () -> election.setElectionQuestions(null));
  }

  @Test
  public void settingAndGettingReturnsCorrespondingStartTime() {
    election.setStart(startTime);
    assertThat(election.getStartTimestamp(), is(startTime));
  }

  @Test
  public void settingAndGettingReturnsCorrespondingEndTime() {
    election.setEnd(endTime);
    assertThat(election.getEndTimestamp(), is(endTime));
  }

  @Test
  public void settingSameRegisteredVotesAndComparingReturnsTrue() {
    List<ElectionVote> votes1 =
        Arrays.asList(
            new ElectionVote("b", 1, false, "", "my election id"),
            new ElectionVote("a", 2, false, "", "my election id"));
    List<ElectionVote> votes2 =
        Arrays.asList(
            new ElectionVote("c", 3, false, "", "my election id"),
            new ElectionVote("d", 4, false, "", "my election id"));
    election.putOpenBallotVotesBySender(SENDER_2, votes2);
    election.putSenderByMessageId(SENDER_1, MESSAGE_ID_1);
    election.putSenderByMessageId(SENDER_2, MESSAGE_ID_2);
    election.putOpenBallotVotesBySender(SENDER_1, votes1);
    String hash =
        Hash.hash(
            votes1.get(1).getId(),
            votes1.get(0).getId(),
            votes2.get(1).getId(),
            votes2.get(0).getId());
    assertThat(election.computerRegisteredVotes(), is(hash));
  }

  @Test
  public void resultsAreCorrectlySorted() {
    List<QuestionResult> unsortedResults = new ArrayList<>();
    unsortedResults.add(new QuestionResult("Candidate1", 30));
    unsortedResults.add(new QuestionResult("Candidate2", 23));
    unsortedResults.add(new QuestionResult("Candidate3", 16));
    unsortedResults.add(new QuestionResult("Candidate4", 43));
    List<ElectionResultQuestion> resultQuestion =
        Collections.singletonList(new ElectionResultQuestion("question_id", unsortedResults));
    election.setResults(resultQuestion);
    List<QuestionResult> sortedResults = election.getResultsForQuestionId("question_id");

    QuestionResult firstResult = sortedResults.get(0);
    assertThat(firstResult.getBallot(), is("Candidate4"));
    assertThat(firstResult.getCount(), is(43));

    QuestionResult secondResult = sortedResults.get(1);
    assertThat(secondResult.getBallot(), is("Candidate1"));
    assertThat(secondResult.getCount(), is(30));

    QuestionResult thirdResult = sortedResults.get(2);
    assertThat(thirdResult.getBallot(), is("Candidate2"));
    assertThat(thirdResult.getCount(), is(23));

    QuestionResult fourthResult = sortedResults.get(3);
    assertThat(fourthResult.getBallot(), is("Candidate3"));
    assertThat(fourthResult.getCount(), is(16));
  }

  @Test
  public void getVersionTest() {
    assertThat(ElectionVersion.OPEN_BALLOT, is(election.getElectionVersion()));
  }

  @Test
  public void testAndSetElectionKey() {
    String key = "key";
    election.setElectionKey("key");
    assertThat(key, is(election.getElectionKey()));
  }

  @Test
  public void electionEncryptionProcess() {
    // First encrypt
    List<ElectionEncryptedVote> encryptedVotes = election.encrypt(electionVotes);

    // Compare results
    for (int i = 0; i < encryptedVotes.size(); i++) {
      ElectionEncryptedVote e = encryptedVotes.get(i);
      ElectionVote o = electionVotes.get(i);
      try {
        byte[] decryptedData = electionPrivateKey.decrypt(e.getVote());
        // Pad the result
        int decryptedINt = ((decryptedData[1] & 0xff)) | (decryptedData[0] & 0xff << 8);
        int openVoteIndice = o.getVote();
        assertEquals(openVoteIndice, decryptedINt);
      } catch (CothorityCryptoException exception) {
      } // Shouldn't do anything
    }
  }

  @Test
  public void getCreationInMillisTest() {
    assertEquals(election.getCreation() * 1000, election.getCreationInMillis());
  }

  @Test
  public void setCreationWithNegativeValueThrowsException() {
    assertThrows(IllegalArgumentException.class, () -> election.setCreation(-1L));
  }

  @Test
  public void setCreationTest() {
    election.setCreation(1L);
    assertEquals(1L, election.getCreation());
  }

  @Test
  public void putOpenBallotWithNullPkThrowsException() {
    List<ElectionVote> list = new ArrayList<>();
    assertThrows(
        IllegalArgumentException.class, () -> election.putOpenBallotVotesBySender(null, list));
  }

  @Test
  public void putOpenBallotWithEmptyVotesThrowsException() {
    List<ElectionVote> list = new ArrayList<>();
    assertThrows(
        IllegalArgumentException.class, () -> election.putOpenBallotVotesBySender(SENDER_1, list));
  }

  @Test
  public void putOpenBallotWithNullVotesThrowsException() {
    assertThrows(
        IllegalArgumentException.class, () -> election.putOpenBallotVotesBySender(SENDER_1, null));
  }

  @Test
  public void putSenderByMessageIdWithNullPkThrowsException() {
    MessageID messageID = new MessageID("foo");
    assertThrows(
        IllegalArgumentException.class, () -> election.putSenderByMessageId(null, messageID));
  }

  @Test
  public void putSenderByMessageIdWithNullMsgIdThrowsException() {
    assertThrows(
        IllegalArgumentException.class, () -> election.putSenderByMessageId(SENDER_1, null));
  }

  @Test
  public void putEncryptedBallotWithNullPkThrowsException() {
    List<ElectionEncryptedVote> list = new ArrayList<>();
    assertThrows(
        IllegalArgumentException.class, () -> election.putEncryptedVotesBySender(null, list));
  }

  @Test
  public void putEncryptedBallotWithEmptyVotesThrowsException() {
    List<ElectionEncryptedVote> list = new ArrayList<>();
    assertThrows(
        IllegalArgumentException.class, () -> election.putEncryptedVotesBySender(SENDER_1, list));
  }

  @Test
  public void putEncryptedBallotWithNullVotesThrowsException() {
    assertThrows(
        IllegalArgumentException.class, () -> election.putEncryptedVotesBySender(SENDER_1, null));
  }

  @Test
  public void setResultsWithNullResultsThrowsException() {
    assertThrows(IllegalArgumentException.class, () -> election.setResults(null));
  }

  @Test
  public void getTypeTest() {
    assertEquals(ELECTION, election.getType());
  }
}

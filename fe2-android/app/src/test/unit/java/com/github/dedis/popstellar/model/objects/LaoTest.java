package com.github.dedis.popstellar.model.objects;

import com.github.dedis.popstellar.model.network.method.message.data.election.ElectionVersion;
import com.github.dedis.popstellar.model.objects.digitalcash.TransactionObject;
import com.github.dedis.popstellar.model.objects.security.MessageID;
import com.github.dedis.popstellar.model.objects.security.PublicKey;

import org.junit.Test;
import org.mockito.internal.util.collections.Sets;

import java.time.Instant;
import java.util.*;

import static com.github.dedis.popstellar.testutils.Base64DataUtils.generateMessageID;
import static com.github.dedis.popstellar.testutils.Base64DataUtils.generatePublicKey;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.Assert.*;

public class LaoTest {

  private static final String LAO_NAME_1 = "LAO name 1";
  private static final PublicKey ORGANIZER = generatePublicKey();
  private static final Set<PublicKey> WITNESSES =
      Sets.newSet(generatePublicKey(), generatePublicKey());
  private static final Set<PublicKey> WITNESSES_WITH_NULL =
      Sets.newSet(generatePublicKey(), null, generatePublicKey());

  private static final Lao LAO_1 = new Lao(LAO_NAME_1, ORGANIZER, Instant.now().getEpochSecond());

  private static final Election election1 =
      new Election.ElectionBuilder(LAO_1.getId(), 2L, "name 1")
          .setElectionVersion(ElectionVersion.OPEN_BALLOT)
          .build();
  private static final Election election2 =
      new Election.ElectionBuilder(LAO_1.getId(), 2L, "name 2")
          .setElectionVersion(ElectionVersion.OPEN_BALLOT)
          .build();
  private static final Election election3 =
      new Election.ElectionBuilder(LAO_1.getId(), 2L, "name 3")
          .setElectionVersion(ElectionVersion.OPEN_BALLOT)
          .build();

  private static final String electionId1 = election1.getId();
  private static final String electionId2 = election2.getId();
  private static final String electionId3 = election3.getId();

  private static final Map<String, Election> elections =
      new HashMap<String, Election>() {
        {
          put(electionId1, election1);
          put(electionId2, election2);
          put(electionId3, election2);
        }
      };

  @Test
  public void removeElectionTest() {
    LAO_1.setElections(new HashMap<>(elections));
    assertTrue(
        LAO_1.removeElection(
            electionId3)); // we want to assert that we can remove electionId3 successfully
    assertEquals(2, LAO_1.getElections().size());
    assertTrue(LAO_1.getElections().containsKey(electionId1));
    assertTrue(LAO_1.getElections().containsKey(electionId2));
    assertFalse(LAO_1.getElections().containsKey(electionId3));

    // we remove electionId2
    LAO_1.setElections(
        new HashMap<String, Election>() {
          {
            put(electionId1, election1);
            put(null, election1);
            put(electionId3, election3);
          }
        });
    // now the removal of electionId2 can't be done
    assertFalse(LAO_1.removeElection(electionId2));
  }

  @Test
  public void updateElections() {
    LAO_1.setElections(new HashMap<>(elections));
    Election e1 =
        new Election.ElectionBuilder(LAO_1.getId(), Instant.now().getEpochSecond(), "name 1")
            .setElectionVersion(ElectionVersion.OPEN_BALLOT)
            .build();

    LAO_1.updateElection(electionId1, e1);
    assertFalse(LAO_1.getElections().containsKey(electionId1));
    assertTrue(LAO_1.getElections().containsKey(e1.getId()));
    assertTrue(LAO_1.getElections().containsKey(electionId2));
    assertTrue(LAO_1.getElections().containsKey(electionId3));
    assertSame(LAO_1.getElections().get(e1.getId()), e1);

    // we create a different election that has the same Id as the first one
    Election e2 =
        new Election.ElectionBuilder(LAO_1.getId(), Instant.now().getEpochSecond(), "name 1")
            .setElectionVersion(ElectionVersion.OPEN_BALLOT)
            .build();

    LAO_1.updateElection(e1.getId(), e2);
    assertNotSame(LAO_1.getElections().get(e1.getId()), e1);
    assertSame(LAO_1.getElections().get(e1.getId()), e2);
  }

  @Test
  public void updateElectionCallWithNull() {
    assertThrows(IllegalArgumentException.class, () -> LAO_1.updateElection("random", null));
  }

  @Test
  public void createLaoNullParametersTest() {
    assertThrows(IllegalArgumentException.class, () -> new Lao(null, ORGANIZER, 2L));
    assertThrows(IllegalArgumentException.class, () -> new Lao((String) null));
  }

  @Test
  public void createLaoEmptyNameTest() {
    assertThrows(IllegalArgumentException.class, () -> new Lao("", ORGANIZER, 2L));
  }

  @Test
  public void createLaoEmptyIdTest() {
    assertThrows(IllegalArgumentException.class, () -> new Lao(""));
  }

  @Test
  public void setAndGetNameTest() {
    assertThat(LAO_1.getName(), is(LAO_NAME_1));
    LAO_1.setName("New Name");
    assertThat(LAO_1.getName(), is("New Name"));
  }

  @Test
  public void setAndGetOrganizerTest() {
    LAO_1.setOrganizer(ORGANIZER);
    assertThat(LAO_1.getOrganizer(), is(ORGANIZER));
  }

  @Test
  public void setAndGetElections() {
    LAO_1.setElections(elections);
    assertThat(LAO_1.getElections(), is(elections));
  }

  @Test
  public void getElection() {
    LAO_1.setElections(
        new HashMap<String, Election>() {
          {
            put(electionId1, election1);
            put(electionId2, election2);
          }
        });
    assertTrue(LAO_1.getElection(electionId1).isPresent());
    assertThat(LAO_1.getElection(electionId1).get(), is(election1));
  }

  @Test
  public void setAndGetWitnessesTest() {

    LAO_1.setWitnesses(WITNESSES);
    assertThat(LAO_1.getWitnesses(), is(WITNESSES));
  }

  @Test
  public void setNullNameTest() {
    assertThrows(IllegalArgumentException.class, () -> LAO_1.setName(null));
  }

  @Test
  public void setEmptyNameTest() {
    assertThrows(IllegalArgumentException.class, () -> LAO_1.setName(""));
  }

  @Test
  public void setNullWitnessesTest() {
    assertThrows(IllegalArgumentException.class, () -> LAO_1.setWitnesses(null));
    assertThrows(IllegalArgumentException.class, () -> LAO_1.setWitnesses(WITNESSES_WITH_NULL));
  }

  @Test
  public void setAndGetModificationIdTest() {
    MessageID id = generateMessageID();
    LAO_1.setModificationId(id);
    assertThat(LAO_1.getModificationId(), is(id));
  }

  @Test
  public void setAndGetCreation() {
    LAO_1.setCreation(0xFFL);
    assertThat(LAO_1.getCreation(), is(0xFFL));
  }

  @Test
  public void setAndGetLastModified() {
    LAO_1.setLastModified(0xFFL);
    assertThat(LAO_1.getLastModified(), is(0xFFL));
  }

  @Test
  public void setAndGetId() {
    LAO_1.setId("New_Id");
    assertThat(LAO_1.getId(), is("New_Id"));
  }

  @Test
  public void nullTransactionObjectUpdateThrowsException() {
    assertThrows(IllegalArgumentException.class, () -> LAO_1.updateTransactionMaps(null));
  }

  @Test
  public void noRollCallWhenTransactionUpdateThrowsException() {
    List<InputObject> inputs = new ArrayList<>();
    List<OutputObject> outputs = new ArrayList<>();
    Lao lao = new Lao("id");
    TransactionObject transactionObject =
        new TransactionObject(Channel.ROOT, 1, inputs, outputs, 1L, "id");
    assertThrows(IllegalStateException.class, () -> lao.updateTransactionMaps(transactionObject));
  }

  @Test
  public void setEmptyIdThrowsException() {
    assertThrows(IllegalArgumentException.class, () -> LAO_1.setId(""));
  }

  @Test
  public void setNullIdThrowsException() {
    assertThrows(IllegalArgumentException.class, () -> LAO_1.setId(null));
  }

  @Test
  public void setPendingUpdatesTest() {
    PendingUpdate update = new PendingUpdate(1L, new MessageID("foo"));
    LAO_1.setPendingUpdates(Collections.singleton(update));
    assertTrue(LAO_1.getPendingUpdates().contains(update));
  }

  @Test
  public void witnessMapTest() {
    MessageID messageID = new MessageID("foo");
    WitnessMessage witnessMessage = new WitnessMessage(messageID);
    LAO_1.updateWitnessMessage(messageID, witnessMessage);
    assertEquals(LAO_1.getWitnessMessages().get(messageID), witnessMessage);
  }
}

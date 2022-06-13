package com.github.dedis.popstellar.model.objects;

import static com.github.dedis.popstellar.testutils.Base64DataUtils.generateMessageID;
import static com.github.dedis.popstellar.testutils.Base64DataUtils.generatePublicKey;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotSame;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertThrows;
import static org.junit.Assert.assertTrue;

import com.github.dedis.popstellar.model.network.method.message.data.election.ElectionVersion;
import com.github.dedis.popstellar.model.objects.security.MessageID;
import com.github.dedis.popstellar.model.objects.security.PublicKey;
import com.github.dedis.popstellar.utility.error.keys.NoRollCallException;

import org.junit.Test;
import org.mockito.internal.util.collections.Sets;

import java.time.Instant;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

public class LaoTest {

  private static final String LAO_NAME_1 = "LAO name 1";
  private static final PublicKey ORGANIZER = generatePublicKey();
  private static final String rollCallId1 = "rollCallId1";
  private static final String rollCallId2 = "rollCallId2";
  private static final String rollCallId3 = "rollCallId3";
  private static final String electionId1 = "electionId1";
  private static final String electionId2 = "electionId2";
  private static final String electionId3 = "electionId3";
  private static final Set<PublicKey> WITNESSES =
      Sets.newSet(generatePublicKey(), generatePublicKey());
  private static final Set<PublicKey> WITNESSES_WITH_NULL =
      Sets.newSet(generatePublicKey(), null, generatePublicKey());

  private static final Lao LAO_1 = new Lao(LAO_NAME_1, ORGANIZER, Instant.now().getEpochSecond());
  private static final Map<String, RollCall> rollCalls =
      new HashMap<String, RollCall>() {
        {
          put(rollCallId1, new RollCall(rollCallId1));
          put(rollCallId2, new RollCall(rollCallId2));
          put(rollCallId3, new RollCall(rollCallId3));
        }
      };
  private static final Map<String, Election> elections =
      new HashMap<String, Election>() {
        {
          put(electionId1, new Election(LAO_1.getId(), 2L, "name 1", ElectionVersion.OPEN_BALLOT));
          put(electionId2, new Election(LAO_1.getId(), 2L, "name 2", ElectionVersion.OPEN_BALLOT));
          put(electionId3, new Election(LAO_1.getId(), 2L, "name 3", ElectionVersion.OPEN_BALLOT));
        }
      };

  @Test
  public void removeRollCallTest() {
    LAO_1.setRollCalls(new HashMap<>(rollCalls));
    assertTrue(
        LAO_1.removeRollCall(
            rollCallId3)); // we want to assert that we can remove rollCallId3 successfully
    assertEquals(2, LAO_1.getRollCalls().size());
    assertTrue(LAO_1.getRollCalls().containsKey(rollCallId1));
    assertTrue(LAO_1.getRollCalls().containsKey(rollCallId2));
    assertFalse(LAO_1.getRollCalls().containsKey(rollCallId3));

    LAO_1.setRollCalls(
        new HashMap<String, RollCall>() {
          {
            put(rollCallId1, new RollCall(rollCallId1));
            put(null, new RollCall(null));
            put(rollCallId3, new RollCall(rollCallId3));
          }
        });
    assertFalse(LAO_1.removeRollCall(rollCallId2));
  }

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
            put(electionId1, new Election(LAO_1.getId(), 2L, "name 1", ElectionVersion.OPEN_BALLOT));
            put(null, new Election(LAO_1.getId(), 2L, "name 1", ElectionVersion.OPEN_BALLOT));
            put(electionId3, new Election(LAO_1.getId(), 2L, "name 3", ElectionVersion.OPEN_BALLOT));
          }
        });
    // now the removal of electionId2 can't be done
    assertFalse(LAO_1.removeElection(electionId2));
  }

  @Test
  public void updateRollCalls() {

    LAO_1.setRollCalls(new HashMap<>(rollCalls));
    RollCall r1 = new RollCall("New r1 id");
    LAO_1.updateRollCall(rollCallId1, r1);
    assertFalse(LAO_1.getRollCalls().containsKey(rollCallId1));
    assertTrue(LAO_1.getRollCalls().containsKey("New r1 id"));
    assertTrue(LAO_1.getRollCalls().containsKey(rollCallId2));
    assertTrue(LAO_1.getRollCalls().containsKey(rollCallId3));
    assertSame(LAO_1.getRollCalls().get("New r1 id"), r1);

    // we create a different roll call that has the same Id as the first one
    RollCall r2 = new RollCall(r1.getId());

    LAO_1.updateRollCall(r1.getId(), r2);
    assertNotSame(LAO_1.getRollCalls().get(r1.getId()), r1);
    assertSame(LAO_1.getRollCalls().get(r1.getId()), r2);
  }

  @Test
  public void updateRollCallWithNull() {
    assertThrows(IllegalArgumentException.class, () -> LAO_1.updateRollCall("random", null));
  }

  @Test
  public void updateElections() {
    LAO_1.setElections(new HashMap<>(elections));
    Election e1 =
        new Election(LAO_1.getId(), Instant.now().getEpochSecond(), "name 1", ElectionVersion.OPEN_BALLOT);
    e1.setId("New e1 id");
    LAO_1.updateElection(electionId1, e1);
    assertFalse(LAO_1.getElections().containsKey(electionId1));
    assertTrue(LAO_1.getElections().containsKey("New e1 id"));
    assertTrue(LAO_1.getElections().containsKey(electionId2));
    assertTrue(LAO_1.getElections().containsKey(electionId3));
    assertSame(LAO_1.getElections().get("New e1 id"), e1);

    // we create a different election that has the same Id as the first one
    Election e2 =
        new Election(LAO_1.getId(), Instant.now().getEpochSecond(), "name 1", ElectionVersion.OPEN_BALLOT);
    e2.setId(e1.getId());

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
    assertThrows(IllegalArgumentException.class, () -> new Lao(null));
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
  public void setAndGetRollCalls() {
    LAO_1.setRollCalls(rollCalls);
    assertThat(LAO_1.getRollCalls(), is(rollCalls));
  }

  @Test
  public void getRollCall() {
    RollCall r1 = new RollCall(rollCallId1);
    RollCall r2 = new RollCall(rollCallId1);
    LAO_1.setRollCalls(
        new HashMap<String, RollCall>() {
          {
            put(rollCallId1, r1);
            put(rollCallId2, r2);
          }
        });
    assertThat(LAO_1.getRollCall(rollCallId1).get(), is(r1));
    r1.setEnd(1);
    r2.setEnd(2);
    try {
      assertEquals(r2, LAO_1.lastRollCallClosed());
    } catch (NoRollCallException e) {
      throw new IllegalArgumentException();
    }
  }

  @Test
  public void setAndGetElections() {
    LAO_1.setElections(elections);
    assertThat(LAO_1.getElections(), is(elections));
  }

  @Test
  public void getElection() {
    Election e1 =
        new Election(LAO_1.getId(), Instant.now().getEpochSecond(), "name 1", ElectionVersion.OPEN_BALLOT);
    Election e2 =
        new Election(LAO_1.getId(), Instant.now().getEpochSecond(), "name 1", ElectionVersion.OPEN_BALLOT);
    LAO_1.setElections(
        new HashMap<String, Election>() {
          {
            put(electionId1, e1);
            put(electionId2, e2);
          }
        });
    assertThat(LAO_1.getElection(electionId1).get(), is(e1));
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
}

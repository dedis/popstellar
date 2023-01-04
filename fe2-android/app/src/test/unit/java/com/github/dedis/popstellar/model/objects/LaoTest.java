package com.github.dedis.popstellar.model.objects;

import com.github.dedis.popstellar.model.objects.security.MessageID;
import com.github.dedis.popstellar.model.objects.security.PublicKey;

import org.junit.Test;
import org.mockito.internal.util.collections.Sets;

import java.time.Instant;
import java.util.Collections;
import java.util.Set;

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

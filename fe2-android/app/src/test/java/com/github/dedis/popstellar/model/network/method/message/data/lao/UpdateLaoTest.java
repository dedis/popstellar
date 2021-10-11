package com.github.dedis.popstellar.model.network.method.message.data.lao;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotEquals;

import android.util.ArraySet;

import com.github.dedis.popstellar.model.network.method.message.data.Action;
import com.github.dedis.popstellar.model.network.method.message.data.Objects;
import com.github.dedis.popstellar.model.objects.Lao;
import com.github.dedis.popstellar.utility.security.Hash;

import org.junit.Test;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

public class UpdateLaoTest {

  private final String name = "New name";
  private final long time = 0xC972;
  private final String organizer = " Organizer Id ";
  private final long lastModified = 972;

  private final Set<String> witnesses = new HashSet<>(Arrays.asList("0x3434", "0x4747"));
  private final UpdateLao updateLao = new UpdateLao("organizer", 10, name, lastModified, witnesses);

  @Test
  public void generateUpdateLaoIdTest() {
    UpdateLao updateLao = new UpdateLao(organizer, time, name, time, new ArraySet<>());
    // Hash(organizer||creation||name)
    String expectedId = Hash.hash(organizer, Long.toString(time), updateLao.getName());
    assertThat(updateLao.getId(), is(expectedId));
  }

  @Test
  public void getObjectTest() {
    assertThat(updateLao.getObject(), is(Objects.LAO.getObject()));
  }

  @Test
  public void getActionTest() {
    assertThat(updateLao.getAction(), is(Action.UPDATE.getAction()));
  }

  @Test
  public void getNameTest() {
    assertThat(updateLao.getName(), is(name));
  }

  @Test
  public void getLastModifiedTest() {
    assertThat(updateLao.getLastModified(), is(lastModified));
  }

  @Test
  public void getIdTest() {
    assertThat(updateLao.getId(), is(Lao.generateLaoId("organizer", 10, name)));
  }

  @Test
  public void getWitnessesTest() {
    assertThat(updateLao.getWitnesses(), is(witnesses));
  }

  @Test
  public void isEqual() {
    assertEquals(updateLao, new UpdateLao("organizer", 10, name, lastModified, witnesses));
    // different creation time so the id won't be the same
    assertNotEquals(updateLao, new UpdateLao("organizer", 20, name, lastModified, witnesses));
    // different organizer so the id won't be the same
    assertNotEquals(updateLao, new UpdateLao("different", 10, name, lastModified, witnesses));
    // different name
    assertNotEquals(updateLao, new UpdateLao("organizer", 10, "random", lastModified, witnesses));
    // different witnesses
    assertNotEquals(
        updateLao,
        new UpdateLao("organizer", 10, name, lastModified, new HashSet<>(Arrays.asList("0x3434"))));
  }
}

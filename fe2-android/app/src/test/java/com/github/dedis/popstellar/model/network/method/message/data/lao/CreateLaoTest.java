package com.github.dedis.popstellar.model.network.method.message.data.lao;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertThrows;

import com.github.dedis.popstellar.model.network.method.message.data.Action;
import com.github.dedis.popstellar.model.network.method.message.data.Objects;
import com.github.dedis.popstellar.model.objects.Lao;
import com.github.dedis.popstellar.utility.security.Hash;

import org.junit.Test;

import java.util.Arrays;
import java.util.List;
import java.util.concurrent.TimeUnit;

public class CreateLaoTest {

  private final String name = " Lao name";
  private final long creation = 0xC972;
  private final String organizer = " Organizer Id ";
  private final List<String> witnesses = Arrays.asList("0x3434", "0x4747");
  private final String id = Lao.generateLaoId(organizer, creation, name);
  CreateLao createLao = new CreateLao(id, name, creation, organizer, witnesses);

  @Test
  public void wrongIdTest() {
    assertThrows(
        IllegalArgumentException.class,
        () -> new CreateLao("wrong Id", name, creation, organizer, witnesses));
  }

  @Test
  public void generateCreateLaoIdTest() {
    CreateLao createLao = new CreateLao(name, organizer);
    // Hash(organizer||creation||name)
    String expectedId =
        Hash.hash(
            createLao.getOrganizer(), Long.toString(createLao.getCreation()), createLao.getName());
    assertThat(createLao.getId(), is(expectedId));
  }

  @Test
  public void getObjectTest() {
    assertThat(createLao.getObject(), is(Objects.LAO.getObject()));
  }

  @Test
  public void getActionTest() {
    assertThat(createLao.getAction(), is(Action.CREATE.getAction()));
  }

  @Test
  public void getIdTest() {
    assertThat(createLao.getId(), is(id));
  }

  @Test
  public void getNameTest() {
    assertThat(createLao.getName(), is(name));
  }

  @Test
  public void getOrganizerTest() {
    assertThat(createLao.getOrganizer(), is(organizer));
  }

  @Test
  public void isEqual() {
    CreateLao createLao1 = new CreateLao(name, organizer);
    try {
      TimeUnit.SECONDS.sleep(1);
      CreateLao createLao2 = new CreateLao(name, organizer);

      // they don't have the same creation time
      assertNotEquals(createLao1, createLao2);
    } catch (InterruptedException e) {
      e.printStackTrace();
    }
    assertEquals(createLao, new CreateLao(id, name, creation, organizer, witnesses));
    assertEquals(new CreateLao(name, organizer), new CreateLao(name, organizer));
    assertNotEquals(createLao1, new CreateLao("random", organizer));
    assertNotEquals(createLao1, new CreateLao(name, "random"));
  }
}

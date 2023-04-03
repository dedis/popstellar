package com.github.dedis.popstellar.model.network.method.message.data.lao;

import com.github.dedis.popstellar.model.network.JsonTestUtils;
import com.github.dedis.popstellar.model.network.method.message.data.Action;
import com.github.dedis.popstellar.model.network.method.message.data.Objects;
import com.github.dedis.popstellar.model.objects.Lao;
import com.github.dedis.popstellar.model.objects.security.PublicKey;
import com.github.dedis.popstellar.testutils.Base64DataUtils;
import com.github.dedis.popstellar.utility.security.Hash;
import com.google.gson.JsonParseException;

import org.junit.Test;

import java.time.Instant;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.TimeUnit;

import static com.github.dedis.popstellar.testutils.Base64DataUtils.generatePublicKey;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.Assert.*;

public class CreateLaoTest {

  private final String name = " Lao name";
  private final long creation = 0xC972;
  private final PublicKey organizer = generatePublicKey();
  private final List<PublicKey> witnesses = Arrays.asList(generatePublicKey(), generatePublicKey());
  private final String id = Lao.generateLaoId(organizer, creation, name);
  private final CreateLao createLao = new CreateLao(id, name, creation, organizer, witnesses);

  @Test
  public void wrongIdTest() {
    assertThrows(
        IllegalArgumentException.class,
        () -> new CreateLao("wrong Id", name, creation, organizer, witnesses));
  }

  @Test
  public void futureCreationTimeTest() {
    long futureCreation = Instant.now().getEpochSecond() + 1000;
    assertThrows(
        IllegalArgumentException.class,
        () -> new CreateLao(id, name, futureCreation, organizer, witnesses));
  }

  @Test
  public void generateCreateLaoIdTest() {
    CreateLao createLao = new CreateLao(name, organizer);
    // Hash(organizer||creation||name)
    String expectedId =
        Hash.hash(
            createLao.getOrganizer().getEncoded(),
            Long.toString(createLao.getCreation()),
            createLao.getName());
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
    assertNotEquals(
        createLao1, new CreateLao(name, Base64DataUtils.generatePublicKeyOtherThan(organizer)));
  }

  @Test
  public void jsonValidationTest() {
    JsonTestUtils.testData(createLao);

    String pathDir = "protocol/examples/messageData/lao_create/";
    String jsonInvalid1 =
        JsonTestUtils.loadFile(pathDir + "wrong_lao_create_additional_params.json");
    String jsonInvalid2 = JsonTestUtils.loadFile(pathDir + "wrong_lao_create_missing_params.json");
    String jsonInvalid3 = JsonTestUtils.loadFile(pathDir + "bad_lao_create_creation_negative.json");
    assertThrows(JsonParseException.class, () -> JsonTestUtils.parse(jsonInvalid1));
    assertThrows(JsonParseException.class, () -> JsonTestUtils.parse(jsonInvalid2));
    assertThrows(JsonParseException.class, () -> JsonTestUtils.parse(jsonInvalid3));
  }
}

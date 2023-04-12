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
import org.mockito.internal.util.collections.Sets;

import java.time.Instant;
import java.util.HashSet;
import java.util.Set;

import static com.github.dedis.popstellar.testutils.Base64DataUtils.generatePublicKey;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.Assert.*;

public class UpdateLaoTest {

  private final String name = "New name";
  private final PublicKey organizer = generatePublicKey();
  private final long lastModified = Instant.now().getEpochSecond();
  private final long creation = lastModified - 10;

  private final Set<PublicKey> witnesses = Sets.newSet(generatePublicKey(), generatePublicKey());
  private final UpdateLao updateLao =
      new UpdateLao(organizer, creation, name, lastModified, witnesses);

  @Test
  public void generateUpdateLaoIdTest() {
    UpdateLao updateLao = new UpdateLao(organizer, creation, name, lastModified, new HashSet<>());
    // Hash(organizer||creation||name)
    String expectedId =
        Hash.hash(organizer.getEncoded(), Long.toString(creation), updateLao.getName());
    assertThat(updateLao.getId(), is(expectedId));
  }

  @Test(expected = IllegalArgumentException.class)
  public void constructorFailsEmptyNameTest() {
    new UpdateLao(organizer, creation, "", lastModified, new HashSet<>());
  }

  @Test(expected = IllegalArgumentException.class)
  public void constructorFailsFutureCreationTimeTest() {
    long futureCreation = Instant.now().getEpochSecond() + 1000;
    new UpdateLao(organizer, futureCreation, name, lastModified, new HashSet<>());
  }

  @Test(expected = IllegalArgumentException.class)
  public void constructorFailsFutureModificationTimeTest() {
    long futureModification = Instant.now().getEpochSecond() + 1000;
    new UpdateLao(organizer, creation, name, futureModification, new HashSet<>());
  }

  @Test(expected = IllegalArgumentException.class)
  public void constructorFailsModificationBeforeCreationTimeTest() {
    new UpdateLao(organizer, creation, name, creation - 10, new HashSet<>());
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
    assertThat(updateLao.getId(), is(Lao.generateLaoId(organizer, creation, name)));
  }

  @Test
  public void getWitnessesTest() {
    assertThat(updateLao.getWitnesses(), is(witnesses));
  }

  @Test
  public void isEqual() {
    assertEquals(updateLao, new UpdateLao(organizer, creation, name, lastModified, witnesses));
    // different creation time so the id won't be the same
    assertNotEquals(updateLao, new UpdateLao(organizer, 20, name, lastModified, witnesses));
    // different organizer so the id won't be the same
    assertNotEquals(
        updateLao,
        new UpdateLao(
            Base64DataUtils.generatePublicKeyOtherThan(organizer),
            10,
            name,
            lastModified,
            witnesses));
    // different name
    assertNotEquals(updateLao, new UpdateLao(organizer, 10, "random", lastModified, witnesses));
    // different witnesses
    assertNotEquals(
        updateLao,
        new UpdateLao(organizer, 10, name, lastModified, Sets.newSet(generatePublicKey())));
  }

  @Test
  public void jsonValidationTest() {
    JsonTestUtils.testData(updateLao);

    String pathDir = "protocol/examples/messageData/lao_update/";
    String jsonInvalid1 =
        JsonTestUtils.loadFile(pathDir + "wrong_lao_update_additional_params.json");
    String jsonInvalid2 = JsonTestUtils.loadFile(pathDir + "wrong_lao_update_missing_params.json");
    String jsonInvalid3 =
        JsonTestUtils.loadFile(pathDir + "bad_lao_update_negative_last_modified.json");
    String jsonInvalid4 =
        JsonTestUtils.loadFile(pathDir + "bad_lao_update_witness_not_base64.json");
    assertThrows(JsonParseException.class, () -> JsonTestUtils.parse(jsonInvalid1));
    assertThrows(JsonParseException.class, () -> JsonTestUtils.parse(jsonInvalid2));
    assertThrows(JsonParseException.class, () -> JsonTestUtils.parse(jsonInvalid3));
    assertThrows(JsonParseException.class, () -> JsonTestUtils.parse(jsonInvalid4));
  }
}

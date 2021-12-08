package com.github.dedis.popstellar.model.network.method.message.data.lao;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertThrows;

import com.github.dedis.popstellar.model.network.JsonTestUtils;
import com.github.dedis.popstellar.model.network.method.message.PublicKeySignaturePair;
import com.github.dedis.popstellar.model.network.method.message.data.Action;
import com.github.dedis.popstellar.model.network.method.message.data.Objects;
import com.github.dedis.popstellar.model.objects.Lao;
import com.google.gson.JsonParseException;

import org.junit.Test;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class StateLaoTest {

  private final String name = " Lao name";
  private final long creation = 0x10;
  private final long lastModified = 0x999;
  private final String organizer = "Organizer Id";
  private final String modificationId = " modification id";
  private final Set<String> witnesses = new HashSet<>(Arrays.asList("0x3434", "0x4747"));
  private final String id = Lao.generateLaoId(organizer, creation, name);
  private final List<PublicKeySignaturePair> modificationSignatures =
      Collections.singletonList(new PublicKeySignaturePair(new byte[10], new byte[10]));
  private final StateLao stateLao =
      new StateLao(
          id,
          name,
          creation,
          lastModified,
          organizer,
          modificationId,
          witnesses,
          modificationSignatures);

  @Test
  public void wrongIdTest() {
    assertThrows(
        IllegalArgumentException.class,
        () ->
            new StateLao(
                "wrong id",
                name,
                creation,
                lastModified,
                organizer,
                modificationId,
                witnesses,
                modificationSignatures));
  }

  @Test
  public void getIdTest() {
    assertThat(stateLao.getId(), is(id));
  }

  @Test
  public void getObjectTest() {
    assertThat(stateLao.getObject(), is(Objects.LAO.getObject()));
  }

  @Test
  public void getActionTest() {
    assertThat(stateLao.getAction(), is(Action.STATE.getAction()));
  }

  @Test
  public void getNameTest() {
    assertThat(stateLao.getName(), is(name));
  }

  @Test
  public void getCreationTest() {
    assertThat(stateLao.getCreation(), is(creation));
  }

  @Test
  public void getLastModifiedTest() {
    assertThat(stateLao.getLastModified(), is(lastModified));
  }

  @Test
  public void getOrganizerTest() {
    assertThat(stateLao.getOrganizer(), is(organizer));
  }

  @Test
  public void getWitnessesTest() {
    assertThat(stateLao.getWitnesses(), is(witnesses));
  }

  @Test
  public void getModificationIdTest() {
    assertThat(stateLao.getModificationId(), is(modificationId));
  }

  @Test
  public void getModificationIdSignaturesTest() {
    assertThat(stateLao.getModificationId(), is(modificationId));
  }

  @Test
  public void isEqualTest() {
    assertEquals(
        stateLao,
        new StateLao(
            id,
            name,
            creation,
            lastModified,
            organizer,
            modificationId,
            witnesses,
            modificationSignatures));
    // The modification id isn't taken into account to know if they are equal
    assertEquals(
        stateLao,
        new StateLao(
            id,
            name,
            creation,
            lastModified,
            organizer,
            "random",
            witnesses,
            modificationSignatures));
    // same goes for modification signatures
    assertEquals(
        stateLao,
        new StateLao(id, name, creation, lastModified, organizer, modificationId, witnesses, null));
    String random = " random string";
    String newId = Lao.generateLaoId(organizer, creation, random);
    assertNotEquals(
        stateLao,
        new StateLao(
            newId,
            random,
            creation,
            lastModified,
            organizer,
            modificationId,
            witnesses,
            modificationSignatures));
    newId = Lao.generateLaoId(random, creation, name);
    assertNotEquals(
        stateLao,
        new StateLao(
            newId,
            name,
            creation,
            lastModified,
            random,
            modificationId,
            witnesses,
            modificationSignatures));
    newId = Lao.generateLaoId(organizer, 99, name);
    assertNotEquals(
        stateLao,
        new StateLao(
            newId,
            name,
            99,
            lastModified,
            organizer,
            modificationId,
            witnesses,
            modificationSignatures));
    assertNotEquals(
        stateLao,
        new StateLao(
            id,
            name,
            creation,
            1000,
            organizer,
            modificationId,
            witnesses,
            modificationSignatures));
    assertNotEquals(
        stateLao,
        new StateLao(
            id,
            name,
            creation,
            lastModified,
            organizer,
            modificationId,
            new HashSet<>(Collections.singletonList("0x3434")),
            modificationSignatures));
  }

  @Test
  public void jsonValidationTest() {
    JsonTestUtils.testData(stateLao);

    String pathDir = "protocol/examples/messageData/lao_state/";
    String jsonInvalid1 =
        JsonTestUtils.loadFile(pathDir + "wrong_lao_state_additional_params.json");
    String jsonInvalid2 = JsonTestUtils.loadFile(pathDir + "wrong_lao_state_missing_params.json");
    assertThrows(JsonParseException.class, () -> JsonTestUtils.parse(jsonInvalid1));
    assertThrows(JsonParseException.class, () -> JsonTestUtils.parse(jsonInvalid2));
  }
}

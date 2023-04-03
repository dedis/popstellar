package com.github.dedis.popstellar.model.network.method.message.data.lao;

import com.github.dedis.popstellar.model.network.JsonTestUtils;
import com.github.dedis.popstellar.model.network.method.message.PublicKeySignaturePair;
import com.github.dedis.popstellar.model.network.method.message.data.Action;
import com.github.dedis.popstellar.model.network.method.message.data.Objects;
import com.github.dedis.popstellar.model.objects.Lao;
import com.github.dedis.popstellar.model.objects.security.MessageID;
import com.github.dedis.popstellar.model.objects.security.PublicKey;
import com.github.dedis.popstellar.testutils.Base64DataUtils;
import com.google.gson.JsonParseException;

import org.junit.Test;
import org.mockito.internal.util.collections.Sets;

import java.time.Instant;
import java.util.*;

import static com.github.dedis.popstellar.testutils.Base64DataUtils.*;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.Assert.*;

public class StateLaoTest {

  private final String name = " Lao name";
  private final long creation = 0x10;
  private final long lastModified = 0x999;
  private final PublicKey organizer = generatePublicKey();
  private final MessageID modificationId = generateMessageID();
  private final Set<PublicKey> witnesses = Sets.newSet(generatePublicKey(), generatePublicKey());
  private final String id = Lao.generateLaoId(organizer, creation, name);
  private final List<PublicKeySignaturePair> modificationSignatures =
      Collections.singletonList(
          new PublicKeySignaturePair(generatePublicKey(), generateSignature()));
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
  public void futureCreationTimeTest() {
    long futureCreation = Instant.now().getEpochSecond() + 1000;
    assertThrows(
        IllegalArgumentException.class,
        () ->
            new StateLao(
                id,
                name,
                futureCreation,
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
            Base64DataUtils.generateMessageIDOtherThan(modificationId),
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
    PublicKey newKey = Base64DataUtils.generatePublicKeyOtherThan(organizer);
    newId = Lao.generateLaoId(newKey, creation, name);
    assertNotEquals(
        stateLao,
        new StateLao(
            newId,
            name,
            creation,
            lastModified,
            newKey,
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
            Sets.newSet(generatePublicKey()),
            modificationSignatures));
  }

  @Test
  public void jsonValidationTest() {
    JsonTestUtils.testData(stateLao);

    String pathDir = "protocol/examples/messageData/lao_state/";
    String jsonInvalid1 =
        JsonTestUtils.loadFile(pathDir + "wrong_lao_state_additional_params.json");
    String jsonInvalid2 = JsonTestUtils.loadFile(pathDir + "wrong_lao_state_missing_params.json");

    String jsonInvalid3 = JsonTestUtils.loadFile(pathDir + "bad_lao_state_creation_negative.json");
    String jsonInvalid4 = JsonTestUtils.loadFile(pathDir + "bad_lao_state_id_not_base64.json");
    String jsonInvalid5 =
        JsonTestUtils.loadFile(pathDir + "bad_lao_state_last_modified_negative.json");
    String jsonInvalid6 =
        JsonTestUtils.loadFile(pathDir + "bad_lao_state_organizer_not_base64.json");
    String jsonInvalid7 = JsonTestUtils.loadFile(pathDir + "bad_lao_state_witness_not_base64.json");
    String jsonInvalid8 =
        JsonTestUtils.loadFile(pathDir + "bad_lao_state_creation_after_last_modified.json");
    assertThrows(JsonParseException.class, () -> JsonTestUtils.parse(jsonInvalid1));
    assertThrows(JsonParseException.class, () -> JsonTestUtils.parse(jsonInvalid2));
    assertThrows(JsonParseException.class, () -> JsonTestUtils.parse(jsonInvalid3));
    // assertThrows(JsonParseException.class, () -> JsonTestUtils.parse(jsonInvalid4));
    assertThrows(JsonParseException.class, () -> JsonTestUtils.parse(jsonInvalid5));
    assertThrows(JsonParseException.class, () -> JsonTestUtils.parse(jsonInvalid6));
    assertThrows(JsonParseException.class, () -> JsonTestUtils.parse(jsonInvalid7));
    assertThrows(IllegalArgumentException.class, () -> JsonTestUtils.parse(jsonInvalid8));
  }
}

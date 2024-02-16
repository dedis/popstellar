package com.github.dedis.popstellar.model.network.method.message.data.lao

import androidx.test.ext.junit.runners.AndroidJUnit4
import com.github.dedis.popstellar.model.network.JsonTestUtils.loadFile
import com.github.dedis.popstellar.model.network.JsonTestUtils.parse
import com.github.dedis.popstellar.model.network.JsonTestUtils.testData
import com.github.dedis.popstellar.model.network.method.message.PublicKeySignaturePair
import com.github.dedis.popstellar.model.network.method.message.data.Action
import com.github.dedis.popstellar.model.network.method.message.data.Objects
import com.github.dedis.popstellar.model.objects.Lao.Companion.generateLaoId
import com.github.dedis.popstellar.testutils.Base64DataUtils
import com.google.gson.JsonParseException
import java.time.Instant
import org.hamcrest.CoreMatchers
import org.hamcrest.MatcherAssert
import org.junit.Assert
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.internal.util.collections.Sets

@RunWith(AndroidJUnit4::class)
class StateLaoTest {
  private val name = " Lao name"
  private val creation = Instant.now().epochSecond - 10000
  private val lastModified = creation + 1
  private val organizer = Base64DataUtils.generatePublicKey()
  private val modificationId = Base64DataUtils.generateMessageID()
  private val witnesses =
    Sets.newSet(Base64DataUtils.generatePublicKey(), Base64DataUtils.generatePublicKey())
  private val id = generateLaoId(organizer, creation, name)
  private val modificationSignatures =
    listOf(
      PublicKeySignaturePair(
        Base64DataUtils.generatePublicKey(),
        Base64DataUtils.generateSignature()
      )
    )
  private val stateLao =
    StateLao(
      id,
      name,
      creation,
      lastModified,
      organizer,
      modificationId,
      witnesses,
      modificationSignatures
    )

  @Test(expected = IllegalArgumentException::class)
  fun constructorFailsIdNotBase64Test() {
    StateLao(
      "wrong id",
      name,
      creation,
      lastModified,
      organizer,
      modificationId,
      witnesses,
      modificationSignatures
    )
  }

  @Test(expected = IllegalArgumentException::class)
  fun constructorFailsInvalidIdHashTest() {
    val wrongId = "A" + id.substring(1)
    StateLao(
      wrongId,
      name,
      creation,
      lastModified,
      organizer,
      modificationId,
      witnesses,
      modificationSignatures
    )
  }

  @Test(expected = IllegalArgumentException::class)
  fun constructorFailsEmptyNameTest() {
    StateLao(
      id,
      "",
      creation,
      lastModified,
      organizer,
      modificationId,
      witnesses,
      modificationSignatures
    )
  }

  @Test(expected = IllegalArgumentException::class)
  fun constructorFailsFutureCreationTimeTest() {
    val futureCreation = Instant.now().epochSecond + 1000
    StateLao(
      id,
      name,
      futureCreation,
      lastModified,
      organizer,
      modificationId,
      witnesses,
      modificationSignatures
    )
  }

  @Test(expected = IllegalArgumentException::class)
  fun constructorFailsFutureModificationTimeTest() {
    val futureModification = Instant.now().epochSecond + 1000
    StateLao(
      id,
      name,
      creation,
      futureModification,
      organizer,
      modificationId,
      witnesses,
      modificationSignatures
    )
  }

  @Test(expected = IllegalArgumentException::class)
  fun constructorFailsModificationBeforeCreationTimeTest() {
    StateLao(
      id,
      name,
      creation,
      creation - 10,
      organizer,
      modificationId,
      witnesses,
      modificationSignatures
    )
  }

  @Test
  fun idTest() {
    MatcherAssert.assertThat(stateLao.id, CoreMatchers.`is`(id))
  }

  @Test
  fun objectTest() {
    MatcherAssert.assertThat(stateLao.`object`, CoreMatchers.`is`(Objects.LAO.`object`))
  }

  @Test
  fun actionTest() {
    MatcherAssert.assertThat(stateLao.action, CoreMatchers.`is`(Action.STATE.action))
  }

  @Test
  fun nameTest() {
    MatcherAssert.assertThat(stateLao.name, CoreMatchers.`is`(name))
  }

  @Test
  fun creationTest() {
    MatcherAssert.assertThat(stateLao.creation, CoreMatchers.`is`(creation))
  }

  @Test
  fun lastModifiedTest() {
    MatcherAssert.assertThat(stateLao.lastModified, CoreMatchers.`is`(lastModified))
  }

  @Test
  fun organizerTest() {
    MatcherAssert.assertThat(stateLao.organizer, CoreMatchers.`is`(organizer))
  }

  @Test
  fun witnessesTest() {
    MatcherAssert.assertThat(stateLao.witnesses, CoreMatchers.`is`(witnesses))
  }

  @Test
  fun modificationIdTest() {
    MatcherAssert.assertThat(stateLao.modificationId, CoreMatchers.`is`(modificationId))
  }

  @Test
  fun modificationIdSignaturesTest() {
    MatcherAssert.assertThat(stateLao.modificationId, CoreMatchers.`is`(modificationId))
  }

  @Test
  fun isEqualTest() {
    Assert.assertEquals(
      stateLao,
      StateLao(
        id,
        name,
        creation,
        lastModified,
        organizer,
        modificationId,
        witnesses,
        modificationSignatures
      )
    )
    // The modification id isn't taken into account to know if they are equal
    Assert.assertEquals(
      stateLao,
      StateLao(
        id,
        name,
        creation,
        lastModified,
        organizer,
        Base64DataUtils.generateMessageIDOtherThan(modificationId),
        witnesses,
        modificationSignatures
      )
    )
    // same goes for modification signatures
    Assert.assertEquals(
      stateLao,
      StateLao(id, name, creation, lastModified, organizer, modificationId, witnesses, null)
    )
    val random = " random string"
    var newId = generateLaoId(organizer, creation, random)
    Assert.assertNotEquals(
      stateLao,
      StateLao(
        newId,
        random,
        creation,
        lastModified,
        organizer,
        modificationId,
        witnesses,
        modificationSignatures
      )
    )
    val newKey = Base64DataUtils.generatePublicKeyOtherThan(organizer)
    newId = generateLaoId(newKey, creation, name)
    Assert.assertNotEquals(
      stateLao,
      StateLao(
        newId,
        name,
        creation,
        lastModified,
        newKey,
        modificationId,
        witnesses,
        modificationSignatures
      )
    )
    newId = generateLaoId(organizer, creation - 1, name)
    Assert.assertNotEquals(
      stateLao,
      StateLao(
        newId,
        name,
        creation - 1,
        lastModified,
        organizer,
        modificationId,
        witnesses,
        modificationSignatures
      )
    )
    Assert.assertNotEquals(
      stateLao,
      StateLao(
        id,
        name,
        creation,
        creation + 10,
        organizer,
        modificationId,
        witnesses,
        modificationSignatures
      )
    )
    Assert.assertNotEquals(
      stateLao,
      StateLao(
        id,
        name,
        creation,
        lastModified,
        organizer,
        modificationId,
        Sets.newSet(Base64DataUtils.generatePublicKey()),
        modificationSignatures
      )
    )
  }

  @Test
  fun jsonValidationTest() {
    testData(stateLao)
    val pathDir = "protocol/examples/messageData/lao_state/"
    val jsonInvalid1 = loadFile(pathDir + "wrong_lao_state_additional_params.json")
    val jsonInvalid2 = loadFile(pathDir + "wrong_lao_state_missing_params.json")
    val jsonInvalid3 = loadFile(pathDir + "bad_lao_state_creation_negative.json")
    val jsonInvalid4 = loadFile(pathDir + "bad_lao_state_last_modified_negative.json")
    val jsonInvalid5 = loadFile(pathDir + "bad_lao_state_organizer_not_base64.json")
    val jsonInvalid6 = loadFile(pathDir + "bad_lao_state_witness_not_base64.json")

    Assert.assertThrows(JsonParseException::class.java) { parse(jsonInvalid1) }
    Assert.assertThrows(JsonParseException::class.java) { parse(jsonInvalid2) }
    Assert.assertThrows(JsonParseException::class.java) { parse(jsonInvalid3) }
    Assert.assertThrows(JsonParseException::class.java) { parse(jsonInvalid4) }
    Assert.assertThrows(JsonParseException::class.java) { parse(jsonInvalid5) }
    Assert.assertThrows(JsonParseException::class.java) { parse(jsonInvalid6) }
  }
}

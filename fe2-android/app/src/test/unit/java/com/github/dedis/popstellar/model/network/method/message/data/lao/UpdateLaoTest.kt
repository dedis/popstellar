package com.github.dedis.popstellar.model.network.method.message.data.lao

import androidx.test.ext.junit.runners.AndroidJUnit4
import com.github.dedis.popstellar.model.network.JsonTestUtils.loadFile
import com.github.dedis.popstellar.model.network.JsonTestUtils.parse
import com.github.dedis.popstellar.model.network.JsonTestUtils.testData
import com.github.dedis.popstellar.model.network.method.message.data.Action
import com.github.dedis.popstellar.model.network.method.message.data.Objects
import com.github.dedis.popstellar.model.objects.Lao.Companion.generateLaoId
import com.github.dedis.popstellar.testutils.Base64DataUtils
import com.github.dedis.popstellar.utility.security.HashSHA256.hash
import com.google.gson.JsonParseException
import java.time.Instant
import org.hamcrest.CoreMatchers
import org.hamcrest.MatcherAssert
import org.junit.Assert
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.internal.util.collections.Sets

@RunWith(AndroidJUnit4::class)
class UpdateLaoTest {
  private val name = "New name"
  private val organizer = Base64DataUtils.generatePublicKey()
  private val lastModified = Instant.now().epochSecond
  private val creation = lastModified - 10
  private val witnesses =
    Sets.newSet(Base64DataUtils.generatePublicKey(), Base64DataUtils.generatePublicKey())
  private val updateLao = UpdateLao(organizer, creation, name, lastModified, witnesses)

  @Test
  fun generateUpdateLaoIdTest() {
    val updateLao = UpdateLao(organizer, creation, name, lastModified, HashSet())
    // Hash(organizer||creation||name)
    val expectedId = hash(organizer.encoded, creation.toString(), updateLao.name)
    MatcherAssert.assertThat(updateLao.id, CoreMatchers.`is`(expectedId))
  }

  @Test(expected = IllegalArgumentException::class)
  fun constructorFailsEmptyNameTest() {
    UpdateLao(organizer, creation, "", lastModified, HashSet())
  }

  @Test(expected = IllegalArgumentException::class)
  fun constructorFailsFutureCreationTimeTest() {
    val futureCreation = Instant.now().epochSecond + 1000
    UpdateLao(organizer, futureCreation, name, lastModified, HashSet())
  }

  @Test(expected = IllegalArgumentException::class)
  fun constructorFailsFutureModificationTimeTest() {
    val futureModification = Instant.now().epochSecond + 1000
    UpdateLao(organizer, creation, name, futureModification, HashSet())
  }

  @Test(expected = IllegalArgumentException::class)
  fun constructorFailsModificationBeforeCreationTimeTest() {
    UpdateLao(organizer, creation, name, creation - 10, HashSet())
  }

  @Test
  fun objectTest() {
    MatcherAssert.assertThat(updateLao.`object`, CoreMatchers.`is`(Objects.LAO.`object`))
  }

  @Test
  fun actionTest() {
    MatcherAssert.assertThat(updateLao.action, CoreMatchers.`is`(Action.UPDATE.action))
  }

  @Test
  fun nameTest() {
    MatcherAssert.assertThat(updateLao.name, CoreMatchers.`is`(name))
  }

  @Test
  fun lastModifiedTest() {
    MatcherAssert.assertThat(updateLao.lastModified, CoreMatchers.`is`(lastModified))
  }

  @Test
  fun idTest() {
    MatcherAssert.assertThat(
      updateLao.id,
      CoreMatchers.`is`(generateLaoId(organizer, creation, name))
    )
  }

  @Test
  fun witnessesTest() {
    MatcherAssert.assertThat(updateLao.witnesses, CoreMatchers.`is`(witnesses))
  }

  @Test
  fun isEqual() {
    Assert.assertEquals(updateLao, UpdateLao(organizer, creation, name, lastModified, witnesses))
    // different creation time so the id won't be the same
    Assert.assertNotEquals(
      updateLao,
      UpdateLao(organizer, creation - 20, name, lastModified, witnesses)
    )
    // different organizer so the id won't be the same
    Assert.assertNotEquals(
      updateLao,
      UpdateLao(
        Base64DataUtils.generatePublicKeyOtherThan(organizer),
        creation,
        name,
        lastModified,
        witnesses
      )
    )
    // different name
    Assert.assertNotEquals(
      updateLao,
      UpdateLao(organizer, creation, "random", lastModified, witnesses)
    )
    // different witnesses
    Assert.assertNotEquals(
      updateLao,
      UpdateLao(
        organizer,
        creation,
        name,
        lastModified,
        Sets.newSet(Base64DataUtils.generatePublicKey())
      )
    )
  }

  @Test
  fun jsonValidationTest() {
    testData(updateLao)
    val pathDir = "protocol/examples/messageData/lao_update/"
    val jsonInvalid1 = loadFile(pathDir + "wrong_lao_update_additional_params.json")
    val jsonInvalid2 = loadFile(pathDir + "wrong_lao_update_missing_params.json")
    val jsonInvalid3 = loadFile(pathDir + "bad_lao_update_negative_last_modified.json")
    val jsonInvalid4 = loadFile(pathDir + "bad_lao_update_witness_not_base64.json")

    Assert.assertThrows(JsonParseException::class.java) { parse(jsonInvalid1) }
    Assert.assertThrows(JsonParseException::class.java) { parse(jsonInvalid2) }
    Assert.assertThrows(JsonParseException::class.java) { parse(jsonInvalid3) }
    Assert.assertThrows(JsonParseException::class.java) { parse(jsonInvalid4) }
  }
}

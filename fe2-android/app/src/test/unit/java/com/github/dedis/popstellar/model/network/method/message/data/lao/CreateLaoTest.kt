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
import java.util.concurrent.TimeUnit
import org.hamcrest.CoreMatchers
import org.hamcrest.MatcherAssert
import org.junit.Assert
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class CreateLaoTest {
  private val name = " Lao name"
  private val creation = Instant.now().epochSecond
  private val organizer = Base64DataUtils.generatePublicKey()
  private val witnesses =
    listOf(Base64DataUtils.generatePublicKey(), Base64DataUtils.generatePublicKey())
  private val id = generateLaoId(organizer, creation, name)
  private val createLao = CreateLao(id, name, creation, organizer, witnesses)

  @Test(expected = IllegalArgumentException::class)
  fun constructorFailsIdNotBase64Test() {
    CreateLao("wrong Id", name, creation, organizer, witnesses)
  }

  @Test(expected = IllegalArgumentException::class)
  fun constructorFailsInvalidIdHashTest() {
    val wrongId = "?" + id.substring(1)
    CreateLao(wrongId, name, creation, organizer, witnesses)
  }

  @Test(expected = IllegalArgumentException::class)
  fun constructorFailsEmptyNameTest() {
    CreateLao(id, "", creation, organizer, witnesses)
  }

  @Test(expected = IllegalArgumentException::class)
  fun constructorFailsFutureCreationTimeTest() {
    val futureCreation = Instant.now().epochSecond + 1000
    CreateLao(id, name, futureCreation, organizer, witnesses)
  }

  @Test
  fun generateCreateLaoIdTest() {
    val createLao = CreateLao(name, organizer, witnesses)
    // Hash(organizer||creation||name)
    val expectedId =
      hash(createLao.organizer.encoded, createLao.creation.toString(), createLao.name)
    MatcherAssert.assertThat(createLao.id, CoreMatchers.`is`(expectedId))
  }

  @Test
  fun objectTest() {
    MatcherAssert.assertThat(createLao.`object`, CoreMatchers.`is`(Objects.LAO.`object`))
  }

  @Test
  fun actionTest() {
    MatcherAssert.assertThat(createLao.action, CoreMatchers.`is`(Action.CREATE.action))
  }

  @Test
  fun idTest() {
    MatcherAssert.assertThat(createLao.id, CoreMatchers.`is`(id))
  }

  @Test
  fun nameTest() {
    MatcherAssert.assertThat(createLao.name, CoreMatchers.`is`(name))
  }

  @Test
  fun organizerTest() {
    MatcherAssert.assertThat(createLao.organizer, CoreMatchers.`is`(organizer))
  }

  @Test
  fun isEqual() {
    val createLao1 = CreateLao(name, organizer, witnesses)
    try {
      TimeUnit.SECONDS.sleep(1)
      val createLao2 = CreateLao(name, organizer, witnesses)

      // they don't have the same creation time
      Assert.assertNotEquals(createLao1, createLao2)
    } catch (e: InterruptedException) {
      e.printStackTrace()
    }

    Assert.assertEquals(createLao, CreateLao(id, name, creation, organizer, witnesses))
    Assert.assertEquals(
      CreateLao(name, organizer, witnesses),
      CreateLao(name, organizer, witnesses)
    )
    Assert.assertNotEquals(createLao1, CreateLao("random", organizer, witnesses))
    Assert.assertNotEquals(
      createLao1,
      CreateLao(name, Base64DataUtils.generatePublicKeyOtherThan(organizer), witnesses)
    )
  }

  @Test
  fun jsonValidationTest() {
    testData(createLao)
    val pathDir = "protocol/examples/messageData/lao_create/"
    val jsonInvalid1 = loadFile(pathDir + "wrong_lao_create_additional_params.json")
    val jsonInvalid2 = loadFile(pathDir + "wrong_lao_create_missing_params.json")
    val jsonInvalid3 = loadFile(pathDir + "bad_lao_create_creation_negative.json")
    val jsonInvalid4 = loadFile(pathDir + "bad_lao_create_organizer_not_base64.json")
    val jsonInvalid5 = loadFile(pathDir + "bad_lao_create_witness_not_base64.json")

    Assert.assertThrows(JsonParseException::class.java) { parse(jsonInvalid1) }
    Assert.assertThrows(JsonParseException::class.java) { parse(jsonInvalid2) }
    Assert.assertThrows(JsonParseException::class.java) { parse(jsonInvalid3) }
    Assert.assertThrows(JsonParseException::class.java) { parse(jsonInvalid4) }
    Assert.assertThrows(JsonParseException::class.java) { parse(jsonInvalid5) }
  }
}

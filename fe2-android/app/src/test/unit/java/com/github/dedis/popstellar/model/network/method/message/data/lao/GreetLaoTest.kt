package com.github.dedis.popstellar.model.network.method.message.data.lao

import androidx.test.ext.junit.runners.AndroidJUnit4
import com.github.dedis.popstellar.model.network.JsonTestUtils.loadFile
import com.github.dedis.popstellar.model.network.JsonTestUtils.parse
import com.github.dedis.popstellar.model.network.JsonTestUtils.testData
import com.github.dedis.popstellar.model.network.method.message.data.Action
import com.github.dedis.popstellar.model.network.method.message.data.Objects
import com.github.dedis.popstellar.model.objects.PeerAddress
import com.github.dedis.popstellar.model.objects.security.PublicKey
import com.google.gson.JsonParseException
import org.junit.Assert
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class GreetLaoTest {
  @Test(expected = IllegalArgumentException::class)
  fun constructorFailsWrongPublicKeyTest() {
    GreetLao(LAO_ID, "IsNotValid", RANDOM_ADDRESS, emptyList())
  }

  @Test(expected = IllegalArgumentException::class)
  fun constructorFailsLaoIdNotBase64Test() {
    GreetLao("wrong id", RANDOM_KEY, RANDOM_ADDRESS, emptyList())
  }

  @Test
  fun actionTest() {
    Assert.assertEquals(Action.GREET.action, GREETING_MSG.action)
  }

  @Test
  fun objectTest() {
    Assert.assertEquals(Objects.LAO.`object`, GREETING_MSG.`object`)
  }

  @Test
  fun frontendKeyTest() {
    Assert.assertEquals(PublicKey(RANDOM_KEY), GREETING_MSG.frontendKey)
  }

  @Test
  fun addressTest() {
    Assert.assertEquals(RANDOM_ADDRESS, GREETING_MSG.address)
  }

  @Test
  fun peersTest() {
    val peersList: List<PeerAddress> = ArrayList(RANDOM_PEER_LIST)
    Assert.assertEquals(GREETING_MSG.peers, peersList)
  }

  @Test
  fun equalsTest() {
    val GREETING_MSG_2 = GreetLao(LAO_ID, RANDOM_KEY, RANDOM_ADDRESS, RANDOM_PEER_LIST)
    val RANDOM_PEER_2 = PeerAddress("123")
    Assert.assertNotEquals(
      GREETING_MSG,
      GreetLao("some_id2", RANDOM_KEY, RANDOM_ADDRESS, RANDOM_PEER_LIST)
    )
    Assert.assertEquals(GREETING_MSG, GREETING_MSG_2)
    Assert.assertNotEquals(GREETING_MSG, null)
    Assert.assertEquals(GREETING_MSG_2.hashCode().toLong(), GREETING_MSG.hashCode().toLong())
    Assert.assertEquals(GREETING_MSG, GREETING_MSG)
    Assert.assertNotEquals(GREETING_MSG, GreetLao(LAO_ID, RANDOM_KEY, "123", RANDOM_PEER_LIST))
    Assert.assertNotEquals(
      GREETING_MSG,
      GreetLao(LAO_ID, RANDOM_KEY, RANDOM_ADDRESS, listOf(RANDOM_PEER_2))
    )
    Assert.assertNotEquals(
      GREETING_MSG,
      GreetLao(
        LAO_ID,
        "TrWJNl4kA9VUBydvUwfWw9A-EJlLL6xLaQqRdynvhYw",
        RANDOM_ADDRESS,
        listOf(RANDOM_PEER_2)
      )
    )
  }

  @Test
  fun toStringTest() {
    val listTest: List<PeerAddress> = ArrayList(RANDOM_PEER_LIST)
    val greetingToString =
      String.format(
        "GreetLao={lao='%s', " + "frontend='%s', " + "address='%s', " + "peers=%s}",
        LAO_ID,
        PublicKey(RANDOM_KEY),
        RANDOM_ADDRESS,
        listTest.toTypedArray().contentToString()
      )
    Assert.assertEquals(greetingToString, GREETING_MSG.toString())
  }

  @Test
  fun jsonValidationTest() {
    testData(GREETING_MSG)
    val pathDir = "protocol/examples/messageData/lao_greet/"
    val jsonInvalid1 = loadFile(pathDir + "wrong_greeting_additional_property_0.json")
    val jsonInvalid2 = loadFile(pathDir + "wrong_greeting_additional_property_2.json")
    val jsonInvalid3 = loadFile(pathDir + "wrong_greeting_invalid_address_2.json")
    val jsonInvalid4 = loadFile(pathDir + "wrong_greeting_invalid_address.json")
    val jsonInvalid5 = loadFile(pathDir + "wrong_greeting_missing_action.json")
    val jsonInvalid6 = loadFile(pathDir + "wrong_greeting_missing_address_0.json")
    val jsonInvalid7 = loadFile(pathDir + "wrong_greeting_missing_address_1.json")
    val jsonInvalid8 = loadFile(pathDir + "wrong_greeting_missing_frontend.json")
    val jsonInvalid9 = loadFile(pathDir + "wrong_greeting_missing_lao.json")
    val jsonInvalid10 = loadFile(pathDir + "wrong_greeting_missing_object.json")
    val jsonInvalid11 = loadFile(pathDir + "wrong_greeting_missing_peers.json")

    Assert.assertThrows(JsonParseException::class.java) { parse(jsonInvalid1) }
    Assert.assertThrows(JsonParseException::class.java) { parse(jsonInvalid2) }
    Assert.assertThrows(JsonParseException::class.java) { parse(jsonInvalid3) }
    Assert.assertThrows(JsonParseException::class.java) { parse(jsonInvalid4) }
    Assert.assertThrows(JsonParseException::class.java) { parse(jsonInvalid5) }
    Assert.assertThrows(JsonParseException::class.java) { parse(jsonInvalid6) }
    Assert.assertThrows(JsonParseException::class.java) { parse(jsonInvalid7) }
    Assert.assertThrows(JsonParseException::class.java) { parse(jsonInvalid8) }
    Assert.assertThrows(JsonParseException::class.java) { parse(jsonInvalid9) }
    Assert.assertThrows(JsonParseException::class.java) { parse(jsonInvalid10) }
    Assert.assertThrows(JsonParseException::class.java) { parse(jsonInvalid11) }
  }

  companion object {
    const val LAO_ID = "p_EYbHyMv6sopI5QhEXBf40MO_eNoq7V_LygBd4c9RA="
    const val RANDOM_KEY = "oOcKZjUeandJOFVgn-E6e-7QksviBBbHUPicdzUgIm8"
    const val RANDOM_ADDRESS = "ws://10.0.2.2:9000/organizer/client"
    val RANDOM_PEER = PeerAddress("ws://10.0.1.1:7000/")
    var RANDOM_PEER_LIST: List<PeerAddress> = listOf(RANDOM_PEER)
    var GREETING_MSG = GreetLao(LAO_ID, RANDOM_KEY, RANDOM_ADDRESS, RANDOM_PEER_LIST)
  }
}

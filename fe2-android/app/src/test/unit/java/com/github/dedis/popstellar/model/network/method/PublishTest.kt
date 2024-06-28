package com.github.dedis.popstellar.model.network.method

import com.github.dedis.popstellar.model.network.method.message.MessageGeneral
import com.github.dedis.popstellar.model.network.method.message.data.rollcall.CreateRollCall
import com.github.dedis.popstellar.model.objects.Channel.Companion.fromString
import com.github.dedis.popstellar.testutils.Base64DataUtils
import com.google.gson.Gson
import java.util.Objects
import org.junit.Assert
import org.junit.Test
import java.time.Instant

class PublishTest {
  @Test
  fun method() {
    Assert.assertEquals("publish", PUBLISH.method)
  }

  @Test
  fun testEquals() {
    Assert.assertEquals(PUBLISH, PUBLISH)
    Assert.assertNotEquals(null, PUBLISH)

    val publish2 = Publish(CHANNEL, 1, MESSAGE_GENERAL)
    Assert.assertNotEquals(PUBLISH, publish2)

    val publish3 = Publish(CHANNEL, ID, MESSAGE_GENERAL)
    Assert.assertEquals(PUBLISH, publish3)
  }

  @Test
  fun testHashCode() {
    Assert.assertEquals(
      Objects.hash(Objects.hash(Objects.hash(CHANNEL), ID), MESSAGE_GENERAL).toLong(),
      PUBLISH.hashCode().toLong()
    )
  }

  @Test
  fun testToString() {
    val expected =
      String.format("Publish{id=%s, channel='%s'," + " message=%s}", ID, CHANNEL, MESSAGE_GENERAL)
    Assert.assertEquals(expected, PUBLISH.toString())
  }

  @Test
  fun emptyMsgGeneralInConstructorThrowsException() {
    Assert.assertThrows(IllegalArgumentException::class.java) { Publish(CHANNEL, ID, null) }
  }

  companion object {
    private val CHANNEL = fromString("root/stuff")
    private const val ID = 42
    private const val LAO_ID = "fEvAfdtNrykd9NPYl9ReHLX-6IP6SFLKTZJLeGUHZ_U="
    private const val NAME = "NAME"
    private val NOW = Instant.now().epochSecond
    private val END = NOW + 30L
    private const val LOCATION = "Location"
    private val DATA = CreateRollCall(
      NAME,
      NOW,
      NOW,
      END,
      LOCATION, null,
      LAO_ID
    )
    private val KEYPAIR = Base64DataUtils.generateKeyPair()
    private val MESSAGE_GENERAL = MessageGeneral(KEYPAIR, DATA, Gson())
    private val PUBLISH = Publish(CHANNEL, ID, MESSAGE_GENERAL)
  }
}

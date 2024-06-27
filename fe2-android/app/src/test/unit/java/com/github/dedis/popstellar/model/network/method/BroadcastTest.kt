package com.github.dedis.popstellar.model.network.method

import com.github.dedis.popstellar.model.network.method.message.MessageGeneral
import com.github.dedis.popstellar.model.network.method.message.data.rollcall.CreateRollCall
import com.github.dedis.popstellar.model.objects.Channel.Companion.fromString
import com.github.dedis.popstellar.testutils.Base64DataUtils
import com.google.gson.Gson
import org.junit.Assert
import org.junit.Test
import java.time.Instant
import java.util.Objects

class BroadcastTest {
  @Test
  fun method() {
    Assert.assertEquals("broadcast", BROADCAST.method)
  }

  @Test
  fun message() {
    Assert.assertEquals(MESSAGE_GENERAL, BROADCAST.message)
  }

  @Test
  fun testEquals() {
    Assert.assertEquals(BROADCAST, BROADCAST)
    Assert.assertNotEquals(null, BROADCAST)

    val broadcast2 = Broadcast(fromString("foo/bar/refoo/rebar"), MESSAGE_GENERAL)
    Assert.assertNotEquals(broadcast2, BROADCAST)

    val broadcast3 = Broadcast(CHANNEL, MESSAGE_GENERAL)
    Assert.assertEquals(BROADCAST, broadcast3)
  }

  @Test
  fun testHashCode() {
    Assert.assertEquals(
      Objects.hash(Objects.hash(CHANNEL), MESSAGE_GENERAL).toLong(),
      BROADCAST.hashCode().toLong()
    )
  }

  @Test
  fun testToString() {
    val expected =
      String.format(
        "Broadcast{channel='%s', method='%s', message=%s}",
        CHANNEL,
        "broadcast",
        MESSAGE_GENERAL
      )
    Assert.assertEquals(expected, BROADCAST.toString())
  }

  @Test
  fun constructorThrowsExceptionForEmptyMessage() {
    Assert.assertThrows(IllegalArgumentException::class.java) { Broadcast(CHANNEL, null) }
  }

  companion object {
    private val CHANNEL = fromString("root/stuff")
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
    private val BROADCAST = Broadcast(CHANNEL, MESSAGE_GENERAL)
  }
}

package com.github.dedis.popstellar.model.network.method

import com.github.dedis.popstellar.model.objects.Channel.Companion.fromString
import java.util.Objects
import org.junit.Assert
import org.junit.Test

class MessageTest {
  @Test
  fun testEquals() {
    Assert.assertEquals(MESSAGE, MESSAGE)
    Assert.assertNotEquals(null, MESSAGE)

    val message2: Message =
      object : Message(fromString("some channel")) {
        override val method: String
          get() = "bar"
      }
    Assert.assertNotEquals(MESSAGE, message2)
  }

  @Test
  fun testHashCode() {
    Assert.assertEquals(Objects.hash(CHANNEL).toLong(), MESSAGE.hashCode().toLong())
  }

  @Test
  fun constructorThrowsExceptionForNullChannel() {
    Assert.assertThrows(IllegalArgumentException::class.java) {
      object : Message(null) {
        override val method: String
          get() = "bar"
      }
    }
  }

  companion object {
    private val CHANNEL = fromString("root/foo")
    private val MESSAGE: Message =
      object : Message(CHANNEL) {
        override val method: String
          get() = "bar"
      }
  }
}

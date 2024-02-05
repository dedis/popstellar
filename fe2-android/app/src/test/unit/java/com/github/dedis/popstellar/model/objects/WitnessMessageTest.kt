package com.github.dedis.popstellar.model.objects

import com.github.dedis.popstellar.model.objects.security.MessageID
import com.github.dedis.popstellar.testutils.Base64DataUtils
import org.junit.Assert
import org.junit.Test

class WitnessMessageTest {
  @Test
  fun addWitnessTest() {
    val witnessMessage = WitnessMessage(MSG_ID)
    witnessMessage.addWitness(PK)
    Assert.assertTrue(witnessMessage.witnesses.contains(PK))
  }

  @Test
  fun toStringTest() {
    val witnessMessage = WitnessMessage(MSG_ID)
    witnessMessage.addWitness(PK)
    witnessMessage.description = DESCRIPTION
    witnessMessage.title = TITLE
    val expected =
      String.format(
        "WitnessMessage{messageId='%s', witnesses=%s, title='%s', description='%s'}",
        MSG_ID,
        listOf(PK),
        TITLE,
        DESCRIPTION
      )
    Assert.assertEquals(expected, witnessMessage.toString())
  }

  @Test
  fun equalsTest() {
    Assert.assertEquals(MESSAGE1, MESSAGE2)
    Assert.assertNotEquals(MESSAGE1, MESSAGE3)
    val message1 = WitnessMessage(MESSAGE1)
    message1.addWitness(PK)
    message1.description = DESCRIPTION
    message1.title = TITLE
    val message2 = WitnessMessage(message1)

    Assert.assertEquals(message1, message2)
    message2.addWitness(Base64DataUtils.generatePublicKey())

    Assert.assertNotEquals(message1, message2)
    Assert.assertNotEquals(message1, null)
  }

  @Test
  fun hashCodeTest() {
    Assert.assertEquals(MESSAGE1.hashCode().toLong(), MESSAGE2.hashCode().toLong())
    Assert.assertNotEquals(MESSAGE2.hashCode().toLong(), MESSAGE3.hashCode().toLong())
  }

  companion object {
    private val PK = Base64DataUtils.generatePublicKey()
    private val MSG_ID = MessageID("foo")
    private const val DESCRIPTION = "description"
    private const val TITLE = "title"
    private val MESSAGE_ID1 = Base64DataUtils.generateMessageID()
    private val MESSAGE_ID3 = Base64DataUtils.generateMessageID()
    private val MESSAGE1 = WitnessMessage(MESSAGE_ID1)
    private val MESSAGE2 = MESSAGE1.copy()
    private val MESSAGE3 = WitnessMessage(MESSAGE_ID3)
  }
}

package com.github.dedis.popstellar.model.objects

import com.github.dedis.popstellar.model.objects.Channel.Companion.fromString
import com.github.dedis.popstellar.model.objects.digitalcash.TransactionObjectBuilder
import org.junit.Assert
import org.junit.Test

class TransactionObjectBuilderTest {
  @Test
  fun buildWithoutChannelThrowsException() {
    Assert.assertThrows(IllegalStateException::class.java) { builder.build() }
  }

  @Test
  fun buildWithoutInputsThrowsException() {
    builder.setChannel(fromString("/root/stuff"))
    Assert.assertThrows(IllegalStateException::class.java) { builder.build() }
  }

  @Test
  fun buildWithoutOutputsThrowsException() {
    builder.setChannel(fromString("/root/stuff"))
    builder.setInputs(ArrayList())
    Assert.assertThrows(IllegalStateException::class.java) { builder.build() }
  }

  @Test
  fun buildWithoutTransactionIdThrowsException() {
    builder.setChannel(fromString("/root/stuff"))
    builder.setInputs(ArrayList())
    builder.setOutputs(ArrayList())
    Assert.assertThrows(IllegalStateException::class.java) { builder.build() }
  }

  companion object {
    private val builder = TransactionObjectBuilder()
  }
}
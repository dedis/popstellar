package com.github.dedis.popstellar.model.network.method.message.data.consensus

import com.github.dedis.popstellar.utility.security.HashSHA256.hash
import org.junit.Assert
import org.junit.Test

class ConsensusKeyTest {

  private val type = "TestType"
  private val id = hash("TestId")
  private val property = "TestProperty"
  private val key = ConsensusKey(type, id, property)

  @Test
  fun typeTest() {
    Assert.assertEquals(type, key.type)
  }

  @Test
  fun idTest() {
    Assert.assertEquals(id, key.id)
  }

  @Test
  fun propertyTest() {
    Assert.assertEquals(property, key.property)
  }

  @Test
  fun equalsTest() {
    Assert.assertEquals(key, ConsensusKey(type, id, property))

    val random = "random"

    Assert.assertNotEquals(key, ConsensusKey(random, id, property))
    Assert.assertNotEquals(key, ConsensusKey(type, random, property))
    Assert.assertNotEquals(key, ConsensusKey(type, id, random))
  }
}

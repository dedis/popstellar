package com.github.dedis.popstellar.model.network.method.message.data.consensus

import androidx.test.ext.junit.runners.AndroidJUnit4
import com.github.dedis.popstellar.model.network.JsonTestUtils
import com.github.dedis.popstellar.model.network.method.message.data.Action
import com.github.dedis.popstellar.model.network.method.message.data.Objects
import com.github.dedis.popstellar.utility.security.HashSHA256.hash
import com.google.gson.JsonParseException
import org.junit.Assert
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class ConsensusElectTest {

  private val timeInSeconds: Long = 1635277619
  private val objId = hash("test")
  private val type = "TestType"
  private val property = "TestProperty"
  private val value: Any = "TestValue"
  private val key = ConsensusKey(type, objId, property)
  private val consensusElect = ConsensusElect(timeInSeconds, objId, type, property, value)

  @Test
  fun instanceIdTest() {
    // Hash("consensus"||key:type||key:id||key:property)
    val expectedId = hash("consensus", type, objId, property)
    Assert.assertEquals(expectedId, consensusElect.instanceId)
  }

  @Test
  fun creationTest() {
    Assert.assertEquals(timeInSeconds, consensusElect.creation)
  }

  @Test
  fun objectTest() {
    Assert.assertEquals(Objects.CONSENSUS.getObject(), consensusElect.getObject())
  }

  @Test
  fun actionTest() {
    Assert.assertEquals(Action.ELECT.action, consensusElect.action)
  }

  @Test
  fun keyTest() {
    Assert.assertEquals(key, consensusElect.key)
  }

  @Test
  fun valueTest() {
    Assert.assertEquals(value, consensusElect.value)
  }

  @Test
  fun equalsTest() {
    Assert.assertEquals(consensusElect, ConsensusElect(timeInSeconds, objId, type, property, value))

    val random = "random"

    Assert.assertNotEquals(
      consensusElect,
      ConsensusElect(timeInSeconds + 1, objId, type, property, value)
    )
    Assert.assertNotEquals(
      consensusElect,
      ConsensusElect(timeInSeconds, random, type, property, value)
    )
    Assert.assertNotEquals(
      consensusElect,
      ConsensusElect(timeInSeconds, objId, random, property, value)
    )
    Assert.assertNotEquals(
      consensusElect,
      ConsensusElect(timeInSeconds, objId, type, random, value)
    )
    Assert.assertNotEquals(
      consensusElect,
      ConsensusElect(timeInSeconds, objId, type, property, random)
    )
  }

  @Test
  fun jsonValidationTest() {
    JsonTestUtils.testData(consensusElect)
    val jsonInvalid =
      JsonTestUtils.loadFile(
        "protocol/examples/messageData/consensus_elect/wrong_elect_negative_created_at.json"
      )

    Assert.assertThrows(JsonParseException::class.java) { JsonTestUtils.parse(jsonInvalid) }
  }
}

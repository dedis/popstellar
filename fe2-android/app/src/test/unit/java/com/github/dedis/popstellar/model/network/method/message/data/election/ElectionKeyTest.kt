package com.github.dedis.popstellar.model.network.method.message.data.election

import androidx.test.ext.junit.runners.AndroidJUnit4
import com.github.dedis.popstellar.model.network.JsonTestUtils.loadFile
import com.github.dedis.popstellar.model.network.JsonTestUtils.parse
import com.github.dedis.popstellar.model.network.JsonTestUtils.testData
import com.github.dedis.popstellar.model.network.method.message.data.Action
import com.github.dedis.popstellar.model.network.method.message.data.Objects
import com.github.dedis.popstellar.testutils.Base64DataUtils
import com.google.gson.JsonParseException
import org.junit.Assert
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class ElectionKeyTest {
  @Test(expected = IllegalArgumentException::class)
  fun constructorFailsElectionIdNotBase64Test() {
    ElectionKey("not base 64", KEY1)
  }

  @Test(expected = IllegalArgumentException::class)
  fun constructorFailsElectionVoteKeyNotBase64Test() {
    ElectionKey(ELEC_ID1, "not base 64")
  }

  @Test
  fun electionIdTest() {
    Assert.assertEquals(ELEC_ID1, ELECTION_KEY1.electionId)
  }

  @Test
  fun electionVoteKeyTest() {
    Assert.assertEquals(KEY1, ELECTION_KEY1.electionVoteKey)
  }

  @Test
  fun actionTest() {
    Assert.assertEquals(Action.KEY.action, ELECTION_KEY1.action)
  }

  @Test
  fun objectTest() {
    Assert.assertEquals(Objects.ELECTION.`object`, ELECTION_KEY1.`object`)
  }

  @Test
  fun equalsTest() {
    Assert.assertNotEquals(ELECTION_KEY1, ELECTION_KEY2)
    val testElect1 = ElectionKey(ELEC_ID1, KEY1)
    Assert.assertEquals(testElect1, ELECTION_KEY1)
    Assert.assertNotEquals(ELECTION_KEY1, null)
    Assert.assertEquals(testElect1.hashCode().toLong(), ELECTION_KEY1.hashCode().toLong())
  }

  @Test
  fun testToString() {
    val testFormat = "ElectionKey{election='$ELEC_ID1', election_key='$KEY1'}"
    Assert.assertEquals(testFormat, ELECTION_KEY1.toString())
  }

  @Test
  fun jsonValidationTest() {
    testData(ELECTION_KEY1)
    val pathDir = "protocol/examples/messageData/election_key/"
    val valid1 = loadFile(pathDir + "election_key.json")
    parse(valid1)

    // Check that invalid data is rejected
    val jsonInvalid1 = loadFile(pathDir + "wrong_election_key_additional_property.json")
    val jsonInvalid2 = loadFile(pathDir + "wrong_election_key_missing_action.json")
    val jsonInvalid3 = loadFile(pathDir + "wrong_election_key_missing_election.json")
    val jsonInvalid4 = loadFile(pathDir + "wrong_election_key_missing_election_key.json")
    val jsonInvalid5 = loadFile(pathDir + "wrong_election_key_missing_object.json")
    Assert.assertThrows(JsonParseException::class.java) { parse(jsonInvalid1) }
    Assert.assertThrows(JsonParseException::class.java) { parse(jsonInvalid2) }
    Assert.assertThrows(JsonParseException::class.java) { parse(jsonInvalid3) }
    Assert.assertThrows(JsonParseException::class.java) { parse(jsonInvalid4) }
    Assert.assertThrows(JsonParseException::class.java) { parse(jsonInvalid5) }
  }

  companion object {
    var ELEC_ID1 = Base64DataUtils.generateRandomBase64String()
    var ELEC_ID2 = Base64DataUtils.generateRandomBase64String()
    var KEY1 = Base64DataUtils.generateRandomBase64String()
    var KEY2 = Base64DataUtils.generateRandomBase64String()
    var ELECTION_KEY1 = ElectionKey(ELEC_ID1, KEY1)
    var ELECTION_KEY2 = ElectionKey(ELEC_ID2, KEY2)
  }
}

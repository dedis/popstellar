package ch.epfl.pop.model.network.method.message.data.socialMedia

import ch.epfl.pop.json.MessageDataProtocol._
import ch.epfl.pop.model.network.method.message.data.socialMedia.AddReactionExample._
import ch.epfl.pop.model.network.method.message.data.{ActionType, ObjectType}
import ch.epfl.pop.model.objects.{Base64Data, Hash, Timestamp}
import org.scalatest.funsuite.{AnyFunSuite => FunSuite}
import org.scalatest.matchers.should.Matchers
import spray.json._

class AddReactionSuite extends FunSuite with Matchers {
  test("Constructor/apply works as intended") {
    ADDREACTION_MESSAGE.reaction_codepoint should equal(REACTION_CODEPOINT)
    ADDREACTION_MESSAGE.chirp_id should equal(CHIRP_ID)
    ADDREACTION_MESSAGE.timestamp should equal(TIMESTAMP)
    ADDREACTION_MESSAGE._object should equal(ObjectType.REACTION)
    ADDREACTION_MESSAGE.action should equal(ActionType.ADD)
  }

  test("json conversions work back and forth") {
    val msg2: AddReaction = AddReaction.buildFromJson(ADDREACTION_MESSAGE.toJson.toString)
    msg2 should equal(ADDREACTION_MESSAGE)
  }
}

object AddReactionExample {
  val REACTION_CODEPOINT: String = "👍"
  val CHIRP_ID: Hash = Hash(Base64Data.encode("chirpid"))
  val TIMESTAMP: Timestamp = Timestamp(0)

  val ADDREACTION_MESSAGE: AddReaction = AddReaction(REACTION_CODEPOINT, CHIRP_ID, TIMESTAMP)
}

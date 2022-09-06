package ch.epfl.pop.model.network.method.message.data.socialMedia

import ch.epfl.pop.json.MessageDataProtocol._
import ch.epfl.pop.model.network.method.message.data.socialMedia.DeleteReactionExample._
import ch.epfl.pop.model.network.method.message.data.{ActionType, ObjectType}
import ch.epfl.pop.model.objects.{Base64Data, Hash, Timestamp}
import org.scalatest.funsuite.{AnyFunSuite => FunSuite}
import org.scalatest.matchers.should.Matchers
import spray.json._

class DeleteReactionSuite extends FunSuite with Matchers {

  test("Constructor/apply works as intended") {
    DELETEREACTION_MESSAGE.reaction_id should equal(REACTION_ID)
    DELETEREACTION_MESSAGE.timestamp should equal(TIMESTAMP)
    DELETEREACTION_MESSAGE._object should equal(ObjectType.REACTION)
    DELETEREACTION_MESSAGE.action should equal(ActionType.DELETE)
  }

  test("json conversions work back and forth") {
    val msg2: DeleteReaction = DeleteReaction.buildFromJson(DELETEREACTION_MESSAGE.toJson.toString)

    msg2 should equal(DELETEREACTION_MESSAGE)
  }
}

object DeleteReactionExample {
  val REACTION_ID: Hash = Hash(Base64Data.encode("reactionid"))
  val TIMESTAMP: Timestamp = Timestamp(0)

  val DELETEREACTION_MESSAGE: DeleteReaction = DeleteReaction(REACTION_ID, TIMESTAMP)
}

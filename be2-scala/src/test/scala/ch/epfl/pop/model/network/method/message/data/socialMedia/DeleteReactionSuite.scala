package ch.epfl.pop.model.network.method.message.data.socialMedia

import org.scalatest.{FunSuite, Matchers}
import ch.epfl.pop.model.objects.Timestamp
import ch.epfl.pop.json.MessageDataProtocol._
import ch.epfl.pop.model.network.method.message.data.{ActionType, ObjectType}
import spray.json._

class DeleteReactionSuite extends FunSuite with Matchers {

    private final val reaction_id: String = "reactionid"
    private final val timestamp = Timestamp(0)

    private final val msg: DeleteReaction = DeleteReaction(reaction_id, timestamp)

    test("Constructor/apply works as intended"){
        msg.reaction_id should equal(reaction_id)
        msg.timestamp should equal(timestamp)
        msg._object should equal(ObjectType.REACTION)
        msg.action should equal(ActionType.DELETE)
    }

    test("json conversions work back and forth"){
        val msg2: DeleteReaction = DeleteReaction.buildFromJson(msg.toJson.toString)

        msg2 should equal(msg)
    }
}
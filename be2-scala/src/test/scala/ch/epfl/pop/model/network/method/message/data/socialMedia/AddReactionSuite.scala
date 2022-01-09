package ch.epfl.pop.model.network.method.message.data.socialMedia

import org.scalatest.{FunSuite, Matchers}
import ch.epfl.pop.model.objects.Timestamp
import ch.epfl.pop.json.MessageDataProtocol._
import ch.epfl.pop.model.network.method.message.data.{ActionType, ObjectType}
import spray.json._

class AddReactionSuite extends FunSuite with Matchers {
    test("Constructor/apply works as intended"){
        val reaction_codepoint: String = "👍"
        val chirp_id: String = "chirpid"
        val timestamp = Timestamp(0)

        val msg: AddReaction = AddReaction(reaction_codepoint, chirp_id, timestamp)

        msg.reaction_codepoint should equal(reaction_codepoint)
        msg.chirp_id should equal(chirp_id)
        msg.timestamp should equal(timestamp)
        msg._object should equal(ObjectType.REACTION)
        msg.action should equal(ActionType.ADD)
    }

    test("json conversions work back and forth"){
        val reaction_codepoint: String = "👍"
        val chirp_id: String = "chirpid"
        val timestamp = Timestamp(0)

        val msg: AddReaction = AddReaction(reaction_codepoint, chirp_id, timestamp)

        val msg2: AddReaction = AddReaction.buildFromJson(msg.toJson.toString)

        msg2 should equal(msg)
    }
}
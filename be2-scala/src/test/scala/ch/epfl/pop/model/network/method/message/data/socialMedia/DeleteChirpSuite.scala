package ch.epfl.pop.model.network.method.message.data.socialMedia

import org.scalatest.{FunSuite, Matchers}
import ch.epfl.pop.model.objects.{Hash, Base64Data, Timestamp}
import ch.epfl.pop.json.MessageDataProtocol._
import ch.epfl.pop.model.network.method.message.data.{ActionType, ObjectType}
import spray.json._

class DeleteChirpSuite extends FunSuite with Matchers {
    test("Constructor/apply works as intended"){
        val chirp_id: Hash = Hash(Base64Data(""))
        val timestamp = Timestamp(0)

        val msg: DeleteChirp = DeleteChirp(chirp_id, timestamp)

        msg.chirp_id should equal(chirp_id)
        msg.timestamp should equal(timestamp)
        msg._object should equal(ObjectType.CHIRP)
        msg.action should equal(ActionType.DELETE)
    }

    test("json conversions work back and forth"){
        val chirp_id: Hash = Hash(Base64Data(""))
        val timestamp = Timestamp(0)

        val msg: DeleteChirp = DeleteChirp(chirp_id, timestamp)

        val msg2: DeleteChirp = DeleteChirp.buildFromJson(msg.toJson.toString)

        msg2 should equal(msg)
    }
}

package ch.epfl.pop.model.network.method.message.data.socialMedia

import org.scalatest.{FunSuite, Matchers}
import ch.epfl.pop.model.objects.Timestamp
import ch.epfl.pop.json.MessageDataProtocol._
import ch.epfl.pop.model.network.method.message.data.{ActionType, ObjectType}
import spray.json._

class AddChirpSuite extends FunSuite with Matchers {
    test("Constructor/apply works as intended"){
        val text: String = "text"
        val parent_id: Option[String] = None
        val timestamp = Timestamp(0)

        val msg: AddChirp = AddChirp(text, parent_id, timestamp)

        msg.text should equal(text)
        msg.parent_id should equal(parent_id)
        msg.timestamp should equal(timestamp)
        msg._object should equal(ObjectType.CHIRP)
        msg.action should equal(ActionType.ADD)
    }

    test("json conversions work back and forth"){
        val text: String = "text"
        val parent_id: Option[String] = None
        val timestamp = Timestamp(0)

        val msg: AddChirp = AddChirp(text, parent_id, timestamp)

        val msg2: AddChirp = AddChirp.buildFromJson(msg.toJson.toString)

        msg2 should equal(msg)
    }
}

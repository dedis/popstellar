package ch.epfl.pop.model.network.method.message.data.socialMedia

import org.scalatest.{FunSuite, Matchers}
import ch.epfl.pop.model.objects.{Base64Data, Hash, Timestamp}
import ch.epfl.pop.model.network.method.message.data.{ActionType, ObjectType}
import ch.epfl.pop.json.MessageDataProtocol._
import spray.json._

class AddBroadcastChirpSuite extends FunSuite with Matchers {
    private final val id: Hash = Hash(Base64Data(""))
    private final val channel: String = "channel"
    private final val timestamp = Timestamp(0)
    private final val msg: AddBroadcastChirp = AddBroadcastChirp(id, channel, timestamp)

    test("Constructor/apply works as intended"){

        msg.chirp_id should equal(id)
        msg.channel should equal(channel)
        msg.timestamp should equal(timestamp)
        msg._object should equal(ObjectType.CHIRP)
        msg.action should equal(ActionType.ADD_BROADCAST)
    }

    test("json conversions work back and forth"){

        val msg2: AddBroadcastChirp = AddBroadcastChirp.buildFromJson(msg.toJson.toString)

        msg2 should equal(msg)
    }
}
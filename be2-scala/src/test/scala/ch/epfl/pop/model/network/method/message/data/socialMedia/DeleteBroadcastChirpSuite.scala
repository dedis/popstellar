package ch.epfl.pop.model.network.method.message.data.socialMedia

import org.scalatest.{FunSuite, Matchers}
import ch.epfl.pop.model.objects.{Base64Data, Channel, Hash, Timestamp}
import ch.epfl.pop.model.network.method.message.data.{ActionType, ObjectType}
import ch.epfl.pop.json.MessageDataProtocol._
import spray.json._

class DeleteBroadcastChirpSuite extends FunSuite with Matchers {
    private final val id: Hash = Hash(Base64Data.encode(""))
    private final val channel: Channel = Channel("/root/channel")
    private final val timestamp = Timestamp(0)
    private final val msg: DeleteBroadcastChirp = DeleteBroadcastChirp(id, channel, timestamp)

    test("Constructor/apply works as intended"){
        msg.chirp_id should equal(id)
        msg.channel should equal(channel)
        msg.timestamp should equal(timestamp)
        msg._object should equal(ObjectType.CHIRP)
        msg.action should equal(ActionType.DELETE_BROADCAST)
    }

    test("json conversions work back and forth"){
        val msg2: DeleteBroadcastChirp = DeleteBroadcastChirp.buildFromJson(msg.toJson.toString)
        msg2 should equal(msg)
    }
}

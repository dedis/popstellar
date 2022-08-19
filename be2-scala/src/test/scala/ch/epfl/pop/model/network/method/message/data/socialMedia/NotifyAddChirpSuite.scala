package ch.epfl.pop.model.network.method.message.data.socialMedia

import ch.epfl.pop.json.MessageDataProtocol._
import ch.epfl.pop.model.network.method.message.data.{ActionType, ObjectType}
import ch.epfl.pop.model.objects.{Base64Data, Channel, Hash, Timestamp}
import org.scalatest.funsuite.{AnyFunSuite => FunSuite}
import org.scalatest.matchers.should.Matchers
import spray.json._

class NotifyAddChirpSuite extends FunSuite with Matchers {
  private final val id: Hash = Hash(Base64Data(""))
  private final val channel: Channel = Channel("/root/channel")
  private final val timestamp = Timestamp(0)
  private final val msg: NotifyAddChirp = NotifyAddChirp(id, channel, timestamp)

  test("Constructor/apply works as intended") {

    msg.chirp_id should equal(id)
    msg.channel should equal(channel)
    msg.timestamp should equal(timestamp)
    msg._object should equal(ObjectType.CHIRP)
    msg.action should equal(ActionType.NOTIFY_ADD)
  }

  test("json conversions work back and forth") {

    val msg2: NotifyAddChirp = NotifyAddChirp.buildFromJson(msg.toJson.toString)

    msg2 should equal(msg)
  }
}

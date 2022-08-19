package ch.epfl.pop.model.network.method.message.data.socialMedia

import ch.epfl.pop.json.MessageDataProtocol._
import ch.epfl.pop.model.network.method.message.data.socialMedia.DeleteChirpExample._
import ch.epfl.pop.model.network.method.message.data.{ActionType, ObjectType}
import ch.epfl.pop.model.objects.{Base64Data, Hash, Timestamp}
import org.scalatest.funsuite.{AnyFunSuite => FunSuite}
import org.scalatest.matchers.should.Matchers
import spray.json._

class DeleteChirpSuite extends FunSuite with Matchers {
  test("Constructor/apply works as intended") {
    DELETECHIRP_MESSAGE.chirp_id should equal(CHIRP_ID)
    DELETECHIRP_MESSAGE.timestamp should equal(TIMESTAMP)
    DELETECHIRP_MESSAGE._object should equal(ObjectType.CHIRP)
    DELETECHIRP_MESSAGE.action should equal(ActionType.DELETE)
  }

  test("json conversions work back and forth") {
    val msg2: DeleteChirp = DeleteChirp.buildFromJson(DELETECHIRP_MESSAGE.toJson.toString)
    msg2 should equal(DELETECHIRP_MESSAGE)
  }
}

object DeleteChirpExample {
  val CHIRP_ID: Hash = Hash(Base64Data(""))
  val TIMESTAMP: Timestamp = Timestamp(0)

  val DELETECHIRP_MESSAGE: DeleteChirp = DeleteChirp(CHIRP_ID, TIMESTAMP)
}

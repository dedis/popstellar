package ch.epfl.pop.model.network.method.message.data.socialMedia

import ch.epfl.pop.json.MessageDataProtocol._
import ch.epfl.pop.model.network.method.message.data.socialMedia.AddChirpExample._
import ch.epfl.pop.model.network.method.message.data.{ActionType, ObjectType}
import ch.epfl.pop.model.objects.{Hash, Timestamp}
import org.scalatest.funsuite.{AnyFunSuite => FunSuite}
import org.scalatest.matchers.should.Matchers
import spray.json._

class AddChirpSuite extends FunSuite with Matchers {

  test("Constructor/apply works as intended") {

    ADDCHIRP_MESSAGE.text should equal(TEXT)
    ADDCHIRP_MESSAGE.parent_id should equal(PARENT_ID)
    ADDCHIRP_MESSAGE.timestamp should equal(TIMESTAMP)
    ADDCHIRP_MESSAGE._object should equal(ObjectType.CHIRP)
    ADDCHIRP_MESSAGE.action should equal(ActionType.ADD)
  }

  test("json conversions work back and forth") {
    val msg2: AddChirp = AddChirp.buildFromJson(ADDCHIRP_MESSAGE.toJson.toString)

    msg2 should equal(ADDCHIRP_MESSAGE)
  }
}

object AddChirpExample {
  final val TEXT: String = "text"
  final val PARENT_ID: Option[Hash] = None
  final val TIMESTAMP = Timestamp(0)

  final val ADDCHIRP_MESSAGE: AddChirp = AddChirp(TEXT, PARENT_ID, TIMESTAMP)
}

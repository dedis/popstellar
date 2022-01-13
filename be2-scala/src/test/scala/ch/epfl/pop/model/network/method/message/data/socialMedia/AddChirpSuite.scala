package ch.epfl.pop.model.network.method.message.data.socialMedia

import org.scalatest.{FunSuite, Matchers}
import ch.epfl.pop.model.objects.Timestamp
import ch.epfl.pop.json.MessageDataProtocol._
import ch.epfl.pop.model.network.method.message.data.{ActionType, ObjectType}
import spray.json._

import AddChirpExample._

class AddChirpSuite extends FunSuite with Matchers {

    test("Constructor/apply works as intended"){

        ADDCHIRP_MESSAGE.text should equal(TEXT)
        ADDCHIRP_MESSAGE.parent_id should equal(PARENT_ID)
        ADDCHIRP_MESSAGE.timestamp should equal(TIMESTAMP)
        ADDCHIRP_MESSAGE._object should equal(ObjectType.CHIRP)
        ADDCHIRP_MESSAGE.action should equal(ActionType.ADD)
    }

    test("json conversions work back and forth"){
        val msg2: AddChirp = AddChirp.buildFromJson(AddChirpExample.ADDCHIRP_MESSAGE.toJson.toString)

        msg2 should equal(AddChirpExample.ADDCHIRP_MESSAGE)
    }
}

object AddChirpExample {
    final val TEXT: String = "text"
    final val PARENT_ID: Option[String] = None
    final val TIMESTAMP = Timestamp(0)

    final val ADDCHIRP_MESSAGE: AddChirp = AddChirp(TEXT, PARENT_ID, TIMESTAMP)
}

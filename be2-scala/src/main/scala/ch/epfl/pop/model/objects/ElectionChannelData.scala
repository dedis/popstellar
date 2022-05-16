package ch.epfl.pop.model.objects

import ch.epfl.pop.json.MessageDataProtocol._
import ch.epfl.pop.model.network.Parsable
import ch.epfl.pop.model.network.method.message.data.ObjectType
import spray.json._

//FIXME: do we need to extend it to ChannelData
//Object type specifying the ElectionChannelData to keep track of the private key
final case class ElectionChannelData (
                                    privateKey: PrivateKey
                                    ) {
  def toJsonString: String = {
    val that: ElectionChannelData = this // tricks the compiler into inferring the right type
    that.toJson.toString
  }

  def update(key: PrivateKey): ElectionChannelData = {
    ElectionChannelData(key)
  }

  def getObjectType: ObjectType.ObjectType = ObjectType.ELECTION
}

object ElectionChannelData  extends Parsable {
  def apply(key: PrivateKey): ElectionChannelData = new ElectionChannelData(key)

  override def buildFromJson(payload: String): ElectionChannelData = payload.parseJson.asJsObject.convertTo[ElectionChannelData]
}


package ch.epfl.pop.model.network.method
import ch.epfl.pop.json.HighLevelProtocol._
import ch.epfl.pop.model.network.Parsable
import ch.epfl.pop.model.objects.{Channel, Hash, PublicKey}
import spray.json._
final case class GreetServer(publicKey: PublicKey, clientAddress: String, serverAddress: String) extends Params {
  override def hasChannel: Boolean = false

  override def hasMessage: Boolean = false
}

object GreetServer extends Parsable {

  def apply(publicKey: PublicKey, clientAddress: String, serverAddress: String): GreetServer = {
    new  GreetServer(publicKey, clientAddress, serverAddress)
  }

  override def buildFromJson(payload: String): Any = payload.parseJson.asJsObject.convertTo[GreetServer]
}

package ch.epfl.pop.model.objects

import ch.epfl.pop.json.MessageDataProtocol._
import ch.epfl.pop.model.network.Parsable
import ch.epfl.pop.model.network.method.message.Message
import ch.epfl.pop.model.network.method.message.data.lao.CreateLao
import ch.epfl.pop.model.network.method.message.data.rollCall.CloseRollCall
import spray.json._

//Ed25519 is used to verify the signatures, therefore I also use it to sign data
import com.google.crypto.tink.subtle.Ed25519Sign

//a pair of byte arrays works better as keypair than the actual keypair object, since Ed25519Sign.Keypair isn't directly convertible to json with spray
//FIXME: the private key should be stored in a secure way in the future
/**
 * @param owner      : the LAO owner's public key, with which the signed messages can be authenticated
 * @param attendees  : list of the last roll call attendees
 * @param privateKey : the LAO's own private key, used to sign messages
 * @param publicKey  : the LAO's own public key, used to sign messages
 * @param witnesses  : the LAO's list of witnesses
 * @param address : the canonical address of the Server
 */
final case class LaoData(
                          owner: PublicKey,
                          attendees: List[PublicKey],
                          privateKey: PrivateKey,
                          publicKey: PublicKey,
                          witnesses: List[PublicKey],
                          address: String
                        ) {
  def toJsonString: String = {
    val that: LaoData = this // tricks the compiler into inferring the right type
    that.toJson.toString
  }

  def updateWith(message: Message, adr: Option[String]): LaoData = {
    message.decodedData.fold(this) {
      case call: CloseRollCall => adr match {
        case Some(str) => LaoData(owner, call.attendees, privateKey, publicKey, witnesses, str)
        case _ => LaoData(owner, call.attendees, privateKey, publicKey, witnesses, address)
      }

      case lao: CreateLao => adr match {
        case Some(str) => LaoData(lao.organizer, lao.organizer :: Nil, privateKey, publicKey, lao.witnesses, str)
        case _ => LaoData(lao.organizer, lao.organizer :: Nil, privateKey, publicKey, lao.witnesses, address)
      }
      case _ => this
    }
  }
}

object LaoData extends Parsable {
  def apply(
             owner: PublicKey,
             attendees: List[PublicKey],
             privateKey: PrivateKey,
             publicKey: PublicKey,
             witnesses: List[PublicKey]
           ): LaoData = {
    new LaoData(owner, attendees, privateKey, publicKey, witnesses, "")
  }

  // to simplify the use of updateWith during a CreateLao process, the keypair is generated here
  // in the same way as it would be elsewhere
  def apply(): LaoData = {
    val keyPair: Ed25519Sign.KeyPair = Ed25519Sign.KeyPair.newKeyPair
    LaoData(null, List.empty, PrivateKey(Base64Data.encode(keyPair.getPrivateKey)), PublicKey(Base64Data.encode(keyPair.getPublicKey)), List.empty, "")
  }

  override def buildFromJson(payload: String): LaoData = payload.parseJson.asJsObject.convertTo[LaoData] // doesn't decode data

  def getName: String = "LaoData"

}

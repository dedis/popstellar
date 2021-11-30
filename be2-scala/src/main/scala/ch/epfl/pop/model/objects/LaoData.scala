package ch.epfl.pop.model.objects

import ch.epfl.pop.json.HighLevelProtocol._
import ch.epfl.pop.json.MessageDataProtocol._
import ch.epfl.pop.model.network.Parsable
import ch.epfl.pop.model.objects._
import ch.epfl.pop.model.network.method.message.Message
import ch.epfl.pop.model.network.method.message.data.lao.CreateLao
import ch.epfl.pop.model.network.method.message.data.rollCall.CloseRollCall
import spray.json._

//Ed25519 is used to verify the signatures, therefore I also use it to sign data
import com.google.crypto.tink.subtle.Ed25519Sign

//a pair of byte arrays works better as keypair than the actual keypair object, since Ed25519Sign.Keypair isn't directly convertible to json with spray
case class LaoData(
    owner: PublicKey,
    attendees: List[PublicKey],
    privateKey: Array[Byte], 
    publicKey: Array[Byte],
    witnesses: List[PublicKey]
){
    def toJsonString: String = {
      val that: LaoData = this // tricks the compiler into inferring the right type
      that.toJson.toString
    }

    def updateWith(message: Message): LaoData = {
      if (message.decodedData == None){
        this
      }
      else if (message.decodedData.get.isInstanceOf[CloseRollCall]){
        LaoData(owner, message.decodedData.get.asInstanceOf[CloseRollCall].attendees, privateKey, publicKey, List.empty)
      }
      else if (message.decodedData.get.isInstanceOf[CreateLao]){
        val ownerPk: PublicKey = message.decodedData.get.asInstanceOf[CreateLao].organizer
        LaoData(ownerPk, List(ownerPk), privateKey, publicKey, List.empty)
      }
      else {
        this
      }
    }

}

object LaoData extends Parsable {
  def apply(
             owner: PublicKey,
             attendees: List[PublicKey],
             privateKey: Array[Byte],
             publicKey: Array[Byte],
             witnesses: List[PublicKey]
           ): LaoData = {
    new LaoData(owner, attendees, privateKey, publicKey, witnesses)
  }

  override def buildFromJson(payload: String): LaoData = payload.parseJson.asJsObject.convertTo[LaoData] // doesn't decode data

  def getName: String = "LaoData"

  //function for write to decide whether a message should change LaoData
  def isAffectedBy(message: Message): Boolean = {
    message.decodedData != None && (message.decodedData.get.isInstanceOf[CloseRollCall] || message.decodedData.get.isInstanceOf[CreateLao])
  }


  //to simplify the use of updateWith during a CreateLao process, the keypair is generated here in the same way as it would be elsewhere
  def emptyLaoData: LaoData = {
    val keyPair: Ed25519Sign.KeyPair = Ed25519Sign.KeyPair.newKeyPair
    LaoData(null, List.empty, keyPair.getPrivateKey, keyPair.getPublicKey, List.empty)
  }
}
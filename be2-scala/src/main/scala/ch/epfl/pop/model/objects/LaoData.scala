package ch.epfl.pop.model.objects

import ch.epfl.pop.json.HighLevelProtocol._
import ch.epfl.pop.json.MessageDataProtocol._
import ch.epfl.pop.model.network.Parsable
import ch.epfl.pop.model.objects._
import ch.epfl.pop.model.network.method.message.Message
import ch.epfl.pop.model.network.method.message.data.lao.CreateLao
import ch.epfl.pop.model.network.method.message.data.rollCall.CloseRollCall
import spray.json._

case class LaoData(
    owner: PublicKey,
    attendees: List[PublicKey],
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
        LaoData(owner, message.decodedData.get.asInstanceOf[CloseRollCall].attendees, List.empty)
      }
      else if (message.decodedData.get.isInstanceOf[CreateLao]){
        val ownerPk: PublicKey = message.decodedData.get.asInstanceOf[CreateLao].organizer
        LaoData(ownerPk, List(ownerPk), List.empty)
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
             witnesses: List[PublicKey]
           ): LaoData = {
    new LaoData(owner, attendees, witnesses)
  }

  override def buildFromJson(payload: String): LaoData = payload.parseJson.asJsObject.convertTo[LaoData] // doesn't decode data

  def getName: String = "LaoData"

  //function for write to decide whether a message should change LaoData
  def isAffectedBy(message: Message): Boolean = {
    message.decodedData != None && (message.decodedData.get.isInstanceOf[CloseRollCall] || message.decodedData.get.isInstanceOf[CreateLao])
  }


  //to simplify the use of updateWith during a CreateLao process
  def emptyLaoData: LaoData = LaoData(null, List.empty, List.empty)
}
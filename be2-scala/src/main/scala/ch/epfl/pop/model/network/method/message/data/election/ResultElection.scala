package ch.epfl.pop.model.network.method.message.data.election

import ch.epfl.pop.json.MessageDataProtocol._
import ch.epfl.pop.model.network.Parsable
import ch.epfl.pop.model.network.method.message.data.ActionType.ActionType
import ch.epfl.pop.model.network.method.message.data.{ActionType, MessageData, ObjectType}
import ch.epfl.pop.model.network.method.message.data.ObjectType.ObjectType
import ch.epfl.pop.model.objects.Signature
import spray.json._

case class ResultElection(
                           questions: List[ElectionQuestionResult],
                           witness_signatures: List[Signature]
                         ) extends MessageData {
  override val _object: ObjectType = ObjectType.ELECTION
  override val action: ActionType = ActionType.RESULT
}

object ResultElection extends Parsable {
  def apply(
             questions: List[ElectionQuestionResult],
             witness_signatures: List[Signature]
           ): ResultElection = new ResultElection(questions, witness_signatures)

  override def buildFromJson(payload: String): ResultElection = {
    val a = payload.parseJson
    println(a)
    val b = a.asJsObject
    println(b)
    val c = b.convertTo[ResultElection]
    print(c)
    c
    //payload.parseJson.asJsObject.convertTo[ResultElection]
  }
}

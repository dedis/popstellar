package ch.epfl.pop.pubsub.graph.validators

import ch.epfl.pop.model.network.JsonRpcRequest
import ch.epfl.pop.model.network.method.message.Message
import ch.epfl.pop.model.network.method.message.data.socialMedia.{AddChirp}
import ch.epfl.pop.model.objects.{Hash, Base64Data, PublicKey, Channel}
import ch.epfl.pop.pubsub.graph.{GraphMessage, PipelineError, DbActor}
import ch.epfl.pop.model.network.method.message.data.{ActionType, ObjectType}
import ch.epfl.pop.model.network.method.message.data.rollCall.CloseRollCall

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.{Await, Future}



case object SocialMediaValidator extends MessageDataContentValidator with EventValidator {
    override def EVENT_HASH_PREFIX: String = "SocialMedia" //to check later on

    def validateAddChirp(rpcMessage: JsonRpcRequest): GraphMessage = {
        def validationError(reason: String): PipelineError = super.validationError(reason, "AddChirp", rpcMessage.id)

        // FIXME: create method for any message instance as 2nd arg
        // method not functional, find some other way
        //def isInstanceOfCloseRollCall(msg: Message): Boolean = msg.decodedData.get._object == ObjectType.ROLL_CALL && msg.decodedData.get.action == ActionType.CLOSE

        rpcMessage.getParamsMessage match {


            // FIXME need more checks
            case Some(message) => {
                val data: AddChirp = message.decodedData.get.asInstanceOf[AddChirp]

                val actualSender: PublicKey = message.sender //sender's PK

                //here, we can actually just get the channel directly and compare with sender but it's for the test
                /*val expectedSender: Base64Data = DbActor.Catchup(rpcMessage.getParamsChannel) match {
                    case DbActorCatchupAck(li) => li.filter(m => m.data.toJson.
                    case _ => Base64Data("") //maybe throw exception?
                }*/
                //check whether sender is in the attendees of the last RollCall (the channel will be the same as his pk normally?? but it's for the example)

                /*val ask: Future[List[PublicKey]] = (DbActor.getInstance ? DbActor.Catchup(Channel.rootChannel)).map {
                    case DbActor.DbActorCatchupAck(li: List[Message]) => li.filter(m => isInstanceOfCloseRollCall(m)) match {
                        case head::Nil => head.decodedData.get.asInstanceOf[CloseRollCall].attendees
                        case _ => Nil //??
                    }
                    case _ => Nil //maybe throw exception?
                }

                val listAttendees = Await.result(ask, duration)*/

                if (!validateTimestampStaleness(data.timestamp)) {
                    Right(validationError(s"stale timestamp (${data.timestamp})"))
                }
                /*else if(!listAttendees.contains(actualSender)){
                    Right(validationError(s"invalid sender (${actualSender})"))
                }*/
                // FIXME: validate parent ID: need to store all the channel's tweet IDs somewhere to compare?
                else{
                    Left(rpcMessage)
                }
            }
            case _ => Right(validationErrorNoMessage(rpcMessage.id))
        }
    }
}
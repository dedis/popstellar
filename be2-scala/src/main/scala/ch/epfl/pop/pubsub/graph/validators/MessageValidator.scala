package ch.epfl.pop.pubsub.graph.validators

import akka.pattern.AskableActorRef

import ch.epfl.pop.model.network.JsonRpcRequest
import ch.epfl.pop.model.network.method.message.Message
import ch.epfl.pop.model.objects.{Channel, Hash, PublicKey}
import ch.epfl.pop.pubsub.AskPatternConstants
import ch.epfl.pop.pubsub.graph.{DbActor, ErrorCodes, GraphMessage, PipelineError}

import scala.concurrent.Await
import scala.concurrent.ExecutionContext.Implicits.global


object MessageValidator extends ContentValidator with AskPatternConstants {
  /**
   * Creates a validation error message for reason <reason> that happened in
   * validator module <validator> with optional error code <errorCode>
   *
   * @param reason    the reason of the validation error
   * @param validator validator module where the error occurred
   * @param errorCode error code related to the error
   * @return a description of the error and where it occurred
   */
  override def validationError(reason: String, validator: String, rpcId: Option[Int], errorCode: ErrorCodes.ErrorCodes = ErrorCodes.INVALID_DATA): PipelineError =
    super.validationError(reason, validator, rpcId, errorCode)

  def validateMessage(rpcMessage: JsonRpcRequest): GraphMessage = {

    val message: Message = rpcMessage.getParamsMessage.get
    val expectedId: Hash = Hash.fromStrings(message.data.toString, message.signature.toString)

    if (message.message_id != expectedId) {
      Right(validationError("Invalid message_id", "MessageValidator", rpcMessage.id))
    } else if (!message.signature.verify(message.sender, message.data)) {
      Right(validationError("Invalid sender signature", "MessageValidator", rpcMessage.id))
    } else if (!message.witness_signatures.forall(ws => ws.verify(message.message_id))) {
      Right(validationError("Invalid witness signature", "MessageValidator", rpcMessage.id))
    } else {
      Left(rpcMessage)
    }
  }

  /**
   * checks whether the sender of the JsonRpcRequest is in the attendee list inside the LAO's data
   * @param sender    the sender we want to verify
   * @param channel   the channel we want the LaoData for
   * @param dbActor   the AskableActorRef we use (by default the main DbActor, obtained through getInstance)
   */
  def validateAttendee(sender: PublicKey, channel: Channel, dbActor: AskableActorRef = DbActor.getInstance): Boolean = {
    val ask = dbActor ? DbActor.ReadLaoData(channel)
    Await.result(ask, duration) match {
      case DbActor.DbActorReadLaoDataAck(Some(laoData)) => {
        laoData.attendees.contains(sender)
      }
      case DbActor.DbActorReadLaoDataAck(None) => {
        false
      }
      case DbActor.DbActorNAck(code, description) => {
        false
      }
    }
  }

  /**
   * checks whether the sender of the JsonRpcRequest is the LAO owner
   * @param sender    the sender we want to verify
   * @param channel   the channel we want the LaoData for
   * @param dbActor   the DbActor we use (by default the main one, obtained through getInstance)
   */
  def validateOwner(sender: PublicKey, channel: Channel, dbActor: AskableActorRef = DbActor.getInstance): Boolean = {
    val ask = dbActor ? DbActor.ReadLaoData(channel)
    Await.result(ask, duration) match {
      case DbActor.DbActorReadLaoDataAck(Some(laoData)) => laoData.owner == sender
      case DbActor.DbActorReadLaoDataAck(None) => false
      case DbActor.DbActorNAck(code, description) => false
    }
  }
}

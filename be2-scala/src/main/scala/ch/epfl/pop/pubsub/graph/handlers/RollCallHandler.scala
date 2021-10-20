package ch.epfl.pop.pubsub.graph.handlers

import akka.NotUsed
import akka.stream.scaladsl.Flow
import ch.epfl.pop.model.network.method.message.Message
import ch.epfl.pop.model.network.requests.rollCall.{JsonRpcRequestCloseRollCall, JsonRpcRequestCreateRollCall, JsonRpcRequestOpenRollCall, JsonRpcRequestReopenRollCall}
import ch.epfl.pop.model.network.{JsonRpcRequest, JsonRpcResponse}
import ch.epfl.pop.pubsub.graph.{DbActor, ErrorCodes, GraphMessage, PipelineError}

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.{Await, Future}
import ch.epfl.pop.model.network.method.message.data.rollCall.CloseRollCall
import ch.epfl.pop.model.objects.Base64Data
import ch.epfl.pop.model.objects.Hash
import ch.epfl.pop.model.objects.Signature
import ch.epfl.pop.model.objects.WitnessSignaturePair
import org.slf4j.LoggerFactory

case object RollCallHandler extends MessageHandler {

  val logger = LoggerFactory.getLogger("Roll Call Handler")

  override val handler: Flow[GraphMessage, GraphMessage, NotUsed] = Flow[GraphMessage].map {
    case Left(jsonRpcMessage) => jsonRpcMessage match {
      case message@(_: JsonRpcRequestCreateRollCall) => handleCreateRollCall(message)
      case message@(_: JsonRpcRequestOpenRollCall) => handleOpenRollCall(message)
      case message@(_: JsonRpcRequestReopenRollCall) => handleReopenRollCall(message)
      case message@(_: JsonRpcRequestCloseRollCall) => handleCloseRollCall(message)
      case _ => Right(PipelineError(
        ErrorCodes.SERVER_ERROR.id,
        "Internal server fault: RollCallHandler was given a message it could not recognize",
        jsonRpcMessage match {
          case r: JsonRpcRequest => r.id
          case r: JsonRpcResponse => r.id
          case _ => None
        }
      ))
    }
    case graphMessage@_ => graphMessage
  }

  def handleCreateRollCall(rpcMessage: JsonRpcRequest): GraphMessage = {
    val ask: Future[GraphMessage] = dbAskWritePropagate(rpcMessage)
    Await.result(ask, duration)
  }

  def handleOpenRollCall(rpcMessage: JsonRpcRequest): GraphMessage = {
    val ask: Future[GraphMessage] = dbAskWritePropagate(rpcMessage)
    Await.result(ask, duration)
  }

  def handleReopenRollCall(rpcMessage: JsonRpcRequest): GraphMessage = {
    val ask: Future[GraphMessage] = dbAskWritePropagate(rpcMessage)
    Await.result(ask, duration)
  }

  def handleCloseRollCall(rpcMessage: JsonRpcRequest): GraphMessage = {
    val ask: Future[GraphMessage] = dbAskWritePropagate(rpcMessage)
    

    //Build attendee message 
    val pks = rpcMessage.getDecodedData.get.asInstanceOf[CloseRollCall].attendees
    val data_64 = Base64Data.encode(pks.map(_.toString.toByte).toArray)
 
    //Build hash from pks
    val builder = new StringBuilder()
    val data_pks = pks.map(pk => (pk.toString.length,pk.toString)).foldLeft(builder){case (b, (len, pk)) => b.append(len).append(pk)}.toString()
    val msg_id = Hash.sha256Hash(data_pks)

    //Default values
    val signature: Signature = null
    val witness_signatures = Nil

    //Final new attendee list message 
    val pk_sender = rpcMessage.getParamsMessage.get.sender
    val message_pks = Message(data_64, pk_sender, null, msg_id, witness_signatures, None)
  
    //Ask DB to write 
    val c = rpcMessage.getParamsChannel
    (dbActor ? DbActor.Write(c, message_pks)).map {
      case DbActor.DbActorWriteAck => {
        logger.info(f"Saved pks to channel ${c.channel}")
        data_pks.zip(1 to data_pks.size).map{case (pk, i) => logger.debug(f"pk_${i} : ${pk}") 
        }
      }
      case DbActor.DbActorNAck(code, description) => logger.error("Could not save attendees public keys")
    }
    
    //Return ask result
    Await.result(ask, duration)
  }
}

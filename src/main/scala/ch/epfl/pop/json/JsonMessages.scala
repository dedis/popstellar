package ch.epfl.pop.json

import ch.epfl.pop.json.Methods.Methods
import ch.epfl.pop.json.JsonUtils.JSON_RPC_VERSION


/**
 * Collection of parsed Json messages
 */
object JsonMessages {

  /** Parsed Json message as output of the JsonParser */
  sealed trait JsonMessage


  /* --------------------------------------------------------- */
  /* ---------------- ANSWER MESSAGES SERVER ----------------- */
  /* --------------------------------------------------------- */

  /** Parsed answer Json message from the server */
  sealed trait JsonMessageAnswerServer extends JsonMessage


  /** Parsed result answer (Int result) Json message from the server */
  final case class AnswerResultIntMessageServer(
                                                 id: Int,
                                                 result: Int = 0,
                                                 jsonrpc: String = JSON_RPC_VERSION
                                               ) extends JsonMessageAnswerServer

  /** Parsed result answer (Array result) Json message from the server */
  final case class AnswerResultArrayMessageServer(
                                                   id: Int,
                                                   result: ChannelMessages,
                                                   jsonrpc: String = JSON_RPC_VERSION
                                                 ) extends JsonMessageAnswerServer

  /** Parsed error answer Json message from the server */
  final case class AnswerErrorMessageServer(
                                             id: Option[Int],
                                             error: MessageErrorContent,
                                             jsonrpc: String = JSON_RPC_VERSION
                                           ) extends JsonMessageAnswerServer

  /** Parsed client propagate a message on a channel query */
  final case class PropagateMessageServer(
                                           params: MessageParameters,
                                           method: Methods = Methods.Message,
                                           jsonrpc: String = JSON_RPC_VERSION
                                         ) extends JsonMessageAnswerServer

  /* --------------------------------------------------------- */
  /* ------------ ADMINISTRATION MESSAGES CLIENT ------------- */
  /* --------------------------------------------------------- */

  /** Parsed Administration Json message from the client */
  sealed class JsonMessagePublishClient(
                                         val params: MessageParameters,
                                         val id: Int,
                                         val method: Methods,
                                         val jsonrpc: String = JSON_RPC_VERSION
                                       ) extends JsonMessagePubSubClient


  /* --------------- ADMIN CLIENT MESSAGES --------------- */

  /** Parsed client LAO creation query */
  final case class CreateLaoMessageClient(
                                           override val params: MessageParameters,
                                           override val id: Int,
                                           override val method: Methods,
                                           override val jsonrpc: String = JSON_RPC_VERSION
                                         ) extends JsonMessagePublishClient(params, id, method, jsonrpc)

  /** Parsed client LAO update query */
  final case class UpdateLaoMessageClient(
                                           override val params: MessageParameters,
                                           override val id: Int,
                                           override val method: Methods,
                                           override val jsonrpc: String = JSON_RPC_VERSION
                                         ) extends JsonMessagePublishClient(params, id, method, jsonrpc)

  /** Parsed client LAO state broadcast query */
  final case class BroadcastLaoMessageClient(
                                              override val params: MessageParameters,
                                              override val id: Int,
                                              override val method: Methods,
                                              override val jsonrpc: String = JSON_RPC_VERSION
                                            ) extends JsonMessagePublishClient(params, id, method, jsonrpc)

  /** Parsed client message witness query */
  final case class WitnessMessageMessageClient(
                                                override val params: MessageParameters,
                                                override val id: Int,
                                                override val method: Methods,
                                                override val jsonrpc: String = JSON_RPC_VERSION
                                              ) extends JsonMessagePublishClient(params, id, method, jsonrpc)

  /** Parsed client meeting creation query */
  final case class CreateMeetingMessageClient(
                                               override val params: MessageParameters,
                                               override val id: Int,
                                               override val method: Methods,
                                               override val jsonrpc: String = JSON_RPC_VERSION
                                             ) extends JsonMessagePublishClient(params, id, method, jsonrpc)

  /** Parsed client meeting state broadcast query */
  final case class BroadcastMeetingMessageClient(
                                                  override val params: MessageParameters,
                                                  override val id: Int,
                                                  override val method: Methods,
                                                  override val jsonrpc: String = JSON_RPC_VERSION
                                                ) extends JsonMessagePublishClient(params, id, method, jsonrpc)



  /* --------------------------------------------------------- */
  /* ---------------- PUBSUB MESSAGES CLIENT ----------------- */
  /* --------------------------------------------------------- */

  /** Parsed PubSub Json message from the client */
  sealed trait JsonMessagePubSubClient extends JsonMessage


  /* --------------- PUBSUB CLIENT MESSAGES --------------- */

  /** Parsed client subscribe to a channel query */
  final case class SubscribeMessageClient(
                                           params: MessageParameters,
                                           id: Int,
                                           method: Methods = Methods.Subscribe,
                                           jsonrpc: String = JSON_RPC_VERSION
                                         ) extends JsonMessagePubSubClient

  /** Parsed client unsubscribe from a channel query */
  final case class UnsubscribeMessageClient(
                                             params: MessageParameters,
                                             id: Int,
                                             method: Methods = Methods.Unsubscribe,
                                             jsonrpc: String = JSON_RPC_VERSION
                                           ) extends JsonMessagePubSubClient

  /** Parsed client catchup on past messages on a channel query */
  final case class CatchupMessageClient(
                                         params: MessageParameters,
                                         id: Int,
                                         method: Methods = Methods.Catchup,
                                         jsonrpc: String = JSON_RPC_VERSION
                                       ) extends JsonMessagePubSubClient
}

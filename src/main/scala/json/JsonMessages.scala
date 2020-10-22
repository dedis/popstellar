package json

import JsonMessageTypes._


object JsonMessages {

  sealed trait JsonMessage


  /* --------------------------------------------------------- */
  /* ---------------- ADMINISTRATION MESSAGES ---------------- */
  /* --------------------------------------------------------- */

  sealed trait JsonMessageAdmin extends JsonMessage

  sealed trait JsonMessageAdminClient extends JsonMessageAdmin
  sealed trait JsonMessageAdminServer extends JsonMessageAdmin


  /*
   * ------------ CLIENT MESSAGES ------------
   */

  /**
   * Parsed message from client asking for the creation of a LAO
   *
   * @param name the LAO's name
   * @param date the LAO's creation date
   * @param organizer the organizer's public key
   * @param witnesses list of the witnesses' public keys
   * @param attestation the client's signature
   */
  final case class CreateLaoMessageClient(
    name: String, date: UNKNOWN, organizer: Key, witnesses: List[Key], attestation: Signature
  ) extends JsonMessageAdminClient

  final case class JoinLaoMessageClient(
    lao: Hash, client: Key, attestation: Signature
  ) extends JsonMessageAdminClient

  final case class CreateEventMessageClient(
     lao: Hash, name: String, location: String, attestation: Signature
  ) extends JsonMessageAdminClient

  final case class CreateElectionMessageClient(
    lao: Hash, name: String, attestation: Signature
  ) extends JsonMessageAdminClient

  final case class CastVoteMessageClient(
    election: Hash, client: Key, vote: UNKNOWN, attestation: Signature
  ) extends JsonMessageAdminClient


  /*
   * ------------ SERVER MESSAGES ------------
   */

  final case class AnswerMessageServer(success: Boolean, error: Option[String]) extends JsonMessageAdminServer




  /* --------------------------------------------------------- */
  /* -------------------- PUBSUB MESSAGES -------------------- */
  /* --------------------------------------------------------- */

  sealed trait JsonMessagePubSub extends JsonMessage

  sealed trait JsonMessagePubSubClient extends JsonMessagePubSub
  sealed trait JsonMessagePubSubServer extends JsonMessagePubSub


  /*
   * ------------ CLIENT MESSAGES ------------
   */

  final case class CreateChannelClient(channel: ChannelName, contract: UNKNOWN) extends JsonMessagePubSubClient
  final case class PublishChannelClient(channel: ChannelName, event: UNKNOWN) extends JsonMessagePubSubClient
  final case class SubscribeChannelClient(channel: ChannelName) extends JsonMessagePubSubClient
  final case class FetchChannelClient(channel: ChannelName, event_id: UNKNOWN) extends JsonMessagePubSubClient


  /*
   * ------------ SERVER MESSAGES ------------
   */

  final case class NotifyChannelServer(channel: ChannelName, event_id: UNKNOWN) extends JsonMessagePubSubServer
  final case class FetchChannelServer(
    channel: ChannelName, event_id: UNKNOWN, event_content: UNKNOWN
  ) extends JsonMessagePubSubServer

}





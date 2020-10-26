package json

import JsonMessageTypes._


/**
 * Collection of parsed Json messages
 */
object JsonMessages {

  /** Parsed Json message as output of the JsonParser */
  sealed trait JsonMessage


  /* --------------------------------------------------------- */
  /* ---------------- ADMINISTRATION MESSAGES ---------------- */
  /* --------------------------------------------------------- */

  /** Parsed Administration Json message */
  sealed trait JsonMessageAdmin extends JsonMessage

  /** Parsed Administration Json message from the client */
  sealed trait JsonMessageAdminClient extends JsonMessageAdmin
  /** Parsed Administration Json message from the server */
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

  /**
   * Parsed message from client asking for joining a LAO
   *
   * @param lao the LAO's id
   * @param client the client's key wishing to join the LAO
   * @param attestation the client's signature
   */
  final case class JoinLaoMessageClient(
    lao: Hash, client: Key, attestation: Signature
  ) extends JsonMessageAdminClient

  /**
   * Parsed message from client asking for the creation of an Event
   *
   * @param lao the LAO's id in which to create the event
   * @param name the event's name
   * @param location the event's location
   * @param attestation the client's signature
   */
  final case class CreateEventMessageClient(
     lao: Hash, name: String, location: String, attestation: Signature
  ) extends JsonMessageAdminClient

  /**
   * Parsed message from client asking for the creation of an Election
   *
   * @param lao the LAO's id in which to create the election
   * @param name the election's name
   * @param attestation the client's signature
   */
  final case class CreateElectionMessageClient(
    lao: Hash, name: String, attestation: Signature
  ) extends JsonMessageAdminClient

  /**
   * Parsed message from client asking for his vote to be casted
   *
   * @param election the election's id
   * @param client the client's key wishing to cast a vote
   * @param vote the encrypted client's vote
   * @param attestation the client's signature
   */
  final case class CastVoteMessageClient(
    election: Hash, client: Key, vote: UNKNOWN, attestation: Signature
  ) extends JsonMessageAdminClient


  /*
   * ------------ SERVER MESSAGES ------------
   */

  /**
   * Parsed message from server answering the client
   *
   * @param success true iff the query was accepted
   * @param error the error message (None if success, Some(std) if failure)
   */
  final case class AnswerMessageServer(success: Boolean, error: Option[String]) extends JsonMessageAdminServer




  /* --------------------------------------------------------- */
  /* -------------------- PUBSUB MESSAGES -------------------- */
  /* --------------------------------------------------------- */

  /** Parsed PubSub Json message */
  sealed trait JsonMessagePubSub extends JsonMessage

  /** Parsed PubSub Json message from the client */
  sealed trait JsonMessagePubSubClient extends JsonMessagePubSub
  /** Parsed PubSub Json message from the server */
  sealed trait JsonMessagePubSubServer extends JsonMessagePubSub


  /*
   * ------------ CLIENT MESSAGES ------------
   */

  /**
   * Parsed message from client asking for the creation of a Channel
   *
   * @param channel the channel's name
   * @param contract the channel's contract
   */
  final case class CreateChannelClient(channel: ChannelName, contract: UNKNOWN) extends JsonMessagePubSubClient

  /**
   * Parsed message from client asking for an event to be published
   *
   * @param channel the channel's name
   * @param event the event to be published
   */
  final case class PublishChannelClient(channel: ChannelName, event: UNKNOWN) extends JsonMessagePubSubClient

  /**
   * Parsed message from client asking to subscribe to a Channel
   *
   * @param channel the channel's name
   */
  final case class SubscribeChannelClient(channel: ChannelName) extends JsonMessagePubSubClient

  /**
   * Parsed message from client asking for the content of a Channel
   *
   * @param channel the channel's name
   * @param event_id the event's id
   */
  final case class FetchChannelClient(channel: ChannelName, event_id: UNKNOWN) extends JsonMessagePubSubClient


  /*
   * ------------ SERVER MESSAGES ------------
   */

  /**
   * Parsed message from server notifying all clients subscribed to a Channel
   *
   * @param channel the channel's name
   * @param event_id the event's id
   */
  final case class NotifyChannelServer(channel: ChannelName, event_id: UNKNOWN) extends JsonMessagePubSubServer

  /**
   * Parsed message from server returning the content of a Channel
   *
   * @param channel the channel's name
   * @param event_id the event's id
   * @param event_content the event's information
   */
  final case class FetchChannelServer(
    channel: ChannelName, event_id: UNKNOWN, event_content: UNKNOWN
  ) extends JsonMessagePubSubServer

}

package ch.epfl.pop.pubsub

/**
 * A trait used for unsubscribe requests
 */
trait UnsubMessage

/**
 * A request to unsubscribe
 * @param channel the id of the channel the client want to unsubscribe to
 * @param id the id of the client request
 */
final case class UnsubRequest(channel: String, id : Int) extends UnsubMessage


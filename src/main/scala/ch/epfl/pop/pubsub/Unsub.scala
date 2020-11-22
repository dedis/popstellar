package ch.epfl.pop.pubsub


trait UnsubMessage

final case class UnsubRequest(channel: String, id : Int) extends UnsubMessage


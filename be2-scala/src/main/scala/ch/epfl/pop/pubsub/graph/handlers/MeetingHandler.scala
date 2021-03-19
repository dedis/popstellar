package ch.epfl.pop.pubsub.graph.handlers

import akka.NotUsed
import akka.stream.scaladsl.Flow
import ch.epfl.pop.model.network.JsonRpcMessage
import ch.epfl.pop.model.network.requests.meeting.{JsonRpcRequestCreateMeeting, JsonRpcRequestStateMeeting}

case object MeetingHandler extends MessageHandler {

  override val handler: Flow[JsonRpcMessage, Nothing, NotUsed] = Flow[JsonRpcMessage].map {
    case message@(_: JsonRpcRequestCreateMeeting) => handleCreateMeeting(message); ???
    case message@(_: JsonRpcRequestStateMeeting) => handleStateMeeting(message); ???
    case _ => ???
  }

  def handleCreateMeeting(message: JsonRpcMessage) {}
  def handleStateMeeting(message: JsonRpcMessage) {}
}

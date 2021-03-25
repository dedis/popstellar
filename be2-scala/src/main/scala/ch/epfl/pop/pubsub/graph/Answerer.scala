package ch.epfl.pop.pubsub.graph

import akka.NotUsed
import akka.http.scaladsl.model.ws.TextMessage
import akka.stream.scaladsl.Flow

object Answerer {

  def sendAnswer(graphMessage: GraphMessage): Unit = TextMessage.Strict("FIXME answer properly")

  val answerer: Flow[GraphMessage, Unit, NotUsed] = Flow[GraphMessage].map(sendAnswer)
}

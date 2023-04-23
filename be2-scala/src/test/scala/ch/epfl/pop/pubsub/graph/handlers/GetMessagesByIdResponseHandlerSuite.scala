package ch.epfl.pop.pubsub.graph.handlers

import akka.NotUsed
import akka.actor.{ActorRef, ActorSystem, Props}
import akka.pattern.AskableActorRef
import akka.stream.scaladsl.Flow
import akka.testkit.TestKit
import ch.epfl.pop.pubsub.AskPatternConstants
import ch.epfl.pop.pubsub.graph.GraphMessage
import org.scalatest.funsuite.AnyFunSuiteLike

class GetMessagesByIdResponseHandlerSuite extends TestKit(ActorSystem("GetMessagesByIdResponseHandlerSuiteSystem")) with AnyFunSuiteLike with AskPatternConstants {
  final val toyDbActorRef: ActorRef = system.actorOf(Props(new ToyDbActor))
  final val boxUnderTest: Flow[GraphMessage, GraphMessage, NotUsed] = GetMessagesByIdResponseHandler.graph(toyDbActorRef)

  test("receiving a get messages by id response sends a write message to the data base"){

  }


}

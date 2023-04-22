package ch.epfl.pop.decentralized

import akka.actor.{ActorSystem, Props}
import akka.pattern.AskableActorRef
import akka.stream.scaladsl.Source
import akka.testkit.{TestKit, TestProbe}
import ch.epfl.pop.model.network.method.{Heartbeat, ParamsWithMap}
import ch.epfl.pop.model.network.{JsonRpcRequest, MethodType}
import ch.epfl.pop.model.objects.{Base64Data, Channel, Hash}
import ch.epfl.pop.pubsub.graph.validators.RpcValidator
import org.scalatest.BeforeAndAfterAll
import org.scalatest.funsuite.{AnyFunSuiteLike => FunSuiteLike}
import org.scalatest.matchers.should.Matchers
import util.examples.JsonRpcRequestExample
import ch.epfl.pop.pubsub.AskPatternConstants

import scala.concurrent.Await
import scala.concurrent.duration.DurationInt
import scala.util.Success

class HeartbeatGeneratorSuite extends TestKit(ActorSystem("HeartbeatGeneratorSuiteSystem")) with FunSuiteLike with Matchers with BeforeAndAfterAll with AskPatternConstants {
  final val CHANNEL1_NAME: String = "/root/wex/lao1Id"
  final val CHANNEL2_NAME: String = "/root/wex/lao2Id"
  final val CHANNEL1 = new Channel(CHANNEL1_NAME)
  final val CHANNEL2 = new Channel(CHANNEL2_NAME)
  final val toyDbActorRef: AskableActorRef = system.actorOf(Props(new ToyDbActor))
  final val MESSAGE1_ID: Hash = Hash(Base64Data.encode("message1Id"))
  final val MESSAGE2_ID: Hash = Hash(Base64Data.encode("message2Id"))
  final val MESSAGE3_ID: Hash = Hash(Base64Data.encode("message3Id"))
  final val MESSAGE4_ID: Hash = Hash(Base64Data.encode("message4Id"))
  final val MESSAGE5_ID: Hash = Hash(Base64Data.encode("message5Id"))

  final val generatorRef: AskableActorRef = system.actorOf(Props(new HeartbeatGenerator(toyDbActorRef)))
  override def afterAll(): Unit = {
    // Stops the test actor system
    TestKit.shutdownActorSystem(system)
  }

  test("generator should send a result to the connectionMediator"){
    val expected = Map(CHANNEL1 -> Set(MESSAGE1_ID), CHANNEL2 -> Set(MESSAGE2_ID))
    val testProbe = TestProbe()
    val ask = generatorRef ? Monitor.GenerateAndSendHeartbeat(testProbe.ref)
    Await.ready(ask, duration).value match {
      case Some(Success(heartbeat : Heartbeat)) => heartbeat.channelsToMessageIds should equal(expected)
      case _ => 1 should equal(0)
    }

  }

}

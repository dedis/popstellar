package ch.epfl.pop.decentralized

import akka.actor.{ActorRef, ActorSystem, Props}
import akka.pattern.AskableActorRef
import akka.testkit.{TestKit, TestProbe}
import ch.epfl.pop.model.network.method.Heartbeat
import ch.epfl.pop.model.objects.{Base64Data, Channel, DbActorNAckException, Hash}
import org.scalatest.BeforeAndAfterAll
import org.scalatest.funsuite.{AnyFunSuiteLike => FunSuiteLike}
import org.scalatest.matchers.should.Matchers
import ch.epfl.pop.pubsub.AskPatternConstants
import ch.epfl.pop.storage.DbActor
import ch.epfl.pop.model.objects.DbActorNAckException

import scala.concurrent.duration.DurationInt

class HeartbeatGeneratorSuite extends TestKit(ActorSystem("HeartbeatGeneratorSuiteSystem")) with FunSuiteLike with Matchers with BeforeAndAfterAll with AskPatternConstants {
  final val CHANNEL1_NAME: String = "/root/wex/lao1Id"
  final val CHANNEL2_NAME: String = "/root/wex/lao2Id"
  final val CHANNEL1 = new Channel(CHANNEL1_NAME)
  final val CHANNEL2 = new Channel(CHANNEL2_NAME)
  final val toyDbActorRef: AskableActorRef = system.actorOf(Props(new ToyDbActor))
  final val failingToyDbActorRef: AskableActorRef = system.actorOf(Props(new FailingToyDbActor))
  final val MESSAGE1_ID: Hash = Hash(Base64Data.encode("message1Id"))
  final val MESSAGE2_ID: Hash = Hash(Base64Data.encode("message2Id"))
  final val MESSAGE3_ID: Hash = Hash(Base64Data.encode("message3Id"))
  final val MESSAGE4_ID: Hash = Hash(Base64Data.encode("message4Id"))
  final val MESSAGE5_ID: Hash = Hash(Base64Data.encode("message5Id"))

  private val timeout = 3.second

  override def afterAll(): Unit = {
    // Stops the test actor system
    TestKit.shutdownActorSystem(system)
  }

  test("generator should send a result to the connectionMediator") {
    val generatorRef: ActorRef = system.actorOf(HeartbeatGenerator.props(toyDbActorRef))
    val expected = Map(CHANNEL1 -> Set(MESSAGE1_ID), CHANNEL2 -> Set(MESSAGE4_ID))
    val testProbe = TestProbe()
    generatorRef ! Monitor.GenerateAndSendHeartbeat(testProbe.ref)
    testProbe.expectMsg(timeout, Heartbeat(expected))
  }

  test("generator should send nothing when failing to query the data base") {
    val generatorRef: ActorRef = system.actorOf(HeartbeatGenerator.props(failingToyDbActorRef))
    val testProbe = TestProbe()
    generatorRef ! Monitor.GenerateAndSendHeartbeat(testProbe.ref)
    testProbe.expectNoMessage(timeout)
  }

}

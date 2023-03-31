package ch.epfl.pop.decentralized

import akka.actor.{ActorRef, ActorSystem, Props}
import akka.pattern.AskableActorRef
import akka.testkit.{ImplicitSender, TestKit}
import ch.epfl.pop.model.objects.{Base64Data, Channel, Hash}
import ch.epfl.pop.pubsub.{AskPatternConstants, PubSubMediator}
import org.scalatest.BeforeAndAfterAll
import org.scalatest.matchers.should.Matchers
import org.scalatest.funsuite.{AnyFunSuiteLike => FunSuiteLike}
import akka.pattern.ask
import ch.epfl.pop.model.network.method.message.Message
import ch.epfl.pop.storage.DbActor

import scala.collection.mutable
import scala.concurrent.Await
class HbActorSuite extends TestKit(ActorSystem("HbActorSuiteActorSystem")) with FunSuiteLike with ImplicitSender with Matchers with BeforeAndAfterAll with AskPatternConstants {

  final val CHANNEL1_NAME: String = "/root/wex/lao1Id"
  final val CHANNEL2_NAME: String = "/root/wex/lao2Id"
  final val MESSAGE1_ID: Hash = Hash(Base64Data.encode("message1Id"))
  final val MESSAGE2_ID: Hash = Hash(Base64Data.encode("message2Id"))
  final val MESSAGE3_ID: Hash = Hash(Base64Data.encode("message3Id"))
  final val MESSAGE4_ID: Hash = Hash(Base64Data.encode("message4" +
    "Id"))
  final val MESSAGE5_ID: Hash = Hash(Base64Data.encode("message5Id"))
  final val MESSAGE1: Message = Message(null, null, null, MESSAGE1_ID, null, null)
  final val MESSAGE4: Message = Message(null, null, null, MESSAGE4_ID, null, null)
  final val RECEIVED_HEART_BEAT = mutable.HashMap(CHANNEL1_NAME -> List(MESSAGE1_ID, MESSAGE2_ID, MESSAGE3_ID), CHANNEL2_NAME -> List(MESSAGE4_ID, MESSAGE5_ID))
  final val toyDbActorRef: AskableActorRef = system.actorOf(Props(new ToyDbActor))

  def sleep(duration: Long = 50): Unit = {
    Thread.sleep(duration)
  }

  test("toyDbActor sends the correct set of channels"){ // needed for debugging
    val ask = toyDbActorRef ? DbActor.GetSetOfChannels()
    val answer = Await.result(ask, duration)
    val res = answer.asInstanceOf[DbActor.DbActorGetSetOfChannelsAck].channels
    val expected : Set[String] = Set(CHANNEL1_NAME, CHANNEL2_NAME)
    res should equal(expected)
  }
  test("toyDbActor catchs up correctly the first channel"){ // needed for debugging
    val ask = toyDbActorRef ? DbActor.ReadChannelData(Channel(CHANNEL1_NAME))
    val answer = Await.result(ask, duration)
    val res = answer.asInstanceOf[DbActor.DbActorReadChannelDataAck].channelData.messages
    val expected = List(MESSAGE1_ID)
    res should equal(expected)
  }
  test("toyDbActor catchs up correctly the second channel") { // needed for debugging
    val ask = toyDbActorRef ? DbActor.ReadChannelData(Channel(CHANNEL2_NAME))
    val answer = Await.result(ask, duration)
    val res = answer.asInstanceOf[DbActor.DbActorReadChannelDataAck].channelData.messages
    val expected = List(MESSAGE4_ID)
    res should equal(expected)
  }

  test("RetrieveHeartbeat correctly retrieves the content of the dataBase") {
    val hbActor: AskableActorRef = system.actorOf(Props(HbActor(toyDbActorRef)))
    val ask = hbActor ? HbActor.RetrieveHeartbeat()
    val answer = Await.result(ask, duration)
    val res = answer.asInstanceOf[HbActor.HbActorRetrieveHeartbeatAck].heartbeatContent
    val expected = mutable.HashMap(CHANNEL1_NAME -> List(MESSAGE1_ID), CHANNEL2_NAME -> List(MESSAGE4_ID))
    res should equal(expected)
  }

  test ("CompareHeartbeat correctly compares the received Heartbeat with the content of the DB") {
    val hbActor: AskableActorRef = system.actorOf(Props(HbActor(toyDbActorRef)))
    val ask = hbActor ? HbActor.CompareHeartbeat(RECEIVED_HEART_BEAT)
    val answer = Await.result(ask, duration)
    val res = answer.asInstanceOf[HbActor.HbActorCompareHeartBeatAck].missingIds
    val expected = mutable.HashMap(CHANNEL1_NAME -> List(MESSAGE2_ID,MESSAGE3_ID), CHANNEL2_NAME -> List(MESSAGE5_ID))
    res should equal(expected)
  }


}

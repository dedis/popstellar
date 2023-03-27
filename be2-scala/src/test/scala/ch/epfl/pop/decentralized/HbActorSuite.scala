package ch.epfl.pop.decentralized

import akka.actor.{ActorSystem, Props}
import akka.testkit.{ImplicitSender, TestKit}
import ch.epfl.pop.decentralized.HbActor._
import ch.epfl.pop.model.objects.{Base64Data, Hash}
import ch.epfl.pop.pubsub.AskPatternConstants
import org.scalatest.BeforeAndAfterAll
import org.scalatest.matchers.should.Matchers
import org.scalatest.funsuite.{AnyFunSuiteLike => FunSuiteLike}
import ch.epfl.pop.decentralized.ToyDbActor

import scala.collection.mutable.HashMap
class HbActorSuite extends TestKit(ActorSystem("HbActorSuiteActorSystem")) with FunSuiteLike with ImplicitSender with Matchers with BeforeAndAfterAll with AskPatternConstants{

  final val CHANNEL1_NAME: String = "/root/wex/lao1Id"
  final val CHANNEL2_NAME: String = "/root/wex/lao2Id"
  final val MESSAGE1_ID: Hash = Hash(Base64Data.encode("message1Id"))
  final val MESSAGE2_ID: Hash = Hash(Base64Data.encode("message2Id"))
  final val MESSAGE3_ID: Hash = Hash(Base64Data.encode("message3Id"))
  final val MESSAGE4_ID: Hash = Hash(Base64Data.encode("message4Id"))
  final val MESSAGE5_ID: Hash = Hash(Base64Data.encode("message5Id"))
  final val RECEIVED_HEART_BEAT = HashMap(CHANNEL1_NAME -> List(MESSAGE1_ID,MESSAGE2_ID,MESSAGE3_ID), CHANNEL2_NAME -> List(MESSAGE4_ID,MESSAGE5_ID))


  def sleep(duration: Long = 50): Unit = {
    Thread.sleep(duration)
  }

  test("RetrieveHeartbeat correctly retrieves the content of the dataBase"){
    val toyDbActor = system.actorOf(Props(ToyDbActor))
    val hbActor = system.actorOf(Props(HbActor(toyDbActor)))
  }








}

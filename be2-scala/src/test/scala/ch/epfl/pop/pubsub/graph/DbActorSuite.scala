package ch.epfl.pop.pubsub.graph

import java.util.concurrent.TimeUnit

import akka.actor.{ActorRef, ActorSystem, Props}
import akka.pattern.AskableActorRef
import akka.testkit.{ImplicitSender, TestActors, TestKit}
import akka.util.Timeout
import ch.epfl.pop.pubsub.{AskPatternConstants, PubSubMediator}
import org.scalatest.{BeforeAndAfterAll, FunSuiteLike, Matchers}
import akka.pattern.ask
import ch.epfl.pop.model.network.method.message.Message
import ch.epfl.pop.model.network.method.message.data.ObjectType
import ch.epfl.pop.model.network.method.message.data.dataObject._
import ch.epfl.pop.model.objects.{Base64Data, Channel, PublicKey, Signature, WitnessSignaturePair}
import util.examples.MessageExample

import scala.reflect.io.Directory
import java.io.File

import scala.concurrent.Await

class DbActorSuite() extends TestKit(ActorSystem("myTestActorSystem"))
  with FunSuiteLike
  with ImplicitSender
  with Matchers with BeforeAndAfterAll with AskPatternConstants {

  implicit val timeout: Timeout = Timeout(1, TimeUnit.SECONDS)

  final val DB_TEST_FOLDER: String = "databaseTest"
  final val DB_TEST_CHANNEL: String = "/root/testChannel"

  final val GENERATOR = scala.util.Random

  val pubSubMediatorRef: ActorRef = system.actorOf(PubSubMediator.props, "PubSubMediator")
  val dbActorRef: AskableActorRef = system.actorOf(Props(DbActor(pubSubMediatorRef, DB_TEST_FOLDER)), "DbActor")

  override def afterAll(): Unit = {
    // Stops the test actor system
    TestKit.shutdownActorSystem(system)

    // Deletes the test database
    val directory = new Directory(new File(DB_TEST_FOLDER))
    directory.deleteRecursively()
  }


  private def generateUniqueChannel: Channel = {
    Channel(s"/root/channelNumber${GENERATOR.nextInt(Int.MaxValue)}")
  }

  test("Testkit should work with FunSuite-like") {
    val echoActor: ActorRef = system.actorOf(TestActors.echoActorProps)
    val message: String = "Testkit is working!"

    echoActor ! message
    expectMsg(message)
  }

  test("DbActor creates channel"){
    val channel: Channel = generateUniqueChannel
    val ask = dbActorRef ? DbActor.CreateChannel(channel, ObjectType.LAO)
    val answer = Await.result(ask, duration)

    answer shouldBe a [DbActor.DbActorAck]
  }

  test("DbActor doesn't create same channel twice"){
    val channel: Channel = generateUniqueChannel
    val ask = dbActorRef ? DbActor.CreateChannel(channel, ObjectType.LAO)
    val answer = Await.result(ask, duration)

    answer shouldBe a [DbActor.DbActorAck]

    val ask2 = dbActorRef ? DbActor.CreateChannel(channel, ObjectType.LAO)
    val answer2 = Await.result(ask2, duration)

    answer2 shouldBe a [DbActor.DbActorNAck]
  }

  test("DbActor knows whether channel exists or not"){
    val channel: Channel = generateUniqueChannel
    val ask = dbActorRef ? DbActor.CreateChannel(channel, ObjectType.LAO)
    Await.ready(ask, duration)

    val ask2 = dbActorRef ? DbActor.ChannelExists(channel)
    val answer2 = Await.result(ask2, duration)

    answer2 shouldBe a [DbActor.DbActorAck]

    val channel3: Channel = new Channel("channel")
    val ask3 = dbActorRef ? DbActor.ChannelExists(channel3)
    val answer3 = Await.result(ask3, duration)

    answer3 shouldBe a [DbActor.DbActorNAck]
  }

  test("DbActor creates missing channels during WRITE") {
    val channel: Channel = generateUniqueChannel
    val message: Message = MessageExample.MESSAGE
    val expected: DbActor.DbActorNAck = DbActor.DbActorNAck(ErrorCodes.INVALID_RESOURCE.id, s"Channel '$channel' does not exist in db")

    var ask = dbActorRef ? DbActor.ChannelExists(channel)
    var answer = Await.result(ask, duration)

    answer shouldBe a [DbActor.DbActorNAck]
    answer.asInstanceOf[DbActor.DbActorNAck] should equal (expected)

    ask = dbActorRef ? DbActor.Write(channel, message)
    Await.result(ask, duration)

    ask = dbActorRef ? DbActor.ChannelExists(channel)
    answer = Await.result(ask, duration)

    answer shouldBe a [DbActor.DbActorAck]
  }
  
  test("DbActor fails during CATCHUP on a missing channel") {
    val expected: DbActor.DbActorNAck = DbActor.DbActorNAck(ErrorCodes.INVALID_RESOURCE.id, "Database cannot catchup from a channel that does not exist in db")

    val ask = dbActorRef ? DbActor.Catchup(generateUniqueChannel)
    val answer = Await.result(ask, duration)

    answer shouldBe a [DbActor.DbActorNAck]
    answer.asInstanceOf[DbActor.DbActorNAck] should equal (expected)
  }
  
  test("DbActor succeeds during CATCHUP on a channel with one message") {
    val message: Message = MessageExample.MESSAGE
    val channel: Channel = generateUniqueChannel

    var ask = dbActorRef ? DbActor.Write(channel, message)
    Await.ready(ask, duration)

    ask = dbActorRef ? DbActor.Catchup(channel)
    val answer = Await.result(ask, duration)

    answer shouldBe a [DbActor.DbActorCatchupAck]
    answer.asInstanceOf[DbActor.DbActorCatchupAck].messages should have size 1
    answer.asInstanceOf[DbActor.DbActorCatchupAck].messages should equal (message :: Nil)
  }

  test("DbActor succeeds during CATCHUP on a channel with multiple messages") {
    val message: Message = MessageExample.MESSAGE
    val messageFaulty: Message = MessageExample.MESSAGE_FAULTY_ID
    val channel: Channel = generateUniqueChannel

    val ask1 = dbActorRef ? DbActor.Write(channel, message)
    val ask2 = dbActorRef ? DbActor.Write(channel, messageFaulty)
    Await.ready(ask1, duration)
    Await.ready(ask2, duration)

    val ask = dbActorRef ? DbActor.Catchup(channel)
    val answer = Await.result(ask, duration)

    answer shouldBe a [DbActor.DbActorCatchupAck]
    answer.asInstanceOf[DbActor.DbActorCatchupAck].messages should have size 2
    answer.asInstanceOf[DbActor.DbActorCatchupAck].messages should contain (message)
    answer.asInstanceOf[DbActor.DbActorCatchupAck].messages should contain (messageFaulty)
  }

  test("DbActor can perform both READ and WRITE flawlessly") {
    val message: Message = MessageExample.MESSAGE
    val channel: Channel = generateUniqueChannel

    var ask = dbActorRef ? DbActor.Write(channel, message)
    var answer = Await.result(ask, duration)

    answer should not be an [DbActor.DbActorNAck]

    ask = dbActorRef ? DbActor.Read(channel, message.message_id)
    answer = Await.result(ask, duration)

    answer shouldBe a [DbActor.DbActorReadAck]
    answer.asInstanceOf[DbActor.DbActorReadAck].message should equal (Some(message))
  }
  
  test("DbActor can overwrite messages") {
    val message: Message = MessageExample.MESSAGE
    val messageModified: Message = message.addWitnessSignature(WitnessSignaturePair(PublicKey(Base64Data("myKey")), Signature(Base64Data("mySig"))))
    val channel: Channel = generateUniqueChannel

    var ask = dbActorRef ? DbActor.Write(channel, message)
    Await.ready(ask, duration)

    ask = dbActorRef ? DbActor.Write(channel, messageModified)
    Await.ready(ask, duration)

    ask = dbActorRef ? DbActor.Read(channel, message.message_id)
    val answer = Await.result(ask, duration)

    answer shouldBe a [DbActor.DbActorReadAck]
    answer.asInstanceOf[DbActor.DbActorReadAck].message should equal (Some(messageModified))
  }

  test("DbActor will fail when creating same channel twice, but then still write in the channel") {
    val channel: Channel = generateUniqueChannel
    val message: Message = MessageExample.MESSAGE

    val ask1 = dbActorRef ? DbActor.CreateChannel(channel, ObjectType.LAO)
    val answer1 = Await.result(ask1, duration)

    answer1 shouldBe a [DbActor.DbActorAck]

    val ask2 = dbActorRef ? DbActor.CreateChannel(channel, ObjectType.LAO)
    val answer2 = Await.result(ask2, duration)

    answer2 shouldBe a [DbActor.DbActorNAck]

    val ask3 = dbActorRef ? DbActor.Write(channel, message)
    val answer3 = Await.result(ask3, duration)


    val ask4 = dbActorRef ? DbActor.Read(channel, message.message_id)
    val answer4 = Await.result(ask4, duration)

    answer4 shouldBe a [DbActor.DbActorReadAck]

  }

  test("DbActor reads ChannelData"){
    val channel: Channel = generateUniqueChannel
    val ask1 = dbActorRef ? DbActor.CreateChannel(channel, ObjectType.LAO)
    Await.ready(ask1, duration)

    val ask2 = dbActorRef ? DbActor.ReadChannelData(channel)
    val answer2 = Await.result(ask2, duration)

    answer2 shouldBe a [DbActor.DbActorReadChannelDataAck]

    answer2.asInstanceOf[DbActor.DbActorReadChannelDataAck].channelData should equal(Some(ChannelData(ObjectType.LAO, List.empty)))
  }

  test("DbActor stores and reads LaoData"){
    val channel: Channel = generateUniqueChannel
    val message: Message = MessageExample.MESSAGE
    val laoData: LaoData = LaoData(PublicKey(Base64Data("a")), List.empty)

    val ask1 = dbActorRef ? DbActor.WriteLaoData(channel, message, laoData)
    val answer1 = Await.result(ask1, duration)

    answer1 shouldBe a [DbActor.DbActorWriteAck]

    val ask3 = dbActorRef ? DbActor.ReadChannelData(channel)
    val answer3 = Await.result(ask3, duration)

    answer3 shouldBe a [DbActor.DbActorReadChannelDataAck]

    answer3.asInstanceOf[DbActor.DbActorReadChannelDataAck].channelData should equal(Some(ChannelData(ObjectType.LAO, List(message.message_id))))

    val ask2 = dbActorRef ? DbActor.ReadLaoData(channel)
    val answer2 = Await.result(ask2, duration)

    answer2 shouldBe a [DbActor.DbActorReadLaoDataAck]

    answer2.asInstanceOf[DbActor.DbActorReadLaoDataAck].laoData should equal(Some(LaoData(PublicKey(Base64Data("a")), List.empty)))

  }

  test("DbActor stores and reads two distinct LaoData objects, to simulate two LAOs in the same database"){
    val channel: Channel = generateUniqueChannel
    val channel2: Channel = generateUniqueChannel
    val message: Message = MessageExample.MESSAGE
    val laoData: LaoData = LaoData(PublicKey(Base64Data("a")), List.empty)
    val laoData2: LaoData = LaoData(PublicKey(Base64Data("a")), List(PublicKey(Base64Data("b"))))

    val ask1 = dbActorRef ? DbActor.WriteLaoData(channel, message, laoData)
    val answer1 = Await.result(ask1, duration)

    answer1 shouldBe a [DbActor.DbActorWriteAck]

    val ask2 = dbActorRef ? DbActor.WriteLaoData(channel2, message, laoData2)
    val answer2 = Await.result(ask2, duration)

    answer2 shouldBe a [DbActor.DbActorWriteAck]

    val ask3 = dbActorRef ? DbActor.ReadChannelData(channel)
    val answer3 = Await.result(ask3, duration)

    answer3 shouldBe a [DbActor.DbActorReadChannelDataAck]

    answer3.asInstanceOf[DbActor.DbActorReadChannelDataAck].channelData should equal(Some(ChannelData(ObjectType.LAO, List(message.message_id))))

    val ask4 = dbActorRef ? DbActor.ReadLaoData(channel)
    val answer4 = Await.result(ask4, duration)

    answer4 shouldBe a [DbActor.DbActorReadLaoDataAck]

    answer4.asInstanceOf[DbActor.DbActorReadLaoDataAck].laoData should equal(Some(LaoData(PublicKey(Base64Data("a")), List.empty)))

    val ask5 = dbActorRef ? DbActor.ReadLaoData(channel2)
    val answer5 = Await.result(ask5, duration)

    answer5 shouldBe a [DbActor.DbActorReadLaoDataAck]

    answer5.asInstanceOf[DbActor.DbActorReadLaoDataAck].laoData should equal(Some(LaoData(PublicKey(Base64Data("a")), List(PublicKey(Base64Data("b"))))))

  }
}

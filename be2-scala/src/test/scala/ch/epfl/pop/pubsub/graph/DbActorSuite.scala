package ch.epfl.pop.pubsub.graph

import java.io.File
import java.util.concurrent.TimeUnit

import akka.actor.{ActorRef, ActorSystem, Props}
import akka.pattern.{AskableActorRef, ask}
import akka.testkit.{ImplicitSender, TestActors, TestKit}
import akka.util.Timeout
import ch.epfl.pop.model.network.method.message.Message
import ch.epfl.pop.model.network.method.message.data.ObjectType
import ch.epfl.pop.model.network.method.message.data.lao.CreateLao
import ch.epfl.pop.model.objects._
import ch.epfl.pop.pubsub.{AskPatternConstants, PubSubMediator}
import org.scalatest.{BeforeAndAfterAll, FunSuiteLike, Matchers}
import util.examples.MessageExample

import scala.concurrent.Await
import scala.util.Failure
import org.scalatest.concurrent.ScalaFutures
import scala.reflect.io.Directory

class DbActorSuite() extends TestKit(ActorSystem("myTestActorSystem"))
  with FunSuiteLike
  with ImplicitSender
  with ScalaFutures
  with Matchers with BeforeAndAfterAll with AskPatternConstants {

  implicit val timeout: Timeout = Timeout(1, TimeUnit.SECONDS)

  final val DB_TEST_FOLDER: String = "databaseTest"
  final val DB_TEST_CHANNEL: String = "/root/testChannel"

  final val TEST_CHANNEL_WRITELAO_1: String = "/root/channelNumber2056764657"
  final val TEST_CHANNEL_WRITELAO_2: String = "/root/channelNumber277501811"
  final val TEST_CHANNEL_WRITELAO_3: String = "/root/channelNumber456561164"
  final val TEST_CHANNEL_WRITELAO_4: String = "/root/channelNumber2056764656"

  final val TEST_CHANNEL_CREATEMANY_1: Channel = Channel(Channel.ROOT_CHANNEL_PREFIX + Base64Data.encode("createmany1"))
  final val TEST_CHANNEL_CREATEMANY_2: Channel = Channel(Channel.ROOT_CHANNEL_PREFIX + Base64Data.encode("createmany2"))
  final val TEST_CHANNEL_CREATEMANY_3: Channel = Channel(Channel.ROOT_CHANNEL_PREFIX + Base64Data.encode("createmany3"))
  final val TEST_CHANNEL_CREATEMANY_4: Channel = Channel(Channel.ROOT_CHANNEL_PREFIX + Base64Data.encode("createmany4"))

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

  test("DbActor creates channel") {
    val channel: Channel = generateUniqueChannel
    val ask = dbActorRef ? DbActor.CreateChannel(channel, ObjectType.LAO)
    val answer = Await.result(ask, duration)

    answer shouldBe a[DbActor.DbActorAck]
  }

  test("DbActor doesn't create same channel twice") {
    val channel: Channel = generateUniqueChannel
    val ask = dbActorRef ? DbActor.CreateChannel(channel, ObjectType.LAO)
    val answer = Await.result(ask, duration)

    answer shouldBe a[DbActor.DbActorAck]

    val ask2 = dbActorRef ? DbActor.CreateChannel(channel, ObjectType.LAO)
    ScalaFutures.whenReady(ask2.failed){
      e => e shouldBe a [DbActorNAckException]
    }
  }

  test("DbActor succeeds during CreateChannelsFromList with an empty list") {
    val ask = dbActorRef ? DbActor.CreateChannelsFromList(List.empty)
    val answer = Await.result(ask, duration)
    answer shouldBe a[DbActor.DbActorAck]
  }

  test("DbActor succeeds during CreateChannelsFromList with a singleton list") {
    val ask = dbActorRef ? DbActor.CreateChannelsFromList(List((TEST_CHANNEL_CREATEMANY_1, ObjectType.LAO)))
    val answer = Await.result(ask, duration)
    answer shouldBe a[DbActor.DbActorAck]
  }

  test("DbActor succeeds during CreateChannelsFromList with more than one element in list") {
    val ask = dbActorRef ? DbActor.CreateChannelsFromList(List((TEST_CHANNEL_CREATEMANY_2, ObjectType.LAO), (TEST_CHANNEL_CREATEMANY_3, ObjectType.LAO)))
    val answer = Await.result(ask, duration)
    answer shouldBe a[DbActor.DbActorAck]
  }

  test("DbActor fails during CreateChannelsFromList when trying to create twice the same channel (namewise) from a list") {
    val ask = dbActorRef ? DbActor.CreateChannelsFromList(List((TEST_CHANNEL_CREATEMANY_4, ObjectType.LAO), (TEST_CHANNEL_CREATEMANY_4, ObjectType.LAO)))
    ScalaFutures.whenReady(ask.failed){
      e => e shouldBe a [DbActorNAckException]
    }
  }

  test("DbActor knows whether channel exists or not") {
    val channel: Channel = generateUniqueChannel
    val ask = dbActorRef ? DbActor.CreateChannel(channel, ObjectType.LAO)
    Await.ready(ask, duration)

    val ask2 = dbActorRef ? DbActor.ChannelExists(channel)
    val answer2 = Await.result(ask2, duration)

    answer2 shouldBe a[DbActor.DbActorAck]

    val channel3: Channel = new Channel("channel")
    val ask3 = dbActorRef ? DbActor.ChannelExists(channel3)

    ScalaFutures.whenReady(ask3.failed){
      e => e shouldBe a [DbActorNAckException]
    }
  }

  test("DbActor creates missing channels during WRITE") {
    val channel: Channel = generateUniqueChannel
    val message: Message = MessageExample.MESSAGE
    val expected: DbActorNAckException = new DbActorNAckException(ErrorCodes.INVALID_RESOURCE.id, s"Channel '$channel' does not exist in db")

    var ask = dbActorRef ? DbActor.ChannelExists(channel)

    ScalaFutures.whenReady(ask.failed){
      e => 
        e shouldBe a [DbActorNAckException]
        e.asInstanceOf[DbActorNAckException] should equal (expected)
    }

    ask = dbActorRef ? DbActor.Write(channel, message)
    Await.result(ask, duration)

    ask = dbActorRef ? DbActor.ChannelExists(channel)
    val answer = Await.result(ask, duration)

    answer shouldBe a[DbActor.DbActorAck]
  }

  test("DbActor fails during CATCHUP on a missing channel") {
    val channel: Channel = generateUniqueChannel
    val expected: DbActorNAckException = new DbActorNAckException(ErrorCodes.INVALID_RESOURCE.id, s"Database cannot catchup from a channel $channel that does not exist in db")

    val ask = dbActorRef ? DbActor.Catchup(channel)

    ScalaFutures.whenReady(ask.failed){
      e => 
        e shouldBe a [DbActorNAckException]
        e.asInstanceOf[DbActorNAckException] should equal (expected)
    }
  }

  test("DbActor succeeds during CATCHUP on a channel with one message") {
    val message: Message = MessageExample.MESSAGE
    val channel: Channel = generateUniqueChannel

    var ask = dbActorRef ? DbActor.Write(channel, message)
    Await.ready(ask, duration)

    ask = dbActorRef ? DbActor.Catchup(channel)
    val answer = Await.result(ask, duration)

    answer shouldBe a[DbActor.DbActorCatchupAck]
    answer.asInstanceOf[DbActor.DbActorCatchupAck].messages should have size 1
    answer.asInstanceOf[DbActor.DbActorCatchupAck].messages should equal(message :: Nil)
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

    answer shouldBe a[DbActor.DbActorCatchupAck]
    answer.asInstanceOf[DbActor.DbActorCatchupAck].messages should have size 2
    answer.asInstanceOf[DbActor.DbActorCatchupAck].messages should contain(message)
    answer.asInstanceOf[DbActor.DbActorCatchupAck].messages should contain(messageFaulty)
  }

  test("DbActor can perform both READ and WRITE flawlessly") {
    val message: Message = MessageExample.MESSAGE
    val channel: Channel = generateUniqueChannel

    var ask = dbActorRef ? DbActor.Write(channel, message)
    var answer = Await.result(ask, duration)

    answer should not be an [Failure[DbActorNAckException]]

    ask = dbActorRef ? DbActor.Read(channel, message.message_id)
    answer = Await.result(ask, duration)

    answer shouldBe a[DbActor.DbActorReadAck]
    answer.asInstanceOf[DbActor.DbActorReadAck].message should equal(Some(message))
  }

  test("DbActor can overwrite messages") {
    val message: Message = MessageExample.MESSAGE
    val messageModified: Message = message.addWitnessSignature(WitnessSignaturePair(PublicKey(Base64Data.encode("myKey")), Signature(Base64Data.encode("mySig"))))
    val channel: Channel = generateUniqueChannel

    var ask = dbActorRef ? DbActor.Write(channel, message)
    Await.ready(ask, duration)

    ask = dbActorRef ? DbActor.Write(channel, messageModified)
    Await.ready(ask, duration)

    ask = dbActorRef ? DbActor.Read(channel, message.message_id)
    val answer = Await.result(ask, duration)

    answer shouldBe a[DbActor.DbActorReadAck]
    answer.asInstanceOf[DbActor.DbActorReadAck].message should equal(Some(messageModified))
  }

  test("DbActor will fail when creating same channel twice, but then still write in the channel") {
    val channel: Channel = generateUniqueChannel
    val message: Message = MessageExample.MESSAGE

    val ask1 = dbActorRef ? DbActor.CreateChannel(channel, ObjectType.LAO)
    val answer1 = Await.result(ask1, duration)

    answer1 shouldBe a[DbActor.DbActorAck]

    val ask2 = dbActorRef ? DbActor.CreateChannel(channel, ObjectType.LAO)

    ScalaFutures.whenReady(ask2.failed){
      e => e shouldBe a [DbActorNAckException]
    }

    val ask3 = dbActorRef ? DbActor.Write(channel, message)
    val answer3 = Await.result(ask3, duration)

    answer3 shouldBe a[DbActor.DbActorWriteAck]

    val ask4 = dbActorRef ? DbActor.Read(channel, message.message_id)
    val answer4 = Await.result(ask4, duration)

    answer4 shouldBe a[DbActor.DbActorReadAck]

  }

  test("DbActor reads ChannelData") {
    val channel: Channel = generateUniqueChannel
    val ask1 = dbActorRef ? DbActor.CreateChannel(channel, ObjectType.LAO)
    Await.ready(ask1, duration)

    val ask2 = dbActorRef ? DbActor.ReadChannelData(channel)
    val answer2 = Await.result(ask2, duration)

    answer2 shouldBe a[DbActor.DbActorReadChannelDataAck]

    answer2.asInstanceOf[DbActor.DbActorReadChannelDataAck].channelData should equal(Some(ChannelData(ObjectType.LAO, List.empty)))
  }

  test("DbActor stores and reads LaoData at creation") {
    val channel: Channel = Channel(TEST_CHANNEL_WRITELAO_1)
    val message: Message = MessageExample.MESSAGE_CREATELAO_SIMPLIFIED
    message.decodedData.get.isInstanceOf[CreateLao] should equal(true)

    val ask1 = dbActorRef ? DbActor.Write(channel, message)
    val answer1 = Await.result(ask1, duration)

    answer1 shouldBe a[DbActor.DbActorWriteAck]

    val ask3 = dbActorRef ? DbActor.ReadChannelData(channel)
    val answer3 = Await.result(ask3, duration)

    answer3 shouldBe a[DbActor.DbActorReadChannelDataAck]

    answer3.asInstanceOf[DbActor.DbActorReadChannelDataAck].channelData should equal(Some(ChannelData(ObjectType.LAO, List(message.message_id))))

    val ask2 = dbActorRef ? DbActor.ReadLaoData(channel)
    val answer2 = Await.result(ask2, duration)

    answer2 shouldBe a[DbActor.DbActorReadLaoDataAck]

    answer2.asInstanceOf[DbActor.DbActorReadLaoDataAck].laoData.get.owner should equal(PublicKey(Base64Data.encode("key")))
    answer2.asInstanceOf[DbActor.DbActorReadLaoDataAck].laoData.get.attendees should equal(List(PublicKey(Base64Data.encode("key"))))
    answer2.asInstanceOf[DbActor.DbActorReadLaoDataAck].laoData.get.witnesses should equal(List.empty)
  }

  test("DbActor stores and reads LaoData at creation and at roll call closing") {
    val channel: Channel = Channel(TEST_CHANNEL_WRITELAO_2)
    val messageLao: Message = MessageExample.MESSAGE_CREATELAO_SIMPLIFIED
    val messageRollCall: Message = MessageExample.MESSAGE_CLOSEROLLCALL

    val ask1 = dbActorRef ? DbActor.Write(channel, messageLao)
    val answer1 = Await.result(ask1, duration)

    answer1 shouldBe a[DbActor.DbActorWriteAck]

    val ask2 = dbActorRef ? DbActor.ReadLaoData(channel)
    val answer2 = Await.result(ask2, duration)

    answer2 shouldBe a[DbActor.DbActorReadLaoDataAck]

    answer2.asInstanceOf[DbActor.DbActorReadLaoDataAck].laoData.get.owner should equal(PublicKey(Base64Data.encode("key")))
    answer2.asInstanceOf[DbActor.DbActorReadLaoDataAck].laoData.get.attendees should equal(List(PublicKey(Base64Data.encode("key"))))
    answer2.asInstanceOf[DbActor.DbActorReadLaoDataAck].laoData.get.witnesses should equal(List.empty)

    val ask3 = dbActorRef ? DbActor.Write(channel, messageRollCall)
    val answer3 = Await.result(ask3, duration)

    answer3 shouldBe a[DbActor.DbActorWriteAck]

    val ask4 = dbActorRef ? DbActor.ReadLaoData(channel)
    val answer4 = Await.result(ask4, duration)

    answer4 shouldBe a[DbActor.DbActorReadLaoDataAck]

    answer4.asInstanceOf[DbActor.DbActorReadLaoDataAck].laoData.get.owner should equal(PublicKey(Base64Data.encode("key")))
    answer4.asInstanceOf[DbActor.DbActorReadLaoDataAck].laoData.get.attendees should equal(List(PublicKey(Base64Data.encode("keyAttendee"))))
    answer4.asInstanceOf[DbActor.DbActorReadLaoDataAck].laoData.get.witnesses should equal(List.empty)
  }

  test("DbActor stores and reads two distinct LaoData objects, to simulate two LAOs in the same database") {
    val channel: Channel = Channel(TEST_CHANNEL_WRITELAO_4)
    val channel2: Channel = Channel(TEST_CHANNEL_WRITELAO_3)
    val message: Message = MessageExample.MESSAGE_CREATELAO_SIMPLIFIED
    val message2: Message = MessageExample.MESSAGE_CREATELAO2

    val ask1 = dbActorRef ? DbActor.Write(channel, message)
    val answer1 = Await.result(ask1, duration)

    answer1 shouldBe a[DbActor.DbActorWriteAck]

    val ask2 = dbActorRef ? DbActor.Write(channel2, message2)
    val answer2 = Await.result(ask2, duration)

    answer2 shouldBe a[DbActor.DbActorWriteAck]

    val ask3 = dbActorRef ? DbActor.ReadChannelData(channel)
    val answer3 = Await.result(ask3, duration)

    answer3 shouldBe a[DbActor.DbActorReadChannelDataAck]

    answer3.asInstanceOf[DbActor.DbActorReadChannelDataAck].channelData should equal(Some(ChannelData(ObjectType.LAO, List(message.message_id))))

    val ask4 = dbActorRef ? DbActor.ReadLaoData(channel)
    val answer4 = Await.result(ask4, duration)

    answer4 shouldBe a[DbActor.DbActorReadLaoDataAck]

    answer4.asInstanceOf[DbActor.DbActorReadLaoDataAck].laoData.get.owner should equal(PublicKey(Base64Data.encode("key")))
    answer4.asInstanceOf[DbActor.DbActorReadLaoDataAck].laoData.get.attendees should equal(List(PublicKey(Base64Data.encode("key"))))
    answer4.asInstanceOf[DbActor.DbActorReadLaoDataAck].laoData.get.witnesses should equal(List.empty)

    val ask5 = dbActorRef ? DbActor.ReadLaoData(channel2)
    val answer5 = Await.result(ask5, duration)

    answer5 shouldBe a[DbActor.DbActorReadLaoDataAck]

    answer5.asInstanceOf[DbActor.DbActorReadLaoDataAck].laoData.get.owner should equal(PublicKey(Base64Data.encode("key2")))
    answer5.asInstanceOf[DbActor.DbActorReadLaoDataAck].laoData.get.attendees should equal(List(PublicKey(Base64Data.encode("key2"))))
    answer5.asInstanceOf[DbActor.DbActorReadLaoDataAck].laoData.get.witnesses should equal(List.empty)

  }

  test("DbActor fails on non-existant ChannelData read"){
    val ask = dbActorRef ? DbActor.ReadChannelData(Channel(Channel.ROOT_CHANNEL_PREFIX + Base64Data.encode("wrong").data))
    ScalaFutures.whenReady(ask.failed){
      e => e shouldBe a [DbActorNAckException]
    }
  }

  test("DbActor fails on non-existant LaoData read"){
    val ask = dbActorRef ? DbActor.ReadLaoData(Channel(Channel.ROOT_CHANNEL_PREFIX + Base64Data.encode("wrong").data))
    ScalaFutures.whenReady(ask.failed){
      e => e shouldBe a [DbActorNAckException]
    }
  }
}

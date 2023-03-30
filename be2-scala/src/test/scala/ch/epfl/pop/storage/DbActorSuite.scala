package ch.epfl.pop.storage

import akka.actor.{ActorRef, ActorSystem, Props}
import akka.pattern.AskableActorRef
import akka.testkit.{ImplicitSender, TestKit}
import ch.epfl.pop.model.network.method.message.Message
import ch.epfl.pop.model.network.method.message.data.ActionType.{ActionType, CREATE}
import ch.epfl.pop.model.network.method.message.data.{ActionType, ObjectType}
import ch.epfl.pop.model.objects.Channel.ROOT_CHANNEL_PREFIX
import ch.epfl.pop.model.objects._
import ch.epfl.pop.pubsub.{AskPatternConstants, MessageRegistry, PubSubMediator}
import org.scalatest.concurrent.ScalaFutures
import org.scalatest.BeforeAndAfterAll
import org.scalatest.funsuite.{AnyFunSuiteLike => FunSuiteLike}
import org.scalatest.matchers.should.Matchers
import util.examples.MessageExample
import util.examples.RollCall.{CreateRollCallExamples, OpenRollCallExamples}

import scala.concurrent.Await

class DbActorSuite extends TestKit(ActorSystem("DbActorSuiteActorSystem")) with FunSuiteLike with ImplicitSender with Matchers with ScalaFutures with BeforeAndAfterAll with AskPatternConstants {

  final val mediatorRef: ActorRef = system.actorOf(PubSubMediator.props)

  final val CHANNEL_NAME: String = "/root/wex"
  final val MESSAGE: Message = MessageExample.MESSAGE_CREATELAO_WORKING
  val LAO_ID: Hash = Hash(Base64Data.encode("laoId"))
  val ELECTION_ID: Hash = Hash(Base64Data.encode("electionId"))
  val ELECTION_NAME: String = s"${ROOT_CHANNEL_PREFIX}${LAO_ID.toString}/private/${ELECTION_ID.toString}"
  val KEYPAIR: KeyPair = KeyPair()

  override def afterAll(): Unit = {
    // Stops the test actor system
    TestKit.shutdownActorSystem(system)
  }

  def sleep(duration: Long = 50): Unit = {
    Thread.sleep(duration)
  }

  test("write can WRITE in an existing channel") {
    val storage: InMemoryStorage = InMemoryStorage()
    val dbActor: ActorRef = system.actorOf(Props(DbActor(mediatorRef, MessageRegistry(), storage)))

    storage.size should equal(0)

    dbActor ! DbActor.CreateChannel(Channel(CHANNEL_NAME), ObjectType.LAO);
    sleep()

    expectMsg(DbActor.DbActorAck())
    storage.size should equal(1)

    // writing to the previously created channel
    dbActor ! DbActor.Write(Channel(CHANNEL_NAME), MESSAGE);
    sleep()

    expectMsg(DbActor.DbActorAck())
    storage.size should equal(2)
    storage.elements(CHANNEL_NAME) should equal(ChannelData(ObjectType.LAO, MESSAGE.message_id :: Nil).toJsonString)
    storage.elements(s"$CHANNEL_NAME${Channel.DATA_SEPARATOR}${MESSAGE.message_id}") should equal(MESSAGE.toJsonString)
  }

  test("write can WRITE in a non-existing channel") {
    val storage: InMemoryStorage = InMemoryStorage()
    val dbActor: ActorRef = system.actorOf(Props(DbActor(mediatorRef, MessageRegistry(), storage)))

    storage.size should equal(0)

    // writing to non-(yet-)existing channel
    dbActor ! DbActor.Write(Channel(CHANNEL_NAME), MESSAGE);
    sleep()

    expectMsg(DbActor.DbActorAck())
    storage.size should equal(2)
    storage.elements(CHANNEL_NAME) should equal(ChannelData(ObjectType.LAO, MESSAGE.message_id :: Nil).toJsonString)
    storage.elements(s"$CHANNEL_NAME${Channel.DATA_SEPARATOR}${MESSAGE.message_id}") should equal(MESSAGE.toJsonString)
  }

  test("write behaves normally for multiple WRITE requests") {
    val storage: InMemoryStorage = InMemoryStorage()
    val dbActor: ActorRef = system.actorOf(Props(DbActor(mediatorRef, MessageRegistry(), storage)))

    val channelName1 = CHANNEL_NAME
    val channelName2 = s"${CHANNEL_NAME}2"

    val message1 = MESSAGE
    val message2 = MESSAGE.copy(message_id = Hash(Base64Data("RmFrZSBtZXNzYWdlX2lkIDopIE5vIGVhc3RlciBlZ2cgcmlnaHQgdGhlcmUhIC0tIE5pY29sYXMgUmF1bGlu")))

    storage.size should equal(0)

    // adding a channel (channel 1)
    dbActor ! DbActor.CreateChannel(Channel(channelName1), ObjectType.LAO);
    sleep()

    expectMsg(DbActor.DbActorAck())
    storage.size should equal(1) // ChannelData for channel 1

    // ------------------------- 1st WRITE REQUEST ------------------------- //

    // writing to the existing channel 1
    dbActor ! DbActor.Write(Channel(channelName1), message1);
    sleep()

    expectMsg(DbActor.DbActorAck())
    storage.size should equal(2) // ChannelData for channel 1 + payload for message 1
    storage.elements(channelName1) should equal(ChannelData(ObjectType.LAO, message1.message_id :: Nil).toJsonString)
    storage.elements(s"$channelName1${Channel.DATA_SEPARATOR}${message1.message_id}") should equal(message1.toJsonString)

    // ------------------------- 2nd WRITE REQUEST ------------------------- //

    // writing to the non-(yet-)existing channel 2
    dbActor ! DbActor.Write(Channel(channelName2), message2);
    sleep()

    expectMsg(DbActor.DbActorAck())
    storage.size should equal(4) // ChannelData for channel 1 & 2 + payload for message 1 & 2
    storage.elements(channelName1) should equal(ChannelData(ObjectType.LAO, message1.message_id :: Nil).toJsonString)
    storage.elements(s"$channelName1${Channel.DATA_SEPARATOR}${message1.message_id}") should equal(message1.toJsonString)
    storage.elements(channelName2) should equal(ChannelData(ObjectType.LAO, message2.message_id :: Nil).toJsonString)
    storage.elements(s"$channelName2${Channel.DATA_SEPARATOR}${message2.message_id}") should equal(message2.toJsonString)

    // ------------------------- 3rd WRITE REQUEST ------------------------- //

    // writing (a new message) to the existing channel 1
    dbActor ! DbActor.Write(Channel(channelName1), message2);
    sleep()

    expectMsg(DbActor.DbActorAck())
    storage.size should equal(5) // ChannelData for channel 1 & 2 + payload for message 1 & 2 (on channel 1) & 2 (on channel 2)
    storage.elements(channelName1) should equal(ChannelData(ObjectType.LAO, message2.message_id :: message1.message_id :: Nil).toJsonString)
    storage.elements(s"$channelName1${Channel.DATA_SEPARATOR}${message1.message_id}") should equal(message1.toJsonString)
    storage.elements(s"$channelName1${Channel.DATA_SEPARATOR}${message2.message_id}") should equal(message2.toJsonString)
    storage.elements(channelName2) should equal(ChannelData(ObjectType.LAO, message2.message_id :: Nil).toJsonString)
    storage.elements(s"$channelName2${Channel.DATA_SEPARATOR}${message2.message_id}") should equal(message2.toJsonString)
  }

  test("createChannel effectively creates a new channel") {
    val storage: InMemoryStorage = InMemoryStorage()
    val dbActor: ActorRef = system.actorOf(Props(DbActor(mediatorRef, MessageRegistry(), storage)), "r")

    storage.size should equal(0)

    dbActor ! DbActor.CreateChannel(Channel(CHANNEL_NAME), ObjectType.LAO);
    sleep()

    expectMsg(DbActor.DbActorAck())
    storage.size should equal(1)
    storage.elements(CHANNEL_NAME) should equal(ChannelData(ObjectType.LAO, Nil).toJsonString)
  }

  test("createChannel does not overwrite channels on duplicates") {
    val storage: InMemoryStorage = InMemoryStorage()
    val dbActor: ActorRef = system.actorOf(Props(DbActor(mediatorRef, MessageRegistry(), storage)))

    storage.size should equal(0)

    dbActor ! DbActor.CreateChannel(Channel(CHANNEL_NAME), ObjectType.LAO);
    sleep()

    expectMsg(DbActor.DbActorAck())
    storage.size should equal(1)
    storage.elements(CHANNEL_NAME) should equal(ChannelData(ObjectType.LAO, Nil).toJsonString)

    dbActor ! DbActor.CreateChannel(Channel(CHANNEL_NAME), ObjectType.LAO);
    sleep()

    storage.size should equal(1)
    storage.elements(CHANNEL_NAME) should equal(ChannelData(ObjectType.LAO, Nil).toJsonString)
  }

  test("createElectionData effectively creates a new channel for the electionData") {
    val storage: InMemoryStorage = InMemoryStorage()
    val dbActor: ActorRef = system.actorOf(Props(DbActor(mediatorRef, MessageRegistry(), storage)))

    storage.size should equal(0)

    dbActor ! DbActor.CreateElectionData(LAO_ID, ELECTION_ID, KEYPAIR);
    sleep()

    expectMsg(DbActor.DbActorAck())
    storage.size should equal(1)
    storage.elements(ELECTION_NAME) should equal(ElectionData(ELECTION_ID, KEYPAIR).toJsonString)
  }

  test("createElectionData does not overwrite channels on duplicates") {
    val storage: InMemoryStorage = InMemoryStorage()
    val dbActor: ActorRef = system.actorOf(Props(DbActor(mediatorRef, MessageRegistry(), storage)))

    storage.size should equal(0)

    dbActor ! DbActor.CreateElectionData(LAO_ID, ELECTION_ID, KEYPAIR);
    sleep()

    expectMsg(DbActor.DbActorAck())
    storage.size should equal(1)
    storage.elements(ELECTION_NAME) should equal(ElectionData(ELECTION_ID, KEYPAIR).toJsonString)

    dbActor ! DbActor.CreateElectionData(LAO_ID, ELECTION_ID, KEYPAIR);
    sleep()

    storage.size should equal(1)
    storage.elements(ELECTION_NAME) should equal(ElectionData(ELECTION_ID, KEYPAIR).toJsonString)
  }

  test("createChannelsFromList creates multiple channels") {
    val storage: InMemoryStorage = InMemoryStorage()
    val dbActor: ActorRef = system.actorOf(Props(DbActor(mediatorRef, MessageRegistry(), storage)))

    storage.size should equal(0)

    dbActor ! DbActor.CreateChannelsFromList((Channel(CHANNEL_NAME), ObjectType.ELECTION) :: (Channel(s"${CHANNEL_NAME}2"), ObjectType.ROLL_CALL) :: Nil);
    sleep()

    expectMsg(DbActor.DbActorAck())
    storage.size should equal(2)
    storage.elements(CHANNEL_NAME) should equal(ChannelData(ObjectType.ELECTION, Nil).toJsonString)
    storage.elements(s"${CHANNEL_NAME}2") should equal(ChannelData(ObjectType.ROLL_CALL, Nil).toJsonString)
  }

  test("createChannelsFromList does not overwrite channels on duplicates (duplicates already created)") {
    val storage: InMemoryStorage = InMemoryStorage()
    val dbActor: ActorRef = system.actorOf(Props(DbActor(mediatorRef, MessageRegistry(), storage)))

    storage.size should equal(0)

    dbActor ! DbActor.CreateChannel(Channel(CHANNEL_NAME), ObjectType.LAO);
    sleep()

    expectMsg(DbActor.DbActorAck())
    storage.size should equal(1)

    // adding 2 new channels (one of which already exists)
    dbActor ! DbActor.CreateChannelsFromList((Channel(CHANNEL_NAME), ObjectType.ELECTION) :: (Channel(s"${CHANNEL_NAME}2"), ObjectType.ROLL_CALL) :: Nil);
    sleep()

    expectMsg(DbActor.DbActorAck())
    storage.size should equal(2)
    storage.elements(CHANNEL_NAME) should equal(ChannelData(ObjectType.LAO, Nil).toJsonString)
    storage.elements(s"${CHANNEL_NAME}2") should equal(ChannelData(ObjectType.ROLL_CALL, Nil).toJsonString)
  }

  test("createChannelsFromList does not overwrite channels on duplicates (duplicates in the list)") {
    val storage: InMemoryStorage = InMemoryStorage()
    val dbActor: ActorRef = system.actorOf(Props(DbActor(mediatorRef, MessageRegistry(), storage)))

    storage.size should equal(0)

    // adding 2 new channels (same elements)
    dbActor ! DbActor.CreateChannelsFromList((Channel(CHANNEL_NAME), ObjectType.LAO) :: (Channel(CHANNEL_NAME), ObjectType.LAO) :: Nil);
    sleep()

    expectMsg(DbActor.DbActorAck())
    storage.size should equal(1)
    storage.elements(CHANNEL_NAME) should equal(ChannelData(ObjectType.LAO, Nil).toJsonString)
  }

  test("createChannelsFromList works for 0 or 1 element") {
    val storage: InMemoryStorage = InMemoryStorage()
    val dbActor: ActorRef = system.actorOf(Props(DbActor(mediatorRef, MessageRegistry(), storage)))

    storage.size should equal(0)

    // adding no channel
    dbActor ! DbActor.CreateChannelsFromList(Nil);
    sleep()

    expectMsg(DbActor.DbActorAck())
    storage.size should equal(0)

    dbActor ! DbActor.CreateChannelsFromList((Channel(CHANNEL_NAME), ObjectType.LAO) :: Nil);
    sleep()

    expectMsg(DbActor.DbActorAck())
    storage.size should equal(1)
    storage.elements(CHANNEL_NAME) should equal(ChannelData(ObjectType.LAO, Nil).toJsonString)
  }

  test("checkChannelExistence does not detect a non-existing channel") {
    val storage: InMemoryStorage = InMemoryStorage()
    val dbActor: AskableActorRef = system.actorOf(Props(DbActor(mediatorRef, MessageRegistry(), storage)))

    storage.size should equal(0)

    // checking the existence of a non-existing channel
    val ask = dbActor ? DbActor.ChannelExists(Channel(CHANNEL_NAME))

    ScalaFutures.whenReady(ask.failed) {
      e =>
        e shouldBe a[DbActorNAckException]
    }

    storage.size should equal(0)
  }

  test("checkChannelExistence succeeds on the detection of an existing channel") {
    val storage: InMemoryStorage = InMemoryStorage()
    val dbActor: ActorRef = system.actorOf(Props(DbActor(mediatorRef, MessageRegistry(), storage)))

    storage.size should equal(0)

    dbActor ! DbActor.CreateChannel(Channel(CHANNEL_NAME), ObjectType.LAO);
    sleep()

    storage.size should equal(1)

    // checking the existence of an existing channel
    dbActor ! DbActor.ChannelExists(Channel(CHANNEL_NAME));
    sleep()

    expectMsg(DbActor.DbActorAck())
    storage.size should equal(1)
    storage.elements(CHANNEL_NAME) should equal(ChannelData(ObjectType.LAO, Nil).toJsonString)
  }

  test("writeLaoData succeeds for both new and updated data") {
    // arrange
    val storage: InMemoryStorage = InMemoryStorage()
    val dbActor: ActorRef = system.actorOf(Props(DbActor(mediatorRef, MessageRegistry(), storage)))

    val messageLao: Message = MessageExample.MESSAGE_CREATELAO_SIMPLIFIED
    val address: Option[String] = Option("ws://popdemo.dedis.ch")

    storage.size should equal(0)

    // act
    dbActor ! DbActor.WriteLaoData(Channel(CHANNEL_NAME), messageLao, address);
    sleep()

    // assert
    expectMsg(DbActor.DbActorAck())
    storage.size should equal(1)

    val actualLaoData1: LaoData = LaoData.buildFromJson(storage.elements(s"$CHANNEL_NAME${Channel.DATA_SEPARATOR}laodata"))

    actualLaoData1.owner should equal(PublicKey(Base64Data.encode("key")))
    actualLaoData1.attendees should equal(List(PublicKey(Base64Data.encode("key"))))
    actualLaoData1.witnesses should equal(List.empty)
  }

  test("writeLaoData succeeds for updated data") {
    // arrange
    val messageRollCall: Message = MessageExample.MESSAGE_CLOSEROLLCALL
    val messageLao: Message = MessageExample.MESSAGE_CREATELAO_SIMPLIFIED
    val address: Option[String] = Option("ws://popdemo.dedis.ch")
    val laoData: LaoData = LaoData().updateWith(messageLao, address)
    val laoDataKey: String = s"$CHANNEL_NAME${Channel.LAO_DATA_LOCATION}"
    val initialStorage: InMemoryStorage = InMemoryStorage()
    initialStorage.write((laoDataKey, laoData.toJsonString))
    val dbActor: ActorRef = system.actorOf(Props(DbActor(mediatorRef, MessageRegistry(), initialStorage)))

    // act
    dbActor ! DbActor.WriteLaoData(Channel(CHANNEL_NAME), messageRollCall, address);
    sleep()

    // assert
    expectMsg(DbActor.DbActorAck())
    initialStorage.size should equal(1)

    val actualLaoData2: LaoData = LaoData.buildFromJson(initialStorage.elements(s"$CHANNEL_NAME${Channel.DATA_SEPARATOR}laodata"))

    actualLaoData2.owner should equal(PublicKey(Base64Data.encode("key")))
    actualLaoData2.attendees should equal(List(PublicKey(Base64Data.encode("key")), PublicKey(Base64Data.encode("keyAttendee"))))
    actualLaoData2.witnesses should equal(List.empty)
  }

  test("readLaoData succeeds for existing LaoData") {
    // arrange
    val channelName1: Channel = Channel(CHANNEL_NAME)
    val publicKey: PublicKey = PublicKey(Base64Data("jsNj23IHALvppqV1xQfP71_3IyAHzivxiCz236_zzQc="))
    val privateKey: PrivateKey = PrivateKey(Base64Data("qRfms3wzSLkxAeBz6UtwA-L1qP0h8D9XI1FSvY68t7Y="))
    val laoData: LaoData = LaoData(PublicKey(Base64Data.encode("key")), List(PublicKey(Base64Data.encode("key"))), privateKey, publicKey, List.empty)
    val laoDataKey: String = s"$CHANNEL_NAME${Channel.LAO_DATA_LOCATION}"
    val initialStorage: InMemoryStorage = InMemoryStorage()
    initialStorage.write((laoDataKey, laoData.toJsonString))
    val dbActor: AskableActorRef = system.actorOf(Props(DbActor(mediatorRef, MessageRegistry(), initialStorage)))

    // act
    val ask = dbActor ? DbActor.ReadLaoData(channelName1)
    val answer = Await.result(ask, duration)

    // assert
    answer shouldBe a[DbActor.DbActorReadLaoDataAck]

    val readLaoData: LaoData = answer.asInstanceOf[DbActor.DbActorReadLaoDataAck].laoData

    readLaoData.owner should equal(PublicKey(Base64Data.encode("key")))
    readLaoData.attendees should equal(List(PublicKey(Base64Data.encode("key"))))
    readLaoData.witnesses should equal(List.empty)
  }

  test("read succeeds for existing message") {
    // arrange
    val channelName1: Channel = Channel(CHANNEL_NAME)
    val initialStorage: InMemoryStorage = InMemoryStorage()
    initialStorage.write((s"$CHANNEL_NAME${Channel.DATA_SEPARATOR}${MESSAGE.message_id}", MESSAGE.toJsonString))
    val dbActor: AskableActorRef = system.actorOf(Props(DbActor(mediatorRef, MessageRegistry(), initialStorage)))

    // act
    val ask = dbActor ? DbActor.Read(channelName1, MESSAGE.message_id)
    val answer = Await.result(ask, duration)

    // assert
    answer shouldBe a[DbActor.DbActorReadAck]

    val readMessage: Message = answer.asInstanceOf[DbActor.DbActorReadAck].message.get

    readMessage should equal(MESSAGE)
  }

  test("read does not fail for non-existing message (returns None)") {
    // arrange
    val channelName1: Channel = Channel(CHANNEL_NAME)
    val initialStorage: InMemoryStorage = InMemoryStorage()
    val dbActor: AskableActorRef = system.actorOf(Props(DbActor(mediatorRef, MessageRegistry(), initialStorage)))

    // act
    val ask = dbActor ? DbActor.Read(channelName1, MESSAGE.message_id)
    val answer = Await.result(ask, duration)

    // assert
    answer shouldBe a[DbActor.DbActorReadAck]
    answer.asInstanceOf[DbActor.DbActorReadAck].message should equal(None)
  }

  test("readChannelData succeeds for existing ChannelData") {
    // arrange
    val channelName1: Channel = Channel(CHANNEL_NAME)
    val initialStorage: InMemoryStorage = InMemoryStorage()
    val channelData: ChannelData = ChannelData(ObjectType.LAO, Nil)
    initialStorage.write((CHANNEL_NAME, channelData.toJsonString))
    val dbActor: AskableActorRef = system.actorOf(Props(DbActor(mediatorRef, MessageRegistry(), initialStorage)))

    // act
    val ask = dbActor ? DbActor.ReadChannelData(channelName1)
    val answer = Await.result(ask, duration)

    // assert
    answer shouldBe a[DbActor.DbActorReadChannelDataAck]

    val readChannelData: ChannelData = answer.asInstanceOf[DbActor.DbActorReadChannelDataAck].channelData

    readChannelData should equal(channelData)
  }

  test("readElectionData succeeds for existing ElectionData") {
    // arrange
    val initialStorage: InMemoryStorage = InMemoryStorage()
    val electionData: ElectionData = ElectionData(ELECTION_ID, KEYPAIR)
    initialStorage.write((ELECTION_NAME, electionData.toJsonString))
    val dbActor: AskableActorRef = system.actorOf(Props(DbActor(mediatorRef, MessageRegistry(), initialStorage)))

    // act
    val ask = dbActor ? DbActor.ReadElectionData(LAO_ID, ELECTION_ID)
    val answer = Await.result(ask, duration)

    // assert
    answer shouldBe a[DbActor.DbActorReadElectionDataAck]

    val readElectionData: ElectionData = answer.asInstanceOf[DbActor.DbActorReadElectionDataAck].electionData

    readElectionData should equal(electionData)
  }

  test("catchup works on a channel with valid ChannelData and messages") {
    // arrange
    val initialStorage: InMemoryStorage = InMemoryStorage()
    val message2 = MESSAGE.copy(message_id = Hash(Base64Data("RmFrZSBtZXNzYWdlX2lkIDopIE5vIGVhc3RlciBlZ2cgcmlnaHQgdGhlcmUhIC0tIE5pY29sYXMgUmF1bGlu")))
    val listIds: List[Hash] = MESSAGE.message_id :: message2.message_id :: Nil
    val channelData: ChannelData = ChannelData(ObjectType.LAO, listIds)
    initialStorage.write(
      (CHANNEL_NAME, channelData.toJsonString),
      (s"$CHANNEL_NAME${Channel.DATA_SEPARATOR}${MESSAGE.message_id}", MESSAGE.toJsonString),
      (s"$CHANNEL_NAME${Channel.DATA_SEPARATOR}${message2.message_id}", message2.toJsonString)
    )
    val dbActor: AskableActorRef = system.actorOf(Props(DbActor(mediatorRef, MessageRegistry(), initialStorage)))

    // act
    val ask = dbActor ? DbActor.Catchup(Channel(CHANNEL_NAME))
    val answer = Await.result(ask, duration)

    // assert
    answer shouldBe a[DbActor.DbActorCatchupAck]

    val list: List[Message] = answer.asInstanceOf[DbActor.DbActorCatchupAck].messages

    list.size should equal(2)
    list should contain(MESSAGE)
    list should contain(message2)
  }

  test("addWitnessSignature should not fail if the messageId is valid") {
    // arrange
    val storage: InMemoryStorage = InMemoryStorage()
    val message: Message = MESSAGE
    val messageId: Hash = message.message_id
    val signature: Signature = Signature(Base64Data.encode("witnessSign")) // not valid signature, just to test storage

    val dbActor: ActorRef = system.actorOf(Props(DbActor(mediatorRef, MessageRegistry(), storage)))

    storage.size should equal(0)

    // act
    dbActor ! DbActor.CreateChannel(Channel(CHANNEL_NAME), ObjectType.LAO);
    sleep()
    // writing to the previously created channel
    dbActor ! DbActor.Write(Channel(CHANNEL_NAME), MESSAGE);
    sleep()
    dbActor ! DbActor.AddWitnessSignature(Channel(CHANNEL_NAME), messageId, signature)

    message.addWitnessSignature(WitnessSignaturePair(message.sender, signature))

    // assert
    expectMsg(DbActor.DbActorAck())
    storage.size should equal(2)
    storage.elements(CHANNEL_NAME) should equal(ChannelData(ObjectType.LAO, MESSAGE.message_id :: Nil).toJsonString)
    storage.elements(s"$CHANNEL_NAME${Channel.DATA_SEPARATOR}${MESSAGE.message_id}") should equal(message.toJsonString)
  }

  test("catchup should not fail on a channel with ChannelData containing missing message_ids (and only return valid messages)") {

    // arrange
    val channelName1: Channel = Channel(CHANNEL_NAME)
    val initialStorage: InMemoryStorage = InMemoryStorage()
    val message2Id: Hash = Hash(Base64Data("RmFrZSBtZXNzYWdlX2lkIDopIE5vIGVhc3RlciBlZ2cgcmlnaHQgdGhlcmUhIC0tIE5pY29sYXMgUmF1bGlu"))
    val listIds: List[Hash] = MESSAGE.message_id :: message2Id :: Nil
    val channelData: ChannelData = ChannelData(ObjectType.LAO, listIds)
    initialStorage.write(
      (CHANNEL_NAME, channelData.toJsonString),
      (s"$CHANNEL_NAME${Channel.DATA_SEPARATOR}${MESSAGE.message_id}", MESSAGE.toJsonString)
    )
    val dbActor: AskableActorRef = system.actorOf(Props(DbActor(mediatorRef, MessageRegistry(), initialStorage)))

    // act
    val ask = dbActor ? DbActor.Catchup(channelName1)
    val answer = Await.result(ask, duration)

    // assert
    answer shouldBe a[DbActor.DbActorCatchupAck]

    val list: List[Message] = answer.asInstanceOf[DbActor.DbActorCatchupAck].messages

    list.size should equal(1)
    list should contain(MESSAGE)
  }

  test("writeRollCallData succeeds for both new and updated data") {
    // arrange
    val storage: InMemoryStorage = InMemoryStorage()
    val dbActor: ActorRef = system.actorOf(Props(DbActor(mediatorRef, MessageRegistry(), storage)))
    val laoId: Hash = Hash(Base64Data.encode("laoId"))
    val rollcallKey: String = s"${ROOT_CHANNEL_PREFIX}${laoId.toString}/rollcall"

    val messageRollcall: Message = CreateRollCallExamples.MESSAGE_CREATE_ROLL_CALL_WORKING

    storage.size should equal(0)

    // act (1)
    dbActor ! DbActor.WriteRollCallData(laoId, messageRollcall);
    sleep()

    // assert
    expectMsg(DbActor.DbActorAck())
    storage.size should equal(1)

    val actualRollcallData: RollCallData = RollCallData.buildFromJson(storage.elements(rollcallKey))

    actualRollcallData.state should equal(ActionType.CREATE)
    actualRollcallData.updateId should equal(CreateRollCallExamples.R_ID)

    // act (2)
    val messageRollcall2: Message = OpenRollCallExamples.MESSAGE_OPEN_ROLL_CALL_WORKING
    dbActor ! DbActor.WriteRollCallData(laoId, messageRollcall2);
    sleep()

    // assert (2)
    expectMsg(DbActor.DbActorAck())
    storage.size should equal(1)

    val actualRollcallData2: RollCallData = RollCallData.buildFromJson(storage.elements(rollcallKey))

    actualRollcallData2.state should equal(ActionType.OPEN)
    actualRollcallData2.updateId should equal(OpenRollCallExamples.UPDATE_ID)
  }

  test("readRollCallData succeeds for existing RollCallData") {
    // arrange
    val laoId: Hash = Hash(Base64Data.encode("laoId"))
    val updateId: Hash = Hash(Base64Data.encode("updateId"))
    val rollcallKey: String = s"${ROOT_CHANNEL_PREFIX}${laoId.toString}/rollcall"
    val rollcallData: RollCallData = RollCallData(updateId, ActionType.CREATE)
    val initialStorage: InMemoryStorage = InMemoryStorage()
    initialStorage.write((rollcallKey, rollcallData.toJsonString))
    val dbActor: AskableActorRef = system.actorOf(Props(DbActor(mediatorRef, MessageRegistry(), initialStorage)))

    // act
    val ask = dbActor ? DbActor.ReadRollCallData(laoId)
    val answer = Await.result(ask, duration)

    // assert
    answer shouldBe a[DbActor.DbActorReadRollCallDataAck]

    val readRollcallData: RollCallData = answer.asInstanceOf[DbActor.DbActorReadRollCallDataAck].rollcallData

    readRollcallData.state should equal(ActionType.CREATE)
    readRollcallData.updateId should equal(updateId)
  }
}

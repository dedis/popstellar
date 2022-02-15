package ch.epfl.pop.storage

import akka.actor.{ActorRef, ActorSystem, Props}
import akka.testkit.{ImplicitSender, TestKit}
import ch.epfl.pop.model.network.method.message.Message
import ch.epfl.pop.model.network.method.message.data.ObjectType
import ch.epfl.pop.model.objects._
import ch.epfl.pop.pubsub.PubSubMediator
import org.scalatest.{BeforeAndAfterAll, FunSuiteLike, Matchers}


class DbActorNewSuite extends TestKit(ActorSystem("DbActorNewSuiteActorSystem")) with FunSuiteLike with ImplicitSender with Matchers with BeforeAndAfterAll {

  final val mediatorRef: ActorRef = system.actorOf(PubSubMediator.props)

  final val CHANNEL_NAME: String = "/root/wex"
  final val MESSAGE: Message = {
    val empty = Base64Data.encode("")
    new Message(empty, PublicKey(empty), Signature(empty), Hash(empty), Nil, None)
  }

  case class InMemoryStorage(initial: Map[String, String] = Map.empty) extends Storage {
    var elements: Map[String, String] = initial
    def size: Int = elements.size

    override def read(key: String): Option[String] = elements.get(key)

    // Note: this write does NOT write as batch
    override def write(keyValues: (String, String)*): Unit = {
      for (kv <- keyValues) {
        elements += (kv._1 -> kv._2)
      }
    }

    override def delete(key: String): Unit = elements -= key

    override def close(): Unit = ()

    def dump(): Unit = for ((k, v) <- elements) println(s"  > $k | $v")
  }

  override def afterAll(): Unit = {
    // Stops the test actor system
    TestKit.shutdownActorSystem(system)
  }

  def sleep(duration: Long = 50): Unit = {
    Thread.sleep(duration)
  }


  // TODO REFACTORING TUOMAS - replace "ignore" by "test" once the READ functions are implemented (readChannelData in particular)
  ignore("write can WRITE in an existing channel") {
    val storage: InMemoryStorage = InMemoryStorage()
    val dbActor: ActorRef = system.actorOf(Props(DbActorNew(mediatorRef, storage)))

    storage.size should equal (0)

    dbActor ! DbActorNew.CreateChannel(Channel(CHANNEL_NAME), ObjectType.LAO); sleep()

    expectMsg(DbActorNew.DbActorAck())
    storage.size should equal (1)

    // writing to the previously created channel
    dbActor ! DbActorNew.Write(Channel(CHANNEL_NAME), MESSAGE); sleep()

    expectMsg(DbActorNew.DbActorAck())
    storage.size should equal (2)
    storage.elements(CHANNEL_NAME) should equal (ChannelData(ObjectType.LAO, MESSAGE.message_id :: Nil).toJsonString)
    storage.elements(s"$CHANNEL_NAME:${MESSAGE.message_id}") should equal (MESSAGE.toJsonString)
  }

  // TODO REFACTORING TUOMAS - replace "ignore" by "test" once the READ functions are implemented (readChannelData in particular)
  ignore("write can WRITE in a non-existing channel") {
    val storage: InMemoryStorage = InMemoryStorage()
    val dbActor: ActorRef = system.actorOf(Props(DbActorNew(mediatorRef, storage)))

    storage.size should equal (0)

    // writing to non-(yet-)existing channel
    dbActor ! DbActorNew.Write(Channel(CHANNEL_NAME), MESSAGE); sleep()

    expectMsg(DbActorNew.DbActorAck())
    storage.size should equal (2)
    storage.elements(CHANNEL_NAME) should equal (ChannelData(ObjectType.LAO, MESSAGE.message_id :: Nil).toJsonString)
    storage.elements(s"$CHANNEL_NAME:${MESSAGE.message_id}") should equal (MESSAGE.toJsonString)
  }

  // TODO REFACTORING TUOMAS - replace "ignore" by "test" once the READ functions are implemented (readChannelData in particular)
  ignore("write behaves normally for multiple WRITE requests") {
    val storage: InMemoryStorage = InMemoryStorage()
    val dbActor: ActorRef = system.actorOf(Props(DbActorNew(mediatorRef, storage)))

    val channelName1 = CHANNEL_NAME
    val channelName2 = s"${CHANNEL_NAME}2"

    val message1 = MESSAGE
    val message2 = MESSAGE.copy(message_id = Hash(Base64Data("RmFrZSBtZXNzYWdlX2lkIDopIE5vIGVhc3RlciBlZ2cgcmlnaHQgdGhlcmUhIC0tIE5pY29sYXMgUmF1bGlu")))

    storage.size should equal (0)

    // adding a channel (channel 1)
    dbActor ! DbActorNew.CreateChannel(Channel(channelName1), ObjectType.LAO); sleep()

    expectMsg(DbActorNew.DbActorAck())
    storage.size should equal (1) // ChannelData for channel 1


    // ------------------------- 1st WRITE REQUEST ------------------------- //

    // writing to the existing channel 1
    dbActor ! DbActorNew.Write(Channel(channelName1), message1); sleep()

    expectMsg(DbActorNew.DbActorAck())
    storage.size should equal (2) // ChannelData for channel 1 + payload for message 1
    storage.elements(channelName1) should equal (ChannelData(ObjectType.LAO, message1.message_id :: Nil).toJsonString)
    storage.elements(s"$channelName1:${message1.message_id}") should equal (message1.toJsonString)


    // ------------------------- 2nd WRITE REQUEST ------------------------- //

    // writing to the non-(yet-)existing channel 2
    dbActor ! DbActorNew.Write(Channel(channelName2), message2); sleep()

    expectMsg(DbActorNew.DbActorAck())
    storage.size should equal (4) // ChannelData for channel 1 & 2 + payload for message 1 & 2
    storage.elements(channelName1) should equal (ChannelData(ObjectType.LAO, message1.message_id :: Nil).toJsonString)
    storage.elements(s"$channelName1:${message1.message_id}") should equal (message1.toJsonString)
    storage.elements(channelName2) should equal (ChannelData(ObjectType.LAO, message2.message_id :: Nil).toJsonString)
    storage.elements(s"$channelName2:${message2.message_id}") should equal (message2.toJsonString)


    // ------------------------- 3rd WRITE REQUEST ------------------------- //

    // writing (a new message) to the existing channel 1
    dbActor ! DbActorNew.Write(Channel(channelName1), message2); sleep()

    expectMsg(DbActorNew.DbActorAck())
    storage.size should equal (5) // ChannelData for channel 1 & 2 + payload for message 1 & 2 (on channel 1) & 2 (on channel 2)
    storage.elements(channelName1) should equal (ChannelData(ObjectType.LAO, message2.message_id :: message1.message_id :: Nil).toJsonString)
    storage.elements(s"$channelName1:${message1.message_id}") should equal (message1.toJsonString)
    storage.elements(s"$channelName1:${message2.message_id}") should equal (message2.toJsonString)
    storage.elements(channelName2) should equal (ChannelData(ObjectType.LAO, message2.message_id :: Nil).toJsonString)
    storage.elements(s"$channelName2:${message2.message_id}") should equal (message2.toJsonString)
  }

  test("createChannel effectively creates a new channel") {
    val storage: InMemoryStorage = InMemoryStorage()
    val dbActor: ActorRef = system.actorOf(Props(DbActorNew(mediatorRef, storage)), "r")

    storage.size should equal (0)

    dbActor ! DbActorNew.CreateChannel(Channel(CHANNEL_NAME), ObjectType.LAO); sleep()

    expectMsg(DbActorNew.DbActorAck())
    storage.size should equal (1)
    storage.elements(CHANNEL_NAME) should equal (ChannelData(ObjectType.LAO, Nil).toJsonString)
  }

  test("createChannel does not overwrite channels on duplicates") {
    val storage: InMemoryStorage = InMemoryStorage()
    val dbActor: ActorRef = system.actorOf(Props(DbActorNew(mediatorRef, storage)))

    storage.size should equal (0)

    dbActor ! DbActorNew.CreateChannel(Channel(CHANNEL_NAME), ObjectType.LAO); sleep()

    expectMsg(DbActorNew.DbActorAck())
    storage.size should equal (1)
    storage.elements(CHANNEL_NAME) should equal (ChannelData(ObjectType.LAO, Nil).toJsonString)

    dbActor ! DbActorNew.CreateChannel(Channel(CHANNEL_NAME), ObjectType.LAO); sleep()

    // expectMsg(DbActorNew.DbActorNAck())
    storage.size should equal (1)
    storage.elements(CHANNEL_NAME) should equal (ChannelData(ObjectType.LAO, Nil).toJsonString)
  }

  test("createChannelsFromList creates multiple channels") {
    val storage: InMemoryStorage = InMemoryStorage()
    val dbActor: ActorRef = system.actorOf(Props(DbActorNew(mediatorRef, storage)))

    storage.size should equal (0)

    dbActor ! DbActorNew.CreateChannelsFromList((Channel(CHANNEL_NAME), ObjectType.ELECTION) :: (Channel(s"${CHANNEL_NAME}2"), ObjectType.ROLL_CALL) :: Nil); sleep()

    expectMsg(DbActorNew.DbActorAck())
    storage.size should equal (2)
    storage.elements(CHANNEL_NAME) should equal (ChannelData(ObjectType.ELECTION, Nil).toJsonString)
    storage.elements(s"${CHANNEL_NAME}2") should equal (ChannelData(ObjectType.ROLL_CALL, Nil).toJsonString)
  }

  test("createChannelsFromList does not overwrite channels on duplicates (duplicates already created)") {
    val storage: InMemoryStorage = InMemoryStorage()
    val dbActor: ActorRef = system.actorOf(Props(DbActorNew(mediatorRef, storage)))

    storage.size should equal (0)

    dbActor ! DbActorNew.CreateChannel(Channel(CHANNEL_NAME), ObjectType.LAO); sleep()

    expectMsg(DbActorNew.DbActorAck())
    storage.size should equal (1)

    // adding 2 new channels (one of which already exists)
    dbActor ! DbActorNew.CreateChannelsFromList((Channel(CHANNEL_NAME), ObjectType.ELECTION) :: (Channel(s"${CHANNEL_NAME}2"), ObjectType.ROLL_CALL) :: Nil); sleep()

    expectMsg(DbActorNew.DbActorAck())
    storage.size should equal (2)
    storage.elements(CHANNEL_NAME) should equal (ChannelData(ObjectType.LAO, Nil).toJsonString)
    storage.elements(s"${CHANNEL_NAME}2") should equal (ChannelData(ObjectType.ROLL_CALL, Nil).toJsonString)
  }

  test("createChannelsFromList does not overwrite channels on duplicates (duplicates in the list)") {
    val storage: InMemoryStorage = InMemoryStorage()
    val dbActor: ActorRef = system.actorOf(Props(DbActorNew(mediatorRef, storage)))

    storage.size should equal (0)

    // adding 2 new channels (same elements)
    dbActor ! DbActorNew.CreateChannelsFromList((Channel(CHANNEL_NAME), ObjectType.LAO) :: (Channel(CHANNEL_NAME), ObjectType.LAO) :: Nil); sleep()

    expectMsg(DbActorNew.DbActorAck())
    storage.size should equal (1)
    storage.elements(CHANNEL_NAME) should equal (ChannelData(ObjectType.LAO, Nil).toJsonString)
  }

  test("createChannelsFromList works for 0 or 1 element") {
    val storage: InMemoryStorage = InMemoryStorage()
    val dbActor: ActorRef = system.actorOf(Props(DbActorNew(mediatorRef, storage)))

    storage.size should equal (0)

    // adding no channel
    dbActor ! DbActorNew.CreateChannelsFromList(Nil); sleep()

    expectMsg(DbActorNew.DbActorAck())
    storage.size should equal (0)

    dbActor ! DbActorNew.CreateChannelsFromList((Channel(CHANNEL_NAME), ObjectType.LAO) :: Nil); sleep()

    expectMsg(DbActorNew.DbActorAck())
    storage.size should equal (1)
    storage.elements(CHANNEL_NAME) should equal (ChannelData(ObjectType.LAO, Nil).toJsonString)
  }

  test("checkChannelExistence does not detect a non-existing channel") {
    val storage: InMemoryStorage = InMemoryStorage()
    val dbActor: ActorRef = system.actorOf(Props(DbActorNew(mediatorRef, storage)))

    storage.size should equal (0)

    // checking the existence of a non-existing channel
    dbActor ! DbActorNew.ChannelExists(Channel(CHANNEL_NAME)); sleep()

    // expectMsg(DbActorNew.DbActorNAck())
    storage.size should equal (0)
  }

  test("checkChannelExistence succeeds on the detection of an existing channel") {
    val storage: InMemoryStorage = InMemoryStorage()
    val dbActor: ActorRef = system.actorOf(Props(DbActorNew(mediatorRef, storage)))

    storage.size should equal (0)

    dbActor ! DbActorNew.CreateChannel(Channel(CHANNEL_NAME), ObjectType.LAO); sleep()

    storage.size should equal (1)

    // checking the existence of an existing channel
    dbActor ! DbActorNew.ChannelExists(Channel(CHANNEL_NAME)); sleep()

    expectMsg(DbActorNew.DbActorAck())
    storage.size should equal (1)
    storage.elements(CHANNEL_NAME) should equal (ChannelData(ObjectType.LAO, Nil).toJsonString)
  }

}

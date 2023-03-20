package ch.epfl.pop.pubsub.graph.validators

import akka.actor.{Actor, ActorSystem, Props, Status}
import akka.pattern.AskableActorRef
import akka.testkit.{ImplicitSender, TestKit}
import akka.util.Timeout
import ch.epfl.pop.model.network.method.message.data.ObjectType
import ch.epfl.pop.model.objects._
import ch.epfl.pop.pubsub.graph.PipelineError
import ch.epfl.pop.storage.DbActor

//import util.examples.MessageExample._
import org.scalatest.BeforeAndAfterAll
import org.scalatest.funsuite.{AnyFunSuiteLike => FunSuiteLike}
import org.scalatest.matchers.should.Matchers
import util.examples.JsonRpcRequestExample._

import scala.concurrent.duration.FiniteDuration

class MessageValidatorSuite extends TestKit(ActorSystem("messageValidatorTestActorSystem")) with FunSuiteLike with ImplicitSender with Matchers with BeforeAndAfterAll {

  // Implicits for system actors
  implicit val duration: FiniteDuration = FiniteDuration(5, "seconds")
  implicit val timeout: Timeout = Timeout(duration)

  override def afterAll(): Unit = {
    // Stops the testKit
    TestKit.shutdownActorSystem(system)
  }

  private final val PUBLIC_KEY: PublicKey = PublicKey(Base64Data("jsNj23IHALvppqV1xQfP71_3IyAHzivxiCz236_zzQc="))
  private final val PRIVATE_KEY: PrivateKey = PrivateKey(Base64Data("qRfms3wzSLkxAeBz6UtwA-L1qP0h8D9XI1FSvY68t7Y="))
  private final val PK_OWNER: PublicKey = PublicKey(Base64Data.encode("owner"))
  private final val PK_ATTENDEE: PublicKey = PublicKey(Base64Data.encode("attendee1"))
  private final val PK_FALSE: PublicKey = PublicKey(Base64Data.encode("false"))
  private final val CHANNEL: Channel = Channel(Channel.ROOT_CHANNEL_PREFIX + "channel")
  private final val laoData: LaoData = LaoData(PK_OWNER, List(PK_ATTENDEE), PRIVATE_KEY, PUBLIC_KEY, List.empty)

  private final val channelData: ChannelData = ChannelData(ObjectType.LAO, List.empty)

  private def mockDbNack: AskableActorRef = {
    val mockedDB = Props(new Actor() {
      override def receive: Receive = {
        case DbActor.ReadLaoData(_) =>
          sender() ! Status.Failure(DbActorNAckException(0, "error"))
        case DbActor.ReadChannelData(_) =>
          sender() ! Status.Failure(DbActorNAckException(0, "error"))
      }
    })
    system.actorOf(mockedDB)
  }

  private def mockDbAckWithNone: AskableActorRef = {
    val mockedDB = Props(new Actor() {
      override def receive: Receive = {
        case DbActor.ReadLaoData(_) =>
          sender() ! Status.Failure(DbActorNAckException(0, "No lao data (mocked)"))
      }
    })
    system.actorOf(mockedDB)
  }

  private def mockDbAckWithLaoData(data: LaoData): AskableActorRef = {
    val mockedDB = Props(new Actor() {
      override def receive: Receive = {
        case DbActor.ReadLaoData(_) =>
          sender() ! DbActor.DbActorReadLaoDataAck(data)
      }
    })
    system.actorOf(mockedDB)
  }

  private def mockDbAckWithChannelData(data: ChannelData): AskableActorRef = {
    val mockedDB = Props(new Actor() {
      override def receive: Receive = {
        case DbActor.ReadChannelData(_) =>
          sender() ! DbActor.DbActorReadChannelDataAck(data)
      }
    })
    system.actorOf(mockedDB)
  }

  test("validateOwner handles NAck with 'false' output") {
    lazy val dbActorRef = mockDbNack
    MessageValidator.validateOwner(PK_OWNER, CHANNEL, dbActorRef) should equal(false)
    system.stop(dbActorRef.actorRef)
  }

  test("validateAttendee handles NAck with 'false' output") {
    lazy val dbActorRef = mockDbNack
    MessageValidator.validateAttendee(PK_ATTENDEE, CHANNEL, dbActorRef) should equal(false)
    system.stop(dbActorRef.actorRef)
  }

  test("validateOwner handles ReadLaoDataAck containing None with 'false' output") {
    lazy val dbActorRef = mockDbAckWithNone
    MessageValidator.validateOwner(PK_OWNER, CHANNEL, dbActorRef) should equal(false)
    system.stop(dbActorRef.actorRef)
  }

  test("validateAttendee handles ReadLaoDataAck containing None with 'false' output") {
    lazy val dbActorRef = mockDbAckWithNone
    MessageValidator.validateAttendee(PK_ATTENDEE, CHANNEL, dbActorRef) should equal(false)
    system.stop(dbActorRef.actorRef)
  }

  test("validateOwner handles ReadLaoDataAck containing LaoData with corresponding output") {
    lazy val dbActorRef = mockDbAckWithLaoData(laoData)
    MessageValidator.validateOwner(PK_OWNER, CHANNEL, dbActorRef) should equal(true)
    MessageValidator.validateOwner(PK_FALSE, CHANNEL, dbActorRef) should equal(false)
    system.stop(dbActorRef.actorRef)
  }

  test("validateAttendee handles ReadLaoDataAck containing LaoData with corresponding output") {
    lazy val dbActorRef = mockDbAckWithLaoData(laoData)
    MessageValidator.validateAttendee(PK_ATTENDEE, CHANNEL, dbActorRef) should equal(true)
    MessageValidator.validateAttendee(PK_FALSE, CHANNEL, dbActorRef) should equal(false)
    system.stop(dbActorRef.actorRef)
  }

  test("validateChannelType handles NAck with 'false' output") {
    lazy val dbActorRef = mockDbNack
    MessageValidator.validateChannelType(ObjectType.LAO, CHANNEL, dbActorRef) should equal(false)
    system.stop(dbActorRef.actorRef)
  }

  test("validateAttendee handles ReadChannelDataAck containing ChannelData with corresponding output") {
    lazy val dbActorRef = mockDbAckWithChannelData(channelData)
    MessageValidator.validateChannelType(ObjectType.LAO, CHANNEL, dbActorRef) should equal(true)
    MessageValidator.validateChannelType(ObjectType.CHIRP, CHANNEL, dbActorRef) should equal(false)
    system.stop(dbActorRef.actorRef)
  }

  test("validateMessage accepts valid Message example") {
    MessageValidator.validateMessage(VALID_RPC) should be(Right(VALID_RPC))
  }

  test("validateMessage rejects request with invalid id") {
    MessageValidator.validateMessage(INVALID_ID_RPC) shouldBe a[Left[_, PipelineError]]
  }

  test("validateMessage rejects request with invalid WS pair") {
    MessageValidator.validateMessage(INVALID_WS_PAIR_RPC) shouldBe a[Left[_, PipelineError]]
  }

  test("validateMessage rejects request with invalid signature") {
    MessageValidator.validateMessage(INVALID_SIGNATURE_RPC) shouldBe a[Left[_, PipelineError]]
  }

}

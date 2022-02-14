package ch.epfl.pop.pubsub.graph.validators

import akka.actor.{Actor,ActorSystem,Props, Status}
import akka.pattern.AskableActorRef
import akka.testkit.{ImplicitSender, TestKit}
import akka.util.Timeout
import ch.epfl.pop.model.network.method.message.data.ObjectType
import ch.epfl.pop.model.objects._
import ch.epfl.pop.pubsub.graph.{DbActor, PipelineError}

//import util.examples.MessageExample._
import org.scalatest.{BeforeAndAfterAll, FunSuiteLike, Matchers}
import util.examples.JsonRpcRequestExample._

import scala.concurrent.duration.FiniteDuration

class MessageValidatorSuite extends TestKit(ActorSystem("messageValidatorTestActorSystem")) with FunSuiteLike with ImplicitSender with Matchers with BeforeAndAfterAll {

  // Implicites for system actors
  implicit val duration = FiniteDuration(5 ,"seconds")
  implicit val timeout = Timeout(duration)
  override def afterAll(): Unit = {
    // Stops the testKit
    TestKit.shutdownActorSystem(system)
  }

  private final val PUBLICKEY: PublicKey = PublicKey(Base64Data("jsNj23IHALvppqV1xQfP71_3IyAHzivxiCz236_zzQc="))
  private final val PRIVATEKEY: PrivateKey = PrivateKey(Base64Data("qRfms3wzSLkxAeBz6UtwA-L1qP0h8D9XI1FSvY68t7Y="))
  private final val PKOWNER: PublicKey = PublicKey(Base64Data.encode("owner"))
  private final val PKATTENDEE: PublicKey = PublicKey(Base64Data.encode("attendee1"))
  private final val PKFALSE: PublicKey = PublicKey(Base64Data.encode("false"))
  private final val CHANNEL: Channel = Channel(Channel.ROOT_CHANNEL_PREFIX + "channel")
  private final val laoData: LaoData = LaoData(PKOWNER, List(PKATTENDEE), PRIVATEKEY, PUBLICKEY, List.empty)

  private final val channelData: ChannelData = ChannelData(ObjectType.LAO, List.empty)

    


  private def mockDbNack: AskableActorRef = {
    val mockedDB = Props(new Actor(){
      override def receive = {
        case DbActor.ReadLaoData(channel) =>
          sender() ! Status.Failure(new DbActorNAckException(0, "error"))
        case DbActor.ReadChannelData(channel) =>
          sender() ! Status.Failure(new DbActorNAckException(0, "error"))
      }
    }
  )
  system.actorOf(mockedDB)
  }

  private def mockDbAckWithNone: AskableActorRef = {
    val mockedDB = Props(new Actor(){
      override def receive = {
        case DbActor.ReadLaoData(channel) =>
          sender() ! DbActor.DbActorReadLaoDataAck(None)
      }
    }
    )
    system.actorOf(mockedDB)
  }

  private def mockDbAckWithLaoData(data: LaoData): AskableActorRef = {
    val mockedDB = Props(new Actor(){
      override def receive = {
        case DbActor.ReadLaoData(channel) =>
          sender() ! DbActor.DbActorReadLaoDataAck(Some(data))
      }
    }
    )
    system.actorOf(mockedDB)
  }

  private def mockDbAckWithChannelData(data: ChannelData): AskableActorRef = {
    val mockedDB = Props(new Actor(){
      override def receive = {
        case DbActor.ReadChannelData(channel) =>
          sender() ! DbActor.DbActorReadChannelDataAck(Some(data))
      }
    }
    )
    system.actorOf(mockedDB)
  }

  test("validateOwner handles NAck with 'false' output"){
    lazy val dbActorRef = mockDbNack
    MessageValidator.validateOwner(PKOWNER, CHANNEL, dbActorRef) should equal(false)
    system.stop(dbActorRef.actorRef)
  }

  test("validateAttendee handles NAck with 'false' output"){
    lazy val dbActorRef = mockDbNack
    MessageValidator.validateAttendee(PKATTENDEE, CHANNEL, dbActorRef) should equal(false)
    system.stop(dbActorRef.actorRef)
  }

  test("validateOwner handles ReadLaoDataAck containing None with 'false' output"){
    lazy val dbActorRef = mockDbAckWithNone
    MessageValidator.validateOwner(PKOWNER, CHANNEL, dbActorRef) should equal(false)
    system.stop(dbActorRef.actorRef)
  }

  test("validateAttendee handles ReadLaoDataAck containing None with 'false' output"){
    lazy val dbActorRef = mockDbAckWithNone
    MessageValidator.validateAttendee(PKATTENDEE, CHANNEL, dbActorRef) should equal(false)
    system.stop(dbActorRef.actorRef)
  }

  test("validateOwner handles ReadLaoDataAck containing LaoData with corresponding output"){
    lazy val dbActorRef = mockDbAckWithLaoData(laoData)
    MessageValidator.validateOwner(PKOWNER, CHANNEL, dbActorRef) should equal(true)
    MessageValidator.validateOwner(PKFALSE, CHANNEL, dbActorRef) should equal(false)
    system.stop(dbActorRef.actorRef)
  }

  test("validateAttendee handles ReadLaoDataAck containing LaoData with corresponding output"){
    lazy val dbActorRef = mockDbAckWithLaoData(laoData)
    MessageValidator.validateAttendee(PKATTENDEE, CHANNEL, dbActorRef) should equal(true)
    MessageValidator.validateAttendee(PKFALSE, CHANNEL, dbActorRef) should equal(false)
    system.stop(dbActorRef.actorRef)
  }

  test("validateChannelType handles NAck with 'false' output"){
    lazy val dbActorRef = mockDbNack
    MessageValidator.validateChannelType(ObjectType.LAO, CHANNEL, dbActorRef) should equal(false)
    system.stop(dbActorRef.actorRef)
  }

  test("validateAttendee handles ReadChannelDataAck containing ChannelData with corresponding output"){
    lazy val dbActorRef = mockDbAckWithChannelData(channelData)
    MessageValidator.validateChannelType(ObjectType.LAO, CHANNEL, dbActorRef) should equal(true)
    MessageValidator.validateChannelType(ObjectType.CHIRP, CHANNEL, dbActorRef) should equal(false)
    system.stop(dbActorRef.actorRef)
  }

  test("validateMessage accepts valid Message example"){
    MessageValidator.validateMessage(VALID_RPC) should be (Left(VALID_RPC))
  }

  test("validateMessage rejects request with invalid id"){
    MessageValidator.validateMessage(INVALID_ID_RPC) shouldBe a [Right[_,PipelineError]]
  }

  test("validateMessage rejects request with invalid WS pair"){
    MessageValidator.validateMessage(INVALID_WS_PAIR_RPC) shouldBe a [Right[_,PipelineError]]
  }

  test("validateMessage rejects request with invalid signature"){
    MessageValidator.validateMessage(INVALID_SIGNATURE_RPC) shouldBe a [Right[_,PipelineError]]
  }    

}

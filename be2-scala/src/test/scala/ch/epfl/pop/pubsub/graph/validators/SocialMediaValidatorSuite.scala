package ch.epfl.pop.pubsub.graph.validators

import java.io.File
import java.util.concurrent.TimeUnit

import akka.actor.{Actor, ActorRef, ActorSystem, Props}
import akka.pattern.AskableActorRef
import akka.testkit.{ImplicitSender, TestKit}
import akka.util.Timeout
import ch.epfl.pop.model.network.method.message.data.ObjectType
import ch.epfl.pop.model.objects._
import ch.epfl.pop.pubsub.graph.{GraphMessage, PipelineError}
import ch.epfl.pop.pubsub.{AskPatternConstants, MessageRegistry, PubSubMediator}
import ch.epfl.pop.storage.{DbActor, InMemoryStorage}
import org.scalatest.BeforeAndAfterAll
import org.scalatest.funsuite.{AnyFunSuiteLike => FunSuiteLike}
import org.scalatest.matchers.should.Matchers
import util.examples.JsonRpcRequestExample._
import util.examples.socialMedia.AddChirpExamples

import scala.reflect.io.Directory

class SocialMediaValidatorSuite extends TestKit(ActorSystem("socialMediaValidatorTestActorSystem"))
    with FunSuiteLike
    with ImplicitSender
    with Matchers with BeforeAndAfterAll with AskPatternConstants {

  final val DB_TEST_FOLDER: String = "databaseSocialMediaTest"

  val pubSubMediatorRef: ActorRef = system.actorOf(PubSubMediator.props, "PubSubMediator")
  val dbActorRef: AskableActorRef = system.actorOf(Props(DbActor(pubSubMediatorRef, MessageRegistry(), InMemoryStorage())), "DbActor")

  // Implicit for system actors
  implicit val timeout: Timeout = Timeout(1, TimeUnit.SECONDS)

  override def afterAll(): Unit = {
    // Stops the testKit
    TestKit.shutdownActorSystem(system)

    // Deletes the test database
    val directory = new Directory(new File(DB_TEST_FOLDER))
    directory.deleteRecursively()
  }

  private final val PUBLIC_KEY: PublicKey = PublicKey(Base64Data("jsNj23IHALvppqV1xQfP71_3IyAHzivxiCz236_zzQc="))
  private final val PRIVATE_KEY: PrivateKey = PrivateKey(Base64Data("qRfms3wzSLkxAeBz6UtwA-L1qP0h8D9XI1FSvY68t7Y="))
  private final val PK_OWNER: PublicKey = PublicKey(Base64Data.encode("owner"))
  private final val laoDataRight: LaoData = LaoData(PK_OWNER, List(AddChirpExamples.SENDER_ADDCHIRP), PRIVATE_KEY, PUBLIC_KEY, List.empty)
  private final val laoDataWrong: LaoData = LaoData(PK_OWNER, List(PK_OWNER), PRIVATE_KEY, PUBLIC_KEY, List.empty)
  private final val channelDataRight: ChannelData = ChannelData(ObjectType.CHIRP, List.empty)
  private final val channelDataWrong: ChannelData = ChannelData(ObjectType.LAO, List.empty)
  private final val channelDataReaction: ChannelData = ChannelData(ObjectType.REACTION, List.empty)

  private def mockDbWorking: AskableActorRef = {
    val dbActorMock = Props(new Actor() {
      override def receive: Receive = {
        case DbActor.ReadLaoData(_) =>
          sender() ! DbActor.DbActorReadLaoDataAck(laoDataRight)
        case DbActor.ReadChannelData(_) =>
          sender() ! DbActor.DbActorReadChannelDataAck(channelDataRight)
        case DbActor.Read(_, _) =>
          sender() ! DbActor.DbActorReadAck
      }
    })
    system.actorOf(dbActorMock)
  }

  private def mockDbWorkingReaction: AskableActorRef = {
    val dbActorMock = Props(new Actor() {
      override def receive: Receive = {
        case DbActor.ReadLaoData(_) =>
          sender() ! DbActor.DbActorReadLaoDataAck(laoDataRight)
        case DbActor.ReadChannelData(_) =>
          sender() ! DbActor.DbActorReadChannelDataAck(channelDataReaction)
        case DbActor.Read(_, _) =>
          sender() ! DbActor.DbActorReadAck
      }
    })
    system.actorOf(dbActorMock)
  }

  private def mockDbWrongTokenReaction: AskableActorRef = {
    val dbActorMock = Props(new Actor() {
      override def receive: Receive = {
        case DbActor.ReadLaoData(_) =>
          sender() ! DbActor.DbActorReadLaoDataAck(laoDataWrong)
        case DbActor.ReadChannelData(_) =>
          sender() ! DbActor.DbActorReadChannelDataAck(channelDataReaction)
        case DbActor.Read(_, _) =>
          sender() ! DbActor.DbActorReadAck
      }
    })
    system.actorOf(dbActorMock)
  }

  private def mockDbWrongToken: AskableActorRef = {
    val dbActorMock = Props(new Actor() {
      override def receive: Receive = {
        case DbActor.ReadLaoData(_) =>
          sender() ! DbActor.DbActorReadLaoDataAck(laoDataWrong)
        case DbActor.ReadChannelData(_) =>
          sender() ! DbActor.DbActorReadChannelDataAck(channelDataRight)
        case DbActor.Read(_, _) =>
          sender() ! DbActor.DbActorReadAck
      }
    })
    system.actorOf(dbActorMock)
  }

  private def mockDbWrongChannel: AskableActorRef = {
    val dbActorMock = Props(new Actor() {
      override def receive: Receive = {
        case DbActor.ReadLaoData(_) =>
          sender() ! DbActor.DbActorReadLaoDataAck(laoDataRight)
        case DbActor.ReadChannelData(_) =>
          sender() ! DbActor.DbActorReadChannelDataAck(channelDataWrong)
        case DbActor.Read(_, _) =>
          sender() ! DbActor.DbActorReadAck
      }
    })
    system.actorOf(dbActorMock)
  }

  // AddChirp
  test("Adding a chirp works as intended") {
    val dbActorRef = mockDbWorking
    val message: GraphMessage = new SocialMediaValidator(dbActorRef).validateAddChirp(ADD_CHIRP_RPC)
    message should equal(Right(ADD_CHIRP_RPC))
    system.stop(dbActorRef.actorRef)
  }

  test("Adding a chirp with too long text fails") {
    val dbActorRef = mockDbWorking
    val message: GraphMessage = new SocialMediaValidator(dbActorRef).validateAddChirp(ADD_CHIRP_WRONG_TEXT_RPC)
    message shouldBe a[Left[_, PipelineError]]
    system.stop(dbActorRef.actorRef)
  }

  test("Adding a chirp with invalid Timestamp fails") {
    val dbActorRef = mockDbWorking
    val message: GraphMessage = new SocialMediaValidator(dbActorRef).validateAddChirp(ADD_CHIRP_WRONG_TIMESTAMP_RPC)
    message shouldBe a[Left[_, PipelineError]]
    system.stop(dbActorRef.actorRef)
  }

  test("Adding a chirp without valid PoP token fails") {
    val dbActorRef = mockDbWrongToken
    val message: GraphMessage = new SocialMediaValidator(dbActorRef).validateAddChirp(ADD_CHIRP_RPC)
    message shouldBe a[Left[_, PipelineError]]
    system.stop(dbActorRef.actorRef)
  }

  test("Adding a chirp on wrong type of channel fails") {
    val dbActorRef = mockDbWrongChannel
    val message: GraphMessage = new SocialMediaValidator(dbActorRef).validateAddChirp(ADD_CHIRP_RPC)
    message shouldBe a[Left[_, PipelineError]]
    system.stop(dbActorRef.actorRef)
  }

  test("Adding a chirp on channel which is not our own social channel fails") {
    val dbActorRef = mockDbWorking
    val message: GraphMessage = new SocialMediaValidator(dbActorRef).validateAddChirp(ADD_CHIRP_WRONG_CHANNEL_RPC)
    message shouldBe a[Left[_, PipelineError]]
    system.stop(dbActorRef.actorRef)
  }

  test("Validating a RpcMessage without Params does not work in validateAddChirp") {
    val dbActorRef = mockDbWorking
    val message: GraphMessage = new SocialMediaValidator(dbActorRef).validateAddChirp(RPC_NO_PARAMS)
    message shouldBe a[Left[_, PipelineError]]
    system.stop(dbActorRef.actorRef)
  }

  // DeleteChirp
  test("Deleting a chirp works as intended") {
    val dbActorRef = mockDbWorking
    val message: GraphMessage = new SocialMediaValidator(dbActorRef).validateDeleteChirp(DELETE_CHIRP_RPC)
    message should equal(Right(DELETE_CHIRP_RPC))
    system.stop(dbActorRef.actorRef)
  }

  test("Deleting a chirp with invalid Timestamp fails") {
    val dbActorRef = mockDbWorking
    val message: GraphMessage = new SocialMediaValidator(dbActorRef).validateDeleteChirp(DELETE_CHIRP_WRONG_TIMESTAMP_RPC)
    message shouldBe a[Left[_, PipelineError]]
    system.stop(dbActorRef.actorRef)
  }

  test("Deleting a chirp without valid PoP token fails") {
    val dbActorRef = mockDbWrongToken
    val message: GraphMessage = new SocialMediaValidator(dbActorRef).validateDeleteChirp(DELETE_CHIRP_RPC)
    message shouldBe a[Left[_, PipelineError]]
    system.stop(dbActorRef.actorRef)
  }

  test("Deleting a chirp on wrong type of channel fails") {
    val dbActorRef = mockDbWrongChannel
    val message: GraphMessage = new SocialMediaValidator(dbActorRef).validateDeleteChirp(DELETE_CHIRP_RPC)
    message shouldBe a[Left[_, PipelineError]]
    system.stop(dbActorRef.actorRef)
  }

  test("Deleting a chirp on channel which is not our own social channel fails") {
    val dbActorRef = mockDbWorking
    val message: GraphMessage = new SocialMediaValidator(dbActorRef).validateDeleteChirp(DELETE_CHIRP_WRONG_CHANNEL_RPC)
    message shouldBe a[Left[_, PipelineError]]
    system.stop(dbActorRef.actorRef)
  }

  test("Validating a RpcMessage without Params does not work in validateDeleteChirp") {
    val dbActorRef = mockDbWorking
    val message: GraphMessage = new SocialMediaValidator(dbActorRef).validateDeleteChirp(RPC_NO_PARAMS)
    message shouldBe a[Left[_, PipelineError]]
    system.stop(dbActorRef.actorRef)
  }

  // AddReaction
  test("Adding a reaction works as intended") {
    val dbActorRef = mockDbWorkingReaction
    val message: GraphMessage = new SocialMediaValidator(dbActorRef).validateAddReaction(ADD_REACTION_RPC)
    message should equal(Right(ADD_REACTION_RPC))
    system.stop(dbActorRef.actorRef)
  }

  test("Adding a reaction with invalid Timestamp fails") {
    val dbActorRef = mockDbWorkingReaction
    val message: GraphMessage = new SocialMediaValidator(dbActorRef).validateAddReaction(ADD_REACTION_WRONG_TIMESTAMP_RPC)
    message shouldBe a[Left[_, PipelineError]]
    system.stop(dbActorRef.actorRef)
  }

  test("Adding a reaction without valid PoP token fails") {
    val dbActorRef = mockDbWrongTokenReaction
    val message: GraphMessage = new SocialMediaValidator(dbActorRef).validateAddReaction(ADD_REACTION_RPC)
    message shouldBe a[Left[_, PipelineError]]
    system.stop(dbActorRef.actorRef)
  }

  test("Adding a reaction on wrong type of channel fails") {
    val dbActorRef = mockDbWrongChannel
    val message: GraphMessage = new SocialMediaValidator(dbActorRef).validateAddReaction(ADD_REACTION_RPC)
    message shouldBe a[Left[_, PipelineError]]
    system.stop(dbActorRef.actorRef)
  }

  test("Validating a RpcMessage without Params does not work in validateAddReaction") {
    val dbActorRef = mockDbWorkingReaction
    val message: GraphMessage = new SocialMediaValidator(dbActorRef).validateAddReaction(RPC_NO_PARAMS)
    message shouldBe a[Left[_, PipelineError]]
    system.stop(dbActorRef.actorRef)
  }

  // DeleteReaction
  test("Deleting a reaction works as intended") {
    val dbActorRef = mockDbWorkingReaction
    val message: GraphMessage = new SocialMediaValidator(dbActorRef).validateDeleteReaction(DELETE_REACTION_RPC)
    message should equal(Right(DELETE_REACTION_RPC))
    system.stop(dbActorRef.actorRef)
  }

  test("Deleting a reaction with invalid Timestamp fails") {
    val dbActorRef = mockDbWorkingReaction
    val message: GraphMessage = new SocialMediaValidator(dbActorRef).validateDeleteReaction(DELETE_REACTION_WRONG_TIMESTAMP_RPC)
    message shouldBe a[Left[_, PipelineError]]
    system.stop(dbActorRef.actorRef)
  }

  test("Deleting a reaction without valid PoP token fails") {
    val dbActorRef = mockDbWrongTokenReaction
    val message: GraphMessage = new SocialMediaValidator(dbActorRef).validateDeleteReaction(DELETE_REACTION_RPC)
    message shouldBe a[Left[_, PipelineError]]
    system.stop(dbActorRef.actorRef)
  }

  test("Deleting a reaction on wrong type of channel fails") {
    val dbActorRef = mockDbWrongChannel
    val message: GraphMessage = new SocialMediaValidator(dbActorRef).validateDeleteReaction(DELETE_REACTION_RPC)
    message shouldBe a[Left[_, PipelineError]]
    system.stop(dbActorRef.actorRef)
  }

  test("Validating a RpcMessage without Params does not work in validateDeleteReaction") {
    val dbActorRef = mockDbWorkingReaction
    val message: GraphMessage = new SocialMediaValidator(dbActorRef).validateDeleteReaction(RPC_NO_PARAMS)
    message shouldBe a[Left[_, PipelineError]]
    system.stop(dbActorRef.actorRef)
  }

}

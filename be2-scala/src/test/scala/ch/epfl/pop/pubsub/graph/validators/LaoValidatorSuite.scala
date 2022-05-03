package ch.epfl.pop.pubsub.graph.validators

import akka.actor.{Actor, ActorRef, ActorSystem, Props}
import ch.epfl.pop.model.network.JsonRpcRequest
import ch.epfl.pop.model.network.method.message.data.ObjectType
import ch.epfl.pop.model.objects.{Base64Data, ChannelData, LaoData, PrivateKey, PublicKey}
import ch.epfl.pop.storage.{DbActor, InMemoryStorage}
import util.examples.Lao.GreetLaoExamples
//import akka.actor.typed.ActorRef
import akka.pattern.AskableActorRef
import akka.testkit.{ImplicitSender, TestKit}
import akka.util.Timeout
import ch.epfl.pop.pubsub.graph.{GraphMessage, PipelineError}
import ch.epfl.pop.pubsub.{AskPatternConstants, MessageRegistry, PubSubMediator}

//import util.examples.MessageExample._
import java.io.File
import java.util.concurrent.TimeUnit

import org.scalatest.{BeforeAndAfterAll, FunSuiteLike, Matchers}
import util.examples.JsonRpcRequestExample._

import scala.reflect.io.Directory

class LaoValidatorSuite extends TestKit(ActorSystem("laoValidatorTestActorSystem"))
  with FunSuiteLike
  with ImplicitSender
  with Matchers with BeforeAndAfterAll with AskPatternConstants {

  implicit val timeout: Timeout = Timeout(1, TimeUnit.SECONDS)

  final val DB_TEST_FOLDER: String = "databaseLaoTest"

  val pubSubMediatorRef: ActorRef = system.actorOf(PubSubMediator.props, "PubSubMediator")
  val dbActorRef: AskableActorRef = system.actorOf(Props(DbActor(pubSubMediatorRef, MessageRegistry(), InMemoryStorage())), "DbActor")

  override def afterAll(): Unit = {
    // Stops the test actor system
    TestKit.shutdownActorSystem(system)

    // Deletes the test database
    val directory = new Directory(new File(DB_TEST_FOLDER))
    directory.deleteRecursively()
  }

  //LAO create
  test("LAO creation works as intended") {
    val message: GraphMessage = LaoValidator.validateCreateLao(CREATE_LAO_RPC)
    message should equal(Left(CREATE_LAO_RPC))
  }

  test("LAO creation fails with wrong channel") {
    val message: GraphMessage = LaoValidator.validateCreateLao(CREATE_LAO_WRONG_CHANNEL_RPC)
    message shouldBe a[Right[_, PipelineError]]
  }

  test("LAO creation fails with stale Timestamp") {
    val message: GraphMessage = LaoValidator.validateCreateLao(CREATE_LAO_WRONG_TIMESTAMP_RPC)
    message shouldBe a[Right[_, PipelineError]]
  }

  test("LAO creation fails with duplicate witnesses") {
    val message: GraphMessage = LaoValidator.validateCreateLao(CREATE_LAO_WRONG_WITNESSES_RPC)
    message shouldBe a[Right[_, PipelineError]]
  }

  test("LAO creation fails with wrong id") {
    val message: GraphMessage = LaoValidator.validateCreateLao(CREATE_LAO_WRONG_ID_RPC)
    message shouldBe a[Right[_, PipelineError]]
  }

  test("LAO creation fails with wrong sender") {
    val message: GraphMessage = LaoValidator.validateCreateLao(CREATE_LAO_WRONG_SENDER_RPC)
    message shouldBe a[Right[_, PipelineError]]
  }

  test("LAO creation fails with empty name") {
    val message: GraphMessage = LaoValidator.validateCreateLao(CREATE_LAO_EMPTY_NAME_RPC)
    message shouldBe a[Right[_, PipelineError]]
  }

  test("LAO creation fails without ParamsWithMessage") {
    val message: GraphMessage = LaoValidator.validateCreateLao(RPC_NO_PARAMS)
    message shouldBe a[Right[_, PipelineError]]
  }

  //LAO greet messages
  private final val sender: PublicKey = PublicKey(Base64Data("p_EYbHyMv6sopI5QhEXBf40MO_eNoq7V_LygBd4c9RA="))

  private final val PUBLIC_KEY: PublicKey = PublicKey(Base64Data("jsNj23IHALvppqV1xQfP71_3IyAHzivxiCz236_zzQc="))
  private final val PRIVATE_KEY: PrivateKey = PrivateKey(Base64Data("qRfms3wzSLkxAeBz6UtwA-L1qP0h8D9XI1FSvY68t7Y="))
  private final val PK_OWNER: PublicKey = PublicKey(Base64Data.encode("wrongOwner"))
  private final val laoDataRight: LaoData = LaoData(sender, List(sender), PRIVATE_KEY, PUBLIC_KEY, List.empty)
  private final val laoDataWrong: LaoData = LaoData(PK_OWNER, List(PK_OWNER), PRIVATE_KEY, PUBLIC_KEY, List.empty)
  private final val channelDataRight: ChannelData = ChannelData(ObjectType.LAO, List.empty)
  private final val channelDataWrong: ChannelData = ChannelData(ObjectType.ELECTION, List.empty)



  private def mockDbWorking: AskableActorRef = {
    val dbActorMock = Props(new Actor() {
      override def receive: Receive = {
        case DbActor.ReadLaoData(_) =>
          sender() ! DbActor.DbActorReadLaoDataAck(laoDataRight)
        case DbActor.ReadChannelData(_) =>
          sender() ! DbActor.DbActorReadChannelDataAck(channelDataRight)
      }
    })
    system.actorOf(dbActorMock)
  }

  private def mockDbWrongChannelSetup: AskableActorRef = {
    val dbActorMock = Props(new Actor() {
      override def receive: Receive = {
        case DbActor.ReadLaoData(_) =>
          sender() ! DbActor.DbActorReadLaoDataAck(laoDataRight)
        case DbActor.ReadChannelData(_) =>
          sender() ! DbActor.DbActorReadChannelDataAck(channelDataWrong)
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
          sender() ! DbActor.DbActorReadChannelDataAck(channelDataWrong)
      }
    })
    system.actorOf(dbActorMock)
  }
}
